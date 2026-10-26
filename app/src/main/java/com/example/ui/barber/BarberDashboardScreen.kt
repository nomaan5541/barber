package com.example.ui.barber

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.ui.viewmodel.MarketplaceViewModel
import java.util.Calendar

@Composable
fun BarberDashboardScreen(
    barberProfile: BarberProfile?,
    defaultDisplayName: String,
    bookings: List<Booking>,
    catalogServices: List<CatalogService>,
    serviceAreas: List<ServiceArea>,
    isBusy: Boolean,
    onToggleOnline: (Boolean) -> Unit,
    onAcceptOrDeclineRequest: (Booking, Boolean, String) -> Unit,
    onAdvanceTripStatus: (String, String) -> Unit,
    onOpenBookingDetail: (String) -> Unit,
    onApplyAsBarber: (String, String, String, Long, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val todayKey = remember { MarketplaceViewModel.defaultTodayDateKey() }
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toLong() }

    // Filter out expired requests from active queue
    val activeNewRequests = remember(bookings, todayKey, currentHour) {
        bookings.filter { b ->
            b.status == "AWAITING_BARBER" &&
                (b.dateKey > todayKey || (b.dateKey == todayKey && b.startHour >= currentHour))
        }.sortedWith(compareBy({ it.dateKey }, { it.startHour }))
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

    var applyName by remember(defaultDisplayName) { mutableStateOf(defaultDisplayName) }
    var applyPhone by remember { mutableStateOf("") }
    var applyShop by remember { mutableStateOf("Royal Blade Home Studio") }
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
        // 1. Online / Offline Availability Switch Card
        item {
            val isOnline = barberProfile?.isOnline ?: true
            LiquidGlassSurface(
                strength = GlassMaterialStrength.FloatingPill,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 4.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = barberProfile?.name ?: defaultDisplayName.ifBlank { "Barber Dashboard" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LiveGlassPulseDot(
                                statusKey = isOnline,
                                color = if (isOnline) SageVerified else Color(0xFF9E9183),
                                size = 8.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "Online · Ready for home appointments" else "Offline · Paused new requests",
                                style = MaterialTheme.typography.bodyMedium,
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
            }
        }

        // Optional Onboarding Card if signed-in user hasn't registered their barber profile yet
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
                            text = "Activate Your Barber Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Complete your quick registration so customers in your area can book you.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = applyName,
                            onValueChange = { applyName = it },
                            label = { Text("Barber Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = applyPhone,
                                onValueChange = { applyPhone = it },
                                label = { Text("Phone Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = applyExp,
                                onValueChange = { applyExp = it.filter { c -> c.isDigit() } },
                                label = { Text("Years Exp.") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = applyUpi,
                            onValueChange = { applyUpi = it },
                            label = { Text("Direct UPI ID (for customer payments)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onApplyAsBarber(
                                    applyName.ifBlank { "Master Barber" },
                                    applyPhone.ifBlank { "+919876543210" },
                                    "Verified home grooming specialist with sanitized kit.",
                                    applyExp.toLongOrNull() ?: 5L,
                                    applyUpi.ifBlank { "barber@upi" },
                                    applyShop,
                                    applyArea,
                                    "Haircut, Beard Trim, Haircut + Beard"
                                )
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_barber_application_button")
                        ) {
                            Text("Submit Barber Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Today's Work Summary Metrics
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                MetricBentoCard(
                    icon = Icons.Filled.Schedule,
                    title = "Today's Bookings",
                    value = "${todaysBookings.size}",
                    subtitle = "${completedToday.size} completed today",
                    modifier = Modifier.weight(1f)
                )
                MetricBentoCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Today's Direct Pay",
                    value = CurrencyUtils.formatRupees(todayDirectEarningsPaise),
                    subtitle = "Paid directly by customers",
                    accentHighlight = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Next Appointment Immediate Action Card
        if (nextAppointment != null) {
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AccentEmerald,
                    shape = RoundedCornerShape(22.dp),
                    accentBorderColor = SageVerified,
                    shadowElevation = 3.dp,
                    interactivePhysics = false,
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
                                    text = "NEXT APPOINTMENT",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SageVerified
                                )
                            }
                            StatusBadge(nextAppointment.status)
                        }
                        Text(
                            text = "${CurrencyUtils.formatHourSlot(nextAppointment.startHour)} · ${nextAppointment.customerName}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${nextAppointment.serviceNamesSummary} (${nextAppointment.durationMinutes} min) · Collect ${CurrencyUtils.formatRupees(nextAppointment.barberServicePayablePaise)} directly",
                            style = MaterialTheme.typography.bodyLarge,
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
                                Text("View Booking", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(nextAppointment.areaName.ifBlank { "Hyderabad" })}")
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
                                Text("Start Navigation", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (nextAppointment.status == "CONFIRMED") {
                            Button(
                                onClick = { onAdvanceTripStatus(nextAppointment.id, "ON_THE_WAY") },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .tactilePressPhysics(enableHaptics = true)
                            ) {
                                Text("On My Way", fontWeight = FontWeight.Bold)
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
                                Text("I've Arrived", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. New Booking Requests Queue
        item {
            SectionHeader(
                eyebrow = "ACTION REQUIRED",
                title = "New Booking Requests (${activeNewRequests.size})",
                subtitle = "Full house address unlocks automatically after you accept the booking.",
                modifier = Modifier.widthIn(max = 600.dp)
            )
        }

        if (activeNewRequests.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.NotificationsActive,
                    title = "No New Requests Waiting",
                    subtitle = "Stay Online to receive instant home haircut requests nearby."
                )
            }
        } else {
            items(activeNewRequests, key = { it.id }) { req ->
                NewBookingRequestCard(
                    booking = req,
                    isBusy = isBusy,
                    onAccept = { onAcceptOrDeclineRequest(req, true, "") },
                    onDecline = { onAcceptOrDeclineRequest(req, false, "Barber unavailable for slot") }
                )
            }
        }
    }
}

@Composable
private fun NewBookingRequestCard(
    booking: Booking,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    LiquidGlassSurface(
        strength = GlassMaterialStrength.AtelierCard,
        shape = RoundedCornerShape(22.dp),
        accentBorderColor = BrassGold,
        shadowElevation = 3.dp,
        interactivePhysics = false,
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
                Text(
                    text = "${booking.dateKey} · ${CurrencyUtils.formatHourSlot(booking.startHour)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = EspressoHeaderBg
                ) {
                    Text(
                        text = "Direct Pay: ${CurrencyUtils.formatRupees(booking.barberServicePayablePaise)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFDF9E),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${booking.serviceNamesSummary} · ${booking.durationMinutes} min",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Area: ${booking.areaName.ifBlank { "Local Zone (~2.5 km)" }} · Exact door hidden until accepted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EspressoHeaderBg,
                        contentColor = Color(0xFFFFDF9E)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .tactilePressPhysics(enableHaptics = true)
                        .testTag("accept_request_button_${booking.id}")
                ) {
                    Text("Accept Request", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
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
