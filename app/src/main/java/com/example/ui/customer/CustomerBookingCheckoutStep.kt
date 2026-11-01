package com.example.ui.customer

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.ServiceArea
import com.example.ui.components.InteractiveLocationPinMap
import com.example.ui.components.LegalPoliciesSheet
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.util.DetectedDoorLocation
import com.example.util.LocationHelper
import com.example.util.RazorpayBookingFeeGateway
import com.example.util.PendingDoorAddressDraft

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConfirmHomeAddressStep(
    isAuthenticated: Boolean,
    savedAddresses: List<CustomerAddress>,
    selectedAddressId: String?,
    detectedLocation: DetectedDoorLocation?,
    pendingDoorDraft: PendingDoorAddressDraft?,
    isFetchingLocation: Boolean,
    optionalInstructions: String,
    onOptionalInstructionsChange: (String) -> Unit,
    onSelectSavedAddress: (String?) -> Unit,
    onRequestGps: () -> Unit,
    onLocationPinned: ((DetectedDoorLocation) -> Unit)? = null,
    onSaveAndContinue: (PendingDoorAddressDraft) -> Unit,
    onContinueWithExistingAddress: () -> Unit
) {
    val activeSaved = savedAddresses.firstOrNull { it.id == selectedAddressId }
        ?: savedAddresses.firstOrNull { it.isDefault }
        ?: savedAddresses.firstOrNull()

    var addressType by remember {
        mutableStateOf(activeSaved?.label ?: pendingDoorDraft?.label ?: "HOME")
    }
    var flatHouse by remember {
        mutableStateOf(activeSaved?.flatHouse ?: pendingDoorDraft?.combinedFlatHouse() ?: "")
    }
    var streetArea by remember {
        mutableStateOf(
            activeSaved?.streetArea
                ?: pendingDoorDraft?.streetArea
                ?: detectedLocation?.streetArea
                ?: "Banjara Hills, Hyderabad"
        )
    }
    var landmark by remember {
        mutableStateOf(activeSaved?.landmark ?: pendingDoorDraft?.landmark ?: detectedLocation?.landmark ?: "")
    }
    var postalCode by remember {
        mutableStateOf(activeSaved?.postalCode ?: pendingDoorDraft?.postalCode ?: detectedLocation?.postalCode ?: "500034")
    }

    var pinnedLocation by remember {
        mutableStateOf<DetectedDoorLocation?>(
            when {
                detectedLocation?.hasValidCoordinates == true -> detectedLocation
                activeSaved != null && (activeSaved.latitude != 0.0 || activeSaved.longitude != 0.0) ->
                    DetectedDoorLocation(
                        doorHint = activeSaved.flatHouse,
                        streetArea = activeSaved.streetArea,
                        localityName = activeSaved.serviceAreaName.ifBlank { activeSaved.label },
                        landmark = activeSaved.landmark,
                        postalCode = activeSaved.postalCode,
                        latitude = activeSaved.latitude,
                        longitude = activeSaved.longitude,
                        accuracyMeters = 5f,
                        providerLabel = "Saved Address Pin"
                    )
                pendingDoorDraft != null && (pendingDoorDraft.latitude != 0.0 || pendingDoorDraft.longitude != 0.0) ->
                    DetectedDoorLocation(
                        doorHint = pendingDoorDraft.doorNumber,
                        streetArea = pendingDoorDraft.streetArea,
                        localityName = pendingDoorDraft.label,
                        landmark = pendingDoorDraft.landmark,
                        postalCode = pendingDoorDraft.postalCode,
                        latitude = pendingDoorDraft.latitude,
                        longitude = pendingDoorDraft.longitude,
                        accuracyMeters = 5f,
                        providerLabel = "Pinned Draft"
                    )
                else -> null
            }
        )
    }

    LaunchedEffect(detectedLocation) {
        val loc = detectedLocation ?: return@LaunchedEffect
        if (loc.hasValidCoordinates) {
            pinnedLocation = loc
        }
        if (streetArea.isBlank() || streetArea == "Banjara Hills, Hyderabad") {
            streetArea = loc.streetArea
        }
        if (landmark.isBlank()) landmark = loc.landmark
        if (loc.postalCode.isNotBlank()) postalCode = loc.postalCode
    }

    val activeLocation = pinnedLocation ?: detectedLocation

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .testTag("step_confirm_address"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Interactive Street Search + Draggable Gate Pin Picker + Live GPS Lock
        item {
            InteractiveLocationPinMap(
                currentLocation = activeLocation,
                isLoadingHardwareGps = isFetchingLocation,
                onRequestHardwareGps = onRequestGps,
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
                    onLocationPinned?.invoke(loc)
                },
                pinTitle = "Pin Exact Home / Society Gate"
            )
        }

        // Saved Addresses Selector
        if (isAuthenticated && savedAddresses.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Saved Home Addresses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    savedAddresses.forEach { addr ->
                        val isChosen = addr.id == (selectedAddressId ?: activeSaved?.id)
                        Card(
                            onClick = {
                                onSelectSavedAddress(addr.id)
                                addressType = addr.label
                                flatHouse = addr.flatHouse
                                streetArea = addr.streetArea
                                landmark = addr.landmark
                                postalCode = addr.postalCode
                                if (addr.latitude != 0.0 || addr.longitude != 0.0) {
                                    pinnedLocation = DetectedDoorLocation(
                                        doorHint = addr.flatHouse,
                                        streetArea = addr.streetArea,
                                        localityName = addr.serviceAreaName.ifBlank { addr.label },
                                        landmark = addr.landmark,
                                        postalCode = addr.postalCode,
                                        latitude = addr.latitude,
                                        longitude = addr.longitude,
                                        accuracyMeters = 5f,
                                        providerLabel = "Saved Address Pin"
                                    )
                                }
                                if (addr.accessInstructions.isNotBlank()) {
                                    onOptionalInstructionsChange(addr.accessInstructions)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                if (isChosen) 2.dp else 1.dp,
                                if (isChosen) SageVerified else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${addr.displayLabel()} · ${addr.flatHouse}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${addr.streetArea} (${addr.postalCode})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isChosen) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = SageVerified)
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onContinueWithExistingAddress,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Use Selected Saved Address →", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Address Search + Saved Label Chips (Home, Work, Other) + Inputs
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Doorstep Address Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HOME" to "Home", "OFFICE" to "Work", "OTHER" to "Other").forEach { (code, title) ->
                            FilterChip(
                                selected = addressType == code,
                                onClick = { addressType = code },
                                label = { Text(title) },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = flatHouse,
                        onValueChange = { flatHouse = it },
                        label = { Text("Flat / House Number *") },
                        placeholder = { Text("e.g., Flat 302, Sunrise Residency") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flow_flat_input")
                    )

                    OutlinedTextField(
                        value = streetArea,
                        onValueChange = { streetArea = it },
                        label = { Text("Street & Area *") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
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
                            onValueChange = { postalCode = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("PIN *") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(116.dp)
                        )
                    }

                    OutlinedTextField(
                        value = optionalInstructions,
                        onValueChange = onOptionalInstructionsChange,
                        label = { Text("Arrival note for Barber") },
                        placeholder = { Text("Call when downstairs") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flow_instructions_input")
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Call when downstairs", "Ask security at gate", "Ring doorbell once").forEach { tip ->
                            FilterChip(
                                selected = optionalInstructions == tip,
                                onClick = { onOptionalInstructionsChange(tip) },
                                label = { Text(tip) }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = SageVerified,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Zero-Trust Privacy: Open pool barbers see only your neighbourhood area. Your exact flat number & phone unlock ONLY after a verified stylist accepts your order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val cleanStreet = streetArea.trim()
                            val cleanPin = postalCode.trim().ifBlank { "500034" }
                            val fallbackLoc = LocationHelper.resolveBestFallbackLocationForText(
                                streetOrArea = cleanStreet,
                                postalCode = cleanPin
                            )
                            val resolvedLat = when {
                                activeLocation?.hasValidCoordinates == true -> activeLocation.latitude
                                activeSaved != null && activeSaved.latitude != 0.0 -> activeSaved.latitude
                                else -> fallbackLoc.latitude
                            }
                            val resolvedLon = when {
                                activeLocation?.hasValidCoordinates == true -> activeLocation.longitude
                                activeSaved != null && activeSaved.longitude != 0.0 -> activeSaved.longitude
                                else -> fallbackLoc.longitude
                            }
                            onSaveAndContinue(
                                PendingDoorAddressDraft(
                                    label = addressType,
                                    doorNumber = flatHouse.trim(),
                                    streetArea = cleanStreet,
                                    landmark = landmark.trim().ifBlank {
                                        activeLocation?.landmark ?: fallbackLoc.landmark
                                    },
                                    postalCode = cleanPin,
                                    accessInstructions = optionalInstructions.trim(),
                                    latitude = resolvedLat,
                                    longitude = resolvedLon
                                )
                            )
                        },
                        enabled = flatHouse.isNotBlank() && streetArea.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EspressoHeaderBg,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .tactilePressPhysics(enabled = flatHouse.isNotBlank() && streetArea.isNotBlank(), enableHaptics = true)
                            .testTag("address_continue_button")
                    ) {
                        Text("Confirm Address & Continue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BookingPaymentStep(
    isAuthenticated: Boolean,
    customerName: String,
    customerPhone: String,
    recipientGender: String,
    recipientAge: Long,
    preferLadyBarber: Boolean,
    bookingMode: String,
    selectedServices: List<CatalogService>,
    selectedDateKey: String,
    selectedStartHour: Long,
    savedAddresses: List<CustomerAddress>,
    selectedAddressId: String?,
    pendingDoorDraft: PendingDoorAddressDraft?,
    detectedLocation: DetectedDoorLocation?,
    serviceAreas: List<ServiceArea>,
    platformConfig: PlatformConfig,
    bookingFeePaise: Long,
    optionalInstructions: String,
    isBusy: Boolean,
    onEditStep: (Int) -> Unit,
    onConfirmBooking: (CustomerAddress, Long) -> Unit
) {
    var showLegalPolicies by remember { mutableStateOf(false) }

    val resolvedAddress = remember(savedAddresses, selectedAddressId, pendingDoorDraft, detectedLocation) {
        val base = savedAddresses.firstOrNull { it.id == selectedAddressId }
            ?: savedAddresses.firstOrNull { it.isDefault }
            ?: savedAddresses.firstOrNull()
        if (base != null) {
            val fallback = LocationHelper.resolveBestFallbackLocationForText(base.streetArea, base.postalCode)
            base.copy(
                latitude = when {
                    base.latitude != 0.0 -> base.latitude
                    detectedLocation?.hasValidCoordinates == true -> detectedLocation.latitude
                    else -> fallback.latitude
                },
                longitude = when {
                    base.longitude != 0.0 -> base.longitude
                    detectedLocation?.hasValidCoordinates == true -> detectedLocation.longitude
                    else -> fallback.longitude
                }
            )
        } else {
            val draftStreet = pendingDoorDraft?.streetArea ?: detectedLocation?.streetArea ?: "Banjara Hills, Hyderabad"
            val draftPin = pendingDoorDraft?.postalCode ?: detectedLocation?.postalCode ?: "500034"
            val fallback = LocationHelper.resolveBestFallbackLocationForText(draftStreet, draftPin)
            CustomerAddress(
                id = "draft_addr",
                label = pendingDoorDraft?.label ?: "HOME",
                flatHouse = pendingDoorDraft?.combinedFlatHouse()?.ifBlank { "Doorstep" } ?: "Doorstep",
                streetArea = draftStreet,
                landmark = pendingDoorDraft?.landmark ?: detectedLocation?.landmark ?: fallback.landmark,
                postalCode = draftPin,
                serviceAreaName = pendingDoorDraft?.streetArea ?: detectedLocation?.localityName ?: fallback.localityName,
                accessInstructions = pendingDoorDraft?.accessInstructions ?: optionalInstructions,
                latitude = pendingDoorDraft?.latitude?.takeIf { it != 0.0 }
                    ?: detectedLocation?.latitude?.takeIf { it != 0.0 }
                    ?: fallback.latitude,
                longitude = pendingDoorDraft?.longitude?.takeIf { it != 0.0 }
                    ?: detectedLocation?.longitude?.takeIf { it != 0.0 }
                    ?: fallback.longitude
            )
        }
    }

    val matchedArea = remember(serviceAreas, resolvedAddress.postalCode) {
        serviceAreas.firstOrNull { it.postalCodes.contains(resolvedAddress.postalCode) }
    }
    val travelChargePaise = matchedArea?.travelChargePaise ?: 0L
    val serviceSubtotalPaise = selectedServices.sumOf { it.pricePaise }
    val barberPayablePaise = serviceSubtotalPaise + travelChargePaise
    val servicesHeadline = remember(selectedServices) {
        selectedServices.joinToString(" + ") { it.name }.ifBlank { "Selected Services" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .testTag("step_booking_payment"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Open Dispatch Stylist & Client Match Summary Card
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (preferLadyBarber) Color(0xFF6A1B9A) else EspressoHeaderBg
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (preferLadyBarber) Icons.Filled.Face3 else Icons.Filled.Radar,
                                    contentDescription = null,
                                    tint = BrassGold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (preferLadyBarber) {
                                        "Lady Hair Stylist Priority Dispatch"
                                    } else {
                                        "Verified Barber Auto-Dispatch"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (bookingMode == "ASAP") {
                                        "ASAP Express (20–35 min) · $selectedDateKey"
                                    } else {
                                        "$selectedDateKey at ${CurrencyUtils.formatHourSlot(selectedStartHour)}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        TextButton(onClick = { onEditStep(2) }, modifier = Modifier.height(48.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "Client: $customerName · $recipientGender ($recipientAge yrs) · Phone: ${customerPhone.ifBlank { "Verified on profile" }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${resolvedAddress.displayLabel()}: ${resolvedAddress.flatHouse}, ${resolvedAddress.streetArea} (${resolvedAddress.postalCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Exact Two-Payment Clarity Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, BrassGold.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_payment_breakdown_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = servicesHeadline,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Barber service charge (paid directly after service)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatRupees(barberPayablePaise),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Booking confirmation & dispatch fee",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Broadcasts to verified stylists & locks live GPS tracking",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatRupees(bookingFeePaise),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        color = EspressoHeaderBg,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Pay now",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFDF9E)
                                )
                                Text(
                                    text = "100% refunded if no stylist accepts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD9CBB8)
                                )
                            }
                            Text(
                                text = CurrencyUtils.formatRupees(bookingFeePaise),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.testTag("pay_now_amount_text")
                            )
                        }
                    }

                    Surface(
                        color = SageVerified.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SageVerified.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Handshake,
                                contentDescription = null,
                                tint = SageVerified,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Pay ${CurrencyUtils.formatRupees(barberPayablePaise)} directly to your barber after the service by cash or UPI (not processed by the app).",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("pay_barber_after_text")
                            )
                        }
                    }

                    Text(
                        text = if (RazorpayBookingFeeGateway.isServerEndpointConfigured()) {
                            "Upfront confirmation fee is verified on the server via Razorpay HMAC signature."
                        } else {
                            "Upfront fee status starts as Pending Server/Admin Verification until verified by Razorpay Cloud Function or Admin desk."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("payment_verification_mode_text")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cancellation: ${platformConfig.policyText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { showLegalPolicies = true },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Policies", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Primary Dispatch CTA
        item {
            Button(
                onClick = { onConfirmBooking(resolvedAddress, travelChargePaise) },
                enabled = !isBusy && isAuthenticated && selectedServices.isNotEmpty(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EspressoHeaderBg,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .tactilePressPhysics(enabled = !isBusy && selectedServices.isNotEmpty(), enableHaptics = true)
                    .testTag("pay_confirm_booking_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = BrassGold, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = BrassGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pay ${CurrencyUtils.formatRupees(bookingFeePaise)} & Request Barber Now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    if (showLegalPolicies) {
        LegalPoliciesSheet(onDismiss = { showLegalPolicies = false })
    }
}
