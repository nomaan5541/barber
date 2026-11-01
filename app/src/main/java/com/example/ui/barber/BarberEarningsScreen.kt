package com.example.ui.barber

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Booking
import com.example.data.model.CurrencyUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricBentoCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassPill
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.theme.BrassGold
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class EarningsPeriod(val id: String, val label: String) {
    ALL("ALL", "All Time"),
    MONTH("MONTH", "This Month"),
    WEEK("WEEK", "Last 7 Days"),
    TODAY("TODAY", "Today")
}

@Composable
fun BarberEarningsScreen(
    bookings: List<Booking>,
    onOpenBookingDetail: ((String) -> Unit)? = null
) {
    var selectedPeriodId by rememberSaveable { mutableStateOf(EarningsPeriod.ALL.id) }

    val todayKey = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    }
    val weekAgoKey = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    val monthPrefix = remember {
        SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
    }

    val allCompletedJobs = remember(bookings) {
        bookings.filter { it.status in setOf("COMPLETED", "DISPUTED") }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    val filteredJobs = remember(allCompletedJobs, selectedPeriodId, todayKey, weekAgoKey, monthPrefix) {
        when (selectedPeriodId) {
            EarningsPeriod.TODAY.id -> allCompletedJobs.filter { it.dateKey == todayKey }
            EarningsPeriod.WEEK.id -> allCompletedJobs.filter { it.dateKey >= weekAgoKey }
            EarningsPeriod.MONTH.id -> allCompletedJobs.filter { it.dateKey.startsWith(monthPrefix) }
            else -> allCompletedJobs
        }
    }

    val cashReceivedPaise = remember(filteredJobs) {
        filteredJobs.filter {
            it.directPaymentStatus in setOf("CASH_DECLARED", "CUSTOMER_CONFIRMED") &&
                it.directPaymentMethod == "CASH"
        }.sumOf {
            if (it.directPaymentDeclaredPaise > 0L) it.directPaymentDeclaredPaise else it.barberServicePayablePaise
        }
    }

    val upiReceivedPaise = remember(filteredJobs) {
        filteredJobs.filter {
            it.directPaymentStatus in setOf("UPI_DECLARED", "CUSTOMER_CONFIRMED") &&
                it.directPaymentMethod == "UPI"
        }.sumOf {
            if (it.directPaymentDeclaredPaise > 0L) it.directPaymentDeclaredPaise else it.barberServicePayablePaise
        }
    }

    val totalMarkedReceivedPaise = cashReceivedPaise + upiReceivedPaise

    val grossServicePaise = remember(filteredJobs) {
        filteredJobs.sumOf { it.barberServicePayablePaise }
    }
    val totalCommissionPaise = remember(filteredJobs) {
        filteredJobs.sumOf { it.barberCommissionPaise }
    }
    val netTakeHomePaise = remember(filteredJobs) {
        filteredJobs.sumOf {
            if (it.barberNetEarningsPaise > 0L) it.barberNetEarningsPaise
            else (it.barberServicePayablePaise - it.barberCommissionPaise).coerceAtLeast(0L)
        }
    }
    val averageTicketPaise = remember(filteredJobs, grossServicePaise) {
        if (filteredJobs.isEmpty()) 0L else grossServicePaise / filteredJobs.size.toLong()
    }

    val unconfirmedOrDisputedCount = remember(filteredJobs) {
        filteredJobs.count {
            it.directPaymentStatus in setOf("PENDING", "UNPAID_DECLARED", "PARTIAL_DECLARED", "DISPUTED") ||
                it.status == "DISPUTED"
        }
    }

    val weeklyServiceTotalPaise = remember(allCompletedJobs, weekAgoKey) {
        allCompletedJobs.filter { it.dateKey >= weekAgoKey }.sumOf { it.barberServicePayablePaise }
    }
    val monthlyServiceTotalPaise = remember(allCompletedJobs, monthPrefix) {
        allCompletedJobs.filter { it.dateKey.startsWith(monthPrefix) }.sumOf { it.barberServicePayablePaise }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("barber_earnings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "DIRECT SERVICE LEDGER",
                title = "Partner Earnings & Payouts",
                subtitle = "Audit gross customer collections, platform commission, net take-home, and Cash vs UPI split.",
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        // Period Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .testTag("earnings_period_filter_row")
            ) {
                items(EarningsPeriod.entries, key = { it.id }) { period ->
                    LiquidGlassPill(
                        text = period.label,
                        selected = selectedPeriodId == period.id,
                        onClick = { selectedPeriodId = period.id },
                        modifier = Modifier.testTag("earnings_period_${period.id}")
                    )
                }
            }
        }

        // Mandatory Direct-Payment Record Disclaimer Banner in FloatingPill Liquid Glass
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.FloatingPill,
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = BrassGold.copy(alpha = 0.5f),
                shadowElevation = 4.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 1)
                    .testTag("barber_direct_payment_disclaimer")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = BrassGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Direct Payment Records Only: Customers pay your service fee directly to you via Cash or UPI at the doorstep. Marking a payment as UPI records your declaration and does not verify bank settlement.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFF8F3EC)
                    )
                }
            }
        }

        // Gross vs Commission vs Net Partner Take-Home Breakdown Card
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 3.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 2)
                    .testTag("barber_net_ledger_card")
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
                        Column {
                            Text(
                                text = "NET PARTNER TAKE-HOME",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = CurrencyUtils.formatRupees(netTakeHomePaise),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Avg ${CurrencyUtils.formatRupees(averageTicketPaise)}/visit",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    LedgerStatRow(
                        label = "Gross Service Value (${filteredJobs.size} visits)",
                        value = CurrencyUtils.formatRupees(grossServicePaise)
                    )
                    LedgerStatRow(
                        label = "Platform Dispatch Commission",
                        value = "- ${CurrencyUtils.formatRupees(totalCommissionPaise)}",
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LedgerStatRow(
                        label = "Marked Collected in Hand / UPI",
                        value = CurrencyUtils.formatRupees(totalMarkedReceivedPaise),
                        bold = true
                    )

                    if (totalMarkedReceivedPaise > 0L) {
                        val cashFraction = (cashReceivedPaise.toFloat() / totalMarkedReceivedPaise.toFloat()).coerceIn(0f, 1f)
                        val cashPct = (cashFraction * 100f).toInt()
                        val upiPct = 100 - cashPct
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cash $cashPct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "UPI $upiPct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (cashFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(cashFraction.coerceAtLeast(0.01f))
                                        .height(8.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            if (1f - cashFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight((1f - cashFraction).coerceAtLeast(0.01f))
                                        .height(8.dp)
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Row 1: Completed Services & Total Marked Received
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 3)
            ) {
                MetricBentoCard(
                    icon = Icons.Filled.CheckCircle,
                    title = "Completed Services",
                    value = "${filteredJobs.size}",
                    subtitle = "Finished home visits",
                    modifier = Modifier.weight(1f)
                )
                MetricBentoCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Total Marked Received",
                    value = CurrencyUtils.formatRupees(totalMarkedReceivedPaise),
                    subtitle = "Direct Cash + UPI",
                    accentHighlight = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: Cash Received & UPI Received
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 4)
            ) {
                MetricBentoCard(
                    icon = Icons.Filled.Payments,
                    title = "Cash Received",
                    value = CurrencyUtils.formatRupees(cashReceivedPaise),
                    subtitle = "Collected in hand",
                    modifier = Modifier.weight(1f)
                )
                MetricBentoCard(
                    icon = Icons.Filled.QrCode2,
                    title = "UPI Declared",
                    value = CurrencyUtils.formatRupees(upiReceivedPaise),
                    subtitle = "Self-recorded UPI",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: Unconfirmed/Disputed & Weekly / Monthly Service Totals
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 5)
            ) {
                MetricBentoCard(
                    icon = Icons.Filled.ReportProblem,
                    title = "Unconfirmed / Disputed",
                    value = "$unconfirmedOrDisputedCount",
                    subtitle = "Pending or under review",
                    modifier = Modifier.weight(1f)
                )
                MetricBentoCard(
                    icon = Icons.Filled.CalendarMonth,
                    title = "7-Day / Month Total",
                    value = CurrencyUtils.formatRupees(weeklyServiceTotalPaise),
                    subtitle = "Month: ${CurrencyUtils.formatRupees(monthlyServiceTotalPaise)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (filteredJobs.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "No Completed Jobs in Selected Period",
                        subtitle = "Once you finish a service and record direct Cash or UPI payment, each itemized receipt appears here."
                    )
                }
            }
        } else {
            itemsIndexed(filteredJobs, key = { _, it -> it.id }) { idx, job ->
                val jobNet = if (job.barberNetEarningsPaise > 0L) {
                    job.barberNetEarningsPaise
                } else {
                    (job.barberServicePayablePaise - job.barberCommissionPaise).coerceAtLeast(0L)
                }
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp,
                    onClick = if (onOpenBookingDetail != null) {
                        { onOpenBookingDetail(job.id) }
                    } else null,
                    interactivePhysics = true,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = idx + 1)
                        .testTag("earnings_receipt_card_${job.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${job.serviceNamesSummary} · ${job.customerName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${job.dateKey} · ${job.directPaymentSummaryLabel()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Net Take-Home: ${CurrencyUtils.formatRupees(jobNet)} (Fee: ${CurrencyUtils.formatRupees(job.barberCommissionPaise)})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyUtils.formatRupees(job.barberServicePayablePaise),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatusBadge(job.directPaymentStatus)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerStatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}
