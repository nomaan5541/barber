package com.example.ui.customer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.model.CustomerAddress
import com.example.data.model.ServiceArea
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.InteractiveLocationPinMap
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassPill
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.glass.LiveGlassPulseDot
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.util.DetectedDoorLocation
import com.example.util.LocationHelper

@Composable
fun CustomerAddressesScreen(
    viewModel: MarketplaceViewModel,
    addresses: List<CustomerAddress>,
    serviceAreas: List<ServiceArea>,
    isBusy: Boolean
) {
    val context = LocalContext.current
    val detectedLocation by viewModel.detectedLocation.collectAsStateWithLifecycle()
    val isFetchingLocation by viewModel.isFetchingLocation.collectAsStateWithLifecycle()

    var label by remember { mutableStateOf("HOME") }
    var flatHouse by remember { mutableStateOf("") }
    var streetArea by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("500034") }
    var accessInstructions by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(addresses.isEmpty()) }
    var pinnedLocation by remember { mutableStateOf<DetectedDoorLocation?>(detectedLocation) }

    LaunchedEffect(detectedLocation) {
        val loc = detectedLocation ?: return@LaunchedEffect
        if (loc.hasValidCoordinates) {
            pinnedLocation = loc
        }
        if (flatHouse.isBlank() && loc.doorHint.isNotBlank()) flatHouse = loc.doorHint
        if (streetArea.isBlank()) streetArea = loc.streetArea
        if (landmark.isBlank()) landmark = loc.landmark
        if (loc.postalCode.isNotBlank()) postalCode = loc.postalCode
    }

    val activeLocation = pinnedLocation ?: detectedLocation

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            viewModel.autoFetchLocationAndPromptDoor(context, promptForHomeAddressAfterFetch = false)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_addresses_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "ENCRYPTED DOORSTEP VAULT",
                title = "Home Door & GPS Addresses",
                subtitle = "Exact flat/door numbers remain private and are shared only with your assigned barber.",
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        // GPS Radar Liquid Glass Banner + Interactive Pin Map
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 1),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.FloatingPill,
                    shape = RoundedCornerShape(22.dp),
                    accentBorderColor = BrassGold.copy(alpha = 0.5f),
                    shadowElevation = 4.dp,
                    interactivePhysics = false,
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(BrassGold.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = null,
                                    tint = BrassGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (activeLocation != null) {
                                        LiveGlassPulseDot(
                                            statusKey = activeLocation.formattedCoordinates(),
                                            color = Color(0xFF8BE8C0),
                                            size = 8.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = if (activeLocation != null) {
                                            "Pin Locked: ${activeLocation.formattedCoordinates()}"
                                        } else {
                                            "Google Location Services Coordinates"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = if (activeLocation != null) {
                                        "${activeLocation.streetArea} (${activeLocation.postalCode})"
                                    } else {
                                        "Tap Auto-GPS or search & drag the map pin below"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD6C8B6)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (LocationHelper.hasLocationPermission(context)) {
                                    viewModel.autoFetchLocationAndPromptDoor(context, promptForHomeAddressAfterFetch = false)
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !isFetchingLocation,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrassGold,
                                contentColor = EspressoHeaderBg
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .tactilePressPhysics(enableHaptics = true)
                                .testTag("auto_fetch_gps_address_button")
                        ) {
                            if (isFetchingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = EspressoHeaderBg,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Auto-GPS", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                InteractiveLocationPinMap(
                    currentLocation = activeLocation,
                    isLoadingHardwareGps = isFetchingLocation,
                    onRequestHardwareGps = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            viewModel.autoFetchLocationAndPromptDoor(context, promptForHomeAddressAfterFetch = false)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onLocationPinned = { loc ->
                        pinnedLocation = loc
                        streetArea = loc.streetArea
                        landmark = loc.landmark
                        if (loc.postalCode.isNotBlank()) {
                            postalCode = loc.postalCode
                        }
                        if (flatHouse.isBlank() && loc.doorHint.isNotBlank()) {
                            flatHouse = loc.doorHint
                        }
                    },
                    pinTitle = "Search Street or Drag Gate Pin"
                )
            }
        }

        // Add New Door Address Atelier Glass Form
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 2)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AddLocationAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Particular Door Address",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HOME", "OFFICE", "OTHER").forEach { lbl ->
                            LiquidGlassPill(
                                text = lbl,
                                selected = label == lbl,
                                onClick = { label = lbl }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = flatHouse,
                        onValueChange = { flatHouse = it },
                        label = { Text("Particular Door / Flat / House No. *") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("address_flat_input")
                    )
                    OutlinedTextField(
                        value = streetArea,
                        onValueChange = { streetArea = it },
                        label = { Text("Street & Locality *") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("address_street_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = landmark,
                            onValueChange = { landmark = it },
                            label = { Text("Landmark") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = postalCode,
                            onValueChange = { postalCode = it },
                            label = { Text("PIN Code *") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(122.dp)
                        )
                    }
                    OutlinedTextField(
                        value = accessInstructions,
                        onValueChange = { accessInstructions = it },
                        label = { Text("Doorbell / Gate / Security Instructions") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                        Text("Set as default home visit address", style = MaterialTheme.typography.bodySmall)
                    }
                    val canSave = !isBusy && flatHouse.isNotBlank() && streetArea.isNotBlank() && postalCode.length >= 4
                    Button(
                        onClick = {
                            if (canSave) {
                                val fallbackLoc = LocationHelper.resolveBestFallbackLocationForText(
                                    streetOrArea = streetArea,
                                    postalCode = postalCode
                                )
                                val resolvedLat = activeLocation?.latitude?.takeIf { it != 0.0 } ?: fallbackLoc.latitude
                                val resolvedLon = activeLocation?.longitude?.takeIf { it != 0.0 } ?: fallbackLoc.longitude
                                viewModel.saveAddress(
                                    label = label,
                                    flatHouse = flatHouse,
                                    streetArea = streetArea,
                                    landmark = landmark.ifBlank { activeLocation?.landmark ?: fallbackLoc.landmark },
                                    postalCode = postalCode,
                                    accessInstructions = accessInstructions,
                                    isDefault = isDefault,
                                    areas = serviceAreas,
                                    latitude = resolvedLat,
                                    longitude = resolvedLon
                                )
                                flatHouse = ""
                                streetArea = ""
                                landmark = ""
                                accessInstructions = ""
                            }
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EspressoHeaderBg,
                            contentColor = Color(0xFFFFDF9E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .tactilePressPhysics(enabled = canSave, enableHaptics = true)
                            .testTag("save_address_button")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Home Door Address", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (addresses.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.LocationOn,
                        title = "No Saved Door Addresses",
                        subtitle = "Tap Auto-GPS above to detect your location and enter your particular door number."
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "Saved Doorstep Locations (${addresses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                )
            }
            itemsIndexed(addresses, key = { _, a -> a.id }) { idx, addr ->
                LiquidGlassSurface(
                    strength = if (addr.isDefault) GlassMaterialStrength.AccentEmerald else GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = true,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = idx + 1)
                        .testTag("saved_address_card_${addr.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (addr.label == "OFFICE") Icons.Filled.Apartment else Icons.Filled.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(addr.label)
                                if (addr.isDefault) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StatusBadge("DEFAULT")
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${addr.flatHouse}, ${addr.streetArea}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${addr.landmark} • PIN ${addr.postalCode} (${addr.serviceAreaName})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (addr.latitude != 0.0 || addr.longitude != 0.0) {
                                Text(
                                    text = "Coordinates: ${LocationHelper.formatCoord(addr.latitude)}, ${LocationHelper.formatCoord(addr.longitude)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (addr.accessInstructions.isNotBlank()) {
                                Text(
                                    text = "Gate Note: ${addr.accessInstructions}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.deleteAddress(addr.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .tactilePressPhysics(pressedScale = 0.88f, enableHaptics = true)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete address",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
