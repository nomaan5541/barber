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
        service.isPackage || service.category == "PACKAGES" -> ServiceVisualMeta(
            drawableRes = R.drawable.img_svc_package_1792915154765,
            badgeLabel = "COMPLETE GROOMING COMBO",
            visualCaption = "Full salon ritual delivered to your home"
        )
        service.category == "HAIRCUTS" ||
            lowerName.contains("haircut") ||
            lowerName.contains("fade") ||
            lowerName.contains("scissor") ||
            lowerName.contains("cut") -> ServiceVisualMeta(
            drawableRes = R.drawable.img_svc_haircut_1792915150040,
            badgeLabel = "AT-HOME HAIRCUTTING",
            visualCaption = "Precision skin fade, taper & scissor tailoring"
        )
        service.category == "BEARD_MOUSTACHE" ||
            lowerName.contains("beard") ||
            lowerName.contains("shave") ||
            lowerName.contains("moustache") -> ServiceVisualMeta(
            drawableRes = R.drawable.img_svc_beard_1792915151623,
            badgeLabel = "BEARD & RAZOR SCULPT",
            visualCaption = "Sharp contour line-up & hot towel finish"
        )
        service.category == "HAIR_CARE" ||
            lowerName.contains("wash") ||
            lowerName.contains("massage") ||
            lowerName.contains("spa") ||
            lowerName.contains("blow") -> ServiceVisualMeta(
            drawableRes = R.drawable.img_svc_hair_care_1792915152006,
            badgeLabel = "SCALP SPA & BLOW-DRY",
            visualCaption = "Revitalizing wash, head massage & styling"
        )
        else -> ServiceVisualMeta(
            drawableRes = R.drawable.img_svc_facial_1792915153778,
            badgeLabel = "FACIAL & DETAN CARE",
            visualCaption = "Charcoal detox, scrub & bright skin care"
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
    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (accentHighlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (accentHighlight) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted -> SageVerified
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                }
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
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary
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
fun CustomerDirectPriceCard(
    serviceSubtotalPaise: Long,
    approvedAddOnPaise: Long,
    travelChargePaise: Long,
    barberServicePayablePaise: Long,
    directPaymentStatusLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_direct_price_card"),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ATELIER RECEIPT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Zero-Fee Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                StatusBadge("₹0 PLATFORM FEE")
            }

            Spacer(modifier = Modifier.height(12.dp))

            PriceLine("Selected Grooming Services", CurrencyUtils.formatRupees(serviceSubtotalPaise))
            if (approvedAddOnPaise > 0L) {
                PriceLine("Approved Extra Add-On", "+ ${CurrencyUtils.formatRupees(approvedAddOnPaise)}")
            }
            if (travelChargePaise > 0L) {
                PriceLine("Doorstep Travel Allowance", "+ ${CurrencyUtils.formatRupees(travelChargePaise)}")
            }
            PriceLine(
                label = "Customer Platform / Booking Fee",
                amount = "₹0 FREE",
                highlightFree = true
            )

            Spacer(modifier = Modifier.height(12.dp))
            PerforatedReceiptDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EspressoHeaderBg,
                contentColor = Color(0xFFF8F3EC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BrassGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Handshake,
                                contentDescription = null,
                                tint = BrassGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pay Barber Directly",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = directPaymentStatusLabel ?: "Settle via UPI or Cash after your cut",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD9CBB8)
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
            }
        }
    }
}

@Composable
fun BarberCommissionBreakdownCard(
    grossCustomerPayablePaise: Long,
    barberCommissionPaise: Long,
    barberNetEarningsPaise: Long,
    directPaymentStatusLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("barber_commission_breakdown_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Barber Payout & Partner Commission",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            PriceLine("Collect Directly from Customer", CurrencyUtils.formatRupees(grossCustomerPayablePaise))
            PriceLine("Customer Platform Fee", "₹0 (Zero Fee)", highlightFree = true)
            PriceLine("DoorCut Partner Commission", "- ${CurrencyUtils.formatRupees(barberCommissionPaise)}")

            Spacer(modifier = Modifier.height(4.dp))
            PerforatedReceiptDivider()
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Net Take-Home",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!directPaymentStatusLabel.isNullOrBlank()) {
                        Text(
                            text = directPaymentStatusLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = CurrencyUtils.formatRupees(barberNetEarningsPaise),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = slideInVertically { -it / 2 } + fadeIn(),
        exit = slideOutVertically { -it / 2 } + fadeOut()
    ) {
        Surface(
            color = EspressoHeaderBg,
            contentColor = Color(0xFFF8F3EC),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.45f)),
            shadowElevation = 4.dp,
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
