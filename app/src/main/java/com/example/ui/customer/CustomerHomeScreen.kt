package com.example.ui.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.ServiceCategory
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.serviceVisualFor
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.glass.LiveGlassPulseDot
import com.example.ui.motion.AnimatedSummaryText
import com.example.ui.motion.DoorCutMotionSpecs
import com.example.ui.motion.LocalReducedMotion
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.util.DetectedDoorLocation
import com.example.util.DoorCutPreferences
import com.example.util.PendingDoorAddressDraft

@Composable
fun CustomerHomeScreen(
    services: List<CatalogService>,
    selectedServiceIds: Set<String>,
    upcomingBooking: Booking?,
    savedAddresses: List<CustomerAddress>,
    detectedLocation: DetectedDoorLocation?,
    pendingDoorDraft: PendingDoorAddressDraft?,
    isFetchingLocation: Boolean,
    onToggleService: (String) -> Unit,
    onStartBookingFlow: () -> Unit,
    onOpenLocationSelector: () -> Unit,
    onRefreshGps: () -> Unit,
    onOpenBookingStatus: (String) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { DoorCutPreferences.ensureLoaded(context) }
    val localPrefs by DoorCutPreferences.state.collectAsStateWithLifecycle()

    var selectedCategory by rememberSaveable { mutableStateOf("POPULAR") }
    var activeDetailService by remember { mutableStateOf<CatalogService?>(null) }

    val defaultAddress = savedAddresses.firstOrNull { it.isDefault } ?: savedAddresses.firstOrNull()
    val locationLabel = remember(defaultAddress, pendingDoorDraft, detectedLocation) {
        when {
            defaultAddress != null -> {
                val cityOrStreet = defaultAddress.streetArea.substringAfterLast(",").trim()
                    .ifBlank { defaultAddress.streetArea.take(22) }
                "${defaultAddress.displayLabel()} · $cityOrStreet"
            }
            pendingDoorDraft != null -> {
                val labelText = when (pendingDoorDraft.label.uppercase()) {
                    "OFFICE" -> "Work"
                    "OTHER" -> "Other"
                    else -> "Home"
                }
                "$labelText · ${pendingDoorDraft.streetArea.take(22)}"
            }
            detectedLocation != null -> "Home · ${detectedLocation.localityName.ifBlank { "Hyderabad" }}"
            else -> "Home · Hyderabad"
        }
    }

    val popularIds = remember {
        setOf(
            "svc_classic_cut",
            "svc_beard_trim",
            "svc_pkg_essential",
            "svc_ladies_haircut",
            "svc_normal_head_cut",
            "svc_traditional_cut"
        )
    }
    val displayedServices = remember(services, selectedCategory) {
        when (selectedCategory) {
            "POPULAR" -> services.filter { popularIds.contains(it.id) }.ifEmpty { services.take(5) }
            "ALL" -> services
            else -> services.filter { it.category == selectedCategory }
        }
    }

    val selectedServices = remember(services, selectedServiceIds) {
        services.filter { selectedServiceIds.contains(it.id) }
    }
    val selectedSubtotalPaise = remember(selectedServices) {
        selectedServices.sumOf { it.pricePaise }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_home_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .testTag("service_selection_screen"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (selectedServices.isNotEmpty()) 104.dp else 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Location Selector ("Home · Hyderabad") with Living Liquid Glass
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 3.dp,
                    onClick = onOpenLocationSelector,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_address_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EspressoHeaderBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = BrassGold,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LiveGlassPulseDot(
                                        statusKey = locationLabel,
                                        color = SageVerified,
                                        size = 7.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = locationLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Change home address",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = defaultAddress?.flatHouse
                                        ?: pendingDoorDraft?.combinedFlatHouse()
                                        ?: "Tap to set flat or house number",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = onRefreshGps,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (isFetchingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = "Detect GPS location",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Sunlit Editorial Hero Card + Instant Open-Dispatch "Book a Barber" CTA
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.48f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.hero_home_grooming_1793031644119),
                                contentDescription = "At-home grooming stylist",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = EspressoHeaderBg.copy(alpha = 0.90f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCut,
                                        contentDescription = null,
                                        tint = BrassGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "INSTANT HOME DISPATCH · MEN & LADIES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFDF9E)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = DoorCutPreferences.localizedGreeting(localPrefs.selectedLanguage),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose your service — a verified nearby Barber or Lady Hair Stylist accepts & tracks live to your door.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onStartBookingFlow,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EspressoHeaderBg,
                                    contentColor = Color(0xFFFFDF9E)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .tactilePressPhysics(
                                        pressedScale = 0.965f,
                                        maxTiltDegrees = 2.0f,
                                        specularColor = Color.White,
                                        specularAlpha = 0.18f,
                                        enableHaptics = true
                                    )
                                    .testTag("home_book_barber_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCut,
                                    contentDescription = null,
                                    tint = BrassGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_book_a_barber),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = BrassGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. "Your next appointment" Card (if booked)
            if (upcomingBooking != null) {
                item {
                    LiquidGlassSurface(
                        strength = GlassMaterialStrength.AccentEmerald,
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 3.dp,
                        onClick = { onOpenBookingStatus(upcomingBooking.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_next_appointment_card")
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LiveGlassPulseDot(
                                        statusKey = upcomingBooking.status,
                                        color = SageVerified,
                                        size = 8.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.home_next_appointment_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SageVerified
                                    )
                                }
                                StatusBadge(upcomingBooking.friendlyStatusText())
                            }
                            val displayStylistTitle = if (upcomingBooking.isOpenDispatch) {
                                if (upcomingBooking.preferLadyBarber) {
                                    "Matching Nearby Lady Hair Stylist..."
                                } else {
                                    "Matching Nearby Verified Barber..."
                                }
                            } else {
                                upcomingBooking.barberName
                            }
                            val scheduleText = if (upcomingBooking.bookingMode == "ASAP") {
                                "ASAP Express Dispatch · ${upcomingBooking.dateKey}"
                            } else {
                                "${upcomingBooking.dateKey} · ${CurrencyUtils.formatHourSlot(upcomingBooking.startHour)}"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BarberAvatarBadge(
                                    name = if (upcomingBooking.isOpenDispatch) "DoorCut" else upcomingBooking.barberName,
                                    isOnline = true,
                                    size = 46.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayStylistTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = scheduleText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = upcomingBooking.serviceNamesSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "Live Map →",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Categories Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.home_categories_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == "POPULAR",
                                onClick = { selectedCategory = "POPULAR" },
                                label = { Text("Popular") },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategory == "ALL",
                                onClick = { selectedCategory = "ALL" },
                                label = { Text("All Services") },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                        items(ServiceCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat.name,
                                onClick = { selectedCategory = cat.name },
                                label = { Text(cat.displayTitle) },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                }
            }

            // 5. Popular Services / Category Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_selection_grid"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory == "POPULAR") {
                            stringResource(R.string.home_popular_services_title)
                        } else {
                            ServiceCategory.entries.firstOrNull { it.name == selectedCategory }?.displayTitle ?: "All Services"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pay barber after cut",
                        style = MaterialTheme.typography.labelMedium,
                        color = SageVerified,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            itemsIndexed(displayedServices, key = { _, it -> it.id }) { index, service ->
                SimpleServiceBookingCard(
                    service = service,
                    isSelected = selectedServiceIds.contains(service.id),
                    isFavourite = localPrefs.favouriteServiceIds.contains(service.id),
                    staggerIndex = index,
                    onToggleSelect = { onToggleService(service.id) },
                    onToggleFavourite = { DoorCutPreferences.toggleFavouriteService(context, service.id) },
                    onOpenDetails = { activeDetailService = service }
                )
            }

            // 6. Special Offers
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.home_special_offers_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LiquidGlassSurface(
                        strength = GlassMaterialStrength.AtelierCard,
                        shape = RoundedCornerShape(22.dp),
                        accentBorderColor = BrassGold.copy(alpha = 0.58f),
                        shadowElevation = 3.dp,
                        onClick = {
                            if (!selectedServiceIds.contains("svc_pkg_essential")) {
                                onToggleService("svc_pkg_essential")
                            }
                            onStartBookingFlow()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(EspressoHeaderBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = BrassGold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Haircut + Beard Value Combo · ₹400",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Save ₹50 · 45 mins · Pay ₹400 directly to barber after service",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Book →",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Sticky Bottom Summary Bar: "2 services · ₹450 · Continue"
        val reducedMotion = LocalReducedMotion.current
        AnimatedVisibility(
            visible = selectedServices.isNotEmpty(),
            enter = slideInVertically(
                animationSpec = DoorCutMotionSpecs.surfaceSettleSpring(reducedMotion)
            ) { it } + fadeIn(tween(150)),
            exit = slideOutVertically { it } + fadeOut(tween(110)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            StickyServicesContinueBar(
                count = selectedServices.size,
                subtotalPaise = selectedSubtotalPaise,
                onContinue = onStartBookingFlow
            )
        }
    }

    activeDetailService?.let { svc ->
        ServiceDetailBottomSheet(
            service = svc,
            isSelected = selectedServiceIds.contains(svc.id),
            onToggleSelect = { onToggleService(svc.id) },
            onGoToCart = onStartBookingFlow,
            onDismiss = { activeDetailService = null }
        )
    }
}

@Composable
fun SimpleServiceBookingCard(
    service: CatalogService,
    isSelected: Boolean,
    isFavourite: Boolean,
    staggerIndex: Int = 0,
    onToggleSelect: () -> Unit,
    onToggleFavourite: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val visual = remember(service.id, service.category, service.name, service.isPackage) {
        serviceVisualFor(service)
    }
    val heartScale by animateFloatAsState(
        targetValue = if (isFavourite) 1.18f else 1.0f,
        animationSpec = DoorCutMotionSpecs.microBounceSpring(reducedMotion),
        label = "fav_heart_scale_${service.id}"
    )
    val addBtnContainer by animateColorAsState(
        targetValue = if (isSelected) SageVerified else EspressoHeaderBg,
        animationSpec = tween(170),
        label = "add_btn_bg_${service.id}"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                SageVerified.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) SageVerified else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .staggeredSpringEntrance(index = staggerIndex)
            .clickable(onClick = onOpenDetails)
            .testTag("service_card_${service.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Sunlit Image + Highlighted Title/Duration on Left, Bold Price + Favourite on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = painterResource(id = visual.drawableRes),
                    contentDescription = service.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = if (service.category == "LADIES_STYLING") {
                            Color(0xFF9C27B0).copy(alpha = 0.12f)
                        } else {
                            BrassGold.copy(alpha = 0.20f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = visual.badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (service.category == "LADIES_STYLING") {
                                Color(0xFF6A1B9A)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${service.durationMinutes} min · Pay barber after cut",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = BrassGold.copy(alpha = 0.24f),
                        border = BorderStroke(1.dp, BrassGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = CurrencyUtils.formatRupees(service.pricePaise),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleFavourite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.OutlinedFavoriteFallback(isFavourite),
                            contentDescription = if (isFavourite) "Remove from favourites" else "Save to favourites",
                            tint = if (isFavourite) Color(0xFFD9383A) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                            }
                        )
                    }
                }
            }

            // Full Visible Service Description (no dark overshading, no truncation squeeze)
            Text(
                text = service.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Bottom Full-Width Bold Add / Added Button
            Button(
                onClick = onToggleSelect,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = addBtnContainer,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .tactilePressPhysics(pressedScale = 0.96f, maxTiltDegrees = 0f, enableHaptics = true)
                    .testTag("add_service_button_${service.id}")
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSelected) {
                        "Added to Booking ✓  •  ${CurrencyUtils.formatRupees(service.pricePaise)}"
                    } else {
                        "Add Service  •  ${CurrencyUtils.formatRupees(service.pricePaise)}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun Icons.OutlinedFavoriteFallback(isFav: Boolean) =
    if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder

@Composable
fun StickyServicesContinueBar(
    count: Int,
    subtotalPaise: Long,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemWord = if (count == 1) "1 service" else "$count services"
    LiquidGlassSurface(
        strength = GlassMaterialStrength.FloatingPill,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 10.dp,
        onClick = onContinue,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("sticky_continue_booking_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedSummaryText(
                    text = "$itemWord · ${CurrencyUtils.formatRupees(subtotalPaise)}",
                    numericOrderKey = subtotalPaise,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pay ${CurrencyUtils.formatRupees(subtotalPaise)} directly to barber after service",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFDF9E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                color = BrassGold,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EspressoHeaderBg
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = EspressoHeaderBg,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
