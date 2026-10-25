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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BarberProfile
import com.example.data.model.Booking
import com.example.data.model.BookingPrivateAddress
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.BarberCommissionBreakdownCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricBentoCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.VisitProgressStepper
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.viewmodel.MarketplaceViewModel

@Composable
fun BarberJobsConsoleScreen(
    viewModel: MarketplaceViewModel,
    barberProfile: BarberProfile?,
    bookings: List<Booking>,
    catalogServices: List<CatalogService>,
    protectedAddresses: Map<String, BookingPrivateAddress>,
    isBusy: Boolean
) {
    if (barberProfile == null || barberProfile.approvalStatus != "APPROVED") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.LockClock,
                    title = "Barber Status: ${barberProfile?.approvalStatus ?: "PENDING"}",
                    subtitle = "Per platform security rules, a barber cannot receive or execute customer bookings until an Administrator approves and activates your barber profile."
                )
            }
        }
        return
    }

    val pendingRequests = bookings.filter { it.status == "AWAITING_BARBER" }
    val activeJobs = bookings.filter { it.status in listOf("CONFIRMED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Studio Console Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EspressoHeaderBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(EspressoHeaderBg, Color(0xFF2D231A))
                            )
                        )
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        BarberAvatarBadge(
                            name = barberProfile.name,
                            isOnline = barberProfile.isOnline,
                            size = 50.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = barberProfile.effectiveShopName(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${barberProfile.name} • ${pendingRequests.size} New • ${activeJobs.size} Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFDF9E)
                            )
                        }
                    }
                    StatusBadge(if (barberProfile.isOnline) "ONLINE" else "OFFLINE")
                }
            }
        }

        if (pendingRequests.isEmpty() && activeJobs.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.WorkOutline,
                    title = "No Active or Incoming Home Visits",
                    subtitle = "Customers book with ₹0 platform fee! Incoming home-visit requests appear here immediately."
                )
            }
        }

        if (pendingRequests.isNotEmpty()) {
            item {
                SectionHeader(
                    eyebrow = "INCOMING DISPATCH",
                    title = "New Home Visit Requests (${pendingRequests.size})"
                )
            }
            items(pendingRequests, key = { it.id }) { booking ->
                BarberIncomingRequestCard(
                    booking = booking,
                    isBusy = isBusy,
                    onAccept = { viewModel.barberRespond(booking, accept = true) },
                    onReject = { reason -> viewModel.barberRespond(booking, accept = false, reason = reason) }
                )
            }
        }

        if (activeJobs.isNotEmpty()) {
            item {
                SectionHeader(
                    eyebrow = "ACTIVE APPOINTMENTS",
                    title = "Assigned Home Visits (${activeJobs.size})"
                )
            }
            items(activeJobs, key = { it.id }) { booking ->
                LaunchedEffect(booking.id) {
                    viewModel.loadProtectedAddress(booking.id)
                }
                BarberExecutionCard(
                    booking = booking,
                    privateAddress = protectedAddresses[booking.id],
                    catalogServices = catalogServices,
                    isBusy = isBusy,
                    onAdvanceStatus = { next -> viewModel.barberAdvanceTripStatus(booking.id, next) },
                    onRequestAddOn = { svc -> viewModel.barberRequestExtraService(booking.id, svc) },
                    onRecordDirectPayment = { status, method, paise, ref ->
                        viewModel.barberRecordDirectPayment(booking.id, status, method, paise, ref)
                    }
                )
            }
        }
    }
}

@Composable
private fun BarberIncomingRequestCard(
    booking: Booking,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onReject: (String) -> Unit
) {
    var rejectReason by remember { mutableStateOf("") }
    var showRejectField by remember { mutableStateOf(false) }

    val commissionPaise = booking.effectiveBarberCommissionPaise()
    val netTakeHomePaise = booking.effectiveBarberNetPaise()

    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(booking.bookingNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${booking.dateKey} • ${CurrencyUtils.formatHourSlot(booking.startHour)} (${booking.durationMinutes} min)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StatusBadge("HOME VISIT")
            }

            Text(
                text = "Customer: ${booking.customerName} • Zone: ${booking.areaName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Services: ${booking.serviceNamesSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BarberCommissionBreakdownCard(
                grossCustomerPayablePaise = booking.barberServicePayablePaise,
                barberCommissionPaise = commissionPaise,
                barberNetEarningsPaise = netTakeHomePaise
            )

            if (booking.customerNotes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Style Note: ${booking.customerNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("barber_accept_${booking.id}")
                ) {
                    Text("Accept & Lock", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { showRejectField = !showRejectField },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Reject")
                }
            }

            if (showRejectField) {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = { Text("Rejection reason") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onReject(rejectReason) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Confirm Rejection")
                }
            }
        }
    }
}

@Composable
private fun BarberExecutionCard(
    booking: Booking,
    privateAddress: BookingPrivateAddress?,
    catalogServices: List<CatalogService>,
    isBusy: Boolean,
    onAdvanceStatus: (String) -> Unit,
    onRequestAddOn: (CatalogService) -> Unit,
    onRecordDirectPayment: (String, String, Long, String) -> Unit
) {
    val context = LocalContext.current
    var showAddOnSelector by remember { mutableStateOf(false) }
    var paymentDeclaration by remember { mutableStateOf("UPI_DECLARED") }
    var amountRupeesText by remember(booking.barberServicePayablePaise) {
        mutableStateOf((booking.barberServicePayablePaise / 100L).toString())
    }
    var upiRef by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(booking.bookingNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${booking.customerName} • ${booking.dateKey} ${CurrencyUtils.formatHourSlot(booking.startHour)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(booking.status)
            }

            VisitProgressStepper(status = booking.status)

            // Authorized Customer Home Door Address & Google Maps Handoff
            if (privateAddress != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Authorized Customer Door Location",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(privateAddress.fullAddressSnapshot, style = MaterialTheme.typography.bodySmall)
                        if (privateAddress.accessInstructions.isNotBlank()) {
                            Text(
                                text = "Door/Gate Note: ${privateAddress.accessInstructions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Contact: ${privateAddress.customerPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedButton(
                            onClick = {
                                val encoded = Uri.encode(privateAddress.fullAddressSnapshot)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded"))
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigate to Customer Door", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            BarberCommissionBreakdownCard(
                grossCustomerPayablePaise = booking.barberServicePayablePaise,
                barberCommissionPaise = booking.effectiveBarberCommissionPaise(),
                barberNetEarningsPaise = booking.effectiveBarberNetPaise(),
                directPaymentStatusLabel = "Collect full service charge from customer; commission settles in barber ledger"
            )

            // Ordered Execution Transitions
            when (booking.status) {
                "CONFIRMED" -> {
                    Button(
                        onClick = { onAdvanceStatus("ON_THE_WAY") },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("1. Mark On The Way", fontWeight = FontWeight.Bold)
                    }
                }
                "ON_THE_WAY" -> {
                    Button(
                        onClick = { onAdvanceStatus("ARRIVED") },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("2. Mark Arrived at Customer Door", fontWeight = FontWeight.Bold)
                    }
                }
                "ARRIVED" -> {
                    Button(
                        onClick = { onAdvanceStatus("IN_PROGRESS") },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("3. Confirm Services & Start Grooming", fontWeight = FontWeight.Bold)
                    }
                }
                "IN_PROGRESS" -> {
                    Text(
                        text = "Extra Service Rule: Never raise prices silently. Send a catalog request for customer approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (booking.pendingAddOnStatus == "REQUESTED") {
                        StatusBadge("AWAITING APPROVAL: ${booking.pendingAddOnName}")
                    } else {
                        OutlinedButton(
                            onClick = { showAddOnSelector = !showAddOnSelector },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Request Catalog Extra Service")
                        }
                        if (showAddOnSelector) {
                            catalogServices.take(6).forEach { svc ->
                                OutlinedButton(
                                    onClick = {
                                        onRequestAddOn(svc)
                                        showAddOnSelector = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("+ ${svc.name} (${CurrencyUtils.formatRupees(svc.pricePaise)}, +${svc.durationMinutes}m)")
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "4. Complete Service & Record Customer Payment (${CurrencyUtils.formatRupees(booking.barberServicePayablePaise)})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val paymentOptions = listOf(
                        "UPI_DECLARED" to "UPI Paid",
                        "CASH_DECLARED" to "Cash Paid",
                        "PARTIAL_DECLARED" to "Partial",
                        "DISPUTED" to "Disputed"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(paymentOptions) { (code, lbl) ->
                            FilterChip(
                                selected = paymentDeclaration == code,
                                onClick = { paymentDeclaration = code },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amountRupeesText,
                            onValueChange = { amountRupeesText = it.filter { c -> c.isDigit() } },
                            label = { Text("Received (₹)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(132.dp)
                        )
                        OutlinedTextField(
                            value = upiRef,
                            onValueChange = { upiRef = it },
                            label = { Text("UPI Ref / Note") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            val paise = (amountRupeesText.toLongOrNull() ?: 0L) * 100L
                            val method = when (paymentDeclaration) {
                                "CASH_DECLARED" -> "CASH"
                                "UPI_DECLARED" -> "UPI"
                                else -> "NONE"
                            }
                            onRecordDirectPayment(paymentDeclaration, method, paise, upiRef)
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Submit Completion & Payment", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BarberScheduleAndServicesScreen(
    viewModel: MarketplaceViewModel,
    barberProfile: BarberProfile?,
    catalogServices: List<CatalogService>,
    isBusy: Boolean
) {
    if (barberProfile == null) {
        EmptyStateCard(
            icon = Icons.Filled.EventAvailable,
            title = "No Barber Profile Found",
            subtitle = "Apply to become a partner barber from the Account tab first."
        )
        return
    }

    var isOnline by remember(barberProfile.isOnline) { mutableStateOf(barberProfile.isOnline) }
    var startHr by remember(barberProfile.workStartHour) { mutableStateOf(barberProfile.workStartHour.toString()) }
    var endHr by remember(barberProfile.workEndHour) { mutableStateOf(barberProfile.workEndHour.toString()) }
    var breakStart by remember(barberProfile.breakStartHour) { mutableStateOf(barberProfile.breakStartHour.toString()) }
    var breakEnd by remember(barberProfile.breakEndHour) { mutableStateOf(barberProfile.breakEndHour.toString()) }
    var bufferMin by remember(barberProfile.travelBufferMinutes) { mutableStateOf(barberProfile.travelBufferMinutes.toString()) }
    var upiId by remember(barberProfile.upiId) { mutableStateOf(barberProfile.upiId) }
    var shopName by remember(barberProfile.shopName) { mutableStateOf(barberProfile.effectiveShopName()) }
    var shopAddress by remember(barberProfile.shopAddress) { mutableStateOf(barberProfile.effectiveShopAddress()) }
    var skillsText by remember(barberProfile.skills) { mutableStateOf(barberProfile.effectiveSkills().joinToString(", ")) }
    var chosenSvcIds by remember(barberProfile.serviceIds) { mutableStateOf(barberProfile.serviceIds.toSet()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "STUDIO & AVAILABILITY",
                title = "Schedule, Shop & Skills",
                subtitle = "Update your working hours, home-visit dispatch status, and signature skills."
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Available for Home Visits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Pause during salon rush hours or time off",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = isOnline, onCheckedChange = { isOnline = it })
                        }
                    }

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Barber Shop / Studio Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { shopAddress = it },
                        label = { Text("Shop Street Address (Shown in Nearest Shops)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = skillsText,
                        onValueChange = { skillsText = it },
                        label = { Text("Signature Haircut & Grooming Skills (comma separated)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHr,
                            onValueChange = { startHr = it.filter { c -> c.isDigit() } },
                            label = { Text("Start Hour (0-23)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endHr,
                            onValueChange = { endHr = it.filter { c -> c.isDigit() } },
                            label = { Text("End Hour (1-24)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = breakStart,
                            onValueChange = { breakStart = it.filter { c -> c.isDigit() } },
                            label = { Text("Lunch Start Hr") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = breakEnd,
                            onValueChange = { breakEnd = it.filter { c -> c.isDigit() } },
                            label = { Text("Lunch End Hr") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bufferMin,
                            onValueChange = { bufferMin = it.filter { c -> c.isDigit() } },
                            label = { Text("Travel Buffer (m)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(150.dp)
                        )
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("Direct Payment UPI ID") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Services You Perform", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    catalogServices.forEach { svc ->
                        val checked = chosenSvcIds.contains(svc.id)
                        Surface(
                            onClick = {
                                chosenSvcIds = if (checked) chosenSvcIds - svc.id else chosenSvcIds + svc.id
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        chosenSvcIds = if (isChecked) chosenSvcIds + svc.id else chosenSvcIds - svc.id
                                    }
                                )
                                Text(
                                    text = "${svc.name} (${CurrencyUtils.formatRupees(svc.pricePaise)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.barberSaveSchedule(
                                isOnline = isOnline,
                                workStartHour = startHr.toLongOrNull() ?: 9L,
                                workEndHour = endHr.toLongOrNull() ?: 20L,
                                breakStartHour = breakStart.toLongOrNull() ?: 13L,
                                breakEndHour = breakEnd.toLongOrNull() ?: 14L,
                                travelBufferMinutes = bufferMin.toLongOrNull() ?: 15L,
                                upiId = upiId,
                                shopName = shopName,
                                shopAddress = shopAddress,
                                skillsText = skillsText,
                                serviceIds = chosenSvcIds.toList()
                            )
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Save Schedule, Shop & Skills", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BarberEarningsScreen(
    bookings: List<Booking>
) {
    val completed = bookings.filter { it.status == "COMPLETED" || it.status == "DISPUTED" }
    val totalGrossCollectedPaise = completed.sumOf { it.directPaymentDeclaredPaise }
    val totalPlatformCommissionPaise = completed.sumOf { it.effectiveBarberCommissionPaise() }
    val totalNetTakeHomePaise = completed.sumOf {
        (it.directPaymentDeclaredPaise - it.effectiveBarberCommissionPaise()).coerceAtLeast(0L)
    }
    val disputedCount = completed.count { it.directPaymentStatus == "DISPUTED" || it.status == "DISPUTED" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "PARTNER PAYOUT LEDGER",
                title = "Earnings & Commission",
                subtitle = "Customers pay ₹0 platform fee. You collect 100% directly via UPI/Cash; DoorCut's partner commission is tracked below."
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EspressoHeaderBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "NET BARBER TAKE-HOME",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFDF9E)
                    )
                    Text(
                        text = CurrencyUtils.formatRupees(totalNetTakeHomePaise),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${completed.size} Completed Visits • $disputedCount Disputed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD6C8B6)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBentoCard(
                    icon = Icons.Filled.Handshake,
                    title = "Gross Collected",
                    value = CurrencyUtils.formatRupees(totalGrossCollectedPaise),
                    subtitle = "Direct UPI & Cash",
                    modifier = Modifier.weight(1f)
                )
                MetricBentoCard(
                    icon = Icons.Filled.Percent,
                    title = "Platform Commission",
                    value = CurrencyUtils.formatRupees(totalPlatformCommissionPaise),
                    subtitle = "Partner fee (Cust. ₹0)",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (completed.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "No Settled Visits Yet",
                    subtitle = "Complete assigned home visits to view your gross receipts, commission, and net take-home earnings."
                )
            }
        } else {
            items(completed, key = { it.id }) { b ->
                val comm = b.effectiveBarberCommissionPaise()
                val net = (b.directPaymentDeclaredPaise - comm).coerceAtLeast(0L)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${b.bookingNumber} • ${b.customerName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${b.dateKey} • Gross ${CurrencyUtils.formatRupees(b.directPaymentDeclaredPaise)} − Comm ${CurrencyUtils.formatRupees(comm)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatRupees(net),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
