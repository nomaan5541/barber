package com.example.ui.customer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.components.LegalPoliciesSheet
import com.example.ui.components.LegalPolicyTab
import com.example.ui.components.SectionHeader
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassPill
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
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
    var showLegalPolicies by remember { mutableStateOf(false) }
    var legalInitialTab by remember { mutableIntStateOf(0) }

    var permissionSnap by remember { mutableStateOf(PhonePermissionsHelper.inspectPermissions(context)) }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionSnap = PhonePermissionsHelper.inspectPermissions(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_profile_help_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "VERIFIED CUSTOMER ACCOUNT",
                title = "Profile, Privacy & Help",
                subtitle = "Manage your verified profile, saved home doors, notifications, and legal policies.",
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        // 1. Verified Customer Account Card (Name, DOB, Age, Gender, Phone)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 1)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isAuthenticated || userAccount == null) {
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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
                            BarberAvatarBadge(name = userAccount.name, isOnline = true, size = 52.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userAccount.name.ifBlank { "DoorCut Customer" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = userAccount.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = SageVerified.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = null,
                                            tint = SageVerified,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val demoLine = buildList {
                                            add("Customer Account")
                                            if (userAccount.gender.isNotBlank()) add(userAccount.gender)
                                            if (userAccount.age > 0L) add("${userAccount.age} yrs")
                                            if (userAccount.dob.isNotBlank()) add("DOB: ${userAccount.dob}")
                                        }.joinToString(" · ")
                                        Text(
                                            text = demoLine,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SageVerified
                                        )
                                    }
                                }
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
                            label = { Text("Verified Mobile Number") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_phone_input")
                        )
                        val canUpdate = !isBusy && nameInput.isNotBlank()
                        Button(
                            onClick = { onUpdateProfile(nameInput.trim(), phoneInput.trim()) },
                            enabled = canUpdate,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EspressoHeaderBg,
                                contentColor = Color(0xFFFFDF9E)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .tactilePressPhysics(enabled = canUpdate, enableHaptics = true)
                                .testTag("save_profile_button")
                        ) {
                            Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Saved Home Addresses
        item {
            LiquidGlassSurface(
                strength = if (savedAddresses.isNotEmpty()) GlassMaterialStrength.AccentEmerald else GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                interactivePhysics = true,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 2)
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
                            text = if (savedAddresses.isEmpty()) "Set flat/house number & GPS pin"
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
                            .tactilePressPhysics(enableHaptics = true)
                            .testTag("manage_saved_addresses_button")
                    ) {
                        Text("Manage", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 3. Language & Notifications
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
                            LiquidGlassPill(
                                text = lang,
                                selected = localPrefs.selectedLanguage == lang,
                                onClick = { DoorCutPreferences.setLanguage(context, lang) },
                                modifier = Modifier.testTag("lang_chip_${lang.lowercase()}")
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
                                    text = "Live Dispatch & Arrival Alerts",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Instant alerts when stylist accepts, departs, and arrives outside",
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

        // 5. Admin Governance Access (shown ONLY for verified ADMIN accounts)
        if (userAccount?.role == "ADMIN") {
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.FloatingPill,
                    shape = RoundedCornerShape(22.dp),
                    accentBorderColor = BrassGold.copy(alpha = 0.5f),
                    shadowElevation = 4.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Marketplace Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Manage partner approvals, catalog pricing, and support disputes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD9CBB8)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onSwitchToAdminMode,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrassGold,
                                contentColor = EspressoHeaderBg
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("switch_to_admin_mode_button")
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admin", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 6. Help, Privacy Policies, Home Safety Charter & Logout
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 4)
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
                            .tactilePressPhysics()
                            .testTag("help_and_support_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Help & Support (${tickets.count { it.status != "RESOLVED" }} open)")
                    }
                    OutlinedButton(
                        onClick = {
                            legalInitialTab = 0
                            showLegalPolicies = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tactilePressPhysics()
                            .testTag("privacy_policy_button")
                    ) {
                        Icon(Icons.Filled.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Policy, Gender Safety & GPS Charter")
                    }
                    OutlinedButton(
                        onClick = {
                            legalInitialTab = 3
                            showLegalPolicies = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tactilePressPhysics()
                            .testTag("cancellation_policy_button")
                    ) {
                        Icon(Icons.Filled.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Payment, Cancellation & Refund Policies")
                    }
                    if (isAuthenticated) {
                        Button(
                            onClick = onSignOut,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .tactilePressPhysics(enableHaptics = true)
                                .testTag("logout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out of Google Account", fontWeight = FontWeight.Bold)
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

    if (showLegalPolicies) {
        LegalPoliciesSheet(
            initialTab = LegalPolicyTab.entries.getOrElse(legalInitialTab) { LegalPolicyTab.PRIVACY },
            onDismiss = { showLegalPolicies = false }
        )
    }
}
