package com.example.ui.barber

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassPill
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.util.DoorCutPreferences
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val ALL_WEEKDAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

private fun formatMinutesClock(totalMinutes: Long): String {
    val normalized = ((totalMinutes % (24L * 60L)) + (24L * 60L)) % (24L * 60L)
    val h24 = (normalized / 60L).toInt()
    val mins = (normalized % 60L).toInt()
    val period = if (h24 >= 12) "PM" else "AM"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return String.format(Locale.US, "%d:%02d %s", h12, mins, period)
}

private fun resolveDayCodeForDateKey(dateKey: String): String? {
    return runCatching {
        val parsed = LocalDate.parse(dateKey)
        parsed.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(Locale.US).take(3)
    }.getOrNull()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarberScheduleScreen(
    barberProfile: BarberProfile?,
    bookings: List<Booking>,
    dateOptions: List<Pair<String, String>>,
    isBusy: Boolean,
    onOpenBookingDetail: (String) -> Unit,
    onSaveWorkingHours: (Long, Long, Long, Long, Long) -> Unit,
    onSaveScheduleWithDays: ((Long, Long, Long, Long, Long, List<String>) -> Unit)? = null
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
    var selectedWorkingDays by remember(barberProfile?.workingDays) {
        val initial = barberProfile?.workingDays?.filter { it in ALL_WEEKDAYS }
        mutableStateOf(
            if (initial.isNullOrEmpty()) listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
            else initial
        )
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
    val selectedDayCode = remember(selectedDateKey) { resolveDayCodeForDateKey(selectedDateKey) }
    val isWeeklyOffDay = selectedDayCode != null && selectedDayCode !in selectedWorkingDays

    val startHourLong = (startHr.toLongOrNull() ?: 9L).coerceIn(0L, 23L)
    val endHourLong = (endHr.toLongOrNull() ?: 20L).coerceIn((startHourLong + 1L).coerceAtMost(24L), 24L)
    val breakStartLong = (breakStart.toLongOrNull() ?: 13L).coerceIn(0L, 23L)
    val breakEndLong = (breakEnd.toLongOrNull() ?: 14L).coerceIn(0L, 24L)
    val activeTravelBufferMin = (bufferMin.toLongOrNull() ?: 15L).coerceIn(0L, 180L)

    val dayCommittedMinutes = remember(agendaForSelectedDate, activeTravelBufferMin) {
        agendaForSelectedDate.sumOf { it.durationMinutes.coerceAtLeast(30L) + activeTravelBufferMin }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("barber_schedule_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "PARTNER DISPATCH CALENDAR",
                title = "My Schedule & Shift",
                subtitle = "Manage your weekly shift days, travel buffers between customer homes, and daily blocked slots.",
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        // Date Picker Row + Mark Unavailable Date in Atelier Liquid Glass
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 1)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dateOptions.take(7), key = { it.first }) { (key, label) ->
                            val isLeave = localPrefs.barberUnavailableDates.contains(key)
                            val dayCode = resolveDayCodeForDateKey(key)
                            val isRest = dayCode != null && dayCode !in selectedWorkingDays
                            val displayLabel = when {
                                isLeave -> "$label (Leave)"
                                isRest -> "$label (Rest)"
                                else -> label
                            }
                            LiquidGlassPill(
                                text = displayLabel,
                                selected = selectedDateKey == key,
                                onClick = { selectedDateKey = key },
                                modifier = Modifier.testTag("schedule_date_pill_$key")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${agendaForSelectedDate.size} visits · ${dayCommittedMinutes}m total route time",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isWeeklyOffDay && !isDateMarkedLeave) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "Weekly Rest ($selectedDayCode)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { DoorCutPreferences.toggleBarberUnavailableDate(context, selectedDateKey) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tactilePressPhysics(enableHaptics = true)
                            .testTag("toggle_unavailable_date_button")
                    ) {
                        Icon(Icons.Filled.EventBusy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isDateMarkedLeave) "Unmark Leave Date ($selectedDateKey)"
                            else "Mark $selectedDateKey as Leave / Unavailable",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Daily Agenda in Time Order with Service + Travel Buffer Window
        if (agendaForSelectedDate.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.CalendarMonth,
                        title = when {
                            isDateMarkedLeave -> "Marked on Personal Leave"
                            isWeeklyOffDay -> "Weekly Rest Day ($selectedDayCode)"
                            else -> "No Appointments on $selectedDateKey"
                        },
                        subtitle = "Confirmed home appointments appear here in chronological order with travel buffer blocks."
                    )
                }
            }
        } else {
            itemsIndexed(agendaForSelectedDate, key = { _, it -> it.id }) { idx, appt ->
                val startMin = appt.startHour * 60L
                val serviceEndMin = startMin + appt.durationMinutes.coerceAtLeast(30L)
                val readyNextMin = serviceEndMin + activeTravelBufferMin
                val netTakeHome = if (appt.barberNetEarningsPaise > 0L) {
                    appt.barberNetEarningsPaise
                } else {
                    appt.barberServicePayablePaise
                }

                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp,
                    onClick = { onOpenBookingDetail(appt.id) },
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = idx + 1)
                        .testTag("schedule_appt_card_${appt.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${formatMinutesClock(startMin)} – ${formatMinutesClock(serviceEndMin)} · ${appt.customerName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${appt.serviceNamesSummary} (${appt.durationMinutes} min) · Net ${CurrencyUtils.formatRupees(netTakeHome)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(appt.status)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.DirectionsBike,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${activeTravelBufferMin}m travel · Next slot ≥ ${formatMinutesClock(readyNextMin)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (appt.areaName.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = appt.areaName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Block Specific Hours with Tactile Liquid Glass Capsules
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 2)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Hourly Slot Control on $selectedDateKey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any slot to block or reopen it. Daily meal break hours are highlighted automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (h in startHourLong until endHourLong.coerceAtLeast(startHourLong + 1L)) {
                            val blocked = localPrefs.barberBlockedSlots.contains("${selectedDateKey}_$h")
                            val isBreakHour = breakEndLong > breakStartLong && h in breakStartLong until breakEndLong
                            val bookedAtHour = agendaForSelectedDate.any { it.startHour == h }
                            val chipLabel = when {
                                blocked -> "${CurrencyUtils.formatHourSlot(h)} (Blocked)"
                                bookedAtHour -> "${CurrencyUtils.formatHourSlot(h)} (Booked)"
                                isBreakHour -> "${CurrencyUtils.formatHourSlot(h)} (Break)"
                                else -> CurrencyUtils.formatHourSlot(h)
                            }
                            LiquidGlassPill(
                                text = chipLabel,
                                selected = blocked || isBreakHour || bookedAtHour,
                                selectedContainerColor = when {
                                    blocked -> MaterialTheme.colorScheme.errorContainer
                                    bookedAtHour -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                selectedContentColor = when {
                                    blocked -> MaterialTheme.colorScheme.onErrorContainer
                                    bookedAtHour -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                onClick = { DoorCutPreferences.toggleBarberBlockedSlot(context, selectedDateKey, h) },
                                modifier = Modifier.testTag("blocked_slot_chip_$h")
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
            itemsIndexed(upcomingLaterBookings.take(5), key = { _, b -> "up_${b.id}" }) { idx, b ->
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 2.dp,
                    onClick = { onOpenBookingDetail(b.id) },
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = idx + 1)
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

        // Weekly Working Days, Working Hours & Travel Buffer in Atelier Liquid Glass
        item {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 3)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Weekly Shift, Break & Travel Buffer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Active Working Days (${selectedWorkingDays.size}/7 days)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("working_days_selector_row")
                    ) {
                        ALL_WEEKDAYS.forEach { dayCode ->
                            val isSelected = dayCode in selectedWorkingDays
                            LiquidGlassPill(
                                text = dayCode,
                                selected = isSelected,
                                onClick = {
                                    selectedWorkingDays = if (isSelected && selectedWorkingDays.size > 1) {
                                        selectedWorkingDays - dayCode
                                    } else if (!isSelected) {
                                        ALL_WEEKDAYS.filter { it in (selectedWorkingDays + dayCode) }
                                    } else {
                                        selectedWorkingDays
                                    }
                                },
                                modifier = Modifier.testTag("working_day_chip_$dayCode")
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHr,
                            onValueChange = { startHr = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Start Hour (24h)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_start_hour_input")
                        )
                        OutlinedTextField(
                            value = endHr,
                            onValueChange = { endHr = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("End Hour (24h)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_end_hour_input")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = breakStart,
                            onValueChange = { breakStart = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Break Start") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_break_start_input")
                        )
                        OutlinedTextField(
                            value = breakEnd,
                            onValueChange = { breakEnd = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Break End") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_break_end_input")
                        )
                        OutlinedTextField(
                            value = bufferMin,
                            onValueChange = { bufferMin = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Buffer (min)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_travel_buffer_input")
                        )
                    }

                    // Quick Travel Buffer Presets for Two-Wheeler Traffic
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Buffer:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(10L, 15L, 30L, 45L).forEach { preset ->
                            LiquidGlassPill(
                                text = "${preset}m",
                                selected = activeTravelBufferMin == preset,
                                onClick = { bufferMin = preset.toString() },
                                modifier = Modifier.testTag("buffer_preset_${preset}m")
                            )
                        }
                    }

                    val canSaveHours = !isBusy && barberProfile != null
                    Button(
                        onClick = {
                            val sH = (startHr.toLongOrNull() ?: 9L).coerceIn(0L, 23L)
                            val eH = (endHr.toLongOrNull() ?: 20L).coerceIn((sH + 1L).coerceAtMost(24L), 24L)
                            val bS = (breakStart.toLongOrNull() ?: 13L).coerceIn(0L, 23L)
                            val bE = (breakEnd.toLongOrNull() ?: 14L).coerceIn(bS.coerceAtMost(24L), 24L)
                            val buf = (bufferMin.toLongOrNull() ?: 15L).coerceIn(0L, 180L)
                            if (onSaveScheduleWithDays != null) {
                                onSaveScheduleWithDays(sH, eH, bS, bE, buf, selectedWorkingDays)
                            } else {
                                onSaveWorkingHours(sH, eH, bS, bE, buf)
                            }
                        },
                        enabled = canSaveHours,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EspressoHeaderBg,
                            contentColor = Color(0xFFFFDF9E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tactilePressPhysics(enabled = canSaveHours, enableHaptics = true)
                            .testTag("save_working_hours_button")
                    ) {
                        Text("Save Shift Days, Hours & Buffer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
