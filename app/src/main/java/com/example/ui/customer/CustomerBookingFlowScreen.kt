package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.ServiceArea
import com.example.data.model.ServiceCategory
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

@Composable
fun CustomerBookingFlowScreen(
    isAuthenticated: Boolean,
    customerName: String,
    customerPhone: String,
    services: List<CatalogService>,
    savedAddresses: List<CustomerAddress>,
    serviceAreas: List<ServiceArea>,
    platformConfig: PlatformConfig,
    selectedServiceIds: Set<String>,
    selectedAddressId: String?,
    selectedDateKey: String,
    selectedStartHour: Long?,
    bookingMode: String,
    recipientGender: String,
    recipientAge: Long,
    preferLadyBarber: Boolean,
    detectedLocation: DetectedDoorLocation?,
    pendingDoorDraft: PendingDoorAddressDraft?,
    isFetchingLocation: Boolean,
    isBusy: Boolean,
    initialStep: Int = if (selectedServiceIds.isNotEmpty()) 2 else 1,
    onToggleService: (String) -> Unit,
    onSelectDate: (String) -> Unit,
    onSelectHour: (Long?) -> Unit,
    onSetBookingMode: (String) -> Unit,
    onUpdateRecipientPreferences: (gender: String, age: Long, preferLadyBarber: Boolean) -> Unit,
    onSelectAddress: (String?) -> Unit,
    onSaveAddressForAuthenticated: (PendingDoorAddressDraft) -> Unit,
    onRequestGps: () -> Unit,
    onConfirmOpenDispatchBooking: (CustomerAddress, Long, String, String) -> Unit,
    onExitFlow: () -> Unit,
    onLocationPinned: ((DetectedDoorLocation) -> Unit)? = null
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { DoorCutPreferences.ensureLoaded(context) }
    val localPrefs by DoorCutPreferences.state.collectAsStateWithLifecycle()

    var currentStep by rememberSaveable { mutableIntStateOf(initialStep.coerceIn(1, 4)) }
    var optionalInstructions by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable(customerPhone) { mutableStateOf(customerPhone) }
    var activeDetailService by remember { mutableStateOf<CatalogService?>(null) }

    val selectedServices = remember(services, selectedServiceIds) {
        services.filter { selectedServiceIds.contains(it.id) }
    }
    val hasLadiesService = remember(selectedServices) {
        selectedServices.any { it.category == "LADIES_STYLING" }
    }

    LaunchedEffect(hasLadiesService) {
        if (hasLadiesService && recipientGender != "FEMALE") {
            onUpdateRecipientPreferences("FEMALE", recipientAge, true)
        }
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
                        2 -> ChoosePreferencesAndTimingStep(
                            bookingMode = bookingMode,
                            selectedDateKey = selectedDateKey,
                            selectedStartHour = selectedStartHour,
                            totalDurationMinutes = totalDurationMin,
                            recipientGender = recipientGender,
                            recipientAge = recipientAge,
                            preferLadyBarber = preferLadyBarber,
                            hasLadiesService = hasLadiesService,
                            contactPhone = contactPhone,
                            onContactPhoneChange = { contactPhone = it },
                            onSetBookingMode = onSetBookingMode,
                            onSelectDate = onSelectDate,
                            onSelectHour = onSelectHour,
                            onUpdateRecipientPreferences = onUpdateRecipientPreferences,
                            onContinue = {
                                if (bookingMode == "ASAP" || selectedStartHour != null) {
                                    currentStep = 3
                                }
                            }
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
                                onSaveAddressForAuthenticated(draft)
                                if (draft.accessInstructions.isNotBlank()) {
                                    optionalInstructions = draft.accessInstructions
                                }
                                currentStep = 4
                            },
                            onContinueWithExistingAddress = { currentStep = 4 },
                            onLocationPinned = onLocationPinned
                        )
                        else -> BookingPaymentStep(
                            isAuthenticated = isAuthenticated,
                            customerName = customerName,
                            customerPhone = contactPhone.ifBlank { customerPhone },
                            recipientGender = recipientGender,
                            recipientAge = recipientAge,
                            preferLadyBarber = preferLadyBarber,
                            bookingMode = bookingMode,
                            selectedServices = selectedServices,
                            selectedDateKey = selectedDateKey,
                            selectedStartHour = selectedStartHour ?: 10L,
                            savedAddresses = savedAddresses,
                            selectedAddressId = selectedAddressId,
                            pendingDoorDraft = pendingDoorDraft,
                            detectedLocation = detectedLocation,
                            serviceAreas = serviceAreas,
                            platformConfig = platformConfig,
                            bookingFeePaise = bookingFeePaise,
                            optionalInstructions = optionalInstructions,
                            isBusy = isBusy,
                            onEditStep = { target -> currentStep = target },
                            onConfirmBooking = { resolvedAddr, travelPaise ->
                                onConfirmOpenDispatchBooking(
                                    resolvedAddr,
                                    travelPaise,
                                    optionalInstructions,
                                    contactPhone.ifBlank { customerPhone }
                                )
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
        "2. Stylist Preferences & Timing",
        "3. Confirm Home GPS Door",
        "4. Review & Dispatch Barber"
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
                        text = "Step $step of 4 · Smart Auto-Dispatch",
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
                    text = "Tap Add Service at the bottom of any card. Nearby verified Barbers & Lady Hair Stylists are automatically matched to your order.",
                    style = MaterialTheme.typography.bodyMedium,
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
private fun ChoosePreferencesAndTimingStep(
    bookingMode: String,
    selectedDateKey: String,
    selectedStartHour: Long?,
    totalDurationMinutes: Long,
    recipientGender: String,
    recipientAge: Long,
    preferLadyBarber: Boolean,
    hasLadiesService: Boolean,
    contactPhone: String,
    onContactPhoneChange: (String) -> Unit,
    onSetBookingMode: (String) -> Unit,
    onUpdateRecipientPreferences: (gender: String, age: Long, preferLadyBarber: Boolean) -> Unit,
    onSelectDate: (String) -> Unit,
    onSelectHour: (Long?) -> Unit,
    onContinue: () -> Unit
) {
    val todayKey = remember { MarketplaceViewModel.defaultTodayDateKey() }
    val tomorrowKey = remember { MarketplaceViewModel.defaultTomorrowDateKey() }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var ageText by rememberSaveable(recipientAge) { mutableStateOf(recipientAge.toString()) }

    val dateOptions = remember {
        val fmtKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val fmtLabel = java.text.SimpleDateFormat("EEE, dd MMM", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        (0..13).map { offset ->
            if (offset > 0) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            fmtKey.format(cal.time) to fmtLabel.format(cal.time)
        }
    }

    val isReadyToContinue = (bookingMode == "ASAP" || selectedStartHour != null) &&
        contactPhone.filter { it.isDigit() }.length >= 10

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .testTag("step_choose_datetime"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Open Dispatch Explanation Banner (No Manual Barber Picker)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SageVerified.copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, SageVerified.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EspressoHeaderBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Radar, contentDescription = null, tint = BrassGold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Uber-Style Smart Stylist Appointment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No manual barber picking needed. Verified nearby stylists see your service, gender & timing requirements and accept instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Recipient Gender, Age, Phone & Lady Hair Barber Preference Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Client Details for Barber Match",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Who is this haircut / grooming session for?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "MALE" to "Male",
                            "FEMALE" to "Female / Lady",
                            "CHILD" to "Child",
                            "OTHER" to "Other"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = recipientGender == code,
                                onClick = {
                                    val defaultLadyPref = if (code == "FEMALE") true else false
                                    onUpdateRecipientPreferences(code, recipientAge, defaultLadyPref)
                                },
                                label = { Text(label, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("recipient_gender_chip_$code")
                            )
                        }
                    }

                    // Conditional "Need a Lady Hair Barber?" Toggle when Female or Ladies Styling is chosen
                    AnimatedVisibility(visible = recipientGender == "FEMALE" || hasLadiesService) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF8E24AA).copy(alpha = 0.10f),
                            border = BorderStroke(1.5.dp, Color(0xFF8E24AA).copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lady_barber_preference_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Face3,
                                        contentDescription = null,
                                        tint = Color(0xFF6A1B9A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Need a Lady Hair Barber?",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Appoint a verified Female Hair Stylist for comfortable ladies' home haircut & styling.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = preferLadyBarber,
                                    onCheckedChange = { checked ->
                                        onUpdateRecipientPreferences(recipientGender, recipientAge, checked)
                                    },
                                    modifier = Modifier.testTag("prefer_lady_barber_switch")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { raw ->
                                val clean = raw.filter { it.isDigit() }.take(3)
                                ageText = clean
                                val parsed = clean.toLongOrNull()?.coerceIn(1L, 120L) ?: recipientAge
                                onUpdateRecipientPreferences(recipientGender, parsed, preferLadyBarber)
                            },
                            label = { Text("Age *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(110.dp)
                                .testTag("recipient_age_input")
                        )

                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { raw ->
                                onContactPhoneChange(raw.filter { it.isDigit() || it == '+' }.take(14))
                            },
                            label = { Text("Mobile Number for Barber *") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            placeholder = { Text("Enter 10-digit mobile number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("recipient_phone_input")
                        )
                    }
                }
            }
        }

        // 3. Dispatch Timing Mode: ASAP Express vs Scheduled Slot
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "When Should Stylist Arrive?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "~${totalDurationMinutes.coerceAtLeast(15L)} mins",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DispatchModeCard(
                            title = "ASAP Now",
                            subtitle = "20–35 min arrival",
                            icon = Icons.Filled.Bolt,
                            selected = bookingMode == "ASAP",
                            onClick = { onSetBookingMode("ASAP") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("booking_mode_asap")
                        )
                        DispatchModeCard(
                            title = "Schedule Slot",
                            subtitle = "Pick date & hour",
                            icon = Icons.Filled.Schedule,
                            selected = bookingMode == "SCHEDULED",
                            onClick = { onSetBookingMode("SCHEDULED") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("booking_mode_scheduled")
                        )
                    }

                    if (bookingMode == "ASAP") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BrassGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ASAP Express Dispatch broadcasts your request immediately to online stylists within 5 km so the nearest available partner can head straight to your door.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        // Date Selector: Today | Tomorrow | Choose date
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

                        Text(
                            text = "Select Preferred Arrival Window",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (hour in 9L..20L) {
                                val isSelected = selectedStartHour == hour
                                val slotBg by animateColorAsState(
                                    targetValue = if (isSelected) EspressoHeaderBg else MaterialTheme.colorScheme.surface,
                                    animationSpec = tween(150),
                                    label = "slot_bg_$hour"
                                )
                                Surface(
                                    onClick = { onSelectHour(hour) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = slotBg,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) BrassGold else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shadowElevation = if (isSelected) 3.dp else 0.dp,
                                    modifier = Modifier
                                        .tactilePressPhysics(pressedScale = 0.94f, maxTiltDegrees = 0f, enableHaptics = true)
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
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isSelected) "Selected" else "Open Pool",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color(0xFFFFDF9E) else SageVerified
                                        )
                                    }
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
                enabled = isReadyToContinue,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EspressoHeaderBg,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .tactilePressPhysics(enabled = isReadyToContinue, enableHaptics = true)
                    .testTag("datetime_continue_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        contactPhone.filter { it.isDigit() }.length < 10 -> "Enter 10-Digit Mobile Number to Continue"
                        bookingMode == "ASAP" -> "Continue with ASAP Express Dispatch"
                        selectedStartHour != null -> "Continue with ${CurrencyUtils.formatHourSlot(selectedStartHour)}"
                        else -> "Select Arrival Time to Continue"
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
private fun DispatchModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) EspressoHeaderBg else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) BrassGold else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .height(68.dp)
            .tactilePressPhysics(pressedScale = 0.96f, maxTiltDegrees = 0f, enableHaptics = true)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) BrassGold else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color(0xFFFFDF9E) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
