package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.BarberProfile
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.ServiceArea
import com.example.data.model.SlotLock
import com.example.data.model.UserAccount
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.CustomerDirectPriceCard
import com.example.ui.components.SkillPill
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.util.LocationHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomerCheckoutFlowScreen(
    viewModel: MarketplaceViewModel,
    userAccount: UserAccount,
    selectedServices: List<CatalogService>,
    rankedBarbers: List<Pair<BarberProfile, Double>>,
    addresses: List<CustomerAddress>,
    serviceAreas: List<ServiceArea>,
    slotLocks: List<SlotLock>,
    platformConfig: PlatformConfig,
    selectedBarberId: String?,
    selectedAddressId: String?,
    selectedDateKey: String,
    selectedStartHour: Long?,
    isBusy: Boolean,
    onBack: () -> Unit,
    onOpenDoorModal: () -> Unit,
    onNavigateToAddresses: () -> Unit,
    onBookingConfirmed: () -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(true) }

    val activeAddress = addresses.firstOrNull { it.id == selectedAddressId }
        ?: addresses.firstOrNull { it.isDefault }
        ?: addresses.firstOrNull()

    val matchedZone = serviceAreas.firstOrNull { zone ->
        activeAddress != null && (zone.id == activeAddress.serviceAreaId || zone.postalCodes.contains(activeAddress.postalCode))
    }
    val travelChargePaise = matchedZone?.travelChargePaise ?: 0L

    val eligibleBarbers = remember(rankedBarbers, selectedServices) {
        val reqIds = selectedServices.map { it.id }
        rankedBarbers.filter { (b, _) ->
            b.isOnline && (b.serviceIds.isEmpty() || b.serviceIds.containsAll(reqIds))
        }
    }

    val chosenPair = eligibleBarbers.firstOrNull { it.first.barberId == selectedBarberId }
        ?: eligibleBarbers.firstOrNull()
    val chosenBarber = chosenPair?.first

    val availableSlots = remember(chosenBarber, selectedDateKey, slotLocks) {
        if (chosenBarber == null) emptyList()
        else viewModel.availableSlotsForBarber(chosenBarber, selectedDateKey, slotLocks)
    }

    val serviceSubtotalPaise = selectedServices.sumOf { it.pricePaise }
    val barberPayablePaise = serviceSubtotalPaise + travelChargePaise
    val barberCommissionPaise = viewModel.calculateBarberCommissionPaise(barberPayablePaise, platformConfig)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Editorial Checkout Top Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .testTag("checkout_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ATELIER RESERVATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Confirm Home Visit Slot",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹0 Booking Fee • Pay Barber Directly After Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = SageVerified,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Step 1: Doorstep Address Card
        CheckoutStepCard(
            stepNumber = "1",
            title = "Your Doorstep Address",
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onOpenDoorModal,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (addresses.isEmpty()) "Auto-GPS Door" else "Add Door")
                    }
                    if (addresses.isNotEmpty()) {
                        TextButton(onClick = onNavigateToAddresses) {
                            Text("Manage")
                        }
                    }
                }
            }
        ) {
            if (activeAddress == null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Please confirm your door/flat number and home address before locking a barber slot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    addresses.forEach { addr ->
                        val isChosen = addr.id == activeAddress.id
                        FilterChip(
                            selected = isChosen,
                            onClick = { viewModel.selectAddress(addr.id) },
                            label = { Text("${addr.label}: ${addr.flatHouse}") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${activeAddress.flatHouse}, ${activeAddress.streetArea}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${activeAddress.landmark} • PIN ${activeAddress.postalCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (matchedZone != null) {
                                    "✓ ${matchedZone.name} • Travel Allowance: ${CurrencyUtils.formatRupees(travelChargePaise)}"
                                } else {
                                    "✓ Home Visit Coverage Active (${activeAddress.serviceAreaName})"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = SageVerified
                            )
                        }
                    }
                }
            }
        }

        // Step 2: Specialist & Shop Selection
        CheckoutStepCard(
            stepNumber = "2",
            title = "Select Nearest Master Barber"
        ) {
            if (eligibleBarbers.isEmpty()) {
                Text(
                    text = "No online barber currently offers all selected services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    eligibleBarbers.forEach { (barber, distKm) ->
                        val isPicked = chosenBarber?.barberId == barber.barberId
                        Surface(
                            onClick = { viewModel.selectBarber(barber.barberId) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isPicked) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = if (isPicked) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BarberAvatarBadge(
                                        name = barber.name,
                                        isOnline = barber.isOnline,
                                        size = 44.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${barber.name} • ${LocationHelper.formatDistanceKm(distKm)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = BrassGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = " ${String.format("%.1f", barber.ratingAvg)} • ${barber.effectiveShopName()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isPicked) StatusBadge("SELECTED")
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    barber.effectiveSkills().take(3).forEach { skill ->
                                        SkillPill(skill = skill)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step 3: Calendar Date Cards & Time Slot Grid
        CheckoutStepCard(
            stepNumber = "3",
            title = "Date & Appointment Window"
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Choose Visit Date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.upcomingDateOptions()) { (key, label) ->
                    val isDateSelected = selectedDateKey == key
                    val parts = label.split(",")
                    val dayLine = parts.firstOrNull()?.trim().orEmpty()
                    val dateLine = parts.getOrNull(1)?.trim() ?: key.takeLast(5)
                    Surface(
                        onClick = { viewModel.selectDate(key) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDateSelected) EspressoHeaderBg else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isDateSelected) 1.5.dp else 1.dp,
                            color = if (isDateSelected) BrassGold else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.width(92.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayLine.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDateSelected) Color(0xFFFFDF9E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateLine,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDateSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Available Barber Slots",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (availableSlots.isEmpty()) {
                Text(
                    text = "No open slots on $selectedDateKey. Try another date or specialist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSlots.forEach { hr ->
                        val isSlotPicked = selectedStartHour == hr
                        Surface(
                            onClick = { viewModel.selectStartHour(hr) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSlotPicked) EspressoHeaderBg else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                width = if (isSlotPicked) 1.5.dp else 1.dp,
                                color = if (isSlotPicked) BrassGold else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.testTag("slot_chip_$hr")
                        ) {
                            Text(
                                text = CurrencyUtils.formatHourSlot(hr),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSlotPicked) Color(0xFFFFDF9E) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Grooming Style Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Grooming Notes (e.g., mid skin fade, keep crown length)") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Transparent Zero-Fee Receipt
        CustomerDirectPriceCard(
            serviceSubtotalPaise = serviceSubtotalPaise,
            approvedAddOnPaise = 0L,
            travelChargePaise = travelChargePaise,
            barberServicePayablePaise = barberPayablePaise
        )

        // Policy Agreement Card
        Card(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = platformConfig.policyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
                    Text(
                        text = "I agree to pay ${CurrencyUtils.formatRupees(barberPayablePaise)} directly to the barber after service",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        val canConfirm = !isBusy &&
            activeAddress != null &&
            chosenBarber != null &&
            selectedStartHour != null &&
            acceptedTerms &&
            !platformConfig.maintenanceMode

        Button(
            onClick = {
                if (activeAddress != null && chosenBarber != null && selectedStartHour != null) {
                    viewModel.confirmZeroFeeBooking(
                        customerName = userAccount.name,
                        customerPhone = userAccount.phone.ifBlank { "Shared on visit" },
                        barber = chosenBarber,
                        selectedServices = selectedServices,
                        address = activeAddress,
                        travelChargePaise = travelChargePaise,
                        barberCommissionPaise = barberCommissionPaise,
                        dateKey = selectedDateKey,
                        startHour = selectedStartHour,
                        customerNotes = notes,
                        policySnapshot = platformConfig.policyText,
                        onSuccess = { onBookingConfirmed() }
                    )
                }
            },
            enabled = canConfirm,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EspressoHeaderBg,
                contentColor = Color(0xFFFFDF9E)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("confirm_zero_fee_booking_button")
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = BrassGold,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Confirm Home Visit • ₹0 Booking Fee",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CheckoutStepCard(
    stepNumber: String,
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(EspressoHeaderBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFDF9E)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (trailing != null) {
                    trailing()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
