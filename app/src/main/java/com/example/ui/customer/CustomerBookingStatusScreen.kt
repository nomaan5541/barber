package com.example.ui.customer

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BarberProfile
import com.example.data.model.Booking
import com.example.data.model.BookingPrivateAddress
import com.example.data.model.CurrencyUtils
import com.example.data.model.PlatformConfig
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SixStageBookingTimeline
import com.example.ui.components.StatusBadge
import com.example.ui.components.TwoPaymentSplitCard
import com.example.ui.glass.AmbientAtelierBackdrop
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.glass.LiveGlassPulseDot
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified

@Composable
fun CustomerBookingStatusScreen(
    booking: Booking?,
    barberProfile: BarberProfile?,
    protectedAddress: BookingPrivateAddress?,
    platformConfig: PlatformConfig,
    isBusy: Boolean,
    onLoadProtectedAddress: (String) -> Unit,
    onApproveExtraService: (Booking, Boolean) -> Unit,
    onConfirmDirectPayment: (String, Boolean, String) -> Unit,
    onCancelBooking: (Booking, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("Plans changed") }
    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeNote by remember { mutableStateOf("") }

    LaunchedEffect(booking?.id) {
        if (booking != null && booking.id.isNotBlank()) {
            onLoadProtectedAddress(booking.id)
        }
    }

    AmbientAtelierBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("customer_booking_status_screen")
        ) {
            // Top Header Bar
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
                            .testTag("booking_status_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Booking Status",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (booking != null) {
                            Text(
                                text = "${booking.bookingNumber.ifBlank { "DoorCut Visit" }} · ${booking.dateKey} at ${CurrencyUtils.formatHourSlot(booking.startHour)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (booking != null) {
                        StatusBadge(booking.status)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

        if (booking == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Filled.Schedule,
                    title = "Booking Not Found",
                    subtitle = "Return to Bookings to view your upcoming and past home barber appointments.",
                    action = {
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Back to Bookings", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
            return@Column
        }

        val etaLabel = when (booking.status) {
            "AWAITING_BARBER" -> "Confirming within ~5 mins"
            "CONFIRMED" -> "Scheduled for ${CurrencyUtils.formatHourSlot(booking.startHour)}"
            "ON_THE_WAY" -> "Arriving in ~15 mins"
            "ARRIVED" -> "Outside your door now"
            "IN_PROGRESS" -> "Service underway (~${booking.durationMinutes} min)"
            "COMPLETED" -> "Completed"
            else -> booking.friendlyStatusText()
        }
        val ratingValue = barberProfile?.ratingAvg ?: 4.9
        val canContactBarber = booking.status in setOf("CONFIRMED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS")
        val canCancel = booking.status in setOf("SLOT_HELD", "PAYMENT_PENDING", "AWAITING_BARBER", "CONFIRMED")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Barber Card + ETA + Call/Message
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.FloatingPill,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 5.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .testTag("booking_status_barber_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BarberAvatarBadge(
                                name = booking.barberName.ifBlank { "Assigned Barber" },
                                isOnline = true,
                                size = 54.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = booking.barberName.ifBlank { "Assigned Barber" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = BrassGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${String.format("%.1f", ratingValue)} · Verified Home Barber",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFFDF9E)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LiveGlassPulseDot(
                                        statusKey = booking.status,
                                        color = SageVerified,
                                        size = 8.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = etaLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF8F3EC)
                                    )
                                }
                            }
                        }

                        if (canContactBarber) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))
                                        runCatching { context.startActivity(dialIntent) }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrassGold,
                                        contentColor = EspressoHeaderBg
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .tactilePressPhysics(enableHaptics = true)
                                        .testTag("call_barber_button")
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = "Call Barber", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call Barber", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:+919876543210")).apply {
                                            putExtra("sms_body", "Hi ${booking.barberName}, regarding my DoorCut booking (${booking.bookingNumber}).")
                                        }
                                        runCatching { context.startActivity(smsIntent) }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFDF9E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .tactilePressPhysics()
                                        .testTag("message_barber_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message Barber", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Message", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Six-Stage Live Timeline
            item {
                SixStageBookingTimeline(
                    status = booking.status,
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            // 3. Extra Service Approval Prompt (if requested by barber during service)
            if (booking.pendingAddOnStatus == "REQUESTED" && booking.pendingAddOnName.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, BrassGold),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .testTag("extra_service_approval_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Approve Extra Service?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${booking.barberName} requested to add ${booking.pendingAddOnName} (+${CurrencyUtils.formatRupees(booking.pendingAddOnPricePaise)} · ${booking.pendingAddOnDurationMin} min) paid directly after service.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onApproveExtraService(booking, true) },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("approve_extra_service_button")
                                ) {
                                    Text("Approve +${CurrencyUtils.formatRupees(booking.pendingAddOnPricePaise)}", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onApproveExtraService(booking, false) },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("No Thanks")
                                }
                            }
                        }
                    }
                }
            }

            // 4. Direct Payment Confirmation Prompt (when service completed & barber declared payment)
            if (booking.status == "COMPLETED" &&
                booking.directPaymentStatus in setOf("CASH_DECLARED", "UPI_DECLARED", "PARTIAL_DECLARED")
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SageVerified.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SageVerified.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .testTag("confirm_direct_payment_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = SageVerified)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Confirm Direct Payment to Barber",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${booking.barberName} recorded ${CurrencyUtils.formatRupees(booking.directPaymentDeclaredPaise)} via ${booking.directPaymentMethod}. Did you pay this directly?",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onConfirmDirectPayment(booking.id, true, "") },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("confirm_paid_barber_button")
                                ) {
                                    Text("Yes, Paid", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showDisputeDialog = true },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Report Issue")
                                }
                            }
                        }
                    }
                }
            }

            // 5. Two-Payment Breakdown Card
            item {
                TwoPaymentSplitCard(
                    serviceSummaryLabel = "${booking.serviceNamesSummary} (${booking.durationMinutes} min)",
                    serviceSubtotalPaise = booking.serviceSubtotalPaise,
                    approvedAddOnPaise = booking.approvedAddOnPaise,
                    travelChargePaise = booking.travelChargePaise,
                    barberServicePayablePaise = booking.barberServicePayablePaise,
                    bookingConfirmationFeePaise = if (booking.platformBookingFeePaise > 0L) {
                        booking.platformBookingFeePaise
                    } else {
                        platformConfig.effectiveCustomerBookingFeePaise(booking.serviceSubtotalPaise)
                    },
                    isBookingFeeAlreadyPaid = true,
                    directPaymentNote = booking.directPaymentSummaryLabel(),
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            // 6. Home Address & Cancellation Policy Card
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Visit Address & Cancellation Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = protectedAddress?.fullAddressSnapshot?.ifBlank { booking.areaName }
                                ?: booking.areaName.ifBlank { "Home Address" },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (!protectedAddress?.accessInstructions.isNullOrBlank()) {
                            Text(
                                text = "Note for barber: ${protectedAddress?.accessInstructions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = booking.policySnapshot.ifBlank { platformConfig.policyText },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (canCancel) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .tactilePressPhysics()
                                    .testTag("cancel_booking_button")
                            ) {
                                Icon(Icons.Filled.EventBusy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel Booking", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showCancelDialog && booking != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Appointment?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Free cancellation up to ${platformConfig.freeCancellationCutoffHours} hours before your slot.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelBooking(booking, cancelReason.ifBlank { "Customer cancelled" })
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Keep Slot")
                }
            }
        )
    }

    if (showDisputeDialog && booking != null) {
        AlertDialog(
            onDismissRequest = { showDisputeDialog = false },
            title = { Text("Report Direct Payment Issue") },
            text = {
                OutlinedTextField(
                    value = disputeNote,
                    onValueChange = { disputeNote = it },
                    label = { Text("What went wrong with the payment?") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmDirectPayment(booking.id, false, disputeNote.ifBlank { "Amount mismatch" })
                        showDisputeDialog = false
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Submit to Support")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisputeDialog = false }, modifier = Modifier.height(48.dp)) {
                    Text("Cancel")
                }
            }
        )
    }
}
