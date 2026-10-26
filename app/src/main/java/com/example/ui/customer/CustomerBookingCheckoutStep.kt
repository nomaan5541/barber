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
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.BarberProfile
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.ServiceArea
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.util.DetectedDoorLocation
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
    onSaveAndContinue: (PendingDoorAddressDraft) -> Unit,
    onContinueWithExistingAddress: () -> Unit
) {
    val activeSaved = savedAddresses.firstOrNull { it.id == selectedAddressId }
        ?: savedAddresses.firstOrNull { it.isDefault }
        ?: savedAddresses.firstOrNull()

    var addressType by remember {
        mutableStateOf(activeSaved?.label ?: pendingDoorDraft?.label ?: "HOME")
    }
    var areaSearchQuery by remember { mutableStateOf("") }
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

    LaunchedEffect(detectedLocation) {
        val loc = detectedLocation ?: return@LaunchedEffect
        if (streetArea.isBlank() || streetArea == "Banjara Hills, Hyderabad") {
            streetArea = loc.streetArea
        }
        if (landmark.isBlank()) landmark = loc.landmark
        if (loc.postalCode.isNotBlank()) postalCode = loc.postalCode
    }

    val quickAreas = remember {
        listOf(
            "Banjara Hills, Hyderabad" to "500034",
            "Jubilee Hills, Hyderabad" to "500033",
            "Madhapur, Hyderabad" to "500081",
            "Gachibowli, Hyderabad" to "500032",
            "Indiranagar, Bengaluru" to "560038"
        )
    }
    val matchingAreas = remember(areaSearchQuery) {
        if (areaSearchQuery.isBlank()) emptyList()
        else quickAreas.filter { it.first.contains(areaSearchQuery, ignoreCase = true) || it.second.contains(areaSearchQuery) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .testTag("step_confirm_address"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Map Pin + GPS Card
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.FloatingPill,
                shape = RoundedCornerShape(20.dp),
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
                            Icon(Icons.Filled.MyLocation, contentDescription = null, tint = BrassGold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (detectedLocation != null) {
                                    "Map Pin Locked · ${detectedLocation.localityName}"
                                } else {
                                    "Pin Your Home Location"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = detectedLocation?.formattedCoordinates()
                                    ?: "Use GPS pin to fill street & PIN automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD9CBB8)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRequestGps,
                        enabled = !isFetchingLocation,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrassGold,
                            contentColor = EspressoHeaderBg
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .tactilePressPhysics(enableHaptics = true)
                    ) {
                        if (isFetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("GPS Pin", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Saved Addresses Selector (if authenticated and has saved addresses)
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
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Address Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Saved Label Chips: Home, Work, Other
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

                    // Area Search
                    OutlinedTextField(
                        value = areaSearchQuery,
                        onValueChange = { areaSearchQuery = it },
                        label = { Text("Search area or neighbourhood") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (matchingAreas.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            matchingAreas.forEach { (area, pin) ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        streetArea = area
                                        postalCode = pin
                                        areaSearchQuery = ""
                                    },
                                    label = { Text("$area ($pin)") }
                                )
                            }
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
                        label = { Text("Optional instructions") },
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
                            text = "Privacy protected: Your full address is shared only with your assigned barber when confirmed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            onSaveAndContinue(
                                PendingDoorAddressDraft(
                                    label = addressType,
                                    doorNumber = flatHouse.trim(),
                                    streetArea = streetArea.trim(),
                                    landmark = landmark.trim(),
                                    postalCode = postalCode.trim().ifBlank { "500034" },
                                    accessInstructions = optionalInstructions.trim(),
                                    latitude = detectedLocation?.latitude ?: 0.0,
                                    longitude = detectedLocation?.longitude ?: 0.0
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
    selectedServices: List<CatalogService>,
    chosenBarber: BarberProfile,
    selectedDateKey: String,
    selectedStartHour: Long,
    savedAddresses: List<CustomerAddress>,
    selectedAddressId: String?,
    pendingDoorDraft: PendingDoorAddressDraft?,
    serviceAreas: List<ServiceArea>,
    platformConfig: PlatformConfig,
    bookingFeePaise: Long,
    optionalInstructions: String,
    isBusy: Boolean,
    onEditStep: (Int) -> Unit,
    onConfirmBooking: (CustomerAddress, Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val resolvedAddress = remember(savedAddresses, selectedAddressId, pendingDoorDraft) {
        savedAddresses.firstOrNull { it.id == selectedAddressId }
            ?: savedAddresses.firstOrNull { it.isDefault }
            ?: savedAddresses.firstOrNull()
            ?: CustomerAddress(
                id = "draft_addr",
                label = pendingDoorDraft?.label ?: "HOME",
                flatHouse = pendingDoorDraft?.combinedFlatHouse()?.ifBlank { "Flat 204" } ?: "Flat 204",
                streetArea = pendingDoorDraft?.streetArea ?: "Banjara Hills, Hyderabad",
                landmark = pendingDoorDraft?.landmark ?: "",
                postalCode = pendingDoorDraft?.postalCode ?: "500034",
                serviceAreaName = pendingDoorDraft?.streetArea ?: "Hyderabad",
                accessInstructions = pendingDoorDraft?.accessInstructions ?: optionalInstructions
            )
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
        // Compact Appointment Summary
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${chosenBarber.name} · $selectedDateKey at ${CurrencyUtils.formatHourSlot(selectedStartHour)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${resolvedAddress.displayLabel()}: ${resolvedAddress.flatHouse}, ${resolvedAddress.streetArea}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onEditStep(2) }, modifier = Modifier.height(48.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }
                }
            }
        }

        // Exact Two-Payment Clarity Card (Matching User Specification)
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(24.dp),
                accentBorderColor = BrassGold.copy(alpha = 0.65f),
                shadowElevation = 4.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_payment_breakdown_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Barber Service Amount (Paid AFTER service)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = servicesHeadline,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Barber service charge (paid after service)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatRupees(barberPayablePaise),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 2. Booking Confirmation Fee
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Booking confirmation fee",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Reserves your appointment & verified barber",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatRupees(bookingFeePaise),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 3. Large "Pay now ₹30" Liquid Glass Box
                    LiquidGlassSurface(
                        strength = GlassMaterialStrength.FloatingPill,
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 3.dp,
                        interactivePhysics = false,
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
                                    color = Color(0xFFFFDF9E)
                                )
                                Text(
                                    text = "Booking confirmation only",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD9CBB8)
                                )
                            }
                            Text(
                                text = CurrencyUtils.formatRupees(bookingFeePaise),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.testTag("pay_now_amount_text")
                            )
                        }
                    }

                    // 4. Direct-to-Barber Instruction Callout
                    LiquidGlassSurface(
                        strength = GlassMaterialStrength.AccentEmerald,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 0.dp,
                        interactivePhysics = false,
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
                                text = "Pay ${CurrencyUtils.formatRupees(barberPayablePaise)} directly to your barber after the service by cash or UPI.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("pay_barber_after_text")
                            )
                        }
                    }

                    Text(
                        text = "Cancellation policy: ${platformConfig.policyText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Primary CTA (or Guest Google Sign-In Gate)
        item {
            if (isAuthenticated) {
                Button(
                    onClick = { onConfirmBooking(resolvedAddress, travelChargePaise) },
                    enabled = !isBusy && selectedServices.isNotEmpty(),
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
                            text = "Pay ${CurrencyUtils.formatRupees(bookingFeePaise)} & Confirm Booking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = false
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Sign in to confirm your booking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your selected services, slot, and address are ready. Sign in with Google to pay ${CurrencyUtils.formatRupees(bookingFeePaise)} & confirm.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (authError != null) {
                            Text(
                                text = authError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = {
                                isSigningIn = true
                                authError = null
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = { isSigningIn = false },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        authError = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isSigningIn = false }
                                )
                            },
                            enabled = !isSigningIn,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EspressoHeaderBg,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .tactilePressPhysics(enabled = !isSigningIn, enableHaptics = true)
                                .testTag("checkout_google_sign_in_button")
                        ) {
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
}
