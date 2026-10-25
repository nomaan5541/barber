package com.example.ui.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.ServiceCategory
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.serviceVisualFor
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.util.DetectedDoorLocation
import com.example.util.LocationHelper
import com.example.util.PendingDoorAddressDraft

/**
 * Structured Service Grid placeholder items matching the canonical grooming catalog IDs.
 * Displayed on the Service Selection landing screen prior to authentication or while catalog syncs,
 * ensuring immediate visual clarity for Haircutting, Beard Sculpting, Hair Care, Facial, and Home Combos.
 */
fun defaultServiceGridPlaceholders(): List<CatalogService> = listOf(
    CatalogService(
        id = "svc_classic_cut",
        name = "Classic Scissor & Clipper Haircut",
        category = "HAIRCUTS",
        description = "Tailored scissor & clipper haircut with neckline taper and clean home cape setup.",
        pricePaise = 30000L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        sortOrder = 1L
    ),
    CatalogService(
        id = "svc_skin_fade",
        name = "Zero / Mid / High Skin Fade",
        category = "HAIRCUTS",
        description = "Seamless razor-sharp skin fade with crisp hairline detailing by fade specialists.",
        pricePaise = 35000L,
        durationMinutes = 40L,
        isActive = true,
        isPackage = false,
        sortOrder = 2L
    ),
    CatalogService(
        id = "svc_beard_trim",
        name = "Beard Sculpting & Razor Line-Up",
        category = "BEARD_MOUSTACHE",
        description = "Symmetrical beard shaping, sharp cheek line-up, and neckline finish.",
        pricePaise = 15000L,
        durationMinutes = 20L,
        isActive = true,
        isPackage = false,
        sortOrder = 3L
    ),
    CatalogService(
        id = "svc_clean_shave",
        name = "Hot Towel Luxury Clean Shave",
        category = "BEARD_MOUSTACHE",
        description = "Rich lather straight-razor shave with soothing post-shave balm.",
        pricePaise = 18000L,
        durationMinutes = 25L,
        isActive = true,
        isPackage = false,
        sortOrder = 4L
    ),
    CatalogService(
        id = "svc_hair_wash",
        name = "Refreshing Hair Wash & Blow Dry",
        category = "HAIR_CARE",
        description = "Deep cleansing shampoo rinse and salon-styled blow dry finish.",
        pricePaise = 10000L,
        durationMinutes = 15L,
        isActive = true,
        isPackage = false,
        sortOrder = 5L
    ),
    CatalogService(
        id = "svc_scalp_oil",
        name = "Ayurvedic Head & Scalp Massage",
        category = "HAIR_CARE",
        description = "20-minute stress-relief warm oil massage for scalp rejuvenation.",
        pricePaise = 20000L,
        durationMinutes = 20L,
        isActive = true,
        isPackage = false,
        sortOrder = 6L
    ),
    CatalogService(
        id = "svc_face_cleanup",
        name = "Charcoal Detan Face Cleanup",
        category = "ADD_ONS",
        description = "Deep pore cleanse, exfoliating scrub, and hydrating charcoal mask.",
        pricePaise = 35000L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        sortOrder = 7L
    ),
    CatalogService(
        id = "svc_pkg_essential",
        name = "Haircut + Beard + Wash Combo",
        category = "PACKAGES",
        description = "Signature at-home package: Precision Haircut, Beard Sculpt & Refreshing Wash.",
        pricePaise = 55000L,
        durationMinutes = 60L,
        isActive = true,
        isPackage = true,
        sortOrder = 8L
    )
)

/**
 * Landing page for DoorCut (`AppRoutes.ROUTE_SERVICE_SELECTION`) when unauthenticated.
 * Displays live Google Location Services coordinates, Door Number setup link, category filters,
 * and the 2-column Service Selection Grid with interactive service cards & placeholders.
 */
@Composable
fun ServiceSelectionLandingScreen(
    detectedLocation: DetectedDoorLocation?,
    isFetchingLocation: Boolean,
    pendingDoorDraft: PendingDoorAddressDraft?,
    selectedServiceIds: Set<String>,
    onToggleService: (String) -> Unit,
    onOpenDoorSetup: () -> Unit,
    onRefreshGps: () -> Unit,
    onProceedToSignIn: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val placeholderServices = remember { defaultServiceGridPlaceholders() }
    val filteredServices = remember(placeholderServices, searchQuery, selectedCategory) {
        placeholderServices.filter { svc ->
            val matchesQuery = searchQuery.isBlank() ||
                svc.name.contains(searchQuery, ignoreCase = true) ||
                svc.description.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || svc.category == selectedCategory
            matchesQuery && matchesCat
        }
    }
    val selectedServices = remember(placeholderServices, selectedServiceIds) {
        placeholderServices.filter { selectedServiceIds.contains(it.id) }
    }

    val draftAsAddress = remember(pendingDoorDraft) {
        pendingDoorDraft?.let {
            CustomerAddress(
                id = "draft",
                label = it.label,
                flatHouse = it.combinedFlatHouse(),
                streetArea = it.streetArea,
                postalCode = it.postalCode,
                latitude = it.latitude,
                longitude = it.longitude
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("service_selection_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live GPS Coordinates & House/Door Setup Header
            item {
                DoorstepLocationHeaderCard(
                    activeAddress = draftAsAddress,
                    detectedLocation = detectedLocation,
                    isFetchingLocation = isFetchingLocation,
                    onOpenDoorSetup = onOpenDoorSetup,
                    onRefreshGps = onRefreshGps
                )
            }

            // 2. Search & Category Filter Bar
            item {
                ServiceCategoryFilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it }
                )
            }

            // 3. Service Selection Section Header + 2-Column Adaptive Service Grid (Above the Fold)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        eyebrow = stringResource(R.string.service_selection_eyebrow),
                        title = stringResource(R.string.service_selection_title),
                        subtitle = stringResource(R.string.service_selection_subtitle),
                        trailing = {
                            if (selectedServices.isNotEmpty()) {
                                StatusBadge("${selectedServices.size} SELECTED")
                            } else {
                                StatusBadge(stringResource(R.string.service_grid_placeholder_badge))
                            }
                        }
                    )

                    if (filteredServices.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Filled.ContentCut,
                            title = "No Matching Services",
                            subtitle = "Clear your search or choose another grooming category above."
                        )
                    } else {
                        ServiceSelectionGrid(
                            services = filteredServices,
                            selectedServiceIds = selectedServiceIds,
                            isPlaceholderPreview = true,
                            onToggleService = onToggleService
                        )
                    }
                }
            }

            // 4. Interactive Google Sign-In Card (Preserves Zero-Trust Auth Invariant)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.service_landing_signin_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.service_landing_signin_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
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
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EspressoHeaderBg,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("landing_google_sign_in_button")
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = BrassGold,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.btn_sign_in_short),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (signInError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = signInError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Sticky Bottom Selection Summary Bar -> Navigates to Sign In / Slot Checkout
        ServiceSelectionStickyBar(
            selectedServices = selectedServices,
            ctaLabel = "Sign In & Choose Slot",
            onProceed = onProceedToSignIn,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun DoorstepLocationHeaderCard(
    activeAddress: CustomerAddress?,
    detectedLocation: DetectedDoorLocation?,
    isFetchingLocation: Boolean,
    onOpenDoorSetup: () -> Unit,
    onRefreshGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = EspressoHeaderBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(EspressoHeaderBg, Color(0xFF2B221A))
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SageVerified,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "₹0 CUSTOMER PLATFORM FEE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = "Pay Barber Only",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFFDF9E)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onOpenDoorSetup,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_address_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BrassGold.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Home Door Location",
                                tint = BrassGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    activeAddress != null -> "${activeAddress.label} DOOR • ${activeAddress.flatHouse}"
                                    isFetchingLocation -> "Detecting GPS door coordinates..."
                                    detectedLocation != null -> "${detectedLocation.localityName} • Enter Door No."
                                    else -> "Set House / Door Number"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    activeAddress != null && activeAddress.latitude != 0.0 ->
                                        "${activeAddress.streetArea} • ${LocationHelper.formatCoord(activeAddress.latitude)}, ${LocationHelper.formatCoord(activeAddress.longitude)}"
                                    activeAddress != null -> "${activeAddress.streetArea} (${activeAddress.postalCode})"
                                    detectedLocation != null -> "${detectedLocation.formattedCoordinates()} • ${detectedLocation.streetArea}"
                                    else -> "Tap to detect GPS coordinates & enter house/door number"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6C8B6),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeAddress == null) "Set Door" else "Change",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFDF9E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRefreshGps,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrassGold)
                        .testTag("gps_refresh_button")
                ) {
                    if (isFetchingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = EspressoHeaderBg,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "Auto-detect GPS Door Location",
                            tint = EspressoHeaderBg
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCategoryFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search skin fade, haircut, beard sculpt, facial...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search services",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = if (searchQuery.isNotBlank()) {
                {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("service_search_input")
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                ServiceCategoryChip(
                    title = "All Services",
                    icon = Icons.Filled.AutoAwesome,
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) }
                )
            }
            items(ServiceCategory.entries) { cat ->
                ServiceCategoryChip(
                    title = cat.displayTitle,
                    icon = categoryIcon(cat),
                    selected = selectedCategory == cat.name,
                    onClick = {
                        onSelectCategory(if (selectedCategory == cat.name) null else cat.name)
                    }
                )
            }
        }
    }
}

private fun categoryIcon(category: ServiceCategory): ImageVector = when (category) {
    ServiceCategory.HAIRCUTS -> Icons.Filled.ContentCut
    ServiceCategory.BEARD_MOUSTACHE -> Icons.Filled.WorkspacePremium
    ServiceCategory.HAIR_CARE -> Icons.Filled.Spa
    ServiceCategory.ADD_ONS -> Icons.Filled.Face
    ServiceCategory.PACKAGES -> Icons.Filled.AutoAwesome
}

@Composable
private fun ServiceCategoryChip(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) EspressoHeaderBg else MaterialTheme.colorScheme.surface,
        label = "categoryChipBg"
    )
    val contentCol by animateColorAsState(
        targetValue = if (selected) Color(0xFFFFDF9E) else MaterialTheme.colorScheme.onSurface,
        label = "categoryChipFg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) BrassGold.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) BrassGold else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentCol
            )
        }
    }
}

/**
 * Responsive 2-column / 3-column Service Grid for the Service Selection screen.
 */
@Composable
fun ServiceSelectionGrid(
    services: List<CatalogService>,
    selectedServiceIds: Set<String>,
    isPlaceholderPreview: Boolean = false,
    onToggleService: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_selection_grid")
    ) {
        val columns = if (maxWidth >= 700.dp) 3 else 2
        val rows = remember(services, columns) { services.chunked(columns) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { service ->
                        ServiceGridCard(
                            service = service,
                            isSelected = selectedServiceIds.contains(service.id),
                            isPlaceholderPreview = isPlaceholderPreview,
                            onToggle = { onToggleService(service.id) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Structured loading placeholder grid shown while Firestore catalog services are syncing.
 */
@Composable
fun ServiceGridLoadingPlaceholders(
    count: Int = 6,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "gridShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "placeholderAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_selection_grid")
    ) {
        val columns = if (maxWidth >= 700.dp) 3 else 2
        val indices = remember(count, columns) { (0 until count).chunked(columns) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            indices.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { index ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("service_grid_placeholder_$index")
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(126.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCut,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .height(16.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.55f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = alpha))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha))
                                    )
                                }
                            }
                        }
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceGridCard(
    service: CatalogService,
    isSelected: Boolean,
    isPlaceholderPreview: Boolean = false,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = remember(service.id, service.category, service.name, service.isPackage) {
        serviceVisualFor(service)
    }
    val actionBg by animateColorAsState(
        targetValue = if (isSelected) SageVerified else EspressoHeaderBg,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "gridCardBtnBg"
    )

    val shortBadge = remember(service.category, service.isPackage) {
        when {
            service.isPackage || service.category == "PACKAGES" -> "COMBO"
            service.category == "HAIRCUTS" -> "HAIRCUT"
            service.category == "BEARD_MOUSTACHE" -> "BEARD"
            service.category == "HAIR_CARE" -> "HAIR CARE"
            else -> "FACIAL"
        }
    }

    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) {
            BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.testTag("service_card_${service.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Visual Photography Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                ) {
                    Image(
                        painter = painterResource(id = visual.drawableRes),
                        contentDescription = "${service.name} — ${visual.visualCaption}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.38f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )

                    // Category Badge (Top-Start)
                    Surface(
                        color = EspressoHeaderBg.copy(alpha = 0.86f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = shortBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFDF9E),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Duration Pill (Top-End)
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${service.durationMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    // Bottom Price + Zero Fee over Scrim
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = CurrencyUtils.formatRupees(service.pricePaise),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            color = SageVerified.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (isPlaceholderPreview) "₹0 Fee" else "₹0 Fee",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Title & Concise Craft Description
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = service.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Accessible ≥48dp Action Button inside Grid Card
            Button(
                onClick = onToggle,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionBg,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                    .height(48.dp)
                    .testTag("service_grid_item_${service.id}")
            ) {
                if (isSelected) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Selected", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ServiceSelectionStickyBar(
    selectedServices: List<CatalogService>,
    ctaLabel: String,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedServices.isNotEmpty(),
        enter = slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        val subtotal = selectedServices.sumOf { it.pricePaise }
        val totalMins = selectedServices.sumOf { it.durationMinutes }
        Surface(
            color = EspressoHeaderBg,
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, BrassGold.copy(alpha = 0.65f)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable(onClick = onProceed)
                .testTag("proceed_checkout_bar")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrassGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${selectedServices.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EspressoHeaderBg
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${CurrencyUtils.formatRupees(subtotal)} • $totalMins min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "₹0 Platform Fee • Pay Barber Directly",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8BE8C0)
                        )
                    }
                }

                Surface(
                    color = BrassGold,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ctaLabel,
                            style = MaterialTheme.typography.labelLarge,
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
}

/**
 * Auth-gated prompt card displayed on protected tabs (e.g. Bookings) when user is signed out.
 */
@Composable
fun ProtectedRouteAuthGate(
    title: String,
    subtitle: String,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EspressoHeaderBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.EventNote,
                        contentDescription = null,
                        tint = BrassGold,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        isLoading = true
                        errorMsg = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { err ->
                                isLoading = false
                                errorMsg = err
                            },
                            scope = scope,
                            onAuthCancelled = { isLoading = false }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EspressoHeaderBg,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("bookings_google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrassGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.btn_sign_in_google),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
