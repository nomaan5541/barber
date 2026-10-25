package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

enum class UserRole {
    CUSTOMER, BARBER, ADMIN
}

enum class AccountStatus {
    ACTIVE, PENDING, SUSPENDED, REJECTED
}

enum class ServiceCategory(val displayTitle: String) {
    HAIRCUTS("Haircuts"),
    BEARD_MOUSTACHE("Beard & Shave"),
    HAIR_CARE("Wash & Hair Care"),
    ADD_ONS("Facial & Detailing"),
    PACKAGES("Home Combos")
}

enum class BookingStatus(val label: String) {
    DRAFT("Draft"),
    SLOT_HELD("Slot Held"),
    PAYMENT_PENDING("Pending"),
    AWAITING_BARBER("Awaiting Barber Acceptance"),
    CONFIRMED("Confirmed"),
    ON_THE_WAY("Barber On The Way"),
    ARRIVED("Barber Arrived"),
    IN_PROGRESS("Service In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired"),
    NO_SHOW("No-Show"),
    DISPUTED("Disputed")
}

@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "CUSTOMER",
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
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
    val isDefault: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BarberProfile(
    @DocumentId val id: String = "",
    val barberId: String = "",
    val name: String = "",
    val bio: String = "",
    val experienceYears: Long = 0L,
    val approvalStatus: String = "PENDING",
    val isOnline: Boolean = true,
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
        return when (barberId) {
            "barber_arman_master" -> listOf("Zero Skin Fade", "Razor Beard Sculpt", "Crop Texture", "Sharp Line-Up")
            "barber_vikram_stylist" -> listOf("Executive Scissor Cut", "Detan Facial", "Hot Towel Shave", "Blow-Dry Style")
            "barber_danish_pro" -> listOf("Classic Taper Cut", "Kids Gentle Cut", "Senior Home Care", "Ayurvedic Head Massage")
            "barber_kabir_atelier" -> listOf("Burst & Drop Fade", "Moustache Styling", "Charcoal Cleanup", "Wedding Grooming")
            else -> listOf("Precision Haircut", "Beard Shaping", "Sanitized Home Kit")
        }
    }

    fun effectivePerfectionBadge(): String {
        if (perfectionBadge.isNotBlank()) return perfectionBadge
        return when (barberId) {
            "barber_arman_master" -> "99% Skin Fade Perfection"
            "barber_vikram_stylist" -> "98% Executive Finish"
            "barber_danish_pro" -> "99% Family & Scissor Craft"
            "barber_kabir_atelier" -> "98% Razor Line Precision"
            else -> "Verified Master Perfection"
        }
    }

    fun effectiveShopName(): String {
        if (shopName.isNotBlank()) return shopName
        return when (barberId) {
            "barber_arman_master" -> "Royal Blade Grooming Atelier"
            "barber_vikram_stylist" -> "Crown & Clipper Men's Studio"
            "barber_danish_pro" -> "Heritage Scissor & Shave Lounge"
            "barber_kabir_atelier" -> "Urban Fade & Beard House"
            else -> "${name.substringBefore(" ")}'s Partner Barber Studio"
        }
    }

    fun effectiveShopAddress(customerLocalityHint: String = ""): String {
        if (shopAddress.isNotBlank()) return shopAddress
        val areaSuffix = if (customerLocalityHint.isNotBlank()) customerLocalityHint else "Main High Street"
        return when (barberId) {
            "barber_arman_master" -> "Shop #14, 100ft Road, $areaSuffix"
            "barber_vikram_stylist" -> "2nd Cross, Signature Plaza, $areaSuffix"
            "barber_danish_pro" -> "Ground Floor, Parkview Arcade, $areaSuffix"
            "barber_kabir_atelier" -> "Studio 7, Metro Boulevard, $areaSuffix"
            else -> "Partner Barber Hub, $areaSuffix"
        }
    }

    fun effectiveLatitude(): Double {
        if (latitude != 0.0) return latitude
        return when (barberId) {
            "barber_arman_master" -> 12.9784
            "barber_vikram_stylist" -> 12.9352
            "barber_danish_pro" -> 12.9719
            "barber_kabir_atelier" -> 12.9698
            else -> 12.9750
        }
    }

    fun effectiveLongitude(): Double {
        if (longitude != 0.0) return longitude
        return when (barberId) {
            "barber_arman_master" -> 77.6408
            "barber_vikram_stylist" -> 77.6245
            "barber_danish_pro" -> 77.6412
            "barber_kabir_atelier" -> 77.7499
            else -> 77.6380
        }
    }
}

@IgnoreExtraProperties
data class CatalogService(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "HAIRCUTS",
    val description: String = "",
    val pricePaise: Long = 0L,
    val durationMinutes: Long = 30L,
    val isActive: Boolean = true,
    val isPackage: Boolean = false,
    val instructions: String = "",
    val sortOrder: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ServiceArea(
    @DocumentId val id: String = "",
    val name: String = "",
    val postalCodes: List<String> = emptyList(),
    val travelChargePaise: Long = 0L,
    val maxDistanceKm: Long = 10L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SlotLock(
    @DocumentId val id: String = "",
    val barberId: String = "",
    val dateKey: String = "",
    val hourSlot: Long = 10L,
    val bookingId: String = "",
    val customerId: String = "",
    val status: String = "HELD",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Booking(
    @DocumentId val id: String = "",
    val bookingNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val barberId: String = "",
    val barberName: String = "",
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
    val bookingFeeStatus: String = "ZERO_CUSTOMER_FEE",
    val bookingFeeTxRef: String = "",
    val barberCommissionPaise: Long = 0L,
    val barberNetEarningsPaise: Long = 0L,
    val barberCommissionStatus: String = "PENDING_SETTLEMENT",
    val directPaymentStatus: String = "PENDING",
    val directPaymentMethod: String = "NONE",
    val directPaymentDeclaredPaise: Long = 0L,
    val directPaymentRef: String = "",
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
    fun effectiveBarberCommissionPaise(defaultBps: Long = 1500L): Long {
        if (barberCommissionPaise > 0L) return barberCommissionPaise
        return (barberServicePayablePaise * defaultBps) / 10000L
    }

    fun effectiveBarberNetPaise(defaultBps: Long = 1500L): Long {
        if (barberNetEarningsPaise > 0L) return barberNetEarningsPaise
        val comm = effectiveBarberCommissionPaise(defaultBps)
        return (barberServicePayablePaise - comm).coerceAtLeast(0L)
    }
}

@IgnoreExtraProperties
data class BookingPrivateAddress(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val barberId: String = "",
    val fullAddressSnapshot: String = "",
    val accessInstructions: String = "",
    val customerPhone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Review(
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

@IgnoreExtraProperties
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

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlatformConfig(
    @DocumentId val id: String = "",
    val bookingFeeType: String = "BARBER_COMMISSION_PERCENT",
    val fixedBookingFeePaise: Long = 0L, // ₹0 Customer Platform Fee
    val percentageBookingFeeBps: Long = 0L, // 0% Customer Platform Fee
    val barberCommissionBps: Long = 1500L, // 15% Charged from Barber
    val barberFlatFeePaise: Long = 0L,
    val freeCancellationCutoffHours: Long = 2L,
    val slotHoldMinutes: Long = 10L,
    val policyText: String = "₹0 Platform Fee for Customers! Free cancellation up to 2 hours before appointment. Pay only the barber service price directly via UPI or Cash after completion.",
    val maintenanceMode: Boolean = false,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object CurrencyUtils {
    fun formatRupees(paise: Long): String {
        val rupees = paise / 100
        val rem = paise % 100
        return if (rem == 0L) {
            "₹$rupees"
        } else {
            String.format(Locale.US, "₹%d.%02d", rupees, rem)
        }
    }

    fun formatHourSlot(hour: Long): String {
        val h = hour.toInt()
        return when {
            h == 0 -> "12:00 AM"
            h < 12 -> "$h:00 AM"
            h == 12 -> "12:00 PM"
            else -> "${h - 12}:00 PM"
        }
    }
}
