package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AuditLog
import com.example.data.model.BarberProfile
import com.example.data.model.Booking
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.PlatformConfig
import com.example.data.model.ServiceCategory
import com.example.data.model.SupportTicket
import com.example.data.model.UserAccount
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricBentoCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkillPill
import com.example.ui.components.StatusBadge
import com.example.ui.components.serviceVisualFor
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.viewmodel.MarketplaceViewModel

@Composable
fun AdminOverviewScreen(
    viewModel: MarketplaceViewModel,
    users: List<UserAccount>,
    barbers: List<BarberProfile>,
    services: List<CatalogService>,
    bookings: List<Booking>,
    tickets: List<SupportTicket>,
    isBusy: Boolean
) {
    val totalCustomers = users.count { it.role == "CUSTOMER" }
    val pendingBarbers = barbers.count { it.approvalStatus == "PENDING" }
    val activeBarbers = barbers.count { it.approvalStatus == "APPROVED" }

    val completedBookings = bookings.filter { it.status == "COMPLETED" }
    val barberCommissionRevenuePaise = completedBookings.sumOf { it.effectiveBarberCommissionPaise() }
    val barberNetTakeHomePaise = completedBookings.sumOf {
        (it.directPaymentDeclaredPaise - it.effectiveBarberCommissionPaise()).coerceAtLeast(0L)
    }

    val disputedBookingsCount = bookings.count { it.status == "DISPUTED" || it.directPaymentStatus == "DISPUTED" }
    val openTicketsCount = tickets.count { it.status != "RESOLVED" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "MARKETPLACE COMMAND CENTER",
                title = "Admin Operations Dashboard",
                subtitle = "₹0 Customer Fee Model • Platform Commission Charged from Partner Barbers"
            )
        }

        // Seed / Sync Canonical Services & Barber Studios Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EspressoHeaderBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(EspressoHeaderBg, Color(0xFF2E231A))
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BrassGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sync Canonical Services & Partner Barber Shops",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Ensure all 12 visual grooming services, 3 PIN zones, ₹0 customer fee / 15% barber commission policy, and 4 partner barber studios with verified skills are active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD6C8B6)
                    )
                    Button(
                        onClick = { viewModel.adminBootstrapCatalog() },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrassGold,
                            contentColor = EspressoHeaderBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_bootstrap_button")
                    ) {
                        Text(
                            text = if (services.isEmpty()) "Seed Catalog & Partner Barber Shops" else "Refresh Catalog, Skills & Barber Shops",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBentoCard(
                    icon = Icons.Filled.Percent,
                    title = "Platform Commission",
                    value = CurrencyUtils.formatRupees(barberCommissionRevenuePaise),
                    subtitle = "Earned from barbers (Cust. ₹0)",
                    accentHighlight = true,
                    modifier = Modifier.weight(1f)
                )
                MetricBentoCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Barber Net Earnings",
                    value = CurrencyUtils.formatRupees(barberNetTakeHomePaise),
                    subtitle = "Retained by partner studios",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBentoCard(
                    icon = Icons.Filled.EventAvailable,
                    title = "Bookings (${bookings.size})",
                    value = "${completedBookings.size} Done",
                    subtitle = "${bookings.count { it.status == "AWAITING_BARBER" }} Pending • ${bookings.count { it.status == "CANCELLED" }} Canc.",
                    modifier = Modifier.weight(1f)
                )
                MetricBentoCard(
                    icon = Icons.Filled.Storefront,
                    title = "Studios & Users",
                    value = "$activeBarbers Active",
                    subtitle = "$pendingBarbers Pending • $totalCustomers Cust.",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBentoCard(
                    icon = Icons.Filled.ReportProblem,
                    title = "Payment Disputes",
                    value = "$disputedBookingsCount Flagged",
                    subtitle = "Requires admin review",
                    modifier = Modifier.weight(1f)
                )
                MetricBentoCard(
                    icon = Icons.Filled.SupportAgent,
                    title = "Support Tickets",
                    value = "$openTicketsCount Open",
                    subtitle = "${tickets.size} Total filed",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminBarbersManagementScreen(
    viewModel: MarketplaceViewModel,
    barbers: List<BarberProfile>,
    isBusy: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "PARTNER QUALITY GATE",
                title = "Barbers & Studios Approval",
                subtitle = "Verify craftsmanship, skills, and sanitized home-visit kits before activation."
            )
        }

        if (barbers.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Verified,
                    title = "No Barber Profiles Yet",
                    subtitle = "Seed partner barber shops from Overview or let barbers apply from the Account screen."
                )
            }
        } else {
            items(barbers, key = { it.barberId }) { barber ->
                var auditReason by remember { mutableStateOf("Verified shop, skills & sanitized home kit") }
                Card(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                BarberAvatarBadge(name = barber.name, isOnline = barber.isOnline, size = 46.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${barber.name} • ${barber.effectiveShopName()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${barber.experienceYears} yrs exp • ★ ${String.format("%.1f", barber.ratingAvg)} • ${barber.effectivePerfectionBadge()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            StatusBadge(barber.approvalStatus)
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            barber.effectiveSkills().forEach { skill ->
                                SkillPill(skill = skill)
                            }
                        }

                        Text(
                            text = barber.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = auditReason,
                            onValueChange = { auditReason = it },
                            label = { Text("Audit Log Reason") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.adminSetBarberStatus(barber.barberId, "APPROVED", auditReason) },
                                enabled = !isBusy && barber.approvalStatus != "APPROVED",
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Approve", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.adminSetBarberStatus(barber.barberId, "SUSPENDED", auditReason) },
                                enabled = !isBusy && barber.approvalStatus != "SUSPENDED",
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Suspend")
                            }
                            OutlinedButton(
                                onClick = { viewModel.adminSetBarberStatus(barber.barberId, "REJECTED", auditReason) },
                                enabled = !isBusy && barber.approvalStatus != "REJECTED",
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCatalogManagementScreen(
    viewModel: MarketplaceViewModel,
    services: List<CatalogService>,
    isBusy: Boolean
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("HAIRCUTS") }
    var desc by remember { mutableStateOf("") }
    var priceRupees by remember { mutableStateOf("300") }
    var durationMin by remember { mutableStateOf("30") }
    var isActive by remember { mutableStateOf(true) }
    var isPackage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "VISUAL MENU CURATION",
                title = "Service & Combo Catalog",
                subtitle = "Each category automatically pairs with its bespoke service photography on the customer's first page."
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editingId == null) "Create Service / Package" else "Editing Service ($editingId)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ServiceCategory.entries) { cat ->
                            FilterChip(
                                selected = category == cat.name,
                                onClick = {
                                    category = cat.name
                                    isPackage = cat == ServiceCategory.PACKAGES
                                },
                                label = { Text(cat.displayTitle) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Service Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceRupees,
                            onValueChange = { priceRupees = it.filter { c -> c.isDigit() } },
                            label = { Text("Direct Barber Price (₹)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = durationMin,
                            onValueChange = { durationMin = it.filter { c -> c.isDigit() } },
                            label = { Text("Duration (min)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                        Text("Active in Menu", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(12.dp))
                        Checkbox(checked = isPackage, onCheckedChange = { isPackage = it })
                        Text("Combo Package", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.adminSaveService(
                                    serviceId = editingId,
                                    name = name,
                                    category = category,
                                    description = desc,
                                    priceRupees = priceRupees.toLongOrNull() ?: 300L,
                                    durationMinutes = durationMin.toLongOrNull() ?: 30L,
                                    isActive = isActive,
                                    isPackage = isPackage,
                                    instructions = "Keep a chair & power socket accessible.",
                                    sortOrder = (services.size + 1).toLong()
                                )
                                editingId = null
                                name = ""
                                desc = ""
                            },
                            enabled = !isBusy && name.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(if (editingId == null) "Add to Catalog" else "Save Changes", fontWeight = FontWeight.Bold)
                        }
                        if (editingId != null) {
                            OutlinedButton(
                                onClick = {
                                    editingId = null
                                    name = ""
                                    desc = ""
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }

        items(services, key = { it.id }) { svc ->
            val visual = remember(svc.id, svc.category, svc.name, svc.isPackage) {
                serviceVisualFor(svc)
            }
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = visual.drawableRes),
                        contentDescription = svc.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(svc.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(if (svc.isActive) "ACTIVE" else "HIDDEN")
                        }
                        Text(
                            text = "${svc.category} • ${CurrencyUtils.formatRupees(svc.pricePaise)} • ${svc.durationMinutes}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            editingId = svc.id
                            name = svc.name
                            category = svc.category
                            desc = svc.description
                            priceRupees = (svc.pricePaise / 100L).toString()
                            durationMin = svc.durationMinutes.toString()
                            isActive = svc.isActive
                            isPackage = svc.isPackage
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBookingsAndDisputesScreen(
    viewModel: MarketplaceViewModel,
    bookings: List<Booking>,
    approvedBarbers: List<BarberProfile>,
    tickets: List<SupportTicket>,
    isBusy: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "DISPUTE & DISPATCH DESK",
                title = "Bookings & Reassignments",
                subtitle = "Intervene on late visits, reassign approved specialists, or settle payment disputes."
            )
        }

        if (tickets.any { it.status != "RESOLVED" }) {
            item {
                Text(
                    text = "Open Support & Dispute Tickets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(tickets.filter { it.status != "RESOLVED" }, key = { it.id }) { ticket ->
                var resolution by remember { mutableStateOf("") }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ticket.category}: ${ticket.subject}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            StatusBadge(ticket.status)
                        }
                        Text(ticket.description, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = resolution,
                            onValueChange = { resolution = it },
                            label = { Text("Investigation & Resolution Outcome") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { viewModel.adminResolveTicket(ticket.id, "RESOLVED", resolution) },
                            enabled = !isBusy && resolution.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Record Resolution & Notify Parties", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (bookings.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Gavel,
                    title = "No Bookings Recorded Yet",
                    subtitle = "Customer reservations and direct-settlement status updates will appear here in real time."
                )
            }
        } else {
            items(bookings.sortedByDescending { it.createdAt?.seconds ?: 0L }, key = { it.id }) { booking ->
                var overrideReason by remember { mutableStateOf("Admin operational intervention") }
                var selectedReplacement by remember { mutableStateOf<BarberProfile?>(null) }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${booking.bookingNumber} • ${booking.customerName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            StatusBadge(booking.status)
                        }
                        Text(
                            text = "Barber: ${booking.barberName} • ${booking.dateKey} ${CurrencyUtils.formatHourSlot(booking.startHour)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Customer Payable: ${CurrencyUtils.formatRupees(booking.barberServicePayablePaise)} (${booking.directPaymentStatus}) | Barber Comm: ${CurrencyUtils.formatRupees(booking.effectiveBarberCommissionPaise())}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (approvedBarbers.size > 1) {
                            Text("Reassign Barber:", style = MaterialTheme.typography.labelMedium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(approvedBarbers) { b ->
                                    FilterChip(
                                        selected = selectedReplacement?.barberId == b.barberId,
                                        onClick = { selectedReplacement = b },
                                        label = { Text(b.name.substringBefore(" ")) }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = overrideReason,
                            onValueChange = { overrideReason = it },
                            label = { Text("Mandatory Audit Reason") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedReplacement != null) {
                                Button(
                                    onClick = {
                                        viewModel.adminOverrideBooking(
                                            booking = booking,
                                            newStatus = "CONFIRMED",
                                            newFeeStatus = booking.bookingFeeStatus,
                                            newDirectPaymentStatus = booking.directPaymentStatus,
                                            replacementBarber = selectedReplacement,
                                            reason = overrideReason
                                        )
                                    },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Reassign", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (booking.status == "DISPUTED" || booking.directPaymentStatus == "DISPUTED") {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.adminOverrideBooking(
                                            booking = booking,
                                            newStatus = "COMPLETED",
                                            newFeeStatus = booking.bookingFeeStatus,
                                            newDirectPaymentStatus = "CUSTOMER_CONFIRMED",
                                            replacementBarber = null,
                                            reason = overrideReason
                                        )
                                    },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Settle Dispute")
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
fun AdminPoliciesAndAuditScreen(
    viewModel: MarketplaceViewModel,
    platformConfig: PlatformConfig,
    auditLogs: List<AuditLog>,
    isBusy: Boolean
) {
    var commissionMode by remember(platformConfig.bookingFeeType) {
        mutableStateOf(
            if (platformConfig.bookingFeeType == "BARBER_FLAT_FEE") "BARBER_FLAT_FEE" else "BARBER_COMMISSION_PERCENT"
        )
    }
    var barberCommBps by remember(platformConfig.barberCommissionBps) {
        mutableStateOf((if (platformConfig.barberCommissionBps > 0L) platformConfig.barberCommissionBps else 1500L).toString())
    }
    var barberFlatFeeRupees by remember(platformConfig.barberFlatFeePaise) {
        mutableStateOf((platformConfig.barberFlatFeePaise / 100L).toString())
    }
    var cutoffHrs by remember(platformConfig.freeCancellationCutoffHours) {
        mutableStateOf(platformConfig.freeCancellationCutoffHours.toString())
    }
    var holdMins by remember(platformConfig.slotHoldMinutes) {
        mutableStateOf(platformConfig.slotHoldMinutes.toString())
    }
    var policyText by remember(platformConfig.policyText) { mutableStateOf(platformConfig.policyText) }
    var maintenance by remember(platformConfig.maintenanceMode) { mutableStateOf(platformConfig.maintenanceMode) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = "GOVERNANCE & REVENUE RULES",
                title = "Commission & Audit Logs",
                subtitle = "Customers always pay ₹0 platform fee. Configure partner barber commission & review immutable logs."
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Barber Commission Policy (Customer Fee = ₹0)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = commissionMode == "BARBER_COMMISSION_PERCENT",
                            onClick = { commissionMode = "BARBER_COMMISSION_PERCENT" },
                            label = { Text("% Commission") }
                        )
                        FilterChip(
                            selected = commissionMode == "BARBER_FLAT_FEE",
                            onClick = { commissionMode = "BARBER_FLAT_FEE" },
                            label = { Text("Flat Per Job (₹)") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = barberCommBps,
                            onValueChange = { barberCommBps = it.filter { c -> c.isDigit() } },
                            label = { Text("Barber Comm (bps, 1500=15%)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = barberFlatFeeRupees,
                            onValueChange = { barberFlatFeeRupees = it.filter { c -> c.isDigit() } },
                            label = { Text("Barber Flat Fee (₹)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cutoffHrs,
                            onValueChange = { cutoffHrs = it.filter { c -> c.isDigit() } },
                            label = { Text("Free Cancel Cutoff (h)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = holdMins,
                            onValueChange = { holdMins = it.filter { c -> c.isDigit() } },
                            label = { Text("Slot Hold (min)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = policyText,
                        onValueChange = { policyText = it },
                        label = { Text("Customer Zero-Fee & Cancellation Policy Text") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Maintenance Mode (Pause New Bookings)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = maintenance, onCheckedChange = { maintenance = it })
                    }
                    Button(
                        onClick = {
                            viewModel.adminSaveBarberCommissionPolicy(
                                commissionMode = commissionMode,
                                barberCommissionBps = barberCommBps.toLongOrNull() ?: 1500L,
                                barberFlatFeeRupees = barberFlatFeeRupees.toLongOrNull() ?: 0L,
                                cutoffHours = cutoffHrs.toLongOrNull() ?: 2L,
                                slotHoldMinutes = holdMins.toLongOrNull() ?: 10L,
                                policyText = policyText,
                                maintenanceMode = maintenance
                            )
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Save Barber Commission Policy", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.HistoryEdu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Immutable Admin Audit Trail (${auditLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(auditLogs.sortedByDescending { it.createdAt?.seconds ?: 0L }, key = { it.id }) { log ->
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "${log.action} • ${log.entityType}:${log.entityId}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(log.reason, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
