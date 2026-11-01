package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.LegalPoliciesSheet
import com.example.ui.components.LegalPolicyTab
import com.example.ui.glass.AmbientAtelierBackdrop
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassPill
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingProfileScreen(
    initialName: String,
    initialEmail: String,
    initialPhone: String,
    detectedLocality: String,
    isBusy: Boolean,
    onCompleteOnboarding: (
        fullName: String,
        phone: String,
        dob: String,
        age: Long,
        gender: String,
        selectedRole: String,
        servesLadies: Boolean,
        servesMen: Boolean,
        experienceYears: Long,
        upiId: String,
        primaryArea: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var fullName by remember(initialName) { mutableStateOf(initialName) }
    var phone by remember(initialPhone) { mutableStateOf(initialPhone) }
    var dobText by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") }
    var selectedRole by remember { mutableStateOf("CUSTOMER") }

    // Barber-specific onboarding fields
    var barberSpecialty by remember { mutableStateOf("MENS") } // MENS, LADIES, UNISEX
    var experienceYearsText by remember { mutableStateOf("4") }
    var upiIdInput by remember { mutableStateOf("") }
    var primaryAreaInput by remember(detectedLocality) { mutableStateOf(detectedLocality) }

    var privacyAccepted by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPoliciesSheet by remember { mutableStateOf(false) }

    val parsedAge = ageText.toLongOrNull() ?: 0L
    val isFormValid = fullName.trim().length >= 2 &&
        phone.trim().length >= 8 &&
        dobText.isNotBlank() &&
        parsedAge in 13L..110L &&
        privacyAccepted &&
        !isBusy

    AmbientAtelierBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_profile_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Identity Header
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.FloatingPill,
                    shape = RoundedCornerShape(24.dp),
                    accentBorderColor = BrassGold.copy(alpha = 0.55f),
                    shadowElevation = 4.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = 0)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BrassGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = EspressoHeaderBg,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Complete Your DoorCut Profile",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = initialEmail.ifBlank { "Google Account Verified" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFDF9E)
                            )
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("onboarding_signout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out",
                                tint = Color(0xFFFFDF9E)
                            )
                        }
                    }
                }
            }

            // 1. Choose Account Role (Customer vs Barber Partner)
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = 1)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. How will you use DoorCut?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your account enters a dedicated role-based experience after setup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RoleChoiceCard(
                                selected = selectedRole == "CUSTOMER",
                                icon = Icons.Filled.Person,
                                title = "Customer",
                                subtitle = "Book haircuts & stylists at home",
                                testTag = "onboarding_role_customer",
                                onClick = { selectedRole = "CUSTOMER" },
                                modifier = Modifier.weight(1f)
                            )
                            RoleChoiceCard(
                                selected = selectedRole == "BARBER",
                                icon = Icons.Filled.ContentCut,
                                title = "Barber / Stylist",
                                subtitle = "Accept nearby home bookings",
                                testTag = "onboarding_role_barber",
                                onClick = {
                                    selectedRole = "BARBER"
                                    barberSpecialty = if (gender == "FEMALE") "LADIES" else "MENS"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Personal Details: Name, DOB, Age, Gender & Phone
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = 2)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "2. Personal & Contact Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name *") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_name_input")
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                            label = { Text("Mobile Phone Number *") },
                            placeholder = { Text("Used for live arrival coordination") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_phone_input")
                        )

                        // Date of Birth + Age Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .height(56.dp)
                                    .tactilePressPhysics(enableHaptics = true)
                                    .testTag("onboarding_dob_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (dobText.isBlank()) "Select Date of Birth *" else "DOB: $dobText",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedTextField(
                                value = ageText,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(3)
                                    ageText = digits
                                    if (dobText.isBlank() && digits.toIntOrNull() in 13..110) {
                                        val approxYear = LocalDate.now().year - digits.toInt()
                                        dobText = "$approxYear-01-01"
                                    }
                                },
                                label = { Text("Age *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.65f)
                                    .testTag("onboarding_age_input")
                            )
                        }

                        Text(
                            text = "Gender (used for safe stylist matching)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MALE" to "Male", "FEMALE" to "Female", "OTHER" to "Other").forEach { (key, label) ->
                                LiquidGlassPill(
                                    text = label,
                                    selected = gender == key,
                                    onClick = {
                                        gender = key
                                        if (selectedRole == "BARBER" && key == "FEMALE") {
                                            barberSpecialty = "LADIES"
                                        }
                                    },
                                    modifier = Modifier.testTag("onboarding_gender_${key.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // 3. Barber Specialization (Only when BARBER role selected)
            if (selectedRole == "BARBER") {
                item {
                    LiquidGlassSurface(
                        strength = GlassMaterialStrength.AccentEmerald,
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 2.dp,
                        interactivePhysics = false,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .staggeredSpringEntrance(index = 3)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "3. Barber / Stylist Partner Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select which customer bookings you serve so DoorCut matches the right requests to your queue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "MENS" to "Men's Barber",
                                    "LADIES" to "Lady Hair Stylist",
                                    "UNISEX" to "Both / Unisex"
                                ).forEach { (key, label) ->
                                    LiquidGlassPill(
                                        text = label,
                                        selected = barberSpecialty == key,
                                        onClick = { barberSpecialty = key },
                                        modifier = Modifier.testTag("barber_specialty_${key.lowercase()}")
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = experienceYearsText,
                                    onValueChange = { experienceYearsText = it.filter { c -> c.isDigit() }.take(2) },
                                    label = { Text("Experience (Yrs)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(0.45f)
                                )
                                OutlinedTextField(
                                    value = upiIdInput,
                                    onValueChange = { upiIdInput = it },
                                    label = { Text("UPI ID (Direct Pay)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(0.55f)
                                )
                            }

                            OutlinedTextField(
                                value = primaryAreaInput,
                                onValueChange = { primaryAreaInput = it },
                                label = { Text("Primary Dispatch Area / Locality") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 4. Privacy Policy & Safety Terms Acceptance
            item {
                LiquidGlassSurface(
                    strength = GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    interactivePhysics = false,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = 4)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { privacyAccepted = !privacyAccepted }
                        ) {
                            Checkbox(
                                checked = privacyAccepted,
                                onCheckedChange = { privacyAccepted = it },
                                modifier = Modifier.testTag("onboarding_privacy_checkbox")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "I agree to the DoorCut Privacy Policy, Live GPS & Phone Sharing Rules, and Home Safety Terms.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        TextButton(
                            onClick = { showPoliciesSheet = true },
                            modifier = Modifier.testTag("onboarding_view_privacy_button")
                        ) {
                            Icon(Icons.Filled.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Read Full Privacy Policy & Safety Terms", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. Submit CTA
            item {
                Button(
                    onClick = {
                        val servesLadies = barberSpecialty in setOf("LADIES", "UNISEX")
                        val servesMen = barberSpecialty in setOf("MENS", "UNISEX")
                        onCompleteOnboarding(
                            fullName.trim(),
                            phone.trim(),
                            dobText.trim(),
                            parsedAge,
                            gender,
                            selectedRole,
                            servesLadies,
                            servesMen,
                            experienceYearsText.toLongOrNull() ?: 3L,
                            upiIdInput.trim(),
                            primaryAreaInput.trim()
                        )
                    },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EspressoHeaderBg,
                        contentColor = Color(0xFFFFDF9E)
                    ),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                        .tactilePressPhysics(enabled = isFormValid, enableHaptics = true)
                        .testTag("onboarding_continue_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedRole == "BARBER") {
                            "Enter Barber Partner Dashboard"
                        } else {
                            "Continue to Customer Home"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val birthDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            dobText = birthDate.toString()
                            val calculated = Period.between(birthDate, LocalDate.now()).years.coerceIn(13, 110)
                            ageText = calculated.toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm DOB")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPoliciesSheet) {
        LegalPoliciesSheet(
            initialTab = LegalPolicyTab.PRIVACY,
            onDismiss = { showPoliciesSheet = false }
        )
    }
}

@Composable
private fun RoleChoiceCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) EspressoHeaderBg else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) BrassGold else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .tactilePressPhysics(enableHaptics = true)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) BrassGold else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = SageVerified,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Color(0xFFFFDF9E) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
