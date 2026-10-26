package com.example.ui.barber

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BarberProfile
import com.example.data.model.Booking
import com.example.data.model.CurrencyUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.util.DoorCutPreferences

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarberScheduleScreen(
    barberProfile: BarberProfile?,
    bookings: List<Booking>,
    dateOptions: List<Pair<String, String>>,
    isBusy: Boolean,
    onOpenBookingDetail: (String) -> Unit,
    onSaveWorkingHours: (Long, Long, Long, Long, Long) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { DoorCutPreferences.ensureLoaded(context) }
    val localPrefs by DoorCutPreferences.state.collectAsStateWithLifecycle()

    val todayKey = remember { MarketplaceViewModel.defaultTodayDateKey() }
    var selectedDateKey by rememberSaveable { mutableStateOf(todayKey) }

    var startHr by remember(barberProfile?.workStartHour) {
        mutableStateOf((barberProfile?.workStartHour ?: 9L).toString())
    }
    var endHr by remember(barberProfile?.workEndHour) {
        mutableStateOf((barberProfile?.workEndHour ?: 20L).toString())
    }
    var breakStart by remember(barberProfile?.breakStartHour) {
        mutableStateOf((barberProfile?.breakStartHour ?: 13L).toString())
    }
    var breakEnd by remember(barberProfile?.breakEndHour) {
        mutableStateOf((barberProfile?.breakEndHour ?: 14L).toString())
    }
    var bufferMin by remember(barberProfile?.travelBufferMinutes) {
        mutableStateOf((barberProfile?.travelBufferMinutes ?: 15L).toString())
    }

    val agendaForSelectedDate = remember(bookings, selectedDateKey) {
        bookings.filter {
            it.dateKey == selectedDateKey && it.status !in setOf("CANCELLED", "EXPIRED")
        }.sortedBy { it.startHour }
    }

    val upcomingLaterBookings = remember(bookings, todayKey) {
        bookings.filter {
            it.dateKey > todayKey && it.status !in setOf("CANCELLED", "EXPIRED")
        }.sortedWith(compareBy({ it.dateKey }, { it.startHour }))
    }

    val isDateMarkedLeave = localPrefs.barberUnavailableDates.contains(selectedDateKey)
    val startHourLong = startHr.toLongOrNull() ?: 9L
    val endHourLong = endHr.toLongOrNull() ?: 20L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("barber_schedule_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "DAILY AGENDA",
                title = "My Schedule",
                subtitle = "View today's appointments in time order, block leave hours, and set travel buffers.",
                modifier = Modifier.widthIn(max = 600.dp)
            )
        }

        // Date Picker Row + Mark Unavailable Date
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dateOptions.take(7), key = { it.first }) { (key, label) ->
                            val isLeave = localPrefs.barberUnavailableDates.contains(key)
                            FilterChip(
                                selected = selectedDateKey == key,
                                onClick = { selectedDateKey = key },
                                label = { Text(if (isLeave) "$label (Off)" else label) },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { DoorCutPreferences.toggleBarberUnavailableDate(context, selectedDateKey) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("toggle_unavailable_date_button")
                    ) {
                        Icon(Icons.Filled.EventBusy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isDateMarkedLeave) "Unmark Leave Date ($selectedDateKey)"
                            else "Mark $selectedDateKey as Leave / Unavailable"
                        )
                    }
                }
            }
        }

        // Daily Agenda in Time Order
        if (agendaForSelectedDate.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.CalendarMonth,
                    title = if (isDateMarkedLeave) "Marked on Leave" else "No Appointments on $selectedDateKey",
                    subtitle = "Confirmed home appointments appear here in chronological order."
                )
            }
        } else {
            items(agendaForSelectedDate, key = { it.id }) { appt ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .clickable { onOpenBookingDetail(appt.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${CurrencyUtils.formatHourSlot(appt.startHour)} · ${appt.customerName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${appt.serviceNamesSummary} (${appt.durationMinutes} min) · ${CurrencyUtils.formatRupees(appt.barberServicePayablePaise)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(appt.status)
                    }
                }
            }
        }

        // Block Specific Hours
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Blocked Hours on $selectedDateKey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any slot to block or reopen it for personal breaks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (h in startHourLong until endHourLong.coerceAtLeast(startHourLong + 1L)) {
                            val blocked = localPrefs.barberBlockedSlots.contains("${selectedDateKey}_$h")
                            FilterChip(
                                selected = blocked,
                                onClick = { DoorCutPreferences.toggleBarberBlockedSlot(context, selectedDateKey, h) },
                                label = {
                                    Text(
                                        if (blocked) "${CurrencyUtils.formatHourSlot(h)} (Blocked)"
                                        else CurrencyUtils.formatHourSlot(h)
                                    )
                                },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                }
            }
        }

        // Upcoming Appointments (Future Days)
        if (upcomingLaterBookings.isNotEmpty()) {
            item {
                Text(
                    text = "Upcoming Days (${upcomingLaterBookings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                )
            }
            items(upcomingLaterBookings.take(5), key = { "up_${it.id}" }) { b ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .clickable { onOpenBookingDetail(b.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${b.dateKey} · ${CurrencyUtils.formatHourSlot(b.startHour)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${b.customerName} · ${b.serviceNamesSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(b.status)
                    }
                }
            }
        }

        // Set Working Hours & Travel Buffer
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Working Hours & Travel Buffer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHr,
                            onValueChange = { startHr = it.filter { c -> c.isDigit() } },
                            label = { Text("Start Hour (24h)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endHr,
                            onValueChange = { endHr = it.filter { c -> c.isDigit() } },
                            label = { Text("End Hour (24h)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = breakStart,
                            onValueChange = { breakStart = it.filter { c -> c.isDigit() } },
                            label = { Text("Break Start") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = breakEnd,
                            onValueChange = { breakEnd = it.filter { c -> c.isDigit() } },
                            label = { Text("Break End") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bufferMin,
                            onValueChange = { bufferMin = it.filter { c -> c.isDigit() } },
                            label = { Text("Buffer (min)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            onSaveWorkingHours(
                                startHr.toLongOrNull() ?: 9L,
                                endHr.toLongOrNull() ?: 20L,
                                breakStart.toLongOrNull() ?: 13L,
                                breakEnd.toLongOrNull() ?: 14L,
                                bufferMin.toLongOrNull() ?: 15L
                            )
                        },
                        enabled = !isBusy && barberProfile != null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_working_hours_button")
                    ) {
                        Text("Save Working Hours & Buffer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
