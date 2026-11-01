package com.example.ui.admin

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Checkbox
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
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassPill
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CrimsonDispute
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
    val platformBookingFeeRevenuePaise = bookings
        .filter { it.bookingFeeStatus == "PAID_VERIFIED" && it.status !in setOf("CANCELLED", "EXPIRED") }
        .sumOf { if (it.platformBookingFeePaise > 0L) it.platformBookingFeePaise else 3000L }
    val barberDirectServiceTotalPaise = completedBookings.sumOf {
        if (it.directPaymentDeclaredPaise > 0L) it.directPaymentDeclaredPaise else it.barberServicePayablePaise
    }

    val disputedBookingsCount = bookings.count { it.status == "DISPUTED" || it.directPaymentStatus == "DISPUTED" }
    val openTicketsCount = tickets.count { it.status != "RESOLVED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_overview_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "MARKETPLACE COMMAND CENTER",
                title = "Admin Operations Dashboard",
                subtitle = "Separate Two-Payment Ledger: Platform Booking Fees vs Direct Customer-to-Barber Payments",
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        // Seed / Sync Canonical Services & Barber Studios Hero Liquid Glass Card
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.FloatingPill,
                shape = RoundedCornerShape(24.dp),
                accentBorderColor = BrassGold.copy(alpha = 0.55f),
                shadowElevation = 5.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 1)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BrassGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sync Canonical Services & Partner Barber Studios",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Ensure Haircut (₹300), Beard Trim (₹150), Haircut + Beard (₹400), ₹30 booking confirmation fee policy, and verified home barbers are synced.",
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
                            .tactilePressPhysics(enableHaptics = true)
                            .testTag("admin_bootstrap_button")
                    ) {
                        Text(
                            text = if (services.isEmpty()) "Seed Catalog & Partner Barber Studios" else "Refresh Catalog, Fees & Barber Studios",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 2)
            ) {
                MetricBentoCard(
                    icon = Icons.Filled.Percent,
                    title = "Booking Fee Revenue",
                    value = CurrencyUtils.formatRupees(platformBookingFeeRevenuePaise),
                    subtitle = "Paid to platform at checkout",
                    accentHighlight = true,
                    modifier = Modifier.weight(1f)
                )
                MetricBentoCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Direct Barber Pay",
                    value = CurrencyUtils.formatRupees(barberDirectServiceTotalPaise),
                    subtitle = "Paid directly to barbers",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 3)
            ) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 4)
            ) {
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

        // OpenSalon clean-room workflow equivalent: Master Chronological Dispatch Schedule
        val upcomingDispatch = bookings
            .filter { it.status !in setOf("CANCELLED", "DECLINED", "EXPIRED") }
            .sortedWith(compareBy<Booking> { it.dateKey }.thenBy { it.startHour })
            .take(8)
        if (upcomingDispatch.isNotEmpty()) {
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .testTag("admin_master_schedule_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Master Dispatch Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        upcomingDispatch.forEach { b ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${b.dateKey} • ${CurrencyUtils.formatHourSlot(b.startHour)} (${b.durationMinutes}m)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${b.customerName} -> ${b.barberName} (${b.areaSummary})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(b.status)
                            }
                        }
                    }
                }
            }
        }

        // Registered Users Governance (ACTIVE / SUSPENDED)
        if (users.isNotEmpty()) {
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .testTag("admin_users_governance_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Registered Accounts (${users.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        users.take(12).forEach { u ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${u.name.ifBlank { "Member" }} (${u.role})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${u.email.ifBlank { u.phone }} • ${u.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (u.role != "ADMIN") {
                                    val toggleTarget = if (u.status == "SUSPENDED") "ACTIVE" else "SUSPENDED"
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.adminSetUserStatus(
                                                userId = u.userId,
                                                status = toggleTarget,
                                                reason = "Admin account governance toggle"
                                            )
                                        },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(if (u.status == "SUSPENDED") "Activate" else "Suspend")
                                    }
                                } else {
                                    StatusBadge("ADMIN")
                                }
                            }
                        }
                    }
                }
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
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_barbers_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "PARTNER QUALITY GATE",
                title = "Barbers & Studios Approval",
                subtitle = "Verify craftsmanship, skills, and sanitized home-visit kits before activation.",
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        if (barbers.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 680.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.Verified,
                        title = "No Barber Profiles Yet",
                        subtitle = "Seed partner barber shops from Overview or let barbers apply from the Account screen."
                    )
                }
            }
        } else {
            itemsIndexed(barbers, key = { _, it -> it.barberId }) { idx, barber ->
                var auditReason by remember { mutableStateOf("Verified shop, skills & sanitized home kit") }
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = idx + 1)
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EspressoHeaderBg,
                                    contentColor = Color(0xFFFFDF9E)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .tactilePressPhysics(enableHaptics = true)
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
                                    .tactilePressPhysics(enableHaptics = true)
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
                                    .tactilePressPhysics(enableHaptics = true)
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
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_catalog_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "VISUAL MENU CURATION",
                title = "Service & Combo Catalog",
                subtitle = "Each category automatically pairs with its bespoke service photography on the customer's first page.",
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 1)
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
                            LiquidGlassPill(
                                text = cat.displayTitle,
                                selected = category == cat.name,
                                onClick = {
                                    category = cat.name
                                    isPackage = cat == ServiceCategory.PACKAGES
                                }
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
                    val canSaveService = !isBusy && name.isNotBlank()
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
                            enabled = canSaveService,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EspressoHeaderBg,
                                contentColor = Color(0xFFFFDF9E)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .tactilePressPhysics(enabled = canSaveService, enableHaptics = true)
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
                                modifier = Modifier
                                    .height(48.dp)
                                    .tactilePressPhysics()
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }

        itemsIndexed(services, key = { _, it -> it.id }) { idx, svc ->
            val visual = remember(svc.id, svc.category, svc.name, svc.isPackage) {
                serviceVisualFor(svc)
            }
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                interactivePhysics = true,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = idx + 1)
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
                        modifier = Modifier
                            .height(48.dp)
                            .tactilePressPhysics()
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
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_bookings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "DISPUTE & DISPATCH DESK",
                title = "Bookings & Reassignments",
                subtitle = "Intervene on late visits, reassign approved specialists, or settle payment disputes.",
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        if (tickets.any { it.status != "RESOLVED" }) {
            item {
                Text(
                    text = "Open Support & Dispute Tickets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                )
            }
            itemsIndexed(tickets.filter { it.status != "RESOLVED" }, key = { _, it -> it.id }) { idx, ticket ->
                var resolution by remember { mutableStateOf("") }
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(22.dp),
                    accentBorderColor = CrimsonDispute.copy(alpha = 0.55f),
                    shadowElevation = 2.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = idx + 1)
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
                        val canResolve = !isBusy && resolution.isNotBlank()
                        Button(
                            onClick = { viewModel.adminResolveTicket(ticket.id, "RESOLVED", resolution) },
                            enabled = canResolve,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EspressoHeaderBg,
                                contentColor = Color(0xFFFFDF9E)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .tactilePressPhysics(enabled = canResolve, enableHaptics = true)
                        ) {
                            Text("Record Resolution & Notify Parties", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (bookings.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 680.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.Gavel,
                        title = "No Bookings Recorded Yet",
                        subtitle = "Customer reservations and direct-settlement status updates will appear here in real time."
                    )
                }
            }
        } else {
            itemsIndexed(bookings.sortedByDescending { it.createdAt?.seconds ?: 0L }, key = { _, it -> it.id }) { idx, booking ->
                var overrideReason by remember { mutableStateOf("Admin operational intervention") }
                var selectedReplacement by remember { mutableStateOf<BarberProfile?>(null) }

                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = idx + 1)
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
                            text = "${booking.bookingFeeSummaryLabel()} • Barber Comm: ${CurrencyUtils.formatRupees(booking.effectiveBarberCommissionPaise())}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = booking.directPaymentSummaryLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        val directAudit = booking.directPaymentAuditSummary()
                        if (directAudit.isNotBlank()) {
                            Text(
                                text = directAudit,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (approvedBarbers.size > 1) {
                            Text("Reassign Barber:", style = MaterialTheme.typography.labelMedium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(approvedBarbers, key = { it.barberId }) { b ->
                                    LiquidGlassPill(
                                        text = b.name.substringBefore(" "),
                                        selected = selectedReplacement?.barberId == b.barberId,
                                        onClick = { selectedReplacement = b }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = overrideReason,
                            onValueChange = { overrideReason = it },
                            label = { Text("Mandatory Audit Reason / Payment UTR") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (booking.platformBookingFeePaise > 0L && booking.bookingFeeStatus != "PAID_VERIFIED") {
                                Button(
                                    onClick = {
                                        viewModel.adminVerifyUpfrontBookingFee(
                                            booking = booking,
                                            paymentReference = overrideReason
                                        )
                                    },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrassGold,
                                        contentColor = EspressoHeaderBg
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .tactilePressPhysics(enableHaptics = true)
                                        .testTag("admin_verify_fee_${booking.id}")
                                ) {
                                    Text("Verify Fee", fontWeight = FontWeight.Bold)
                                }
                            }
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EspressoHeaderBg,
                                        contentColor = Color(0xFFFFDF9E)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .tactilePressPhysics(enableHaptics = true)
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
                                        .tactilePressPhysics(enableHaptics = true)
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
    var feeMode by remember(platformConfig.bookingFeeType) {
        mutableStateOf(if (platformConfig.bookingFeeType == "PERCENTAGE") "PERCENTAGE" else "FIXED")
    }
    var fixedBookingFeeRupees by remember(platformConfig.fixedBookingFeePaise) {
        val effPaise = platformConfig.effectiveCustomerBookingFeePaise()
        mutableStateOf((effPaise / 100L).toString())
    }
    var percentageFeeBps by remember(platformConfig.percentageBookingFeeBps) {
        mutableStateOf(platformConfig.percentageBookingFeeBps.toString())
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
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_policies_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "GOVERNANCE & BOOKING FEE",
                title = "Booking Fee & Audit Logs",
                subtitle = "Configure the upfront Booking Confirmation Fee (default ₹30) and cancellation rules.",
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 1)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Booking Confirmation Fee & Cancellation Policy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidGlassPill(
                            text = "Fixed Fee (₹)",
                            selected = feeMode == "FIXED",
                            onClick = { feeMode = "FIXED" }
                        )
                        LiquidGlassPill(
                            text = "% Booking Fee",
                            selected = feeMode == "PERCENTAGE",
                            onClick = { feeMode = "PERCENTAGE" }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fixedBookingFeeRupees,
                            onValueChange = { fixedBookingFeeRupees = it.filter { c -> c.isDigit() } },
                            label = { Text("Booking Fee (₹, default 30)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = percentageFeeBps,
                            onValueChange = { percentageFeeBps = it.filter { c -> c.isDigit() } },
                            label = { Text("Fee % (bps, 500=5%)") },
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
                        label = { Text("Two-Payment & Cancellation Policy Text") },
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
                            viewModel.adminSavePlatformPolicy(
                                bookingFeeType = feeMode,
                                fixedBookingFeeRupees = fixedBookingFeeRupees.toLongOrNull() ?: 30L,
                                percentageBookingFeeBps = percentageFeeBps.toLongOrNull() ?: 0L,
                                barberCommissionBps = 0L,
                                cutoffHours = cutoffHrs.toLongOrNull() ?: 2L,
                                slotHoldMinutes = holdMins.toLongOrNull() ?: 10L,
                                policyText = policyText,
                                maintenanceMode = maintenance
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
                            .height(50.dp)
                            .tactilePressPhysics(enableHaptics = true)
                    ) {
                        Text("Save Booking Fee & Policy", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
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
        }

        itemsIndexed(auditLogs.sortedByDescending { it.createdAt?.seconds ?: 0L }, key = { _, it -> it.id }) { idx, log ->
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 1.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = idx + 1)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "${log.action} • ${log.entityType}:${log.entityId}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(log.reason, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
