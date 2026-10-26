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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BarberProfile
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.PlatformConfig
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg

@Composable
fun BarberProfileScreen(
    barberProfile: BarberProfile?,
    displayName: String,
    catalogServices: List<CatalogService>,
    platformConfig: PlatformConfig,
    isBusy: Boolean,
    onSaveBarberServicesAndUpi: (String, String, String, List<String>) -> Unit,
    onCreateSupportTicket: (String, String) -> Unit,
    onSwitchToCustomerMode: () -> Unit,
    onSignOut: () -> Unit
) {
    val activeName = barberProfile?.name?.ifBlank { displayName } ?: displayName.ifBlank { "Partner Barber" }
    val approvalStatus = barberProfile?.approvalStatus ?: "PENDING_SETUP"

    var upiInput by remember(barberProfile?.upiId) { mutableStateOf(barberProfile?.upiId.orEmpty()) }
    var shopNameInput by remember(barberProfile?.shopName) {
        mutableStateOf(barberProfile?.effectiveShopName().orEmpty())
    }
    var areaAddressInput by remember(barberProfile?.shopAddress) {
        mutableStateOf(barberProfile?.effectiveShopAddress("Hyderabad").orEmpty())
    }
    var selectedIds by remember(barberProfile?.serviceIds, catalogServices) {
        val initial = barberProfile?.serviceIds?.toSet().orEmpty()
        mutableStateOf(if (initial.isEmpty()) catalogServices.take(4).map { it.id }.toSet() else initial)
    }

    var showSupportModal by remember { mutableStateOf(false) }
    var supportSubject by remember { mutableStateOf("") }
    var supportDesc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("barber_profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "PARTNER ACCOUNT",
                title = "Barber Profile",
                subtitle = "Manage your verification status, services offered, travel preferences, and support.",
                modifier = Modifier.widthIn(max = 600.dp)
            )
        }

        // 1. Public Identity, Ratings & Verification Status
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BarberAvatarBadge(
                            name = activeName,
                            isOnline = barberProfile?.isOnline ?: true,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = BrassGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${String.format("%.1f", barberProfile?.ratingAvg ?: 5.0)} (${barberProfile?.reviewCount ?: 0L} reviews)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFDF9E)
                                )
                            }
                        }
                        StatusBadge(approvalStatus)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = BrassGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (approvalStatus == "APPROVED") {
                                "ID, hygiene kit & barber skill check verified"
                            } else {
                                "Verification documents under Admin review"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF8F3EC)
                        )
                    }
                }
            }
        }

        // 2. Service Areas, Travel Preferences & Working Hours Summary
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
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Service Areas & Direct UPI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(
                        value = shopNameInput,
                        onValueChange = { shopNameInput = it },
                        label = { Text("Public Studio / Display Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = areaAddressInput,
                        onValueChange = { areaAddressInput = it },
                        label = { Text("Primary Service Area") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it },
                        label = { Text("Direct Customer UPI ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hours: ${CurrencyUtils.formatHourSlot(barberProfile?.workStartHour ?: 9L)} – ${CurrencyUtils.formatHourSlot(barberProfile?.workEndHour ?: 20L)} · Travel buffer: ${barberProfile?.travelBufferMinutes ?: 15L} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Services Offered & Prices (with Admin Approval Rule Notice)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Services Offered & Standard Prices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Standard prices are governed by DoorCut Admin so customers see predictable rates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    catalogServices.take(6).forEach { svc ->
                        val checked = selectedIds.contains(svc.id)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedIds = if (isChecked) selectedIds + svc.id else selectedIds - svc.id
                                    }
                                )
                                Text(
                                    text = "${svc.name} (${svc.durationMinutes}m)",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = CurrencyUtils.formatRupees(svc.pricePaise),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onSaveBarberServicesAndUpi(
                                upiInput.trim(),
                                shopNameInput.trim(),
                                areaAddressInput.trim(),
                                selectedIds.toList()
                            )
                        },
                        enabled = !isBusy && barberProfile != null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_barber_profile_button")
                    ) {
                        Text("Save Services & Preferences", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Support, Switch to Customer Mode & Logout
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
                    OutlinedButton(
                        onClick = onSwitchToCustomerMode,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("switch_to_customer_mode_button")
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Switch to Customer Mode", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showSupportModal = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partner Support & Policies")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("barber_logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSupportModal) {
        AlertDialog(
            onDismissRequest = { showSupportModal = false },
            title = { Text("Barber Partner Support") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = platformConfig.policyText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = supportSubject,
                        onValueChange = { supportSubject = it },
                        label = { Text("Issue Topic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supportDesc,
                        onValueChange = { supportDesc = it },
                        label = { Text("Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateSupportTicket(
                            supportSubject.ifBlank { "Barber Partner Help" },
                            supportDesc.ifBlank { "Assistance requested" }
                        )
                        showSupportModal = false
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportModal = false }, modifier = Modifier.height(48.dp)) {
                    Text("Close")
                }
            }
        )
    }
}
