package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PhonePermissionsSnapshot(
    val hasFineLocation: Boolean,
    val hasCoarseLocation: Boolean,
    val hasNotifications: Boolean,
    val hasCamera: Boolean
) {
    val hasAnyLocation: Boolean
        get() = hasFineLocation || hasCoarseLocation

    val allCoreGranted: Boolean
        get() = hasAnyLocation && hasNotifications

    val allGrantedIncludingCamera: Boolean
        get() = hasAnyLocation && hasNotifications && hasCamera

    val missingCount: Int
        get() = listOf(hasAnyLocation, hasNotifications, hasCamera).count { !it }
}

object PhonePermissionsHelper {

    fun inspectPermissions(context: Context): PhonePermissionsSnapshot {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val camera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        return PhonePermissionsSnapshot(
            hasFineLocation = fine,
            hasCoarseLocation = coarse,
            hasNotifications = notifications,
            hasCamera = camera
        )
    }

    /**
     * Runtime permissions requested automatically on app startup:
     * Location (precise + approximate) for doorstep GPS coordinates & nearest barbers,
     * plus POST_NOTIFICATIONS (Android 13+) for live barber arrival alerts.
     */
    fun startupPermissionsArray(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    /**
     * Complete set of runtime phone permissions used across DoorCut features
     * (Doorstep GPS Location, Live Arrival Notifications, and Hairstyle Reference Camera).
     */
    fun allFeaturePermissionsArray(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

object DoorCutNotificationHelper {
    private const val CHANNEL_ID = "doorcut_home_visits"
    private const val CHANNEL_NAME = "Home Visit & Barber Arrival Alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time updates when your barber confirms, dispatches, or arrives at your door."
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendVisitNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return try {
            ensureChannel(context)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}

enum class DoorstepPaymentOption(
    val code: String,
    val title: String,
    val subtitle: String,
    val shortBadge: String
) {
    CARD_POS_AT_DOOR(
        code = "CARD_POS_AT_DOOR",
        title = "Credit / Debit Card at Door (POS)",
        subtitle = "Barber brings wireless tap & chip card terminal • ₹0 surcharge",
        shortBadge = "Card POS"
    ),
    UPI_AT_DOOR(
        code = "UPI_AT_DOOR",
        title = "UPI QR Scan to Barber",
        subtitle = "Pay via GPay, PhonePe, or Paytm directly after haircut",
        shortBadge = "UPI QR"
    ),
    CASH_AT_DOOR(
        code = "CASH_AT_DOOR",
        title = "Cash After Service",
        subtitle = "Settle directly in cash once your grooming is complete",
        shortBadge = "Cash"
    );

    companion object {
        fun fromCode(code: String): DoorstepPaymentOption =
            entries.firstOrNull { it.code == code } ?: CARD_POS_AT_DOOR
    }
}

data class CustomerGroomingPreferences(
    val paymentOption: DoorstepPaymentOption = DoorstepPaymentOption.CARD_POS_AT_DOOR,
    val savedCardLabel: String = "",
    val savedCardLast4: String = "",
    val savedUpiId: String = "",
    val preferredHaircutStyle: String = "Traditional Neat Side-Part",
    val preferredOilType: String = "Cooling Navratna Oil",
    val quietSession: Boolean = false,
    val floorSheetAndCapeRequired: Boolean = true,
    val referencePhotoAttached: Boolean = false,
    val referencePhotoNote: String = ""
) {
    fun buildBookingPreferencesPrefix(): String {
        val parts = mutableListOf<String>()
        val payDesc = when (paymentOption) {
            DoorstepPaymentOption.CARD_POS_AT_DOOR -> {
                if (savedCardLast4.length == 4) {
                    "Pay: Card POS (${savedCardLabel.ifBlank { "Card" }} •••• $savedCardLast4)"
                } else {
                    "Pay: Card POS Machine at Door"
                }
            }
            DoorstepPaymentOption.UPI_AT_DOOR -> {
                if (savedUpiId.isNotBlank()) "Pay: UPI ($savedUpiId)" else "Pay: UPI QR at Door"
            }
            DoorstepPaymentOption.CASH_AT_DOOR -> "Pay: Cash at Door"
        }
        parts.add(payDesc)
        parts.add("Style: $preferredHaircutStyle")
        if (quietSession) parts.add("Quiet Cut")
        if (floorSheetAndCapeRequired) parts.add("Floor Sheet + Cape")
        if (referencePhotoAttached) parts.add("Ref Photo Ready")
        return "[${parts.joinToString(" | ")}]"
    }
}

object CustomerPreferencesStore {
    private const val PREFS_NAME = "doorcut_customer_prefs"
    private val _state = MutableStateFlow(CustomerGroomingPreferences())
    val state: StateFlow<CustomerGroomingPreferences> = _state.asStateFlow()
    private var initialized = false

    fun ensureLoaded(context: Context) {
        if (initialized) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _state.value = CustomerGroomingPreferences(
            paymentOption = DoorstepPaymentOption.fromCode(
                sp.getString("payment_option", DoorstepPaymentOption.CARD_POS_AT_DOOR.code).orEmpty()
            ),
            savedCardLabel = sp.getString("saved_card_label", "").orEmpty(),
            savedCardLast4 = sp.getString("saved_card_last4", "").orEmpty(),
            savedUpiId = sp.getString("saved_upi_id", "").orEmpty(),
            preferredHaircutStyle = sp.getString("preferred_cut_style", "Traditional Neat Side-Part").orEmpty(),
            preferredOilType = sp.getString("preferred_oil", "Cooling Navratna Oil").orEmpty(),
            quietSession = sp.getBoolean("quiet_session", false),
            floorSheetAndCapeRequired = sp.getBoolean("floor_sheet_cape", true),
            referencePhotoAttached = sp.getBoolean("ref_photo_attached", false),
            referencePhotoNote = sp.getString("ref_photo_note", "").orEmpty()
        )
        initialized = true
    }

    fun update(context: Context, transform: (CustomerGroomingPreferences) -> CustomerGroomingPreferences) {
        ensureLoaded(context)
        val updated = transform(_state.value)
        _state.value = updated
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("payment_option", updated.paymentOption.code)
            .putString("saved_card_label", updated.savedCardLabel.take(40))
            .putString("saved_card_last4", updated.savedCardLast4.filter { it.isDigit() }.takeLast(4))
            .putString("saved_upi_id", updated.savedUpiId.take(60))
            .putString("preferred_cut_style", updated.preferredHaircutStyle.take(60))
            .putString("preferred_oil", updated.preferredOilType.take(40))
            .putBoolean("quiet_session", updated.quietSession)
            .putBoolean("floor_sheet_cape", updated.floorSheetAndCapeRequired)
            .putBoolean("ref_photo_attached", updated.referencePhotoAttached)
            .putString("ref_photo_note", updated.referencePhotoNote.take(80))
            .apply()
    }
}
