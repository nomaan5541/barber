package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Booking
import com.example.data.model.BookingPrivateAddress
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.ServiceArea
import com.example.data.model.SupportTicket
import com.example.data.model.UserAccount
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.CustomerDirectPriceCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.VisitProgressStepper
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.viewmodel.MarketplaceViewModel

@Composable
fun CustomerBookingsScreen(
    viewModel: MarketplaceViewModel,
    userAccount: UserAccount,
    bookings: List<Booking>,
    protectedAddresses: Map<String, BookingPrivateAddress>,
    isBusy: Boolean,
    onRepeatBookingToDiscover: () -> Unit
) {
    var filterTab by remember { mutableStateOf("ACTIVE") }

    val sorted = remember(bookings, filterTab) {
        val activeStatuses = setOf(
            "SLOT_HELD", "PAYMENT_PENDING", "AWAITING_BARBER", "CONFIRMED",
            "ON_THE_WAY", "ARRIVED", "IN_PROGRESS", "DISPUTED"
        )
        val filtered = when (filterTab) {
            "ACTIVE" -> bookings.filter { activeStatuses.contains(it.status) || (it.status == "COMPLETED" && it.directPaymentStatus != "CUSTOMER_CONFIRMED") }
            else -> bookings.filter { !activeStatuses.contains(it.status) }
        }
        filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "LIVE VISIT TRACKER & RECEIPTS",
                title = "My Home Visits",
                subtitle = "Track master barber arrival, approve add-ons, and verify direct UPI/Cash settlement."
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Segmented Filter Switcher
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SegmentTabButton(
                        title = "Active & Settlement",
                        selected = filterTab == "ACTIVE",
                        onClick = { filterTab = "ACTIVE" },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentTabButton(
                        title = "Past Receipts",
                        selected = filterTab == "HISTORY",
                        onClick = { filterTab = "HISTORY" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (sorted.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.EventNote,
                    title = "No Bookings Here Yet",
                    subtitle = "Book any home grooming service with ₹0 platform fee, track barber arrival, and verify direct UPI/cash payment here."
                )
            }
        } else {
            items(sorted, key = { it.id }) { booking ->
                LaunchedEffect(booking.id) {
                    viewModel.loadProtectedAddress(booking.id)
                }
                CustomerBookingCard(
                    booking = booking,
                    privateAddress = protectedAddresses[booking.id],
                    customerName = userAccount.name,
                    isBusy = isBusy,
                    onApproveExtra = { approve -> viewModel.customerRespondExtraService(booking, approve) },
                    onVerifyPayment = { confirm, reason -> viewModel.customerVerifyDirectPayment(booking.id, confirm, reason) },
                    onCancel = { reason -> viewModel.customerCancelBooking(booking, reason) },
                    onSubmitReview = { rating, comment -> viewModel.submitReview(booking, userAccount.name, rating, comment) },
                    onRepeat = {
                        viewModel.repeatBooking(booking)
                        onRepeatBookingToDiscover()
                    }
                )
            }
        }
    }
}

@Composable
private fun SegmentTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) EspressoHeaderBg else Color.Transparent,
        contentColor = if (selected) Color(0xFFFFDF9E) else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CustomerBookingCard(
    booking: Booking,
    privateAddress: BookingPrivateAddress?,
    customerName: String,
    isBusy: Boolean,
    onApproveExtra: (Boolean) -> Unit,
    onVerifyPayment: (Boolean, String) -> Unit,
    onCancel: (String) -> Unit,
    onSubmitReview: (Long, String) -> Unit,
    onRepeat: () -> Unit
) {
    var cancelReason by remember { mutableStateOf("") }
    var showCancelBox by remember { mutableStateOf(false) }
    var disputeNote by remember { mutableStateOf("") }
    var showDisputeBox by remember { mutableStateOf(false) }
    var reviewStars by remember { mutableLongStateOf(5L) }
    var reviewComment by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_booking_card_${booking.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.bookingNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${booking.dateKey} • ${CurrencyUtils.formatHourSlot(booking.startHour)} (${booking.durationMinutes} min)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StatusBadge(booking.status)
            }

            // Live 5-Step Visit Progress Stepper
            VisitProgressStepper(status = booking.status)

            // Specialist & Services Strip
            Row(verticalAlignment = Alignment.CenterVertically) {
                BarberAvatarBadge(name = booking.barberName, isOnline = true, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.barberName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ContentCut,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = booking.serviceNamesSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (privateAddress != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = privateAddress.fullAddressSnapshot,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            CustomerDirectPriceCard(
                serviceSubtotalPaise = booking.serviceSubtotalPaise,
                approvedAddOnPaise = booking.approvedAddOnPaise,
                travelChargePaise = booking.travelChargePaise,
                barberServicePayablePaise = booking.barberServicePayablePaise,
                directPaymentStatusLabel = "Settlement: ${booking.directPaymentStatus.replace("_", " ")}"
            )

            // Extra Service Change Approval Gate
            if (booking.pendingAddOnStatus == "REQUESTED") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Action Required: Barber Requested Extra Service",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "+ ${booking.pendingAddOnName} (${CurrencyUtils.formatRupees(booking.pendingAddOnPricePaise)}, +${booking.pendingAddOnDurationMin}m)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onApproveExtra(true) },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Approve Add-On")
                            }
                            OutlinedButton(
                                onClick = { onApproveExtra(false) },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Decline")
                            }
                        }
                    }
                }
            }

            // Direct Payment Verification Prompt
            val barberDeclared = booking.directPaymentStatus in listOf(
                "CASH_DECLARED", "UPI_DECLARED", "PARTIAL_DECLARED", "UNPAID_DECLARED"
            )
            if (barberDeclared) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Verify Direct Payment to Barber",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Barber declared: ${booking.directPaymentMethod} • ${CurrencyUtils.formatRupees(booking.directPaymentDeclaredPaise)} (${booking.directPaymentStatus})" +
                                if (booking.directPaymentRef.isNotBlank()) " • Ref: ${booking.directPaymentRef}" else "",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onVerifyPayment(true, "") },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Confirm Paid")
                            }
                            OutlinedButton(
                                onClick = { showDisputeBox = !showDisputeBox },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Raise Dispute")
                            }
                        }
                        if (showDisputeBox) {
                            OutlinedTextField(
                                value = disputeNote,
                                onValueChange = { disputeNote = it },
                                label = { Text("Describe payment/price disagreement") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    onVerifyPayment(false, disputeNote)
                                    showDisputeBox = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Escalate Dispute to Admin")
                            }
                        }
                    }
                }
            }

            // Free Cancellation
            if (booking.status in listOf("SLOT_HELD", "PAYMENT_PENDING", "AWAITING_BARBER", "CONFIRMED")) {
                if (!showCancelBox) {
                    OutlinedButton(
                        onClick = { showCancelBox = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Cancel Appointment (₹0 Fee)")
                    }
                } else {
                    Text(
                        text = "Policy: ${booking.policySnapshot}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason for cancellation") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onCancel(cancelReason)
                                showCancelBox = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Confirm Cancel")
                        }
                        OutlinedButton(
                            onClick = { showCancelBox = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Keep Booking")
                        }
                    }
                }
            }

            // Post-Completion Star Rating & Review + Repeat Booking
            if (booking.status == "COMPLETED") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (!booking.hasReview) {
                    Text(
                        text = "Rate ${booking.barberName}'s Craftsmanship",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1L..5L).forEach { star ->
                            val activeStar = star <= reviewStars
                            Surface(
                                onClick = { reviewStars = star },
                                shape = RoundedCornerShape(12.dp),
                                color = if (activeStar) EspressoHeaderBg else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(
                                    1.dp,
                                    if (activeStar) BrassGold else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$star",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (activeStar) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "$star stars",
                                        tint = if (activeStar) BrassGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("Share feedback on haircut perfection & punctuality") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onSubmitReview(reviewStars, reviewComment) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Submit Verified Review", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "✓ Verified review submitted by $customerName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                OutlinedButton(
                    onClick = onRepeat,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Book Same Services Again", fontWeight = FontWeight.SemiBold)
                }
            }

            if (booking.cancellationReason.isNotBlank()) {
                Text(
                    text = "Record Note: ${booking.cancellationReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AccountAndSupportScreen(
    viewModel: MarketplaceViewModel,
    userAccount: UserAccount,
    tickets: List<SupportTicket>,
    allServices: List<CatalogService>,
    serviceAreas: List<ServiceArea>,
    isBusy: Boolean,
    onSignOut: () -> Unit
) {
    var name by remember(userAccount.name) { mutableStateOf(userAccount.name) }
    var phone by remember(userAccount.phone) { mutableStateOf(userAccount.phone) }

    var ticketCategory by remember { mutableStateOf("PAYMENT_DISPUTE") }
    var ticketBookingRef by remember { mutableStateOf("") }
    var ticketSubject by remember { mutableStateOf("") }
    var ticketDesc by remember { mutableStateOf("") }

    var showBarberApply by remember { mutableStateOf(false) }
    var barberShopName by remember { mutableStateOf("") }
    var barberShopAddress by remember { mutableStateOf("") }
    var barberSkillsText by remember { mutableStateOf("Zero Skin Fade, Razor Beard Sculpt, Scissor Cut") }
    var barberBio by remember { mutableStateOf("Master barber offering salon-grade home visits with sanitized tools.") }
    var barberExp by remember { mutableStateOf("5") }
    var barberUpi by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Editorial Member Crest Banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EspressoHeaderBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        BarberAvatarBadge(name = userAccount.name, isOnline = true, size = 52.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = userAccount.name.ifBlank { "DoorCut Member" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = userAccount.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6C8B6)
                            )
                        }
                    }
                    StatusBadge(userAccount.role)
                }
            }
        }

        // Personal Contact Details
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Personal Contact Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Number (Shared only on confirmed visit)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.updateProfile(name, phone) },
                        enabled = !isBusy && name.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Partner Barber & Shop Registration
        if (userAccount.role == "CUSTOMER") {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Own a Barber Shop or Do Home Visits?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Customers pay ₹0 platform fee so you get more home bookings! DoorCut charges a transparent partner commission on completed bookings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!showBarberApply) {
                            Button(
                                onClick = { showBarberApply = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Register as Partner Barber & Shop", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedTextField(
                                value = barberShopName,
                                onValueChange = { barberShopName = it },
                                label = { Text("Barber Shop / Studio Name") },
                                placeholder = { Text("e.g., Royal Blade Men's Salon") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = barberShopAddress,
                                onValueChange = { barberShopAddress = it },
                                label = { Text("Shop Street Address (for walk-in & distance)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = barberSkillsText,
                                onValueChange = { barberSkillsText = it },
                                label = { Text("Haircut & Grooming Skills (comma separated)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = barberBio,
                                onValueChange = { barberBio = it },
                                label = { Text("Short Bio & Perfection Specialties") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = barberExp,
                                    onValueChange = { barberExp = it.filter { c -> c.isDigit() } },
                                    label = { Text("Years Exp") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.width(120.dp)
                                )
                                OutlinedTextField(
                                    value = barberUpi,
                                    onValueChange = { barberUpi = it },
                                    label = { Text("Direct Payment UPI ID") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.applyAsBarber(
                                        name = name,
                                        phone = phone,
                                        bio = barberBio,
                                        experienceYears = barberExp.toLongOrNull() ?: 3L,
                                        upiId = barberUpi,
                                        shopName = barberShopName.ifBlank { "${name.substringBefore(" ")}'s Barber Studio" },
                                        shopAddress = barberShopAddress.ifBlank { "Local Partner Hub" },
                                        skillsText = barberSkillsText,
                                        serviceIds = allServices.map { it.id },
                                        serviceAreaIds = serviceAreas.map { it.id }
                                    )
                                    showBarberApply = false
                                },
                                enabled = !isBusy && name.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Submit Barber & Shop for Approval", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Support & Dispute Tickets (Scrollable LazyRow chips so labels never wrap/squish!)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Concierge Support & Disputes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val ticketCategories = listOf(
                        "PAYMENT_DISPUTE" to "Payment Dispute",
                        "LATE_OR_NO_SHOW" to "Late / No-Show",
                        "SAFETY_CONCERN" to "Safety Concern",
                        "GENERAL" to "General Help"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ticketCategories) { (catCode, catLabel) ->
                            FilterChip(
                                selected = ticketCategory == catCode,
                                onClick = { ticketCategory = catCode },
                                label = { Text(catLabel) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = ticketBookingRef,
                        onValueChange = { ticketBookingRef = it },
                        label = { Text("Booking Reference (Optional, e.g. DC-20261026-1042)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ticketSubject,
                        onValueChange = { ticketSubject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ticketDesc,
                        onValueChange = { ticketDesc = it },
                        label = { Text("Details") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val cRole = if (userAccount.role == "BARBER") "BARBER" else "CUSTOMER"
                            viewModel.createSupportTicket(cRole, ticketBookingRef, ticketCategory, ticketSubject, ticketDesc)
                            ticketSubject = ""
                            ticketDesc = ""
                        },
                        enabled = !isBusy && ticketSubject.isNotBlank() && ticketDesc.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Support Ticket", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (tickets.isNotEmpty()) {
            item {
                Text(
                    text = "My Support Tickets (${tickets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(tickets, key = { it.id }) { t ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            StatusBadge(t.status)
                        }
                        Text(
                            text = "${t.category.replace("_", " ")} • Ref: ${t.bookingId.ifBlank { "General" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(t.description, style = MaterialTheme.typography.bodySmall)
                        if (t.adminResolution.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Admin Resolution: ${t.adminResolution}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of DoorCut", fontWeight = FontWeight.Bold)
            }
        }
    }
}
