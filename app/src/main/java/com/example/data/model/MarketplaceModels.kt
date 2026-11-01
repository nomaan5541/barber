package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.util.Locale

enum class ServiceCategory(val displayTitle: String) {
    HAIRCUTS("Men's Haircuts"),
    LADIES_STYLING("Ladies Styling"),
    BEARD_MOUSTACHE("Beard & Shave"),
    HAIR_CARE("Spa & Hair Care"),
    ADD_ONS("Add-Ons"),
    PACKAGES("Value Combos")
}

object CurrencyUtils {
    private val inrFormat: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    fun formatRupees(paise: Long): String {
        val rupees = paise / 100.0
        return inrFormat.format(rupees)
    }

    fun formatHourSlot(hour: Long): String {
        val h = hour.toInt().coerceIn(0, 23)
        return when {
            h == 0 -> "12:00 AM"
            h < 12 -> "$h:00 AM"
            h == 12 -> "12:00 PM"
            else -> "${h - 12}:00 PM"
        }
    }
}

data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "CUSTOMER",
    val status: String = "ACTIVE",
    val dob: String = "",
    val age: Long = 0L,
    val gender: String = "",
    val onboardingCompleted: Boolean = false,
    val privacyAccepted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class BarberProfile(
    @DocumentId val id: String = "",
    val barberId: String = "",
    val name: String = "",
    val phone: String = "",
    val gender: String = "MALE",
    val servesLadies: Boolean = false,
    val servesMen: Boolean = true,
    val bio: String = "",
    val experienceYears: Long = 3L,
    val approvalStatus: String = "APPROVED",
    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = true,
    val ratingAvg: Double = 5.0,
    val reviewCount: Long = 0L,
    val serviceIds: List<String> = emptyList(),
    val serviceAreaIds: List<String> = emptyList(),
    val workingDays: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
    val workStartHour: Long = 9L,
    val workEndHour: Long = 20L,
    val breakStartHour: Long = 13L,
    val breakEndHour: Long = 14L,
    val travelBufferMinutes: Long = 15L,
    val upiId: String = "",
    val skills: List<String> = emptyList(),
    val perfectionBadge: String = "",
    val shopName: String = "",
    val shopAddress: String = "",
    val homeVisitAvailable: Boolean = true,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveSkills(): List<String> {
        if (skills.isNotEmpty()) return skills
        return when {
            servesLadies && servesMen -> listOf("Unisex Home Stylist", "Precision Haircut", "Hygienic Kit")
            servesLadies -> listOf("Lady Hair Stylist", "Layered Cut & Blowout", "Hygienic Kit")
            else -> listOf("Precision Haircut", "Razor Beard Line-Up", "Hygienic Kit")
        }
    }

    fun effectivePerfectionBadge(): String {
        if (perfectionBadge.isNotBlank()) return perfectionBadge
        return if (servesLadies && gender == "FEMALE") {
            "Verified Lady Hair Stylist"
        } else {
            "Verified DoorCut Partner"
        }
    }

    fun effectiveShopName(): String {
        if (shopName.isNotBlank()) return shopName
        return if (name.isNotBlank()) "$name Home Studio" else "DoorCut Partner Studio"
    }

    fun effectiveShopAddress(fallbackZoneName: String = "Local Dispatch Area"): String {
        if (shopAddress.isNotBlank()) return shopAddress
        return fallbackZoneName
    }
}

data class CatalogService(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "HAIRCUTS",
    val description: String = "",
    val pricePaise: Long = 0L,
    val durationMinutes: Long = 30L,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @get:PropertyName("isPackage")
    @set:PropertyName("isPackage")
    var isPackage: Boolean = false,
    val instructions: String = "",
    val sortOrder: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ServiceArea(
    @DocumentId val id: String = "",
    val name: String = "",
    val postalCodes: List<String> = emptyList(),
    val travelChargePaise: Long = 0L,
    val maxDistanceKm: Long = 12L,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CustomerAddress(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val label: String = "HOME",
    val flatHouse: String = "",
    val streetArea: String = "",
    val landmark: String = "",
    val postalCode: String = "",
    val serviceAreaId: String = "",
    val serviceAreaName: String = "",
    val accessInstructions: String = "",
    @get:PropertyName("isDefault")
    @set:PropertyName("isDefault")
    var isDefault: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun fullSummary(): String {
        val parts = listOf(flatHouse, streetArea, landmark, postalCode).filter { it.isNotBlank() }
        return parts.joinToString(", ")
    }

    fun displayLabel(): String {
        return when (label.uppercase(Locale.US)) {
            "HOME" -> "Home"
            "OFFICE", "WORK" -> "Work"
            else -> "Other"
        }
    }

    fun hasValidCoordinates(): Boolean = latitude != 0.0 || longitude != 0.0
}

data class SlotLock(
    @DocumentId val id: String = "",
    val barberId: String = "",
    val dateKey: String = "",
    val hourSlot: Long = 10L,
    val bookingId: String = "",
    val customerId: String = "",
    val status: String = "CONFIRMED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class Booking(
    @DocumentId val id: String = "",
    val bookingNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerGender: String = "MALE",
    val customerAge: Long = 0L,
    val preferLadyBarber: Boolean = false,
    val bookingMode: String = "ASAP",
    val customerLat: Double = 0.0,
    val customerLon: Double = 0.0,
    val barberId: String = "UNASSIGNED",
    val barberName: String = "Assigning Verified Barber",
    val barberPhone: String = "",
    val barberLat: Double = 0.0,
    val barberLon: Double = 0.0,
    val status: String = "AWAITING_BARBER",
    val dateKey: String = "",
    val startHour: Long = 10L,
    val durationMinutes: Long = 30L,
    val serviceIds: List<String> = emptyList(),
    val serviceNamesSummary: String = "",
    val areaName: String = "",
    val serviceSubtotalPaise: Long = 0L,
    val approvedAddOnPaise: Long = 0L,
    val travelChargePaise: Long = 0L,
    val barberServicePayablePaise: Long = 0L,
    val platformBookingFeePaise: Long = 0L,
    val totalCustomerCostPaise: Long = 0L,
    val bookingFeeStatus: String = "UNPAID",
    val bookingFeeTxRef: String = "",
    val barberCommissionPaise: Long = 0L,
    val barberNetEarningsPaise: Long = 0L,
    val barberCommissionStatus: String = "PENDING_SETTLEMENT",
    val directPaymentStatus: String = "PENDING",
    val directPaymentMethod: String = "NONE",
    val directPaymentDeclaredPaise: Long = 0L,
    val directPaymentRef: String = "",
    val directPaymentRecordedBy: String = "",
    val directPaymentRecordedAt: Timestamp? = null,
    val pendingAddOnServiceId: String = "",
    val pendingAddOnName: String = "",
    val pendingAddOnPricePaise: Long = 0L,
    val pendingAddOnDurationMin: Long = 0L,
    val pendingAddOnStatus: String = "NONE",
    val customerNotes: String = "",
    val cancellationReason: String = "",
    val policySnapshot: String = "",
    val hasReview: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isOpenDispatch: Boolean
        get() = barberId == "UNASSIGNED" || barberId.isBlank()

    @get:Exclude
    val areaSummary: String
        get() = areaName.ifBlank { "Home Doorstep" }

    fun isActiveVisit(): Boolean {
        return status in setOf("AWAITING_BARBER", "CONFIRMED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS")
    }

    fun isUpcoming(): Boolean = isActiveVisit()

    fun isCompletedTab(): Boolean {
        return status in setOf("COMPLETED", "DISPUTED")
    }

    fun isCancelledTab(): Boolean {
        return status in setOf("CANCELLED", "DECLINED") || status.startsWith("CANCELLED")
    }

    fun friendlyStatusText(): String {
        return when (status) {
            "AWAITING_BARBER" -> if (preferLadyBarber) "Finding Lady Stylist" else "Finding Nearby Barber"
            "CONFIRMED" -> "Barber Appointed"
            "ON_THE_WAY" -> "Barber On The Way"
            "ARRIVED" -> "Arrived at Doorstep"
            "IN_PROGRESS" -> "Service In Progress"
            "COMPLETED" -> "Completed"
            "DISPUTED" -> "Payment Disputed"
            "CANCELLED", "DECLINED" -> "Cancelled"
            else -> status.replace('_', ' ')
        }
    }

    fun effectiveBarberCommissionPaise(defaultBps: Long = 1500L): Long {
        if (barberCommissionPaise > 0L) return barberCommissionPaise
        return (barberServicePayablePaise * defaultBps) / 10000L
    }

    fun effectiveBarberNetPaise(defaultBps: Long = 1500L): Long {
        if (barberNetEarningsPaise > 0L) return barberNetEarningsPaise
        return (barberServicePayablePaise - effectiveBarberCommissionPaise(defaultBps)).coerceAtLeast(0L)
    }

    fun customerDemographicSummary(): String {
        val genderLabel = when (customerGender.uppercase(Locale.US)) {
            "FEMALE" -> "Female"
            "CHILD" -> "Child"
            "OTHER" -> "Other"
            else -> "Male"
        }
        val agePart = if (customerAge > 0L) " · ${customerAge} yrs" else ""
        val ladyBadge = if (preferLadyBarber) " · Lady Stylist Needed" else ""
        return "$genderLabel$agePart$ladyBadge"
    }

    fun bookingFeeSummaryLabel(): String {
        return when (bookingFeeStatus) {
            "ZERO_CUSTOMER_FEE" -> "Zero Upfront Fee"
            "PAID_VERIFIED" -> "Booking Fee Verified (${CurrencyUtils.formatRupees(platformBookingFeePaise)})"
            "UNPAID" -> "Booking Fee Pending Server/Admin Verification (${CurrencyUtils.formatRupees(platformBookingFeePaise)})"
            "FAILED" -> "Booking Fee Verification Failed"
            "REFUND_PENDING" -> "Booking Fee Refund Pending"
            "REFUNDED" -> "Booking Fee Refunded"
            else -> bookingFeeStatus.replace('_', ' ')
        }
    }

    fun directPaymentSummaryLabel(): String {
        return when (directPaymentStatus) {
            "CASH_DECLARED" -> "Direct Cash Recorded by Barber (${CurrencyUtils.formatRupees(directPaymentDeclaredPaise)}) · Not App-Processed"
            "UPI_DECLARED" -> "Direct UPI Recorded by Barber (${CurrencyUtils.formatRupees(directPaymentDeclaredPaise)}) · Not App-Processed"
            "CUSTOMER_CONFIRMED" -> "Customer Confirmed Direct Payment (${directPaymentMethod.ifBlank { "Cash/UPI" }}) · Not App-Processed"
            "UNPAID_DECLARED" -> "Direct Payment Unconfirmed"
            "DISPUTED" -> "Direct Payment Disputed"
            else -> "Pay barber directly after service (Cash / UPI)"
        }
    }

    fun directPaymentAuditSummary(): String {
        if (directPaymentRecordedBy.isBlank()) return ""
        val timeText = directPaymentRecordedAt?.toDate()?.let { date ->
            java.text.SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(date)
        } ?: "Just now"
        val recorder = if (directPaymentRecordedBy == barberId && barberName.isNotBlank()) {
            barberName
        } else {
            "Barber (${directPaymentRecordedBy.take(8)})"
        }
        val refPart = if (directPaymentRef.isNotBlank()) " · Ref: $directPaymentRef" else ""
        return "Recorded by $recorder at $timeText$refPart"
    }
}

data class BookingPrivateAddress(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val barberId: String = "UNASSIGNED",
    val fullAddressSnapshot: String = "",
    val accessInstructions: String = "",
    val customerPhone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ReviewItem(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val barberId: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val status: String = "PUBLISHED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SupportTicket(
    @DocumentId val id: String = "",
    val createdBy: String = "",
    val creatorRole: String = "CUSTOMER",
    val bookingId: String = "",
    val category: String = "GENERAL",
    val subject: String = "",
    val description: String = "",
    val status: String = "OPEN",
    val adminResolution: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AuditLogEntry(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

typealias AuditLog = AuditLogEntry

data class PlatformConfig(
    @DocumentId val id: String = "global",
    val bookingFeeType: String = "FIXED",
    val fixedBookingFeePaise: Long = 2900L,
    val percentageBookingFeeBps: Long = 0L,
    val barberCommissionBps: Long = 1500L,
    val barberFlatFeePaise: Long = 0L,
    val freeCancellationCutoffHours: Long = 2L,
    val slotHoldMinutes: Long = 10L,
    val policyText: String = "Pay a small slot confirmation fee now and pay the service charge directly to your appointed barber after the service by Cash or UPI. Free cancellation before your barber starts travelling.",
    val maintenanceMode: Boolean = false,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveCustomerBookingFeePaise(serviceSubtotalPaise: Long = 30000L): Long =
        computeCustomerConfirmationFeePaise(serviceSubtotalPaise)

    fun computeCustomerConfirmationFeePaise(serviceSubtotalPaise: Long): Long {
        if (serviceSubtotalPaise <= 0L) return 0L
        return when (bookingFeeType) {
            "PERCENTAGE" -> {
                if (percentageBookingFeeBps > 0L) {
                    (serviceSubtotalPaise * percentageBookingFeeBps) / 10000L
                } else {
                    fixedBookingFeePaise.takeIf { it > 0L } ?: 2900L
                }
            }
            else -> fixedBookingFeePaise.takeIf { it > 0L } ?: 2900L
        }
    }

    fun computeBarberCommissionPaise(servicePayablePaise: Long): Long {
        return when (bookingFeeType) {
            "BARBER_FLAT_FEE" -> barberFlatFeePaise.coerceAtMost(servicePayablePaise)
            else -> {
                val bps = if (barberCommissionBps > 0L) barberCommissionBps else 1500L
                (servicePayablePaise * bps) / 10000L
            }
        }
    }
}

data class VerifiedPayment(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val amountPaise: Long = 0L,
    val provider: String = "ADMIN_MANUAL",
    val orderId: String = "",
    val paymentId: String = "",
    val verifiedBy: String = "",
    @ServerTimestamp val verifiedAt: Timestamp? = null
)
