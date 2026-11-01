package com.example.ui.barber

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BarberProfile
import com.example.data.model.Booking
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.ServiceArea
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricBentoCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.glass.LiveGlassPulseDot
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.AmberPending
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.util.DetectedDoorLocation
import com.example.util.LocationHelper
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

/**
 * Pure schedule & travel-buffer conflict checker for Barber Partner dispatch cards.
 * Returns a human-readable warning string if [candidate] overlaps an active visit (+ travel buffer)
 * or falls inside the barber's configured break window; returns null when the slot fits cleanly.
 */
fun detectBarberSlotConflict(
    candidate: Booking,
    confirmedBookings: List<Booking>,
    travelBufferMinutes: Long,
    breakStartHour: Long,
    breakEndHour: Long
): String? {
    val bufferMin = travelBufferMinutes.coerceIn(0L, 180L)
    val candStartMin = candidate.startHour * 60L
    val candEndMin = candStartMin + candidate.durationMinutes.coerceAtLeast(15L)

    if (candidate.bookingMode != "ASAP" && breakEndHour > breakStartHour) {
        val breakStartMin = breakStartHour * 60L
        val breakEndMin = breakEndHour * 60L
        if (candStartMin < breakEndMin && candEndMin > breakStartMin) {
            return "Falls in your ${CurrencyUtils.formatHourSlot(breakStartHour)}–${CurrencyUtils.formatHourSlot(breakEndHour)} break"
        }
    }

    val sameDayActive = confirmedBookings.filter {
        it.id != candidate.id &&
            it.dateKey == candidate.dateKey &&
            it.status in setOf("CONFIRMED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS")
    }

    for (existing in sameDayActive) {
        val existStartMin = existing.startHour * 60L
        val existEndMin = existStartMin + existing.durationMinutes.coerceAtLeast(15L)
        if (candStartMin < (existEndMin + bufferMin) && (candEndMin + bufferMin) > existStartMin) {
            return "Overlaps ${existing.customerName.ifBlank { "booked visit" }} (${CurrencyUtils.formatHourSlot(existing.startHour)}) + ${bufferMin}m travel buffer"
        }
    }
    return null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarberDashboardScreen(
    barberProfile: BarberProfile?,
    defaultDisplayName: String,
    defaultPhone: String = "",
    bookings: List<Booking>,
    openDispatchBookings: List<Booking>,
    catalogServices: List<CatalogService>,
    serviceAreas: List<ServiceArea>,
    isBusy: Boolean,
    onToggleOnline: (Boolean) -> Unit,
    onClaimOpenBooking: (Booking) -> Unit,
    onAcceptOrDeclineRequest: (Booking, Boolean, String) -> Unit,
    onAdvanceTripStatus: (String, String) -> Unit,
    onOpenBookingDetail: (String) -> Unit,
    onApplyAsBarber: (String, String, String, Long, String, String, String, String) -> Unit,
    detectedLocation: DetectedDoorLocation? = null,
    isFetchingLocation: Boolean = false,
    onRequestBarberGps: (() -> Unit)? = null,
    onSyncLiveGpsCoordinates: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val todayKey = remember { MarketplaceViewModel.defaultTodayDateKey() }

    var filterMode by rememberSaveable { mutableStateOf("MATCHING") }
    var maxRadiusKmFilter by rememberSaveable { mutableStateOf(0) } // 0 = All, 3, 7, 15
    var sortMode by rememberSaveable { mutableStateOf("ASAP") } // ASAP, NEAREST, PAYOUT

    val barberLat = detectedLocation?.latitude?.takeIf { it != 0.0 } ?: barberProfile?.latitude ?: 0.0
    val barberLon = detectedLocation?.longitude?.takeIf { it != 0.0 } ?: barberProfile?.longitude ?: 0.0
    val hasBarberCoords = barberLat != 0.0 || barberLon != 0.0

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onRequestBarberGps?.invoke()
        }
    }

    // Background passive radar sync while Barber is Online and permission is granted
    val isOnline = barberProfile?.isOnline ?: true
    LaunchedEffect(isOnline, barberProfile?.barberId) {
        if (isOnline && barberProfile != null && onSyncLiveGpsCoordinates != null && LocationHelper.hasLocationPermission(context)) {
            LocationHelper.observeLiveGpsLocations(context, intervalMillis = 15_000L).collectLatest { loc ->
                if (loc.latitude != 0.0 || loc.longitude != 0.0) {
                    onSyncLiveGpsCoordinates(loc.latitude, loc.longitude)
                }
            }
        }
    }

    // Combine Open Dispatch Pool + any directly assigned AWAITING_BARBER requests with distance & sort filters
    val combinedDispatchQueue = remember(
        openDispatchBookings,
        bookings,
        barberProfile,
        filterMode,
        maxRadiusKmFilter,
        sortMode,
        barberLat,
        barberLon
    ) {
        val directPending = bookings.filter { it.status == "AWAITING_BARBER" }
        val merged = (openDispatchBookings + directPending)
            .distinctBy { it.id }
            .filter { it.status == "AWAITING_BARBER" }

        val skillFiltered = if (filterMode == "MATCHING" && barberProfile != null) {
            merged.filter { req ->
                val ladyCheck = !req.preferLadyBarber || barberProfile.servesLadies || barberProfile.gender == "FEMALE"
                val menCheck = if (req.customerGender == "MALE") barberProfile.servesMen else true
                ladyCheck && menCheck
            }
        } else {
            merged
        }

        val radiusFiltered = if (maxRadiusKmFilter > 0 && hasBarberCoords) {
            skillFiltered.filter { req ->
                val dist = LocationHelper.computeRealDistanceKm(barberLat, barberLon, req.customerLat, req.customerLon)
                dist == null || dist <= maxRadiusKmFilter.toDouble()
            }
        } else {
            skillFiltered
        }

        when (sortMode) {
            "NEAREST" -> radiusFiltered.sortedWith(
                compareBy<Booking> {
                    LocationHelper.computeRealDistanceKm(barberLat, barberLon, it.customerLat, it.customerLon) ?: 999.0
                }.thenByDescending { it.bookingMode == "ASAP" }
            )
            "PAYOUT" -> radiusFiltered.sortedWith(
                compareByDescending<Booking> { it.barberServicePayablePaise }
                    .thenByDescending { it.bookingMode == "ASAP" }
            )
            else -> radiusFiltered.sortedWith(
                compareByDescending<Booking> { it.bookingMode == "ASAP" }
                    .thenBy { it.dateKey }
                    .thenBy { it.startHour }
            )
        }
    }

    val todaysBookings = remember(bookings, todayKey) {
        bookings.filter {
            it.dateKey == todayKey && it.status !in setOf("CANCELLED", "EXPIRED")
        }.sortedBy { it.startHour }
    }

    val nextAppointment = remember(bookings) {
        bookings.filter {
            it.status in setOf("CONFIRMED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS")
        }.sortedWith(compareBy({ it.dateKey }, { it.startHour })).firstOrNull()
    }

    val completedToday = remember(bookings, todayKey) {
        bookings.filter { it.dateKey == todayKey && it.status in setOf("COMPLETED", "DISPUTED") }
    }

    val todayDirectEarningsPaise = remember(completedToday) {
        completedToday.sumOf {
            if (it.directPaymentDeclaredPaise > 0L) it.directPaymentDeclaredPaise else it.barberServicePayablePaise
        }
    }

    val todayNetTakeHomePaise = remember(completedToday) {
        completedToday.sumOf {
            if (it.barberNetEarningsPaise > 0L) {
                it.barberNetEarningsPaise
            } else {
                (it.barberServicePayablePaise - it.barberCommissionPaise).coerceAtLeast(0L)
            }
        }
    }

    var applyName by remember(defaultDisplayName) { mutableStateOf(defaultDisplayName) }
    var applyPhone by remember(defaultPhone) { mutableStateOf(defaultPhone) }
    var applyShop by remember { mutableStateOf("DoorCut Verified Partner") }
    var applyArea by remember { mutableStateOf("Banjara Hills, Hyderabad") }
    var applyUpi by remember { mutableStateOf("") }
    var applyExp by remember { mutableStateOf("5") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("barber_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Online / Offline Availability, Specialization & Live GPS Radar Header
        item {
            val specLabel = when {
                barberProfile == null -> "Partner Setup Required"
                barberProfile.servesLadies && barberProfile.servesMen -> "Unisex Stylist (Men & Ladies)"
                barberProfile.servesLadies -> "Verified Lady Hair Stylist"
                else -> "Verified Men's Barber"
            }
            LiquidGlassSurface(
                strength = GlassMaterialStrength.FloatingPill,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 4.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = barberProfile?.name ?: defaultDisplayName.ifBlank { "Barber Partner" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = specLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = BrassGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LiveGlassPulseDot(
                                    statusKey = isOnline,
                                    color = if (isOnline) SageVerified else Color(0xFF9E9183),
                                    size = 8.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOnline) "Online · Receiving Live Customer Dispatch" else "Offline · Dispatch Paused",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOnline) Color(0xFFFFDF9E) else Color(0xFFD9CBB8)
                                )
                            }
                        }
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { if (barberProfile != null) onToggleOnline(it) },
                            enabled = !isBusy && barberProfile != null,
                            modifier = Modifier.testTag("barber_online_switch")
                        )
                    }

                    // Live Barber GPS Telemetry & 1-Tap Radar Lock Bar
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.32f)),
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
                                    imageVector = if (hasBarberCoords) Icons.Filled.GpsFixed else Icons.Filled.LocationSearching,
                                    contentDescription = null,
                                    tint = if (hasBarberCoords) Color(0xFF8BE8C0) else BrassGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (hasBarberCoords) {
                                            String.format(Locale.US, "Radar Pin: %.4f, %.4f", barberLat, barberLon)
                                        } else {
                                            "Barber GPS Radar Not Locked"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = detectedLocation?.streetArea
                                            ?: barberProfile?.shopAddress?.ifBlank { "Lock GPS for exact customer distances & ride ETAs" }
                                            ?: "Lock GPS for exact customer distances & ride ETAs",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFD9CBB8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    if (LocationHelper.hasLocationPermission(context)) {
                                        onRequestBarberGps?.invoke()
                                    } else {
                                        gpsPermissionLauncher.launch(LocationHelper.locationPermissionsArray())
                                    }
                                },
                                enabled = !isFetchingLocation,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = BrassGold,
                                    contentColor = EspressoHeaderBg
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("barber_lock_gps_button")
                            ) {
                                if (isFetchingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        strokeWidth = 2.dp,
                                        color = EspressoHeaderBg
                                    )
                                } else {
                                    Text(
                                        text = if (hasBarberCoords) "Refresh GPS" else "Lock GPS",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Optional Onboarding Card if signed-in barber hasn't created their BarberProfile document yet
        if (barberProfile == null) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, BrassGold),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .testTag("barber_onboarding_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Activate Your Barber Partner Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter your verified mobile number and UPI ID to claim live customer bookings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = applyName,
                            onValueChange = { applyName = it },
                            label = { Text("Stylist Full Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = applyPhone,
                                onValueChange = { applyPhone = it.filter { c -> c.isDigit() || c == '+' }.take(14) },
                                label = { Text("Mobile Number *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = applyExp,
                                onValueChange = { applyExp = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("Years Exp.") },
                                singleLine = true,
                                modifier = Modifier.width(110.dp)
                            )
                        }
                        OutlinedTextField(
                            value = applyUpi,
                            onValueChange = { applyUpi = it },
                            label = { Text("Direct UPI ID (for customer payments) *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val validPhone = applyPhone.filter { it.isDigit() }.length >= 10
                        Button(
                            onClick = {
                                onApplyAsBarber(
                                    applyName.ifBlank { "Verified Stylist" },
                                    applyPhone.trim(),
                                    "Verified home grooming specialist with sanitized kit.",
                                    applyExp.toLongOrNull() ?: 5L,
                                    applyUpi.ifBlank { "partner@upi" },
                                    applyShop,
                                    applyArea,
                                    "Haircut, Beard Trim, Ladies Styling"
                                )
                            },
                            enabled = !isBusy && applyName.isNotBlank() && validPhone,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_barber_application_button")
                        ) {
                            Text("Activate Partner Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Today's Work Summary Metrics (Gross Collected + Net Take-Home)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                MetricBentoCard(
                    icon = Icons.Filled.Schedule,
                    title = "Active & Today",
                    value = "${todaysBookings.size}",
                    subtitle = "${completedToday.size} completed · ${barberProfile?.travelBufferMinutes ?: 15L}m buffer",
                    modifier = Modifier.weight(1f)
                )
                MetricBentoCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Today's Payout",
                    value = CurrencyUtils.formatRupees(todayDirectEarningsPaise),
                    subtitle = "Net ${CurrencyUtils.formatRupees(todayNetTakeHomePaise)} take-home",
                    accentHighlight = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Active Trip Immediate Action Card (with Live GPS Distance & Compass Bearing)
        if (nextAppointment != null) {
            val activeDistKm = LocationHelper.computeRealDistanceKm(
                barberLat,
                barberLon,
                nextAppointment.customerLat,
                nextAppointment.customerLon
            )
            val activeEtaMin = activeDistKm?.let { LocationHelper.estimateTravelMinutes(it, isAsapExpress = true) }

            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(2.dp, SageVerified),
                    colors = CardDefaults.cardColors(containerColor = SageVerified.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .testTag("barber_next_appointment_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LiveGlassPulseDot(statusKey = nextAppointment.status, color = SageVerified, size = 8.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activeDistKm != null && activeEtaMin != null) {
                                        "ACTIVE TRIP · ${LocationHelper.formatDistanceKm(activeDistKm)} (~${activeEtaMin}m)"
                                    } else {
                                        "ACTIVE CUSTOMER TRIP · LIVE GPS"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SageVerified
                                )
                            }
                            StatusBadge(nextAppointment.status)
                        }
                        Text(
                            text = "${nextAppointment.customerName} · ${nextAppointment.customerGender} (${nextAppointment.customerAge}y)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${nextAppointment.serviceNamesSummary} (${nextAppointment.durationMinutes} min) · Collect ${CurrencyUtils.formatRupees(nextAppointment.barberServicePayablePaise)} directly",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onOpenBookingDetail(nextAppointment.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EspressoHeaderBg,
                                    contentColor = Color(0xFFFFDF9E)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .tactilePressPhysics(enableHaptics = true)
                                    .testTag("barber_view_booking_button")
                            ) {
                                Text("Open Live Trip & Map", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val query = if (nextAppointment.customerLat != 0.0 && nextAppointment.customerLon != 0.0) {
                                        "${nextAppointment.customerLat},${nextAppointment.customerLon}"
                                    } else {
                                        nextAppointment.areaName.ifBlank { "Hyderabad" }
                                    }
                                    val geoUri = Uri.parse("google.navigation:q=${Uri.encode(query)}")
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri)) }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .tactilePressPhysics()
                                    .testTag("barber_start_navigation_button")
                            ) {
                                Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Navigate", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (nextAppointment.status == "CONFIRMED") {
                            Button(
                                onClick = {
                                    onAdvanceTripStatus(nextAppointment.id, "ON_THE_WAY")
                                    onOpenBookingDetail(nextAppointment.id)
                                },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SageVerified, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .tactilePressPhysics(enableHaptics = true)
                            ) {
                                Text("Start Ride & Share Live GPS (On My Way)", fontWeight = FontWeight.ExtraBold)
                            }
                        } else if (nextAppointment.status == "ON_THE_WAY") {
                            Button(
                                onClick = { onAdvanceTripStatus(nextAppointment.id, "ARRIVED") },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .tactilePressPhysics(enableHaptics = true)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark Arrived at Customer Door", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Open Customer Dispatch Pool + Distance Radius & Smart Sort Controls
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionHeader(
                    eyebrow = "LIVE CUSTOMER DISPATCH POOL",
                    title = "Select Customer Request (${combinedDispatchQueue.size})",
                    subtitle = "Filter by travel radius, payout, or skill match. Exact flat & phone unlock once you claim."
                )

                // Skill Match Filter + Smart Sort Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterMode == "MATCHING",
                        onClick = { filterMode = "MATCHING" },
                        label = { Text("Matched Skills") },
                        modifier = Modifier.height(48.dp)
                    )
                    FilterChip(
                        selected = filterMode == "ALL",
                        onClick = { filterMode = "ALL" },
                        label = { Text("All Requests") },
                        modifier = Modifier.height(48.dp)
                    )
                    FilterChip(
                        selected = sortMode == "ASAP",
                        onClick = { sortMode = "ASAP" },
                        label = { Text("Sort: Urgent ASAP") },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("barber_sort_asap")
                    )
                    FilterChip(
                        selected = sortMode == "NEAREST",
                        onClick = { sortMode = "NEAREST" },
                        label = { Text("Sort: Nearest") },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("barber_sort_nearest")
                    )
                    FilterChip(
                        selected = sortMode == "PAYOUT",
                        onClick = { sortMode = "PAYOUT" },
                        label = { Text("Sort: Top Payout") },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("barber_sort_payout")
                    )
                }

                // Travel Radius Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        0 to "Any Distance",
                        3 to "≤ 3 km",
                        7 to "≤ 7 km",
                        15 to "≤ 15 km"
                    ).forEach { (km, title) ->
                        FilterChip(
                            selected = maxRadiusKmFilter == km,
                            onClick = { maxRadiusKmFilter = km },
                            label = { Text(title) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("barber_radius_filter_$km")
                        )
                    }
                }
            }
        }

        if (combinedDispatchQueue.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.NotificationsActive,
                    title = if (maxRadiusKmFilter > 0) {
                        "No Requests Within $maxRadiusKmFilter km"
                    } else {
                        "Watching Live Customer Radar"
                    },
                    subtitle = if (maxRadiusKmFilter > 0) {
                        "Tap 'Any Distance' above to widen your dispatch radar or refresh your GPS pin."
                    } else {
                        "As soon as a customer books a home haircut or styling session nearby, it appears here for you to claim."
                    }
                )
            }
        } else {
            items(combinedDispatchQueue, key = { it.id }) { req ->
                val canServeLadyReq = !req.preferLadyBarber ||
                    (barberProfile?.servesLadies == true || barberProfile?.gender == "FEMALE")
                val distKm = LocationHelper.computeRealDistanceKm(
                    barberLat,
                    barberLon,
                    req.customerLat,
                    req.customerLon
                )
                val bearingCompass = if (distKm != null) {
                    LocationHelper.compassDirection(
                        LocationHelper.bearingDegrees(barberLat, barberLon, req.customerLat, req.customerLon)
                    )
                } else null
                val rideEtaMin = distKm?.let {
                    LocationHelper.estimateTravelMinutes(it, isAsapExpress = req.bookingMode == "ASAP")
                }
                val slotConflictWarning = detectBarberSlotConflict(
                    candidate = req,
                    confirmedBookings = bookings,
                    travelBufferMinutes = barberProfile?.travelBufferMinutes ?: 15L,
                    breakStartHour = barberProfile?.breakStartHour ?: 13L,
                    breakEndHour = barberProfile?.breakEndHour ?: 14L
                )

                OpenDispatchRequestCard(
                    booking = req,
                    distanceKm = distKm,
                    bearingCompass = bearingCompass,
                    rideEtaMinutes = rideEtaMin,
                    travelBufferMinutes = barberProfile?.travelBufferMinutes ?: 15L,
                    slotConflictWarning = slotConflictWarning,
                    isBusy = isBusy,
                    canClaim = barberProfile != null && canServeLadyReq,
                    ineligibleReason = if (!canServeLadyReq) {
                        "Customer requested a verified Lady Hair Stylist"
                    } else null,
                    onAccept = {
                        if (req.isOpenDispatch) {
                            onClaimOpenBooking(req)
                        } else {
                            onAcceptOrDeclineRequest(req, true, "")
                        }
                    },
                    onDecline = if (!req.isOpenDispatch) {
                        { onAcceptOrDeclineRequest(req, false, "Barber unavailable for slot") }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpenDispatchRequestCard(
    booking: Booking,
    distanceKm: Double?,
    bearingCompass: String?,
    rideEtaMinutes: Int?,
    travelBufferMinutes: Long,
    slotConflictWarning: String?,
    isBusy: Boolean,
    canClaim: Boolean,
    ineligibleReason: String?,
    onAccept: () -> Unit,
    onDecline: (() -> Unit)?
) {
    val netTakeHomePaise = if (booking.barberNetEarningsPaise > 0L) {
        booking.barberNetEarningsPaise
    } else {
        (booking.barberServicePayablePaise - booking.barberCommissionPaise).coerceAtLeast(0L)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, BrassGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .testTag("barber_request_card_${booking.id}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (booking.bookingMode == "ASAP") {
                        Surface(
                            color = Color(0xFFD84315),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "ASAP EXPRESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (booking.bookingMode == "ASAP") {
                            "Immediate Visit"
                        } else {
                            "${booking.dateKey} · ${CurrencyUtils.formatHourSlot(booking.startHour)}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EspressoHeaderBg
                    ) {
                        Text(
                            text = CurrencyUtils.formatRupees(booking.barberServicePayablePaise),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFDF9E),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    if (booking.barberCommissionPaise > 0L) {
                        Text(
                            text = "Net ${CurrencyUtils.formatRupees(netTakeHomePaise)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SageVerified,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Live Distance + Compass Bearing + Ride ETA + Demographics Badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (distanceKm != null) {
                    Surface(
                        color = SageVerified.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, SageVerified.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TwoWheeler,
                                contentDescription = null,
                                tint = SageVerified,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val dirLabel = bearingCompass?.let { " $it" }.orEmpty()
                            val etaLabel = rideEtaMinutes?.let { " · ~${it}m ride" }.orEmpty()
                            Text(
                                text = "${LocationHelper.formatDistanceKm(distanceKm)}$dirLabel$etaLabel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = SageVerified
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${booking.customerName.ifBlank { "Customer" }} · ${booking.customerGender} (${booking.customerAge} yrs)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (booking.preferLadyBarber) {
                    Surface(
                        color = Color(0xFF8E24AA).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFF8E24AA)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Face3, contentDescription = null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lady Hair Stylist Requested",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF6A1B9A)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${booking.serviceNamesSummary} · ${booking.durationMinutes} min",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zone: ${booking.areaSummary.ifBlank { "Hyderabad" }} · Exact flat & phone unlock on accept",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Smart Schedule & Travel-Buffer Fit Indicator
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (slotConflictWarning != null) {
                    AmberPending.copy(alpha = 0.14f)
                } else {
                    SageVerified.copy(alpha = 0.10f)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (slotConflictWarning != null) Icons.Filled.WarningAmber else Icons.Filled.Explore,
                        contentDescription = null,
                        tint = if (slotConflictWarning != null) AmberPending else SageVerified,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = slotConflictWarning
                            ?: "Fits your schedule cleanly (+${travelBufferMinutes}m travel buffer)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (slotConflictWarning != null) AmberPending else SageVerified
                    )
                }
            }

            if (ineligibleReason != null) {
                Text(
                    text = ineligibleReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = !isBusy && canClaim,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EspressoHeaderBg,
                        contentColor = Color(0xFFFFDF9E)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .tactilePressPhysics(enabled = !isBusy && canClaim, enableHaptics = true)
                        .testTag("accept_request_button_${booking.id}")
                ) {
                    Text(
                        text = if (booking.isOpenDispatch) "Accept Customer Booking" else "Accept Request",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                if (onDecline != null) {
                    OutlinedButton(
                        onClick = onDecline,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .height(52.dp)
                            .tactilePressPhysics()
                            .testTag("decline_request_button_${booking.id}")
                    ) {
                        Text("Decline", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
