package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AuditLog
import com.example.data.model.BarberProfile
import com.example.data.model.Booking
import com.example.data.model.BookingPrivateAddress
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.Review
import com.example.data.model.ServiceArea
import com.example.data.model.SlotLock
import com.example.data.model.SupportTicket
import com.example.data.model.UserAccount
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MarketplaceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing DoorCut.")
    }

    // --- 1. User Account & Role Bootstrap ---

    fun observeUserAccount(userId: String): Flow<UserAccount?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    suspend fun ensureUserAccount(
        userId: String,
        displayName: String,
        email: String,
        phone: String = ""
    ): UserAccount {
        val docRef = db.collection("users").document(userId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                return snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserAccount(id = userId, userId = userId, name = displayName, email = email, phone = phone)
            }
            val isOwnerAdmin = email.equals("nomaankhannfs2@gmail.com", ignoreCase = true)
            val initialRole = if (isOwnerAdmin) "ADMIN" else "CUSTOMER"
            val payload = mapOf(
                "userId" to userId,
                "name" to displayName.ifBlank { "DoorCut Member" }.take(100),
                "email" to email.take(150),
                "phone" to phone.take(20),
                "role" to initialRole,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val fresh = docRef.get().await()
            return fresh.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)!!
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateOwnProfile(name: String, phone: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "name" to name.trim().take(100),
                    "phone" to phone.trim().take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun applyToBecomeBarber(
        name: String,
        phone: String,
        bio: String,
        experienceYears: Long,
        upiId: String,
        shopName: String = "",
        shopAddress: String = "",
        skills: List<String> = listOf("Precision Haircut", "Skin Fade", "Beard Sculpting"),
        serviceIds: List<String>,
        serviceAreaIds: List<String>
    ) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val barberRef = db.collection("barberProfiles").document(uid)
        try {
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "name" to name.trim().take(100),
                    "phone" to phone.trim().take(20),
                    "role" to "BARBER",
                    "status" to "PENDING",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val barberPayload = mapOf(
                "barberId" to uid,
                "name" to name.trim().take(100),
                "bio" to bio.trim().take(500),
                "experienceYears" to experienceYears.coerceIn(0L, 60L),
                "approvalStatus" to "PENDING",
                "isOnline" to true,
                "ratingAvg" to 5.0,
                "reviewCount" to 0L,
                "serviceIds" to serviceIds.take(50),
                "serviceAreaIds" to serviceAreaIds.take(20),
                "workingDays" to listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
                "workStartHour" to 9L,
                "workEndHour" to 20L,
                "breakStartHour" to 13L,
                "breakEndHour" to 14L,
                "travelBufferMinutes" to 15L,
                "upiId" to upiId.trim().take(100),
                "skills" to skills.map { it.trim().take(40) }.filter { it.isNotBlank() }.take(15),
                "perfectionBadge" to "Verified Home Specialist",
                "shopName" to shopName.trim().ifBlank { "${name.trim().substringBefore(" ")}'s Barber Studio" }.take(120),
                "shopAddress" to shopAddress.trim().take(240),
                "homeVisitAvailable" to true,
                "latitude" to 0.0,
                "longitude" to 0.0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(barberRef, barberPayload)
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, barberRef.path)
            throw e
        }
    }

    // --- 2. Customer Saved Door / Home Addresses ---

    fun observeCustomerAddresses(customerId: String): Flow<List<CustomerAddress>> {
        val path = "users/$customerId/addresses"
        return db.collection("users").document(customerId).collection("addresses")
            .snapshots()
            .map { snap -> snap.toObjects(CustomerAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun saveCustomerAddress(
        label: String,
        flatHouse: String,
        streetArea: String,
        landmark: String,
        postalCode: String,
        serviceAreaId: String,
        serviceAreaName: String,
        accessInstructions: String,
        isDefault: Boolean,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("addresses")
        val docRef = colRef.document(UUID.randomUUID().toString())
        try {
            val payload = mapOf(
                "customerId" to uid,
                "label" to label,
                "flatHouse" to flatHouse.trim().take(120),
                "streetArea" to streetArea.trim().take(200),
                "landmark" to landmark.trim().take(150),
                "postalCode" to postalCode.trim().ifBlank { "560038" }.take(12),
                "serviceAreaId" to serviceAreaId.take(128),
                "serviceAreaName" to serviceAreaName.take(120),
                "accessInstructions" to accessInstructions.trim().take(300),
                "isDefault" to isDefault,
                "latitude" to latitude.coerceIn(-90.0, 90.0),
                "longitude" to longitude.coerceIn(-180.0, 180.0),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun deleteCustomerAddress(addressId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("addresses").document(addressId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 3. Catalog Services, Service Areas & Platform Config ---

    fun observeActiveServices(): Flow<List<CatalogService>> {
        val path = "services"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(CatalogService::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAllServicesForAdmin(): Flow<List<CatalogService>> {
        val path = "services"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.toObjects(CatalogService::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeActiveServiceAreas(): Flow<List<ServiceArea>> {
        val path = "serviceAreas"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap -> snap.toObjects(ServiceArea::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observePlatformConfig(): Flow<PlatformConfig> {
        val path = "platformConfig/global"
        return db.collection("platformConfig").document("global")
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(PlatformConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: PlatformConfig()
                } else {
                    PlatformConfig()
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    // --- 4. Barber Profiles & Schedule ---

    fun observeApprovedBarbers(): Flow<List<BarberProfile>> {
        val path = "barberProfiles"
        return db.collection(path)
            .whereEqualTo("approvalStatus", "APPROVED")
            .snapshots()
            .map { snap -> snap.toObjects(BarberProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeOwnBarberProfile(barberId: String): Flow<BarberProfile?> {
        val path = "barberProfiles/$barberId"
        return db.collection("barberProfiles").document(barberId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(BarberProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    fun observeAllBarbersForAdmin(): Flow<List<BarberProfile>> {
        val path = "barberProfiles"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(BarberProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun updateBarberScheduleAndAvailability(
        isOnline: Boolean,
        workStartHour: Long,
        workEndHour: Long,
        breakStartHour: Long,
        breakEndHour: Long,
        travelBufferMinutes: Long,
        upiId: String,
        shopName: String,
        shopAddress: String,
        skills: List<String>,
        serviceIds: List<String>
    ) {
        val uid = requireUserId()
        val docRef = db.collection("barberProfiles").document(uid)
        try {
            docRef.update(
                mapOf(
                    "isOnline" to isOnline,
                    "workStartHour" to workStartHour.coerceIn(0L, 23L),
                    "workEndHour" to workEndHour.coerceIn(1L, 24L),
                    "breakStartHour" to breakStartHour.coerceIn(0L, 23L),
                    "breakEndHour" to breakEndHour.coerceIn(0L, 24L),
                    "travelBufferMinutes" to travelBufferMinutes.coerceIn(0L, 180L),
                    "upiId" to upiId.trim().take(100),
                    "shopName" to shopName.trim().take(120),
                    "shopAddress" to shopAddress.trim().take(240),
                    "skills" to skills.map { it.trim().take(40) }.filter { it.isNotBlank() }.take(15),
                    "serviceIds" to serviceIds.take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 5. Active Slot Locks & Atomic Zero-Customer-Fee Booking Creation ---

    fun observeActiveSlotLocks(): Flow<List<SlotLock>> {
        val path = "slotLocks"
        return db.collection(path)
            .whereIn("status", listOf("HELD", "CONFIRMED"))
            .snapshots()
            .map { snap -> snap.toObjects(SlotLock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    /**
     * Atomically reserves the barber's time slot via deterministic lock ID, snapshots service prices,
     * applies ₹0 platform fee to the customer, calculates Barber Commission, and stores protected door address.
     */
    suspend fun createBookingWithSlotLock(
        customerName: String,
        customerPhone: String,
        barber: BarberProfile,
        selectedServices: List<CatalogService>,
        address: CustomerAddress,
        travelChargePaise: Long,
        barberCommissionPaise: Long,
        dateKey: String,
        startHour: Long,
        customerNotes: String,
        policySnapshot: String
    ): String {
        val uid = requireUserId()
        require(selectedServices.isNotEmpty()) { "Select at least one service." }
        val lockId = "${barber.barberId}_${dateKey}_${startHour}"
        val bookingId = "book_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val bookingNumber = "DC-${dateKey.replace("-", "")}-${(1000..9999).random()}"

        val serviceSubtotalPaise = selectedServices.sumOf { it.pricePaise }
        val totalDurationMin = selectedServices.sumOf { it.durationMinutes }
        val barberPayablePaise = serviceSubtotalPaise + travelChargePaise
        val safeCommissionPaise = barberCommissionPaise.coerceIn(0L, barberPayablePaise)
        val barberNetPaise = (barberPayablePaise - safeCommissionPaise).coerceAtLeast(0L)
        val summary = selectedServices.joinToString(", ") { "${it.name} (${CurrencyUtils.formatRupees(it.pricePaise)})" }

        val lockRef = db.collection("slotLocks").document(lockId)
        val bookingRef = db.collection("bookings").document(bookingId)
        val privRef = bookingRef.collection("privateDetails").document("main")

        try {
            db.runTransaction { tx ->
                val existingLock = tx.get(lockRef)
                if (existingLock.exists()) {
                    val status = existingLock.getString("status") ?: "HELD"
                    if (status == "HELD" || status == "CONFIRMED") {
                        throw IllegalStateException("SLOT_UNAVAILABLE: This barber time slot was just reserved by another customer.")
                    }
                }

                val lockData = mutableMapOf<String, Any>(
                    "barberId" to barber.barberId,
                    "dateKey" to dateKey,
                    "hourSlot" to startHour,
                    "bookingId" to bookingId,
                    "customerId" to uid,
                    "status" to "CONFIRMED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!existingLock.exists()) {
                    lockData["createdAt"] = FieldValue.serverTimestamp()
                } else {
                    lockData["createdAt"] = existingLock.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
                }
                tx.set(lockRef, lockData)

                tx.set(
                    bookingRef,
                    mapOf(
                        "bookingNumber" to bookingNumber,
                        "customerId" to uid,
                        "customerName" to customerName.ifBlank { "Customer" }.take(100),
                        "barberId" to barber.barberId,
                        "barberName" to barber.name.take(100),
                        "status" to "AWAITING_BARBER",
                        "dateKey" to dateKey,
                        "startHour" to startHour,
                        "durationMinutes" to totalDurationMin.coerceIn(5L, 600L),
                        "serviceIds" to selectedServices.map { it.id }.take(15),
                        "serviceNamesSummary" to summary.take(500),
                        "areaName" to address.serviceAreaName.ifBlank { address.streetArea }.take(120),
                        "serviceSubtotalPaise" to serviceSubtotalPaise,
                        "approvedAddOnPaise" to 0L,
                        "travelChargePaise" to travelChargePaise,
                        "barberServicePayablePaise" to barberPayablePaise,
                        "platformBookingFeePaise" to 0L, // ₹0 Customer Platform Fee
                        "totalCustomerCostPaise" to barberPayablePaise,
                        "bookingFeeStatus" to "ZERO_CUSTOMER_FEE",
                        "bookingFeeTxRef" to "FREE-SLOT-${bookingNumber.takeLast(4)}",
                        "barberCommissionPaise" to safeCommissionPaise,
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
                        "policySnapshot" to policySnapshot.take(500),
                        "hasReview" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val fullAddr = "${address.flatHouse}, ${address.streetArea}, ${address.landmark} - PIN ${address.postalCode}"
                tx.set(
                    privRef,
                    mapOf(
                        "bookingId" to bookingId,
                        "customerId" to uid,
                        "barberId" to barber.barberId,
                        "fullAddressSnapshot" to fullAddr.take(500),
                        "accessInstructions" to address.accessInstructions.take(300),
                        "customerPhone" to customerPhone.take(20),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            return bookingId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, bookingRef.path)
            throw e
        }
    }

    // --- 6. Observing Bookings & Protected Address ---

    fun observeCustomerBookings(customerId: String): Flow<List<Booking>> {
        val path = "bookings"
        return db.collection(path)
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snap -> snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeBarberBookings(barberId: String): Flow<List<Booking>> {
        val path = "bookings"
        return db.collection(path)
            .whereEqualTo("barberId", barberId)
            .snapshots()
            .map { snap -> snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAllBookingsForAdmin(): Flow<List<Booking>> {
        val path = "bookings"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun getBookingById(bookingId: String): Booking? {
        val docRef = db.collection("bookings").document(bookingId)
        try {
            val snap = docRef.get().await()
            return if (snap.exists()) snap.toObject(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun getProtectedBookingAddress(bookingId: String): BookingPrivateAddress? {
        val docRef = db.collection("bookings").document(bookingId).collection("privateDetails").document("main")
        try {
            val snap = docRef.get().await()
            return if (snap.exists()) snap.toObject(BookingPrivateAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    // --- 7. Customer & Barber Booking Transitions ---

    suspend fun barberRespondToBooking(booking: Booking, accept: Boolean, reason: String = "") {
        requireUserId()
        val bookingRef = db.collection("bookings").document(booking.id)
        val lockId = "${booking.barberId}_${booking.dateKey}_${booking.startHour}"
        val lockRef = db.collection("slotLocks").document(lockId)
        try {
            val batch = db.batch()
            if (accept) {
                batch.update(
                    bookingRef,
                    mapOf(
                        "status" to "CONFIRMED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    bookingRef,
                    mapOf(
                        "status" to "CANCELLED",
                        "cancellationReason" to "Barber declined: ${reason.ifBlank { "Schedule conflict" }}".take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    lockRef,
                    mapOf(
                        "status" to "RELEASED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path)
            throw e
        }
    }

    suspend fun barberUpdateExecutionStatus(bookingId: String, nextStatus: String) {
        requireUserId()
        val bookingRef = db.collection("bookings").document(bookingId)
        try {
            bookingRef.update(
                mapOf(
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path)
            throw e
        }
    }

    suspend fun barberRequestExtraService(bookingId: String, extraService: CatalogService) {
        requireUserId()
        val bookingRef = db.collection("bookings").document(bookingId)
        try {
            bookingRef.update(
                mapOf(
                    "pendingAddOnServiceId" to extraService.id,
                    "pendingAddOnName" to extraService.name.take(100),
                    "pendingAddOnPricePaise" to extraService.pricePaise,
                    "pendingAddOnDurationMin" to extraService.durationMinutes,
                    "pendingAddOnStatus" to "REQUESTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path)
            throw e
        }
    }

    suspend fun customerRespondToExtraService(booking: Booking, approve: Boolean, commissionBps: Long = 1500L) {
        requireUserId()
        val bookingRef = db.collection("bookings").document(booking.id)
        try {
            if (approve && booking.pendingAddOnStatus == "REQUESTED") {
                val newApprovedAddOn = booking.approvedAddOnPaise + booking.pendingAddOnPricePaise
                val newBarberPayable = booking.serviceSubtotalPaise + newApprovedAddOn + booking.travelChargePaise
                val newTotalCost = newBarberPayable + booking.platformBookingFeePaise
                val newCommission = ((newBarberPayable * commissionBps) / 10000L).coerceIn(0L, newBarberPayable)
                val newNet = (newBarberPayable - newCommission).coerceAtLeast(0L)
                val newSummary = "${booking.serviceNamesSummary} + ${booking.pendingAddOnName} (${CurrencyUtils.formatRupees(booking.pendingAddOnPricePaise)})"
                bookingRef.update(
                    mapOf(
                        "approvedAddOnPaise" to newApprovedAddOn,
                        "barberServicePayablePaise" to newBarberPayable,
                        "totalCustomerCostPaise" to newTotalCost,
                        "barberCommissionPaise" to newCommission,
                        "barberNetEarningsPaise" to newNet,
                        "durationMinutes" to (booking.durationMinutes + booking.pendingAddOnDurationMin).coerceAtMost(600L),
                        "serviceNamesSummary" to newSummary.take(500),
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path)
            throw e
        }
    }

    suspend fun barberRecordDirectPayment(
        bookingId: String,
        declaredStatus: String,
        method: String,
        amountReceivedPaise: Long,
        referenceNote: String
    ) {
        requireUserId()
        val bookingRef = db.collection("bookings").document(bookingId)
        try {
            val updates = mutableMapOf<String, Any>(
                "directPaymentStatus" to declaredStatus,
                "directPaymentMethod" to method,
                "directPaymentDeclaredPaise" to amountReceivedPaise.coerceAtLeast(0L),
                "directPaymentRef" to referenceNote.trim().take(120),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (declaredStatus == "DISPUTED") {
                updates["status"] = "DISPUTED"
            } else {
                updates["status"] = "COMPLETED"
            }
            bookingRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path)
            throw e
        }
    }

    suspend fun customerVerifyDirectPayment(bookingId: String, confirmPaid: Boolean, disputeReason: String = "") {
        requireUserId()
        val bookingRef = db.collection("bookings").document(bookingId)
        try {
            if (confirmPaid) {
                bookingRef.update(
                    mapOf(
                        "directPaymentStatus" to "CUSTOMER_CONFIRMED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                bookingRef.update(
                    mapOf(
                        "status" to "DISPUTED",
                        "directPaymentStatus" to "DISPUTED",
                        "cancellationReason" to "Payment dispute: ${disputeReason.ifBlank { "Amount mismatch" }}".take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path)
            throw e
        }
    }

    suspend fun customerCancelBooking(booking: Booking, reason: String) {
        requireUserId()
        val bookingRef = db.collection("bookings").document(booking.id)
        val lockId = "${booking.barberId}_${booking.dateKey}_${booking.startHour}"
        val lockRef = db.collection("slotLocks").document(lockId)
        try {
            val batch = db.batch()
            val nextFeeStatus = if (booking.platformBookingFeePaise > 0L) "REFUND_PENDING" else "ZERO_CUSTOMER_FEE"
            batch.update(
                bookingRef,
                mapOf(
                    "status" to "CANCELLED",
                    "bookingFeeStatus" to nextFeeStatus,
                    "cancellationReason" to reason.ifBlank { "Customer cancelled" }.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                lockRef,
                mapOf(
                    "status" to "RELEASED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path)
            throw e
        }
    }

    // --- 8. Reviews & Support Tickets ---

    fun observeReviewsForBarber(barberId: String): Flow<List<Review>> {
        val path = "reviews"
        return db.collection(path)
            .whereEqualTo("barberId", barberId)
            .whereEqualTo("status", "PUBLISHED")
            .snapshots()
            .map { snap -> snap.toObjects(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun submitCustomerReview(
        booking: Booking,
        customerName: String,
        rating: Long,
        comment: String
    ) {
        val uid = requireUserId()
        val reviewRef = db.collection("reviews").document(UUID.randomUUID().toString())
        val bookingRef = db.collection("bookings").document(booking.id)
        try {
            val batch = db.batch()
            batch.set(
                reviewRef,
                mapOf(
                    "bookingId" to booking.id,
                    "customerId" to uid,
                    "customerName" to customerName.ifBlank { "Verified Customer" }.take(100),
                    "barberId" to booking.barberId,
                    "rating" to rating.coerceIn(1L, 5L),
                    "comment" to comment.trim().take(600),
                    "status" to "PUBLISHED",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                bookingRef,
                mapOf(
                    "hasReview" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reviewRef.path)
            throw e
        }
    }

    fun observeMySupportTickets(userId: String): Flow<List<SupportTicket>> {
        val path = "supportTickets"
        return db.collection(path)
            .whereEqualTo("createdBy", userId)
            .snapshots()
            .map { snap -> snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAllSupportTicketsForAdmin(): Flow<List<SupportTicket>> {
        val path = "supportTickets"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun createSupportTicket(
        creatorRole: String,
        bookingId: String,
        category: String,
        subject: String,
        description: String
    ) {
        val uid = requireUserId()
        val docRef = db.collection("supportTickets").document(UUID.randomUUID().toString())
        try {
            docRef.set(
                mapOf(
                    "createdBy" to uid,
                    "creatorRole" to creatorRole,
                    "bookingId" to bookingId.take(128),
                    "category" to category,
                    "subject" to subject.trim().take(150),
                    "description" to description.trim().take(1000),
                    "status" to "OPEN",
                    "adminResolution" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- 9. Admin Operations, Initial Catalog Auto-Ensure & Audit Logs ---

    fun observeAuditLogsForAdmin(): Flow<List<AuditLog>> {
        val path = "auditLogs"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserAccount>> {
        val path = "users"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    private suspend fun recordAudit(action: String, entityType: String, entityId: String, reason: String) {
        val uid = requireUserId()
        val logRef = db.collection("auditLogs").document(UUID.randomUUID().toString())
        logRef.set(
            mapOf(
                "actorId" to uid,
                "action" to action.take(100),
                "entityType" to entityType.take(60),
                "entityId" to entityId.take(128),
                "reason" to reason.ifBlank { "Admin operational action" }.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun adminSetBarberApproval(barberId: String, approvalStatus: String, reason: String) {
        requireUserId()
        val barberRef = db.collection("barberProfiles").document(barberId)
        val userRef = db.collection("users").document(barberId)
        try {
            barberRef.update(
                mapOf(
                    "approvalStatus" to approvalStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                val mappedStatus = when (approvalStatus) {
                    "APPROVED" -> "ACTIVE"
                    "SUSPENDED" -> "SUSPENDED"
                    "REJECTED" -> "REJECTED"
                    else -> "PENDING"
                }
                userRef.update(
                    mapOf(
                        "status" to mappedStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            recordAudit("BARBER_STATUS_$approvalStatus", "BarberProfile", barberId, reason)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, barberRef.path)
            throw e
        }
    }

    suspend fun adminSaveService(
        serviceId: String?,
        name: String,
        category: String,
        description: String,
        pricePaise: Long,
        durationMinutes: Long,
        isActive: Boolean,
        isPackage: Boolean,
        instructions: String,
        sortOrder: Long
    ) {
        requireUserId()
        val id = serviceId ?: "svc_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val docRef = db.collection("services").document(id)
        try {
            val snap = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "name" to name.trim().take(100),
                "category" to category,
                "description" to description.trim().take(500),
                "pricePaise" to pricePaise.coerceAtLeast(0L),
                "durationMinutes" to durationMinutes.coerceIn(5L, 480L),
                "isActive" to isActive,
                "isPackage" to isPackage,
                "instructions" to instructions.trim().take(300),
                "sortOrder" to sortOrder,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!snap.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            } else {
                payload["createdAt"] = snap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            }
            docRef.set(payload).await()
            recordAudit(if (snap.exists()) "UPDATE_SERVICE" else "CREATE_SERVICE", "Service", id, "Updated service $name price=${CurrencyUtils.formatRupees(pricePaise)}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun adminUpdatePlatformConfig(
        bookingFeeType: String,
        barberCommissionBps: Long,
        barberFlatFeePaise: Long,
        freeCancellationCutoffHours: Long,
        slotHoldMinutes: Long,
        policyText: String,
        maintenanceMode: Boolean
    ) {
        val uid = requireUserId()
        val docRef = db.collection("platformConfig").document("global")
        try {
            docRef.set(
                mapOf(
                    "bookingFeeType" to bookingFeeType,
                    "fixedBookingFeePaise" to 0L, // Customer platform fee permanently ₹0
                    "percentageBookingFeeBps" to 0L,
                    "barberCommissionBps" to barberCommissionBps.coerceIn(0L, 5000L),
                    "barberFlatFeePaise" to barberFlatFeePaise.coerceIn(0L, 500000L),
                    "freeCancellationCutoffHours" to freeCancellationCutoffHours.coerceIn(0L, 72L),
                    "slotHoldMinutes" to slotHoldMinutes.coerceIn(1L, 60L),
                    "policyText" to policyText.trim().take(1000),
                    "maintenanceMode" to maintenanceMode,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAudit("UPDATE_BARBER_COMMISSION_POLICY", "PlatformConfig", "global", "Updated barber commission policy ($bookingFeeType)")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun adminResolveDisputeOrOverrideBooking(
        booking: Booking,
        newStatus: String,
        newFeeStatus: String,
        newDirectPaymentStatus: String,
        replacementBarber: BarberProfile?,
        reason: String
    ) {
        requireUserId()
        val bookingRef = db.collection("bookings").document(booking.id)
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "bookingFeeStatus" to newFeeStatus,
                "directPaymentStatus" to newDirectPaymentStatus,
                "cancellationReason" to reason.trim().take(300),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (replacementBarber != null && replacementBarber.barberId != booking.barberId) {
                updates["barberId"] = replacementBarber.barberId
                updates["barberName"] = replacementBarber.name.take(100)
                val privRef = bookingRef.collection("privateDetails").document("main")
                val privSnap = privRef.get().await()
                if (privSnap.exists()) {
                    privRef.update(
                        mapOf(
                            "barberId" to replacementBarber.barberId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            bookingRef.update(updates).await()
            recordAudit("BOOKING_OVERRIDE", "Booking", booking.id, reason)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path)
            throw e
        }
    }

    suspend fun adminResolveSupportTicket(ticketId: String, newStatus: String, resolutionNote: String) {
        requireUserId()
        val docRef = db.collection("supportTickets").document(ticketId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "adminResolution" to resolutionNote.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAudit("RESOLVE_TICKET", "SupportTicket", ticketId, resolutionNote)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Ensures the canonical service catalog, service zones, and 4 partner barber shops exist
     * when a brand-new database has no services yet, so first-time customers immediately see
     * services with photography, nearest barber shops, and recommended barbers with skills.
     */
    suspend fun ensureCanonicalCatalogIfEmpty(isAdminUser: Boolean) {
        requireUserId()
        try {
            val existingServices = db.collection("services").whereEqualTo("isActive", true).limit(1).get().await()
            if (!existingServices.isEmpty) {
                // Ensure 4th partner shop barber_kabir_atelier also exists if missing
                val kabirDoc = db.collection("barberProfiles").document("barber_kabir_atelier").get().await()
                if (!kabirDoc.exists()) {
                    seedSinglePartnerShopBarber(
                        id = "barber_kabir_atelier",
                        name = "Kabir Malhotra",
                        bio = "Master Burst Fade & Razor Line-Up Artist. Dispatches from Urban Fade & Beard House for home visits.",
                        expYears = 7L,
                        rating = 4.9,
                        reviews = 38L,
                        skills = listOf("Burst & Drop Fade", "Moustache Styling", "Charcoal Cleanup", "Razor Line-Up"),
                        perfection = "98% Razor Line Precision",
                        shopName = "Urban Fade & Beard House",
                        shopAddress = "Studio 7, Metro Boulevard, Whitefield & Central",
                        lat = 12.9698,
                        lon = 77.7499
                    )
                }
                return
            }
            if (isAdminUser) {
                adminBootstrapMasterCatalog()
            } else {
                seedCanonicalCatalogForColdStart()
            }
        } catch (_: Exception) {
            // Non-fatal on cold start
        }
    }

    private suspend fun seedSinglePartnerShopBarber(
        id: String,
        name: String,
        bio: String,
        expYears: Long,
        rating: Double,
        reviews: Long,
        skills: List<String>,
        perfection: String,
        shopName: String,
        shopAddress: String,
        lat: Double,
        lon: Double
    ) {
        val allSvcIds = canonicalCatalogSpecs().map { it.id }
        val allAreaIds = listOf("zone_central", "zone_south", "zone_west")
        db.collection("barberProfiles").document(id).set(
            mapOf(
                "barberId" to id,
                "name" to name,
                "bio" to bio,
                "experienceYears" to expYears,
                "approvalStatus" to "APPROVED",
                "isOnline" to true,
                "ratingAvg" to rating,
                "reviewCount" to reviews,
                "serviceIds" to allSvcIds,
                "serviceAreaIds" to allAreaIds,
                "workingDays" to listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
                "workStartHour" to 9L,
                "workEndHour" to 21L,
                "breakStartHour" to 14L,
                "breakEndHour" to 15L,
                "travelBufferMinutes" to 15L,
                "upiId" to "${id.substringAfter("_")}@upi",
                "skills" to skills,
                "perfectionBadge" to perfection,
                "shopName" to shopName,
                "shopAddress" to shopAddress,
                "homeVisitAvailable" to true,
                "latitude" to lat,
                "longitude" to lon,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private data class SeedSvc(
        val id: String,
        val name: String,
        val cat: String,
        val desc: String,
        val paise: Long,
        val min: Long,
        val pkg: Boolean
    )

    private fun canonicalCatalogSpecs(): List<SeedSvc> = listOf(
        SeedSvc("svc_classic_cut", "Classic Scissor & Clipper Haircut", "HAIRCUTS", "Tailored scissor & clipper haircut with neckline taper and clean home cape setup.", 30000L, 30L, false),
        SeedSvc("svc_skin_fade", "Zero / Mid / High Skin Fade", "HAIRCUTS", "Seamless razor-sharp skin fade with crisp hairline detailing by fade specialists.", 35000L, 40L, false),
        SeedSvc("svc_kids_cut", "Kids' Gentle Home Haircut", "HAIRCUTS", "Patient, mess-free haircut at home for children under 12.", 25000L, 30L, false),
        SeedSvc("svc_senior_cut", "Senior Comfort Haircut", "HAIRCUTS", "Relaxed, comfortable at-home haircut designed for seniors.", 25000L, 30L, false),
        SeedSvc("svc_beard_trim", "Beard Sculpting & Razor Line-Up", "BEARD_MOUSTACHE", "Symmetrical beard shaping, sharp cheek line-up, and neckline finish.", 15000L, 20L, false),
        SeedSvc("svc_clean_shave", "Hot Towel Luxury Clean Shave", "BEARD_MOUSTACHE", "Rich lather straight-razor shave with soothing post-shave balm.", 18000L, 25L, false),
        SeedSvc("svc_hair_wash", "Refreshing Hair Wash & Blow Dry", "HAIR_CARE", "Deep cleansing shampoo rinse and salon-styled blow dry finish.", 10000L, 15L, false),
        SeedSvc("svc_scalp_oil", "Ayurvedic Head & Scalp Massage", "HAIR_CARE", "20-minute stress-relief warm oil massage for scalp rejuvenation.", 20000L, 20L, false),
        SeedSvc("svc_face_cleanup", "Charcoal Detan Face Cleanup", "ADD_ONS", "Deep pore cleanse, exfoliating scrub, and hydrating charcoal mask.", 35000L, 30L, false),
        SeedSvc("svc_brow_neck", "Eyebrow, Ear & Neck Detailing", "ADD_ONS", "Crisp finishing touches for an ultra-clean groomed appearance.", 8000L, 10L, false),
        SeedSvc("svc_pkg_essential", "Haircut + Beard + Hair Wash Combo", "PACKAGES", "Signature at-home package: Precision Haircut, Beard Sculpt & Refreshing Wash.", 55000L, 60L, true),
        SeedSvc("svc_pkg_complete", "Executive Complete Grooming Package", "PACKAGES", "Skin Fade Haircut + Razor Beard + Detan Facial Cleanup + Scalp Massage.", 89900L, 90L, true)
    )

    private fun fullMasterCatalogSpecs(): List<SeedSvc> = listOf(
        SeedSvc("svc_normal_head_cut", "Normal Head Cut", "HAIRCUTS", "Clean, balanced everyday head cut using sanitized clippers & scissors with even side grading and neat top trim.", 19900L, 25L, false),
        SeedSvc("svc_traditional_cut", "Traditional Haircut (Side-Part / Fauji)", "HAIRCUTS", "Classic Indian barbershop scissor-over-comb haircut with neat side-part, ear contour, and disposable razor neck finish.", 22900L, 30L, false),
        SeedSvc("svc_classic_cut", "Classic Scissor & Clipper Haircut", "HAIRCUTS", "Tailored executive scissor layering & clipper blend customized to your crown and face shape.", 30000L, 30L, false),
        SeedSvc("svc_skin_fade", "Zero / Mid / High Skin Fade", "HAIRCUTS", "Seamless foil-shaver & precision guard gradient fade with razor-sharp hairline detailing.", 35000L, 40L, false),
        SeedSvc("svc_crew_buzz_cut", "Crew Cut / Uniform Buzz Cut", "HAIRCUTS", "Low-maintenance military crew or single-guard buzz cut with sharp temple and nape outline.", 17900L, 20L, false),
        SeedSvc("svc_kids_cut", "Kids' Gentle Home Haircut", "HAIRCUTS", "Patient, mess-free haircut at home for children under 12 with quiet clippers.", 25000L, 30L, false),
        SeedSvc("svc_senior_cut", "Senior Comfort Haircut", "HAIRCUTS", "Relaxed, unhurried at-home haircut designed for elders in their favorite armchair.", 25000L, 30L, false),
        SeedSvc("svc_beard_trim", "Beard Sculpting & Razor Line-Up", "BEARD_MOUSTACHE", "Symmetrical beard shaping, sharp cheek line-up, moustache trim, and neckline finish.", 15000L, 20L, false),
        SeedSvc("svc_foam_shave", "Traditional Brush & Foam Clean Shave", "BEARD_MOUSTACHE", "Classic antiseptic cream lather shave with fresh single-use blade, alum block & aftershave splash.", 12900L, 20L, false),
        SeedSvc("svc_clean_shave", "Hot Towel Luxury Clean Shave", "BEARD_MOUSTACHE", "Two-pass rich lather straight-razor shave with aromatic steam towel & cooling balm.", 18000L, 25L, false),
        SeedSvc("svc_scalp_oil", "Ayurvedic Champi Head & Shoulder Massage", "HAIR_CARE", "20-minute stress-relief warm coconut, almond, or cooling Navratna oil scalp rejuvenation.", 20000L, 20L, false),
        SeedSvc("svc_hair_wash", "Refreshing Hair Wash & Blow Dry", "HAIR_CARE", "Deep cleansing shampoo rinse and salon-styled blow dry volume finish.", 10000L, 15L, false),
        SeedSvc("svc_face_cleanup", "Charcoal Detan Face Cleanup", "ADD_ONS", "Deep pore cleanse, walnut exfoliating scrub, and peel-off activated charcoal detan mask.", 35000L, 30L, false),
        SeedSvc("svc_brow_neck", "Eyebrow, Ear & Neck Detailing", "ADD_ONS", "Crisp finishing touches for eyebrows, ear contour, and clean nape hairline.", 8000L, 10L, false),
        SeedSvc("svc_pkg_essential", "Normal / Classic Cut + Beard + Wash Combo", "PACKAGES", "Everyday value package: Precision Haircut, Beard Sculpt & Refreshing Hair Wash.", 49900L, 60L, true),
        SeedSvc("svc_pkg_complete", "Executive Fade + Beard + Detan + Champi", "PACKAGES", "Complete home transformation: Skin Fade + Razor Beard + Charcoal Detan + Champi Massage.", 89900L, 90L, true),
        SeedSvc("svc_pkg_royal_home", "Groom's & Festive Luxury Home Ritual", "PACKAGES", "95-minute festive grooming ritual: Signature Cut, Hot Towel Shave, Detan Facial, Neck Polish & Styling.", 109900L, 95L, true)
    )

    private data class SeedBarberShop(
        val id: String,
        val name: String,
        val bio: String,
        val exp: Long,
        val rating: Double,
        val reviews: Long,
        val skills: List<String>,
        val perfection: String,
        val shopName: String,
        val shopAddr: String,
        val lat: Double,
        val lon: Double
    )

    private fun canonicalBarberShopSpecs(): List<SeedBarberShop> = listOf(
        SeedBarberShop(
            id = "barber_arman_master",
            name = "Arman Shaikh",
            bio = "Master Fade & Beard Specialist with hospital-grade sanitized cordless tools. Dispatches to homes within 25 mins.",
            exp = 9L,
            rating = 4.9,
            reviews = 64L,
            skills = listOf("Zero Skin Fade", "Razor Beard Sculpt", "Textured Crop", "Sharp Hairline"),
            perfection = "99% Skin Fade Perfection",
            shopName = "Royal Blade Grooming Atelier",
            shopAddr = "Shop #14, 100ft Road, Indiranagar Central",
            lat = 12.9784,
            lon = 77.6408
        ),
        SeedBarberShop(
            id = "barber_vikram_stylist",
            name = "Vikram Rathore",
            bio = "Senior Celebrity Stylist & Executive Grooming Expert. Known for scissor layering and luxury facials at home.",
            exp = 8L,
            rating = 4.8,
            reviews = 51L,
            skills = listOf("Executive Scissor Cut", "Detan Facial", "Hot Towel Shave", "Blow-Dry Styling"),
            perfection = "98% Executive Finish",
            shopName = "Crown & Clipper Men's Studio",
            shopAddr = "2nd Cross, Signature Plaza, Koramangala 4th Block",
            lat = 12.9352,
            lon = 77.6245
        ),
        SeedBarberShop(
            id = "barber_danish_pro",
            name = "Danish Siddiqui",
            bio = "Classic Taper, Kids & Senior Comfort specialist. Patient, punctual, zero-mess floor sheet setup.",
            exp = 7L,
            rating = 4.9,
            reviews = 47L,
            skills = listOf("Classic Scissor Cut", "Kids Gentle Cut", "Senior Home Care", "Head Massage"),
            perfection = "99% Family & Scissor Craft",
            shopName = "Heritage Scissor & Shave Lounge",
            shopAddr = "Ground Floor, Parkview Arcade, MG & Central Zone",
            lat = 12.9719,
            lon = 77.6412
        ),
        SeedBarberShop(
            id = "barber_kabir_atelier",
            name = "Kabir Malhotra",
            bio = "Burst Fade, Mullet Taper & Moustache Sculpting Pro. Available for both salon visits and doorstep grooming.",
            exp = 7L,
            rating = 4.8,
            reviews = 38L,
            skills = listOf("Burst & Drop Fade", "Moustache Styling", "Charcoal Cleanup", "Razor Line-Up"),
            perfection = "98% Razor Line Precision",
            shopName = "Urban Fade & Beard House",
            shopAddr = "Studio 7, Metro Boulevard, Tech Corridor",
            lat = 12.9698,
            lon = 77.7499
        )
    )

    private suspend fun seedCanonicalCatalogForColdStart() {
        val batch = db.batch()
        val areas = listOf(
            Triple("zone_central", "Central & Indiranagar Zone", listOf("560001", "560038", "560008", "400001", "110001")),
            Triple("zone_south", "Koramangala & HSR Layout Zone", listOf("560034", "560102", "400050", "110016")),
            Triple("zone_west", "Whitefield & Tech Corridor Zone", listOf("560066", "560037", "400076", "122002"))
        )
        areas.forEachIndexed { idx, (id, name, pins) ->
            batch.set(
                db.collection("serviceAreas").document(id),
                mapOf(
                    "name" to name,
                    "postalCodes" to pins,
                    "travelChargePaise" to if (idx == 2) 2000L else 0L,
                    "maxDistanceKm" to 15L,
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        val catalog = canonicalCatalogSpecs()
        catalog.forEachIndexed { idx, s ->
            batch.set(
                db.collection("services").document(s.id),
                mapOf(
                    "name" to s.name,
                    "category" to s.cat,
                    "description" to s.desc,
                    "pricePaise" to s.paise,
                    "durationMinutes" to s.min,
                    "isActive" to true,
                    "isPackage" to s.pkg,
                    "instructions" to "Please keep a sturdy chair, power socket, and towel area accessible.",
                    "sortOrder" to (idx + 1).toLong(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        val allSvcIds = catalog.map { it.id }
        val allAreaIds = areas.map { it.first }
        canonicalBarberShopSpecs().forEach { b ->
            batch.set(
                db.collection("barberProfiles").document(b.id),
                mapOf(
                    "barberId" to b.id,
                    "name" to b.name,
                    "bio" to b.bio,
                    "experienceYears" to b.exp,
                    "approvalStatus" to "APPROVED",
                    "isOnline" to true,
                    "ratingAvg" to b.rating,
                    "reviewCount" to b.reviews,
                    "serviceIds" to allSvcIds,
                    "serviceAreaIds" to allAreaIds,
                    "workingDays" to listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
                    "workStartHour" to 9L,
                    "workEndHour" to 21L,
                    "breakStartHour" to 14L,
                    "breakEndHour" to 15L,
                    "travelBufferMinutes" to 15L,
                    "upiId" to "${b.id.substringAfter("_")}@upi",
                    "skills" to b.skills,
                    "perfectionBadge" to b.perfection,
                    "shopName" to b.shopName,
                    "shopAddress" to b.shopAddr,
                    "homeVisitAvailable" to true,
                    "latitude" to b.lat,
                    "longitude" to b.lon,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
    }

    /**
     * Admin one-tap bootstrap/refresh to populate the grooming service catalog,
     * supported service zones, Barber Commission policy (₹0 customer fee, 15% barber commission),
     * and partner barber shops with verified skills & GPS coordinates.
     */
    suspend fun adminBootstrapMasterCatalog() {
        val uid = requireUserId()
        try {
            val batch = db.batch()

            // 1. Platform Config (₹0 Customer Fee, 15% Commission from Barber)
            batch.set(
                db.collection("platformConfig").document("global"),
                mapOf(
                    "bookingFeeType" to "BARBER_COMMISSION_PERCENT",
                    "fixedBookingFeePaise" to 0L,
                    "percentageBookingFeeBps" to 0L,
                    "barberCommissionBps" to 1500L,
                    "barberFlatFeePaise" to 0L,
                    "freeCancellationCutoffHours" to 2L,
                    "slotHoldMinutes" to 10L,
                    "policyText" to "₹0 Platform Fee for Customers! Free cancellation up to 2 hours before appointment. Pay only the barber service price directly via UPI or Cash after completion.",
                    "maintenanceMode" to false,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 2. Service Areas
            val areas = listOf(
                Triple("zone_central", "Central & Indiranagar Zone", listOf("560001", "560038", "560008", "400001", "110001")),
                Triple("zone_south", "Koramangala & HSR Layout Zone", listOf("560034", "560102", "400050", "110016")),
                Triple("zone_west", "Whitefield & Tech Corridor Zone", listOf("560066", "560037", "400076", "122002"))
            )
            areas.forEachIndexed { idx, (id, name, pins) ->
                val areaDoc = db.collection("serviceAreas").document(id)
                val existingSnap = areaDoc.get().await()
                batch.set(
                    areaDoc,
                    mapOf(
                        "name" to name,
                        "postalCodes" to pins,
                        "travelChargePaise" to if (idx == 2) 2000L else 0L,
                        "maxDistanceKm" to 15L,
                        "isActive" to true,
                        "createdAt" to (existingSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // 3. Complete Service Catalog (All 17 Canonical Haircut & Grooming Services)
            val catalog = fullMasterCatalogSpecs()
            catalog.forEachIndexed { idx, s ->
                val svcDoc = db.collection("services").document(s.id)
                val existingSnap = svcDoc.get().await()
                batch.set(
                    svcDoc,
                    mapOf(
                        "name" to s.name,
                        "category" to s.cat,
                        "description" to s.desc,
                        "pricePaise" to s.paise,
                        "durationMinutes" to s.min,
                        "isActive" to true,
                        "isPackage" to s.pkg,
                        "instructions" to "Please keep a sturdy chair, power socket, and towel area accessible.",
                        "sortOrder" to (idx + 1).toLong(),
                        "createdAt" to (existingSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // 4. Partner Barber Shops & Master Barbers with Skills
            val allSvcIds = catalog.map { it.id }
            val allAreaIds = areas.map { it.first }
            canonicalBarberShopSpecs().forEach { b ->
                val bDoc = db.collection("barberProfiles").document(b.id)
                val existingSnap = bDoc.get().await()
                batch.set(
                    bDoc,
                    mapOf(
                        "barberId" to b.id,
                        "name" to b.name,
                        "bio" to b.bio,
                        "experienceYears" to b.exp,
                        "approvalStatus" to "APPROVED",
                        "isOnline" to true,
                        "ratingAvg" to b.rating,
                        "reviewCount" to b.reviews,
                        "serviceIds" to allSvcIds,
                        "serviceAreaIds" to allAreaIds,
                        "workingDays" to listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
                        "workStartHour" to 9L,
                        "workEndHour" to 21L,
                        "breakStartHour" to 14L,
                        "breakEndHour" to 15L,
                        "travelBufferMinutes" to 15L,
                        "upiId" to "${b.id.substringAfter("_")}@upi",
                        "skills" to b.skills,
                        "perfectionBadge" to b.perfection,
                        "shopName" to b.shopName,
                        "shopAddress" to b.shopAddr,
                        "homeVisitAvailable" to true,
                        "latitude" to b.lat,
                        "longitude" to b.lon,
                        "createdAt" to (existingSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            recordAudit("BOOTSTRAP_CATALOG", "Platform", "initial_seed", "Initialized catalog, partner barber shops, skills, and zero-customer-fee barber commission policy")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "bootstrap")
            throw e
        }
    }
}
