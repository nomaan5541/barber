package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.PlatformConfig
import com.example.data.model.UserAccount
import com.example.data.repository.MarketplaceRepository
import com.example.ui.admin.AdminBarbersManagementScreen
import com.example.ui.admin.AdminBookingsAndDisputesScreen
import com.example.ui.admin.AdminCatalogManagementScreen
import com.example.ui.admin.AdminOverviewScreen
import com.example.ui.admin.AdminPoliciesAndAuditScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.performSignOut
import com.example.ui.barber.BarberDashboardScreen
import com.example.ui.barber.BarberEarningsScreen
import com.example.ui.barber.BarberJobDetailScreen
import com.example.ui.barber.BarberProfileScreen
import com.example.ui.barber.BarberScheduleScreen
import com.example.ui.components.DismissibleStatusBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.customer.CustomerAddressesScreen
import com.example.ui.customer.CustomerBookingFlowScreen
import com.example.ui.customer.CustomerBookingStatusScreen
import com.example.ui.customer.CustomerBookingsScreen
import com.example.ui.customer.CustomerFavouritesScreen
import com.example.ui.customer.CustomerHomeScreen
import com.example.ui.customer.CustomerProfileHelpScreen
import com.example.ui.customer.DoorNumberSetupScreen
import com.example.ui.customer.ProtectedRouteAuthGate
import com.example.ui.customer.canonicalGroomingCatalog
import com.example.ui.customer.mergeWithCanonicalCatalog
import com.example.ui.glass.AmbientAtelierBackdrop
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassNavigationDock
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.DoorCutMotionSpecs
import com.example.ui.motion.LocalReducedMotion
import com.example.ui.motion.tactilePressPhysics
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
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

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
    var hasLocationPermissionState by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }
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
                        launchLocationError = "Grant location access so DoorCut can detect your home coordinates."
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
        if (hasLocationPermissionState) triggerLaunchCoordinateFetch()
    }

    LaunchedEffect(requestLocationOnLaunch) {
        if (!requestLocationOnLaunch) return@LaunchedEffect
        hasLocationPermissionState = LocationHelper.hasLocationPermission(context)
        if (hasLocationPermissionState) triggerLaunchCoordinateFetch()
        val snap = PhonePermissionsHelper.inspectPermissions(context)
        if ((!snap.hasAnyLocation || !snap.hasNotifications) && !hasRequestedLaunchPermission) {
            hasRequestedLaunchPermission = true
            launchPermissionLauncher.launch(PhonePermissionsHelper.startupPermissionsArray())
        }
    }

    val requestOrRefreshCoordinates: () -> Unit = {
        hasLocationPermissionState = LocationHelper.hasLocationPermission(context)
        if (hasLocationPermissionState) triggerLaunchCoordinateFetch()
        else launchPermissionLauncher.launch(PhonePermissionsHelper.startupPermissionsArray())
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
            onToggleService = { id ->
                guestSelectedServiceIds = if (guestSelectedServiceIds.contains(id)) {
                    guestSelectedServiceIds - id
                } else {
                    guestSelectedServiceIds + id
                }
            },
            onEnsureService = { id -> guestSelectedServiceIds = guestSelectedServiceIds + id },
            onRequestCoordinates = requestOrRefreshCoordinates,
            onSaveDoorDraft = { pendingDoorDraft = it }
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
    onEnsureService: (String) -> Unit,
    onRequestCoordinates: () -> Unit,
    onSaveDoorDraft: (PendingDoorAddressDraft) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.ROUTE_CUSTOMER_HOME
    val services = remember { canonicalGroomingCatalog() }
    var guestBarberId by remember { mutableStateOf<String?>(null) }
    var guestDateKey by remember { mutableStateOf(MarketplaceViewModel.defaultTodayDateKey()) }
    var guestStartHour by remember { mutableStateOf<Long?>(null) }

    val hideChrome = currentRoute in setOf(
        AppRoutes.ROUTE_BOOKING_FLOW,
        AppRoutes.ROUTE_DOOR_SETUP,
        AppRoutes.ROUTE_AUTH
    )

    val reducedMotion = LocalReducedMotion.current
    DoorCutScaffoldChrome(
        portalSubtitle = "CUSTOMER MODE",
        showChrome = !hideChrome,
        currentRoute = currentRoute,
        navTabs = customerNavTabs,
        onSelectTab = { navController.navigateSingleTopTo(it) },
        topBarActions = {
            TextButton(
                onClick = { navController.navigate(AppRoutes.ROUTE_AUTH) },
                modifier = Modifier
                    .height(48.dp)
                    .padding(end = 8.dp)
                    .tactilePressPhysics(pressedScale = 0.95f)
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
            startDestination = AppRoutes.ROUTE_CUSTOMER_HOME,
            enterTransition = {
                fadeIn(tween(180)) + scaleIn(
                    initialScale = if (reducedMotion) 1f else 0.985f,
                    animationSpec = DoorCutMotionSpecs.surfaceSettleSpring(reducedMotion)
                )
            },
            exitTransition = { fadeOut(tween(130)) },
            popEnterTransition = {
                fadeIn(tween(170)) + scaleIn(
                    initialScale = if (reducedMotion) 1f else 0.985f,
                    animationSpec = DoorCutMotionSpecs.surfaceSettleSpring(reducedMotion)
                )
            },
            popExitTransition = { fadeOut(tween(120)) },
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppRoutes.ROUTE_CUSTOMER_HOME) {
                CustomerHomeScreen(
                    services = services,
                    selectedServiceIds = selectedServiceIds,
                    upcomingBooking = null,
                    savedAddresses = emptyList(),
                    detectedLocation = launchLocation,
                    pendingDoorDraft = pendingDoorDraft,
                    isFetchingLocation = isLaunchFetchingLocation,
                    onToggleService = onToggleService,
                    onStartBookingFlow = { navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW) },
                    onOpenLocationSelector = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                    onRefreshGps = onRequestCoordinates,
                    onOpenBookingStatus = {}
                )
            }

            composable(AppRoutes.ROUTE_BOOKING_FLOW) {
                CustomerBookingFlowScreen(
                    isAuthenticated = false,
                    customerName = "Guest",
                    customerPhone = "",
                    services = services,
                    approvedBarbers = emptyList(),
                    savedAddresses = emptyList(),
                    serviceAreas = emptyList(),
                    slotLocks = emptyList(),
                    platformConfig = PlatformConfig(),
                    selectedServiceIds = selectedServiceIds,
                    selectedBarberId = guestBarberId,
                    selectedAddressId = null,
                    selectedDateKey = guestDateKey,
                    selectedStartHour = guestStartHour,
                    detectedLocation = launchLocation,
                    pendingDoorDraft = pendingDoorDraft,
                    isFetchingLocation = isLaunchFetchingLocation,
                    isBusy = false,
                    onToggleService = onToggleService,
                    onSelectBarber = { guestBarberId = it },
                    onSelectDate = { guestDateKey = it },
                    onSelectHour = { guestStartHour = it },
                    onSelectAddress = {},
                    onSaveAddressForAuthenticated = {},
                    onSaveDraftForGuest = onSaveDoorDraft,
                    onRequestGps = onRequestCoordinates,
                    onConfirmBooking = { _, _, _, _ -> navController.navigate(AppRoutes.ROUTE_AUTH) },
                    onExitFlow = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.ROUTE_BOOKINGS) {
                ProtectedRouteAuthGate(
                    title = stringResource(R.string.bookings_signin_gate_title),
                    subtitle = stringResource(R.string.bookings_signin_gate_subtitle),
                    onAuthSuccess = {}
                )
            }

            composable(AppRoutes.ROUTE_FAVOURITES) {
                CustomerFavouritesScreen(
                    services = services,
                    approvedBarbers = emptyList(),
                    onQuickBookService = { svc ->
                        onEnsureService(svc.id)
                        navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW)
                    },
                    onQuickBookBarber = { barber ->
                        guestBarberId = barber.barberId
                        navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW)
                    },
                    onBrowseHome = { navController.navigateSingleTopTo(AppRoutes.ROUTE_CUSTOMER_HOME) }
                )
            }

            composable(AppRoutes.ROUTE_PROFILE) {
                CustomerProfileHelpScreen(
                    isAuthenticated = false,
                    userAccount = null,
                    savedAddresses = emptyList(),
                    tickets = emptyList(),
                    platformConfig = PlatformConfig(),
                    isBusy = false,
                    onSignInClick = { navController.navigate(AppRoutes.ROUTE_AUTH) },
                    onUpdateProfile = { _, _ -> },
                    onManageAddresses = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                    onCreateSupportTicket = { _, _ -> },
                    onSwitchToBarberMode = { navController.navigate(AppRoutes.ROUTE_AUTH) },
                    onSwitchToAdminMode = { navController.navigate(AppRoutes.ROUTE_AUTH) },
                    onSignOut = {}
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
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.ROUTE_CUSTOMER_HOME

    val viewModel: MarketplaceViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                val databaseId = app.getString(R.string.firestore_database_id)
                MarketplaceViewModel(
                    repository = MarketplaceRepository(FirebaseFirestore.getInstance(databaseId)),
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

    LaunchedEffect(launchLocation) { viewModel.seedInitialLocation(launchLocation) }
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
                subtitle = "Your DoorCut account has been suspended. Please contact support."
            )
        }
        return
    }

    val currentPortal = portalOverride ?: when (userAccount.role) {
        "BARBER" -> ActivePortal.BARBER
        else -> ActivePortal.CUSTOMER
    }

    val activeTabs = when (currentPortal) {
        ActivePortal.CUSTOMER -> customerNavTabs
        ActivePortal.BARBER -> barberNavTabs
        ActivePortal.ADMIN -> adminNavTabs
    }

    val portalSubtitle = when (currentPortal) {
        ActivePortal.CUSTOMER -> "CUSTOMER MODE"
        ActivePortal.BARBER -> "BARBER MODE"
        ActivePortal.ADMIN -> "ADMIN DASHBOARD"
    }

    val hideChrome = currentRoute in setOf(
        AppRoutes.ROUTE_BOOKING_FLOW,
        AppRoutes.ROUTE_BOOKING_STATUS,
        AppRoutes.ROUTE_BARBER_JOB_DETAIL,
        AppRoutes.ROUTE_DOOR_SETUP
    )

    val catalogList = remember(activeServicesState) {
        mergeWithCanonicalCatalog((activeServicesState as? UiState.Success)?.data ?: emptyList())
    }
    val nextCustomerBooking = remember(customerBookings) {
        customerBookings.filter { it.isUpcoming() }
            .sortedWith(compareBy({ it.dateKey }, { it.startHour }))
            .firstOrNull()
    }

    val reducedMotion = LocalReducedMotion.current
    DoorCutScaffoldChrome(
        portalSubtitle = portalSubtitle,
        showChrome = !hideChrome,
        currentRoute = currentRoute,
        navTabs = activeTabs,
        onSelectTab = { navController.navigateSingleTopTo(it) },
        statusBanner = statusBanner,
        onDismissBanner = { viewModel.clearBanner() },
        topBarActions = {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.ChromeBar,
                shape = RoundedCornerShape(50),
                shadowElevation = 1.dp,
                interactivePhysics = false,
                modifier = Modifier.padding(end = 10.dp)
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
                            navController.navigateSingleTopTo(AppRoutes.ROUTE_CUSTOMER_HOME)
                        },
                        modifier = Modifier.testTag("portal_switch_customer")
                    )
                    PortalSwitchPill(
                        label = "Barber",
                        selected = currentPortal == ActivePortal.BARBER,
                        onClick = {
                            viewModel.switchPortal(ActivePortal.BARBER)
                            navController.navigateSingleTopTo(AppRoutes.ROUTE_BARBER_HOME)
                        },
                        modifier = Modifier.testTag("portal_switch_barber")
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
                    }
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = if (currentPortal == ActivePortal.BARBER) AppRoutes.ROUTE_BARBER_HOME else AppRoutes.ROUTE_CUSTOMER_HOME,
            enterTransition = {
                fadeIn(tween(180)) + scaleIn(
                    initialScale = if (reducedMotion) 1f else 0.985f,
                    animationSpec = DoorCutMotionSpecs.surfaceSettleSpring(reducedMotion)
                )
            },
            exitTransition = { fadeOut(tween(130)) },
            popEnterTransition = {
                fadeIn(tween(170)) + scaleIn(
                    initialScale = if (reducedMotion) 1f else 0.985f,
                    animationSpec = DoorCutMotionSpecs.surfaceSettleSpring(reducedMotion)
                )
            },
            popExitTransition = { fadeOut(tween(120)) },
            modifier = Modifier.fillMaxSize()
        ) {
            // --- CUSTOMER MODE (4 Bottom Tabs + Guided Sub-Routes) ---
            composable(AppRoutes.ROUTE_CUSTOMER_HOME) {
                CustomerHomeScreen(
                    services = catalogList,
                    selectedServiceIds = selectedServiceIds,
                    upcomingBooking = nextCustomerBooking,
                    savedAddresses = customerAddresses,
                    detectedLocation = vmDetectedLocation ?: launchLocation,
                    pendingDoorDraft = pendingDoorDraft,
                    isFetchingLocation = vmIsFetchingLocation || isLaunchFetchingLocation,
                    onToggleService = { viewModel.toggleServiceSelection(it) },
                    onStartBookingFlow = { navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW) },
                    onOpenLocationSelector = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                    onRefreshGps = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.autoFetchLocationAndPromptDoor(context, promptForHomeAddressAfterFetch = false)
                        } else {
                            onRequestLaunchCoordinates()
                        }
                    },
                    onOpenBookingStatus = { bookingId ->
                        navController.navigate(AppRoutes.bookingStatusRoute(bookingId))
                    }
                )
            }

            composable(AppRoutes.ROUTE_BOOKING_FLOW) {
                CustomerBookingFlowScreen(
                    isAuthenticated = true,
                    customerName = userAccount.name.ifBlank { displayName },
                    customerPhone = userAccount.phone,
                    services = catalogList,
                    approvedBarbers = approvedBarbers,
                    savedAddresses = customerAddresses,
                    serviceAreas = serviceAreas,
                    slotLocks = slotLocks,
                    platformConfig = platformConfig,
                    selectedServiceIds = selectedServiceIds,
                    selectedBarberId = selectedBarberId,
                    selectedAddressId = selectedAddressId,
                    selectedDateKey = selectedDateKey,
                    selectedStartHour = selectedStartHour,
                    detectedLocation = vmDetectedLocation ?: launchLocation,
                    pendingDoorDraft = pendingDoorDraft,
                    isFetchingLocation = vmIsFetchingLocation || isLaunchFetchingLocation,
                    isBusy = isBusy,
                    onToggleService = { viewModel.toggleServiceSelection(it) },
                    onSelectBarber = { viewModel.selectBarber(it) },
                    onSelectDate = { viewModel.selectDate(it) },
                    onSelectHour = { viewModel.selectStartHour(it) },
                    onSelectAddress = { viewModel.selectAddress(it) },
                    onSaveAddressForAuthenticated = { draft ->
                        viewModel.savePendingDoorDraft(draft = draft, areas = serviceAreas)
                    },
                    onSaveDraftForGuest = {},
                    onRequestGps = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.autoFetchLocationAndPromptDoor(context, promptForHomeAddressAfterFetch = false)
                        } else {
                            onRequestLaunchCoordinates()
                        }
                    },
                    onConfirmBooking = { barber, addr, bookingFeePaise, notes ->
                        val chosenServices = catalogList.filter { selectedServiceIds.contains(it.id) }
                        val areaTravel = serviceAreas.firstOrNull { it.id == addr.serviceAreaId }?.travelChargePaise ?: 0L
                        val hour = selectedStartHour ?: 10L
                        viewModel.confirmCustomerBooking(
                            customerName = userAccount.name.ifBlank { displayName },
                            customerPhone = userAccount.phone.ifBlank { "+919876543210" },
                            barber = barber,
                            selectedServices = chosenServices,
                            address = addr,
                            travelChargePaise = areaTravel,
                            platformBookingFeePaise = bookingFeePaise,
                            barberCommissionPaise = viewModel.calculateBarberCommissionPaise(
                                chosenServices.sumOf { it.pricePaise } + areaTravel,
                                platformConfig
                            ),
                            dateKey = selectedDateKey,
                            startHour = hour,
                            customerNotes = notes,
                            policySnapshot = platformConfig.policyText,
                            bookingFeeTxRef = "BFEE-${System.currentTimeMillis() % 100000}",
                            onSuccess = { newBookingId ->
                                navController.navigate(AppRoutes.bookingStatusRoute(newBookingId)) {
                                    popUpTo(AppRoutes.ROUTE_CUSTOMER_HOME)
                                }
                            }
                        )
                    },
                    onExitFlow = { navController.popBackStack() }
                )
            }

            composable(
                route = AppRoutes.ROUTE_BOOKING_STATUS,
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { entry ->
                val bookingId = entry.arguments?.getString("bookingId").orEmpty()
                val targetBooking = customerBookings.firstOrNull { it.id == bookingId }
                val targetBarber = approvedBarbers.firstOrNull { it.barberId == targetBooking?.barberId }
                CustomerBookingStatusScreen(
                    booking = targetBooking,
                    barberProfile = targetBarber,
                    protectedAddress = protectedAddresses[bookingId],
                    platformConfig = platformConfig,
                    isBusy = isBusy,
                    onLoadProtectedAddress = { viewModel.loadProtectedAddress(it) },
                    onApproveExtraService = { b, approve -> viewModel.customerRespondExtraService(b, approve) },
                    onConfirmDirectPayment = { id, ok, reason -> viewModel.customerVerifyDirectPayment(id, ok, reason) },
                    onCancelBooking = { b, reason -> viewModel.customerCancelBooking(b, reason) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.ROUTE_BOOKINGS) {
                CustomerBookingsScreen(
                    customerName = userAccount.name.ifBlank { displayName },
                    bookings = customerBookings,
                    isBusy = isBusy,
                    onOpenBookingStatus = { navController.navigate(AppRoutes.bookingStatusRoute(it)) },
                    onBookAgain = { prev ->
                        viewModel.repeatBooking(prev)
                        navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW)
                    },
                    onSubmitReview = { b, stars, comment ->
                        viewModel.submitReview(b, userAccount.name.ifBlank { displayName }, stars, comment)
                    },
                    onStartNewBooking = { navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW) }
                )
            }

            composable(AppRoutes.ROUTE_FAVOURITES) {
                CustomerFavouritesScreen(
                    services = catalogList,
                    approvedBarbers = approvedBarbers,
                    onQuickBookService = { svc ->
                        viewModel.ensureServiceSelected(svc.id)
                        navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW)
                    },
                    onQuickBookBarber = { barber ->
                        viewModel.selectBarber(barber.barberId)
                        navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW)
                    },
                    onBrowseHome = { navController.navigateSingleTopTo(AppRoutes.ROUTE_CUSTOMER_HOME) }
                )
            }

            composable(AppRoutes.ROUTE_PROFILE) {
                CustomerProfileHelpScreen(
                    isAuthenticated = true,
                    userAccount = userAccount,
                    savedAddresses = customerAddresses,
                    tickets = myTickets,
                    platformConfig = platformConfig,
                    isBusy = isBusy,
                    onSignInClick = {},
                    onUpdateProfile = { name, phone -> viewModel.updateProfile(name, phone) },
                    onManageAddresses = { navController.navigate(AppRoutes.ROUTE_ADDRESSES) },
                    onCreateSupportTicket = { subj, desc ->
                        viewModel.createSupportTicket("CUSTOMER", "", "GENERAL", subj, desc)
                    },
                    onSwitchToBarberMode = {
                        viewModel.switchPortal(ActivePortal.BARBER)
                        navController.navigateSingleTopTo(AppRoutes.ROUTE_BARBER_HOME)
                    },
                    onSwitchToAdminMode = {
                        viewModel.switchPortal(ActivePortal.ADMIN)
                        navController.navigateSingleTopTo(AppRoutes.ROUTE_ADMIN_OVERVIEW)
                    },
                    onSignOut = { performSignOut(context = context, onSignOutComplete = {}, scope = scope) }
                )
            }

            composable(AppRoutes.ROUTE_ADDRESSES) {
                BackHandler { navController.popBackStack() }
                CustomerAddressesScreen(
                    viewModel = viewModel,
                    addresses = customerAddresses,
                    serviceAreas = serviceAreas,
                    isBusy = isBusy
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

            // --- BARBER MODE (4 Bottom Tabs + Guided Job Detail Route) ---
            composable(AppRoutes.ROUTE_BARBER_HOME) {
                BarberDashboardScreen(
                    barberProfile = ownBarberProfile,
                    defaultDisplayName = userAccount.name.ifBlank { displayName },
                    bookings = barberBookings,
                    catalogServices = catalogList,
                    serviceAreas = serviceAreas,
                    isBusy = isBusy,
                    onToggleOnline = { viewModel.barberToggleOnline(it) },
                    onAcceptOrDeclineRequest = { b, accept, reason -> viewModel.barberRespond(b, accept, reason) },
                    onAdvanceTripStatus = { id, next -> viewModel.barberAdvanceTripStatus(id, next) },
                    onOpenBookingDetail = { id -> navController.navigate(AppRoutes.barberJobDetailRoute(id)) },
                    onApplyAsBarber = { name, phone, bio, exp, upi, shop, area, skills ->
                        viewModel.applyAsBarber(
                            name = name,
                            phone = phone,
                            bio = bio,
                            experienceYears = exp,
                            upiId = upi,
                            shopName = shop,
                            shopAddress = area,
                            skillsText = skills,
                            serviceIds = catalogList.take(5).map { it.id },
                            serviceAreaIds = serviceAreas.map { it.id }.ifEmpty { listOf("zone_central") }
                        )
                    }
                )
            }

            composable(
                route = AppRoutes.ROUTE_BARBER_JOB_DETAIL,
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { entry ->
                val bookingId = entry.arguments?.getString("bookingId").orEmpty()
                val job = barberBookings.firstOrNull { it.id == bookingId }
                BarberJobDetailScreen(
                    booking = job,
                    protectedAddress = protectedAddresses[bookingId],
                    catalogServices = catalogList,
                    isBusy = isBusy,
                    onLoadProtectedAddress = { viewModel.loadProtectedAddress(it) },
                    onAdvanceTripStatus = { id, next -> viewModel.barberAdvanceTripStatus(id, next) },
                    onFinishService = { id -> viewModel.barberFinishService(id) },
                    onRequestExtraService = { id, svc -> viewModel.barberRequestExtraService(id, svc) },
                    onRecordDirectPayment = { id, status, method, paise, ref ->
                        viewModel.barberRecordDirectPayment(id, status, method, paise, ref)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.ROUTE_BARBER_SCHEDULE) {
                val dateOptions = remember { viewModel.upcomingDateOptions(10) }
                BarberScheduleScreen(
                    barberProfile = ownBarberProfile,
                    bookings = barberBookings,
                    dateOptions = dateOptions,
                    isBusy = isBusy,
                    onOpenBookingDetail = { id -> navController.navigate(AppRoutes.barberJobDetailRoute(id)) },
                    onSaveWorkingHours = { startH, endH, breakS, breakE, buffer ->
                        val prof = ownBarberProfile ?: return@BarberScheduleScreen
                        viewModel.barberSaveSchedule(
                            isOnline = prof.isOnline,
                            workStartHour = startH,
                            workEndHour = endH,
                            breakStartHour = breakS,
                            breakEndHour = breakE,
                            travelBufferMinutes = buffer,
                            upiId = prof.upiId,
                            shopName = prof.shopName,
                            shopAddress = prof.shopAddress,
                            skillsText = prof.effectiveSkills().joinToString(", "),
                            serviceIds = prof.serviceIds,
                            workingDays = prof.workingDays
                        )
                    }
                )
            }

            composable(AppRoutes.ROUTE_BARBER_EARNINGS) {
                BarberEarningsScreen(bookings = barberBookings)
            }

            composable(AppRoutes.ROUTE_BARBER_PROFILE) {
                BarberProfileScreen(
                    barberProfile = ownBarberProfile,
                    displayName = userAccount.name.ifBlank { displayName },
                    catalogServices = catalogList,
                    platformConfig = platformConfig,
                    isBusy = isBusy,
                    onSaveBarberServicesAndUpi = { upi, shop, area, svcIds ->
                        val prof = ownBarberProfile ?: return@BarberProfileScreen
                        viewModel.barberSaveSchedule(
                            isOnline = prof.isOnline,
                            workStartHour = prof.workStartHour,
                            workEndHour = prof.workEndHour,
                            breakStartHour = prof.breakStartHour,
                            breakEndHour = prof.breakEndHour,
                            travelBufferMinutes = prof.travelBufferMinutes,
                            upiId = upi,
                            shopName = shop,
                            shopAddress = area,
                            skillsText = prof.effectiveSkills().joinToString(", "),
                            serviceIds = svcIds,
                            workingDays = prof.workingDays
                        )
                    },
                    onCreateSupportTicket = { subj, desc ->
                        viewModel.createSupportTicket("BARBER", "", "PARTNER_SUPPORT", subj, desc)
                    },
                    onSwitchToCustomerMode = {
                        viewModel.switchPortal(ActivePortal.CUSTOMER)
                        navController.navigateSingleTopTo(AppRoutes.ROUTE_CUSTOMER_HOME)
                    },
                    onSignOut = { performSignOut(context = context, onSignOutComplete = {}, scope = scope) }
                )
            }

            // --- ADMIN DASHBOARD ROUTES ---
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
                AdminBarbersManagementScreen(viewModel = viewModel, barbers = adminAllBarbers, isBusy = isBusy)
            }
            composable(AppRoutes.ROUTE_ADMIN_CATALOG) {
                AdminCatalogManagementScreen(viewModel = viewModel, services = adminAllServices, isBusy = isBusy)
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
    statusBanner: String? = null,
    onDismissBanner: () -> Unit = {},
    topBarActions: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    AmbientAtelierBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (showChrome) {
                    LiquidGlassSurface(
                        strength = GlassMaterialStrength.ChromeBar,
                        shape = RoundedCornerShape(0.dp),
                        shadowElevation = 2.dp,
                        interactivePhysics = false
                    ) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                    }
                }
            },
            bottomBar = {
                if (showChrome) {
                    LiquidGlassNavigationDock(
                        navTabs = navTabs,
                        currentRoute = currentRoute,
                        onSelectTab = onSelectTab
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                DismissibleStatusBanner(message = statusBanner, onDismiss = onDismissBanner)
                Box(modifier = Modifier.fillMaxSize()) { content() }
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
    val bgColor by animateColorAsState(
        targetValue = if (selected) EspressoHeaderBg else Color.Transparent,
        animationSpec = tween(170),
        label = "portal_pill_bg_$label"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color(0xFFFFDF9E) else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(170),
        label = "portal_pill_text_$label"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = if (selected) BorderStroke(1.dp, BrassGold.copy(alpha = 0.45f)) else null,
        modifier = modifier
            .tactilePressPhysics(pressedScale = 0.94f, maxTiltDegrees = 0f, enableHaptics = true)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
