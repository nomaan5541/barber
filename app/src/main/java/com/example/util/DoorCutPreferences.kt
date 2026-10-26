package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DoorCutLocalPrefsState(
    val favouriteServiceIds: Set<String> = setOf("svc_classic_cut", "svc_pkg_essential"),
    val favouriteBarberIds: Set<String> = setOf("barber_arman_master"),
    val selectedLanguage: String = "English",
    val notificationsEnabled: Boolean = true,
    val barberBlockedSlots: Set<String> = emptySet(),
    val barberUnavailableDates: Set<String> = emptySet()
)

/**
 * Lightweight local preferences store for UI favourites, language selection,
 * notification preferences, and barber local slot blocks.
 */
object DoorCutPreferences {
    private const val PREFS_NAME = "doorcut_user_prefs"
    private const val KEY_FAV_SERVICES = "fav_service_ids"
    private const val KEY_FAV_BARBERS = "fav_barber_ids"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_BLOCKED_SLOTS = "barber_blocked_slots"
    private const val KEY_UNAVAILABLE_DATES = "barber_unavailable_dates"

    private val _state = MutableStateFlow(DoorCutLocalPrefsState())
    val state: StateFlow<DoorCutLocalPrefsState> = _state.asStateFlow()
    private var initialized = false

    fun ensureLoaded(context: Context) {
        if (initialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasSavedServices = prefs.contains(KEY_FAV_SERVICES)
        val favServices = if (hasSavedServices) {
            prefs.getStringSet(KEY_FAV_SERVICES, emptySet())?.toSet() ?: emptySet()
        } else {
            setOf("svc_classic_cut", "svc_pkg_essential")
        }
        val hasSavedBarbers = prefs.contains(KEY_FAV_BARBERS)
        val favBarbers = if (hasSavedBarbers) {
            prefs.getStringSet(KEY_FAV_BARBERS, emptySet())?.toSet() ?: emptySet()
        } else {
            setOf("barber_arman_master")
        }
        val lang = prefs.getString(KEY_LANGUAGE, "English") ?: "English"
        val notif = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        val blockedSlots = prefs.getStringSet(KEY_BLOCKED_SLOTS, emptySet())?.toSet() ?: emptySet()
        val unavailableDates = prefs.getStringSet(KEY_UNAVAILABLE_DATES, emptySet())?.toSet() ?: emptySet()

        _state.value = DoorCutLocalPrefsState(
            favouriteServiceIds = favServices,
            favouriteBarberIds = favBarbers,
            selectedLanguage = lang,
            notificationsEnabled = notif,
            barberBlockedSlots = blockedSlots,
            barberUnavailableDates = unavailableDates
        )
        initialized = true
    }

    fun toggleFavouriteService(context: Context, serviceId: String) {
        ensureLoaded(context)
        val current = _state.value.favouriteServiceIds.toMutableSet()
        if (current.contains(serviceId)) current.remove(serviceId) else current.add(serviceId)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_FAV_SERVICES, current)
            .apply()
        _state.value = _state.value.copy(favouriteServiceIds = current)
    }

    fun toggleFavouriteBarber(context: Context, barberId: String) {
        ensureLoaded(context)
        val current = _state.value.favouriteBarberIds.toMutableSet()
        if (current.contains(barberId)) current.remove(barberId) else current.add(barberId)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_FAV_BARBERS, current)
            .apply()
        _state.value = _state.value.copy(favouriteBarberIds = current)
    }

    fun setLanguage(context: Context, language: String) {
        ensureLoaded(context)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
        _state.value = _state.value.copy(selectedLanguage = language)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        ensureLoaded(context)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS, enabled)
            .apply()
        _state.value = _state.value.copy(notificationsEnabled = enabled)
    }

    fun toggleBarberBlockedSlot(context: Context, dateKey: String, hour: Long) {
        ensureLoaded(context)
        val key = "${dateKey}_$hour"
        val current = _state.value.barberBlockedSlots.toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_BLOCKED_SLOTS, current)
            .apply()
        _state.value = _state.value.copy(barberBlockedSlots = current)
    }

    fun toggleBarberUnavailableDate(context: Context, dateKey: String) {
        ensureLoaded(context)
        val current = _state.value.barberUnavailableDates.toMutableSet()
        if (current.contains(dateKey)) current.remove(dateKey) else current.add(dateKey)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_UNAVAILABLE_DATES, current)
            .apply()
        _state.value = _state.value.copy(barberUnavailableDates = current)
    }

    fun localizedGreeting(language: String): String = when (language) {
        "Hindi" -> "शानदार लुक यहीं से शुरू होता है।"
        "Telugu" -> "స్టైలిష్ లుక్ ఇక్కడే మొదలవుతుంది."
        else -> "Looking sharp starts here."
    }
}
