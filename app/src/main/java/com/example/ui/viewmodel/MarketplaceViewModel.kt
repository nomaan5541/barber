package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLogEntry
import com.example.data.model.BarberProfile
import com.example.data.model.Booking
import com.example.data.model.BookingPrivateAddress
import com.example.data.model.CatalogService
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.ServiceArea
import com.example.data.model.SupportTicket
import com.example.data.model.UserAccount
import com.example.data.repository.MarketplaceRepository
import com.example.ui.customer.canonicalGroomingCatalog
import com.example.util.DetectedDoorLocation
import com.example.util.LocationFetchResult
import com.example.util.LocationHelper
import com.example.util.PendingDoorAddressDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ActivePortal {
    CUSTOMER,
    BARBER,
    ADMIN
}

data class MarketplaceUiState(
    val isLoadingAuth: Boolean = true,
    val isAuthenticated: Boolean = false,
    val uid: String? = null,
    val userAccount: UserAccount? = null,
    val activePortal: ActivePortal = ActivePortal.CUSTOMER,

    // Catalog & Partner Barbers
    val services: List<CatalogService> = emptyList(),
    val serviceAreas: List<ServiceArea> = emptyList(),
    val approvedBarbers: List<BarberProfile> = emptyList(),
    val ownBarberProfile: BarberProfile? = null,
    val platformConfig: PlatformConfig = PlatformConfig(),

    // Real-time GPS & Door Number Setup State
    val detectedLocation: DetectedDoorLocation? = null,
    val isFetchingLocation: Boolean = false,
    val locationStatusMessage: String? = null,
    val pendingDoorDraft: PendingDoorAddressDraft? = null,

    // Customer Booking Cart & Open-Dispatch Checkout State (No Manual Barber Selection)
    val selectedServiceIds: Set<String> = emptySet(),
    val selectedDateKey: String = LocalDate.now().toString(),
    val selectedStartHour: Long = 10L,
    val bookingMode: String = "ASAP",
    val bookingRecipientGender: String = "MALE",
    val bookingRecipientAge: Long = 26L,
    val preferLadyBarber: Boolean = false,
    val savedAddresses: List<CustomerAddress> = emptyList(),
    val selectedAddressId: String? = null,
    val customerBookings: List<Booking> = emptyList(),

    // Barber Open-Dispatch Queue & Assigned Jobs
    val openDispatchBookings: List<Booking> = emptyList(),
    val barberBookings: List<Booking> = emptyList(),
    val unlockedAddresses: Map<String, BookingPrivateAddress> = emptyMap(),

    // Support & Admin
    val supportTickets: List<SupportTicket> = emptyList(),
    val adminAllBookings: List<Booking> = emptyList(),
    val adminAllUsers: List<UserAccount> = emptyList(),
    val adminAllBarbers: List<BarberProfile> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),

    // Feedback & Loading
    val isBusy: Boolean = false,
    val snackbarMessage: String? = null
) {
    val requiresOnboarding: Boolean
        get() = isAuthenticated &&
            userAccount != null &&
            !userAccount.onboardingCompleted &&
            userAccount.role != "ADMIN"

    val selectedServices: List<CatalogService>
        get() {
            val catalog = services.ifEmpty { canonicalGroomingCatalog() }
            return catalog.filter { selectedServiceIds.contains(it.id) }
        }

    val serviceSubtotalPaise: Long
        get() = selectedServices.sumOf { it.pricePaise }

    val totalDurationMinutes: Long
        get() = selectedServices.sumOf { it.durationMinutes }

    val computedBookingFeePaise: Long
        get() = platformConfig.computeCustomerConfirmationFeePaise(serviceSubtotalPaise)

    val computedBarberCommissionPaise: Long
        get() = platformConfig.computeBarberCommissionPaise(serviceSubtotalPaise)
}

class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MarketplaceRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    val detectedLocation: StateFlow<DetectedDoorLocation?> = _uiState
        .map { it.detectedLocation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val isFetchingLocation: StateFlow<Boolean> = _uiState
        .map { it.isFetchingLocation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val streamJobs = mutableListOf<Job>()

    init {
        observeAuthenticationState()
    }

    private fun observeAuthenticationState() {
        viewModelScope.launch {
            repo.observeAuthUid().collectLatest { uid ->
                cancelActiveStreams()
                if (uid == null) {
                    _uiState.update {
                        it.copy(
                            isLoadingAuth = false,
                            isAuthenticated = false,
                            uid = null,
                            userAccount = null,
                            activePortal = ActivePortal.CUSTOMER,
                            savedAddresses = emptyList(),
                            customerBookings = emptyList(),
                            openDispatchBookings = emptyList(),
                            barberBookings = emptyList(),
                            unlockedAddresses = emptyMap(),
                            supportTickets = emptyList(),
                            adminAllBookings = emptyList(),
                            adminAllUsers = emptyList(),
                            adminAllBarbers = emptyList(),
                            auditLogs = emptyList()
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingAuth = true, isAuthenticated = true, uid = uid) }
                    val profileResult = repo.ensureSignedInProfile()
                    repo.ensureInitialSeedDataIfEmpty()

                    val initialAccount = profileResult.getOrNull()
                    val defaultPortal = when (initialAccount?.role) {
                        "ADMIN" -> ActivePortal.ADMIN
                        "BARBER" -> ActivePortal.BARBER
                        else -> ActivePortal.CUSTOMER
                    }
                    val defaultGender = initialAccount?.gender?.takeIf { it.isNotBlank() } ?: "MALE"
                    val defaultAge = initialAccount?.age?.takeIf { it > 0L } ?: 26L

                    _uiState.update { state ->
                        state.copy(
                            isLoadingAuth = false,
                            isAuthenticated = true,
                            uid = uid,
                            userAccount = initialAccount,
                            activePortal = defaultPortal,
                            bookingRecipientGender = defaultGender,
                            bookingRecipientAge = defaultAge,
                            preferLadyBarber = defaultGender == "FEMALE"
                        )
                    }
                    startAuthenticatedStreams(uid, initialAccount?.role == "ADMIN")
                }
            }
        }
    }

    private fun cancelActiveStreams() {
        streamJobs.forEach { it.cancel() }
        streamJobs.clear()
    }

    private fun startAuthenticatedStreams(uid: String, isAdminInitial: Boolean) {
        streamJobs += viewModelScope.launch {
            repo.observeUserAccount(uid).collectLatest { account ->
                val portalForRole = when (account?.role) {
                    "ADMIN" -> if (_uiState.value.activePortal == ActivePortal.CUSTOMER || _uiState.value.activePortal == ActivePortal.BARBER) {
                        _uiState.value.activePortal
                    } else {
                        ActivePortal.ADMIN
                    }
                    "BARBER" -> ActivePortal.BARBER
                    else -> ActivePortal.CUSTOMER
                }
                _uiState.update { state ->
                    state.copy(
                        userAccount = account,
                        activePortal = portalForRole,
                        bookingRecipientGender = if (state.selectedServiceIds.isEmpty() && !account?.gender.isNullOrBlank()) {
                            account!!.gender
                        } else {
                            state.bookingRecipientGender
                        },
                        bookingRecipientAge = if (state.selectedServiceIds.isEmpty() && (account?.age ?: 0L) > 0L) {
                            account!!.age
                        } else {
                            state.bookingRecipientAge
                        }
                    )
                }
            }
        }

        streamJobs += viewModelScope.launch {
            repo.observeActiveServices(includeInactiveForAdmin = isAdminInitial).collectLatest { list ->
                _uiState.update { it.copy(services = list) }
            }
        }

        streamJobs += viewModelScope.launch {
            repo.observeActiveServiceAreas(includeInactiveForAdmin = isAdminInitial).collectLatest { areas ->
                _uiState.update { it.copy(serviceAreas = areas) }
            }
        }

        streamJobs += viewModelScope.launch {
            repo.observeApprovedBarbers(includeAllForAdmin = isAdminInitial).collectLatest { barbers ->
                _uiState.update { it.copy(approvedBarbers = barbers) }
            }
        }

        streamJobs += viewModelScope.launch {
            repo.observeOwnBarberProfile(uid).collectLatest { own ->
                _uiState.update { it.copy(ownBarberProfile = own) }
            }
        }

        streamJobs += viewModelScope.launch {
            repo.observePlatformConfig().collectLatest { cfg ->
                _uiState.update { it.copy(platformConfig = cfg) }
            }
        }

        streamJobs += viewModelScope.launch {
            repo.observeCustomerAddresses(uid)
                .catch { Log.w("MarketplaceVM", "observeCustomerAddresses closed", it); emit(emptyList()) }
                .collectLatest { addrs ->
                    _uiState.update { state ->
                        val chosen = state.selectedAddressId?.takeIf { id -> addrs.any { it.id == id } }
                            ?: addrs.firstOrNull()?.id
                        state.copy(savedAddresses = addrs, selectedAddressId = chosen)
                    }
                }
        }

        streamJobs += viewModelScope.launch {
            repo.observeCustomerBookings(uid)
                .catch { Log.w("MarketplaceVM", "observeCustomerBookings closed", it); emit(emptyList()) }
                .collectLatest { bookings ->
                    _uiState.update { it.copy(customerBookings = bookings) }
                }
        }

        streamJobs += viewModelScope.launch {
            repo.observeOpenDispatchBookings()
                .catch { Log.w("MarketplaceVM", "observeOpenDispatchBookings closed", it); emit(emptyList()) }
                .collectLatest { openBookings ->
                    _uiState.update { it.copy(openDispatchBookings = openBookings) }
                }
        }

        streamJobs += viewModelScope.launch {
            repo.observeBarberBookings(uid)
                .catch { Log.w("MarketplaceVM", "observeBarberBookings closed", it); emit(emptyList()) }
                .collectLatest { jobs ->
                    _uiState.update { it.copy(barberBookings = jobs) }
                }
        }

        streamJobs += viewModelScope.launch {
            repo.observeSupportTickets(uid, isAdminInitial)
                .catch { Log.w("MarketplaceVM", "observeSupportTickets closed", it); emit(emptyList()) }
                .collectLatest { tickets ->
                    _uiState.update { it.copy(supportTickets = tickets) }
                }
        }

        if (isAdminInitial) {
            streamJobs += viewModelScope.launch {
                repo.observeAllBookingsForAdmin().collectLatest { all ->
                    _uiState.update { it.copy(adminAllBookings = all) }
                }
            }
            streamJobs += viewModelScope.launch {
                repo.observeAllUsersForAdmin().collectLatest { users ->
                    _uiState.update { it.copy(adminAllUsers = users) }
                }
            }
            streamJobs += viewModelScope.launch {
                repo.observeAllBarbersForAdmin().collectLatest { allBarbers ->
                    _uiState.update { it.copy(adminAllBarbers = allBarbers) }
                }
            }
            streamJobs += viewModelScope.launch {
                repo.observeAuditLogs().collectLatest { logs ->
                    _uiState.update { it.copy(auditLogs = logs) }
                }
            }
        }
    }

    // --- GPS Auto-Fetch & Door Number Draft Actions ---

    fun autoFetchDeviceLocation(silent: Boolean = false) {
        val context = getApplication<Application>().applicationContext
        if (!LocationHelper.hasLocationPermission(context)) {
            if (!silent) {
                _uiState.update {
                    it.copy(locationStatusMessage = "Allow Location access so your appointed barber can track to your doorstep.")
                }
            }
            return
        }
        if (_uiState.value.isFetchingLocation) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true, locationStatusMessage = null) }
            when (val res = LocationHelper.fetchCurrentDoorLocation(context)) {
                is LocationFetchResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isFetchingLocation = false,
                            detectedLocation = res.location,
                            locationStatusMessage = null
                        )
                    }
                }
                is LocationFetchResult.PermissionMissing -> {
                    _uiState.update {
                        it.copy(
                            isFetchingLocation = false,
                            locationStatusMessage = "Location permission required for live doorstep dispatch."
                        )
                    }
                }
                is LocationFetchResult.Unavailable -> {
                    _uiState.update {
                        it.copy(
                            isFetchingLocation = false,
                            locationStatusMessage = res.reason
                        )
                    }
                }
            }
        }
    }

    fun autoFetchLocationAndPromptDoor(
        @Suppress("UNUSED_PARAMETER") context: Context? = null,
        @Suppress("UNUSED_PARAMETER") promptForHomeAddressAfterFetch: Boolean = false
    ) {
        autoFetchDeviceLocation(silent = false)
    }

    fun seedInitialLocation(location: DetectedDoorLocation?) {
        if (location != null && _uiState.value.detectedLocation == null) {
            _uiState.update { it.copy(detectedLocation = location) }
        }
    }

    fun updatePinnedLocation(location: DetectedDoorLocation) {
        if (location.hasValidCoordinates) {
            _uiState.update {
                it.copy(
                    detectedLocation = location,
                    locationStatusMessage = null
                )
            }
        }
    }

    fun syncBarberHubLocation(location: DetectedDoorLocation, updateShopAddress: Boolean = false) {
        if (!location.hasValidCoordinates) return
        _uiState.update { it.copy(detectedLocation = location, locationStatusMessage = null) }
        if (_uiState.value.isAuthenticated && _uiState.value.ownBarberProfile != null) {
            viewModelScope.launch {
                repo.updateBarberProfileLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    shopAddress = if (updateShopAddress) location.streetArea else null
                )
            }
        }
    }

    fun confirmDoorAddressSetup(draft: PendingDoorAddressDraft, onSaved: (() -> Unit)? = null) {
        _uiState.update { it.copy(pendingDoorDraft = draft) }
        if (_uiState.value.isAuthenticated) {
            val matchedArea = _uiState.value.serviceAreas.firstOrNull {
                it.postalCodes.contains(draft.postalCode.trim())
            } ?: _uiState.value.serviceAreas.firstOrNull()
            saveNewAddress(
                label = draft.label,
                flatHouse = draft.combinedFlatHouse(),
                streetArea = draft.streetArea,
                landmark = draft.landmark,
                postalCode = draft.postalCode,
                matchedArea = matchedArea,
                instructions = draft.accessInstructions,
                latitude = draft.latitude,
                longitude = draft.longitude,
                onSuccess = onSaved
            )
        } else {
            onSaved?.invoke()
        }
    }

    // --- Role-Locked Portal & Onboarding ---

    fun switchPortal(portal: ActivePortal) {
        val currentRole = _uiState.value.userAccount?.role
        // Only ADMIN can inspect cross-portals; CUSTOMER and BARBER stay in their dedicated role experience
        if (currentRole == "ADMIN") {
            _uiState.update { it.copy(activePortal = portal) }
        }
    }

    fun completeOnboarding(
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
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val state = _uiState.value
            val lat = state.detectedLocation?.latitude ?: 0.0
            val lon = state.detectedLocation?.longitude ?: 0.0
            val catalogIds = state.services.ifEmpty { canonicalGroomingCatalog() }.map { it.id }

            val res = repo.completeUserOnboarding(
                fullName = fullName,
                phone = phone,
                dob = dob,
                age = age,
                gender = gender,
                selectedRole = selectedRole,
                servesLadies = servesLadies,
                servesMen = servesMen,
                experienceYears = experienceYears,
                upiId = upiId,
                primaryArea = primaryArea.ifBlank { state.detectedLocation?.localityName.orEmpty() },
                latitude = lat,
                longitude = lon,
                defaultServiceIds = catalogIds
            )
            _uiState.update { s ->
                val nextPortal = if (selectedRole == "BARBER") ActivePortal.BARBER else ActivePortal.CUSTOMER
                s.copy(
                    isBusy = false,
                    activePortal = if (res.isSuccess && s.userAccount?.role != "ADMIN") nextPortal else s.activePortal,
                    bookingRecipientGender = gender,
                    bookingRecipientAge = age,
                    preferLadyBarber = gender == "FEMALE",
                    snackbarMessage = res.fold(
                        onSuccess = {
                            if (selectedRole == "BARBER") {
                                "Welcome Partner! Your Barber Mode dashboard is live."
                            } else {
                                "Welcome to DoorCut! Ready to book your home appointment."
                            }
                        },
                        onFailure = { "Could not complete profile setup: ${it.localizedMessage}" }
                    )
                )
            }
            if (res.isSuccess) {
                onComplete()
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun toggleServiceInCart(serviceId: String) {
        _uiState.update { state ->
            val updated = if (state.selectedServiceIds.contains(serviceId)) {
                state.selectedServiceIds - serviceId
            } else {
                state.selectedServiceIds + serviceId
            }
            // Auto-check Ladies Styling service -> default Female + Lady Stylist preference
            val catalog = state.services.ifEmpty { canonicalGroomingCatalog() }
            val hasLadiesSvc = catalog.any { updated.contains(it.id) && it.category == "LADIES_STYLING" }
            state.copy(
                selectedServiceIds = updated,
                bookingRecipientGender = if (hasLadiesSvc) "FEMALE" else state.bookingRecipientGender,
                preferLadyBarber = if (hasLadiesSvc) true else state.preferLadyBarber
            )
        }
    }

    fun setBookingDateAndHour(dateKey: String, hour: Long) {
        _uiState.update { it.copy(selectedDateKey = dateKey, selectedStartHour = hour) }
    }

    fun setBookingMode(mode: String) {
        _uiState.update { it.copy(bookingMode = if (mode == "SCHEDULED") "SCHEDULED" else "ASAP") }
    }

    fun setRecipientPreferences(gender: String, age: Long, preferLadyBarber: Boolean) {
        _uiState.update {
            it.copy(
                bookingRecipientGender = gender,
                bookingRecipientAge = age.coerceIn(1L, 110L),
                preferLadyBarber = if (gender == "FEMALE") preferLadyBarber else false
            )
        }
    }

    fun selectAddress(addressId: String) {
        _uiState.update { it.copy(selectedAddressId = addressId) }
    }

    fun repeatCompletedBooking(pastBooking: Booking, onReadyToBook: () -> Unit) {
        val validIds = pastBooking.serviceIds.toSet()
        if (validIds.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    selectedServiceIds = validIds,
                    bookingRecipientGender = pastBooking.customerGender.ifBlank { state.bookingRecipientGender },
                    bookingRecipientAge = pastBooking.customerAge.takeIf { it > 0L } ?: state.bookingRecipientAge,
                    preferLadyBarber = pastBooking.preferLadyBarber
                )
            }
            onReadyToBook()
        }
    }

    fun signOut() {
        repo.signOut()
    }

    // --- Profile & Door Address Persistence ---

    fun updateProfileContact(name: String, phone: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repo.updateUserContact(name, phone)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = { "Profile updated" },
                        onFailure = { err -> "Failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun saveNewAddress(
        label: String,
        flatHouse: String,
        streetArea: String,
        landmark: String,
        postalCode: String,
        matchedArea: ServiceArea?,
        instructions: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repo.saveCustomerAddress(
                label = label,
                flatHouse = flatHouse,
                streetArea = streetArea,
                landmark = landmark,
                postalCode = postalCode,
                serviceArea = matchedArea,
                accessInstructions = instructions,
                isDefault = _uiState.value.savedAddresses.isEmpty(),
                latitude = latitude,
                longitude = longitude
            )
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    selectedAddressId = res.getOrNull() ?: state.selectedAddressId,
                    snackbarMessage = res.fold(
                        onSuccess = { "Door address & live GPS pin saved" },
                        onFailure = { err -> "Could not save address: ${err.localizedMessage}" }
                    )
                )
            }
            if (res.isSuccess) onSuccess?.invoke()
        }
    }

    fun saveAddress(
        label: String,
        flatHouse: String,
        streetArea: String,
        landmark: String,
        postalCode: String,
        accessInstructions: String,
        @Suppress("UNUSED_PARAMETER") isDefault: Boolean = false,
        areas: List<ServiceArea> = emptyList(),
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ) {
        val matched = areas.firstOrNull { it.postalCodes.contains(postalCode.trim()) }
            ?: areas.firstOrNull()
            ?: _uiState.value.serviceAreas.firstOrNull()
        saveNewAddress(
            label = label,
            flatHouse = flatHouse,
            streetArea = streetArea,
            landmark = landmark,
            postalCode = postalCode,
            matchedArea = matched,
            instructions = accessInstructions,
            latitude = latitude,
            longitude = longitude
        )
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            repo.deleteCustomerAddress(addressId)
        }
    }

    // --- Open-Dispatch Booking Confirmation (Customer Never Selects Barber Manually) ---

    fun confirmOpenDispatchBooking(
        customerPhone: String,
        customerNotes: String,
        bookingFeeTxRef: String,
        onBookingCreated: (String) -> Unit
    ) {
        val state = _uiState.value
        val servicesToBook = state.selectedServices

        var address = state.savedAddresses.firstOrNull { it.id == state.selectedAddressId }
            ?: state.savedAddresses.firstOrNull()

        if (servicesToBook.isEmpty()) {
            showMessage("Please select at least one grooming service.")
            return
        }

        val cleanPhone = customerPhone.trim().ifBlank { state.userAccount?.phone.orEmpty() }
        if (cleanPhone.length < 8) {
            showMessage("Please provide a valid phone number so your appointed barber can contact you.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            if (address == null && state.pendingDoorDraft != null) {
                val draft = state.pendingDoorDraft
                val matchedArea = state.serviceAreas.firstOrNull {
                    it.postalCodes.contains(draft.postalCode.trim())
                } ?: state.serviceAreas.firstOrNull()
                val savedId = repo.saveCustomerAddress(
                    label = draft.label,
                    flatHouse = draft.combinedFlatHouse(),
                    streetArea = draft.streetArea,
                    landmark = draft.landmark,
                    postalCode = draft.postalCode,
                    serviceArea = matchedArea,
                    accessInstructions = draft.accessInstructions,
                    isDefault = true,
                    latitude = draft.latitude,
                    longitude = draft.longitude
                ).getOrNull()
                address = CustomerAddress(
                    id = savedId.orEmpty(),
                    customerId = state.uid.orEmpty(),
                    label = draft.label,
                    flatHouse = draft.combinedFlatHouse(),
                    streetArea = draft.streetArea,
                    landmark = draft.landmark,
                    postalCode = draft.postalCode,
                    serviceAreaId = matchedArea?.id.orEmpty(),
                    serviceAreaName = matchedArea?.name ?: draft.streetArea,
                    accessInstructions = draft.accessInstructions,
                    isDefault = true,
                    latitude = draft.latitude,
                    longitude = draft.longitude
                )
            }

            val finalAddress = address
            if (finalAddress == null) {
                _uiState.update {
                    it.copy(isBusy = false, snackbarMessage = "Please confirm your flat/house address before booking.")
                }
                return@launch
            }

            if (state.userAccount?.phone.isNullOrBlank()) {
                repo.updateUserContact(state.userAccount?.name.orEmpty().ifBlank { repo.currentDisplayName() }, cleanPhone)
            }

            val area = state.serviceAreas.firstOrNull { it.id == finalAddress.serviceAreaId }
                ?: state.serviceAreas.firstOrNull { it.postalCodes.contains(finalAddress.postalCode) }

            val res = repo.createOpenDispatchBooking(
                customerName = state.userAccount?.name?.ifBlank { repo.currentDisplayName() } ?: repo.currentDisplayName(),
                customerPhone = cleanPhone,
                customerGender = state.bookingRecipientGender,
                customerAge = state.bookingRecipientAge,
                preferLadyBarber = state.preferLadyBarber,
                bookingMode = state.bookingMode,
                selectedServices = servicesToBook,
                address = finalAddress,
                serviceArea = area,
                dateKey = state.selectedDateKey,
                startHour = state.selectedStartHour,
                customerNotes = customerNotes,
                bookingFeeTxRef = bookingFeeTxRef,
                config = state.platformConfig
            )

            _uiState.update { current ->
                current.copy(
                    isBusy = false,
                    selectedServiceIds = if (res.isSuccess) emptySet() else current.selectedServiceIds,
                    snackbarMessage = res.fold(
                        onSuccess = {
                            if (current.preferLadyBarber) {
                                "Request live! Broadcasting to nearby verified Lady Hair Stylists."
                            } else {
                                "Request live! Broadcasting to nearby verified Barbers."
                            }
                        },
                        onFailure = { err -> err.localizedMessage ?: "Could not dispatch booking." }
                    )
                )
            }

            res.getOrNull()?.let { bookingId ->
                onBookingCreated(bookingId)
            }
        }
    }

    fun cancelBooking(booking: Booking, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repo.cancelBooking(booking, reason)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = { "Booking ${booking.bookingNumber} cancelled" },
                        onFailure = { err -> "Cancel failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun respondToExtraServiceRequest(booking: Booking, approve: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repo.customerRespondToAddOn(booking, approve, _uiState.value.platformConfig)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = { if (approve) "Extra service approved" else "Extra service declined" },
                        onFailure = { err -> "Failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun confirmOrDisputeDirectPayment(bookingId: String, confirmed: Boolean) {
        viewModelScope.launch {
            val res = repo.customerConfirmOrDisputeDirectPayment(bookingId, confirmed)
            _uiState.update {
                it.copy(
                    snackbarMessage = res.fold(
                        onSuccess = { if (confirmed) "Direct payment confirmed" else "Dispute reported to Support" },
                        onFailure = { err -> "Error: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun submitReview(booking: Booking, rating: Int, comment: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repo.submitReview(booking, rating, comment)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = { "Thank you for rating ${booking.barberName}!" },
                        onFailure = { err -> "Could not submit review: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    // --- Barber Open-Dispatch Claiming, Live GPS Streaming & Job Lifecycle ---

    fun barberClaimOpenBooking(booking: Booking, onClaimed: ((String) -> Unit)? = null) {
        val state = _uiState.value
        val profile = state.ownBarberProfile
        if (profile == null) {
            showMessage("Complete your Barber Partner profile first to accept customer requests.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val liveLat = state.detectedLocation?.latitude ?: profile.latitude
            val liveLon = state.detectedLocation?.longitude ?: profile.longitude
            val res = repo.barberClaimOpenBooking(
                bookingId = booking.id,
                barberProfile = profile,
                liveLat = liveLat,
                liveLon = liveLon
            )
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = { "Appointment ${booking.bookingNumber} accepted! Customer door address & phone unlocked." },
                        onFailure = { err -> err.localizedMessage ?: "Unable to accept request." }
                    )
                )
            }
            if (res.isSuccess) {
                loadProtectedAddressForJob(booking.id)
                onClaimed?.invoke(booking.id)
            }
        }
    }

    fun updateBarberLiveLocation(bookingId: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repo.barberUpdateLiveLocation(bookingId, latitude, longitude)
        }
    }

    fun applyOrUpdateBarberProfile(
        name: String,
        phone: String,
        gender: String = "MALE",
        servesLadies: Boolean = false,
        servesMen: Boolean = true,
        bio: String,
        experienceYears: Long,
        selectedServiceIds: List<String>,
        selectedAreaIds: List<String>,
        workStartHour: Long,
        workEndHour: Long,
        upiId: String,
        shopName: String = "",
        shopAddress: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        workingDays: List<String>? = null,
        breakStartHour: Long? = null,
        breakEndHour: Long? = null,
        travelBufferMinutes: Long? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val existing = _uiState.value.ownBarberProfile
            val lat = latitude?.takeIf { it != 0.0 }
                ?: _uiState.value.detectedLocation?.latitude?.takeIf { it != 0.0 }
                ?: existing?.latitude
                ?: 0.0
            val lon = longitude?.takeIf { it != 0.0 }
                ?: _uiState.value.detectedLocation?.longitude?.takeIf { it != 0.0 }
                ?: existing?.longitude
                ?: 0.0
            val res = repo.applyOrSaveBarberProfile(
                name = name,
                phone = phone,
                gender = gender,
                servesLadies = servesLadies,
                servesMen = servesMen,
                bio = bio,
                experienceYears = experienceYears,
                serviceIds = selectedServiceIds,
                serviceAreaIds = selectedAreaIds,
                workStartHour = workStartHour,
                workEndHour = workEndHour,
                upiId = upiId,
                shopName = shopName,
                shopAddress = shopAddress,
                latitude = lat,
                longitude = lon,
                workingDays = workingDays ?: existing?.workingDays ?: listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
                breakStartHour = breakStartHour ?: existing?.breakStartHour ?: 13L,
                breakEndHour = breakEndHour ?: existing?.breakEndHour ?: 14L,
                travelBufferMinutes = travelBufferMinutes ?: existing?.travelBufferMinutes ?: 15L
            )
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = { "Barber Partner profile & hub settings saved" },
                        onFailure = { err -> "Failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun toggleBarberOnline(online: Boolean) {
        viewModelScope.launch {
            val lat = _uiState.value.detectedLocation?.latitude ?: 0.0
            val lon = _uiState.value.detectedLocation?.longitude ?: 0.0
            repo.toggleBarberOnline(online, lat, lon)
        }
    }

    fun barberRespondToRequest(booking: Booking, accept: Boolean, reason: String = "") {
        if (booking.isOpenDispatch && accept) {
            barberClaimOpenBooking(booking)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repo.barberRespondToBooking(booking, accept, reason)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = { if (accept) "Appointment confirmed" else "Appointment declined" },
                        onFailure = { err -> "Failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun barberAdvanceTripStatus(bookingId: String, nextStatus: String) {
        viewModelScope.launch {
            val lat = _uiState.value.detectedLocation?.latitude ?: 0.0
            val lon = _uiState.value.detectedLocation?.longitude ?: 0.0
            val res = repo.barberUpdateTripStatus(bookingId, nextStatus, lat, lon)
            _uiState.update {
                it.copy(
                    snackbarMessage = res.fold(
                        onSuccess = { "Status updated to ${nextStatus.replace('_', ' ')}" },
                        onFailure = { err -> "Update failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun loadProtectedAddressForJob(bookingId: String) {
        viewModelScope.launch {
            repo.fetchProtectedBookingAddress(bookingId).getOrNull()?.let { priv ->
                _uiState.update { state ->
                    state.copy(unlockedAddresses = state.unlockedAddresses + (bookingId to priv))
                }
            }
        }
    }

    fun barberRequestExtraService(bookingId: String, service: CatalogService) {
        viewModelScope.launch {
            val res = repo.barberRequestExtraService(bookingId, service)
            _uiState.update {
                it.copy(
                    snackbarMessage = res.fold(
                        onSuccess = { "Sent ${service.name} approval request to customer" },
                        onFailure = { err -> "Request failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun barberFinishAndDeclareDirectPayment(
        bookingId: String,
        declaredStatus: String,
        method: String,
        amountPaise: Long,
        note: String,
        markCompleted: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repo.barberDeclareDirectPayment(
                bookingId = bookingId,
                declaredStatus = declaredStatus,
                method = method,
                amountPaise = amountPaise,
                referenceNote = note,
                markCompleted = markCompleted
            )
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = {
                            if (markCompleted) "Visit completed & direct payment logged"
                            else "Direct payment record saved"
                        },
                        onFailure = { err -> "Could not record payment: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    // --- Support Tickets ---

    fun raiseSupportTicket(
        creatorRole: String,
        bookingId: String,
        category: String,
        subject: String,
        description: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repo.createSupportTicket(creatorRole, bookingId, category, subject, description)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = { "Support ticket submitted" },
                        onFailure = { err -> "Could not submit ticket: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    // --- Admin Operations ---

    fun adminApproveOrSuspendBarber(barberId: String, status: String, reason: String) {
        viewModelScope.launch {
            val res = repo.adminSetBarberApproval(barberId, status, reason)
            _uiState.update {
                it.copy(
                    snackbarMessage = res.fold(
                        onSuccess = { "Barber status set to $status (audit logged)" },
                        onFailure = { err -> "Admin error: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun adminSaveService(
        id: String?,
        name: String,
        category: String,
        description: String,
        priceRupees: Long,
        durationMinutes: Long,
        isPackage: Boolean,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            val res = repo.adminUpsertService(
                id, name, category, description, priceRupees, durationMinutes, isPackage, isActive
            )
            _uiState.update {
                it.copy(
                    snackbarMessage = res.fold(
                        onSuccess = { "Service $name saved" },
                        onFailure = { err -> "Failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun adminSaveZone(
        id: String?,
        name: String,
        pinsCsv: String,
        travelRupees: Long,
        maxKm: Long,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            val res = repo.adminUpsertServiceArea(id, name, pinsCsv, travelRupees, maxKm, isActive)
            _uiState.update {
                it.copy(
                    snackbarMessage = res.fold(
                        onSuccess = { "Service area $name saved" },
                        onFailure = { err -> "Failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun adminOverrideBooking(
        booking: Booking,
        newStatus: String,
        newBarber: BarberProfile?,
        markBarberCommissionSettled: Boolean,
        reason: String
    ) {
        viewModelScope.launch {
            val res = repo.adminOverrideBooking(booking, newStatus, newBarber, markBarberCommissionSettled, reason)
            _uiState.update {
                it.copy(
                    snackbarMessage = res.fold(
                        onSuccess = { "Booking ${booking.bookingNumber} updated" },
                        onFailure = { err -> "Override failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun adminResolveTicket(ticketId: String, resolution: String) {
        viewModelScope.launch {
            val res = repo.adminResolveTicket(ticketId, resolution)
            _uiState.update {
                it.copy(
                    snackbarMessage = res.fold(
                        onSuccess = { "Ticket resolved" },
                        onFailure = { err -> "Failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun adminSavePlatformConfig(
        feeType: String,
        barberCommissionPercent: Long,
        barberFlatFeeRupees: Long,
        cutoffHours: Long,
        policyText: String
    ) {
        viewModelScope.launch {
            val res = repo.adminSavePlatformConfig(
                feeType = feeType,
                barberCommissionPercent = barberCommissionPercent,
                barberFlatFeeRupees = barberFlatFeeRupees,
                cutoffHours = cutoffHours,
                policyText = policyText
            )
            _uiState.update {
                it.copy(
                    snackbarMessage = res.fold(
                        onSuccess = { "Platform config updated" },
                        onFailure = { err -> "Failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun upcomingDateOptions(days: Int = 10): List<Pair<String, String>> {
        val labelFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.US)
        val today = LocalDate.now()
        return (0 until days).map { offset ->
            val d = today.plusDays(offset.toLong())
            val prefix = when (offset) {
                0 -> "Today · "
                1 -> "Tomorrow · "
                else -> ""
            }
            d.toString() to "$prefix${d.format(labelFormatter)}"
        }
    }

    fun barberSaveScheduleAndProfile(
        workStartHour: Long,
        workEndHour: Long,
        upiId: String,
        shopName: String,
        shopAddress: String,
        serviceIds: List<String>,
        servesLadies: Boolean? = null,
        servesMen: Boolean? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        workingDays: List<String>? = null,
        breakStartHour: Long? = null,
        breakEndHour: Long? = null,
        travelBufferMinutes: Long? = null,
        bio: String? = null,
        experienceYears: Long? = null,
        serviceAreaIds: List<String>? = null
    ) {
        val prof = _uiState.value.ownBarberProfile
        val account = _uiState.value.userAccount
        applyOrUpdateBarberProfile(
            name = prof?.name?.ifBlank { account?.name.orEmpty() } ?: account?.name.orEmpty(),
            phone = prof?.phone?.ifBlank { account?.phone.orEmpty() } ?: account?.phone.orEmpty(),
            gender = prof?.gender?.ifBlank { account?.gender ?: "MALE" } ?: (account?.gender ?: "MALE"),
            servesLadies = servesLadies ?: prof?.servesLadies ?: false,
            servesMen = servesMen ?: prof?.servesMen ?: true,
            bio = bio ?: prof?.bio.orEmpty().ifBlank { "Verified DoorCut Home Grooming Partner" },
            experienceYears = experienceYears ?: prof?.experienceYears ?: 3L,
            selectedServiceIds = serviceIds.ifEmpty {
                _uiState.value.services.ifEmpty { canonicalGroomingCatalog() }.map { it.id }
            },
            selectedAreaIds = serviceAreaIds?.takeIf { it.isNotEmpty() }
                ?: prof?.serviceAreaIds?.ifEmpty {
                    _uiState.value.serviceAreas.map { it.id }.ifEmpty { listOf("zone_central") }
                }
                ?: listOf("zone_central"),
            workStartHour = workStartHour,
            workEndHour = workEndHour,
            upiId = upiId,
            shopName = shopName,
            shopAddress = shopAddress,
            latitude = latitude,
            longitude = longitude,
            workingDays = workingDays,
            breakStartHour = breakStartHour,
            breakEndHour = breakEndHour,
            travelBufferMinutes = travelBufferMinutes
        )
    }

    fun adminBootstrapCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            repo.ensureInitialSeedDataIfEmpty()
            _uiState.update { it.copy(isBusy = false, snackbarMessage = "Catalog & service areas verified") }
        }
    }

    fun adminSetBarberStatus(barberId: String, status: String, reason: String) {
        adminApproveOrSuspendBarber(barberId, status, reason)
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
        @Suppress("UNUSED_PARAMETER") instructions: String = "",
        @Suppress("UNUSED_PARAMETER") sortOrder: Long = 1L
    ) {
        adminSaveService(
            id = serviceId,
            name = name,
            category = category,
            description = description,
            priceRupees = priceRupees,
            durationMinutes = durationMinutes,
            isPackage = isPackage,
            isActive = isActive
        )
    }

    fun adminResolveTicket(
        ticketId: String,
        @Suppress("UNUSED_PARAMETER") status: String,
        resolution: String
    ) {
        adminResolveTicket(ticketId, resolution)
    }

    fun adminOverrideBooking(
        booking: Booking,
        newStatus: String,
        @Suppress("UNUSED_PARAMETER") newFeeStatus: String,
        newDirectPaymentStatus: String,
        replacementBarber: BarberProfile?,
        reason: String
    ) {
        adminOverrideBooking(
            booking = booking,
            newStatus = newStatus,
            newBarber = replacementBarber,
            markBarberCommissionSettled = newDirectPaymentStatus == "CUSTOMER_CONFIRMED",
            reason = reason
        )
    }

    fun adminSavePlatformPolicy(
        bookingFeeType: String,
        fixedBookingFeeRupees: Long,
        @Suppress("UNUSED_PARAMETER") percentageBookingFeeBps: Long,
        barberCommissionBps: Long,
        cutoffHours: Long,
        @Suppress("UNUSED_PARAMETER") slotHoldMinutes: Long,
        policyText: String,
        @Suppress("UNUSED_PARAMETER") maintenanceMode: Boolean
    ) {
        adminSavePlatformConfig(
            feeType = bookingFeeType,
            barberCommissionPercent = if (barberCommissionBps > 0L) barberCommissionBps / 100L else 15L,
            barberFlatFeeRupees = fixedBookingFeeRupees,
            cutoffHours = cutoffHours,
            policyText = policyText
        )
    }

    fun adminSetUserStatus(userId: String, status: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repo.adminSetUserStatus(userId, status, reason)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = { "User account updated to $status" },
                        onFailure = { err -> "Failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    fun adminVerifyUpfrontBookingFee(booking: Booking, paymentReference: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repo.adminVerifyUpfrontBookingFee(booking, paymentReference)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    snackbarMessage = res.fold(
                        onSuccess = { "Verified booking fee for ${booking.bookingNumber}" },
                        onFailure = { err -> "Verification failed: ${err.localizedMessage}" }
                    )
                )
            }
        }
    }

    companion object {
        fun defaultTodayDateKey(): String = LocalDate.now().toString()
        fun defaultTomorrowDateKey(): String = LocalDate.now().plusDays(1L).toString()
    }
}
