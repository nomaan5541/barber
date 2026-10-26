package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BarberProfile
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.ServiceArea
import com.example.data.model.ServiceCategory
import com.example.data.model.SlotLock
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.glass.AmbientAtelierBackdrop
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.DoorCutMotionSpecs
import com.example.ui.motion.LocalReducedMotion
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.util.DetectedDoorLocation
import com.example.util.DoorCutPreferences
import com.example.util.PendingDoorAddressDraft

private fun defaultFallbackBarbers(): List<BarberProfile> = listOf(
    BarberProfile(
        id = "barber_arman_master",
        barberId = "barber_arman_master",
        name = "Arman Siddiqui",
        experienceYears = 7L,
        approvalStatus = "APPROVED",
        isOnline = true,
        ratingAvg = 4.9,
        reviewCount = 48L,
        workStartHour = 9L,
        workEndHour = 20L,
        breakStartHour = 13L,
        breakEndHour = 14L
    ),
    BarberProfile(
        id = "barber_vikram_stylist",
        barberId = "barber_vikram_stylist",
        name = "Vikram Rathore",
        experienceYears = 8L,
        approvalStatus = "APPROVED",
        isOnline = true,
        ratingAvg = 4.8,
        reviewCount = 36L,
        workStartHour = 10L,
        workEndHour = 21L,
        breakStartHour = 14L,
        breakEndHour = 15L
    ),
    BarberProfile(
        id = "barber_danish_pro",
        barberId = "barber_danish_pro",
        name = "Danish Khan",
        experienceYears = 6L,
        approvalStatus = "APPROVED",
        isOnline = true,
        ratingAvg = 4.9,
        reviewCount = 29L,
        workStartHour = 8L,
        workEndHour = 19L,
        breakStartHour = 13L,
        breakEndHour = 14L
    )
)

@Composable
fun CustomerBookingFlowScreen(
    isAuthenticated: Boolean,
    customerName: String,
    customerPhone: String,
    services: List<CatalogService>,
    approvedBarbers: List<BarberProfile>,
    savedAddresses: List<CustomerAddress>,
    serviceAreas: List<ServiceArea>,
    slotLocks: List<SlotLock>,
    platformConfig: PlatformConfig,
    selectedServiceIds: Set<String>,
    selectedBarberId: String?,
    selectedAddressId: String?,
    selectedDateKey: String,
    selectedStartHour: Long?,
    detectedLocation: DetectedDoorLocation?,
    pendingDoorDraft: PendingDoorAddressDraft?,
    isFetchingLocation: Boolean,
    isBusy: Boolean,
    initialStep: Int = if (selectedServiceIds.isNotEmpty()) 2 else 1,
    onToggleService: (String) -> Unit,
    onSelectBarber: (String?) -> Unit,
    onSelectDate: (String) -> Unit,
    onSelectHour: (Long?) -> Unit,
    onSelectAddress: (String?) -> Unit,
    onSaveAddressForAuthenticated: (PendingDoorAddressDraft) -> Unit,
    onSaveDraftForGuest: (PendingDoorAddressDraft) -> Unit,
    onRequestGps: () -> Unit,
    onConfirmBooking: (BarberProfile, CustomerAddress, Long, String) -> Unit,
    onExitFlow: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { DoorCutPreferences.ensureLoaded(context) }
    val localPrefs by DoorCutPreferences.state.collectAsStateWithLifecycle()

    var currentStep by rememberSaveable { mutableIntStateOf(initialStep.coerceIn(1, 4)) }
    var optionalInstructions by rememberSaveable { mutableStateOf("") }
    var activeDetailService by remember { mutableStateOf<CatalogService?>(null) }

    val availableBarbers = remember(approvedBarbers) {
        approvedBarbers.filter { it.approvalStatus == "APPROVED" && it.isOnline }
            .ifEmpty { defaultFallbackBarbers() }
    }
    val chosenBarber = remember(availableBarbers, selectedBarberId) {
        availableBarbers.firstOrNull { it.barberId == selectedBarberId } ?: availableBarbers.first()
    }
    val selectedServices = remember(services, selectedServiceIds) {
        services.filter { selectedServiceIds.contains(it.id) }
    }
    val serviceSubtotalPaise = remember(selectedServices) { selectedServices.sumOf { it.pricePaise } }
    val totalDurationMin = remember(selectedServices) { selectedServices.sumOf { it.durationMinutes } }
    val bookingFeePaise = remember(serviceSubtotalPaise, platformConfig) {
        platformConfig.effectiveCustomerBookingFeePaise(serviceSubtotalPaise)
    }

    val handleBack = {
        if (currentStep > 1) {
            currentStep -= 1
        } else {
            onExitFlow()
        }
    }
    BackHandler { handleBack() }

    val reducedMotion = LocalReducedMotion.current
    AmbientAtelierBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("customer_booking_flow_screen")
        ) {
            // Step Progress Header
            BookingStepTopBar(
                step = currentStep,
                onBack = handleBack
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        DoorCutMotionSpecs.directionalStepTransform(
                            forward = targetState >= initialState,
                            reducedMotion = reducedMotion
                        )
                    },
                    label = "booking_flow_step"
                ) { step ->
                    when (step) {
                        1 -> ChooseServicesStep(
                        services = services,
                        selectedServiceIds = selectedServiceIds,
                        favouriteIds = localPrefs.favouriteServiceIds,
                        subtotalPaise = serviceSubtotalPaise,
                        onToggleService = onToggleService,
                        onToggleFavourite = { DoorCutPreferences.toggleFavouriteService(context, it) },
                        onOpenDetails = { activeDetailService = it },
                        onContinue = { if (selectedServices.isNotEmpty()) currentStep = 2 }
                    )
                    2 -> ChooseDateAndTimeStep(
                        barbers = availableBarbers,
                        chosenBarber = chosenBarber,
                        selectedDateKey = selectedDateKey,
                        selectedStartHour = selectedStartHour,
                        totalDurationMinutes = totalDurationMin,
                        slotLocks = slotLocks,
                        locallyBlockedSlots = localPrefs.barberBlockedSlots,
                        unavailableDates = localPrefs.barberUnavailableDates,
                        onSelectBarber = { onSelectBarber(it.barberId) },
                        onSelectDate = onSelectDate,
                        onSelectHour = onSelectHour,
                        onContinue = { if (selectedStartHour != null) currentStep = 3 }
                    )
                    3 -> ConfirmHomeAddressStep(
                        isAuthenticated = isAuthenticated,
                        savedAddresses = savedAddresses,
                        selectedAddressId = selectedAddressId,
                        detectedLocation = detectedLocation,
                        pendingDoorDraft = pendingDoorDraft,
                        isFetchingLocation = isFetchingLocation,
                        optionalInstructions = optionalInstructions,
                        onOptionalInstructionsChange = { optionalInstructions = it },
                        onSelectSavedAddress = onSelectAddress,
                        onRequestGps = onRequestGps,
                        onSaveAndContinue = { draft ->
                            if (isAuthenticated) {
                                onSaveAddressForAuthenticated(draft)
                            } else {
                                onSaveDraftForGuest(draft)
                            }
                            if (draft.accessInstructions.isNotBlank()) {
                                optionalInstructions = draft.accessInstructions
                            }
                            currentStep = 4
                        },
                        onContinueWithExistingAddress = { currentStep = 4 }
                    )
                    else -> BookingPaymentStep(
                        isAuthenticated = isAuthenticated,
                        customerName = customerName,
                        customerPhone = customerPhone,
                        selectedServices = selectedServices,
                        chosenBarber = chosenBarber,
                        selectedDateKey = selectedDateKey,
                        selectedStartHour = selectedStartHour ?: 10L,
                        savedAddresses = savedAddresses,
                        selectedAddressId = selectedAddressId,
                        pendingDoorDraft = pendingDoorDraft,
                        serviceAreas = serviceAreas,
                        platformConfig = platformConfig,
                        bookingFeePaise = bookingFeePaise,
                        optionalInstructions = optionalInstructions,
                        isBusy = isBusy,
                        onEditStep = { target -> currentStep = target },
                        onConfirmBooking = { resolvedAddr, travelPaise ->
                            onConfirmBooking(chosenBarber, resolvedAddr, travelPaise, optionalInstructions)
                        }
                    )
                    }
                }
            }
        }
    }

    activeDetailService?.let { svc ->
        ServiceDetailBottomSheet(
            service = svc,
            isSelected = selectedServiceIds.contains(svc.id),
            onToggleSelect = { onToggleService(svc.id) },
            onGoToCart = { currentStep = 2 },
            onDismiss = { activeDetailService = null }
        )
    }
}

@Composable
private fun BookingStepTopBar(
    step: Int,
    onBack: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val titles = listOf(
        "1. Choose Services",
        "2. Choose Date & Time",
        "3. Confirm Home Address",
        "4. Booking Payment"
    )
    LiquidGlassSurface(
        strength = GlassMaterialStrength.ChromeBar,
        shape = RoundedCornerShape(0.dp),
        shadowElevation = 2.dp,
        interactivePhysics = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("booking_flow_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titles[(step - 1).coerceIn(0, 3)],
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Step $step of 4",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..4).forEach { idx ->
                    val fillFraction by animateFloatAsState(
                        targetValue = if (idx <= step) 1f else 0f,
                        animationSpec = DoorCutMotionSpecs.liquidPillSpring(reducedMotion),
                        label = "step_progress_$idx"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fillFraction.coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx == step) BrassGold else EspressoHeaderBg
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooseServicesStep(
    services: List<CatalogService>,
    selectedServiceIds: Set<String>,
    favouriteIds: Set<String>,
    subtotalPaise: Long,
    onToggleService: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onOpenDetails: (CatalogService) -> Unit,
    onContinue: () -> Unit
) {
    var categoryFilter by rememberSaveable { mutableStateOf("ALL") }
    val filtered = remember(services, categoryFilter) {
        if (categoryFilter == "ALL") services else services.filter { it.category == categoryFilter }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("step_choose_services")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (selectedServiceIds.isNotEmpty()) 104.dp else 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Tap Add on the services you want at home. All service prices are paid directly to your barber after completion.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = categoryFilter == "ALL",
                            onClick = { categoryFilter = "ALL" },
                            label = { Text("All") },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                    items(ServiceCategory.entries) { cat ->
                        FilterChip(
                            selected = categoryFilter == cat.name,
                            onClick = { categoryFilter = cat.name },
                            label = { Text(cat.displayTitle) },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { svc ->
                SimpleServiceBookingCard(
                    service = svc,
                    isSelected = selectedServiceIds.contains(svc.id),
                    isFavourite = favouriteIds.contains(svc.id),
                    onToggleSelect = { onToggleService(svc.id) },
                    onToggleFavourite = { onToggleFavourite(svc.id) },
                    onOpenDetails = { onOpenDetails(svc) }
                )
            }
        }

        if (selectedServiceIds.isNotEmpty()) {
            StickyServicesContinueBar(
                count = selectedServiceIds.size,
                subtotalPaise = subtotalPaise,
                onContinue = onContinue,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChooseDateAndTimeStep(
    barbers: List<BarberProfile>,
    chosenBarber: BarberProfile,
    selectedDateKey: String,
    selectedStartHour: Long?,
    totalDurationMinutes: Long,
    slotLocks: List<SlotLock>,
    locallyBlockedSlots: Set<String>,
    unavailableDates: Set<String>,
    onSelectBarber: (BarberProfile) -> Unit,
    onSelectDate: (String) -> Unit,
    onSelectHour: (Long?) -> Unit,
    onContinue: () -> Unit
) {
    val todayKey = remember { MarketplaceViewModel.defaultTodayDateKey() }
    val tomorrowKey = remember { MarketplaceViewModel.defaultTomorrowDateKey() }
    var showCalendarDialog by remember { mutableStateOf(false) }

    val dateOptions = remember {
        val fmtKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val fmtLabel = java.text.SimpleDateFormat("EEE, dd MMM", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        (0..13).map { offset ->
            if (offset > 0) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            fmtKey.format(cal.time) to fmtLabel.format(cal.time)
        }
    }

    val lockedHours = remember(slotLocks, chosenBarber.barberId, selectedDateKey) {
        slotLocks
            .filter {
                it.barberId == chosenBarber.barberId &&
                    it.dateKey == selectedDateKey &&
                    (it.status == "HELD" || it.status == "CONFIRMED")
            }
            .map { it.hourSlot }
            .toSet()
    }
    val dateUnavailable = unavailableDates.contains(selectedDateKey)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .testTag("step_choose_datetime"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Estimated Duration Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Estimated service duration: ${totalDurationMinutes.coerceAtLeast(15L)} minutes",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Date Selector: Today | Tomorrow | Choose date
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateQuickPill(
                        label = "Today",
                        selected = selectedDateKey == todayKey,
                        onClick = { onSelectDate(todayKey) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("date_pill_today")
                    )
                    DateQuickPill(
                        label = "Tomorrow",
                        selected = selectedDateKey == tomorrowKey,
                        onClick = { onSelectDate(tomorrowKey) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("date_pill_tomorrow")
                    )
                    OutlinedButton(
                        onClick = { showCalendarDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .testTag("date_pill_choose")
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedDateKey != todayKey && selectedDateKey != tomorrowKey) {
                                dateOptions.firstOrNull { it.first == selectedDateKey }?.second ?: selectedDateKey
                            } else {
                                "Choose date"
                            },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Barber Picker
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Verified Barber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(barbers, key = { it.barberId }) { barber ->
                        val selected = barber.barberId == chosenBarber.barberId
                        LiquidGlassSurface(
                            strength = if (selected) GlassMaterialStrength.AccentEmerald else GlassMaterialStrength.AtelierCard,
                            shape = RoundedCornerShape(18.dp),
                            accentBorderColor = if (selected) SageVerified else null,
                            shadowElevation = if (selected) 4.dp else 1.dp,
                            onClick = { onSelectBarber(barber) },
                            modifier = Modifier.width(214.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BarberAvatarBadge(name = barber.name, isOnline = barber.isOnline, size = 44.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = barber.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = BrassGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${String.format("%.1f", barber.ratingAvg)} · ${barber.experienceYears}y exp",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Time Slots Grid (Reservable vs Clearly Labeled Unavailable)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Available Time Slots",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (dateUnavailable) {
                    Text(
                        text = "${chosenBarber.name} has marked this date unavailable. Please pick another date or barber.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (hour in chosenBarber.workStartHour until chosenBarber.workEndHour) {
                            val isBreak = hour >= chosenBarber.breakStartHour && hour < chosenBarber.breakEndHour
                            val isBooked = lockedHours.contains(hour) || locallyBlockedSlots.contains("${selectedDateKey}_$hour")
                            val isReservable = !isBreak && !isBooked
                            val isSelected = selectedStartHour == hour
                            val slotBg by animateColorAsState(
                                targetValue = when {
                                    isSelected -> EspressoHeaderBg
                                    isReservable -> MaterialTheme.colorScheme.surface
                                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                animationSpec = tween(160),
                                label = "slot_bg_$hour"
                            )

                            Surface(
                                onClick = { if (isReservable) onSelectHour(hour) },
                                enabled = isReservable,
                                shape = RoundedCornerShape(14.dp),
                                color = slotBg,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    when {
                                        isSelected -> BrassGold
                                        isReservable -> MaterialTheme.colorScheme.outlineVariant
                                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    }
                                ),
                                shadowElevation = if (isSelected) 4.dp else 0.dp,
                                modifier = Modifier
                                    .tactilePressPhysics(enabled = isReservable, pressedScale = 0.94f, maxTiltDegrees = 0f, enableHaptics = true)
                                    .testTag("slot_button_$hour")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = CurrencyUtils.formatHourSlot(hour),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isSelected -> Color.White
                                            isReservable -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        }
                                    )
                                    Text(
                                        text = when {
                                            isSelected -> "Selected"
                                            isBreak -> "Unavailable · Break"
                                            isBooked -> "Unavailable"
                                            else -> "Available"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            isSelected -> Color(0xFFFFDF9E)
                                            isReservable -> SageVerified
                                            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onContinue,
                enabled = selectedStartHour != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EspressoHeaderBg,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .tactilePressPhysics(enabled = selectedStartHour != null, enableHaptics = true)
                    .testTag("datetime_continue_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedStartHour != null) {
                        "Continue with ${CurrencyUtils.formatHourSlot(selectedStartHour)}"
                    } else {
                        "Select a Time Slot to Continue"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showCalendarDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            title = { Text("Choose Appointment Date", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(320.dp)
                ) {
                    items(dateOptions) { (key, label) ->
                        Surface(
                            onClick = {
                                onSelectDate(key)
                                showCalendarDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (key == selectedDateKey) EspressoHeaderBg else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (key == selectedDateKey) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalendarDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DateQuickPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) EspressoHeaderBg else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) BrassGold else MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (selected) 3.dp else 0.dp,
        modifier = modifier
            .height(50.dp)
            .tactilePressPhysics(pressedScale = 0.95f, maxTiltDegrees = 0f, enableHaptics = true)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
