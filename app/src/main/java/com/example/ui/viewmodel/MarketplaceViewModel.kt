package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLog
import com.example.data.model.BarberProfile
import com.example.data.model.Booking
import com.example.data.model.BookingPrivateAddress
import com.example.data.model.CatalogService
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.ServiceArea
import com.example.data.model.SlotLock
import com.example.data.model.SupportTicket
import com.example.data.model.UserAccount
import com.example.data.repository.MarketplaceRepository
import com.example.util.DetectedDoorLocation
import com.example.util.LocationFetchResult
import com.example.util.LocationHelper
import com.example.util.PendingDoorAddressDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ActivePortal {
    CUSTOMER, BARBER, ADMIN
}

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceViewModel(
    private val repository: MarketplaceRepository,
    val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _selectedPortalOverride = MutableStateFlow<ActivePortal?>(null)
    val selectedPortalOverride: StateFlow<ActivePortal?> = _selectedPortalOverride.asStateFlow()

    // Live GPS Location & Auto Door-Address Onboarding State
    private val _detectedLocation = MutableStateFlow<DetectedDoorLocation?>(null)
    val detectedLocation: StateFlow<DetectedDoorLocation?> = _detectedLocation.asStateFlow()

    private val _isFetchingLocation = MutableStateFlow(false)
    val isFetchingLocation: StateFlow<Boolean> = _isFetchingLocation.asStateFlow()

    private val _locationErrorMessage = MutableStateFlow<String?>(null)
    val locationErrorMessage: StateFlow<String?> = _locationErrorMessage.asStateFlow()

    private val _showDoorAddressPrompt = MutableStateFlow(false)
    val showDoorAddressPrompt: StateFlow<Boolean> = _showDoorAddressPrompt.asStateFlow()

    private val _hasAttemptedLoginLocationFetch = MutableStateFlow(false)
    val hasAttemptedLoginLocationFetch: StateFlow<Boolean> = _hasAttemptedLoginLocationFetch.asStateFlow()

    // Customer Checkout Draft State
    private val _selectedServiceIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedServiceIds: StateFlow<Set<String>> = _selectedServiceIds.asStateFlow()

    private val _selectedBarberId = MutableStateFlow<String?>(null)
    val selectedBarberId: StateFlow<String?> = _selectedBarberId.asStateFlow()

    private val _selectedAddressId = MutableStateFlow<String?>(null)
    val selectedAddressId: StateFlow<String?> = _selectedAddressId.asStateFlow()

    private val _selectedDateKey = MutableStateFlow(defaultTomorrowDateKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _selectedStartHour = MutableStateFlow<Long?>(null)
    val selectedStartHour: StateFlow<Long?> = _selectedStartHour.asStateFlow()

    // Protected Address cache for authorized booking views
    private val _protectedAddresses = MutableStateFlow<Map<String, BookingPrivateAddress>>(emptyMap())
    val protectedAddresses: StateFlow<Map<String, BookingPrivateAddress>> = _protectedAddresses.asStateFlow()

    val userAccountState: StateFlow<UiState<UserAccount>> = repository.observeUserAccount(currentUserId)
        .map<UserAccount?, UiState<UserAccount>> { acct ->
            if (acct != null) UiState.Success(acct) else UiState.Loading
        }
        .catch { e ->
            Log.w(TAG, "observeUserAccount error", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load account"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val isAdminFlow = userAccountState.map { state ->
        (state as? UiState.Success)?.data?.role == "ADMIN"
    }

    val activeServicesState: StateFlow<UiState<List<CatalogService>>> = repository.observeActiveServices()
        .map<List<CatalogService>, UiState<List<CatalogService>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "observeActiveServices error", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load catalog"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val serviceAreasState: StateFlow<List<ServiceArea>> = repository.observeActiveServiceAreas()
        .catch { e ->
            Log.w(TAG, "observeActiveServiceAreas error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val platformConfigState: StateFlow<PlatformConfig> = repository.observePlatformConfig()
        .catch { e ->
            Log.w(TAG, "observePlatformConfig error", e)
            emit(PlatformConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlatformConfig())

    val approvedBarbersState: StateFlow<List<BarberProfile>> = repository.observeApprovedBarbers()
        .catch { e ->
            Log.w(TAG, "observeApprovedBarbers error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val ownBarberProfileState: StateFlow<BarberProfile?> = repository.observeOwnBarberProfile(currentUserId)
        .catch { e ->
            Log.w(TAG, "observeOwnBarberProfile error", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val customerAddressesState: StateFlow<List<CustomerAddress>> = repository.observeCustomerAddresses(currentUserId)
        .catch { e ->
            Log.w(TAG, "observeCustomerAddresses error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeSlotLocksState: StateFlow<List<SlotLock>> = repository.observeActiveSlotLocks()
        .catch { e ->
            Log.w(TAG, "observeActiveSlotLocks error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customerBookingsState: StateFlow<List<Booking>> = repository.observeCustomerBookings(currentUserId)
        .catch { e ->
            Log.w(TAG, "observeCustomerBookings error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val barberBookingsState: StateFlow<List<Booking>> = repository.observeBarberBookings(currentUserId)
        .catch { e ->
            Log.w(TAG, "observeBarberBookings error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myTicketsState: StateFlow<List<SupportTicket>> = repository.observeMySupportTickets(currentUserId)
        .catch { e ->
            Log.w(TAG, "observeMySupportTickets error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Admin-scoped flows (only subscribed when user is ADMIN)
    val adminAllServicesState: StateFlow<List<CatalogService>> = isAdminFlow
        .flatMapLatest { isAdmin ->
            if (isAdmin) repository.observeAllServicesForAdmin() else flowOf(emptyList())
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminAllBarbersState: StateFlow<List<BarberProfile>> = isAdminFlow
        .flatMapLatest { isAdmin ->
            if (isAdmin) repository.observeAllBarbersForAdmin() else flowOf(emptyList())
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminAllBookingsState: StateFlow<List<Booking>> = isAdminFlow
        .flatMapLatest { isAdmin ->
            if (isAdmin) repository.observeAllBookingsForAdmin() else flowOf(emptyList())
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminAllUsersState: StateFlow<List<UserAccount>> = isAdminFlow
        .flatMapLatest { isAdmin ->
            if (isAdmin) repository.observeAllUsersForAdmin() else flowOf(emptyList())
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminAllTicketsState: StateFlow<List<SupportTicket>> = isAdminFlow
        .flatMapLatest { isAdmin ->
            if (isAdmin) repository.observeAllSupportTicketsForAdmin() else flowOf(emptyList())
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminAuditLogsState: StateFlow<List<AuditLog>> = isAdminFlow
        .flatMapLatest { isAdmin ->
            if (isAdmin) repository.observeAuditLogsForAdmin() else flowOf(emptyList())
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            try {
                val acct = repository.ensureUserAccount(
                    userId = currentUserId,
                    displayName = initialDisplayName,
                    email = initialEmail
                )
                repository.ensureCanonicalCatalogIfEmpty(isAdminUser = acct.role == "ADMIN")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to ensure user account", e)
                _statusBanner.value = e.localizedMessage ?: "Account initialization error"
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun switchPortal(portal: ActivePortal?) {
        _selectedPortalOverride.value = portal
    }

    // --- Auto-Fetch Customer GPS Location & Door Address Prompt ---

    fun seedInitialLocation(location: DetectedDoorLocation?) {
        if (location != null && _detectedLocation.value == null) {
            _detectedLocation.value = location
            _locationErrorMessage.value = null
            _hasAttemptedLoginLocationFetch.value = true
        }
    }

    fun markLoginLocationAttempted() {
        _hasAttemptedLoginLocationFetch.value = true
    }

    fun openDoorAddressPrompt() {
        _showDoorAddressPrompt.value = true
    }

    fun dismissDoorAddressPrompt() {
        _showDoorAddressPrompt.value = false
    }

    fun autoFetchLocationAndPromptDoor(
        context: Context,
        promptForHomeAddressAfterFetch: Boolean = true
    ) {
        if (_isFetchingLocation.value) return
        _hasAttemptedLoginLocationFetch.value = true
        viewModelScope.launch {
            _isFetchingLocation.value = true
            try {
                when (val res = LocationHelper.fetchCurrentDoorLocation(context.applicationContext)) {
                    is LocationFetchResult.Success -> {
                        _detectedLocation.value = res.location
                        _locationErrorMessage.value = null
                        if (promptForHomeAddressAfterFetch) {
                            _showDoorAddressPrompt.value = true
                        } else {
                            _statusBanner.value = "GPS coordinates locked: ${res.location.formattedCoordinates()} (${res.location.localityName})"
                        }
                    }
                    is LocationFetchResult.PermissionMissing -> {
                        _locationErrorMessage.value = "Location permission is required to read live GPS coordinates."
                        if (promptForHomeAddressAfterFetch) {
                            _showDoorAddressPrompt.value = true
                        }
                    }
                    is LocationFetchResult.Unavailable -> {
                        _locationErrorMessage.value = res.reason
                        if (promptForHomeAddressAfterFetch) {
                            _showDoorAddressPrompt.value = true
                        } else {
                            _statusBanner.value = res.reason
                        }
                    }
                }
            } finally {
                _isFetchingLocation.value = false
            }
        }
    }

    fun toggleServiceSelection(serviceId: String) {
        val current = _selectedServiceIds.value.toMutableSet()
        if (current.contains(serviceId)) current.remove(serviceId) else current.add(serviceId)
        _selectedServiceIds.value = current
        _selectedStartHour.value = null
    }

    fun ensureServiceSelected(serviceId: String) {
        val current = _selectedServiceIds.value.toMutableSet()
        current.add(serviceId)
        _selectedServiceIds.value = current
    }

    fun clearSelectedServices() {
        _selectedServiceIds.value = emptySet()
        _selectedBarberId.value = null
        _selectedStartHour.value = null
    }

    fun selectBarber(barberId: String?) {
        _selectedBarberId.value = barberId
        _selectedStartHour.value = null
    }

    fun selectAddress(addressId: String?) {
        _selectedAddressId.value = addressId
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
        _selectedStartHour.value = null
    }

    fun selectStartHour(hour: Long?) {
        _selectedStartHour.value = hour
    }

    fun upcomingDateOptions(): List<Pair<String, String>> {
        val fmtKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val fmtLabel = SimpleDateFormat("EEE, dd MMM", Locale.US)
        val cal = Calendar.getInstance()
        return (0..6).map { offset ->
            if (offset > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
            fmtKey.format(cal.time) to fmtLabel.format(cal.time)
        }
    }

    /**
     * Platform fee for customers is ₹0. Instead, the platform charges a commission from the Barber.
     */
    fun calculateBarberCommissionPaise(barberPayablePaise: Long, config: PlatformConfig): Long {
        return when (config.bookingFeeType) {
            "BARBER_FLAT_FEE" -> config.barberFlatFeePaise.coerceIn(0L, barberPayablePaise)
            else -> {
                val bps = if (config.barberCommissionBps > 0L) config.barberCommissionBps else 1500L
                ((barberPayablePaise * bps) / 10000L).coerceIn(0L, barberPayablePaise)
            }
        }
    }

    fun availableSlotsForBarber(
        barber: BarberProfile,
        dateKey: String,
        activeLocks: List<SlotLock>
    ): List<Long> {
        if (!barber.isOnline || barber.approvalStatus != "APPROVED") return emptyList()
        val lockedHours = activeLocks
            .filter { it.barberId == barber.barberId && it.dateKey == dateKey && (it.status == "HELD" || it.status == "CONFIRMED") }
            .map { it.hourSlot }
            .toSet()

        val slots = mutableListOf<Long>()
        for (h in barber.workStartHour until barber.workEndHour) {
            val isBreak = h >= barber.breakStartHour && h < barber.breakEndHour
            if (!isBreak && !lockedHours.contains(h)) {
                slots.add(h)
            }
        }
        return slots
    }

    fun loadProtectedAddress(bookingId: String) {
        if (_protectedAddresses.value.containsKey(bookingId)) return
        viewModelScope.launch {
            try {
                val priv = repository.getProtectedBookingAddress(bookingId)
                if (priv != null) {
                    _protectedAddresses.value = _protectedAddresses.value + (bookingId to priv)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Protected address not accessible: ${e.message}")
            }
        }
    }

    // --- Actions ---

    fun updateProfile(name: String, phone: String) = runAction("Profile updated") {
        repository.updateOwnProfile(name, phone)
    }

    fun applyAsBarber(
        name: String,
        phone: String,
        bio: String,
        experienceYears: Long,
        upiId: String,
        shopName: String,
        shopAddress: String,
        skillsText: String,
        serviceIds: List<String>,
        serviceAreaIds: List<String>
    ) = runAction("Barber & Shop application submitted for Admin approval") {
        val parsedSkills = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        repository.applyToBecomeBarber(
            name = name,
            phone = phone,
            bio = bio,
            experienceYears = experienceYears,
            upiId = upiId,
            shopName = shopName,
            shopAddress = shopAddress,
            skills = parsedSkills.ifEmpty { listOf("Precision Haircut", "Skin Fade", "Beard Sculpt") },
            serviceIds = serviceIds,
            serviceAreaIds = serviceAreaIds
        )
    }

    fun savePendingDoorDraft(
        draft: PendingDoorAddressDraft,
        areas: List<ServiceArea>
    ) {
        saveAddress(
            label = draft.label,
            flatHouse = draft.combinedFlatHouse(),
            streetArea = draft.streetArea,
            landmark = draft.landmark,
            postalCode = draft.postalCode,
            accessInstructions = draft.accessInstructions,
            isDefault = true,
            areas = areas,
            latitude = draft.latitude.takeIf { it != 0.0 } ?: _detectedLocation.value?.latitude ?: 0.0,
            longitude = draft.longitude.takeIf { it != 0.0 } ?: _detectedLocation.value?.longitude ?: 0.0
        )
    }

    fun saveAddress(
        label: String,
        flatHouse: String,
        streetArea: String,
        landmark: String,
        postalCode: String,
        accessInstructions: String,
        isDefault: Boolean,
        areas: List<ServiceArea>,
        latitude: Double = _detectedLocation.value?.latitude ?: 0.0,
        longitude: Double = _detectedLocation.value?.longitude ?: 0.0
    ) = runAction("Home door address & coordinates saved! Showing nearest barbers & shops.") {
        val cleanPin = postalCode.trim().ifBlank { "560038" }
        val matchedArea = areas.firstOrNull { it.postalCodes.contains(cleanPin) } ?: areas.firstOrNull()
        val areaId = matchedArea?.id ?: "zone_central"
        val areaName = matchedArea?.name ?: "Local Home Zone ($cleanPin)"
        val newId = repository.saveCustomerAddress(
            label = label,
            flatHouse = flatHouse,
            streetArea = streetArea,
            landmark = landmark,
            postalCode = cleanPin,
            serviceAreaId = areaId,
            serviceAreaName = areaName,
            accessInstructions = accessInstructions,
            isDefault = isDefault,
            latitude = latitude,
            longitude = longitude
        )
        _selectedAddressId.value = newId
        _showDoorAddressPrompt.value = false
    }

    fun deleteAddress(addressId: String) = runAction("Address removed") {
        repository.deleteCustomerAddress(addressId)
    }

    fun confirmZeroFeeBooking(
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
        policySnapshot: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val bookingId = repository.createBookingWithSlotLock(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    barber = barber,
                    selectedServices = selectedServices,
                    address = address,
                    travelChargePaise = travelChargePaise,
                    barberCommissionPaise = barberCommissionPaise,
                    dateKey = dateKey,
                    startHour = startHour,
                    customerNotes = customerNotes,
                    policySnapshot = policySnapshot
                )
                clearSelectedServices()
                _statusBanner.value = "Slot reserved with ₹0 booking fee! Request sent to ${barber.name}."
                onSuccess(bookingId)
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Booking failed"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun repeatBooking(booking: Booking) {
        _selectedServiceIds.value = booking.serviceIds.toSet()
        _selectedBarberId.value = booking.barberId
        _statusBanner.value = "Loaded services & barber from ${booking.bookingNumber}. Pick a date & slot!"
    }

    fun customerCancelBooking(booking: Booking, reason: String) = runAction("Booking cancelled & slot released") {
        repository.customerCancelBooking(booking, reason)
    }

    fun customerRespondExtraService(booking: Booking, approve: Boolean) =
        runAction(if (approve) "Extra service approved & barber payable updated" else "Extra service declined") {
            val bps = platformConfigState.value.barberCommissionBps.let { if (it > 0L) it else 1500L }
            repository.customerRespondToExtraService(booking, approve, bps)
        }

    fun customerVerifyDirectPayment(bookingId: String, confirmPaid: Boolean, disputeReason: String = "") =
        runAction(if (confirmPaid) "Direct payment confirmed!" else "Payment dispute escalated to Admin") {
            repository.customerVerifyDirectPayment(bookingId, confirmPaid, disputeReason)
        }

    fun submitReview(booking: Booking, customerName: String, rating: Long, comment: String) =
        runAction("Thank you! Your review has been published.") {
            repository.submitCustomerReview(booking, customerName, rating, comment)
        }

    fun createSupportTicket(
        creatorRole: String,
        bookingId: String,
        category: String,
        subject: String,
        description: String
    ) = runAction("Support ticket submitted") {
        repository.createSupportTicket(creatorRole, bookingId, category, subject, description)
    }

    // --- Barber Actions ---

    fun barberRespond(booking: Booking, accept: Boolean, reason: String = "") =
        runAction(if (accept) "Appointment accepted & locked!" else "Request rejected & slot freed") {
            repository.barberRespondToBooking(booking, accept, reason)
        }

    fun barberAdvanceTripStatus(bookingId: String, nextStatus: String) =
        runAction("Status updated to $nextStatus") {
            repository.barberUpdateExecutionStatus(bookingId, nextStatus)
        }

    fun barberRequestExtraService(bookingId: String, service: CatalogService) =
        runAction("Sent ${service.name} approval request to customer") {
            repository.barberRequestExtraService(bookingId, service)
        }

    fun barberRecordDirectPayment(
        bookingId: String,
        declaredStatus: String,
        method: String,
        amountReceivedPaise: Long,
        referenceNote: String
    ) = runAction("Direct service payment recorded") {
        repository.barberRecordDirectPayment(bookingId, declaredStatus, method, amountReceivedPaise, referenceNote)
    }

    fun barberSaveSchedule(
        isOnline: Boolean,
        workStartHour: Long,
        workEndHour: Long,
        breakStartHour: Long,
        breakEndHour: Long,
        travelBufferMinutes: Long,
        upiId: String,
        shopName: String,
        shopAddress: String,
        skillsText: String,
        serviceIds: List<String>
    ) = runAction("Barber schedule, shop details & skills saved") {
        val parsedSkills = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        repository.updateBarberScheduleAndAvailability(
            isOnline = isOnline,
            workStartHour = workStartHour,
            workEndHour = workEndHour,
            breakStartHour = breakStartHour,
            breakEndHour = breakEndHour,
            travelBufferMinutes = travelBufferMinutes,
            upiId = upiId,
            shopName = shopName,
            shopAddress = shopAddress,
            skills = parsedSkills,
            serviceIds = serviceIds
        )
    }

    // --- Admin Actions ---

    fun adminBootstrapCatalog() = runAction("Master catalog, partner barber shops & barber commission policy synced!") {
        repository.adminBootstrapMasterCatalog()
    }

    fun adminSetBarberStatus(barberId: String, status: String, reason: String) =
        runAction("Barber status updated to $status (Audited)") {
            repository.adminSetBarberApproval(barberId, status, reason)
        }

    fun adminSaveService(
        serviceId: String?,
        name: String,
        category: String,
        description: String,
        priceRupees: Long,
        durationMinutes: Long,
        isActive: Boolean,
        isPackage: Boolean,
        instructions: String,
        sortOrder: Long
    ) = runAction("Catalog service saved (existing bookings keep snapshotted prices)") {
        repository.adminSaveService(
            serviceId = serviceId,
            name = name,
            category = category,
            description = description,
            pricePaise = priceRupees * 100L,
            durationMinutes = durationMinutes,
            isActive = isActive,
            isPackage = isPackage,
            instructions = instructions,
            sortOrder = sortOrder
        )
    }

    fun adminSaveBarberCommissionPolicy(
        commissionMode: String,
        barberCommissionBps: Long,
        barberFlatFeeRupees: Long,
        cutoffHours: Long,
        slotHoldMinutes: Long,
        policyText: String,
        maintenanceMode: Boolean
    ) = runAction("Barber commission & cancellation policy updated") {
        repository.adminUpdatePlatformConfig(
            bookingFeeType = commissionMode,
            barberCommissionBps = barberCommissionBps,
            barberFlatFeePaise = barberFlatFeeRupees * 100L,
            freeCancellationCutoffHours = cutoffHours,
            slotHoldMinutes = slotHoldMinutes,
            policyText = policyText,
            maintenanceMode = maintenanceMode
        )
    }

    fun adminOverrideBooking(
        booking: Booking,
        newStatus: String,
        newFeeStatus: String,
        newDirectPaymentStatus: String,
        replacementBarber: BarberProfile?,
        reason: String
    ) = runAction("Booking intervention applied & logged to audit trail") {
        repository.adminResolveDisputeOrOverrideBooking(
            booking, newStatus, newFeeStatus, newDirectPaymentStatus, replacementBarber, reason
        )
    }

    fun adminResolveTicket(ticketId: String, newStatus: String, resolutionNote: String) =
        runAction("Support ticket updated") {
            repository.adminResolveSupportTicket(ticketId, newStatus, resolutionNote)
        }

    private fun runAction(successMsg: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                block()
                _statusBanner.value = successMsg
            } catch (e: Exception) {
                Log.e(TAG, "Action failed", e)
                _statusBanner.value = e.localizedMessage ?: "Operation failed"
            } finally {
                _isBusy.value = false
            }
        }
    }

    companion object {
        private const val TAG = "MarketplaceVM"

        private fun defaultTomorrowDateKey(): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }
    }
}
