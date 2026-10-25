package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UserAccount
import com.example.data.repository.MarketplaceRepository
import com.example.ui.admin.AdminBarbersManagementScreen
import com.example.ui.admin.AdminBookingsAndDisputesScreen
import com.example.ui.admin.AdminCatalogManagementScreen
import com.example.ui.admin.AdminOverviewScreen
import com.example.ui.admin.AdminPoliciesAndAuditScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.performSignOut
import com.example.ui.barber.BarberEarningsScreen
import com.example.ui.barber.BarberJobsConsoleScreen
import com.example.ui.barber.BarberScheduleAndServicesScreen
import com.example.ui.components.DismissibleStatusBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.customer.AccountAndSupportScreen
import com.example.ui.customer.CustomerAddressesScreen
import com.example.ui.customer.CustomerBookingsScreen
import com.example.ui.customer.CustomerCartScreen
import com.example.ui.customer.CustomerDiscoverAndCheckoutScreen
import com.example.ui.customer.DoorNumberSetupScreen
import com.example.ui.customer.GuestAccountHubScreen
import com.example.ui.customer.GuestCartScreen
import com.example.ui.customer.ProtectedRouteAuthGate
import com.example.ui.customer.ServiceSelectionLandingScreen
import com.example.ui.customer.mergeWithCanonicalCatalog
import com.example.ui.navigation.AppRoutes
import com.example.ui.navigation.NavTabSpec
import com.example.ui.navigation.adminNavTabs
import com.example.ui.navigation.barberNavTabs
import com.example.ui.navigation.customerNavTabs
import com.example.ui.navigation.navigateSingleTopTo
import com.example.ui.theme.BrassGold
import com.example.ui.theme.DoorCutTheme
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.viewmodel.ActivePortal
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.DetectedDoorLocation
import com.example.util.DoorCutNotificationHelper
import com.example.util.LocationFetchResult
import com.example.util.LocationHelper
import com.example.util.PendingDoorAddressDraft
import com.example.util.PhonePermissionsHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DoorCutNotificationHelper.ensureChannel(this)
        enableEdgeToEdge()
        setContent {
            DoorCutTheme {
                DoorCutRootNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Root Jetpack Compose Navigation host for DoorCut.
 * Sets `AppRoutes.ROUTE_SERVICE_SELECTION` ('Service Selection') as the landing page (`startDestination`)
 * while automatically detecting GPS coordinates via Google Location Services on app launch.
 */
@Composable
fun DoorCutRootNavigation(
    auth: FirebaseAuth = Firebase.auth,
    requestLocationOnLaunch: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var launchLocation by remember { mutableStateOf<DetectedDoorLocation?>(null) }
    var isLaunchFetchingLocation by remember { mutableStateOf(false) }
    var launchLocationError by remember { mutableStateOf<String?>(null) }
    var hasLocationPermissionState by remember {
        mutableStateOf(LocationHelper.hasLocationPermission(context))
    }
    var hasRequestedLaunchPermission by rememberSaveable { mutableStateOf(false) }
    var pendingDoorDraft by remember { mutableStateOf<PendingDoorAddressDraft?>(null) }
    var guestSelectedServiceIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun triggerLaunchCoordinateFetch() {
        if (isLaunchFetchingLocation) return
        scope.launch {
            isLaunchFetchingLocation = true
            try {
                when (val result = LocationHelper.fetchCurrentDoorLocation(context)) {
                    is LocationFetchResult.Success -> {
                        launchLocation = result.location
                        launchLocationError = null
                    }
                    is LocationFetchResult.PermissionMissing -> {
                        hasLocationPermissionState = false
                        launchLocationError = "Grant location access so Google Location Services can detect your coordinates."
                    }
                    is LocationFetchResult.Unavailable -> {
                        launchLocationError = result.reason
                    }
                }
            } finally {
                isLaunchFetchingLocation = false
            }
        }
    }

    val launchPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermissionState = granted || LocationHelper.hasLocationPermission(context)
        if (hasLocationPermissionState) {
            triggerLaunchCoordinateFetch()
        } else {
            launchLocationError = "Location permission skipped. Enter your specific house or door number anytime."
        }
    }

    LaunchedEffect(requestLocationOnLaunch) {
        if (!requestLocationOnLaunch) return@LaunchedEffect
        hasLocationPermissionState = LocationHelper.hasLocationPermission(context)
        if (hasLocationPermissionState) {
            triggerLaunchCoordinateFetch()
        }
        val startupPerms = PhonePermissionsHelper.startupPermissionsArray()
        val snap = PhonePermissionsHelper.inspectPermissions(context)
        if ((!snap.hasAnyLocation || !snap.hasNotifications) && !hasRequestedLaunchPermission) {
            hasRequestedLaunchPermission = true
            launchPermissionLauncher.launch(startupPerms)
        }
    }

    val requestOrRefreshCoordinates: () -> Unit = {
        hasLocationPermissionState = LocationHelper.hasLocationPermission(context)
        if (hasLocationPermissionState) {
            triggerLaunchCoordinateFetch()
        } else {
            launchPermissionLauncher.launch(PhonePermissionsHelper.startupPermissionsArray())
        }
    }

    val user = currentUser
    if (user == null) {
        GuestNavigationHost(
            launchLocation = launchLocation,
            isLaunchFetchingLocation = isLaunchFetchingLocation,
            hasLocationPermission = hasLocationPermissionState,
            launchLocationError = launchLocationError,
            pendingDoorDraft = pendingDoorDraft,
            selectedServiceIds = guestSelectedServiceIds,
            onToggleService = { serviceId ->
                val updated = guestSelectedServiceIds.toMutableSet()
                if (updated.contains(serviceId)) updated.remove(serviceId) else updated.add(serviceId)
                guestSelectedServiceIds = updated
            },
            onClearSelections = { guestSelectedServiceIds = emptySet() },
            onRequestCoordinates = requestOrRefreshCoordinates,
            onSaveDoorDraft = { draft -> pendingDoorDraft = draft }
        )
    } else {
        AuthenticatedNavigationHost(
            currentUserId = user.uid,
            displayName = user.displayName ?: "DoorCut Member",
            email = user.email ?: "",
            launchLocation = launchLocation,
            isLaunchFetchingLocation = isLaunchFetchingLocation,
            hasLocationPermission = hasLocationPermissionState,
            launchLocationError = launchLocationError,
            pendingDoorDraft = pendingDoorDraft,
            guestSelectedServiceIds = guestSelectedServiceIds,
            onRequestLaunchCoordinates = requestOrRefreshCoordinates,
            onConsumePendingDoorDraft = { pendingDoorDraft = null },
            onConsumeGuestSelections = { guestSelectedServiceIds = emptySet() }
        )
    }
}

@Composable
private fun GuestNavigationHost(
    launchLocation: DetectedDoorLocation?,
    isLaunchFetchingLocation: Boolean,
    hasLocationPermission: Boolean,
    launchLocationError: String?,
    pendingDoorDraft: PendingDoorAddressDraft?,
    selectedServiceIds: Set<String>,
    onToggleService: (String) -> Unit,
    onClearSelections: () -> Unit,
    onRequestCoordinates: () -> Unit,
    onSaveDoorDraft: (PendingDoorAddressDraft) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.ROUTE_SERVICE_SELECTION

    val hideChrome = currentRoute == AppRoutes.ROUTE_DOOR_SETUP || currentRoute == AppRoutes.ROUTE_AUTH

    DoorCutScaffoldChrome(
        portalSubtitle = "AT-HOME ATELIER",
        showChrome = !hideChrome,
        currentRoute = currentRoute,
        navTabs = customerNavTabs,
        cartBadgeCount = selectedServiceIds.size,
        onSelectTab = { route -> navController.navigateSingleTopTo(route) },
        topBarActions = {
            TextButton(
                onClick = { navController.navigate(AppRoutes.ROUTE_AUTH) },
                modifier = Modifier
                    .height(48.dp)
                    .padding(end = 8.dp)
                    .testTag("top_bar_sign_in_button")
            ) {
                Text(
                    text = stringResource(R.string.btn_sign_in_short),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = AppRoutes.ROUTE_SERVICE_SELECTION,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppRoutes.ROUTE_SERVICE_SELECTION) {
                ServiceSelectionLandingScreen(
                    detectedLocation = launchLocation,
                    isFetchingLocation = isLaunchFetchingLocation,
                    pendingDoorDraft = pendingDoorDraft,
                    selectedServiceIds = selectedServiceIds,
                    onToggleService = onToggleService,
                    onOpenDoorSetup = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                    onRefreshGps = onRequestCoordinates,
                    onProceedToSignIn = { navController.navigateSingleTopTo(AppRoutes.ROUTE_CART) },
                    onAuthSuccess = {},
                    onNavigateToCart = { navController.navigateSingleTopTo(AppRoutes.ROUTE_CART) }
                )
            }

            composable(AppRoutes.ROUTE_DOOR_SETUP) {
                BackHandler { navController.popBackStack() }
                DoorNumberSetupScreen(
                    detectedLocation = launchLocation,
                    isFetchingLocation = isLaunchFetchingLocation,
                    hasLocationPermission = hasLocationPermission,
                    locationErrorMessage = launchLocationError,
                    initialDraft = pendingDoorDraft,
                    allowDismiss = true,
                    primaryButtonLabel = stringResource(R.string.door_btn_confirm_save),
                    onRequestDetectCoordinates = onRequestCoordinates,
                    onConfirmDoorAddress = { draft ->
                        onSaveDoorDraft(draft)
                        navController.popBackStack()
                    },
                    onSkipOrClose = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.ROUTE_BOOKINGS) {
                ProtectedRouteAuthGate(
                    title = stringResource(R.string.bookings_signin_gate_title),
                    subtitle = stringResource(R.string.bookings_signin_gate_subtitle),
                    onAuthSuccess = {}
                )
            }

            composable(AppRoutes.ROUTE_CART) {
                GuestCartScreen(
                    detectedLocation = launchLocation,
                    pendingDoorDraft = pendingDoorDraft,
                    selectedServiceIds = selectedServiceIds,
                    onToggleService = onToggleService,
                    onClearCart = onClearSelections,
                    onBrowseServices = { navController.navigateSingleTopTo(AppRoutes.ROUTE_SERVICE_SELECTION) },
                    onOpenDoorSetup = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                    onAuthSuccess = {}
                )
            }

            composable(AppRoutes.ROUTE_ADDRESSES) {
                DoorNumberSetupScreen(
                    detectedLocation = launchLocation,
                    isFetchingLocation = isLaunchFetchingLocation,
                    hasLocationPermission = hasLocationPermission,
                    locationErrorMessage = launchLocationError,
                    initialDraft = pendingDoorDraft,
                    allowDismiss = true,
                    primaryButtonLabel = stringResource(R.string.door_btn_confirm_save),
                    onRequestDetectCoordinates = onRequestCoordinates,
                    onConfirmDoorAddress = { draft ->
                        onSaveDoorDraft(draft)
                        navController.navigateSingleTopTo(AppRoutes.ROUTE_SERVICE_SELECTION)
                    },
                    onSkipOrClose = {
                        navController.navigateSingleTopTo(AppRoutes.ROUTE_SERVICE_SELECTION)
                    }
                )
            }

            composable(AppRoutes.ROUTE_PROFILE) {
                GuestAccountHubScreen(
                    detectedLocation = launchLocation,
                    pendingDoorDraft = pendingDoorDraft,
                    selectedServiceCount = selectedServiceIds.size,
                    onNavigateToServices = { navController.navigateSingleTopTo(AppRoutes.ROUTE_SERVICE_SELECTION) },
                    onNavigateToCart = { navController.navigateSingleTopTo(AppRoutes.ROUTE_CART) },
                    onNavigateToDoorSetup = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                    onRefreshGps = onRequestCoordinates,
                    onAuthSuccess = {}
                )
            }

            composable(AppRoutes.ROUTE_AUTH) {
                BackHandler { navController.popBackStack() }
                AuthScreen(
                    onAuthSuccess = {},
                    detectedLocation = launchLocation,
                    pendingDoorDraft = pendingDoorDraft,
                    onOpenDoorSetup = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedNavigationHost(
    currentUserId: String,
    displayName: String,
    email: String,
    launchLocation: DetectedDoorLocation?,
    isLaunchFetchingLocation: Boolean,
    hasLocationPermission: Boolean,
    launchLocationError: String?,
    pendingDoorDraft: PendingDoorAddressDraft?,
    guestSelectedServiceIds: Set<String>,
    onRequestLaunchCoordinates: () -> Unit,
    onConsumePendingDoorDraft: () -> Unit,
    onConsumeGuestSelections: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController: NavHostController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.ROUTE_SERVICE_SELECTION

    val viewModel: MarketplaceViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MarketplaceViewModel(
                    repository = MarketplaceRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email
                )
            }
        }
    )

    val accountState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val portalOverride by viewModel.selectedPortalOverride.collectAsStateWithLifecycle()

    val activeServicesState by viewModel.activeServicesState.collectAsStateWithLifecycle()
    val serviceAreas by viewModel.serviceAreasState.collectAsStateWithLifecycle()
    val platformConfig by viewModel.platformConfigState.collectAsStateWithLifecycle()
    val approvedBarbers by viewModel.approvedBarbersState.collectAsStateWithLifecycle()
    val ownBarberProfile by viewModel.ownBarberProfileState.collectAsStateWithLifecycle()
    val customerAddresses by viewModel.customerAddressesState.collectAsStateWithLifecycle()
    val slotLocks by viewModel.activeSlotLocksState.collectAsStateWithLifecycle()
    val customerBookings by viewModel.customerBookingsState.collectAsStateWithLifecycle()
    val barberBookings by viewModel.barberBookingsState.collectAsStateWithLifecycle()
    val protectedAddresses by viewModel.protectedAddresses.collectAsStateWithLifecycle()
    val myTickets by viewModel.myTicketsState.collectAsStateWithLifecycle()

    val selectedServiceIds by viewModel.selectedServiceIds.collectAsStateWithLifecycle()
    val selectedBarberId by viewModel.selectedBarberId.collectAsStateWithLifecycle()
    val selectedAddressId by viewModel.selectedAddressId.collectAsStateWithLifecycle()
    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val selectedStartHour by viewModel.selectedStartHour.collectAsStateWithLifecycle()

    val vmDetectedLocation by viewModel.detectedLocation.collectAsStateWithLifecycle()
    val vmIsFetchingLocation by viewModel.isFetchingLocation.collectAsStateWithLifecycle()
    val vmLocationError by viewModel.locationErrorMessage.collectAsStateWithLifecycle()

    val adminAllServices by viewModel.adminAllServicesState.collectAsStateWithLifecycle()
    val adminAllBarbers by viewModel.adminAllBarbersState.collectAsStateWithLifecycle()
    val adminAllBookings by viewModel.adminAllBookingsState.collectAsStateWithLifecycle()
    val adminAllUsers by viewModel.adminAllUsersState.collectAsStateWithLifecycle()
    val adminAllTickets by viewModel.adminAllTicketsState.collectAsStateWithLifecycle()
    val adminAuditLogs by viewModel.adminAuditLogsState.collectAsStateWithLifecycle()

    LaunchedEffect(launchLocation) {
        viewModel.seedInitialLocation(launchLocation)
    }

    LaunchedEffect(guestSelectedServiceIds) {
        if (guestSelectedServiceIds.isNotEmpty()) {
            guestSelectedServiceIds.forEach { viewModel.ensureServiceSelected(it) }
            onConsumeGuestSelections()
        }
    }

    LaunchedEffect(pendingDoorDraft, serviceAreas) {
        val draft = pendingDoorDraft ?: return@LaunchedEffect
        viewModel.savePendingDoorDraft(draft = draft, areas = serviceAreas)
        onConsumePendingDoorDraft()
    }

    if (accountState is UiState.Error) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Filled.AdminPanelSettings,
                title = "Session Load Error",
                subtitle = (accountState as UiState.Error).message
            )
        }
        return
    }

    val userAccount = (accountState as? UiState.Success)?.data ?: UserAccount(
        id = currentUserId,
        userId = currentUserId,
        name = displayName,
        email = email,
        role = "CUSTOMER",
        status = "ACTIVE"
    )

    if (userAccount.status == "SUSPENDED") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Filled.AdminPanelSettings,
                title = "Account Suspended",
                subtitle = "Your DoorCut account has been suspended by platform administration. Please contact support."
            )
        }
        return
    }

    // Service Selection is the default landing page; Barbers/Admins can switch portals in the TopAppBar
    val currentPortal = portalOverride ?: ActivePortal.CUSTOMER

    val activeTabs = when (currentPortal) {
        ActivePortal.CUSTOMER -> customerNavTabs
        ActivePortal.BARBER -> barberNavTabs
        ActivePortal.ADMIN -> adminNavTabs
    }

    val portalSubtitle = when (currentPortal) {
        ActivePortal.CUSTOMER -> "AT-HOME ATELIER"
        ActivePortal.BARBER -> "BARBER STUDIO"
        ActivePortal.ADMIN -> "OPERATIONS"
    }

    val hideChrome = currentRoute == AppRoutes.ROUTE_DOOR_SETUP

    DoorCutScaffoldChrome(
        portalSubtitle = portalSubtitle,
        showChrome = !hideChrome,
        currentRoute = currentRoute,
        navTabs = activeTabs,
        cartBadgeCount = selectedServiceIds.size,
        onSelectTab = { route -> navController.navigateSingleTopTo(route) },
        statusBanner = statusBanner,
        onDismissBanner = { viewModel.clearBanner() },
        topBarActions = {
            if (userAccount.role == "ADMIN" || userAccount.role == "BARBER") {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PortalSwitchPill(
                            label = "Customer",
                            selected = currentPortal == ActivePortal.CUSTOMER,
                            onClick = {
                                viewModel.switchPortal(ActivePortal.CUSTOMER)
                                navController.navigateSingleTopTo(AppRoutes.ROUTE_SERVICE_SELECTION)
                            },
                            modifier = Modifier.testTag("portal_switch_customer")
                        )
                        if (userAccount.role == "ADMIN") {
                            PortalSwitchPill(
                                label = "Admin",
                                selected = currentPortal == ActivePortal.ADMIN,
                                onClick = {
                                    viewModel.switchPortal(ActivePortal.ADMIN)
                                    navController.navigateSingleTopTo(AppRoutes.ROUTE_ADMIN_OVERVIEW)
                                },
                                modifier = Modifier.testTag("portal_switch_admin")
                            )
                            if (ownBarberProfile != null) {
                                PortalSwitchPill(
                                    label = "Barber",
                                    selected = currentPortal == ActivePortal.BARBER,
                                    onClick = {
                                        viewModel.switchPortal(ActivePortal.BARBER)
                                        navController.navigateSingleTopTo(AppRoutes.ROUTE_BARBER_JOBS)
                                    }
                                )
                            }
                        } else {
                            PortalSwitchPill(
                                label = "Barber",
                                selected = currentPortal == ActivePortal.BARBER,
                                onClick = {
                                    viewModel.switchPortal(ActivePortal.BARBER)
                                    navController.navigateSingleTopTo(AppRoutes.ROUTE_BARBER_JOBS)
                                },
                                modifier = Modifier.testTag("portal_switch_barber")
                            )
                        }
                    }
                }
            }
        }
    ) {
        val catalogList = remember(activeServicesState) {
            mergeWithCanonicalCatalog((activeServicesState as? UiState.Success)?.data ?: emptyList())
        }

        NavHost(
            navController = navController,
            startDestination = AppRoutes.ROUTE_SERVICE_SELECTION,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppRoutes.ROUTE_SERVICE_SELECTION) {
                CustomerDiscoverAndCheckoutScreen(
                    viewModel = viewModel,
                    userAccount = userAccount,
                    servicesState = activeServicesState,
                    approvedBarbers = approvedBarbers,
                    addresses = customerAddresses,
                    serviceAreas = serviceAreas,
                    slotLocks = slotLocks,
                    platformConfig = platformConfig,
                    selectedServiceIds = selectedServiceIds,
                    selectedBarberId = selectedBarberId,
                    selectedAddressId = selectedAddressId,
                    selectedDateKey = selectedDateKey,
                    selectedStartHour = selectedStartHour,
                    isBusy = isBusy,
                    onNavigateToDoorSetup = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                    onNavigateToBookings = { navController.navigateSingleTopTo(AppRoutes.ROUTE_BOOKINGS) },
                    onNavigateToAddresses = { navController.navigateSingleTopTo(AppRoutes.ROUTE_ADDRESSES) },
                    onNavigateToCart = { navController.navigateSingleTopTo(AppRoutes.ROUTE_CART) }
                )
            }

            composable(AppRoutes.ROUTE_DOOR_SETUP) {
                BackHandler { navController.popBackStack() }
                val defaultAddr = customerAddresses.firstOrNull { it.isDefault } ?: customerAddresses.firstOrNull()
                val seededDraft = remember(defaultAddr) {
                    defaultAddr?.let {
                        PendingDoorAddressDraft(
                            label = it.label,
                            doorNumber = it.flatHouse,
                            streetArea = it.streetArea,
                            landmark = it.landmark,
                            postalCode = it.postalCode,
                            accessInstructions = it.accessInstructions,
                            latitude = it.latitude,
                            longitude = it.longitude
                        )
                    }
                }
                DoorNumberSetupScreen(
                    detectedLocation = vmDetectedLocation ?: launchLocation,
                    isFetchingLocation = vmIsFetchingLocation || isLaunchFetchingLocation,
                    hasLocationPermission = hasLocationPermission || LocationHelper.hasLocationPermission(context),
                    locationErrorMessage = vmLocationError ?: launchLocationError,
                    initialDraft = seededDraft,
                    isBusy = isBusy,
                    allowDismiss = true,
                    onRequestDetectCoordinates = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.autoFetchLocationAndPromptDoor(context, promptForHomeAddressAfterFetch = false)
                        } else {
                            onRequestLaunchCoordinates()
                        }
                    },
                    onConfirmDoorAddress = { draft ->
                        viewModel.savePendingDoorDraft(draft = draft, areas = serviceAreas)
                        navController.popBackStack()
                    },
                    onSkipOrClose = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.ROUTE_BOOKINGS) {
                CustomerBookingsScreen(
                    viewModel = viewModel,
                    userAccount = userAccount,
                    bookings = customerBookings,
                    protectedAddresses = protectedAddresses,
                    isBusy = isBusy,
                    onRepeatBookingToDiscover = {
                        navController.navigateSingleTopTo(AppRoutes.ROUTE_CART)
                    }
                )
            }

            composable(AppRoutes.ROUTE_CART) {
                CustomerCartScreen(
                    viewModel = viewModel,
                    userAccount = userAccount,
                    allServices = catalogList,
                    approvedBarbers = approvedBarbers,
                    addresses = customerAddresses,
                    serviceAreas = serviceAreas,
                    slotLocks = slotLocks,
                    platformConfig = platformConfig,
                    selectedServiceIds = selectedServiceIds,
                    selectedBarberId = selectedBarberId,
                    selectedAddressId = selectedAddressId,
                    selectedDateKey = selectedDateKey,
                    selectedStartHour = selectedStartHour,
                    isBusy = isBusy,
                    onBrowseServices = { navController.navigateSingleTopTo(AppRoutes.ROUTE_SERVICE_SELECTION) },
                    onOpenDoorSetup = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                    onNavigateToAddresses = { navController.navigateSingleTopTo(AppRoutes.ROUTE_ADDRESSES) },
                    onBookingConfirmed = { navController.navigateSingleTopTo(AppRoutes.ROUTE_BOOKINGS) }
                )
            }

            composable(AppRoutes.ROUTE_ADDRESSES) {
                CustomerAddressesScreen(
                    viewModel = viewModel,
                    addresses = customerAddresses,
                    serviceAreas = serviceAreas,
                    isBusy = isBusy
                )
            }

            composable(AppRoutes.ROUTE_PROFILE) {
                AccountAndSupportScreen(
                    viewModel = viewModel,
                    userAccount = userAccount,
                    bookings = customerBookings,
                    addresses = customerAddresses,
                    selectedServiceCount = selectedServiceIds.size,
                    tickets = myTickets,
                    allServices = catalogList,
                    serviceAreas = serviceAreas,
                    isBusy = isBusy,
                    onNavigateToServices = { navController.navigateSingleTopTo(AppRoutes.ROUTE_SERVICE_SELECTION) },
                    onNavigateToBookings = { navController.navigateSingleTopTo(AppRoutes.ROUTE_BOOKINGS) },
                    onNavigateToCart = { navController.navigateSingleTopTo(AppRoutes.ROUTE_CART) },
                    onNavigateToAddresses = { navController.navigateSingleTopTo(AppRoutes.ROUTE_ADDRESSES) },
                    onNavigateToDoorSetup = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                    onSwitchToBarberPortal = {
                        viewModel.switchPortal(ActivePortal.BARBER)
                        navController.navigateSingleTopTo(AppRoutes.ROUTE_BARBER_JOBS)
                    },
                    onSwitchToAdminPortal = {
                        viewModel.switchPortal(ActivePortal.ADMIN)
                        navController.navigateSingleTopTo(AppRoutes.ROUTE_ADMIN_OVERVIEW)
                    },
                    onSignOut = {
                        performSignOut(context = context, onSignOutComplete = {}, scope = scope)
                    }
                )
            }

            // Barber Studio Routes
            composable(AppRoutes.ROUTE_BARBER_JOBS) {
                BarberJobsConsoleScreen(
                    viewModel = viewModel,
                    barberProfile = ownBarberProfile,
                    bookings = barberBookings,
                    catalogServices = catalogList,
                    protectedAddresses = protectedAddresses,
                    isBusy = isBusy
                )
            }

            composable(AppRoutes.ROUTE_BARBER_SCHEDULE) {
                BarberScheduleAndServicesScreen(
                    viewModel = viewModel,
                    barberProfile = ownBarberProfile,
                    catalogServices = catalogList,
                    isBusy = isBusy
                )
            }

            composable(AppRoutes.ROUTE_BARBER_EARNINGS) {
                BarberEarningsScreen(bookings = barberBookings)
            }

            // Admin Operations Routes
            composable(AppRoutes.ROUTE_ADMIN_OVERVIEW) {
                AdminOverviewScreen(
                    viewModel = viewModel,
                    users = adminAllUsers,
                    barbers = adminAllBarbers,
                    services = adminAllServices,
                    bookings = adminAllBookings,
                    tickets = adminAllTickets,
                    isBusy = isBusy
                )
            }

            composable(AppRoutes.ROUTE_ADMIN_BARBERS) {
                AdminBarbersManagementScreen(
                    viewModel = viewModel,
                    barbers = adminAllBarbers,
                    isBusy = isBusy
                )
            }

            composable(AppRoutes.ROUTE_ADMIN_CATALOG) {
                AdminCatalogManagementScreen(
                    viewModel = viewModel,
                    services = adminAllServices,
                    isBusy = isBusy
                )
            }

            composable(AppRoutes.ROUTE_ADMIN_BOOKINGS) {
                AdminBookingsAndDisputesScreen(
                    viewModel = viewModel,
                    bookings = adminAllBookings,
                    approvedBarbers = approvedBarbers,
                    tickets = adminAllTickets,
                    isBusy = isBusy
                )
            }

            composable(AppRoutes.ROUTE_ADMIN_POLICIES) {
                AdminPoliciesAndAuditScreen(
                    viewModel = viewModel,
                    platformConfig = platformConfig,
                    auditLogs = adminAuditLogs,
                    isBusy = isBusy
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoorCutScaffoldChrome(
    portalSubtitle: String,
    showChrome: Boolean,
    currentRoute: String,
    navTabs: List<NavTabSpec>,
    onSelectTab: (String) -> Unit,
    cartBadgeCount: Int = 0,
    statusBanner: String? = null,
    onDismissBanner: () -> Unit = {},
    topBarActions: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showChrome) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(EspressoHeaderBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCut,
                                        contentDescription = stringResource(R.string.cd_brand_logo),
                                        tint = BrassGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = portalSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        actions = { topBarActions() }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                }
            }
        },
        bottomBar = {
            if (showChrome) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        navTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab.route) },
                                colors = navItemColors,
                                icon = {
                                    if (tab.route == AppRoutes.ROUTE_CART && cartBadgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = EspressoHeaderBg,
                                                    contentColor = Color(0xFFFFDF9E),
                                                    modifier = Modifier.testTag("cart_nav_badge")
                                                ) {
                                                    Text("$cartBadgeCount")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DismissibleStatusBanner(
                message = statusBanner,
                onDismiss = onDismissBanner
            )
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

@Composable
private fun PortalSwitchPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) EspressoHeaderBg else Color.Transparent,
        contentColor = if (selected) Color(0xFFFFDF9E) else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
