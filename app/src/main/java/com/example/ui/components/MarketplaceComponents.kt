package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.glass.LiveGlassPulseDot
import com.example.ui.motion.DoorCutMotionSpecs
import com.example.ui.motion.LocalReducedMotion
import com.example.ui.theme.AmberPending
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CrimsonDispute
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified

data class ServiceVisualMeta(
    @DrawableRes val drawableRes: Int,
    val badgeLabel: String,
    val visualCaption: String
)

fun serviceVisualFor(service: CatalogService): ServiceVisualMeta {
    val lowerName = service.name.lowercase()
    return when {
        service.category == "LADIES_STYLING" ||
            lowerName.contains("ladies") ||
            lowerName.contains("lady") ||
            lowerName.contains("women") ||
            lowerName.contains("layer") -> ServiceVisualMeta(
            drawableRes = R.drawable.ladies_hair_stylist_1793031670379,
            badgeLabel = "LADY HAIR STYLIST AT HOME",
            visualCaption = "Precision ladies haircut, layers & salon blowout at home"
        )
        service.isPackage || service.category == "PACKAGES" -> ServiceVisualMeta(
            drawableRes = R.drawable.hero_home_grooming_1793031644119,
            badgeLabel = "COMPLETE GROOMING COMBO",
            visualCaption = "Full salon ritual delivered to your home"
        )
        service.category == "HAIRCUTS" ||
            lowerName.contains("haircut") ||
            lowerName.contains("fade") ||
            lowerName.contains("scissor") ||
            lowerName.contains("cut") -> ServiceVisualMeta(
            drawableRes = R.drawable.mens_precision_cut_1793031645963,
            badgeLabel = "AT-HOME HAIRCUTTING",
            visualCaption = "Precision skin fade, taper & scissor tailoring"
        )
        service.category == "BEARD_MOUSTACHE" ||
            lowerName.contains("beard") ||
            lowerName.contains("shave") ||
            lowerName.contains("moustache") -> ServiceVisualMeta(
            drawableRes = R.drawable.beard_razor_shave_1793031671256,
            badgeLabel = "BEARD & RAZOR SCULPT",
            visualCaption = "Sharp contour line-up & hot towel finish"
        )
        else -> ServiceVisualMeta(
            drawableRes = R.drawable.spa_detan_combo_1793031700195,
            badgeLabel = "SCALP SPA & DETAN CARE",
            visualCaption = "Revitalizing champi, detan facial & blow-dry care"
        )
    }
}

@Composable
fun SectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun BarberAvatarBadge(
    name: String,
    isOnline: Boolean = true,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val initials = name
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "DC" }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            EspressoHeaderBg,
                            Color(0xFF3B2D1D)
                        )
                    )
                )
                .border(1.5.dp, BrassGold.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = if (size >= 48.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFDF9E)
            )
        }
        Box(
            modifier = Modifier
                .size(13.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(2.dp)
                .clip(CircleShape)
                .background(if (isOnline) SageVerified else Color(0xFF8C8277))
        )
    }
}

@Composable
fun SkillPill(
    skill: String,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
    ) {
        Text(
            text = skill,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (highlighted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun MetricBentoCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accentHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        strength = if (accentHighlight) GlassMaterialStrength.AccentEmerald else GlassMaterialStrength.AtelierCard,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        interactivePhysics = true,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VisitProgressStepper(
    status: String,
    modifier: Modifier = Modifier
) {
    if (status in listOf("CANCELLED", "REJECTED", "NO_SHOW")) return

    val steps = listOf(
        "Requested",
        "Confirmed",
        "On Way",
        "Grooming",
        "Done"
    )
    val activeStepIndex = when (status) {
        "SLOT_HELD", "PAYMENT_PENDING", "AWAITING_BARBER" -> 0
        "CONFIRMED" -> 1
        "ON_THE_WAY", "ARRIVED" -> 2
        "IN_PROGRESS" -> 3
        "COMPLETED", "DISPUTED" -> 4
        else -> 0
    }

    LiquidGlassSurface(
        strength = GlassMaterialStrength.AtelierCard,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 2.dp,
        interactivePhysics = false,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { idx, label ->
                val isCompleted = idx < activeStepIndex || status == "COMPLETED"
                val isCurrent = idx == activeStepIndex && status != "COMPLETED"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted -> SageVerified
                                    isCurrent -> EspressoHeaderBg
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            )
                            .then(
                                if (isCurrent) Modifier.border(1.dp, BrassGold.copy(alpha = 0.7f), CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color(0xFFFFDF9E)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (idx == 2 && status == "ARRIVED") "Arrived" else label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isCompleted -> SageVerified
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PerforatedReceiptDivider(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        drawLine(
            color = lineColor,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
    }
}

@Composable
fun SixStageBookingTimeline(
    status: String,
    modifier: Modifier = Modifier
) {
    val stages = listOf(
        "AWAITING_BARBER" to "Waiting for barber confirmation",
        "CONFIRMED" to "Barber assigned and booking confirmed",
        "ON_THE_WAY" to "Barber is on the way",
        "ARRIVED" to "Barber has arrived",
        "IN_PROGRESS" to "Service in progress",
        "COMPLETED" to "Service completed"
    )

    val activeIndex = when (status) {
        "DRAFT", "SLOT_HELD", "PAYMENT_PENDING", "AWAITING_BARBER" -> 0
        "CONFIRMED" -> 1
        "ON_THE_WAY" -> 2
        "ARRIVED" -> 3
        "IN_PROGRESS" -> 4
        "COMPLETED", "DISPUTED" -> 5
        else -> -1
    }

    LiquidGlassSurface(
        strength = GlassMaterialStrength.AtelierCard,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        interactivePhysics = false,
        modifier = modifier
            .fillMaxWidth()
            .testTag("six_stage_booking_timeline")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (status in setOf("CANCELLED", "EXPIRED", "NO_SHOW")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "This appointment is no longer active.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                stages.forEachIndexed { idx, (_, title) ->
                    val isDone = idx < activeIndex || status in setOf("COMPLETED", "DISPUTED")
                    val isCurrent = idx == activeIndex && status !in setOf("COMPLETED", "DISPUTED")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> SageVerified
                                        isCurrent -> EspressoHeaderBg
                                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color(0xFFFFDF9E) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isCurrent -> MaterialTheme.colorScheme.onSurface
                                    isDone -> SageVerified
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (isCurrent) {
                                Text(
                                    text = "Current stage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (isCurrent) {
                            LiveGlassPulseDot(
                                statusKey = status,
                                color = BrassGold,
                                size = 9.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visually separates the Two Payments everywhere in the app:
 * 1) Booking confirmation fee paid to DoorCut to confirm the slot (e.g. ₹30)
 * 2) Service charge paid directly to the barber after service by Cash or UPI (e.g. ₹450)
 * Never combines them into a single confusing payout figure.
 */
@Composable
fun TwoPaymentSplitCard(
    serviceSummaryLabel: String,
    serviceSubtotalPaise: Long,
    approvedAddOnPaise: Long = 0L,
    travelChargePaise: Long = 0L,
    barberServicePayablePaise: Long,
    bookingConfirmationFeePaise: Long,
    isBookingFeeAlreadyPaid: Boolean,
    directPaymentNote: String? = null,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        strength = GlassMaterialStrength.AtelierCard,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        interactivePhysics = false,
        modifier = modifier
            .fillMaxWidth()
            .testTag("two_payment_split_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Two Clear, Separate Payments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Box 1: Pay Now (Booking Confirmation Fee)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBookingFeeAlreadyPaid) "1. Booking confirmation fee (Paid)" else "1. Pay now — Booking confirmation fee",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Reserves your verified barber & appointment slot",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = CurrencyUtils.formatRupees(bookingConfirmationFeePaise),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBookingFeeAlreadyPaid) SageVerified else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Box 2: Pay Directly to Barber After Service
            LiquidGlassSurface(
                strength = GlassMaterialStrength.FloatingPill,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 2.dp,
                interactivePhysics = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BrassGold.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Handshake,
                                    contentDescription = null,
                                    tint = BrassGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "2. Pay after service to barber",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = serviceSummaryLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD9CBB8),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = CurrencyUtils.formatRupees(barberServicePayablePaise),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFDF9E)
                        )
                    }
                    if (approvedAddOnPaise > 0L || travelChargePaise > 0L) {
                        Text(
                            text = buildString {
                                append("Services: ${CurrencyUtils.formatRupees(serviceSubtotalPaise)}")
                                if (approvedAddOnPaise > 0L) append(" + Extra: ${CurrencyUtils.formatRupees(approvedAddOnPaise)}")
                                if (travelChargePaise > 0L) append(" + Travel: ${CurrencyUtils.formatRupees(travelChargePaise)}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFDF9E)
                        )
                    }
                    Text(
                        text = directPaymentNote
                            ?: "Pay ${CurrencyUtils.formatRupees(barberServicePayablePaise)} directly to your barber after the service by cash or UPI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF8F3EC)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerDirectPriceCard(
    serviceSubtotalPaise: Long,
    approvedAddOnPaise: Long,
    travelChargePaise: Long,
    barberServicePayablePaise: Long,
    bookingConfirmationFeePaise: Long = 3000L,
    directPaymentStatusLabel: String? = null,
    modifier: Modifier = Modifier
) {
    TwoPaymentSplitCard(
        serviceSummaryLabel = "Selected grooming services",
        serviceSubtotalPaise = serviceSubtotalPaise,
        approvedAddOnPaise = approvedAddOnPaise,
        travelChargePaise = travelChargePaise,
        barberServicePayablePaise = barberServicePayablePaise,
        bookingConfirmationFeePaise = bookingConfirmationFeePaise,
        isBookingFeeAlreadyPaid = true,
        directPaymentNote = directPaymentStatusLabel,
        modifier = modifier.testTag("customer_direct_price_card")
    )
}

@Composable
private fun PriceLine(
    label: String,
    amount: String,
    highlightFree: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (highlightFree) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = SageVerified,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (highlightFree) SageVerified else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (highlightFree) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlightFree) SageVerified else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val normalized = status.uppercase()
    val (bg, fg, borderCol) = when {
        normalized in setOf(
            "CONFIRMED", "COMPLETED", "APPROVED", "ACTIVE", "ZERO_CUSTOMER_FEE",
            "₹0 PLATFORM FEE", "PAID_VERIFIED", "CUSTOMER_CONFIRMED", "ONLINE",
            "HOME VISIT", "SELECTED", "DEFAULT"
        ) || normalized.contains("SELECTED") -> Triple(
            SageVerified.copy(alpha = 0.14f),
            SageVerified,
            SageVerified.copy(alpha = 0.35f)
        )
        normalized in setOf(
            "DISPUTED", "CANCELLED", "REJECTED", "SUSPENDED", "FAILED", "NO_SHOW", "OFFLINE", "HIDDEN"
        ) -> Triple(
            CrimsonDispute.copy(alpha = 0.14f),
            CrimsonDispute,
            CrimsonDispute.copy(alpha = 0.35f)
        )
        else -> Triple(
            AmberPending.copy(alpha = 0.15f),
            AmberPending,
            AmberPending.copy(alpha = 0.38f)
        )
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Text(
            text = status.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null
) {
    LiquidGlassSurface(
        strength = GlassMaterialStrength.AtelierCard,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp,
        interactivePhysics = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(16.dp))
                action()
            }
        }
    }
}

@Composable
fun DismissibleStatusBanner(
    message: String?,
    onDismiss: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = slideInVertically(
            animationSpec = DoorCutMotionSpecs.surfaceSettleSpring(reducedMotion)
        ) { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        LiquidGlassSurface(
            strength = GlassMaterialStrength.FloatingPill,
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 6.dp,
            interactivePhysics = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("status_banner")
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(BrassGold.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = BrassGold,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF8F3EC),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss notification",
                        tint = Color(0xFFD9CBB8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
