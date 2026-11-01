package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AuditLogEntry
import com.example.data.model.BarberProfile
import com.example.data.model.Booking
import com.example.data.model.BookingPrivateAddress
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.ReviewItem
import com.example.data.model.ServiceArea
import com.example.data.model.SupportTicket
import com.example.data.model.UserAccount
import com.example.data.firebase.DoorCutFirebase
import com.example.util.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = DoorCutFirebase.auth()
) {

    constructor(context: Context) : this(
        DoorCutFirebase.firestore(context),
        DoorCutFirebase.auth()
    )

    companion object {
        private const val TAG = "MarketplaceRepo"
        const val OWNER_EMAIL = "nomaankhannfs2@gmail.com"
        const val UNASSIGNED_BARBER_ID = "UNASSIGNED"
    }

    fun currentUid(): String? = auth.currentUser?.uid
    fun currentEmail(): String? = auth.currentUser?.email
    fun currentDisplayName(): String = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "DoorCut Member"

    fun observeAuthUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun ensureSignedInProfile(): Result<UserAccount> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        val uid = user.uid
        val email = user.email.orEmpty()
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").ifBlank { "DoorCut User" }

        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        val isBootstrappedOwner = email.equals(OWNER_EMAIL, ignoreCase = true) && user.isEmailVerified

        if (!snap.exists()) {
            val assignedRole = if (isBootstrappedOwner) "ADMIN" else "CUSTOMER"
            val payload = mapOf(
                "userId" to uid,
                "name" to displayName.take(100),
                "email" to email.take(150),
                "phone" to "",
                "role" to assignedRole,
                "status" to "ACTIVE",
                "dob" to "",
                "age" to 0L,
                "gender" to "",
                "onboardingCompleted" to isBootstrappedOwner,
                "privacyAccepted" to isBootstrappedOwner,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userRef.set(payload).await()
            if (isBootstrappedOwner) {
                runCatching {
                    db.collection("admins").document(uid).set(
                        mapOf("email" to email, "grantedAt" to FieldValue.serverTimestamp())
                    ).await()
                }
            }
        } else if (isBootstrappedOwner) {
            val currentRole = snap.getString("role") ?: "CUSTOMER"
            if (currentRole != "ADMIN") {
                userRef.update(
                    mapOf(
                        "role" to "ADMIN",
                        "status" to "ACTIVE",
                        "onboardingCompleted" to true,
                        "privacyAccepted" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

        val fresh = userRef.get().await()
        fresh.toObject(UserAccount::class.java) ?: UserAccount(
            id = uid,
            userId = uid,
            name = displayName,
            email = email,
            role = if (isBootstrappedOwner) "ADMIN" else "CUSTOMER",
            onboardingCompleted = isBootstrappedOwner
        )
    }

    fun observeUserAccount(uid: String): Flow<UserAccount?> = callbackFlow {
        val reg: ListenerRegistration = db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeUserAccount failed", err)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snap?.toObject(UserAccount::class.java))
            }
        awaitClose { reg.remove() }
    }

    suspend fun completeUserOnboarding(
        fullName: String,
        phone: String,
        dob: String,
        age: Long,
        gender: String,
        selectedRole: String,
        servesLadies: Boolean,
        servesMen: Boolean,
        experienceYears: Long,
        upiId: String,
        primaryArea: String,
        latitude: Double,
        longitude: Double,
        defaultServiceIds: List<String>
    ): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        val cleanName = fullName.trim().take(100).ifBlank { currentDisplayName() }
        val cleanPhone = phone.trim().take(30)
        val cleanGender = gender.uppercase().takeIf { it in setOf("MALE", "FEMALE", "OTHER") } ?: "MALE"
        val cleanAge = age.coerceIn(1L, 110L)
        val isOwnerAdmin = currentEmail()?.equals(OWNER_EMAIL, ignoreCase = true) == true &&
            auth.currentUser?.isEmailVerified == true

        val targetRole = when {
            isOwnerAdmin -> "ADMIN"
            selectedRole == "BARBER" -> "BARBER"
            else -> "CUSTOMER"
        }
        val targetStatus = if (targetRole == "BARBER") "PENDING" else "ACTIVE"

        db.collection("users").document(uid).update(
            mapOf(
                "name" to cleanName,
                "phone" to cleanPhone,
                "dob" to dob.trim().take(30),
                "age" to cleanAge,
                "gender" to cleanGender,
                "onboardingCompleted" to true,
                "privacyAccepted" to true,
                "role" to targetRole,
                "status" to targetStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        if (targetRole == "BARBER") {
            val skillsList = buildList {
                if (servesLadies) {
                    add("Lady Hair Stylist")
                    add("Layered Cut & Blowout")
                }
                if (servesMen) {
                    add("Precision Haircut")
                    add("Beard & Razor Trim")
                }
                add("Sanitized Home Kit")
            }.take(10)

            val badge = when {
                servesLadies && cleanGender == "FEMALE" -> "Verified Lady Hair Stylist"
                servesLadies && servesMen -> "Verified Unisex Home Stylist"
                else -> "Verified DoorCut Partner"
            }

            val profileRef = db.collection("barberProfiles").document(uid)
            val existingSnap = profileRef.get().await()
            if (!existingSnap.exists()) {
                profileRef.set(
                    mapOf(
                        "barberId" to uid,
                        "name" to cleanName,
                        "phone" to cleanPhone,
                        "gender" to cleanGender,
                        "servesLadies" to servesLadies,
                        "servesMen" to servesMen,
                        "bio" to "Verified home grooming specialist (${experienceYears.coerceIn(0L, 60L)} yrs experience).",
                        "experienceYears" to experienceYears.coerceIn(0L, 60L),
                        "approvalStatus" to "APPROVED",
                        "isOnline" to true,
                        "ratingAvg" to 5.0,
                        "reviewCount" to 0L,
                        "serviceIds" to defaultServiceIds.take(40),
                        "serviceAreaIds" to listOf("zone_central", "zone_south", "zone_west"),
                        "workingDays" to listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
                        "workStartHour" to 8L,
                        "workEndHour" to 21L,
                        "breakStartHour" to 13L,
                        "breakEndHour" to 14L,
                        "travelBufferMinutes" to 15L,
                        "upiId" to upiId.trim().take(100),
                        "skills" to skillsList,
                        "perfectionBadge" to badge,
                        "shopName" to "$cleanName Home Studio".take(120),
                        "shopAddress" to primaryArea.trim().ifBlank { "Active Home Dispatch Zone" }.take(240),
                        "homeVisitAvailable" to true,
                        "latitude" to latitude.coerceIn(-90.0, 90.0),
                        "longitude" to longitude.coerceIn(-180.0, 180.0),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                profileRef.update(
                    mapOf(
                        "name" to cleanName,
                        "phone" to cleanPhone,
                        "gender" to cleanGender,
                        "servesLadies" to servesLadies,
                        "servesMen" to servesMen,
                        "experienceYears" to experienceYears.coerceIn(0L, 60L),
                        "upiId" to upiId.trim().take(100),
                        "isOnline" to true,
                        "latitude" to latitude.coerceIn(-90.0, 90.0),
                        "longitude" to longitude.coerceIn(-180.0, 180.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    suspend fun updateUserContact(name: String, phone: String): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        db.collection("users").document(uid).update(
            mapOf(
                "name" to name.trim().take(100),
                "phone" to phone.trim().take(30),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // --- Initial Service Catalog & Service Areas Seed (Zero Fake Barbers) ---

    suspend fun ensureInitialSeedDataIfEmpty() {
        val uid = currentUid() ?: return
        try {
            val servicesSnap = db.collection("services").whereEqualTo("isActive", true).limit(1).get().await()
            if (servicesSnap.isEmpty) {
                seedDefaultCatalog()
            }
            val areasSnap = db.collection("serviceAreas").whereEqualTo("isActive", true).limit(1).get().await()
            if (areasSnap.isEmpty) {
                seedDefaultServiceAreas()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Seed check skipped for $uid: ${e.message}")
        }
    }

    private suspend fun seedDefaultCatalog() {
        val defaults = listOf(
            Triple("svc_classic_cut", "Haircut", Triple("HAIRCUTS", 30000L, 30L)) to
                ("Clean scissor & clipper haircut at home with protective cape, floor sheet, and neat neckline finish." to false),
            Triple("svc_beard_trim", "Beard Trim", Triple("BEARD_MOUSTACHE", 15000L, 15L)) to
                ("Even beard trim, sharp cheek & jawline line-up with a fresh single-use blade, and soothing aftershave." to false),
            Triple("svc_pkg_essential", "Haircut + Beard", Triple("PACKAGES", 40000L, 45L)) to
                ("Complete Haircut + crisp Beard Trim & razor line-up in one smooth 45-minute home visit." to true),
            Triple("svc_ladies_haircut", "Ladies Haircut & Trim", Triple("LADIES_STYLING", 39900L, 40L)) to
                ("U-cut, V-cut, straight trim, or fringe shaping at home by a verified hair stylist with full floor cleanup." to false),
            Triple("svc_ladies_layer_blowout", "Ladies Layer Cut & Blowout", Triple("LADIES_STYLING", 54900L, 50L)) to
                ("Multi-layer feather/step haircut with round-brush volume blow-dry styling in the comfort of home." to false),
            Triple("svc_normal_head_cut", "Normal Head Cut", Triple("HAIRCUTS", 19900L, 25L)) to
                ("Balanced everyday head cut using sanitized clippers & scissors with crisp neck outline." to false),
            Triple("svc_traditional_cut", "Traditional Haircut", Triple("HAIRCUTS", 22900L, 30L)) to
                ("Classic scissor-over-comb haircut with neat side-part, tapered ear contour, and fresh blade finish." to false),
            Triple("svc_skin_fade", "Zero / Mid / High Skin Fade", Triple("HAIRCUTS", 35000L, 40L)) to
                ("Seamless foil-shaver & precision guard gradient fade with sharp hairline detailing." to false),
            Triple("svc_kids_cut", "Kids' Gentle Home Haircut", Triple("HAIRCUTS", 25000L, 30L)) to
                ("Calm, patient, low-noise clipper & rounded-tip scissor haircut at home for children under 12." to false),
            Triple("svc_clean_shave", "Hot Towel Clean Shave", Triple("BEARD_MOUSTACHE", 18000L, 25L)) to
                ("Two-pass warm-towel shave with pre-shave oil, rich cream lather, and hydrating post-shave balm." to false),
            Triple("svc_scalp_oil", "Head Massage (Champi)", Triple("HAIR_CARE", 20000L, 20L)) to
                ("20-minute stress-relieving scalp, forehead & shoulder oil massage." to false),
            Triple("svc_face_cleanup", "Detan Face Cleanup", Triple("ADD_ONS", 35000L, 30L)) to
                ("4-step tan-removal cleanser, exfoliating scrub, mask, and brightening massage." to false),
            Triple("svc_pkg_complete", "Complete Grooming Package", Triple("PACKAGES", 79900L, 80L)) to
                ("Haircut + Beard/Styling + Detan Face Cleanup + Relaxing Head Massage." to true)
        )

        defaults.forEachIndexed { idx, (meta, extra) ->
            val (docId, name, catPriceDur) = meta
            val (category, pricePaise, durationMin) = catPriceDur
            val (desc, isPkg) = extra
            runCatching {
                db.collection("services").document(docId).set(
                    mapOf(
                        "name" to name,
                        "category" to category,
                        "description" to desc,
                        "pricePaise" to pricePaise,
                        "durationMinutes" to durationMin,
                        "isActive" to true,
                        "isPackage" to isPkg,
                        "instructions" to "Keep a comfortable chair near good lighting and a power socket.",
                        "sortOrder" to (idx + 1).toLong(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    private suspend fun seedDefaultServiceAreas() {
        val areas = listOf(
            Triple("zone_central", "Central Metro Zone", listOf("500034", "500033", "560038", "560001", "400050")),
            Triple("zone_south", "South Residential Zone", listOf("500081", "500032", "560034", "560102", "110016")),
            Triple("zone_west", "West & Tech Corridor", listOf("500084", "500019", "560066", "560103", "411014"))
        )
        areas.forEach { (id, name, pins) ->
            runCatching {
                db.collection("serviceAreas").document(id).set(
                    mapOf(
                        "name" to name,
                        "postalCodes" to pins,
                        "travelChargePaise" to 0L,
                        "maxDistanceKm" to 15L,
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    // --- Real-Time Streams ---

    fun observeActiveServices(includeInactiveForAdmin: Boolean = false): Flow<List<CatalogService>> = callbackFlow {
        val query: Query = if (includeInactiveForAdmin) {
            db.collection("services")
        } else {
            db.collection("services").whereEqualTo("isActive", true)
        }
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "observeActiveServices error", err)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snap?.documents
                ?.mapNotNull { it.toObject(CatalogService::class.java) }
                ?.sortedBy { it.sortOrder }
                .orEmpty()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    fun observeActiveServiceAreas(includeInactiveForAdmin: Boolean = false): Flow<List<ServiceArea>> = callbackFlow {
        val query: Query = if (includeInactiveForAdmin) {
            db.collection("serviceAreas")
        } else {
            db.collection("serviceAreas").whereEqualTo("isActive", true)
        }
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "observeActiveServiceAreas error", err)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snap?.documents?.mapNotNull { it.toObject(ServiceArea::class.java) }.orEmpty()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    fun observeApprovedBarbers(includeAllForAdmin: Boolean = false): Flow<List<BarberProfile>> = callbackFlow {
        val query: Query = if (includeAllForAdmin) {
            db.collection("barberProfiles")
        } else {
            db.collection("barberProfiles").whereEqualTo("approvalStatus", "APPROVED")
        }
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "observeApprovedBarbers error", err)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snap?.documents
                ?.mapNotNull { it.toObject(BarberProfile::class.java) }
                ?.sortedByDescending { it.ratingAvg }
                .orEmpty()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    fun observeOwnBarberProfile(uid: String): Flow<BarberProfile?> = callbackFlow {
        val reg = db.collection("barberProfiles").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeOwnBarberProfile error", err)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snap?.toObject(BarberProfile::class.java))
            }
        awaitClose { reg.remove() }
    }

    fun observePlatformConfig(): Flow<PlatformConfig> = callbackFlow {
        val reg = db.collection("platformConfig").document("global")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) {
                    trySend(PlatformConfig())
                    return@addSnapshotListener
                }
                trySend(snap.toObject(PlatformConfig::class.java) ?: PlatformConfig())
            }
        awaitClose { reg.remove() }
    }

    fun observeCustomerAddresses(uid: String): Flow<List<CustomerAddress>> = callbackFlow {
        val reg = db.collection("users").document(uid).collection("addresses")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeCustomerAddresses error", err)
                    close(err)
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(CustomerAddress::class.java) }
                    ?.sortedByDescending { it.isDefault }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeCustomerBookings(uid: String): Flow<List<Booking>> = callbackFlow {
        val reg = db.collection("bookings")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeCustomerBookings error", err)
                    close(err)
                    return@addSnapshotListener
                }
                val items = snap?.documents
                    ?.mapNotNull { it.toObject(Booking::class.java) }
                    ?.sortedWith(compareByDescending<Booking> { it.dateKey }.thenByDescending { it.startHour })
                    .orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    fun observeBarberBookings(barberId: String): Flow<List<Booking>> = callbackFlow {
        val reg = db.collection("bookings")
            .whereEqualTo("barberId", barberId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeBarberBookings error", err)
                    close(err)
                    return@addSnapshotListener
                }
                val items = snap?.documents
                    ?.mapNotNull { it.toObject(Booking::class.java) }
                    ?.sortedWith(compareBy<Booking> { it.dateKey }.thenBy { it.startHour })
                    .orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    /**
     * Real-time stream of UNASSIGNED open customer booking requests so Barbers can pick & claim
     * requests matching their specialization, gender preference, and distance.
     */
    fun observeOpenDispatchBookings(): Flow<List<Booking>> = callbackFlow {
        val reg = db.collection("bookings")
            .whereEqualTo("barberId", UNASSIGNED_BARBER_ID)
            .whereEqualTo("status", "AWAITING_BARBER")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeOpenDispatchBookings error", err)
                    close(err)
                    return@addSnapshotListener
                }
                val items = snap?.documents
                    ?.mapNotNull { it.toObject(Booking::class.java) }
                    ?.sortedWith(
                        compareByDescending<Booking> { it.bookingMode == "ASAP" }
                            .thenBy { it.dateKey }
                            .thenBy { it.startHour }
                    )
                    .orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    fun observeAllBookingsForAdmin(): Flow<List<Booking>> = callbackFlow {
        val reg = db.collection("bookings")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeAllBookingsForAdmin error", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snap?.documents
                    ?.mapNotNull { it.toObject(Booking::class.java) }
                    ?.sortedWith(compareByDescending<Booking> { it.dateKey }.thenByDescending { it.startHour })
                    .orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    fun observeSupportTickets(uid: String, isAdmin: Boolean): Flow<List<SupportTicket>> = callbackFlow {
        val query: Query = if (isAdmin) {
            db.collection("supportTickets")
        } else {
            db.collection("supportTickets").whereEqualTo("createdBy", uid)
        }
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "observeSupportTickets error", err)
                close(err)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { it.toObject(SupportTicket::class.java) }.orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeAuditLogs(): Flow<List<AuditLogEntry>> = callbackFlow {
        val reg = db.collection("auditLogs")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snap?.documents
                    ?.mapNotNull { it.toObject(AuditLogEntry::class.java) }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    .orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserAccount>> = callbackFlow {
        val reg = db.collection("users")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeAllUsersForAdmin error", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snap?.documents
                    ?.mapNotNull { it.toObject(UserAccount::class.java) }
                    ?.sortedBy { it.name.lowercase() }
                    .orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    fun observeAllBarbersForAdmin(): Flow<List<BarberProfile>> = callbackFlow {
        val reg = db.collection("barberProfiles")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeAllBarbersForAdmin error", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snap?.documents
                    ?.mapNotNull { it.toObject(BarberProfile::class.java) }
                    ?.sortedWith(
                        compareBy<BarberProfile> { it.approvalStatus != "PENDING" }
                            .thenBy { it.name.lowercase() }
                    )
                    .orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    // --- Customer Saved Door Addresses ---

    suspend fun saveCustomerAddress(
        label: String,
        flatHouse: String,
        streetArea: String,
        landmark: String,
        postalCode: String,
        serviceArea: ServiceArea?,
        accessInstructions: String,
        isDefault: Boolean,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ): Result<String> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        val docId = "addr_${UUID.randomUUID().toString().take(8)}"
        val payload = mapOf(
            "customerId" to uid,
            "label" to label.takeIf { it in setOf("HOME", "OFFICE", "OTHER") }.orEmpty().ifBlank { "HOME" },
            "flatHouse" to flatHouse.trim().take(140),
            "streetArea" to streetArea.trim().take(200),
            "landmark" to landmark.trim().take(150),
            "postalCode" to postalCode.trim().take(16),
            "serviceAreaId" to (serviceArea?.id ?: ""),
            "serviceAreaName" to (serviceArea?.name ?: streetArea.trim().take(60)),
            "accessInstructions" to accessInstructions.trim().take(300),
            "isDefault" to isDefault,
            "latitude" to latitude.coerceIn(-90.0, 90.0),
            "longitude" to longitude.coerceIn(-180.0, 180.0),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("addresses").document(docId).set(payload).await()
        docId
    }

    suspend fun deleteCustomerAddress(addressId: String): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        db.collection("users").document(uid).collection("addresses").document(addressId).delete().await()
    }

    // --- Uber / Rapido Open-Dispatch Booking Creation ---

    suspend fun createOpenDispatchBooking(
        customerName: String,
        customerPhone: String,
        customerGender: String,
        customerAge: Long,
        preferLadyBarber: Boolean,
        bookingMode: String,
        selectedServices: List<CatalogService>,
        address: CustomerAddress,
        serviceArea: ServiceArea?,
        dateKey: String,
        startHour: Long,
        customerNotes: String,
        bookingFeeTxRef: String,
        config: PlatformConfig,
        preallocatedBookingId: String? = null
    ): Result<String> = runCatching {
        val uid = currentUid() ?: error("Please sign in to confirm your booking")
        require(selectedServices.isNotEmpty()) { "Select at least one grooming service" }
        require(customerPhone.isNotBlank()) { "Phone number is required so your appointed barber can coordinate arrival" }

        val bookingId = preallocatedBookingId?.takeIf { it.isNotBlank() }
            ?: "bk_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val bookingNum = "DC-${(1000..9999).random()}"

        // Cross-check authoritative service prices & durations from Firestore when seeded
        val resolvedServices = selectedServices.take(15).map { clientSvc ->
            val snap = runCatching { db.collection("services").document(clientSvc.id).get().await() }.getOrNull()
            if (snap != null && snap.exists()) {
                val active = snap.getBoolean("isActive") ?: true
                require(active) { "Service '${clientSvc.name}' is currently inactive." }
                clientSvc.copy(
                    name = snap.getString("name")?.takeIf { it.isNotBlank() } ?: clientSvc.name,
                    pricePaise = snap.getLong("pricePaise") ?: clientSvc.pricePaise,
                    durationMinutes = snap.getLong("durationMinutes") ?: clientSvc.durationMinutes
                )
            } else {
                clientSvc
            }
        }

        val authoritativeConfig = runCatching {
            db.collection("platformConfig").document("global").get().await()
                .toObject(PlatformConfig::class.java)
        }.getOrNull() ?: config

        val subtotalPaise = resolvedServices.sumOf { it.pricePaise }
        val travelPaise = serviceArea?.travelChargePaise ?: 0L
        val barberPayablePaise = subtotalPaise + travelPaise
        val customerFeePaise = authoritativeConfig.computeCustomerConfirmationFeePaise(subtotalPaise)
        val totalCustomerCostPaise = barberPayablePaise + customerFeePaise
        val barberCommissionPaise = authoritativeConfig.computeBarberCommissionPaise(barberPayablePaise)
        val barberNetPaise = (barberPayablePaise - barberCommissionPaise).coerceAtLeast(0L)
        val totalDuration = resolvedServices.sumOf { it.durationMinutes }.coerceIn(10L, 480L)
        val summary = resolvedServices.joinToString(", ") {
            "${it.name} (${CurrencyUtils.formatRupees(it.pricePaise)})"
        }.take(490)

        // Zero-trust fee status: NEVER spoof PAID_VERIFIED on client unless server/Admin receipt exists
        val resolvedFeeStatus = if (customerFeePaise <= 0L) {
            "ZERO_CUSTOMER_FEE"
        } else {
            val verifiedSnap = runCatching {
                db.collection("verifiedPayments").document(bookingId).get().await()
            }.getOrNull()
            val verifiedAmount = verifiedSnap?.getLong("amountPaise") ?: 0L
            if (verifiedSnap != null && verifiedSnap.exists() && verifiedAmount >= customerFeePaise) {
                "PAID_VERIFIED"
            } else {
                "UNPAID"
            }
        }

        val cleanGender = customerGender.uppercase().takeIf {
            it in setOf("MALE", "FEMALE", "CHILD", "OTHER")
        } ?: "MALE"

        val dispatchLabel = if (preferLadyBarber) {
            "Matching Verified Lady Stylist"
        } else {
            "Matching Nearby Verified Barber"
        }

        val bookingRef = db.collection("bookings").document(bookingId)
        val privRef = bookingRef.collection("privateDetails").document("main")

        val bookingPayload = mapOf(
            "bookingNumber" to bookingNum,
            "customerId" to uid,
            "customerName" to customerName.trim().ifBlank { currentDisplayName() }.take(100),
            "customerPhone" to customerPhone.trim().take(30),
            "customerGender" to cleanGender,
            "customerAge" to customerAge.coerceIn(0L, 120L),
            "preferLadyBarber" to preferLadyBarber,
            "bookingMode" to if (bookingMode == "SCHEDULED") "SCHEDULED" else "ASAP",
            "customerLat" to address.latitude.coerceIn(-90.0, 90.0),
            "customerLon" to address.longitude.coerceIn(-180.0, 180.0),
            "barberId" to UNASSIGNED_BARBER_ID,
            "barberName" to dispatchLabel,
            "barberPhone" to "",
            "barberLat" to 0.0,
            "barberLon" to 0.0,
            "status" to "AWAITING_BARBER",
            "dateKey" to dateKey,
            "startHour" to startHour.coerceIn(0L, 23L),
            "durationMinutes" to totalDuration,
            "serviceIds" to resolvedServices.map { it.id }.take(15),
            "serviceNamesSummary" to summary,
            "areaName" to address.serviceAreaName.ifBlank { address.streetArea }.take(120),
            "serviceSubtotalPaise" to subtotalPaise,
            "approvedAddOnPaise" to 0L,
            "travelChargePaise" to travelPaise,
            "barberServicePayablePaise" to barberPayablePaise,
            "platformBookingFeePaise" to customerFeePaise,
            "totalCustomerCostPaise" to totalCustomerCostPaise,
            "bookingFeeStatus" to resolvedFeeStatus,
            "bookingFeeTxRef" to bookingFeeTxRef.trim().ifBlank { "DISPATCH-${bookingNum}" }.take(120),
            "barberCommissionPaise" to barberCommissionPaise,
            "barberNetEarningsPaise" to barberNetPaise,
            "barberCommissionStatus" to "PENDING_SETTLEMENT",
            "directPaymentStatus" to "PENDING",
            "directPaymentMethod" to "NONE",
            "directPaymentDeclaredPaise" to 0L,
            "directPaymentRef" to "",
            "pendingAddOnServiceId" to "",
            "pendingAddOnName" to "",
            "pendingAddOnPricePaise" to 0L,
            "pendingAddOnDurationMin" to 0L,
            "pendingAddOnStatus" to "NONE",
            "customerNotes" to customerNotes.trim().take(500),
            "cancellationReason" to "",
            "policySnapshot" to authoritativeConfig.policyText.take(500),
            "hasReview" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val privPayload = mapOf(
            "bookingId" to bookingId,
            "customerId" to uid,
            "barberId" to UNASSIGNED_BARBER_ID,
            "fullAddressSnapshot" to address.fullSummary().ifBlank { address.flatHouse }.take(500),
            "accessInstructions" to address.accessInstructions.take(300),
            "customerPhone" to customerPhone.trim().take(30),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(bookingRef, bookingPayload)
        batch.set(privRef, privPayload)
        batch.commit().await()
        bookingId
    }

    /**
     * Computes deterministic hour slots occupied by a booking including the barber's travel buffer.
     */
    fun computeOccupiedHours(startHour: Long, durationMinutes: Long, travelBufferMinutes: Long): List<Long> {
        val totalMinutes = (durationMinutes.coerceAtLeast(15L) + travelBufferMinutes.coerceAtLeast(0L))
        val hoursNeeded = ((totalMinutes + 59L) / 60L).coerceIn(1L, 10L)
        return (0L until hoursNeeded)
            .map { (startHour + it).coerceIn(0L, 23L) }
            .distinct()
    }

    /**
     * Atomic transaction allowing an approved Barber to claim an UNASSIGNED customer booking
     * from the Open Dispatch Pool while atomically locking `slotLocks/{barberId}_{dateKey}_{hour}`
     * across service duration + travel buffer (`MarioMahir/BarberFlow` double-booking prevention).
     */
    suspend fun barberClaimOpenBooking(
        bookingId: String,
        barberProfile: BarberProfile,
        liveLat: Double,
        liveLon: Double
    ): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        val bookingRef = db.collection("bookings").document(bookingId)
        val startLat = if (liveLat != 0.0) liveLat else barberProfile.latitude
        val startLon = if (liveLon != 0.0) liveLon else barberProfile.longitude

        db.runTransaction { tx ->
            val snap = tx.get(bookingRef)
            if (!snap.exists()) {
                throw FirebaseFirestoreException(
                    "Booking no longer exists.",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )
            }
            val currentBarber = snap.getString("barberId").orEmpty()
            val currentStatus = snap.getString("status").orEmpty()
            if (currentBarber != UNASSIGNED_BARBER_ID || currentStatus != "AWAITING_BARBER") {
                throw FirebaseFirestoreException(
                    "Another barber just accepted this request.",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }

            val dateKey = snap.getString("dateKey").orEmpty()
            val startHour = snap.getLong("startHour") ?: 10L
            val durationMinutes = snap.getLong("durationMinutes") ?: 30L
            val bookingMode = snap.getString("bookingMode").orEmpty()
            val customerId = snap.getString("customerId").orEmpty()

            val occupiedHours = computeOccupiedHours(
                startHour = startHour,
                durationMinutes = durationMinutes,
                travelBufferMinutes = barberProfile.travelBufferMinutes
            )

            // Validate working hours and break window for scheduled slots
            if (bookingMode == "SCHEDULED") {
                val endExclusive = startHour + occupiedHours.size
                if (startHour < barberProfile.workStartHour || endExclusive > barberProfile.workEndHour) {
                    throw FirebaseFirestoreException(
                        "Slot (${startHour}:00–${endExclusive}:00) falls outside your working hours (${barberProfile.workStartHour}:00–${barberProfile.workEndHour}:00).",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    )
                }
                if (barberProfile.breakEndHour > barberProfile.breakStartHour) {
                    val hitsBreak = occupiedHours.any { hr ->
                        hr >= barberProfile.breakStartHour && hr < barberProfile.breakEndHour
                    }
                    if (hitsBreak) {
                        throw FirebaseFirestoreException(
                            "Slot overlaps your scheduled break (${barberProfile.breakStartHour}:00–${barberProfile.breakEndHour}:00).",
                            FirebaseFirestoreException.Code.FAILED_PRECONDITION
                        )
                    }
                }
            }

            // Read ALL slotLocks BEFORE any write (Firestore transaction invariant)
            val lockRefsAndSnaps = occupiedHours.map { hour ->
                val lockRef = db.collection("slotLocks").document("${uid}_${dateKey}_${hour}")
                Triple(hour, lockRef, tx.get(lockRef))
            }

            for ((hour, _, lockSnap) in lockRefsAndSnaps) {
                if (lockSnap.exists()) {
                    val lockStatus = lockSnap.getString("status").orEmpty()
                    val lockBookingId = lockSnap.getString("bookingId").orEmpty()
                    if (lockStatus in setOf("HELD", "CONFIRMED") && lockBookingId != bookingId) {
                        throw FirebaseFirestoreException(
                            "Schedule conflict at ${hour}:00 on $dateKey (includes ${barberProfile.travelBufferMinutes}m travel buffer).",
                            FirebaseFirestoreException.Code.ABORTED
                        )
                    }
                }
            }

            // Perform atomic writes: claim booking + confirm slotLocks
            tx.update(
                bookingRef,
                mapOf(
                    "barberId" to uid,
                    "barberName" to barberProfile.name.ifBlank { currentDisplayName() }.take(100),
                    "barberPhone" to barberProfile.phone.take(30),
                    "barberLat" to startLat.coerceIn(-90.0, 90.0),
                    "barberLon" to startLon.coerceIn(-180.0, 180.0),
                    "status" to "CONFIRMED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            for ((hour, lockRef, lockSnap) in lockRefsAndSnaps) {
                if (lockSnap.exists()) {
                    tx.update(
                        lockRef,
                        mapOf(
                            "bookingId" to bookingId,
                            "customerId" to customerId,
                            "status" to "CONFIRMED",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        lockRef,
                        mapOf(
                            "barberId" to uid,
                            "dateKey" to dateKey,
                            "hourSlot" to hour,
                            "bookingId" to bookingId,
                            "customerId" to customerId,
                            "status" to "CONFIRMED",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }.await()
    }

    suspend fun barberUpdateLiveLocation(
        bookingId: String,
        latitude: Double,
        longitude: Double
    ): Result<Unit> = runCatching {
        if (latitude == 0.0 && longitude == 0.0) return@runCatching
        db.collection("bookings").document(bookingId).update(
            mapOf(
                "barberLat" to latitude.coerceIn(-90.0, 90.0),
                "barberLon" to longitude.coerceIn(-180.0, 180.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun fetchProtectedBookingAddress(bookingId: String): Result<BookingPrivateAddress?> = runCatching {
        val snap = db.collection("bookings").document(bookingId)
            .collection("privateDetails").document("main").get().await()
        snap.toObject(BookingPrivateAddress::class.java)
    }

    // --- Lifecycle State Transitions ---

    private suspend fun releaseBookingSlotLocks(booking: Booking) {
        if (booking.barberId.isBlank() || booking.barberId == UNASSIGNED_BARBER_ID || booking.dateKey.length != 10) return
        val candidateHours = computeOccupiedHours(
            startHour = booking.startHour,
            durationMinutes = booking.durationMinutes,
            travelBufferMinutes = 180L
        )
        for (hour in candidateHours) {
            val lockRef = db.collection("slotLocks").document("${booking.barberId}_${booking.dateKey}_${hour}")
            runCatching {
                val snap = lockRef.get().await()
                if (snap.exists() && snap.getString("bookingId") == booking.id && snap.getString("status") != "RELEASED") {
                    lockRef.update(
                        mapOf(
                            "status" to "RELEASED",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        }
    }

    suspend fun cancelBooking(
        booking: Booking,
        reason: String
    ): Result<Unit> = runCatching {
        val bookingRef = db.collection("bookings").document(booking.id)
        bookingRef.update(
            mapOf(
                "status" to "CANCELLED",
                "cancellationReason" to reason.trim().ifBlank { "Cancelled by customer" }.take(300),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        releaseBookingSlotLocks(booking)
    }

    suspend fun barberRespondToBooking(booking: Booking, accept: Boolean, reason: String = ""): Result<Unit> = runCatching {
        val bookingRef = db.collection("bookings").document(booking.id)
        if (accept) {
            bookingRef.update(
                mapOf(
                    "status" to "CONFIRMED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            bookingRef.update(
                mapOf(
                    "status" to "CANCELLED",
                    "cancellationReason" to reason.ifBlank { "Barber unavailable — please rebook" }.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            releaseBookingSlotLocks(booking)
        }
    }

    suspend fun barberUpdateTripStatus(
        bookingId: String,
        nextStatus: String,
        liveLat: Double = 0.0,
        liveLon: Double = 0.0
    ): Result<Unit> = runCatching {
        require(nextStatus in setOf("ON_THE_WAY", "ARRIVED", "IN_PROGRESS", "COMPLETED", "NO_SHOW"))
        val updates = mutableMapOf<String, Any>(
            "status" to nextStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (liveLat != 0.0 || liveLon != 0.0) {
            updates["barberLat"] = liveLat.coerceIn(-90.0, 90.0)
            updates["barberLon"] = liveLon.coerceIn(-180.0, 180.0)
        }
        db.collection("bookings").document(bookingId).update(updates).await()
    }

    suspend fun barberRequestExtraService(
        bookingId: String,
        extraService: CatalogService
    ): Result<Unit> = runCatching {
        db.collection("bookings").document(bookingId).update(
            mapOf(
                "pendingAddOnServiceId" to extraService.id,
                "pendingAddOnName" to extraService.name.take(100),
                "pendingAddOnPricePaise" to extraService.pricePaise,
                "pendingAddOnDurationMin" to extraService.durationMinutes,
                "pendingAddOnStatus" to "REQUESTED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun customerRespondToAddOn(
        booking: Booking,
        approve: Boolean,
        config: PlatformConfig = PlatformConfig()
    ): Result<Unit> = runCatching {
        val bookingRef = db.collection("bookings").document(booking.id)
        if (approve && booking.pendingAddOnStatus == "REQUESTED") {
            val newAddOnTotal = booking.approvedAddOnPaise + booking.pendingAddOnPricePaise
            val newBarberPayable = booking.serviceSubtotalPaise + newAddOnTotal + booking.travelChargePaise
            val newTotalCustomerCost = newBarberPayable + booking.platformBookingFeePaise
            val newBarberCommission = config.computeBarberCommissionPaise(newBarberPayable)
            val newBarberNet = (newBarberPayable - newBarberCommission).coerceAtLeast(0L)
            val updatedSummary = "${booking.serviceNamesSummary} + ${booking.pendingAddOnName} (${CurrencyUtils.formatRupees(booking.pendingAddOnPricePaise)})".take(490)
            bookingRef.update(
                mapOf(
                    "approvedAddOnPaise" to newAddOnTotal,
                    "barberServicePayablePaise" to newBarberPayable,
                    "totalCustomerCostPaise" to newTotalCustomerCost,
                    "barberCommissionPaise" to newBarberCommission,
                    "barberNetEarningsPaise" to newBarberNet,
                    "durationMinutes" to (booking.durationMinutes + booking.pendingAddOnDurationMin).coerceAtMost(600L),
                    "serviceNamesSummary" to updatedSummary,
                    "pendingAddOnStatus" to "APPROVED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            bookingRef.update(
                mapOf(
                    "pendingAddOnStatus" to "DECLINED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun barberDeclareDirectPayment(
        bookingId: String,
        declaredStatus: String,
        method: String,
        amountPaise: Long,
        referenceNote: String,
        markCompleted: Boolean
    ): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        val updates = mutableMapOf<String, Any>(
            "directPaymentStatus" to declaredStatus,
            "directPaymentMethod" to method,
            "directPaymentDeclaredPaise" to amountPaise.coerceAtLeast(0L),
            "directPaymentRef" to referenceNote.trim().take(120),
            "directPaymentRecordedBy" to uid,
            "directPaymentRecordedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (markCompleted) {
            updates["status"] = "COMPLETED"
        } else if (declaredStatus == "DISPUTED") {
            updates["status"] = "DISPUTED"
        }
        db.collection("bookings").document(bookingId).update(updates).await()
    }

    suspend fun customerConfirmOrDisputeDirectPayment(
        bookingId: String,
        confirmed: Boolean
    ): Result<Unit> = runCatching {
        val updates = if (confirmed) {
            mapOf(
                "directPaymentStatus" to "CUSTOMER_CONFIRMED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "directPaymentStatus" to "DISPUTED",
                "status" to "DISPUTED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        db.collection("bookings").document(bookingId).update(updates).await()
    }

    suspend fun submitReview(
        booking: Booking,
        rating: Int,
        comment: String
    ): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        val reviewId = "rev_${booking.id}"
        val batch = db.batch()
        batch.set(
            db.collection("reviews").document(reviewId),
            mapOf(
                "bookingId" to booking.id,
                "customerId" to uid,
                "customerName" to booking.customerName,
                "barberId" to booking.barberId,
                "rating" to rating.coerceIn(1, 5).toLong(),
                "comment" to comment.trim().take(600),
                "status" to "PUBLISHED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("bookings").document(booking.id),
            mapOf(
                "hasReview" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
    }

    // --- Barber Onboarding & Profile Management ---

    suspend fun applyOrSaveBarberProfile(
        name: String,
        phone: String,
        gender: String = "MALE",
        servesLadies: Boolean = false,
        servesMen: Boolean = true,
        bio: String,
        experienceYears: Long,
        serviceIds: List<String>,
        serviceAreaIds: List<String>,
        workStartHour: Long,
        workEndHour: Long,
        upiId: String,
        shopName: String = "",
        shopAddress: String = "",
        skills: List<String> = emptyList(),
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        workingDays: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
        breakStartHour: Long = 13L,
        breakEndHour: Long = 14L,
        travelBufferMinutes: Long = 15L
    ): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        val profileRef = db.collection("barberProfiles").document(uid)
        val existingSnap = profileRef.get().await()
        val effectiveSkills = skills.ifEmpty {
            buildList {
                if (servesLadies) add("Lady Hair Stylist")
                if (servesMen) add("Precision Haircut")
                add("Sanitized Home Kit")
            }
        }.take(15)
        val cleanGender = gender.uppercase().takeIf { it in setOf("MALE", "FEMALE", "OTHER") } ?: "MALE"
        val cleanWorkingDays = workingDays
            .filter { it in setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN") }
            .distinct()
            .ifEmpty { listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN") }
            .take(7)
        val badgeLabel = if (servesLadies && cleanGender == "FEMALE") {
            "Verified Lady Hair Stylist"
        } else {
            "Verified DoorCut Partner"
        }

        if (!existingSnap.exists()) {
            val fallbackLoc = LocationHelper.resolveBestFallbackLocationForText(shopAddress, "")
            val initLat = if (latitude != 0.0) latitude else fallbackLoc.latitude
            val initLon = if (longitude != 0.0) longitude else fallbackLoc.longitude
            profileRef.set(
                mapOf(
                    "barberId" to uid,
                    "name" to name.trim().take(100),
                    "phone" to phone.trim().take(30),
                    "gender" to cleanGender,
                    "servesLadies" to servesLadies,
                    "servesMen" to servesMen,
                    "bio" to bio.trim().take(500),
                    "experienceYears" to experienceYears.coerceIn(0L, 60L),
                    "approvalStatus" to "APPROVED",
                    "isOnline" to true,
                    "ratingAvg" to 5.0,
                    "reviewCount" to 0L,
                    "serviceIds" to serviceIds.take(50),
                    "serviceAreaIds" to serviceAreaIds.take(20),
                    "workingDays" to cleanWorkingDays,
                    "workStartHour" to workStartHour.coerceIn(0L, 23L),
                    "workEndHour" to workEndHour.coerceIn(1L, 24L),
                    "breakStartHour" to breakStartHour.coerceIn(0L, 23L),
                    "breakEndHour" to breakEndHour.coerceIn(0L, 24L),
                    "travelBufferMinutes" to travelBufferMinutes.coerceIn(0L, 180L),
                    "upiId" to upiId.trim().take(100),
                    "skills" to effectiveSkills,
                    "perfectionBadge" to badgeLabel,
                    "shopName" to shopName.trim().ifBlank { "${name.trim()} Home Studio" }.take(120),
                    "shopAddress" to shopAddress.trim().ifBlank { fallbackLoc.streetArea }.take(240),
                    "homeVisitAvailable" to true,
                    "latitude" to initLat.coerceIn(-90.0, 90.0),
                    "longitude" to initLon.coerceIn(-180.0, 180.0),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val updates = mutableMapOf<String, Any>(
                "name" to name.trim().take(100),
                "phone" to phone.trim().take(30),
                "gender" to cleanGender,
                "servesLadies" to servesLadies,
                "servesMen" to servesMen,
                "bio" to bio.trim().take(500),
                "experienceYears" to experienceYears.coerceIn(0L, 60L),
                "serviceIds" to serviceIds.take(50),
                "serviceAreaIds" to serviceAreaIds.take(20),
                "workingDays" to cleanWorkingDays,
                "workStartHour" to workStartHour.coerceIn(0L, 23L),
                "workEndHour" to workEndHour.coerceIn(1L, 24L),
                "breakStartHour" to breakStartHour.coerceIn(0L, 23L),
                "breakEndHour" to breakEndHour.coerceIn(0L, 24L),
                "travelBufferMinutes" to travelBufferMinutes.coerceIn(0L, 180L),
                "upiId" to upiId.trim().take(100),
                "skills" to effectiveSkills,
                "perfectionBadge" to badgeLabel,
                "shopName" to shopName.trim().ifBlank { "${name.trim()} Home Studio" }.take(120),
                "shopAddress" to shopAddress.trim().ifBlank { "Active Home Dispatch Zone" }.take(240),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (latitude != 0.0 || longitude != 0.0) {
                updates["latitude"] = latitude.coerceIn(-90.0, 90.0)
                updates["longitude"] = longitude.coerceIn(-180.0, 180.0)
            }
            profileRef.update(updates).await()
        }
    }

    suspend fun toggleBarberOnline(isOnline: Boolean, liveLat: Double = 0.0, liveLon: Double = 0.0): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        val updates = mutableMapOf<String, Any>(
            "isOnline" to isOnline,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (liveLat != 0.0 || liveLon != 0.0) {
            updates["latitude"] = liveLat.coerceIn(-90.0, 90.0)
            updates["longitude"] = liveLon.coerceIn(-180.0, 180.0)
        }
        db.collection("barberProfiles").document(uid).update(updates).await()
    }

    suspend fun updateBarberProfileLocation(
        latitude: Double,
        longitude: Double,
        shopAddress: String? = null
    ): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        if (latitude == 0.0 && longitude == 0.0) return@runCatching
        val updates = mutableMapOf<String, Any>(
            "latitude" to latitude.coerceIn(-90.0, 90.0),
            "longitude" to longitude.coerceIn(-180.0, 180.0),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!shopAddress.isNullOrBlank()) {
            updates["shopAddress"] = shopAddress.trim().take(240)
        }
        db.collection("barberProfiles").document(uid).update(updates).await()
    }

    // --- Support Tickets ---

    suspend fun createSupportTicket(
        creatorRole: String,
        bookingId: String,
        category: String,
        subject: String,
        description: String
    ): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        val ticketId = "tkt_${UUID.randomUUID().toString().take(8)}"
        db.collection("supportTickets").document(ticketId).set(
            mapOf(
                "createdBy" to uid,
                "creatorRole" to if (creatorRole == "BARBER") "BARBER" else "CUSTOMER",
                "bookingId" to bookingId,
                "category" to category,
                "subject" to subject.trim().take(150),
                "description" to description.trim().take(1000),
                "status" to "OPEN",
                "adminResolution" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // --- Admin Actions + Mandatory Audit Logging ---

    private suspend fun recordAudit(action: String, entityType: String, entityId: String, reason: String) {
        val uid = currentUid() ?: return
        val logId = "aud_${UUID.randomUUID().toString().take(10)}"
        db.collection("auditLogs").document(logId).set(
            mapOf(
                "actorId" to uid,
                "action" to action.take(100),
                "entityType" to entityType.take(60),
                "entityId" to entityId.take(128),
                "reason" to reason.ifBlank { "Admin operation" }.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun adminSetBarberApproval(
        barberId: String,
        newStatus: String,
        reason: String
    ): Result<Unit> = runCatching {
        db.collection("barberProfiles").document(barberId).update(
            mapOf(
                "approvalStatus" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        runCatching {
            val userDoc = db.collection("users").document(barberId).get().await()
            if (userDoc.exists()) {
                val mappedUserStatus = if (newStatus == "APPROVED") "ACTIVE" else newStatus
                db.collection("users").document(barberId).update(
                    mapOf("status" to mappedUserStatus, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
        }
        recordAudit("SET_BARBER_$newStatus", "BarberProfile", barberId, reason)
    }

    suspend fun adminUpsertService(
        id: String?,
        name: String,
        category: String,
        description: String,
        priceRupees: Long,
        durationMinutes: Long,
        isPackage: Boolean,
        isActive: Boolean
    ): Result<Unit> = runCatching {
        val docId = id?.takeIf { it.isNotBlank() } ?: "svc_${UUID.randomUUID().toString().take(8)}"
        val ref = db.collection("services").document(docId)
        val snap = ref.get().await()
        val payload = mutableMapOf<String, Any>(
            "name" to name.trim().take(100),
            "category" to category,
            "description" to description.trim().take(500),
            "pricePaise" to (priceRupees * 100L).coerceAtLeast(0L),
            "durationMinutes" to durationMinutes.coerceIn(5L, 480L),
            "isActive" to isActive,
            "isPackage" to isPackage,
            "instructions" to "Keep a chair ready near good lighting and a power socket.",
            "sortOrder" to 10L,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!snap.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            ref.set(payload).await()
        } else {
            payload["createdAt"] = snap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            ref.set(payload).await()
        }
        recordAudit("UPSERT_SERVICE", "Service", docId, "Updated catalog item $name")
    }

    suspend fun adminUpsertServiceArea(
        id: String?,
        name: String,
        postalCodesCsv: String,
        travelChargeRupees: Long,
        maxDistanceKm: Long,
        isActive: Boolean
    ): Result<Unit> = runCatching {
        val docId = id?.takeIf { it.isNotBlank() } ?: "zone_${UUID.randomUUID().toString().take(6)}"
        val pins = postalCodesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(30)
        val ref = db.collection("serviceAreas").document(docId)
        val snap = ref.get().await()
        val payload = mutableMapOf<String, Any>(
            "name" to name.trim().take(120),
            "postalCodes" to pins,
            "travelChargePaise" to (travelChargeRupees * 100L).coerceAtLeast(0L),
            "maxDistanceKm" to maxDistanceKm.coerceIn(1L, 100L),
            "isActive" to isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!snap.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            ref.set(payload).await()
        } else {
            payload["createdAt"] = snap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            ref.set(payload).await()
        }
        recordAudit("UPSERT_SERVICE_AREA", "ServiceArea", docId, "Updated zone $name")
    }

    suspend fun adminOverrideBooking(
        booking: Booking,
        newStatus: String,
        newBarber: BarberProfile?,
        markBarberCommissionSettled: Boolean,
        reason: String
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "cancellationReason" to reason.trim().take(300),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (newBarber != null) {
            updates["barberId"] = newBarber.barberId
            updates["barberName"] = newBarber.name
            updates["barberPhone"] = newBarber.phone
        }
        if (markBarberCommissionSettled) {
            updates["barberCommissionStatus"] = "SETTLED"
        }
        db.collection("bookings").document(booking.id).update(updates).await()
        if (newBarber != null) {
            runCatching {
                db.collection("bookings").document(booking.id)
                    .collection("privateDetails").document("main")
                    .update(mapOf("barberId" to newBarber.barberId, "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
            }
        }
        recordAudit("BOOKING_OVERRIDE_$newStatus", "Booking", booking.id, reason)
    }

    suspend fun adminResolveTicket(ticketId: String, resolution: String): Result<Unit> = runCatching {
        db.collection("supportTickets").document(ticketId).update(
            mapOf(
                "status" to "RESOLVED",
                "adminResolution" to resolution.trim().take(1000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        recordAudit("RESOLVE_TICKET", "SupportTicket", ticketId, resolution)
    }

    suspend fun adminSavePlatformConfig(
        feeType: String,
        barberCommissionPercent: Long,
        barberFlatFeeRupees: Long,
        cutoffHours: Long,
        policyText: String
    ): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        db.collection("platformConfig").document("global").set(
            mapOf(
                "bookingFeeType" to feeType,
                "fixedBookingFeePaise" to 2900L,
                "percentageBookingFeeBps" to 0L,
                "barberCommissionBps" to (barberCommissionPercent * 100L).coerceIn(0L, 5000L),
                "barberFlatFeePaise" to (barberFlatFeeRupees * 100L).coerceIn(0L, 500000L),
                "freeCancellationCutoffHours" to cutoffHours.coerceIn(0L, 72L),
                "slotHoldMinutes" to 10L,
                "policyText" to policyText.trim().take(1000),
                "maintenanceMode" to false,
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        recordAudit("UPDATE_PLATFORM_CONFIG", "PlatformConfig", "global", "Updated commission & cancellation rules")
    }

    suspend fun adminSetUserStatus(
        userId: String,
        newStatus: String,
        reason: String
    ): Result<Unit> = runCatching {
        require(newStatus in setOf("ACTIVE", "PENDING", "SUSPENDED", "REJECTED"))
        db.collection("users").document(userId).update(
            mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        recordAudit("SET_USER_$newStatus", "User", userId, reason.ifBlank { "Admin account status update" })
    }

    suspend fun adminVerifyUpfrontBookingFee(
        booking: Booking,
        paymentReference: String
    ): Result<Unit> = runCatching {
        val uid = currentUid() ?: error("Not signed in")
        val cleanRef = paymentReference.trim().ifBlank { "ADM-VERIFIED-${booking.bookingNumber}" }.take(120)
        val amount = booking.platformBookingFeePaise.coerceAtLeast(100L)
        val batch = db.batch()
        batch.set(
            db.collection("verifiedPayments").document(booking.id),
            mapOf(
                "bookingId" to booking.id,
                "customerId" to booking.customerId,
                "amountPaise" to amount,
                "provider" to "ADMIN_MANUAL",
                "orderId" to "ORD-${booking.bookingNumber}".take(120),
                "paymentId" to cleanRef,
                "verifiedBy" to uid,
                "verifiedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("bookings").document(booking.id),
            mapOf(
                "bookingFeeStatus" to "PAID_VERIFIED",
                "bookingFeeTxRef" to cleanRef,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        recordAudit("VERIFY_BOOKING_FEE", "Booking", booking.id, "Verified upfront booking fee ($cleanRef)")
    }
}
