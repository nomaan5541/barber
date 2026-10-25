package com.example.ui.customer

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BarberProfile
import com.example.data.model.CatalogService
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.ServiceArea
import com.example.data.model.SlotLock
import com.example.data.model.UserAccount
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkillPill
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.LocationHelper

@Composable
fun CustomerDiscoverAndCheckoutScreen(
    viewModel: MarketplaceViewModel,
    userAccount: UserAccount,
    servicesState: UiState<List<CatalogService>>,
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
    onNavigateToDoorSetup: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToAddresses: () -> Unit
) {
    val context = LocalContext.current
    var isCheckoutOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val detectedLocation by viewModel.detectedLocation.collectAsStateWithLifecycle()
    val isFetchingLocation by viewModel.isFetchingLocation.collectAsStateWithLifecycle()
    val hasAttemptedLoginLocationFetch by viewModel.hasAttemptedLoginLocationFetch.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.autoFetchLocationAndPromptDoor(
                context = context,
                promptForHomeAddressAfterFetch = false
            )
        }
    }

    LaunchedEffect(hasAttemptedLoginLocationFetch) {
        if (!hasAttemptedLoginLocationFetch) {
            viewModel.markLoginLocationAttempted()
            if (LocationHelper.hasLocationPermission(context)) {
                viewModel.autoFetchLocationAndPromptDoor(
                    context = context,
                    promptForHomeAddressAfterFetch = false
                )
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    val liveServices = (servicesState as? UiState.Success)?.data ?: emptyList()
    val displayServices = remember(liveServices) {
        liveServices.ifEmpty { defaultServiceGridPlaceholders() }
    }
    val isShowingFallbackPlaceholders = liveServices.isEmpty()

    val selectedServices = remember(displayServices, selectedServiceIds) {
        displayServices.filter { selectedServiceIds.contains(it.id) }
    }

    val activeAddress = remember(addresses, selectedAddressId) {
        addresses.firstOrNull { it.id == selectedAddressId }
            ?: addresses.firstOrNull { it.isDefault }
            ?: addresses.firstOrNull()
    }

    val userLat = activeAddress?.latitude?.takeIf { it != 0.0 } ?: detectedLocation?.latitude ?: 0.0
    val userLon = activeAddress?.longitude?.takeIf { it != 0.0 } ?: detectedLocation?.longitude ?: 0.0
    val localityHint = activeAddress?.serviceAreaName?.substringBefore("(")?.trim()
        ?.ifBlank { detectedLocation?.localityName.orEmpty() }
        ?: detectedLocation?.localityName.orEmpty()

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

    if (isCheckoutOpen && selectedServices.isNotEmpty()) {
        BackHandler { isCheckoutOpen = false }
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
            onBack = { isCheckoutOpen = false },
            onOpenDoorModal = onNavigateToDoorSetup,
            onNavigateToAddresses = onNavigateToAddresses,
            onBookingConfirmed = {
                isCheckoutOpen = false
                onNavigateToBookings()
            }
        )
        return
    }

    val filteredServices = remember(displayServices, searchQuery, selectedCategory) {
        displayServices.filter { svc ->
            val matchesQuery = searchQuery.isBlank() ||
                svc.name.contains(searchQuery, ignoreCase = true) ||
                svc.description.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || svc.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    val defaultHaircutId = remember(displayServices) {
        displayServices.firstOrNull { it.category == "HAIRCUTS" }?.id ?: displayServices.firstOrNull()?.id
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("service_selection_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Top Doorstep GPS Coordinates & House/Door Selector Capsule
            item {
                DoorstepLocationHeaderCard(
                    activeAddress = activeAddress,
                    detectedLocation = detectedLocation,
                    isFetchingLocation = isFetchingLocation,
                    onOpenDoorSetup = onNavigateToDoorSetup,
                    onRefreshGps = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.autoFetchLocationAndPromptDoor(
                                context = context,
                                promptForHomeAddressAfterFetch = false
                            )
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )
            }

            // 2. Search Bar + Grooming Category Filter Chips
            item {
                ServiceCategoryFilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it }
                )
            }

            // 3. Service Selection Header + 2-Column Service Grid (with Loading & Catalog Placeholders)
            item {
                SectionHeader(
                    eyebrow = stringResource(R.string.service_selection_eyebrow),
                    title = stringResource(R.string.service_selection_title),
                    subtitle = stringResource(R.string.service_selection_subtitle),
                    trailing = {
                        when {
                            selectedServices.isNotEmpty() -> StatusBadge("${selectedServices.size} SELECTED")
                            isShowingFallbackPlaceholders -> StatusBadge(stringResource(R.string.service_grid_placeholder_badge))
                        }
                    }
                )
            }

            when (servicesState) {
                is UiState.Loading -> {
                    item {
                        ServiceGridLoadingPlaceholders(count = 6)
                    }
                }
                is UiState.Error -> {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.ContentCut,
                            title = "Could not load grooming services",
                            subtitle = servicesState.message
                        )
                    }
                }
                is UiState.Success -> {
                    item {
                        if (filteredServices.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Filled.ContentCut,
                                title = "No Matching Services",
                                subtitle = "Try clearing your search query or choosing another grooming category."
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ServiceSelectionGrid(
                                    services = filteredServices,
                                    selectedServiceIds = selectedServiceIds,
                                    isPlaceholderPreview = isShowingFallbackPlaceholders,
                                    onToggleService = { serviceId ->
                                        viewModel.toggleServiceSelection(serviceId)
                                    }
                                )
                                if (liveServices.isEmpty() && userAccount.role == "ADMIN") {
                                    Button(
                                        onClick = { viewModel.adminBootstrapCatalog() },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("bootstrap_catalog_button")
                                    ) {
                                        Text("Initialize Live Catalog & Partner Barber Shops")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Recommended Nearest Haircut & Perfectionists + Verified Skills
            if (rankedSpecialists.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column {
                        SectionHeader(
                            eyebrow = "NEAREST MASTER CRAFTSMEN",
                            title = "Recommended Haircut & Perfectionists",
                            subtitle = "Ranked by distance from your door • Verified fade, scissor & beard skills"
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.testTag("recommended_specialists_row")
                        ) {
                            items(rankedSpecialists, key = { it.first.barberId }) { (barber, distKm) ->
                                NearestPerfectionBarberCard(
                                    barber = barber,
                                    distanceKm = distKm,
                                    isSelected = selectedBarberId == barber.barberId,
                                    onSelectSpecialist = {
                                        viewModel.selectBarber(barber.barberId)
                                        if (selectedServiceIds.isEmpty() && defaultHaircutId != null) {
                                            viewModel.ensureServiceSelected(defaultHaircutId)
                                        }
                                        isCheckoutOpen = true
                                    }
                                )
                            }
                        }
                    }
                }

                // 5. Nearest Partner Barber Shops (Home Visit + Walk-In Studio)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SectionHeader(
                        eyebrow = "NEIGHBORHOOD PARTNER STUDIOS",
                        title = "Nearest Barber Shops (Home & Salon)",
                        subtitle = "Verified local shops that dispatch barbers to homes or welcome walk-ins"
                    )
                }

                items(rankedSpecialists, key = { "shop_${it.first.barberId}" }) { (barber, distKm) ->
                    NearestBarberShopCard(
                        barber = barber,
                        distanceKm = distKm,
                        localityHint = localityHint,
                        onBookHomeVisit = {
                            viewModel.selectBarber(barber.barberId)
                            if (selectedServiceIds.isEmpty() && defaultHaircutId != null) {
                                viewModel.ensureServiceSelected(defaultHaircutId)
                            }
                            isCheckoutOpen = true
                        },
                        onOpenShopDirections = {
                            val query = Uri.encode("${barber.effectiveShopName()}, ${barber.effectiveShopAddress(localityHint)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
                            try {
                                context.startActivity(mapIntent)
                            } catch (_: Exception) {
                            }
                        }
                    )
                }
            }
        }

        // Animated Sticky Bottom Zero-Fee Checkout Dock
        ServiceSelectionStickyBar(
            selectedServices = selectedServices,
            ctaLabel = "Choose Slot",
            onProceed = { isCheckoutOpen = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NearestPerfectionBarberCard(
    barber: BarberProfile,
    distanceKm: Double,
    isSelected: Boolean,
    onSelectSpecialist: () -> Unit
) {
    val skills = barber.effectiveSkills()
    val perfection = barber.effectivePerfectionBadge()

    Card(
        onClick = onSelectSpecialist,
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(302.dp)
            .testTag("specialist_card_${barber.barberId}")
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
                Surface(
                    color = EspressoHeaderBg,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "✦ $perfection",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFDF9E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    color = SageVerified.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = LocationHelper.formatDistanceKm(distanceKm),
                        style = MaterialTheme.typography.labelSmall,
                        color = SageVerified,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                BarberAvatarBadge(
                    name = barber.name,
                    isOnline = barber.isOnline,
                    size = 50.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = barber.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = barber.effectiveShopName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = BrassGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = " ${String.format("%.1f", barber.ratingAvg)} (${barber.reviewCount}) • ${barber.experienceYears} yrs exp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "VERIFIED GROOMING SKILLS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    skills.take(4).forEachIndexed { idx, skill ->
                        SkillPill(skill = skill, highlighted = idx == 0)
                    }
                }
            }

            Button(
                onClick = onSelectSpecialist,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) SageVerified else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isSelected) "Specialist Selected ✓" else "Book Home Haircut",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NearestBarberShopCard(
    barber: BarberProfile,
    distanceKm: Double,
    localityHint: String,
    onBookHomeVisit: () -> Unit,
    onOpenShopDirections: () -> Unit
) {
    val shopName = barber.effectiveShopName()
    val shopAddress = barber.effectiveShopAddress(localityHint)
    val skills = barber.effectiveSkills()

    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("barber_shop_card_${barber.barberId}")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(EspressoHeaderBg, Color(0xFF2D231A))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrassGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = BrassGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = shopName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${LocationHelper.formatDistanceKm(distanceKm)} away • Home Dispatch & Walk-In",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFDF9E)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = BrassGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = " ${String.format("%.1f", barber.ratingAvg)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shopAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BarberAvatarBadge(name = barber.name, isOnline = barber.isOnline, size = 34.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Lead Master: ${barber.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = barber.effectivePerfectionBadge(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    skills.take(3).forEach { skill ->
                        SkillPill(skill = skill)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onBookHomeVisit,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call to Home", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenShopDirections,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Visit Shop")
                    }
                }
            }
        }
    }
}
