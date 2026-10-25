package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BarberProfile
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.ServiceArea
import com.example.data.model.SlotLock
import com.example.data.model.UserAccount
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.CustomerDirectPriceCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.serviceVisualFor
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.util.CustomerPreferencesStore
import com.example.util.DetectedDoorLocation
import com.example.util.DoorCutNotificationHelper
import com.example.util.DoorstepPaymentOption
import com.example.util.LocationHelper
import com.example.util.PendingDoorAddressDraft

/**
 * Authenticated Cart & Card Payment screen (`AppRoutes.ROUTE_CART`), placed right after Bookings
 * in the bottom navigation bar. Allows reviewing selected haircuts, choosing Credit/Debit Card POS,
 * UPI QR, or Cash at Door, and confirming a home barber time slot.
 */
@Composable
fun CustomerCartScreen(
    viewModel: MarketplaceViewModel,
    userAccount: UserAccount,
    allServices: List<CatalogService>,
    approvedBarbers: List<BarberProfile>,
    addresses: List<CustomerAddress>,
    serviceAreas: List<ServiceArea>,
    slotLocks: List<SlotLock>,
    platformConfig: PlatformConfig,
    selectedServiceIds: Set<String>,
    selectedBarberId: String?,
    selectedAddressId: String?,
    selectedDateKey: String,
    selectedStartHour: Long?,
    isBusy: Boolean,
    onBrowseServices: () -> Unit,
    onOpenDoorSetup: () -> Unit,
    onNavigateToAddresses: () -> Unit,
    onBookingConfirmed: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { CustomerPreferencesStore.ensureLoaded(context) }
    val prefs by CustomerPreferencesStore.state.collectAsStateWithLifecycle()

    var isSlotCheckoutOpen by rememberSaveable { mutableStateOf(false) }
    var inspectedService by remember { mutableStateOf<CatalogService?>(null) }

    val catalog = remember(allServices) { mergeWithCanonicalCatalog(allServices) }
    val selectedServices = remember(catalog, selectedServiceIds) {
        catalog.filter { selectedServiceIds.contains(it.id) }
    }

    val activeAddress = remember(addresses, selectedAddressId) {
        addresses.firstOrNull { it.id == selectedAddressId }
            ?: addresses.firstOrNull { it.isDefault }
            ?: addresses.firstOrNull()
    }
    val detectedLocation by viewModel.detectedLocation.collectAsStateWithLifecycle()
    val userLat = activeAddress?.latitude?.takeIf { it != 0.0 } ?: detectedLocation?.latitude ?: 0.0
    val userLon = activeAddress?.longitude?.takeIf { it != 0.0 } ?: detectedLocation?.longitude ?: 0.0

    val rankedSpecialists = remember(approvedBarbers, userLat, userLon) {
        approvedBarbers.mapIndexed { idx, barber ->
            val distKm = LocationHelper.computeEffectiveDistanceKm(
                userLat = userLat,
                userLon = userLon,
                shopLat = barber.effectiveLatitude(),
                shopLon = barber.effectiveLongitude(),
                fallbackIndex = idx
            )
            barber to distKm
        }.sortedWith(compareBy<Pair<BarberProfile, Double>> { !it.first.isOnline }.thenBy { it.second })
    }

    val matchedZone = serviceAreas.firstOrNull { zone ->
        activeAddress != null && (zone.id == activeAddress.serviceAreaId || zone.postalCodes.contains(activeAddress.postalCode))
    }
    val travelChargePaise = matchedZone?.travelChargePaise ?: 0L

    if (isSlotCheckoutOpen && selectedServices.isNotEmpty()) {
        BackHandler { isSlotCheckoutOpen = false }
        CustomerCheckoutFlowScreen(
            viewModel = viewModel,
            userAccount = userAccount,
            selectedServices = selectedServices,
            rankedBarbers = rankedSpecialists,
            addresses = addresses,
            serviceAreas = serviceAreas,
            slotLocks = slotLocks,
            platformConfig = platformConfig,
            selectedBarberId = selectedBarberId,
            selectedAddressId = selectedAddressId,
            selectedDateKey = selectedDateKey,
            selectedStartHour = selectedStartHour,
            isBusy = isBusy,
            initialNotesPrefix = prefs.buildBookingPreferencesPrefix(),
            onBack = { isSlotCheckoutOpen = false },
            onOpenDoorModal = onOpenDoorSetup,
            onNavigateToAddresses = onNavigateToAddresses,
            onBookingConfirmed = {
                DoorCutNotificationHelper.sendVisitNotification(
                    context = context,
                    title = "Home Haircut Slot Requested • ₹0 Fee",
                    message = "Your ${selectedServices.joinToString { it.name }} request (${prefs.paymentOption.shortBadge} at door) has been sent to your barber."
                )
                isSlotCheckoutOpen = false
                onBookingConfirmed()
            }
        )
        return
    }

    inspectedService?.let { svc ->
        ServiceDetailBottomSheet(
            service = svc,
            isSelected = selectedServiceIds.contains(svc.id),
            onToggleSelect = { viewModel.toggleServiceSelection(svc.id) },
            onDismiss = { inspectedService = null }
        )
    }

    CartContentLayout(
        catalog = catalog,
        selectedServices = selectedServices,
        selectedServiceIds = selectedServiceIds,
        addressSummary = activeAddress?.let { "${it.label}: ${it.flatHouse}, ${it.streetArea}" },
        travelChargePaise = travelChargePaise,
        paymentOption = prefs.paymentOption,
        savedCardLabel = prefs.savedCardLabel,
        savedCardLast4 = prefs.savedCardLast4,
        savedUpiId = prefs.savedUpiId,
        onSelectPaymentOption = { opt ->
            CustomerPreferencesStore.update(context) { it.copy(paymentOption = opt) }
        },
        onUpdateCardDetails = { label, last4 ->
            CustomerPreferencesStore.update(context) {
                it.copy(savedCardLabel = label, savedCardLast4 = last4)
            }
        },
        onUpdateUpiId = { upi ->
            CustomerPreferencesStore.update(context) { it.copy(savedUpiId = upi) }
        },
        onToggleService = { viewModel.toggleServiceSelection(it) },
        onClearCart = { viewModel.clearSelectedServices() },
        onInspectService = { inspectedService = it },
        onBrowseServices = onBrowseServices,
        onOpenDoorSetup = onOpenDoorSetup,
        bottomActionSlot = {
            Button(
                onClick = { isSlotCheckoutOpen = true },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EspressoHeaderBg,
                    contentColor = Color(0xFFFFDF9E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("cart_proceed_slot_button")
            ) {
                Text(
                    text = "Select Barber & Time Slot • ₹0 Fee",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BrassGold
                )
            }
        }
    )
}

/**
 * Guest Cart & Card Payment screen when user has not yet signed in with Google.
 */
@Composable
fun GuestCartScreen(
    detectedLocation: DetectedDoorLocation?,
    pendingDoorDraft: PendingDoorAddressDraft?,
    selectedServiceIds: Set<String>,
    onToggleService: (String) -> Unit,
    onClearCart: () -> Unit,
    onBrowseServices: () -> Unit,
    onOpenDoorSetup: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    LaunchedEffect(Unit) { CustomerPreferencesStore.ensureLoaded(context) }
    val prefs by CustomerPreferencesStore.state.collectAsStateWithLifecycle()

    var isSigningIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var inspectedService by remember { mutableStateOf<CatalogService?>(null) }

    val catalog = remember { canonicalGroomingCatalog() }
    val selectedServices = remember(catalog, selectedServiceIds) {
        catalog.filter { selectedServiceIds.contains(it.id) }
    }

    val addressSummary = remember(pendingDoorDraft, detectedLocation) {
        when {
            pendingDoorDraft != null -> "${pendingDoorDraft.label}: ${pendingDoorDraft.combinedFlatHouse()}, ${pendingDoorDraft.streetArea}"
            detectedLocation != null -> "${detectedLocation.localityName} (${detectedLocation.formattedCoordinates()}) • Tap to add Door No."
            else -> null
        }
    }

    inspectedService?.let { svc ->
        ServiceDetailBottomSheet(
            service = svc,
            isSelected = selectedServiceIds.contains(svc.id),
            onToggleSelect = { onToggleService(svc.id) },
            onDismiss = { inspectedService = null }
        )
    }

    CartContentLayout(
        catalog = catalog,
        selectedServices = selectedServices,
        selectedServiceIds = selectedServiceIds,
        addressSummary = addressSummary,
        travelChargePaise = 0L,
        paymentOption = prefs.paymentOption,
        savedCardLabel = prefs.savedCardLabel,
        savedCardLast4 = prefs.savedCardLast4,
        savedUpiId = prefs.savedUpiId,
        onSelectPaymentOption = { opt ->
            CustomerPreferencesStore.update(context) { it.copy(paymentOption = opt) }
        },
        onUpdateCardDetails = { label, last4 ->
            CustomerPreferencesStore.update(context) {
                it.copy(savedCardLabel = label, savedCardLast4 = last4)
            }
        },
        onUpdateUpiId = { upi ->
            CustomerPreferencesStore.update(context) { it.copy(savedUpiId = upi) }
        },
        onToggleService = onToggleService,
        onClearCart = onClearCart,
        onInspectService = { inspectedService = it },
        onBrowseServices = onBrowseServices,
        onOpenDoorSetup = onOpenDoorSetup,
        bottomActionSlot = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (signInError != null) {
                    Text(
                        text = signInError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = {
                        isSigningIn = true
                        signInError = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isSigningIn = false
                                onAuthSuccess()
                            },
                            onAuthError = { err ->
                                isSigningIn = false
                                signInError = err
                            },
                            scope = scope,
                            onAuthCancelled = { isSigningIn = false }
                        )
                    },
                    enabled = !isSigningIn,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EspressoHeaderBg,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("cart_google_sign_in_button")
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrassGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sign In with Google & Choose Slot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun CartContentLayout(
    catalog: List<CatalogService>,
    selectedServices: List<CatalogService>,
    selectedServiceIds: Set<String>,
    addressSummary: String?,
    travelChargePaise: Long,
    paymentOption: DoorstepPaymentOption,
    savedCardLabel: String,
    savedCardLast4: String,
    savedUpiId: String,
    onSelectPaymentOption: (DoorstepPaymentOption) -> Unit,
    onUpdateCardDetails: (String, String) -> Unit,
    onUpdateUpiId: (String) -> Unit,
    onToggleService: (String) -> Unit,
    onClearCart: () -> Unit,
    onInspectService: (CatalogService) -> Unit,
    onBrowseServices: () -> Unit,
    onOpenDoorSetup: () -> Unit,
    bottomActionSlot: @Composable () -> Unit
) {
    val quickSuggestions = remember(catalog, selectedServiceIds) {
        val priorityIds = listOf(
            "svc_normal_head_cut",
            "svc_traditional_cut",
            "svc_beard_trim",
            "svc_traditional_shave",
            "svc_scalp_oil",
            "svc_brow_neck"
        )
        priorityIds.mapNotNull { id -> catalog.firstOrNull { it.id == id } }
            .filter { !selectedServiceIds.contains(it.id) }
    }

    val subtotalPaise = selectedServices.sumOf { it.pricePaise }
    val totalMinutes = selectedServices.sumOf { it.durationMinutes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_cart_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = stringResource(R.string.cart_eyebrow),
                title = stringResource(R.string.cart_title),
                subtitle = stringResource(R.string.cart_subtitle),
                trailing = {
                    if (selectedServices.isNotEmpty()) {
                        StatusBadge("${selectedServices.size} ITEMS • ${totalMinutes}M")
                    } else {
                        StatusBadge("₹0 FEE")
                    }
                }
            )
        }

        // Doorstep Delivery Address Strip
        item {
            Surface(
                onClick = onOpenDoorSetup,
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cart_door_address_card")
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
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = BrassGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Home Visit Doorstep",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = addressSummary ?: "Tap to detect GPS coordinates & enter House/Door No.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = onOpenDoorSetup) {
                        Text(if (addressSummary == null) "Set Door" else "Edit")
                    }
                }
            }
        }

        if (selectedServices.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.ShoppingBag,
                    title = "Your Grooming Cart Is Empty",
                    subtitle = "Add a Normal Head Cut (₹199), Traditional Haircut (₹229), Skin Fade, or Beard Trim below—always with ₹0 customer platform fee.",
                    action = {
                        Button(
                            onClick = onBrowseServices,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EspressoHeaderBg),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("cart_browse_services_button")
                        ) {
                            Icon(Icons.Filled.ContentCut, contentDescription = null, tint = BrassGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Explore All Haircuts & Combos", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Services (${selectedServices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onClearCart,
                        modifier = Modifier.testTag("cart_clear_all_button")
                    ) {
                        Text("Clear Cart", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            items(selectedServices, key = { it.id }) { svc ->
                CartServiceItemRow(
                    service = svc,
                    onInspect = { onInspectService(svc) },
                    onRemove = { onToggleService(svc.id) }
                )
            }
        }

        // Quick-Add Popular Cuts & Add-Ons
        if (quickSuggestions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (selectedServices.isEmpty()) {
                            "Quick-Add Popular Haircuts"
                        } else {
                            "Complete Your Cut (Quick Add-Ons)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(quickSuggestions, key = { "suggest_${it.id}" }) { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.width(235.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = suggestion.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${CurrencyUtils.formatRupees(suggestion.pricePaise)} • ${suggestion.durationMinutes}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedButton(
                                        onClick = { onToggleService(suggestion.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("cart_quick_add_${suggestion.id}")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add to Cart", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Doorstep Card / UPI / Cash Payment Selector (Always Accessible)
        item {
            DoorstepCardAndPaymentSelector(
                selectedOption = paymentOption,
                savedCardLabel = savedCardLabel,
                savedCardLast4 = savedCardLast4,
                savedUpiId = savedUpiId,
                onSelectOption = onSelectPaymentOption,
                onUpdateCardDetails = onUpdateCardDetails,
                onUpdateUpiId = onUpdateUpiId
            )
        }

        // Zero-Fee Price Summary & Primary Action
        if (selectedServices.isNotEmpty()) {
            item {
                CustomerDirectPriceCard(
                    serviceSubtotalPaise = subtotalPaise,
                    approvedAddOnPaise = 0L,
                    travelChargePaise = travelChargePaise,
                    barberServicePayablePaise = subtotalPaise + travelChargePaise,
                    directPaymentStatusLabel = "Preferred at Door: ${paymentOption.title}"
                )
            }

            item {
                bottomActionSlot()
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CartServiceItemRow(
    service: CatalogService,
    onInspect: () -> Unit,
    onRemove: () -> Unit
) {
    val visual = remember(service.id, service.category, service.name, service.isPackage) {
        serviceVisualFor(service)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_${service.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = visual.drawableRes),
                contentDescription = service.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = CurrencyUtils.formatRupees(service.pricePaise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• ${service.durationMinutes} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onInspect,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "View ${service.name} details",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("cart_remove_${service.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Remove ${service.name} from cart",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DoorstepCardAndPaymentSelector(
    selectedOption: DoorstepPaymentOption,
    savedCardLabel: String,
    savedCardLast4: String,
    savedUpiId: String,
    onSelectOption: (DoorstepPaymentOption) -> Unit,
    onUpdateCardDetails: (String, String) -> Unit,
    onUpdateUpiId: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cart_payment_options_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CARD, UPI & CASH OPTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pay Barber at Doorstep",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge("₹0 EXTRA FEE")
            }

            DoorstepPaymentOption.entries.forEach { option ->
                val isSelected = selectedOption == option
                val icon: ImageVector = when (option) {
                    DoorstepPaymentOption.CARD_POS_AT_DOOR -> Icons.Filled.CreditCard
                    DoorstepPaymentOption.UPI_AT_DOOR -> Icons.Filled.QrCodeScanner
                    DoorstepPaymentOption.CASH_AT_DOOR -> Icons.Filled.Payments
                }
                Surface(
                    onClick = { onSelectOption(option) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.8.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_option_${option.code.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectOption(option) }
                        )
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = option.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (selectedOption == DoorstepPaymentOption.CARD_POS_AT_DOOR) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = savedCardLabel,
                        onValueChange = { onUpdateCardDetails(it, savedCardLast4) },
                        label = { Text("Card Nickname (Optional)") },
                        placeholder = { Text("e.g., Visa / HDFC") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = savedCardLast4,
                        onValueChange = { digits ->
                            onUpdateCardDetails(savedCardLabel, digits.filter { it.isDigit() }.take(4))
                        },
                        label = { Text("Last 4") },
                        placeholder = { Text("4242") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(104.dp)
                    )
                }
                Text(
                    text = "No card number or CVV is collected in-app. Your barber brings a wireless Tap & PIN POS terminal to your door.",
                    style = MaterialTheme.typography.labelSmall,
                    color = SageVerified
                )
            } else if (selectedOption == DoorstepPaymentOption.UPI_AT_DOOR) {
                OutlinedTextField(
                    value = savedUpiId,
                    onValueChange = onUpdateUpiId,
                    label = { Text("Preferred UPI Handle (Optional)") },
                    placeholder = { Text("e.g., name@okicici") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
