package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
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
    fun customerRegistrationGpsAddressAndZeroFeeBooking_enforcesZeroTrustRules() = runBlocking {
        val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@example.com"
        val bobEmail = "bob_${UUID.randomUUID().toString().take(6)}@example.com"

        val aliceUid = signInTestUser(aliceEmail)
        val repo = MarketplaceRepository(firestore)

        val aliceAccount = withTimeout(5000L) {
            repo.ensureUserAccount(aliceUid, "Alice Customer", aliceEmail, "9876543210")
        }
        assertEquals("CUSTOMER", aliceAccount.role)
        assertEquals("ACTIVE", aliceAccount.status)

        // Ensure canonical catalog & partner barber shops can be initialized on fresh DB
        withTimeout(8000L) {
            repo.ensureCanonicalCatalogIfEmpty(isAdminUser = false)
        }

        val addrId = withTimeout(5000L) {
            repo.saveCustomerAddress(
                label = "HOME",
                flatHouse = "Door 12B, Emerald Residency",
                streetArea = "Indiranagar 100ft Rd",
                landmark = "Near Park",
                postalCode = "560038",
                serviceAreaId = "zone_central",
                serviceAreaName = "Central Zone",
                accessInstructions = "Ring intercom 12",
                isDefault = true,
                latitude = 12.9716,
                longitude = 77.6412
            )
        }
        assertTrue(addrId.isNotBlank())

        val emittedAddresses = withTimeout(3000L) {
            repo.observeCustomerAddresses(aliceUid).first { it.any { a -> a.id == addrId } }
        }
        val savedAddr = emittedAddresses.first { it.id == addrId }
        assertEquals(12.9716, savedAddr.latitude, 0.001)

        // Verify zero-customer-fee booking with barber commission succeeds
        val services = withTimeout(3000L) {
            repo.observeActiveServices().first { it.isNotEmpty() }
        }
        val barbers = withTimeout(3000L) {
            repo.observeApprovedBarbers().first { it.isNotEmpty() }
        }
        val chosenService = services.first()
        val chosenBarber = barbers.first()
        val expectedCommission = (chosenService.pricePaise * 1500L) / 10000L

        val uniqueYear = 2027 + kotlin.random.Random.nextInt(1, 70)
        val uniqueMonth = kotlin.random.Random.nextInt(1, 12)
        val uniqueDay = kotlin.random.Random.nextInt(10, 28)
        val dynamicDateKey = String.format(java.util.Locale.US, "%04d-%02d-%02d", uniqueYear, uniqueMonth, uniqueDay)

        val bookingId = withTimeout(5000L) {
            repo.createBookingWithSlotLock(
                customerName = "Alice Customer",
                customerPhone = "9876543210",
                barber = chosenBarber,
                selectedServices = listOf(chosenService),
                address = savedAddr,
                travelChargePaise = 0L,
                barberCommissionPaise = expectedCommission,
                dateKey = dynamicDateKey,
                startHour = 11L,
                customerNotes = "Haircut at home",
                policySnapshot = "Zero customer platform fee"
            )
        }
        val createdBooking = withTimeout(3000L) { repo.getBookingById(bookingId) }
        assertNotNull(createdBooking)
        assertEquals(0L, createdBooking?.platformBookingFeePaise)
        assertEquals("ZERO_CUSTOMER_FEE", createdBooking?.bookingFeeStatus)
        assertEquals(expectedCommission, createdBooking?.barberCommissionPaise)

        // Cross-user isolation: Bob cannot read Alice's saved door addresses
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
    }

    @Test
    fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MarketplaceRepository(firestore)
        var unauthException: Throwable? = null
        try {
            withTimeout(3000L) {
                repo.observeActiveServices().first()
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

    @Test
    fun barberRegistration_includesShopAndSkills_startsInPendingStatus() = runBlocking {
        val barberEmail = "barber_${UUID.randomUUID().toString().take(6)}@example.com"
        val barberUid = signInTestUser(barberEmail)
        val repo = MarketplaceRepository(firestore)

        withTimeout(5000L) {
            repo.ensureUserAccount(barberUid, "Rohan Stylist", barberEmail, "9988776655")
            repo.applyToBecomeBarber(
                name = "Rohan Stylist",
                phone = "9988776655",
                bio = "Fade & beard specialist",
                experienceYears = 6L,
                upiId = "rohan@upi",
                shopName = "Rohan Fade Studio",
                shopAddress = "HSR Sector 2",
                skills = listOf("Skin Fade", "Razor Beard"),
                serviceIds = listOf("svc_classic_cut"),
                serviceAreaIds = listOf("zone_central")
            )
        }

        val ownProfile = withTimeout(3000L) {
            repo.observeOwnBarberProfile(barberUid).first { it != null }
        }
        assertNotNull(ownProfile)
        assertEquals("PENDING", ownProfile?.approvalStatus)
        assertEquals("Rohan Fade Studio", ownProfile?.shopName)
        assertTrue(ownProfile?.skills?.contains("Skin Fade") == true)
    }
}
