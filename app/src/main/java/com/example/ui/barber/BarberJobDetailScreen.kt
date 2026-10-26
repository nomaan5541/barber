package com.example.ui.barber

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.model.Booking
import com.example.data.model.BookingPrivateAddress
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified

@Composable
fun BarberJobDetailScreen(
    booking: Booking?,
    protectedAddress: BookingPrivateAddress?,
    catalogServices: List<CatalogService>,
    isBusy: Boolean,
    onLoadProtectedAddress: (String) -> Unit,
    onAdvanceTripStatus: (String, String) -> Unit,
    onFinishService: (String) -> Unit,
    onRequestExtraService: (String, CatalogService) -> Unit,
    onRecordDirectPayment: (String, String, String, Long, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    LaunchedEffect(booking?.id) {
        if (booking != null && booking.id.isNotBlank()) {
            onLoadProtectedAddress(booking.id)
        }
    }

    var selectedPaymentOutcome by remember { mutableStateOf("CASH") }
    var amountRupeesInput by remember(booking?.barberServicePayablePaise) {
        mutableStateOf(((booking?.barberServicePayablePaise ?: 0L) / 100L).toString())
    }
    var paymentNoteInput by remember { mutableStateOf("") }
    var showAddExtraPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("barber_job_detail_screen")
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
                    .testTag("barber_job_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Booking Details & Job Flow",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (booking != null) {
                    Text(
                        text = "${booking.dateKey} · ${CurrencyUtils.formatHourSlot(booking.startHour)}",
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (booking == null) {
            EmptyStateCard(
                icon = Icons.Filled.LocationOn,
                title = "Appointment Not Found",
                subtitle = "Return to your Barber Dashboard to view active appointments."
            )
            return@Column
        }

        val fullAddressText = protectedAddress?.fullAddressSnapshot?.ifBlank { booking.areaName }
            ?: booking.areaName.ifBlank { "Customer Home Address" }
        val customerPhone = protectedAddress?.customerPhone?.ifBlank { "+919876543210" } ?: "+919876543210"

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Customer, Full Address, Landmark & Navigation Card (All on 1 page!)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = EspressoHeaderBg),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = booking.customerName.ifBlank { "Customer" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = fullAddressText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFF8F3EC)
                        )
                        if (!protectedAddress?.accessInstructions.isNullOrBlank()) {
                            Text(
                                text = "Instructions: ${protectedAddress?.accessInstructions}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFFDF9E),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(fullAddressText)}")
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
                                    .testTag("job_detail_navigate_button")
                            ) {
                                Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Directions", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$customerPhone")))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BrassGold),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFDF9E)),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                                    .testTag("job_detail_call_customer_button")
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = "Call Customer", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call")
                            }
                            OutlinedButton(
                                onClick = {
                                    val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$customerPhone"))
                                    runCatching { context.startActivity(sms) }
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BrassGold),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFDF9E)),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message Customer", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SMS")
                            }
                        }
                    }
                }
            }

            // 2. Services & Direct Amount to Collect Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
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
                                        text = "Cash or UPI after finishing service",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = CurrencyUtils.formatRupees(booking.barberServicePayablePaise),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 3. Guided Service Execution Sequence (Steps 1–5)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Guided Job Workflow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        when (booking.status) {
                            "CONFIRMED" -> {
                                Button(
                                    onClick = { onAdvanceTripStatus(booking.id, "ON_THE_WAY") },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("step_on_my_way_button")
                                ) {
                                    Text("On My Way", fontWeight = FontWeight.Bold)
                                }
                            }
                            "ON_THE_WAY" -> {
                                Button(
                                    onClick = { onAdvanceTripStatus(booking.id, "ARRIVED") },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("step_start_service_button")
                                ) {
                                    Text("2. Start Service", fontWeight = FontWeight.Bold)
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
                                        .testTag("request_extra_service_toggle")
                                ) {
                                    Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Request Customer Approval for Extra Service")
                                }

                                if (showAddExtraPicker) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(catalogServices.take(6), key = { it.id }) { svc ->
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    onRequestExtraService(booking.id, svc)
                                                    showAddExtraPicker = false
                                                },
                                                label = { Text("${svc.name} (+${CurrencyUtils.formatRupees(svc.pricePaise)})") }
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
                                        text = "Service completion recorded! Record or update direct payment below.",
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

            // 4. Separate Step 6: Record Direct Payment Method & Amount (decoupled from service completion!)
            if (booking.status in setOf("COMPLETED", "DISPUTED")) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.5.dp, BrassGold),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
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

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "CASH" to "Cash",
                                    "UPI" to "UPI",
                                    "UNPAID" to "Unconfirmed",
                                    "DISPUTED" to "Disputed"
                                ).forEach { (key, label) ->
                                    FilterChip(
                                        selected = selectedPaymentOutcome == key,
                                        onClick = { selectedPaymentOutcome = key },
                                        label = { Text(label) },
                                        modifier = Modifier.height(48.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = amountRupeesInput,
                                onValueChange = { amountRupeesInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Direct Amount Received (₹)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = paymentNoteInput,
                                onValueChange = { paymentNoteInput = it },
                                label = { Text("Optional UPI Ref / Note") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val paise = (amountRupeesInput.toLongOrNull() ?: 0L) * 100L
                                    val (declaredStatus, method) = when (selectedPaymentOutcome) {
                                        "CASH" -> "CASH_DECLARED" to "CASH"
                                        "UPI" -> "UPI_DECLARED" to "UPI"
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("save_direct_payment_record_button")
                            ) {
                                Text("Save Direct Payment Record", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
