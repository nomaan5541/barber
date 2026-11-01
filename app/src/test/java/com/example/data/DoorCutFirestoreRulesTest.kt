package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class DoorCutFirestoreRulesTest : FirestoreEmulatorTestBase() {

    @Test
    fun openDispatchBookingClaimedByVerifiedBarber_unlocksProtectedDoorAddress(): Unit = runBlocking {
        val runId = UUID.randomUUID().toString().take(8)
        val barberEmail = "priya.stylist.$runId@doorcut.test"
        val customerEmail = "ananya.customer.$runId@doorcut.test"
        val barberUid = signInTestUser(barberEmail)

        withContext(Dispatchers.IO) {
            withTimeout(10_000L) {
                firestore.collection("users").document(barberUid).set(
                    mapOf(
                        "userId" to barberUid,
                        "name" to "Priya Stylist",
                        "email" to barberEmail,
                        "phone" to "+919840099887",
                        "role" to "BARBER",
                        "status" to "PENDING",
                        "dob" to "1996-09-12",
                        "age" to 29L,
                        "gender" to "FEMALE",
                        "onboardingCompleted" to true,
                        "privacyAccepted" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                firestore.collection("barberProfiles").document(barberUid).set(
                    mapOf(
                        "barberId" to barberUid,
                        "name" to "Priya Stylist",
                        "phone" to "+919840099887",
                        "gender" to "FEMALE",
                        "servesLadies" to true,
                        "servesMen" to true,
                        "bio" to "Certified Lady Hair Stylist",
                        "experienceYears" to 6L,
                        "approvalStatus" to "APPROVED",
                        "isOnline" to true,
                        "ratingAvg" to 5.0,
                        "reviewCount" to 0L,
                        "serviceIds" to listOf("svc_ladies_haircut"),
                        "serviceAreaIds" to listOf("zone_central"),
                        "workingDays" to listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
                        "workStartHour" to 9L,
                        "workEndHour" to 20L,
                        "breakStartHour" to 13L,
                        "breakEndHour" to 14L,
                        "travelBufferMinutes" to 15L,
                        "upiId" to "priya@upi",
                        "skills" to listOf("Lady Hair Stylist", "Blowout"),
                        "perfectionBadge" to "Verified Lady Hair Stylist",
                        "shopName" to "Priya Home Studio",
                        "shopAddress" to "Central Metro Zone",
                        "homeVisitAvailable" to true,
                        "latitude" to 17.4310,
                        "longitude" to 78.4090,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

        val customerUid = signInTestUser(customerEmail)
        val bookingId = "bk_kotlin_rule_${runId}"

        withContext(Dispatchers.IO) {
            withTimeout(10_000L) {
                val baseBookingMap = mapOf(
                    "bookingNumber" to "DC-9091",
                    "customerId" to customerUid,
                    "customerName" to "Ananya Rao",
                    "customerPhone" to "+919811122334",
                    "customerGender" to "FEMALE",
                    "customerAge" to 26L,
                    "preferLadyBarber" to true,
                    "bookingMode" to "ASAP",
                    "customerLat" to 17.4250,
                    "customerLon" to 78.4140,
                    "barberId" to "UNASSIGNED",
                    "barberName" to "Matching Verified Lady Stylist",
                    "barberPhone" to "",
                    "barberLat" to 0.0,
                    "barberLon" to 0.0,
                    "status" to "AWAITING_BARBER",
                    "dateKey" to "2026-10-28",
                    "startHour" to 11L,
                    "durationMinutes" to 40L,
                    "serviceIds" to listOf("svc_ladies_haircut"),
                    "serviceNamesSummary" to "Ladies Haircut & Trim (₹399)",
                    "areaName" to "Central Metro Zone",
                    "serviceSubtotalPaise" to 39900L,
                    "approvedAddOnPaise" to 0L,
                    "travelChargePaise" to 0L,
                    "barberServicePayablePaise" to 39900L,
                    "platformBookingFeePaise" to 2900L,
                    "totalCustomerCostPaise" to 42800L,
                    "bookingFeeStatus" to "UNPAID",
                    "bookingFeeTxRef" to "DISPATCH-9091",
                    "barberCommissionPaise" to 5985L,
                    "barberNetEarningsPaise" to 33915L,
                    "barberCommissionStatus" to "PENDING_SETTLEMENT",
                    "directPaymentStatus" to "PENDING",
                    "directPaymentMethod" to "NONE",
                    "directPaymentDeclaredPaise" to 0L,
                    "directPaymentRef" to "",
                    "directPaymentRecordedBy" to "",
                    "pendingAddOnServiceId" to "",
                    "pendingAddOnName" to "",
                    "pendingAddOnPricePaise" to 0L,
                    "pendingAddOnDurationMin" to 0L,
                    "pendingAddOnStatus" to "NONE",
                    "customerNotes" to "Call at lobby",
                    "cancellationReason" to "",
                    "policySnapshot" to "Free cancellation before dispatch",
                    "hasReview" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                // 1. Customer attempting to forge PAID_VERIFIED without /verifiedPayments/{bookingId} MUST fail
                try {
                    firestore.collection("bookings").document("forged_$bookingId")
                        .set(baseBookingMap + ("bookingFeeStatus" to "PAID_VERIFIED"))
                        .await()
                    fail("Customer spoofing PAID_VERIFIED without server verifiedPayment must be denied")
                } catch (expected: Exception) {
                    assertEquals(
                        FirebaseFirestoreException.Code.PERMISSION_DENIED,
                        expected.unwrapFirestoreException()?.code
                    )
                }

                // 2. Customer creating booking with honest UNPAID bookingFeeStatus succeeds
                val bookingRef = firestore.collection("bookings").document(bookingId)
                val privRef = bookingRef.collection("privateDetails").document("main")
                val batch = firestore.batch()
                batch.set(bookingRef, baseBookingMap)
                batch.set(
                    privRef,
                    mapOf(
                        "bookingId" to bookingId,
                        "customerId" to customerUid,
                        "barberId" to "UNASSIGNED",
                        "fullAddressSnapshot" to "Flat 502, Emerald Towers",
                        "accessInstructions" to "Intercom 502",
                        "customerPhone" to "+919811122334",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            }
        }

        // Switch back to Barber Priya: before claiming, privateDetails must be DENIED
        signInTestUser(barberEmail)
        withContext(Dispatchers.IO) {
            withTimeout(10_000L) {
                try {
                    firestore.collection("bookings").document(bookingId)
                        .collection("privateDetails").document("main").get().await()
                    fail("Unclaimed booking privateDetails should be permission denied")
                } catch (expected: Exception) {
                    assertEquals(
                        FirebaseFirestoreException.Code.PERMISSION_DENIED,
                        expected.unwrapFirestoreException()?.code
                    )
                }

                // Barber Priya claims the booking from the open dispatch queue
                firestore.collection("bookings").document(bookingId).update(
                    mapOf(
                        "barberId" to barberUid,
                        "barberName" to "Priya Stylist",
                        "barberPhone" to "+919840099887",
                        "barberLat" to 17.4310,
                        "barberLon" to 78.4090,
                        "status" to "CONFIRMED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Now privateDetails IS unlocked for Priya!
                val privSnap = firestore.collection("bookings").document(bookingId)
                    .collection("privateDetails").document("main").get().await()
                assertNotNull(privSnap)
                assertEquals("+919811122334", privSnap.getString("customerPhone"))

                // Priya streams ON_THE_WAY live GPS movement and completes visit
                firestore.collection("bookings").document(bookingId).update(
                    mapOf(
                        "status" to "COMPLETED",
                        "barberLat" to 17.4250,
                        "barberLon" to 78.4140,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Declaring direct payment without directPaymentRecordedBy audit field must fail
                try {
                    firestore.collection("bookings").document(bookingId).update(
                        mapOf(
                            "directPaymentStatus" to "UPI_DECLARED",
                            "directPaymentMethod" to "UPI_DIRECT",
                            "directPaymentDeclaredPaise" to 39900L,
                            "directPaymentRef" to "UPI-8899",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    fail("Barber direct payment declaration without audit metadata must be denied")
                } catch (expected: Exception) {
                    assertEquals(
                        FirebaseFirestoreException.Code.PERMISSION_DENIED,
                        expected.unwrapFirestoreException()?.code
                    )
                }

                // Declaring direct payment WITH directPaymentRecordedBy + directPaymentRecordedAt succeeds
                firestore.collection("bookings").document(bookingId).update(
                    mapOf(
                        "directPaymentStatus" to "UPI_DECLARED",
                        "directPaymentMethod" to "UPI_DIRECT",
                        "directPaymentDeclaredPaise" to 39900L,
                        "directPaymentRef" to "UPI-8899",
                        "directPaymentRecordedBy" to barberUid,
                        "directPaymentRecordedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Unit
            }
        }
    }
}
