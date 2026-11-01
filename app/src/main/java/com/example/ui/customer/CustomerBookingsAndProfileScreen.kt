package com.example.ui.customer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Booking
import com.example.data.model.CurrencyUtils
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.glass.LocalLiquidGlassPalette
import com.example.ui.motion.DoorCutMotionSpecs
import com.example.ui.motion.LocalReducedMotion
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.BrassGoldLight
import com.example.ui.theme.EspressoHeaderBg

/**
 * Customer My Bookings page split into three simple tabs with a Living Liquid Glass
 * sliding capsule tab selector and tactile appointment cards:
 * 0 -> Upcoming (bookings that have not finished)
 * 1 -> Completed (previous services with Book Again + Rate Barber)
 * 2 -> Cancelled (cancelled/expired appointments)
 */
@Composable
fun CustomerBookingsScreen(
    customerName: String,
    bookings: List<Booking>,
    isBusy: Boolean,
    onOpenBookingStatus: (String) -> Unit,
    onBookAgain: (Booking) -> Unit,
    onSubmitReview: (Booking, Long, String) -> Unit,
    onStartNewBooking: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val dark = LocalLiquidGlassPalette.current.isDark
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var reviewingBooking by remember { mutableStateOf<Booking?>(null) }
    var ratingStars by remember { mutableLongStateOf(5L) }
    var reviewComment by remember { mutableStateOf("") }

    val sortedBookings = remember(bookings) {
        bookings.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }
    val upcomingList = remember(sortedBookings) { sortedBookings.filter { it.isUpcoming() } }
    val completedList = remember(sortedBookings) { sortedBookings.filter { it.isCompletedTab() } }
    val cancelledList = remember(sortedBookings) { sortedBookings.filter { it.isCancelledTab() } }

    val activeList = when (selectedTab) {
        0 -> upcomingList
        1 -> completedList
        else -> cancelledList
    }

    val tabTitles = listOf(
        "Upcoming (${upcomingList.size})",
        "Completed (${completedList.size})",
        "Cancelled (${cancelledList.size})"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_bookings_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Liquid Glass 3-Segment Sliding Tab Pill
        Box(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            LiquidGlassSurface(
                strength = GlassMaterialStrength.ChromeBar,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 3.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    val segmentWidth = maxWidth / 3
                    val pillOffset by animateDpAsState(
                        targetValue = segmentWidth * selectedTab,
                        animationSpec = DoorCutMotionSpecs.liquidPillSpring(reducedMotion),
                        label = "bookings_tab_pill_offset"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = pillOffset)
                            .width(segmentWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = if (dark) {
                                        listOf(Color(0xFF3A2C16), Color(0xFF281F12))
                                    } else {
                                        listOf(EspressoHeaderBg, Color(0xFF332619))
                                    }
                                )
                            )
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            val tag = when (index) {
                                0 -> "bookings_tab_upcoming"
                                1 -> "bookings_tab_completed"
                                else -> "bookings_tab_cancelled"
                            }
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) BrassGoldLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(160),
                                label = "bookings_tab_text_$index"
                            )
                            val interactionSource = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .semantics { this.selected = isSelected }
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        role = Role.Tab,
                                        onClick = { selectedTab = index }
                                    )
                                    .testTag(tag),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeList.isEmpty()) {
                item {
                    val (emptyTitle, emptySubtitle) = when (selectedTab) {
                        0 -> "No Upcoming Appointments" to "Book a verified home barber in under a minute."
                        1 -> "No Completed Visits Yet" to "Your finished haircuts and grooming history will appear here."
                        else -> "No Cancelled Bookings" to "You have no cancelled appointments."
                    }
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        title = emptyTitle,
                        subtitle = emptySubtitle,
                        action = if (selectedTab == 0) {
                            {
                                Button(
                                    onClick = onStartNewBooking,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EspressoHeaderBg,
                                        contentColor = Color(0xFFFFDF9E)
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .tactilePressPhysics(enableHaptics = true)
                                        .testTag("empty_bookings_book_now_button")
                                ) {
                                    Text("Book a Barber", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else null
                    )
                }
            } else {
                itemsIndexed(
                    items = activeList,
                    key = { _, item -> item.id.ifBlank { item.bookingNumber } }
                ) { index, booking ->
                    CustomerBookingItemCard(
                        booking = booking,
                        index = index,
                        isCompleted = selectedTab == 1,
                        isBusy = isBusy,
                        onOpenStatus = { onOpenBookingStatus(booking.id) },
                        onBookAgain = { onBookAgain(booking) },
                        onOpenReview = {
                            reviewingBooking = booking
                            ratingStars = 5L
                            reviewComment = ""
                        }
                    )
                }
            }
        }
    }

    val targetReview = reviewingBooking
    if (targetReview != null) {
        AlertDialog(
            onDismissRequest = { reviewingBooking = null },
            title = { Text("Rate ${targetReview.barberName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1L..5L).forEach { star ->
                            IconButton(
                                onClick = { ratingStars = star },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "$star stars",
                                    tint = if (star <= ratingStars) BrassGold else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("How was your haircut?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitReview(
                            targetReview,
                            ratingStars,
                            reviewComment.ifBlank { "Great home service!" }
                        )
                        reviewingBooking = null
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Submit Review", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { reviewingBooking = null },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
private fun CustomerBookingItemCard(
    booking: Booking,
    index: Int,
    isCompleted: Boolean,
    isBusy: Boolean,
    onOpenStatus: () -> Unit,
    onBookAgain: () -> Unit,
    onOpenReview: () -> Unit
) {
    LiquidGlassSurface(
        strength = if (booking.isUpcoming()) GlassMaterialStrength.AccentEmerald else GlassMaterialStrength.AtelierCard,
        shape = RoundedCornerShape(22.dp),
        onClick = onOpenStatus,
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .staggeredSpringEntrance(index = index)
            .testTag("booking_card_${booking.id}")
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val stylistLine = when {
                        booking.isOpenDispatch && booking.preferLadyBarber -> "Matching Lady Hair Stylist..."
                        booking.isOpenDispatch -> "Matching Nearby Verified Barber..."
                        else -> "Stylist: ${booking.barberName.ifBlank { "Verified Stylist" }}"
                    }
                    BarberAvatarBadge(
                        name = if (booking.isOpenDispatch) "DoorCut" else booking.barberName.ifBlank { "Stylist" },
                        isOnline = booking.isUpcoming(),
                        size = 44.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = booking.serviceNamesSummary.ifBlank { "Home Grooming" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stylistLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(booking.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (booking.bookingMode == "ASAP") {
                            "ASAP Express · ${booking.dateKey}"
                        } else {
                            "${booking.dateKey} · ${CurrencyUtils.formatHourSlot(booking.startHour)}"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatRupees(booking.barberServicePayablePaise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pay barber directly",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = booking.friendlyStatusText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (booking.isUpcoming()) {
                    Button(
                        onClick = onOpenStatus,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EspressoHeaderBg,
                            contentColor = Color(0xFFFFDF9E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tactilePressPhysics(enableHaptics = true)
                            .testTag("track_booking_button_${booking.id}")
                    ) {
                        Text("Track Live Status", fontWeight = FontWeight.Bold)
                    }
                } else if (isCompleted) {
                    Button(
                        onClick = onBookAgain,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EspressoHeaderBg,
                            contentColor = Color(0xFFFFDF9E)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .tactilePressPhysics(enableHaptics = true)
                            .testTag("book_again_button_${booking.id}")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Book Again", fontWeight = FontWeight.Bold)
                    }
                    if (!booking.hasReview) {
                        OutlinedButton(
                            onClick = onOpenReview,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .tactilePressPhysics()
                                .testTag("rate_barber_button_${booking.id}")
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = BrassGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rate Visit")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenStatus,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tactilePressPhysics()
                    ) {
                        Text("View Details")
                    }
                }
            }
        }
    }
}
