package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Villa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.glass.AmbientAtelierBackdrop
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassPill
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.glass.LiveGlassPulseDot
import com.example.ui.motion.AnimatedSummaryText
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.components.InteractiveLocationPinMap
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.util.DetectedDoorLocation
import com.example.util.LocationHelper
import com.example.util.PendingDoorAddressDraft

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoorNumberSetupScreen(
    detectedLocation: DetectedDoorLocation?,
    isFetchingLocation: Boolean,
    hasLocationPermission: Boolean,
    locationErrorMessage: String? = null,
    initialDraft: PendingDoorAddressDraft? = null,
    isBusy: Boolean = false,
    allowDismiss: Boolean = true,
    primaryButtonLabel: String? = null,
    onRequestDetectCoordinates: () -> Unit,
    onLocationPinned: ((DetectedDoorLocation) -> Unit)? = null,
    onConfirmDoorAddress: (PendingDoorAddressDraft) -> Unit,
    onSkipOrClose: () -> Unit
) {
    if (allowDismiss) {
        BackHandler { onSkipOrClose() }
    }

    var label by remember(initialDraft?.label) {
        mutableStateOf(initialDraft?.label ?: "HOME")
    }
    var doorNumber by remember(initialDraft?.doorNumber) {
        mutableStateOf(initialDraft?.doorNumber ?: detectedLocation?.doorHint.orEmpty())
    }
    var buildingName by remember(initialDraft?.buildingName) {
        mutableStateOf(initialDraft?.buildingName.orEmpty())
    }
    var streetArea by remember(initialDraft?.streetArea) {
        mutableStateOf(initialDraft?.streetArea ?: detectedLocation?.streetArea.orEmpty())
    }
    var landmark by remember(initialDraft?.landmark) {
        mutableStateOf(initialDraft?.landmark ?: detectedLocation?.landmark.orEmpty())
    }
    var postalCode by remember(initialDraft?.postalCode) {
        mutableStateOf(
            initialDraft?.postalCode?.ifBlank { null }
                ?: detectedLocation?.postalCode?.ifBlank { null }
                ?: "560038"
        )
    }
    var accessInstructions by remember(initialDraft?.accessInstructions) {
        mutableStateOf(initialDraft?.accessInstructions.orEmpty())
    }

    var pinnedLocation by remember {
        mutableStateOf<DetectedDoorLocation?>(
            when {
                detectedLocation?.hasValidCoordinates == true -> detectedLocation
                initialDraft != null && (initialDraft.latitude != 0.0 || initialDraft.longitude != 0.0) ->
                    DetectedDoorLocation(
                        doorHint = initialDraft.doorNumber,
                        streetArea = initialDraft.streetArea,
                        localityName = initialDraft.label,
                        landmark = initialDraft.landmark,
                        postalCode = initialDraft.postalCode,
                        latitude = initialDraft.latitude,
                        longitude = initialDraft.longitude,
                        accuracyMeters = 5f,
                        providerLabel = "Saved Door Pin"
                    )
                else -> null
            }
        )
    }

    // Automatically hydrate street, landmark, PIN, and door hint when Google Location Services locks coordinates
    LaunchedEffect(detectedLocation) {
        val loc = detectedLocation ?: return@LaunchedEffect
        if (loc.hasValidCoordinates) {
            pinnedLocation = loc
        }
        if (doorNumber.isBlank() && loc.doorHint.isNotBlank()) {
            doorNumber = loc.doorHint
        }
        if (streetArea.isBlank() || streetArea.startsWith("GPS Coordinates") || streetArea.startsWith("Pinned GPS")) {
            streetArea = loc.streetArea
        }
        if (landmark.isBlank()) {
            landmark = loc.landmark
        }
        if (loc.postalCode.isNotBlank()) {
            postalCode = loc.postalCode
        }
    }

    val activeLocation = pinnedLocation ?: detectedLocation

    val effectiveStreet = streetArea.trim().ifBlank {
        activeLocation?.streetArea?.trim().orEmpty()
    }
    val effectivePin = postalCode.trim().ifBlank {
        activeLocation?.postalCode?.trim()?.ifBlank { "500034" } ?: "500034"
    }
    val canSubmit = !isBusy &&
        doorNumber.trim().isNotBlank() &&
        effectiveStreet.isNotBlank() &&
        effectivePin.length >= 4

    AmbientAtelierBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("door_setup_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Atelier Brand & Skip Header in ChromeBar Liquid Glass
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.ChromeBar,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.door_setup_eyebrow),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (allowDismiss) {
                            TextButton(
                                onClick = onSkipOrClose,
                                modifier = Modifier
                                    .height(48.dp)
                                    .tactilePressPhysics(pressedScale = 0.95f)
                                    .testTag("skip_door_setup_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.door_btn_skip),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.door_btn_skip),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Editorial Title Block
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.staggeredSpringEntrance(index = 0)
                ) {
                    Text(
                        text = stringResource(R.string.door_setup_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.door_setup_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 1. Google Location Services Live Coordinates Radar Liquid Glass Card
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.FloatingPill,
                    shape = RoundedCornerShape(26.dp),
                    accentBorderColor = BrassGold.copy(alpha = 0.55f),
                    shadowElevation = 6.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = 1)
                        .testTag("detected_coordinates_card")
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Custom Canvas Concentric Brass Radar & Crosshairs
                        Canvas(
                            modifier = Modifier.matchParentSize()
                        ) {
                            val center = Offset(x = size.width * 0.86f, y = size.height * 0.34f)
                            val maxRadius = size.minDimension * 0.62f
                            drawCircle(
                                color = BrassGold.copy(alpha = 0.09f),
                                radius = maxRadius,
                                center = center,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawCircle(
                                color = BrassGold.copy(alpha = 0.15f),
                                radius = maxRadius * 0.65f,
                                center = center,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            drawCircle(
                                color = BrassGold.copy(alpha = 0.24f),
                                radius = maxRadius * 0.32f,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawLine(
                                color = BrassGold.copy(alpha = 0.10f),
                                start = Offset(center.x - maxRadius, center.y),
                                end = Offset(center.x + maxRadius, center.y),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = BrassGold.copy(alpha = 0.10f),
                                start = Offset(center.x, center.y - maxRadius),
                                end = Offset(center.x, center.y + maxRadius),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = when {
                                        activeLocation != null -> SageVerified
                                        isFetchingLocation -> BrassGold.copy(alpha = 0.22f)
                                        else -> Color.White.copy(alpha = 0.12f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (activeLocation != null) Color.Transparent else BrassGold.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isFetchingLocation) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(13.dp),
                                                color = BrassGold,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (activeLocation != null) Icons.Filled.GpsFixed else Icons.Filled.LocationSearching,
                                                contentDescription = stringResource(R.string.cd_gps_radar_icon),
                                                tint = if (activeLocation != null) Color.White else BrassGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when {
                                                isFetchingLocation -> stringResource(R.string.door_coordinates_detecting)
                                                activeLocation != null -> stringResource(R.string.door_coordinates_locked)
                                                !hasLocationPermission -> stringResource(R.string.door_coordinates_pending)
                                                else -> "READY TO DETECT GPS"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (activeLocation != null) Color.White else Color(0xFFFFDF9E)
                                        )
                                    }
                                }

                                if (activeLocation != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LiveGlassPulseDot(
                                            statusKey = activeLocation.formattedCoordinates(),
                                            color = Color(0xFF8BE8C0),
                                            size = 8.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = activeLocation.formattedAccuracy(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF8BE8C0)
                                        )
                                    }
                                }
                            }

                            // Explicit Latitude & Longitude Telemetry Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CoordinateMetricTile(
                                    label = "LATITUDE",
                                    value = activeLocation?.formattedLatitude()
                                        ?: if (isFetchingLocation) "Acquiring…" else "— — . — — —°",
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("latitude_readout")
                                )
                                CoordinateMetricTile(
                                    label = "LONGITUDE",
                                    value = activeLocation?.formattedLongitude()
                                        ?: if (isFetchingLocation) "Acquiring…" else "— — . — — —°",
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("longitude_readout")
                                )
                            }

                            // Detected Street / Status Message
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = when {
                                        activeLocation != null -> "${activeLocation.localityName} • PIN ${activeLocation.postalCode}"
                                        isFetchingLocation -> "Connecting to Google Fused Location Provider…"
                                        !hasLocationPermission -> "Allow location access or search & drag the map pin below"
                                        else -> "Tap below to fetch live coordinates or drag the gate pin on map"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        activeLocation != null -> activeLocation.streetArea
                                        locationErrorMessage != null -> locationErrorMessage
                                        else -> "Exact gate coordinates help your assigned barber navigate right to your doorstep."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD9CCBC),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = onRequestDetectCoordinates,
                                enabled = !isFetchingLocation,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BrassGold),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFFDF9E)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .tactilePressPhysics(enableHaptics = true)
                                    .testTag("detect_gps_coordinates_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = null,
                                    tint = BrassGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (activeLocation == null) {
                                        stringResource(R.string.door_btn_detect_gps)
                                    } else {
                                        stringResource(R.string.door_btn_refresh_gps)
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 1B. Interactive Street Search & Draggable Gate Pin Picker
                InteractiveLocationPinMap(
                    currentLocation = activeLocation,
                    isLoadingHardwareGps = isFetchingLocation,
                    onRequestHardwareGps = onRequestDetectCoordinates,
                    onLocationPinned = { loc ->
                        pinnedLocation = loc
                        streetArea = loc.streetArea
                        landmark = loc.landmark
                        if (loc.postalCode.isNotBlank()) {
                            postalCode = loc.postalCode
                        }
                        if (doorNumber.isBlank() && loc.doorHint.isNotBlank()) {
                            doorNumber = loc.doorHint
                        }
                        onLocationPinned?.invoke(loc)
                    },
                    pinTitle = "Search Street or Drag Gate Pin"
                )

                // 2. Specific House / Door Number Form Atelier Liquid Glass Card
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 3.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = 2)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DoorFront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PARTICULAR DOOR DETAILS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Where should your barber knock?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Tactile Liquid Glass Property Type Selector Pills
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassPill(
                                text = "HOME",
                                leadingIcon = Icons.Filled.Home,
                                selected = label == "HOME",
                                onClick = { label = "HOME" }
                            )
                            LiquidGlassPill(
                                text = "APARTMENT",
                                leadingIcon = Icons.Filled.Apartment,
                                selected = label == "APARTMENT",
                                onClick = { label = "APARTMENT" }
                            )
                            LiquidGlassPill(
                                text = "VILLA",
                                leadingIcon = Icons.Filled.Villa,
                                selected = label == "VILLA",
                                onClick = { label = "VILLA" }
                            )
                            LiquidGlassPill(
                                text = "OFFICE",
                                leadingIcon = Icons.Filled.Business,
                                selected = label == "OFFICE",
                                onClick = { label = "OFFICE" }
                            )
                        }

                        // Primary Specific House / Door Number Input
                        OutlinedTextField(
                            value = doorNumber,
                            onValueChange = { doorNumber = it },
                            label = { Text(stringResource(R.string.door_label_house_input)) },
                            placeholder = { Text(stringResource(R.string.door_placeholder_house_input)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.DoorFront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("modal_door_input")
                        )

                        // Optional Building / Floor / Society
                        OutlinedTextField(
                            value = buildingName,
                            onValueChange = { buildingName = it },
                            label = { Text(stringResource(R.string.door_label_building_input)) },
                            placeholder = { Text(stringResource(R.string.door_placeholder_building_input)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Apartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("door_building_input")
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                        // Auto-filled Street & Locality
                        OutlinedTextField(
                            value = streetArea,
                            onValueChange = { streetArea = it },
                            label = { Text(stringResource(R.string.door_label_street_input)) },
                            placeholder = { Text("e.g., 100 Feet Road, Indiranagar") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("modal_street_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = landmark,
                                onValueChange = { landmark = it },
                                label = { Text(stringResource(R.string.door_label_landmark_input)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("door_landmark_input")
                            )
                            OutlinedTextField(
                                value = postalCode,
                                onValueChange = { postalCode = it.take(10) },
                                label = { Text(stringResource(R.string.door_label_pin_input)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .width(126.dp)
                                    .testTag("door_pin_input")
                            )
                        }

                        OutlinedTextField(
                            value = accessInstructions,
                            onValueChange = { accessInstructions = it },
                            label = { Text(stringResource(R.string.door_label_instructions_input)) },
                            placeholder = { Text("e.g., Ring bell 204, security intercom #12") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("door_instructions_input")
                        )

                        // Privacy Trust Strip in AccentEmerald Glass
                        LiquidGlassSurface(
                            strength = GlassMaterialStrength.AccentEmerald,
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 0.dp,
                            interactivePhysics = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = SageVerified,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Exact house/door numbers are encrypted and revealed only to your assigned barber.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Primary Confirm CTA with Physics Compression
                Button(
                    onClick = {
                        val fallbackLoc = LocationHelper.resolveBestFallbackLocationForText(
                            streetOrArea = effectiveStreet,
                            postalCode = effectivePin
                        )
                        val resolvedLat = when {
                            activeLocation?.hasValidCoordinates == true -> activeLocation.latitude
                            initialDraft != null && initialDraft.latitude != 0.0 -> initialDraft.latitude
                            else -> fallbackLoc.latitude
                        }
                        val resolvedLon = when {
                            activeLocation?.hasValidCoordinates == true -> activeLocation.longitude
                            initialDraft != null && initialDraft.longitude != 0.0 -> initialDraft.longitude
                            else -> fallbackLoc.longitude
                        }
                        onConfirmDoorAddress(
                            PendingDoorAddressDraft(
                                label = label,
                                doorNumber = doorNumber.trim(),
                                buildingName = buildingName.trim(),
                                streetArea = effectiveStreet,
                                landmark = landmark.trim().ifBlank {
                                    activeLocation?.landmark ?: fallbackLoc.landmark
                                },
                                postalCode = effectivePin,
                                accessInstructions = accessInstructions.trim(),
                                latitude = resolvedLat,
                                longitude = resolvedLon
                            )
                        )
                    },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EspressoHeaderBg,
                        contentColor = Color(0xFFFFDF9E)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .staggeredSpringEntrance(index = 3)
                        .tactilePressPhysics(enabled = canSubmit, enableHaptics = true)
                        .testTag("modal_save_door_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = BrassGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = BrassGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = primaryButtonLabel ?: stringResource(R.string.door_btn_confirm_save),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CoordinateMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.34f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFDF9E)
            )
            AnimatedSummaryText(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
