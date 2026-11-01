package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CatalogService
import com.example.data.model.PlatformConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MarketplaceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun customerOnboardingOpenDispatchAndBarberClaimLiveGps_enforcesZeroTrustRules() = runBlocking {
        val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@doorcut.test"
        val bobEmail = "bob_${UUID.randomUUID().toString().take(6)}@doorcut.test"
        val priyaEmail = "priya_stylist_${UUID.randomUUID().toString().take(6)}@doorcut.test"

        // 1. Alice signs in & completes Customer onboarding (DOB, Age, Gender=FEMALE)
        val aliceUid = signInTestUser(aliceEmail)
        val repo = MarketplaceRepository(firestore, auth)

        val initialProfile = withTimeout(5000L) { repo.ensureSignedInProfile().getOrThrow() }
        assertEquals(aliceUid, initialProfile.userId)

        withTimeout(5000L) {
            repo.completeUserOnboarding(
                fullName = "AliceCustomer",
                phone = "+919840011223",
                dob = "1998-05-14",
                age = 27L,
                gender = "FEMALE",
                selectedRole = "CUSTOMER",
                servesLadies = false,
                servesMen = false,
                experienceYears = 0L,
                upiId = "",
                primaryArea = "Jubilee Hills",
                latitude = 17.4315,
                longitude = 78.4070,
                defaultServiceIds = emptyList()
            ).getOrThrow()
        }

        val addrId = withTimeout(5000L) {
            repo.saveCustomerAddress(
                label = "HOME",
                flatHouse = "Flat 402, Lotus Enclave",
                streetArea = "Jubilee Hills Road 36",
                landmark = "Near Metro Pillar",
                postalCode = "500033",
                serviceArea = null,
                accessInstructions = "Call at lobby",
                isDefault = true,
                latitude = 17.4315,
                longitude = 78.4070
            ).getOrThrow()
        }
        assertTrue(addrId.isNotBlank())

        val savedAddr = withTimeout(4000L) {
            repo.observeCustomerAddresses(aliceUid).first { it.any { a -> a.id == addrId } }
                .first { it.id == addrId }
        }
        assertEquals(17.4315, savedAddr.latitude, 0.001)

        // 2. Alice dispatches two Open-Pool bookings on 2026-11-15:
        //    Booking 1 at 11:00 (50 min service) + Booking 2 at 12:00 (40 min service)
        val ladiesHaircut50Min = CatalogService(
            id = "svc_ladies_layered_test",
            name = "Ladies Layered Cut",
            category = "LADIES_STYLING",
            description = "Layered haircut at home",
            pricePaise = 49900L,
            durationMinutes = 50L,
            isActive = true
        )
        val ladiesTrim40Min = CatalogService(
            id = "svc_ladies_trim_test",
            name = "Ladies Haircut & Trim",
            category = "LADIES_STYLING",
            description = "Clean U-cut or V-cut at home",
            pricePaise = 39900L,
            durationMinutes = 40L,
            isActive = true
        )

        val bookingId = withTimeout(5000L) {
            repo.createOpenDispatchBooking(
                customerName = "AliceCustomer",
                customerPhone = "+919840011223",
                customerGender = "FEMALE",
                customerAge = 27L,
                preferLadyBarber = true,
                bookingMode = "SCHEDULED",
                selectedServices = listOf(ladiesHaircut50Min),
                address = savedAddr,
                serviceArea = null,
                dateKey = "2026-11-15",
                startHour = 11L,
                customerNotes = "Need Lady Hair Stylist",
                bookingFeeTxRef = "BFEE-TEST-101",
                config = PlatformConfig()
            ).getOrThrow()
        }
        assertTrue(bookingId.isNotBlank())

        val overlappingBookingId = withTimeout(5000L) {
            repo.createOpenDispatchBooking(
                customerName = "AliceCustomer",
                customerPhone = "+919840011223",
                customerGender = "FEMALE",
                customerAge = 27L,
                preferLadyBarber = true,
                bookingMode = "SCHEDULED",
                selectedServices = listOf(ladiesTrim40Min),
                address = savedAddr,
                serviceArea = null,
                dateKey = "2026-11-15",
                startHour = 12L,
                customerNotes = "Second overlapping slot",
                bookingFeeTxRef = "BFEE-TEST-102",
                config = PlatformConfig()
            ).getOrThrow()
        }

        // Verify client-created booking has honest UNPAID bookingFeeStatus (never spoofed PAID_VERIFIED)
        val createdBooking = withTimeout(4000L) {
            repo.observeSingleBooking(bookingId).first { it != null }!!
        }
        assertEquals("UNPAID", createdBooking.bookingFeeStatus)

        // 3. Cross-user isolation: Bob cannot read Alice's saved door addresses
        signInTestUser(bobEmail)
        var crossUserException: Throwable? = null
        try {
            withTimeout(3000L) {
                repo.observeCustomerAddresses(aliceUid).first()
            }
        } catch (e: Throwable) {
            crossUserException = e
        }
        assertNotNull("Expected PERMISSION_DENIED on cross-user address read", crossUserException)
        val fsEx = checkNotNull(crossUserException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $crossUserException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)

        // 4. Priya signs in, completes Barber Partner onboarding (Lady Stylist, 15m travel buffer), claims Booking 1
        val priyaUid = signInTestUser(priyaEmail)
        withTimeout(5000L) {
            repo.ensureSignedInProfile().getOrThrow()
            repo.completeUserOnboarding(
                fullName = "Priya Stylist",
                phone = "+919840099887",
                dob = "1995-08-20",
                age = 30L,
                gender = "FEMALE",
                selectedRole = "BARBER",
                servesLadies = true,
                servesMen = true,
                experienceYears = 6L,
                upiId = "priya@upi",
                primaryArea = "Jubilee Hills",
                latitude = 17.4280,
                longitude = 78.4120,
                defaultServiceIds = listOf("svc_ladies_layered_test", "svc_ladies_trim_test")
            ).getOrThrow()
        }

        val priyaProfile = withTimeout(4000L) {
            repo.observeOwnBarberProfile(priyaUid).first { it != null }!!
        }
        assertTrue(priyaProfile.servesLadies)

        // Open pool exposes Alice's UNASSIGNED booking to Priya
        val openPool = withTimeout(4000L) {
            repo.observeOpenDispatchBookings().first { list -> list.any { it.id == bookingId } }
        }
        assertTrue(openPool.any { it.id == bookingId && it.isOpenDispatch && it.preferLadyBarber })

        // Priya claims Booking 1 (11:00, 50m + 15m travel buffer = 65m -> atomically locks 11:00 AND 12:00)
        withTimeout(5000L) {
            repo.barberClaimOpenBooking(
                bookingId = bookingId,
                barberProfile = priyaProfile,
                liveLat = 17.4280,
                liveLon = 78.4120
            ).getOrThrow()
        }

        // Attempting to claim overlapping Booking 2 at 12:00 on 2026-11-15 MUST fail due to atomic slotLocks!
        val overlapAttempt = withTimeout(5000L) {
            repo.barberClaimOpenBooking(
                bookingId = overlappingBookingId,
                barberProfile = priyaProfile,
                liveLat = 17.4280,
                liveLon = 78.4120
            )
        }
        assertTrue(
            "Expected overlapping booking claim at 12:00 to be blocked by travel-buffer slotLocks",
            overlapAttempt.isFailure
        )

        // Protected home address & verified phone now unlock for Priya on Booking 1
        val unlockedAddress = withTimeout(4000L) {
            repo.fetchProtectedBookingAddress(bookingId).getOrThrow()
        }
        assertNotNull(unlockedAddress)
        assertEquals("+919840011223", unlockedAddress?.customerPhone)

        // Priya streams live GPS coordinates, completes visit, and declares direct post-service payment with audit metadata
        withTimeout(5000L) {
            repo.barberUpdateTripStatus(bookingId, "ON_THE_WAY", 17.4295, 78.4095).getOrThrow()
            repo.barberUpdateLiveLocation(bookingId, 17.4305, 78.4080).getOrThrow()
            repo.barberUpdateTripStatus(bookingId, "COMPLETED", 17.4315, 78.4070).getOrThrow()
            repo.barberDeclareDirectPayment(
                bookingId = bookingId,
                method = "UPI_DIRECT",
                declaredAmountPaise = 49900L,
                paymentRef = "UPI-TEST-991",
                isFullAmount = true
            ).getOrThrow()
        }

        val completedBooking = withTimeout(4000L) {
            repo.observeSingleBooking(bookingId).first { it?.directPaymentStatus == "UPI_DECLARED" }!!
        }
        assertEquals(priyaUid, completedBooking.directPaymentRecordedBy)
        assertNotNull(completedBooking.directPaymentRecordedAt)
    }

    @Test
    fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MarketplaceRepository(firestore, auth)

        var unauthException: Throwable? = null
        try {
            withTimeout(3000L) {
                repo.observeOpenDispatchBookings().first()
            }
        } catch (e: Throwable) {
            unauthException = e
        }
        assertNotNull("Expected PERMISSION_DENIED for unauthenticated caller", unauthException)
        val fsEx = checkNotNull(unauthException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $unauthException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }
}
