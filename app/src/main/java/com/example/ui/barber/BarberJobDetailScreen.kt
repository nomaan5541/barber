package com.example.ui.barber

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Booking
import com.example.data.model.BookingPrivateAddress
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LiveBarberTrackingMap
import com.example.ui.components.StatusBadge
import com.example.ui.components.VisitProgressStepper
import com.example.ui.glass.AmbientAtelierBackdrop
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassPill
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
import com.example.util.LocationHelper

@Composable
fun BarberJobDetailScreen(
    booking: Booking?,
    protectedAddress: BookingPrivateAddress?,
    catalogServices: List<CatalogService>,
    isBusy: Boolean,
    onLoadProtectedAddress: (String) -> Unit,
    onAdvanceTripStatus: (String, String) -> Unit,
    onUpdateLiveLocation: (String, Double, Double) -> Unit,
    onFinishService: (String) -> Unit,
    onRequestExtraService: (String, CatalogService) -> Unit,
    onRecordDirectPayment: (String, String, String, Long, String) -> Unit,
    onBack: () -> Unit,
    barberUpiId: String = ""
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val reducedMotion = LocalReducedMotion.current

    var hasLocationPerm by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }
    var gpsPacketsSent by remember { mutableIntStateOf(0) }
    var copiedUpiToast by remember { mutableStateOf(false) }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocationPerm = LocationHelper.hasLocationPermission(context)
    }

    LaunchedEffect(booking?.id) {
        if (booking != null && booking.id.isNotBlank()) {
            onLoadProtectedAddress(booking.id)
        }
    }

    // Stream continuous real GPS telemetry to Firestore while barber is riding / arriving
    LaunchedEffect(booking?.id, booking?.status, hasLocationPerm) {
        val activeBooking = booking ?: return@LaunchedEffect
        if (hasLocationPerm && activeBooking.status in setOf("CONFIRMED", "ON_THE_WAY", "ARRIVED")) {
            LocationHelper.observeLiveGpsLocations(context).collect { loc ->
                if (loc.latitude != 0.0 && loc.longitude != 0.0) {
                    gpsPacketsSent += 1
                    onUpdateLiveLocation(activeBooking.id, loc.latitude, loc.longitude)
                }
            }
        }
    }

    var selectedPaymentOutcome by remember { mutableStateOf("CASH") }
    var amountRupeesInput by remember(booking?.barberServicePayablePaise) {
        mutableStateOf(((booking?.barberServicePayablePaise ?: 0L) / 100L).toString())
    }
    var paymentNoteInput by remember { mutableStateOf("") }
    var showAddExtraPicker by remember { mutableStateOf(false) }

    // 5-Point Home Salon Hygiene & Sanitization Protocol State
    val hygieneLabels = remember {
        listOf(
            "Clipper blades & guards UV / spray sanitized",
            "Fresh disposable neck strip & clean salon cape",
            "Single-use razor blade unsealed before client",
            "Floor drop-sheet laid out for zero-mess trim",
            "Post-cut hair sweep & disposal bag sealed"
        )
    }
    val hygieneChecked = remember(booking?.id) {
        mutableStateListOf(false, false, false, false, false)
    }

    AmbientAtelierBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("barber_job_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.ChromeBar,
                shape = RoundedCornerShape(0.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .tactilePressPhysics(pressedScale = 0.9f)
                            .testTag("barber_job_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Trip & Customer Door",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (booking != null) {
                            Text(
                                text = if (booking.bookingMode == "ASAP") {
                                    "ASAP Express · ${booking.dateKey}"
                                } else {
                                    "${booking.dateKey} · ${CurrencyUtils.formatHourSlot(booking.startHour)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (booking != null) {
                        LiveGlassPulseDot(statusKey = booking.status, color = BrassGold, size = 8.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(booking.status)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            if (booking == null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.LocationOn,
                        title = "Appointment Not Found",
                        subtitle = "Return to your Barber Dashboard to view active appointments."
                    )
                }
                return@Column
            }

            val fullAddressText = protectedAddress?.fullAddressSnapshot?.ifBlank { booking.areaName }
                ?: booking.areaName.ifBlank { "Customer Home Address" }
            val verifiedCustomerPhone = remember(protectedAddress?.customerPhone, booking.customerPhone) {
                (protectedAddress?.customerPhone?.ifBlank { booking.customerPhone } ?: booking.customerPhone).trim()
            }
            val liveDistanceKm = remember(
                booking.barberLat,
                booking.barberLon,
                booking.customerLat,
                booking.customerLon
            ) {
                LocationHelper.computeRealDistanceKm(
                    booking.barberLat,
                    booking.barberLon,
                    booking.customerLat,
                    booking.customerLon
                )
            }
            val liveCompass = remember(
                booking.barberLat,
                booking.barberLon,
                booking.customerLat,
                booking.customerLon
            ) {
                if (liveDistanceKm != null) {
                    LocationHelper.compassDirection(
                        LocationHelper.bearingDegrees(
                            booking.barberLat,
                            booking.barberLon,
                            booking.customerLat,
                            booking.customerLon
                        )
                    )
                } else null
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("barber_job_detail_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Live Barber -> Customer GPS Route Map
                item {
                    LiveBarberTrackingMap(
                        booking = booking,
                        destinationAddressQuery = fullAddressText,
                        isBarberPerspective = true,
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }

                // Live GPS Telemetry Strip or Grant Permission Banner
                if (!hasLocationPerm && booking.status in setOf("CONFIRMED", "ON_THE_WAY")) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = BrassGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, BrassGold),
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Share Live Movement with Customer",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Allow GPS so the customer can see your scooter icon approaching live.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        locationPermLauncher.launch(LocationHelper.locationPermissionsArray())
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Enable GPS", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (booking.status in setOf("CONFIRMED", "ON_THE_WAY", "ARRIVED")) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SageVerified.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SageVerified.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .testTag("barber_gps_telemetry_strip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.GpsFixed,
                                        contentDescription = null,
                                        tint = SageVerified,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (liveDistanceKm != null) {
                                            "Doorstep Radar: ${LocationHelper.formatDistanceKm(liveDistanceKm)} ${liveCompass.orEmpty()} · ~${LocationHelper.estimateTravelMinutes(liveDistanceKm)}m ride"
                                        } else {
                                            "Live GPS Telemetry Active · Broadcasting movement"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SageVerified
                                    )
                                }
                                Text(
                                    text = if (gpsPacketsSent > 0) "$gpsPacketsSent pings" else "Live",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    VisitProgressStepper(
                        status = booking.status,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .staggeredSpringEntrance(index = 0)
                    )
                }

                // 2. Customer Demographics, Protected Door Address, Call/SMS & 1-Tap Arrival Templates
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${booking.customerName.ifBlank { "Customer" }} · ${booking.customerGender} (${booking.customerAge} yrs)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            if (booking.preferLadyBarber) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Face3, contentDescription = null, tint = BrassGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lady Hair Stylist Requested",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFFFDF9E),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = fullAddressText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFF8F3EC)
                            )
                            if (!protectedAddress?.accessInstructions.isNullOrBlank()) {
                                Text(
                                    text = "Arrival note: ${protectedAddress?.accessInstructions}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFFDF9E),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        val target = if (booking.customerLat != 0.0 && booking.customerLon != 0.0) {
                                            "${booking.customerLat},${booking.customerLon}"
                                        } else {
                                            fullAddressText
                                        }
                                        val geoUri = Uri.parse("google.navigation:q=${Uri.encode(target)}")
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri)) }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrassGold,
                                        contentColor = EspressoHeaderBg
                                    ),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(48.dp)
                                        .tactilePressPhysics(enableHaptics = true)
                                        .testTag("job_detail_navigate_button")
                                ) {
                                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Directions", fontWeight = FontWeight.Bold)
                                }
                                if (verifiedCustomerPhone.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$verifiedCustomerPhone")))
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, BrassGold),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFDF9E)),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .height(48.dp)
                                            .tactilePressPhysics(enableHaptics = true)
                                            .testTag("job_detail_call_customer_button")
                                    ) {
                                        Icon(Icons.Filled.Call, contentDescription = "Call Customer", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$verifiedCustomerPhone"))
                                            runCatching { context.startActivity(sms) }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, BrassGold),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFDF9E)),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .height(48.dp)
                                            .tactilePressPhysics(enableHaptics = true)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message Customer", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SMS")
                                    }
                                }
                            }

                            // 1-Tap Quick Arrival SMS Chips
                            if (verifiedCustomerPhone.isNotBlank() && booking.status in setOf("CONFIRMED", "ON_THE_WAY", "ARRIVED")) {
                                val quickSmsTemplates = listOf(
                                    "On my way (~10m)" to "Hi ${booking.customerName}, your DoorCut stylist is on the way and will reach in ~10 mins.",
                                    "At society gate" to "Hi ${booking.customerName}, I've reached your society/building gate for your DoorCut appointment.",
                                    "Need intercom/pin" to "Hi ${booking.customerName}, please approve security gate entry or share your flat intercom code."
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    quickSmsTemplates.forEachIndexed { idx, (chipTitle, body) ->
                                        AssistChip(
                                            onClick = {
                                                val smsIntent = Intent(
                                                    Intent.ACTION_SENDTO,
                                                    Uri.parse("smsto:$verifiedCustomerPhone")
                                                ).apply {
                                                    putExtra("sms_body", body)
                                                }
                                                runCatching { context.startActivity(smsIntent) }
                                            },
                                            label = {
                                                Text(
                                                    text = chipTitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFFFDF9E)
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = Color.White.copy(alpha = 0.10f)
                                            ),
                                            modifier = Modifier.testTag("quick_sms_chip_$idx")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Services & Direct Amount to Collect Card
                item {
                    val netTakeHomePaise = if (booking.barberNetEarningsPaise > 0L) {
                        booking.barberNetEarningsPaise
                    } else {
                        (booking.barberServicePayablePaise - booking.barberCommissionPaise).coerceAtLeast(0L)
                    }
                    LiquidGlassSurface(
                        strength = GlassMaterialStrength.AtelierCard,
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 2.dp,
                        interactivePhysics = false,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .staggeredSpringEntrance(index = 2)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Services & Direct Collection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${booking.serviceNamesSummary} · Estimated ${booking.durationMinutes} min",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            LiquidGlassSurface(
                                strength = GlassMaterialStrength.AccentEmerald,
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 1.dp,
                                interactivePhysics = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Collect directly from customer",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Cash or UPI · Your net take-home: ${CurrencyUtils.formatRupees(netTakeHomePaise)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    AnimatedSummaryText(
                                        text = CurrencyUtils.formatRupees(booking.barberServicePayablePaise),
                                        numericOrderKey = booking.barberServicePayablePaise,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 3B. Interactive 5-Point Home Salon Hygiene & Kit Sanitization Checklist
                item {
                    val completedChecks = hygieneChecked.count { it }
                    LiquidGlassSurface(
                        strength = GlassMaterialStrength.AtelierCard,
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 2.dp,
                        interactivePhysics = false,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .testTag("barber_hygiene_checklist_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CleaningServices,
                                        contentDescription = null,
                                        tint = SageVerified,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Home Salon Hygiene Protocol",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (completedChecks == hygieneLabels.size) {
                                        SageVerified
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                                ) {
                                    Text(
                                        text = if (completedChecks == hygieneLabels.size) {
                                            "5/5 Verified"
                                        } else {
                                            "$completedChecks/${hygieneLabels.size} Checked"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (completedChecks == hygieneLabels.size) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        },
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .testTag("hygiene_progress_badge")
                                    )
                                }
                            }

                            hygieneLabels.forEachIndexed { idx, itemText ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { hygieneChecked[idx] = !hygieneChecked[idx] }
                                        .padding(vertical = 2.dp)
                                        .testTag("hygiene_check_$idx"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = hygieneChecked[idx],
                                        onCheckedChange = { hygieneChecked[idx] = it }
                                    )
                                    Text(
                                        text = itemText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Guided Service Execution Sequence
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
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Guided Trip & Service Flow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            AnimatedContent(
                                targetState = booking.status,
                                transitionSpec = {
                                    DoorCutMotionSpecs.directionalStepTransform(
                                        forward = true,
                                        reducedMotion = reducedMotion
                                    )
                                },
                                label = "job_workflow_stage_transition"
                            ) { stage ->
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    when (stage) {
                                        "CONFIRMED" -> {
                                            Button(
                                                onClick = { onAdvanceTripStatus(booking.id, "ON_THE_WAY") },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = EspressoHeaderBg,
                                                    contentColor = Color(0xFFFFDF9E)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .tactilePressPhysics(enableHaptics = true)
                                                    .testTag("step_on_my_way_button")
                                            ) {
                                                Text("Start Ride & Broadcast Live GPS (On My Way)", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        "ON_THE_WAY" -> {
                                            Button(
                                                onClick = { onAdvanceTripStatus(booking.id, "ARRIVED") },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = EspressoHeaderBg,
                                                    contentColor = Color(0xFFFFDF9E)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .tactilePressPhysics(enableHaptics = true)
                                                    .testTag("step_arrived_button")
                                            ) {
                                                Text("1. I've Arrived at Customer's Home", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        "ARRIVED" -> {
                                            Button(
                                                onClick = { onAdvanceTripStatus(booking.id, "IN_PROGRESS") },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = EspressoHeaderBg,
                                                    contentColor = Color(0xFFFFDF9E)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .tactilePressPhysics(enableHaptics = true)
                                                    .testTag("step_start_service_button")
                                            ) {
                                                Text("2. Start Grooming Session", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        "IN_PROGRESS" -> {
                                            OutlinedButton(
                                                onClick = { showAddExtraPicker = !showAddExtraPicker },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .tactilePressPhysics(enableHaptics = true)
                                                    .testTag("request_extra_service_toggle")
                                            ) {
                                                Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Request Customer Approval for Extra Service")
                                            }

                                            if (showAddExtraPicker) {
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(catalogServices.take(6), key = { it.id }) { svc ->
                                                        LiquidGlassPill(
                                                            text = "${svc.name} (+${CurrencyUtils.formatRupees(svc.pricePaise)})",
                                                            selected = false,
                                                            onClick = {
                                                                onRequestExtraService(booking.id, svc)
                                                                showAddExtraPicker = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            Button(
                                                onClick = { onFinishService(booking.id) },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = EspressoHeaderBg,
                                                    contentColor = Color(0xFFFFDF9E)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                                    .tactilePressPhysics(enableHaptics = true)
                                                    .testTag("step_finish_service_button")
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Finish Service", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        "COMPLETED", "DISPUTED" -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SageVerified)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Service completion recorded! Show UPI QR pass or record payment below.",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SageVerified
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4B. Direct UPI QR Payment Pass Card (shown during IN_PROGRESS / COMPLETED / DISPUTED)
                if (booking.status in setOf("IN_PROGRESS", "COMPLETED", "DISPUTED")) {
                    item {
                        val effectiveUpi = barberUpiId.trim().ifBlank { "doorcut.partner@upi" }
                        val rupeesVal = (booking.barberServicePayablePaise / 100L).coerceAtLeast(1L)
                        val upiDeepLink = remember(effectiveUpi, booking.barberName, rupeesVal, booking.bookingNumber) {
                            "upi://pay?pa=${Uri.encode(effectiveUpi)}&pn=${Uri.encode(booking.barberName.ifBlank { "DoorCut Stylist" })}&am=$rupeesVal&cu=INR&tn=${Uri.encode("DoorCut ${booking.bookingNumber}")}"
                        }

                        LiquidGlassSurface(
                            strength = GlassMaterialStrength.AtelierCard,
                            shape = RoundedCornerShape(22.dp),
                            shadowElevation = 2.dp,
                            interactivePhysics = false,
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .testTag("barber_upi_payment_pass_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Deterministic QR Matrix Visual for UPI Payload
                                DeterministicUpiQrMatrix(
                                    payload = upiDeepLink,
                                    modifier = Modifier.size(104.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.QrCode2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Direct UPI Payment Pass",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${CurrencyUtils.formatRupees(booking.barberServicePayablePaise)} → $effectiveUpi",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (copiedUpiToast) {
                                            "✓ UPI ID copied to clipboard!"
                                        } else {
                                            "Customer pays 100% directly to your bank via GPay / PhonePe / Paytm."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (copiedUpiToast) SageVerified else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("Barber UPI ID", effectiveUpi))
                                            copiedUpiToast = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("copy_barber_upi_button")
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Copy UPI ID", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Record Direct Payment Method & Amount
                if (booking.status in setOf("COMPLETED", "DISPUTED")) {
                    item {
                        LiquidGlassSurface(
                            strength = GlassMaterialStrength.AtelierCard,
                            shape = RoundedCornerShape(22.dp),
                            accentBorderColor = BrassGold,
                            shadowElevation = 3.dp,
                            interactivePhysics = false,
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .staggeredSpringEntrance(index = 4)
                                .testTag("record_direct_payment_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Record Direct Payment (Separate from Completion)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Current status: ${booking.directPaymentSummaryLabel()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(
                                        listOf(
                                            "CASH" to "Cash",
                                            "UPI" to "UPI",
                                            "PARTIAL" to "Partial Paid",
                                            "UNPAID" to "Unconfirmed",
                                            "DISPUTED" to "Disputed"
                                        ),
                                        key = { it.first }
                                    ) { (key, label) ->
                                        LiquidGlassPill(
                                            text = label,
                                            selected = selectedPaymentOutcome == key,
                                            onClick = { selectedPaymentOutcome = key }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = amountRupeesInput,
                                    onValueChange = { amountRupeesInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("Direct Amount Received (₹)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = paymentNoteInput,
                                    onValueChange = { paymentNoteInput = it },
                                    label = { Text("Optional UPI Ref / Note") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        val paise = (amountRupeesInput.toLongOrNull() ?: 0L) * 100L
                                        val (declaredStatus, method) = when (selectedPaymentOutcome) {
                                            "CASH" -> "CASH_DECLARED" to "CASH"
                                            "UPI" -> "UPI_DECLARED" to "UPI"
                                            "PARTIAL" -> "PARTIAL_DECLARED" to "CASH"
                                            "UNPAID" -> "UNPAID_DECLARED" to "NONE"
                                            else -> "DISPUTED" to "NONE"
                                        }
                                        onRecordDirectPayment(
                                            booking.id,
                                            declaredStatus,
                                            method,
                                            paise,
                                            paymentNoteInput.trim()
                                        )
                                    },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EspressoHeaderBg,
                                        contentColor = Color(0xFFFFDF9E)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .tactilePressPhysics(enableHaptics = true)
                                        .testTag("submit_direct_payment_button")
                                ) {
                                    Text("Save Payment Record", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeterministicUpiQrMatrix(
    payload: String,
    modifier: Modifier = Modifier
) {
    val gridSize = 21
    val hashSeed = remember(payload) { payload.hashCode() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellW = size.width / gridSize
            val cellH = size.height / gridSize

            fun isFinderModule(r: Int, c: Int, topR: Int, topC: Int): Boolean {
                val lr = r - topR
                val lc = c - topC
                if (lr !in 0..6 || lc !in 0..6) return false
                val outer = lr == 0 || lr == 6 || lc == 0 || lc == 6
                val inner = lr in 2..4 && lc in 2..4
                return outer || inner
            }

            fun inFinderZone(r: Int, c: Int): Boolean {
                return (r <= 7 && c <= 7) ||
                    (r <= 7 && c >= gridSize - 8) ||
                    (r >= gridSize - 8 && c <= 7)
            }

            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val dark = if (inFinderZone(r, c)) {
                        isFinderModule(r, c, 0, 0) ||
                            isFinderModule(r, c, 0, gridSize - 7) ||
                            isFinderModule(r, c, gridSize - 7, 0)
                    } else {
                        val charCode = payload[(r * gridSize + c) % payload.length].code
                        val mix = (hashSeed xor (r * 37 + c * 91 + charCode * 13))
                        (mix and 1) == 0
                    }

                    if (dark) {
                        drawRect(
                            color = Color(0xFF14110E),
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW * 0.96f, cellH * 0.96f)
                        )
                    }
                }
            }
        }
    }
}
