package com.example.ui.barber

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BarberProfile
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.PlatformConfig
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.InteractiveLocationPinMap
import com.example.ui.components.LegalPoliciesSheet
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
import com.example.ui.theme.SageVerified
import com.example.util.DetectedDoorLocation
import com.example.util.LocationHelper
import java.util.Locale

@Composable
fun BarberProfileScreen(
    barberProfile: BarberProfile?,
    displayName: String,
    partnerPhone: String = "",
    isAdmin: Boolean = false,
    catalogServices: List<CatalogService>,
    platformConfig: PlatformConfig,
    isBusy: Boolean,
    onSaveBarberServicesAndUpi: (
        upiId: String,
        shopName: String,
        areaAddress: String,
        serviceIds: List<String>,
        servesLadies: Boolean,
        servesMen: Boolean
    ) -> Unit,
    onCreateSupportTicket: (String, String) -> Unit,
    onSwitchToAdminMode: () -> Unit = {},
    onSignOut: () -> Unit,
    detectedLocation: DetectedDoorLocation? = null,
    isFetchingLocation: Boolean = false,
    onRequestGpsLocation: () -> Unit = {},
    onSaveFullBarberProfile: ((
        upiId: String,
        shopName: String,
        areaAddress: String,
        serviceIds: List<String>,
        servesLadies: Boolean,
        servesMen: Boolean,
        bio: String,
        experienceYears: Long,
        latitude: Double,
        longitude: Double
    ) -> Unit)? = null
) {
    val activeName = barberProfile?.name?.ifBlank { displayName } ?: displayName.ifBlank { "Partner Stylist" }
    val activePhone = barberProfile?.phone?.ifBlank { partnerPhone } ?: partnerPhone
    val activeGender = barberProfile?.gender?.ifBlank { "MALE" } ?: "MALE"
    val approvalStatus = barberProfile?.approvalStatus ?: "PENDING_SETUP"
    val isOnline = barberProfile?.isOnline ?: true

    var upiInput by remember(barberProfile?.upiId) { mutableStateOf(barberProfile?.upiId.orEmpty()) }
    var shopNameInput by remember(barberProfile?.shopName) {
        mutableStateOf(barberProfile?.effectiveShopName().orEmpty())
    }
    var areaAddressInput by remember(barberProfile?.shopAddress) {
        mutableStateOf(barberProfile?.effectiveShopAddress("Hyderabad").orEmpty())
    }
    var bioInput by remember(barberProfile?.bio) {
        mutableStateOf(barberProfile?.bio.orEmpty())
    }
    var expYearsInput by remember(barberProfile?.experienceYears) {
        mutableStateOf((barberProfile?.experienceYears ?: 3L).toString())
    }
    var servesLadies by remember(barberProfile?.servesLadies, activeGender) {
        mutableStateOf(barberProfile?.servesLadies ?: (activeGender == "FEMALE"))
    }
    var servesMen by remember(barberProfile?.servesMen, activeGender) {
        mutableStateOf(barberProfile?.servesMen ?: (activeGender != "FEMALE"))
    }
    var selectedIds by remember(barberProfile?.serviceIds, catalogServices) {
        val initial = barberProfile?.serviceIds?.toSet().orEmpty()
        mutableStateOf(if (initial.isEmpty()) catalogServices.take(5).map { it.id }.toSet() else initial)
    }

    var coverageRadiusKm by rememberSaveable { mutableDoubleStateOf(10.0) }

    val initialHubLocation = remember(barberProfile?.latitude, barberProfile?.longitude, barberProfile?.shopAddress) {
        val profLat = barberProfile?.latitude ?: 0.0
        val profLon = barberProfile?.longitude ?: 0.0
        if (kotlin.math.abs(profLat) > 0.0001 || kotlin.math.abs(profLon) > 0.0001) {
            DetectedDoorLocation(
                doorHint = "",
                streetArea = barberProfile?.effectiveShopAddress("Hyderabad").orEmpty(),
                localityName = "Hyderabad",
                landmark = "",
                postalCode = "",
                latitude = profLat,
                longitude = profLon
            )
        } else {
            LocationHelper.resolveBestFallbackLocationForText(
                streetOrArea = barberProfile?.effectiveShopAddress("Banjara Hills, Hyderabad").orEmpty(),
                postalCode = ""
            )
        }
    }

    var hubLocation by remember(initialHubLocation.latitude, initialHubLocation.longitude) {
        mutableStateOf(initialHubLocation)
    }

    LaunchedEffect(detectedLocation?.latitude, detectedLocation?.longitude) {
        val loc = detectedLocation
        if (loc != null && loc.hasValidCoordinates) {
            hubLocation = loc
            val formattedArea = listOf(loc.streetArea, loc.localityName)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
            if (formattedArea.isNotBlank()) {
                areaAddressInput = formattedArea
            }
        }
    }

    var showSupportModal by remember { mutableStateOf(false) }
    var showPoliciesSheet by remember { mutableStateOf(false) }
    var supportSubject by remember { mutableStateOf("") }
    var supportDesc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("barber_profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "VERIFIED PARTNER ACCOUNT",
                title = "Barber & Stylist Profile",
                subtitle = "Pin your dispatch base hub on the map, customize your specialization & bio, and manage offered services.",
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        // 1. Public Identity, Verified Phone, Gender Specialization & Approval Status
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.FloatingPill,
                shape = RoundedCornerShape(24.dp),
                accentBorderColor = BrassGold.copy(alpha = 0.55f),
                shadowElevation = 4.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 1)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BarberAvatarBadge(
                            name = activeName,
                            isOnline = isOnline,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = BrassGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", barberProfile?.ratingAvg ?: 5.0)} (${barberProfile?.reviewCount ?: 0L} reviews) · $activeGender",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFDF9E)
                                )
                            }
                        }
                        LiveGlassPulseDot(
                            statusKey = "$approvalStatus-$isOnline",
                            color = if (isOnline) SageVerified else BrassGold,
                            size = 8.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(approvalStatus)
                    }

                    if (activePhone.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = null, tint = BrassGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Customer Dispatch Contact: $activePhone",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF8F3EC)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = BrassGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (approvalStatus == "APPROVED") {
                                "Verified DoorCut Partner · Base GPS (${String.format(Locale.US, "%.4f, %.4f", hubLocation.latitude, hubLocation.longitude)})"
                            } else {
                                "Complete & save your profile below to receive open customer requests"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF8F3EC)
                        )
                    }
                }
            }
        }

        // 2. Interactive Base Studio / Dispatch Hub GPS Pin Map
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 2)
                    .testTag("barber_base_hub_map_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dispatch Base Hub & Radar Coverage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Pin where you start your shift so Open Dispatch calculates accurate ride distances and ETAs to nearby homes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coverage Preview:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(5, 10, 15).forEach { km ->
                            LiquidGlassPill(
                                text = "$km km",
                                selected = coverageRadiusKm.toInt() == km,
                                onClick = { coverageRadiusKm = km.toDouble() },
                                modifier = Modifier.testTag("barber_coverage_radius_$km")
                            )
                        }
                    }

                    InteractiveLocationPinMap(
                        currentLocation = hubLocation,
                        isLoadingHardwareGps = isFetchingLocation,
                        onRequestHardwareGps = onRequestGpsLocation,
                        onLocationPinned = { pinned ->
                            hubLocation = pinned
                            val formattedArea = listOf(pinned.streetArea, pinned.localityName)
                                .filter { it.isNotBlank() }
                                .distinct()
                                .joinToString(", ")
                            if (formattedArea.isNotBlank()) {
                                areaAddressInput = formattedArea
                            }
                        },
                        pinTitle = "Pin Your Dispatch Hub / Starting Point",
                        coverageRadiusKm = coverageRadiusKm
                    )
                }
            }
        }

        // 3. Specialization, Experience, Bio & Direct UPI
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 3)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Specialization, Bio & Direct UPI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Choose which customer requests appear in your Open Dispatch Pool (including Female clients requesting a Lady Hair Stylist).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = servesMen,
                            onClick = {
                                if (servesMen && !servesLadies) return@FilterChip
                                servesMen = !servesMen
                            },
                            label = { Text("Men & Kids Barber") },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("chip_serves_men")
                        )
                        FilterChip(
                            selected = servesLadies,
                            onClick = {
                                if (servesLadies && !servesMen) return@FilterChip
                                servesLadies = !servesLadies
                            },
                            label = { Text("Ladies Hair Stylist") },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("chip_serves_ladies")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = shopNameInput,
                            onValueChange = { shopNameInput = it.take(80) },
                            label = { Text("Public Studio / Partner Title") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.7f)
                                .testTag("barber_shop_name_input")
                        )
                        OutlinedTextField(
                            value = expYearsInput,
                            onValueChange = { expYearsInput = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Exp (Yrs)") },
                            leadingIcon = { Icon(Icons.Filled.WorkOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("barber_experience_input")
                        )
                    }
                    OutlinedTextField(
                        value = areaAddressInput,
                        onValueChange = { areaAddressInput = it.take(180) },
                        label = { Text("Primary Dispatch Neighbourhood") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("barber_area_address_input")
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it.take(380) },
                        label = { Text("Craft Bio & Specialties (shown on customer live card)") },
                        placeholder = { Text("e.g., Skin fade specialist, bridal & layer styling, sterilized cordless kit.") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("barber_bio_input")
                    )
                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it.take(80) },
                        label = { Text("Direct Customer UPI ID (generates doorstep QR pass)") },
                        placeholder = { Text("yourname@okaxis") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("barber_upi_input")
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Shift: ${(barberProfile?.workingDays ?: listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")).joinToString(", ")} · ${CurrencyUtils.formatHourSlot(barberProfile?.workStartHour ?: 9L)} – ${CurrencyUtils.formatHourSlot(barberProfile?.workEndHour ?: 20L)} · Buffer: ${barberProfile?.travelBufferMinutes ?: 15L}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Services Offered & Prices
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 4)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Services Offered & Standard Prices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Standard rates are transparent so customers pay the exact listed amount directly to you after completion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    catalogServices.forEach { svc ->
                        val checked = selectedIds.contains(svc.id)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedIds = if (isChecked) selectedIds + svc.id else selectedIds - svc.id
                                    }
                                )
                                Text(
                                    text = "${svc.name} (${svc.durationMinutes}m)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = CurrencyUtils.formatRupees(svc.pricePaise),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    val canSaveProfile = !isBusy
                    Button(
                        onClick = {
                            val expYears = (expYearsInput.toLongOrNull() ?: 3L).coerceIn(0L, 50L)
                            val resolvedHub = if (hubLocation.hasValidCoordinates) {
                                hubLocation
                            } else {
                                LocationHelper.resolveBestFallbackLocationForText(areaAddressInput, "")
                            }
                            if (onSaveFullBarberProfile != null) {
                                onSaveFullBarberProfile(
                                    upiInput.trim(),
                                    shopNameInput.trim(),
                                    areaAddressInput.trim(),
                                    selectedIds.toList(),
                                    servesLadies,
                                    servesMen,
                                    bioInput.trim(),
                                    expYears,
                                    resolvedHub.latitude,
                                    resolvedHub.longitude
                                )
                            } else {
                                onSaveBarberServicesAndUpi(
                                    upiInput.trim(),
                                    shopNameInput.trim(),
                                    areaAddressInput.trim(),
                                    selectedIds.toList(),
                                    servesLadies,
                                    servesMen
                                )
                            }
                        },
                        enabled = canSaveProfile,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EspressoHeaderBg,
                            contentColor = Color(0xFFFFDF9E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .tactilePressPhysics(enabled = canSaveProfile, enableHaptics = true)
                            .testTag("save_barber_profile_button")
                    ) {
                        Text("Save Hub Pin, Bio, Services & UPI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Legal Policies, Partner Support, Optional Admin Console & Logout
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 5)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isAdmin) {
                        OutlinedButton(
                            onClick = onSwitchToAdminMode,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .tactilePressPhysics(enableHaptics = true)
                                .testTag("barber_open_admin_button")
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Admin Governance Console", fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = { showPoliciesSheet = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tactilePressPhysics()
                            .testTag("barber_privacy_policy_button")
                    ) {
                        Icon(Icons.Filled.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Policy, Safety Charter & Terms", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { showSupportModal = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tactilePressPhysics()
                            .testTag("barber_support_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partner Support Desk")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tactilePressPhysics(enableHaptics = true)
                            .testTag("barber_logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out of Partner Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPoliciesSheet) {
        LegalPoliciesSheet(onDismiss = { showPoliciesSheet = false })
    }

    if (showSupportModal) {
        AlertDialog(
            onDismissRequest = { showSupportModal = false },
            title = { Text("Barber Partner Support") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = platformConfig.policyText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = supportSubject,
                        onValueChange = { supportSubject = it },
                        label = { Text("Issue Topic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supportDesc,
                        onValueChange = { supportDesc = it },
                        label = { Text("Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateSupportTicket(
                            supportSubject.ifBlank { "Barber Partner Help" },
                            supportDesc.ifBlank { "Assistance requested" }
                        )
                        showSupportModal = false
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportModal = false }, modifier = Modifier.height(48.dp)) {
                    Text("Close")
                }
            }
        )
    }
}
