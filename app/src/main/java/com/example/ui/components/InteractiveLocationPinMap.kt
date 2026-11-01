package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrassGold
import com.example.ui.theme.SageVerified
import com.example.ui.theme.SageVerifiedDark
import com.example.util.DetectedDoorLocation
import com.example.util.LocationHelper
import com.example.util.OsrmRoadRouter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Reusable interactive pin-on-map + real Android Geocoder forward/reverse search component.
 * Solves indoor weak-GPS issues, eliminates 0.0/0.0 coordinates, supports drag-to-pin street gates,
 * precision 25m directional nudges, system GPS settings prompt, and optional Barber coverage ring.
 */
@Composable
fun InteractiveLocationPinMap(
    currentLocation: DetectedDoorLocation?,
    isLoadingHardwareGps: Boolean,
    onRequestHardwareGps: () -> Unit,
    onLocationPinned: (DetectedDoorLocation) -> Unit,
    modifier: Modifier = Modifier,
    pinTitle: String = "Pin Exact Society / Building Gate",
    coverageRadiusKm: Double? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val defaultSeed = remember { LocationHelper.defaultFeaturedLocations().first() }
    val seedLocation = if (currentLocation != null && currentLocation.hasValidCoordinates) {
        currentLocation
    } else {
        defaultSeed
    }

    var activeLat by remember { mutableDoubleStateOf(seedLocation.latitude) }
    var activeLon by remember { mutableDoubleStateOf(seedLocation.longitude) }
    var resolvedLocation by remember { mutableStateOf(seedLocation) }
    var panVisualOffsetX by remember { mutableFloatStateOf(0f) }
    var panVisualOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingPin by remember { mutableStateOf(false) }
    var isReverseGeocoding by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchingAddress by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<DetectedDoorLocation>>(emptyList()) }
    var showSearchDropdown by remember { mutableStateOf(false) }

    val featuredSpots = remember { LocationHelper.defaultFeaturedLocations() }
    var reverseGeocodeJob by remember { mutableStateOf<Job?>(null) }
    var osmPinTileBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Sync external hardware GPS lock updates into the interactive map canvas
    LaunchedEffect(currentLocation?.latitude, currentLocation?.longitude, currentLocation?.streetArea) {
        if (currentLocation != null && currentLocation.hasValidCoordinates && !isDraggingPin) {
            activeLat = currentLocation.latitude
            activeLon = currentLocation.longitude
            resolvedLocation = currentLocation
        }
    }

    // Fetch OpenStreetMap slippy tile (zoom 16) when pin settles
    LaunchedEffect(activeLat, activeLon, isDraggingPin) {
        if (!isDraggingPin && activeLat != 0.0 && activeLon != 0.0) {
            val tileCoord = OsrmRoadRouter.latLonToOsmTile(activeLat, activeLon, zoom = 16)
            osmPinTileBitmap = OsrmRoadRouter.fetchOsmTileBitmap(tileCoord.zoom, tileCoord.x, tileCoord.y)
        }
    }

    // Debounced forward geocoding search when the user types a street, area, PIN code, or "lat, lon"
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.length < 2) {
            searchResults = emptyList()
            isSearchingAddress = false
            showSearchDropdown = false
        } else {
            isSearchingAddress = true
            delay(240L)
            val matches = LocationHelper.searchLocationsByQuery(context, trimmed, maxResults = 5)
            searchResults = matches
            isSearchingAddress = false
            showSearchDropdown = matches.isNotEmpty()
        }
    }

    fun commitPinnedCoordinates(newLat: Double, newLon: Double, sourceLabel: String) {
        activeLat = newLat
        activeLon = newLon
        val immediateUpdate = resolvedLocation.copy(
            latitude = newLat,
            longitude = newLon,
            accuracyMeters = 4f,
            providerLabel = sourceLabel
        )
        resolvedLocation = immediateUpdate
        onLocationPinned(immediateUpdate)

        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = scope.launch {
            isReverseGeocoding = true
            val geocoded = LocationHelper.reverseGeocodeCoordinates(
                context = context,
                latitude = newLat,
                longitude = newLon,
                accuracyMeters = 4f,
                providerLabel = sourceLabel
            )
            resolvedLocation = geocoded
            isReverseGeocoding = false
            onLocationPinned(geocoded)
        }
    }

    fun nudgeByMeters(northMeters: Double, eastMeters: Double) {
        val (newLat, newLon) = LocationHelper.offsetCoordinatesByMeters(
            latitude = activeLat,
            longitude = activeLon,
            northMeters = northMeters,
            eastMeters = eastMeters
        )
        panVisualOffsetX -= (eastMeters * 0.65).toFloat()
        panVisualOffsetY += (northMeters * 0.65).toFloat()
        commitPinnedCoordinates(newLat, newLon, "Precision Gate Pin")
    }

    val isSystemGpsEnabled = remember(currentLocation, isLoadingHardwareGps) {
        LocationHelper.isLocationEnabledOnDevice(context)
    }

    val pinLiftPx by animateFloatAsState(
        targetValue = if (isDraggingPin) with(density) { -12.dp.toPx() } else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pinLiftAnimation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_location_pin_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row + Live GPS status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = pinTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Search street/PIN or drag map under the pin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (currentLocation?.hasValidCoordinates == true) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = if (isReverseGeocoding) {
                            "Resolving..."
                        } else {
                            resolvedLocation.formattedAccuracy()
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (currentLocation?.hasValidCoordinates == true) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // System Location Services Disabled Banner
            AnimatedVisibility(visible = !isSystemGpsEnabled) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Phone GPS is turned off. Turn on Location or search/drag pin.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { LocationHelper.openDeviceLocationSettings(context) },
                            modifier = Modifier.testTag("turn_on_system_gps_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Turn On", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Live Forward Geocoder Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search street, apartment, landmark or PIN") },
                placeholder = { Text("e.g. Banjara Hills, 500034, Indiranagar...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (isSearchingAddress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            showSearchDropdown = false
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear location search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("location_pin_search_input")
            )

            // Search Suggestions Dropdown
            AnimatedVisibility(visible = showSearchDropdown && searchResults.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        searchResults.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeLat = item.latitude
                                        activeLon = item.longitude
                                        resolvedLocation = item
                                        searchQuery = ""
                                        showSearchDropdown = false
                                        onLocationPinned(item)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                                    .testTag("location_search_result_$index"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.localityName.ifBlank { item.streetArea },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.streetArea} · PIN ${item.postalCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (index < searchResults.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // Quick Neighbourhood Jump Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                featuredSpots.forEachIndexed { idx, spot ->
                    AssistChip(
                        onClick = {
                            activeLat = spot.latitude
                            activeLon = spot.longitude
                            resolvedLocation = spot
                            onLocationPinned(spot)
                        },
                        label = {
                            Text(
                                text = spot.localityName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("quick_area_pin_$idx")
                    )
                }
            }

            // Interactive Draggable Street Map Canvas with Center Target Pin
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF141B22))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    .pointerInput(Unit) {
                        var dragLat = activeLat
                        var dragLon = activeLon
                        detectDragGestures(
                            onDragStart = {
                                isDraggingPin = true
                                dragLat = activeLat
                                dragLon = activeLon
                            },
                            onDragEnd = {
                                isDraggingPin = false
                                commitPinnedCoordinates(dragLat, dragLon, "Interactive Map Pin")
                            },
                            onDragCancel = {
                                isDraggingPin = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                panVisualOffsetX += dragAmount.x
                                panVisualOffsetY += dragAmount.y
                                // Web Mercator slippy-map drag projection at zoom 16
                                val (dLat, dLon) = OsrmRoadRouter.pixelDeltaToLatLonDelta(
                                    centerLat = dragLat,
                                    zoom = 16,
                                    dxPx = dragAmount.x,
                                    dyPx = dragAmount.y
                                )
                                val nextLat = (dragLat + dLat).coerceIn(-85.0, 85.0)
                                val nextLon = (dragLon + dLon).coerceIn(-180.0, 180.0)
                                dragLat = nextLat
                                dragLon = nextLon
                                activeLat = nextLat
                                activeLon = nextLon
                            }
                        )
                    }
                    .testTag("interactive_pin_map_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w * 0.5f
                    val cy = h * 0.5f

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF17212B), Color(0xFF0E151C))
                        )
                    )

                    val loadedTile = osmPinTileBitmap
                    if (loadedTile != null) {
                        val tileSide = max(w, h).roundToInt().coerceAtLeast(256)
                        drawImage(
                            image = loadedTile,
                            dstOffset = IntOffset(
                                x = (cx - tileSide * 0.5f + (panVisualOffsetX % 64f)).roundToInt(),
                                y = (cy - tileSide * 0.5f + (panVisualOffsetY % 64f)).roundToInt()
                            ),
                            dstSize = IntSize(tileSide, tileSide),
                            alpha = 0.86f
                        )
                    }

                    val gridSpacing = 44.dp.toPx()
                    val modX = ((panVisualOffsetX % gridSpacing) + gridSpacing) % gridSpacing
                    val modY = ((panVisualOffsetY % gridSpacing) + gridSpacing) % gridSpacing

                    // Park & urban block footprints
                    var bx = modX - gridSpacing
                    var colIdx = 0
                    while (bx < w + gridSpacing) {
                        var by = modY - gridSpacing
                        var rowIdx = 0
                        while (by < h + gridSpacing) {
                            val isPark = (colIdx + rowIdx) % 5 == 0
                            drawRoundRect(
                                color = if (isPark) {
                                    SageVerified.copy(alpha = 0.16f)
                                } else {
                                    Color.White.copy(alpha = 0.045f)
                                },
                                topLeft = Offset(bx + 6f, by + 6f),
                                size = Size(gridSpacing - 12f, gridSpacing - 12f),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            by += gridSpacing
                            rowIdx++
                        }
                        bx += gridSpacing
                        colIdx++
                    }

                    // Minor street grid lines
                    var gx = modX
                    while (gx < w) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 2f
                        )
                        gx += gridSpacing
                    }
                    var gy = modY
                    while (gy < h) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 2f
                        )
                        gy += gridSpacing
                    }

                    // Arterial avenues shifting smoothly with pan offset
                    val artX = ((cx + panVisualOffsetX * 0.5f) % w + w) % w
                    val artY = ((cy + panVisualOffsetY * 0.5f) % h + h) % h
                    drawLine(
                        color = BrassGold.copy(alpha = 0.24f),
                        start = Offset(artX, 0f),
                        end = Offset(artX, h),
                        strokeWidth = 7f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.16f),
                        start = Offset(0f, artY),
                        end = Offset(w, artY),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )

                    // Optional Barber Partner Service Coverage Radius Ring
                    if (coverageRadiusKm != null && coverageRadiusKm > 0.0) {
                        val maxRadiusPx = min(w, h) * 0.42f
                        val scaledRadius = ((coverageRadiusKm.toFloat() / 15f).coerceIn(0.28f, 1f)) * maxRadiusPx
                        drawCircle(
                            color = SageVerifiedDark.copy(alpha = 0.14f),
                            radius = scaledRadius,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = SageVerifiedDark.copy(alpha = 0.65f),
                            radius = scaledRadius,
                            center = Offset(cx, cy),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        )
                    }

                    // Ground crosshair & pin shadow under the exact target coordinate
                    drawCircle(
                        color = Color.Black.copy(alpha = if (isDraggingPin) 0.25f else 0.45f),
                        radius = if (isDraggingPin) 11.dp.toPx() else 7.dp.toPx(),
                        center = Offset(cx, cy + 4.dp.toPx())
                    )
                    drawCircle(
                        color = BrassGold,
                        radius = 3.5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }

                // Floating Center Target Pin (lifts with spring physics while dragging)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(0, (pinLiftPx - 16.dp.toPx()).roundToInt()) }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BrassGold,
                        shadowElevation = if (isDraggingPin) 10.dp else 4.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = "Pinned Gate Location",
                                tint = Color(0xFF1E1608),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Top-left Live Lat/Lon Telemetry Pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xCC0D131A),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.5f, %.5f", activeLat, activeLon),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE8EEF5),
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("pin_map_coordinates_badge")
                    )
                }

                // Bottom-left Drag Hint / Coverage Pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xCC0D131A),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (coverageRadiusKm != null) {
                            "Coverage Radar: ${coverageRadiusKm.roundToInt()} km radius"
                        } else if (isDraggingPin) {
                            "Release to lock exact gate..."
                        } else {
                            "Drag map to place pin on gate"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = BrassGold,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Bottom-right OpenStreetMap Attribution Pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xB30D131A),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "© OpenStreetMap",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD5DEE8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Use Current GPS + Precision 25m Gate Micro-Nudge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRequestHardwareGps,
                    enabled = !isLoadingHardwareGps,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("pin_map_use_gps_button")
                ) {
                    if (isLoadingHardwareGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (currentLocation?.hasValidCoordinates == true) {
                                Icons.Filled.GpsFixed
                            } else {
                                Icons.Filled.LocationSearching
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isLoadingHardwareGps) "Locking..." else "Use Live GPS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 25m N / W / E / S fine-tune buttons (48dp touch targets for accessibility)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { nudgeByMeters(northMeters = 25.0, eastMeters = 0.0) },
                        modifier = Modifier.testTag("nudge_pin_north")
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move pin 25 meters North")
                    }
                    IconButton(
                        onClick = { nudgeByMeters(northMeters = 0.0, eastMeters = -25.0) },
                        modifier = Modifier.testTag("nudge_pin_west")
                    ) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Move pin 25 meters West")
                    }
                    IconButton(
                        onClick = { nudgeByMeters(northMeters = 0.0, eastMeters = 25.0) },
                        modifier = Modifier.testTag("nudge_pin_east")
                    ) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Move pin 25 meters East")
                    }
                    IconButton(
                        onClick = { nudgeByMeters(northMeters = -25.0, eastMeters = 0.0) },
                        modifier = Modifier.testTag("nudge_pin_south")
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move pin 25 meters South")
                    }
                }
            }

            // Resolved Reverse-Geocoded Street Summary Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = resolvedLocation.streetArea,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("pin_map_resolved_street")
                        )
                        Text(
                            text = "${resolvedLocation.localityName} · PIN ${resolvedLocation.postalCode} · ${resolvedLocation.providerLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
