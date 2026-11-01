package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassPill
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified

enum class LegalPolicyTab(val title: String) {
    PRIVACY("Privacy & Data"),
    SAFETY("Gender & Home Safety"),
    LOCATION("Live GPS & Phone"),
    PAYMENTS("Payments & Refunds")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalPoliciesSheet(
    initialTab: LegalPolicyTab = LegalPolicyTab.PRIVACY,
    cancellationPolicySummary: String = "",
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("legal_policies_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Policy,
                        contentDescription = null,
                        tint = BrassGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DoorCut Privacy, Safety & Terms",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close policies")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LegalPolicyTab.entries.forEach { tab ->
                    LiquidGlassPill(
                        text = tab.title,
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            when (activeTab) {
                LegalPolicyTab.PRIVACY -> {
                    PolicySectionCard(
                        icon = Icons.Filled.GppGood,
                        title = "1. Personal Data & Zero-Trust Door Isolation",
                        points = listOf(
                            "DoorCut collects your Google account email, full name, date of birth, gender, mobile phone number, and home dispatch coordinates solely to fulfill doorstep grooming appointments.",
                            "Your flat/house door number and phone number are stored in an isolated protected Firestore subcollection (/privateDetails/main) and are NEVER visible in the open barber request pool.",
                            "Only the single verified Barber or Lady Hair Stylist who claims your active booking can view your exact flat/door number and phone number—and access is automatically revoked once the booking is cancelled or expired.",
                            "We never sell personal data, contact numbers, or location history to third-party advertisers."
                        )
                    )
                }
                LegalPolicyTab.SAFETY -> {
                    PolicySectionCard(
                        icon = Icons.Filled.VerifiedUser,
                        title = "2. Lady Hair Stylist Matching & Home Safety",
                        points = listOf(
                            "Gender-Safe Dispatch: Female customers can select 'Need a Lady Hair Barber / Female Stylist' during booking so their request is highlighted specifically for verified female hair stylists.",
                            "Recipient Details: Sharing recipient gender (Male / Female / Child) and age ensures the appointed professional carries the right styling kit, capes, and gentle tools.",
                            "Hygiene Protocol: Every partner barber unseals a fresh single-use blade in front of the customer, uses sanitized combs/clippers, and lays a disposable floor sheet.",
                            "24x7 Safety Escalation: Both customers and barbers can raise an immediate Safety Concern ticket or contact local emergency services (112) from the live trip screen."
                        )
                    )
                }
                LegalPolicyTab.LOCATION -> {
                    PolicySectionCard(
                        icon = Icons.Filled.LocationOn,
                        title = "3. Live Barber GPS Movement & Phone Coordination",
                        points = listOf(
                            "Customer GPS Pin: Used to pinpoint your home street and calculate real-time barber ETA without manual coordinate typing.",
                            "Barber Live Movement: When a barber taps 'On My Way', live GPS coordinates stream only to the matched customer's active booking map until arrival.",
                            "Direct Phone Coordination: Once matched, both customer and appointed barber can call or SMS each other directly via Android's dialer for gate/intercom entry."
                        )
                    )
                }
                LegalPolicyTab.PAYMENTS -> {
                    PolicySectionCard(
                        icon = Icons.Filled.Payments,
                        title = "4. Two-Part Transparent Payment & Cancellation Policy",
                        points = buildList {
                            add("Slot Confirmation Fee: Paid at booking confirmation to reserve dispatch priority.")
                            add("Direct Service Payment: 100% of the haircut/styling service amount is paid directly to your appointed barber after completion via Cash or UPI.")
                            add("No Surprise Add-Ons: Extra services during a home visit require explicit in-app customer approval before the payable total changes.")
                            if (cancellationPolicySummary.isNotBlank()) {
                                add(cancellationPolicySummary)
                            } else {
                                add("Free Cancellation: Cancel free of charge before your appointed barber begins travelling to your location.")
                            }
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EspressoHeaderBg,
                    contentColor = Color(0xFFFFDF9E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .tactilePressPhysics(enableHaptics = true)
                    .testTag("close_legal_policies_button")
            ) {
                Text("Understood", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PolicySectionCard(
    icon: ImageVector,
    title: String,
    points: List<String>
) {
    LiquidGlassSurface(
        strength = GlassMaterialStrength.AtelierCard,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        interactivePhysics = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SageVerified,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            points.forEach { bullet ->
                Text(
                    text = "• $bullet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
