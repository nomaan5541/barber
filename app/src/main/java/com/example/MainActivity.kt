package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
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
import com.example.data.model.UserAccount
import com.example.ui.admin.AdminBarbersManagementScreen
import com.example.ui.admin.AdminBookingsAndDisputesScreen
import com.example.ui.admin.AdminCatalogManagementScreen
import com.example.ui.admin.AdminOverviewScreen
import com.example.ui.admin.AdminPoliciesAndAuditScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.OnboardingProfileScreen
import com.example.ui.barber.BarberDashboardScreen
import com.example.ui.barber.BarberEarningsScreen
import com.example.ui.barber.BarberJobDetailScreen
import com.example.ui.barber.BarberProfileScreen
import com.example.ui.barber.BarberScheduleScreen
import com.example.ui.components.DismissibleStatusBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LegalPoliciesSheet
import com.example.ui.customer.CustomerAddressesScreen
import com.example.ui.customer.CustomerBookingFlowScreen
import com.example.ui.customer.CustomerBookingStatusScreen
import com.example.ui.customer.CustomerBookingsScreen
import com.example.ui.customer.CustomerFavouritesScreen
import com.example.ui.customer.CustomerHomeScreen
import com.example.ui.customer.CustomerProfileHelpScreen
import com.example.ui.customer.DoorNumberSetupScreen
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
import com.example.ui.theme.SageVerified
import com.example.ui.viewmodel.ActivePortal
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.util.DetectedDoorLocation
import com.example.util.DoorCutNotificationHelper
import com.example.util.LocationFetchResult
import com.example.util.LocationHelper
import com.example.util.PendingDoorAddressDraft
import com.example.util.PhonePermissionsHelper
import com.example.data.firebase.DoorCutFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
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

internal fun signOutDoorCutSession(
    context: Context,
    scope: CoroutineScope,
    onSignedOut: () -> Unit = {}
) {
    DoorCutFirebase.auth().signOut()
    scope.launch {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("Auth", "Failed to clear credential state on sign-out: ${e.message}")
        } finally {
            onSignedOut()
        }
    }
}

@Composable
fun DoorCutRootNavigation(
    auth: FirebaseAuth = DoorCutFirebase.auth(),
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
        // Strict Google Sign-In Entry Gate: No guest bypass
        AuthScreen(
            onSignedIn = {
                if (LocationHelper.hasLocationPermission(context)) {
                    triggerLaunchCoordinateFetch()
                }
            }
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
            onRequestLaunchCoordinates = requestOrRefreshCoordinates
        )
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
    onRequestLaunchCoordinates: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: MarketplaceViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" } as Application
                MarketplaceViewModel(app)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTopPoliciesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(launchLocation) {
        viewModel.seedInitialLocation(launchLocation)
    }

    // 1. Wait briefly while verified Firestore account profile loads after Google Sign-In
    if (uiState.isLoadingAuth && uiState.userAccount == null) {
        AmbientAtelierBackdrop {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Verifying your DoorCut account…",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        return
    }

    // 2. Mandatory Post-Google-Sign-In Onboarding (Role: Customer vs Barber, Name, DOB, Age, Gender, Phone, Privacy Consent)
    if (uiState.requiresOnboarding) {
        OnboardingProfileScreen(
            initialName = uiState.userAccount?.name?.ifBlank { displayName } ?: displayName,
            initialEmail = uiState.userAccount?.email?.ifBlank { email } ?: email,
            initialPhone = uiState.userAccount?.phone.orEmpty(),
            detectedLocality = (uiState.detectedLocation ?: launchLocation)?.localityName.orEmpty(),
            isBusy = uiState.isBusy,
            onCompleteOnboarding = { fullName, phone, dob, age, gender, selectedRole, servesLadies, servesMen, expYears, upiId, primaryArea ->
                viewModel.completeOnboarding(
                    fullName = fullName,
                    phone = phone,
                    dob = dob,
                    age = age,
                    gender = gender,
                    selectedRole = selectedRole,
                    servesLadies = servesLadies,
                    servesMen = servesMen,
                    experienceYears = expYears,
                    upiId = upiId,
                    primaryArea = primaryArea
                )
            },
            onSignOut = {
                signOutDoorCutSession(context = context, scope = scope) {
                    viewModel.signOut()
                }
            }
        )
        return
    }

    val userAccount = uiState.userAccount ?: UserAccount(
        id = currentUserId,
        userId = currentUserId,
        name = displayName,
        email = email,
        role = "CUSTOMER",
        status = "ACTIVE",
        onboardingCompleted = true
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

    // 3. Dedicated Role-Locked Experience (Customer -> Customer Portal, Barber -> Barber Portal, Admin -> Admin Portal)
    val currentPortal = when (userAccount.role) {
        "ADMIN" -> uiState.activePortal
        "BARBER" -> ActivePortal.BARBER
        else -> ActivePortal.CUSTOMER
    }

    val navController: NavHostController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val startDestination = when (currentPortal) {
        ActivePortal.BARBER -> AppRoutes.ROUTE_BARBER_HOME
        ActivePortal.ADMIN -> AppRoutes.ROUTE_ADMIN_OVERVIEW
        ActivePortal.CUSTOMER -> AppRoutes.ROUTE_CUSTOMER_HOME
    }
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    val activeTabs = when (currentPortal) {
        ActivePortal.CUSTOMER -> customerNavTabs
        ActivePortal.BARBER -> barberNavTabs
        ActivePortal.ADMIN -> adminNavTabs
    }

    val portalSubtitle = when (currentPortal) {
        ActivePortal.CUSTOMER -> "HOME GROOMING · CUSTOMER"
        ActivePortal.BARBER -> "DISPATCH CONSOLE · BARBER PARTNER"
        ActivePortal.ADMIN -> "GOVERNANCE · ADMIN CONSOLE"
    }

    val hideChrome = currentRoute in setOf(
        AppRoutes.ROUTE_BOOKING_FLOW,
        AppRoutes.ROUTE_BOOKING_STATUS,
        AppRoutes.ROUTE_BARBER_JOB_DETAIL,
        AppRoutes.ROUTE_DOOR_SETUP
    )

    val catalogList = remember(uiState.services) {
        mergeWithCanonicalCatalog(uiState.services)
    }
    val nextCustomerBooking = remember(uiState.customerBookings) {
        uiState.customerBookings.filter { it.isUpcoming() }
            .sortedWith(compareBy({ it.dateKey }, { it.startHour }))
            .firstOrNull()
    }

    val effectiveLocation = uiState.detectedLocation ?: launchLocation
    val effectiveIsFetchingLoc = uiState.isFetchingLocation || isLaunchFetchingLocation

    val reducedMotion = LocalReducedMotion.current
    DoorCutScaffoldChrome(
        portalSubtitle = portalSubtitle,
        showChrome = !hideChrome,
        currentRoute = currentRoute,
        navTabs = activeTabs,
        onSelectTab = { navController.navigateSingleTopTo(it) },
        statusBanner = uiState.snackbarMessage,
        onDismissBanner = { viewModel.clearSnackbar() },
        topBarActions = {
            if (userAccount.role == "ADMIN") {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.ChromeBar,
                    shape = RoundedCornerShape(50),
                    shadowElevation = 1.dp,
                    interactivePhysics = false,
                    modifier = Modifier.padding(end = 6.dp)
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
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = EspressoHeaderBg,
                        border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = if (currentPortal == ActivePortal.BARBER) SageVerified else BrassGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (currentPortal == ActivePortal.BARBER) "Partner" else "Verified",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFDF9E)
                            )
                        }
                    }
                    IconButton(
                        onClick = { showTopPoliciesSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("top_bar_policies_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Policy,
                            contentDescription = "Privacy Policy & Safety Charter",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
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
            // --- CUSTOMER PORTAL ---
            composable(AppRoutes.ROUTE_CUSTOMER_HOME) {
                CustomerHomeScreen(
                    services = catalogList,
                    selectedServiceIds = uiState.selectedServiceIds,
                    upcomingBooking = nextCustomerBooking,
                    savedAddresses = uiState.savedAddresses,
                    detectedLocation = effectiveLocation,
                    pendingDoorDraft = uiState.pendingDoorDraft,
                    isFetchingLocation = effectiveIsFetchingLoc,
                    onToggleService = { viewModel.toggleServiceInCart(it) },
                    onStartBookingFlow = { navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW) },
                    onOpenLocationSelector = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                    onRefreshGps = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.autoFetchDeviceLocation(silent = false)
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
                    savedAddresses = uiState.savedAddresses,
                    serviceAreas = uiState.serviceAreas,
                    platformConfig = uiState.platformConfig,
                    selectedServiceIds = uiState.selectedServiceIds,
                    selectedAddressId = uiState.selectedAddressId,
                    selectedDateKey = uiState.selectedDateKey,
                    selectedStartHour = uiState.selectedStartHour,
                    bookingMode = uiState.bookingMode,
                    recipientGender = uiState.bookingRecipientGender,
                    recipientAge = uiState.bookingRecipientAge,
                    preferLadyBarber = uiState.preferLadyBarber,
                    detectedLocation = effectiveLocation,
                    pendingDoorDraft = uiState.pendingDoorDraft,
                    isFetchingLocation = effectiveIsFetchingLoc,
                    isBusy = uiState.isBusy,
                    onToggleService = { viewModel.toggleServiceInCart(it) },
                    onSelectDate = { dateKey ->
                        viewModel.setBookingDateAndHour(dateKey, uiState.selectedStartHour)
                    },
                    onSelectHour = { hour ->
                        if (hour != null) {
                            viewModel.setBookingDateAndHour(uiState.selectedDateKey, hour)
                        }
                    },
                    onSetBookingMode = { mode -> viewModel.setBookingMode(mode) },
                    onUpdateRecipientPreferences = { gender, age, preferLady ->
                        viewModel.setRecipientPreferences(gender, age, preferLady)
                    },
                    onSelectAddress = { addrId ->
                        if (addrId != null) viewModel.selectAddress(addrId)
                    },
                    onSaveAddressForAuthenticated = { draft ->
                        viewModel.confirmDoorAddressSetup(draft)
                    },
                    onRequestGps = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.autoFetchDeviceLocation(silent = false)
                        } else {
                            onRequestLaunchCoordinates()
                        }
                    },
                    onConfirmOpenDispatchBooking = { addr, _, contactPhone, notes ->
                        if (addr.id.isNotBlank()) {
                            viewModel.selectAddress(addr.id)
                        }
                        viewModel.confirmOpenDispatchBooking(
                            customerPhone = contactPhone,
                            customerNotes = notes,
                            bookingFeeTxRef = "BFEE-${System.currentTimeMillis() % 100000}",
                            onBookingCreated = { newBookingId ->
                                navController.navigate(AppRoutes.bookingStatusRoute(newBookingId)) {
                                    popUpTo(AppRoutes.ROUTE_CUSTOMER_HOME)
                                }
                            }
                        )
                    },
                    onExitFlow = { navController.popBackStack() },
                    onLocationPinned = { pinned -> viewModel.updatePinnedLocation(pinned) }
                )
            }

            composable(
                route = AppRoutes.ROUTE_BOOKING_STATUS,
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { entry ->
                val bookingId = entry.arguments?.getString("bookingId").orEmpty()
                val targetBooking = uiState.customerBookings.firstOrNull { it.id == bookingId }
                val targetBarber = uiState.approvedBarbers.firstOrNull { it.barberId == targetBooking?.barberId }
                CustomerBookingStatusScreen(
                    booking = targetBooking,
                    barberProfile = targetBarber,
                    protectedAddress = uiState.unlockedAddresses[bookingId],
                    platformConfig = uiState.platformConfig,
                    isBusy = uiState.isBusy,
                    onLoadProtectedAddress = { viewModel.loadProtectedAddressForJob(it) },
                    onApproveExtraService = { b, approve -> viewModel.respondToExtraServiceRequest(b, approve) },
                    onConfirmDirectPayment = { id, ok, _ -> viewModel.confirmOrDisputeDirectPayment(id, ok) },
                    onCancelBooking = { b, reason -> viewModel.cancelBooking(b, reason) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.ROUTE_BOOKINGS) {
                CustomerBookingsScreen(
                    customerName = userAccount.name.ifBlank { displayName },
                    bookings = uiState.customerBookings,
                    isBusy = uiState.isBusy,
                    onOpenBookingStatus = { navController.navigate(AppRoutes.bookingStatusRoute(it)) },
                    onBookAgain = { prev ->
                        viewModel.repeatCompletedBooking(prev) {
                            navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW)
                        }
                    },
                    onSubmitReview = { b, stars, comment ->
                        viewModel.submitReview(b, stars.toInt(), comment)
                    },
                    onStartNewBooking = { navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW) }
                )
            }

            composable(AppRoutes.ROUTE_FAVOURITES) {
                CustomerFavouritesScreen(
                    services = catalogList,
                    onQuickBookService = { svc ->
                        if (!uiState.selectedServiceIds.contains(svc.id)) {
                            viewModel.toggleServiceInCart(svc.id)
                        }
                        navController.navigate(AppRoutes.ROUTE_BOOKING_FLOW)
                    },
                    onBrowseHome = { navController.navigateSingleTopTo(AppRoutes.ROUTE_CUSTOMER_HOME) }
                )
            }

            composable(AppRoutes.ROUTE_PROFILE) {
                CustomerProfileHelpScreen(
                    isAuthenticated = true,
                    userAccount = userAccount,
                    savedAddresses = uiState.savedAddresses,
                    tickets = uiState.supportTickets,
                    platformConfig = uiState.platformConfig,
                    isBusy = uiState.isBusy,
                    onSignInClick = {},
                    onUpdateProfile = { name, phone -> viewModel.updateProfileContact(name, phone) },
                    onManageAddresses = { navController.navigate(AppRoutes.ROUTE_ADDRESSES) },
                    onCreateSupportTicket = { subj, desc ->
                        viewModel.raiseSupportTicket("CUSTOMER", "", "GENERAL", subj, desc)
                    },
                    onSwitchToAdminMode = {
                        viewModel.switchPortal(ActivePortal.ADMIN)
                        navController.navigateSingleTopTo(AppRoutes.ROUTE_ADMIN_OVERVIEW)
                    },
                    onSignOut = {
                        signOutDoorCutSession(context = context, scope = scope) {
                            viewModel.signOut()
                        }
                    }
                )
            }

            composable(AppRoutes.ROUTE_ADDRESSES) {
                BackHandler { navController.popBackStack() }
                CustomerAddressesScreen(
                    viewModel = viewModel,
                    addresses = uiState.savedAddresses,
                    serviceAreas = uiState.serviceAreas,
                    isBusy = uiState.isBusy
                )
            }

            composable(AppRoutes.ROUTE_DOOR_SETUP) {
                BackHandler { navController.popBackStack() }
                val defaultAddr = uiState.savedAddresses.firstOrNull { it.isDefault } ?: uiState.savedAddresses.firstOrNull()
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
                    detectedLocation = effectiveLocation,
                    isFetchingLocation = effectiveIsFetchingLoc,
                    hasLocationPermission = hasLocationPermission || LocationHelper.hasLocationPermission(context),
                    locationErrorMessage = uiState.locationStatusMessage ?: launchLocationError,
                    initialDraft = seededDraft,
                    isBusy = uiState.isBusy,
                    allowDismiss = true,
                    onRequestDetectCoordinates = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.autoFetchDeviceLocation(silent = false)
                        } else {
                            onRequestLaunchCoordinates()
                        }
                    },
                    onConfirmDoorAddress = { draft ->
                        viewModel.confirmDoorAddressSetup(draft) {
                            navController.popBackStack()
                        }
                    },
                    onSkipOrClose = { navController.popBackStack() },
                    onLocationPinned = { pinned -> viewModel.updatePinnedLocation(pinned) }
                )
            }

            // --- BARBER PARTNER PORTAL ---
            composable(AppRoutes.ROUTE_BARBER_HOME) {
                BarberDashboardScreen(
                    barberProfile = uiState.ownBarberProfile,
                    defaultDisplayName = userAccount.name.ifBlank { displayName },
                    defaultPhone = userAccount.phone,
                    bookings = uiState.barberBookings,
                    openDispatchBookings = uiState.openDispatchBookings,
                    catalogServices = catalogList,
                    serviceAreas = uiState.serviceAreas,
                    isBusy = uiState.isBusy,
                    onToggleOnline = { viewModel.toggleBarberOnline(it) },
                    onClaimOpenBooking = { openBooking ->
                        viewModel.barberClaimOpenBooking(openBooking) { claimedId ->
                            navController.navigate(AppRoutes.barberJobDetailRoute(claimedId))
                        }
                    },
                    onAcceptOrDeclineRequest = { b, accept, reason ->
                        viewModel.barberRespondToRequest(b, accept, reason)
                    },
                    onAdvanceTripStatus = { id, next -> viewModel.barberAdvanceTripStatus(id, next) },
                    onOpenBookingDetail = { id -> navController.navigate(AppRoutes.barberJobDetailRoute(id)) },
                    onApplyAsBarber = { name, phone, bio, exp, upi, shop, area, _ ->
                        val resolvedCoords = effectiveLocation?.takeIf { it.hasValidCoordinates }
                            ?: LocationHelper.resolveBestFallbackLocationForText(area, "")
                        viewModel.applyOrUpdateBarberProfile(
                            name = name,
                            phone = phone,
                            gender = userAccount.gender.ifBlank { "MALE" },
                            servesLadies = userAccount.gender == "FEMALE",
                            servesMen = userAccount.gender != "FEMALE",
                            bio = bio,
                            experienceYears = exp,
                            selectedServiceIds = catalogList.map { it.id },
                            selectedAreaIds = uiState.serviceAreas.map { it.id }.ifEmpty { listOf("zone_central") },
                            workStartHour = 9L,
                            workEndHour = 20L,
                            upiId = upi,
                            shopName = shop,
                            shopAddress = area,
                            latitude = resolvedCoords.latitude,
                            longitude = resolvedCoords.longitude
                        )
                    },
                    detectedLocation = effectiveLocation,
                    isFetchingLocation = effectiveIsFetchingLoc,
                    onRequestBarberGps = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.autoFetchDeviceLocation(silent = false)
                        } else {
                            onRequestLaunchCoordinates()
                        }
                    },
                    onSyncLiveGpsCoordinates = { lat, lon ->
                        viewModel.syncBarberHubLocation(
                            DetectedDoorLocation(
                                doorHint = "",
                                streetArea = uiState.ownBarberProfile?.shopAddress.orEmpty(),
                                localityName = "Hyderabad",
                                landmark = "",
                                postalCode = "",
                                latitude = lat,
                                longitude = lon
                            ),
                            updateShopAddress = false
                        )
                    }
                )
            }

            composable(
                route = AppRoutes.ROUTE_BARBER_JOB_DETAIL,
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { entry ->
                val bookingId = entry.arguments?.getString("bookingId").orEmpty()
                val job = uiState.barberBookings.firstOrNull { it.id == bookingId }
                    ?: uiState.openDispatchBookings.firstOrNull { it.id == bookingId }
                BarberJobDetailScreen(
                    booking = job,
                    protectedAddress = uiState.unlockedAddresses[bookingId],
                    catalogServices = catalogList,
                    isBusy = uiState.isBusy,
                    onLoadProtectedAddress = { viewModel.loadProtectedAddressForJob(it) },
                    onAdvanceTripStatus = { id, next -> viewModel.barberAdvanceTripStatus(id, next) },
                    onUpdateLiveLocation = { id, lat, lon -> viewModel.updateBarberLiveLocation(id, lat, lon) },
                    onFinishService = { id -> viewModel.barberAdvanceTripStatus(id, "COMPLETED") },
                    onRequestExtraService = { id, svc -> viewModel.barberRequestExtraService(id, svc) },
                    onRecordDirectPayment = { id, status, method, paise, ref ->
                        viewModel.barberFinishAndDeclareDirectPayment(
                            bookingId = id,
                            declaredStatus = status,
                            method = method,
                            amountPaise = paise,
                            note = ref,
                            markCompleted = true
                        )
                    },
                    onBack = { navController.popBackStack() },
                    barberUpiId = uiState.ownBarberProfile?.upiId.orEmpty()
                )
            }

            composable(AppRoutes.ROUTE_BARBER_SCHEDULE) {
                val dateOptions = remember { viewModel.upcomingDateOptions(10) }
                BarberScheduleScreen(
                    barberProfile = uiState.ownBarberProfile,
                    bookings = uiState.barberBookings,
                    dateOptions = dateOptions,
                    isBusy = uiState.isBusy,
                    onOpenBookingDetail = { id -> navController.navigate(AppRoutes.barberJobDetailRoute(id)) },
                    onSaveWorkingHours = { startH, endH, breakS, breakE, bufferM ->
                        val prof = uiState.ownBarberProfile
                        viewModel.barberSaveScheduleAndProfile(
                            workStartHour = startH,
                            workEndHour = endH,
                            upiId = prof?.upiId.orEmpty(),
                            shopName = prof?.shopName.orEmpty(),
                            shopAddress = prof?.shopAddress.orEmpty(),
                            serviceIds = prof?.serviceIds.orEmpty(),
                            breakStartHour = breakS,
                            breakEndHour = breakE,
                            travelBufferMinutes = bufferM
                        )
                    },
                    onSaveScheduleWithDays = { startH, endH, breakS, breakE, bufferM, workingDays ->
                        val prof = uiState.ownBarberProfile
                        viewModel.barberSaveScheduleAndProfile(
                            workStartHour = startH,
                            workEndHour = endH,
                            upiId = prof?.upiId.orEmpty(),
                            shopName = prof?.shopName.orEmpty(),
                            shopAddress = prof?.shopAddress.orEmpty(),
                            serviceIds = prof?.serviceIds.orEmpty(),
                            workingDays = workingDays,
                            breakStartHour = breakS,
                            breakEndHour = breakE,
                            travelBufferMinutes = bufferM
                        )
                    }
                )
            }

            composable(AppRoutes.ROUTE_BARBER_EARNINGS) {
                BarberEarningsScreen(
                    bookings = uiState.barberBookings,
                    onOpenBookingDetail = { id -> navController.navigate(AppRoutes.barberJobDetailRoute(id)) }
                )
            }

            composable(AppRoutes.ROUTE_BARBER_PROFILE) {
                BarberProfileScreen(
                    barberProfile = uiState.ownBarberProfile,
                    displayName = userAccount.name.ifBlank { displayName },
                    partnerPhone = userAccount.phone,
                    isAdmin = userAccount.role == "ADMIN",
                    catalogServices = catalogList,
                    platformConfig = uiState.platformConfig,
                    isBusy = uiState.isBusy,
                    onSaveBarberServicesAndUpi = { upi, shop, area, svcIds, servesLadies, servesMen ->
                        val prof = uiState.ownBarberProfile
                        viewModel.barberSaveScheduleAndProfile(
                            workStartHour = prof?.workStartHour ?: 9L,
                            workEndHour = prof?.workEndHour ?: 20L,
                            upiId = upi,
                            shopName = shop,
                            shopAddress = area,
                            serviceIds = svcIds,
                            servesLadies = servesLadies,
                            servesMen = servesMen
                        )
                    },
                    onCreateSupportTicket = { subj, desc ->
                        viewModel.raiseSupportTicket("BARBER", "", "PARTNER_SUPPORT", subj, desc)
                    },
                    onSwitchToAdminMode = {
                        viewModel.switchPortal(ActivePortal.ADMIN)
                        navController.navigateSingleTopTo(AppRoutes.ROUTE_ADMIN_OVERVIEW)
                    },
                    onSignOut = {
                        signOutDoorCutSession(context = context, scope = scope) {
                            viewModel.signOut()
                        }
                    },
                    detectedLocation = effectiveLocation,
                    isFetchingLocation = effectiveIsFetchingLoc,
                    onRequestGpsLocation = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.autoFetchDeviceLocation(silent = false)
                        } else {
                            onRequestLaunchCoordinates()
                        }
                    },
                    onSaveFullBarberProfile = { upi, shop, area, svcIds, servesLadies, servesMen, bio, expYears, lat, lon ->
                        val prof = uiState.ownBarberProfile
                        viewModel.barberSaveScheduleAndProfile(
                            workStartHour = prof?.workStartHour ?: 9L,
                            workEndHour = prof?.workEndHour ?: 20L,
                            upiId = upi,
                            shopName = shop,
                            shopAddress = area,
                            serviceIds = svcIds,
                            servesLadies = servesLadies,
                            servesMen = servesMen,
                            latitude = lat,
                            longitude = lon,
                            bio = bio,
                            experienceYears = expYears
                        )
                    }
                )
            }

            // --- ADMIN GOVERNANCE ROUTES ---
            composable(AppRoutes.ROUTE_ADMIN_OVERVIEW) {
                AdminOverviewScreen(
                    viewModel = viewModel,
                    users = uiState.adminAllUsers.ifEmpty { listOf(userAccount) },
                    barbers = uiState.adminAllBarbers.ifEmpty { uiState.approvedBarbers },
                    services = catalogList,
                    bookings = uiState.adminAllBookings,
                    tickets = uiState.supportTickets,
                    isBusy = uiState.isBusy
                )
            }
            composable(AppRoutes.ROUTE_ADMIN_BARBERS) {
                AdminBarbersManagementScreen(
                    viewModel = viewModel,
                    barbers = uiState.adminAllBarbers.ifEmpty { uiState.approvedBarbers },
                    isBusy = uiState.isBusy
                )
            }
            composable(AppRoutes.ROUTE_ADMIN_CATALOG) {
                AdminCatalogManagementScreen(
                    viewModel = viewModel,
                    services = catalogList,
                    isBusy = uiState.isBusy
                )
            }
            composable(AppRoutes.ROUTE_ADMIN_BOOKINGS) {
                AdminBookingsAndDisputesScreen(
                    viewModel = viewModel,
                    bookings = uiState.adminAllBookings,
                    approvedBarbers = uiState.approvedBarbers,
                    tickets = uiState.supportTickets,
                    isBusy = uiState.isBusy
                )
            }
            composable(AppRoutes.ROUTE_ADMIN_POLICIES) {
                AdminPoliciesAndAuditScreen(
                    viewModel = viewModel,
                    platformConfig = uiState.platformConfig,
                    auditLogs = uiState.auditLogs,
                    isBusy = uiState.isBusy
                )
            }
        }
    }

    if (showTopPoliciesSheet) {
        LegalPoliciesSheet(onDismiss = { showTopPoliciesSheet = false })
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
                            windowInsets = TopAppBarDefaults.windowInsets,
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
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                if (statusBanner != null) {
                    Box(
                        modifier = if (!showChrome) {
                            Modifier.windowInsetsPadding(WindowInsets.statusBars)
                        } else {
                            Modifier
                        }
                    ) {
                        DismissibleStatusBanner(message = statusBanner, onDismiss = onDismissBanner)
                    }
                }
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
