package com.example.ui.customer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.SupportTicket
import com.example.data.model.UserAccount
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.EspressoHeaderBg
import com.example.util.DoorCutPreferences
import com.example.util.PhonePermissionsHelper

@Composable
fun CustomerProfileHelpScreen(
    isAuthenticated: Boolean,
    userAccount: UserAccount?,
    savedAddresses: List<CustomerAddress>,
    tickets: List<SupportTicket>,
    platformConfig: PlatformConfig,
    isBusy: Boolean,
    onSignInClick: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onManageAddresses: () -> Unit,
    onCreateSupportTicket: (String, String) -> Unit,
    onSwitchToBarberMode: () -> Unit,
    onSwitchToAdminMode: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { DoorCutPreferences.ensureLoaded(context) }
    val localPrefs by DoorCutPreferences.state.collectAsStateWithLifecycle()

    var nameInput by remember(userAccount?.name) { mutableStateOf(userAccount?.name.orEmpty()) }
    var phoneInput by remember(userAccount?.phone) { mutableStateOf(userAccount?.phone.orEmpty()) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var ticketSubject by remember { mutableStateOf("") }
    var ticketBody by remember { mutableStateOf("") }
    var activePolicyDialog by remember { mutableStateOf<String?>(null) }

    var permissionSnap by remember { mutableStateOf(PhonePermissionsHelper.inspectPermissions(context)) }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionSnap = PhonePermissionsHelper.inspectPermissions(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("customer_profile_help_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "ACCOUNT & PREFERENCES",
                title = "Profile & Help",
                subtitle = "Manage your saved addresses, language, notifications, and support in one place.",
                modifier = Modifier.widthIn(max = 600.dp)
            )
        }

        // 1. Account Card (Name & Phone or Sign In)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
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
                    if (!isAuthenticated || userAccount == null) {
                        Text(
                            text = "Sign in to sync your bookings & home addresses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Use Google Sign-In to manage appointments, saved addresses, and support.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onSignInClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EspressoHeaderBg,
                                contentColor = Color(0xFFFFDF9E)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_sign_in_button")
                        ) {
                            Text("Continue with Google", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BarberAvatarBadge(name = userAccount.name, isOnline = true, size = 50.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = userAccount.name.ifBlank { "DoorCut Customer" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userAccount.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_phone_input")
                        )
                        Button(
                            onClick = { onUpdateProfile(nameInput.trim(), phoneInput.trim()) },
                            enabled = !isBusy && nameInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_profile_button")
                        ) {
                            Text("Save Name & Phone", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Saved Home Addresses
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved Home Addresses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (savedAddresses.isEmpty()) "Set flat/house number & landmark"
                            else "${savedAddresses.size} saved (${savedAddresses.first().displayLabel()}: ${savedAddresses.first().flatHouse})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onManageAddresses,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("manage_saved_addresses_button")
                    ) {
                        Text("Manage")
                    }
                }
            }
        }

        // 3. Language & Notifications
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Language Selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("English", "Hindi", "Telugu").forEach { lang ->
                            FilterChip(
                                selected = localPrefs.selectedLanguage == lang,
                                onClick = { DoorCutPreferences.setLanguage(context, lang) },
                                label = { Text(lang) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("lang_chip_${lang.lowercase()}")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Booking & Arrival Notifications",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Alerts when barber is assigned, on the way, and outside",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = localPrefs.notificationsEnabled,
                            onCheckedChange = { DoorCutPreferences.setNotificationsEnabled(context, it) },
                            modifier = Modifier.testTag("notifications_toggle")
                        )
                    }
                }
            }
        }

        // 4. Phone Permissions Readiness Card (if any missing)
        if (!permissionSnap.allGrantedIncludingCamera) {
            item {
                PhonePermissionsPromptCard(
                    snapshot = permissionSnap,
                    onRequestPermissions = {
                        permLauncher.launch(PhonePermissionsHelper.allFeaturePermissionsArray())
                    },
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }
        }

        // 5. Switch to Barber Mode / Admin Dashboard
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EspressoHeaderBg),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ContentCut, contentDescription = null, tint = Color(0xFFFFDF9E))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Are you a Barber?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Open Barber Mode to accept home appointments, navigate to customers, and track direct payments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD9CBB8)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onSwitchToBarberMode,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFDF9E),
                                contentColor = EspressoHeaderBg
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("switch_to_barber_mode_button")
                        ) {
                            Text("Open Barber Mode", fontWeight = FontWeight.Bold)
                        }
                        if (userAccount?.role == "ADMIN") {
                            OutlinedButton(
                                onClick = onSwitchToAdminMode,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("switch_to_admin_mode_button")
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Admin")
                            }
                        }
                    }
                }
            }
        }

        // 6. Help, Policies & Logout
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
                    OutlinedButton(
                        onClick = { if (isAuthenticated) showSupportDialog = true else onSignInClick() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("help_and_support_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Help & Support (${tickets.count { it.status != "RESOLVED" }} open)")
                    }
                    OutlinedButton(
                        onClick = { activePolicyDialog = "CANCELLATION" },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cancellation_policy_button")
                    ) {
                        Icon(Icons.Filled.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancellation & Refund Policies")
                    }
                    OutlinedButton(
                        onClick = { activePolicyDialog = "PRIVACY" },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("privacy_policy_button")
                    ) {
                        Icon(Icons.Filled.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Policy")
                    }
                    if (isAuthenticated) {
                        Button(
                            onClick = onSignOut,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("logout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Help & Support") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ticketSubject,
                        onValueChange = { ticketSubject = it },
                        label = { Text("Topic (e.g. Booking or Payment help)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ticketBody,
                        onValueChange = { ticketBody = it },
                        label = { Text("Tell us how we can help") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateSupportTicket(
                            ticketSubject.ifBlank { "Customer Support" },
                            ticketBody.ifBlank { "Need help with my booking" }
                        )
                        ticketSubject = ""
                        ticketBody = ""
                        showSupportDialog = false
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }, modifier = Modifier.height(48.dp)) {
                    Text("Cancel")
                }
            }
        )
    }

    if (activePolicyDialog != null) {
        val isCancel = activePolicyDialog == "CANCELLATION"
        AlertDialog(
            onDismissRequest = { activePolicyDialog = null },
            title = { Text(if (isCancel) "Cancellation & Refund Policy" else "Privacy & Address Safety") },
            text = {
                Text(
                    text = if (isCancel) {
                        platformConfig.policyText
                    } else {
                        "Your full flat/house number and phone contact are stored in a protected collection and shared only with your assigned barber once your booking is active."
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(onClick = { activePolicyDialog = null }, modifier = Modifier.height(48.dp)) {
                    Text("Got It")
                }
            }
        )
    }
}
