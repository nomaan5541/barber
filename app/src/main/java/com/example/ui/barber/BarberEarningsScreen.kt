package com.example.ui.barber

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BarberEarningsScreen(
    bookings: List<Booking>
) {
    val completedJobs = remember(bookings) {
        bookings.filter { it.status in setOf("COMPLETED", "DISPUTED") }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    val cashReceivedPaise = remember(completedJobs) {
        completedJobs.filter {
            it.directPaymentStatus in setOf("CASH_DECLARED", "CUSTOMER_CONFIRMED") &&
                it.directPaymentMethod == "CASH"
        }.sumOf {
            if (it.directPaymentDeclaredPaise > 0L) it.directPaymentDeclaredPaise else it.barberServicePayablePaise
        }
    }

    val upiReceivedPaise = remember(completedJobs) {
        completedJobs.filter {
            it.directPaymentStatus in setOf("UPI_DECLARED", "CUSTOMER_CONFIRMED") &&
                it.directPaymentMethod == "UPI"
        }.sumOf {
            if (it.directPaymentDeclaredPaise > 0L) it.directPaymentDeclaredPaise else it.barberServicePayablePaise
        }
    }

    val totalMarkedReceivedPaise = cashReceivedPaise + upiReceivedPaise

    val unconfirmedOrDisputedCount = remember(completedJobs) {
        completedJobs.count {
            it.directPaymentStatus in setOf("PENDING", "UNPAID_DECLARED", "PARTIAL_DECLARED", "DISPUTED") ||
                it.status == "DISPUTED"
        }
    }

    val weekAgoKey = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    val monthPrefix = remember {
        SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
    }

    val weeklyServiceTotalPaise = remember(completedJobs, weekAgoKey) {
        completedJobs.filter { it.dateKey >= weekAgoKey }.sumOf { it.barberServicePayablePaise }
    }
    val monthlyServiceTotalPaise = remember(completedJobs, monthPrefix) {
        completedJobs.filter { it.dateKey.startsWith(monthPrefix) }.sumOf { it.barberServicePayablePaise }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("barber_earnings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "DIRECT SERVICE LEDGER",
                title = "Barber Earnings",
                subtitle = "Track cash and UPI payments collected directly from your customers.",
                modifier = Modifier.widthIn(max = 600.dp)
            )
        }

        // Mandatory Direct-Payment Record Disclaimer Banner
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = EspressoHeaderBg,
                contentColor = Color(0xFFF8F3EC),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
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
                        text = "Direct Payment Records Only: These figures track direct customer-to-barber payments, not platform-held funds. Marking a payment as UPI records your declaration and does not verify bank settlement.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFF8F3EC)
                    )
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
            ) {
                MetricBentoCard(
                    icon = Icons.Filled.CheckCircle,
                    title = "Completed Services",
                    value = "${completedJobs.size}",
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

        if (completedJobs.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "No Completed Jobs Yet",
                    subtitle = "Once you finish a service and record direct Cash or UPI payment, each receipt appears here."
                )
            }
        } else {
            items(completedJobs, key = { it.id }) { job ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
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
