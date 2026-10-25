package com.example.ui.customer

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.data.model.CustomerAddress
import com.example.data.model.ServiceArea
import com.example.data.model.SupportTicket
import com.example.data.model.UserAccount
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.ui.viewmodel.ActivePortal
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.util.CustomerPreferencesStore
import com.example.util.DetectedDoorLocation
import com.example.util.DoorCutNotificationHelper
import com.example.util.PendingDoorAddressDraft
import com.example.util.PhonePermissionsHelper

/**
 * Full-featured Member Account screen (`AppRoutes.ROUTE_PROFILE`) displaying:
 * 1. Member profile & lifetime zero-fee summary
 * 2. Previous Bookings & 1-Tap Rebook history
 * 3. All Available Customer Features Hub (Cart, Saved Doors, GPS Setup, Services)
 * 4. Phone Permissions Readiness & Live Notification Test
 * 5. Saved Card POS, UPI & Cash-at-Door Payment Preferences
 * 6. Personal Haircut & Home Hygiene Style Preferences (with Camera / Photo Picker reference)
 * 7. Contact Info, Partner Barber Registration, Safety Helpline, Support Tickets & Sign Out
 */
@Composable
fun AccountAndSupportScreen(
    viewModel: MarketplaceViewModel,
    userAccount: UserAccount,
    bookings: List<Booking>,
    addresses: List<CustomerAddress>,
    selectedServiceCount: Int,
    tickets: List<SupportTicket>,
    allServices: List<CatalogService>,
    serviceAreas: List<ServiceArea>,
    isBusy: Boolean,
    onNavigateToServices: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToAddresses: () -> Unit,
    onNavigateToDoorSetup: () -> Unit,
    onSwitchToBarberPortal: () -> Unit = {},
    onSwitchToAdminPortal: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { CustomerPreferencesStore.ensureLoaded(context) }
    val prefs by CustomerPreferencesStore.state.collectAsStateWithLifecycle()

    var permissionSnapshot by remember {
        mutableStateOf(PhonePermissionsHelper.inspectPermissions(context))
    }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var notificationFeedback by remember { mutableStateOf<String?>(null) }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionSnapshot = PhonePermissionsHelper.inspectPermissions(context)
        if (permissionSnapshot.hasAnyLocation) {
            viewModel.autoFetchLocationAndPromptDoor(context, promptForHomeAddressAfterFetch = false)
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            capturedBitmap = bmp
            CustomerPreferencesStore.update(context) {
                it.copy(
                    referencePhotoAttached = true,
                    referencePhotoNote = "Camera hairstyle reference captured"
                )
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionSnapshot = PhonePermissionsHelper.inspectPermissions(context)
        if (granted) {
            cameraPreviewLauncher.launch(null)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            CustomerPreferencesStore.update(context) {
                it.copy(
                    referencePhotoAttached = true,
                    referencePhotoNote = "Gallery hairstyle reference selected"
                )
            }
        }
    }

    var name by remember(userAccount.name) { mutableStateOf(userAccount.name) }
    var phone by remember(userAccount.phone) { mutableStateOf(userAccount.phone) }

    var historyFilter by remember { mutableStateOf("ALL") }
    val sortedBookings = remember(bookings, historyFilter) {
        val base = when (historyFilter) {
            "COMPLETED" -> bookings.filter { it.status == "COMPLETED" }
            "ACTIVE" -> bookings.filter {
                it.status in setOf("SLOT_HELD", "PAYMENT_PENDING", "AWAITING_BARBER", "CONFIRMED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS")
            }
            else -> bookings
        }
        base.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    var ticketCategory by remember { mutableStateOf("PAYMENT_DISPUTE") }
    var ticketBookingRef by remember { mutableStateOf("") }
    var ticketSubject by remember { mutableStateOf("") }
    var ticketDesc by remember { mutableStateOf("") }

    var showBarberApply by remember { mutableStateOf(false) }
    var barberShopName by remember { mutableStateOf("") }
    var barberShopAddress by remember { mutableStateOf("") }
    var barberSkillsText by remember { mutableStateOf("Normal Head Cut, Traditional Cut, Zero Skin Fade, Beard Sculpt") }
    var barberBio by remember { mutableStateOf("Master barber offering salon-grade home visits with sanitized tools.") }
    var barberExp by remember { mutableStateOf("5") }
    var barberUpi by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_account_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Editorial Member Crest & Summary Stats
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EspressoHeaderBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            BarberAvatarBadge(name = userAccount.name, isOnline = true, size = 54.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = userAccount.name.ifBlank { "DoorCut Member" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = userAccount.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD6C8B6)
                                )
                            }
                        }
                        StatusBadge(userAccount.role)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccountStatPill(
                            value = "${bookings.size}",
                            label = "Bookings",
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatPill(
                            value = "${addresses.size}",
                            label = "Home Doors",
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatPill(
                            value = "₹0",
                            label = "Platform Fees",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. All Available Features & Quick Navigation Hub
        item {
            AccountQuickFeaturesGrid(
                selectedServiceCount = selectedServiceCount,
                savedDoorsCount = addresses.size,
                bookingsCount = bookings.size,
                onNavigateToServices = onNavigateToServices,
                onNavigateToCart = onNavigateToCart,
                onNavigateToAddresses = onNavigateToAddresses,
                onNavigateToBookings = onNavigateToBookings,
                onNavigateToDoorSetup = onNavigateToDoorSetup
            )
        }

        // 3. Previous Bookings & 1-Tap Rebook Section
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_previous_bookings_section")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Previous Bookings & Rebook",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Repeat your favourite haircut & specialist in one tap",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (bookings.isNotEmpty()) {
                            TextButton(onClick = onNavigateToBookings) {
                                Text("See All")
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL" to "All (${bookings.size})", "COMPLETED" to "Completed", "ACTIVE" to "Active").forEach { (code, label) ->
                            FilterChip(
                                selected = historyFilter == code,
                                onClick = { historyFilter = code },
                                label = { Text(label) }
                            )
                        }
                    }

                    if (sortedBookings.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No appointments in this filter yet.",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Once you book a Normal Head Cut, Traditional Haircut, or Combo, your receipts and instant Rebook buttons appear right here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onNavigateToServices,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("account_book_first_cut_button")
                                ) {
                                    Icon(Icons.Filled.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Browse Haircuts & Book Home Visit", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            sortedBookings.take(5).forEach { booking ->
                                PreviousBookingSummaryCard(
                                    booking = booking,
                                    onRepeatBooking = {
                                        viewModel.repeatBooking(booking)
                                        onNavigateToCart()
                                    },
                                    onOpenReceipt = onNavigateToBookings
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Phone Permissions & Live Notification Readiness Section
        item {
            PhonePermissionsAccountCard(
                snapshot = permissionSnapshot,
                notificationFeedback = notificationFeedback,
                onRequestAllPermissions = {
                    multiPermissionLauncher.launch(PhonePermissionsHelper.allFeaturePermissionsArray())
                },
                onSendTestNotification = {
                    val sent = DoorCutNotificationHelper.sendVisitNotification(
                        context = context,
                        title = "DoorCut Arrival Alert Ready",
                        message = "Notifications are active! You'll be alerted when your barber confirms and reaches your door."
                    )
                    notificationFeedback = if (sent) {
                        "Test arrival notification sent to your phone status bar!"
                    } else {
                        "Grant Notification permission first to receive arrival alerts."
                    }
                },
                onOpenSystemSettings = { PhonePermissionsHelper.openAppSettings(context) }
            )
        }

        // 5. Saved Card POS, UPI & Cash Payment Preferences
        item {
            DoorstepCardAndPaymentSelector(
                selectedOption = prefs.paymentOption,
                savedCardLabel = prefs.savedCardLabel,
                savedCardLast4 = prefs.savedCardLast4,
                savedUpiId = prefs.savedUpiId,
                onSelectOption = { opt ->
                    CustomerPreferencesStore.update(context) { it.copy(paymentOption = opt) }
                },
                onUpdateCardDetails = { label, last4 ->
                    CustomerPreferencesStore.update(context) {
                        it.copy(savedCardLabel = label, savedCardLast4 = last4)
                    }
                },
                onUpdateUpiId = { upi ->
                    CustomerPreferencesStore.update(context) { it.copy(savedUpiId = upi) }
                },
                modifier = Modifier.testTag("account_payment_card_section")
            )
        }

        // 6. Grooming & Hygiene Preferences + Hairstyle Reference Camera / Photo Picker
        item {
            GroomingPreferencesCard(
                preferredCutStyle = prefs.preferredHaircutStyle,
                preferredOil = prefs.preferredOilType,
                quietSession = prefs.quietSession,
                floorSheetAndCape = prefs.floorSheetAndCapeRequired,
                referencePhotoAttached = prefs.referencePhotoAttached,
                referencePhotoNote = prefs.referencePhotoNote,
                capturedBitmap = capturedBitmap,
                onSelectCutStyle = { style ->
                    CustomerPreferencesStore.update(context) { it.copy(preferredHaircutStyle = style) }
                },
                onSelectOil = { oil ->
                    CustomerPreferencesStore.update(context) { it.copy(preferredOilType = oil) }
                },
                onToggleQuiet = { enabled ->
                    CustomerPreferencesStore.update(context) { it.copy(quietSession = enabled) }
                },
                onToggleFloorSheet = { enabled ->
                    CustomerPreferencesStore.update(context) { it.copy(floorSheetAndCapeRequired = enabled) }
                },
                onCaptureCameraReference = {
                    val snap = PhonePermissionsHelper.inspectPermissions(context)
                    permissionSnapshot = snap
                    if (snap.hasCamera) {
                        cameraPreviewLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onPickGalleryReference = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        // 7. Personal Contact Details
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Personal Contact Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Number (Shared only on confirmed visit)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.updateProfile(name, phone) },
                        enabled = !isBusy && name.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("account_save_profile_button")
                    ) {
                        Text("Save Contact Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 8. Partner Barber Registration OR Studio Switcher
        if (userAccount.role == "CUSTOMER") {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Own a Barber Shop or Do Home Visits?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Customers pay ₹0 platform fee so you get more home bookings! DoorCut charges a transparent partner commission only on completed cuts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!showBarberApply) {
                            Button(
                                onClick = { showBarberApply = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Register as Partner Barber & Shop", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedTextField(
                                value = barberShopName,
                                onValueChange = { barberShopName = it },
                                label = { Text("Barber Shop / Studio Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = barberShopAddress,
                                onValueChange = { barberShopAddress = it },
                                label = { Text("Shop Street Address") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = barberSkillsText,
                                onValueChange = { barberSkillsText = it },
                                label = { Text("Haircut & Grooming Skills (comma separated)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = barberBio,
                                onValueChange = { barberBio = it },
                                label = { Text("Short Bio & Specialties") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = barberExp,
                                    onValueChange = { barberExp = it.filter { c -> c.isDigit() } },
                                    label = { Text("Years Exp") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.width(120.dp)
                                )
                                OutlinedTextField(
                                    value = barberUpi,
                                    onValueChange = { barberUpi = it },
                                    label = { Text("Direct Payment UPI ID") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.applyAsBarber(
                                        name = name,
                                        phone = phone,
                                        bio = barberBio,
                                        experienceYears = barberExp.toLongOrNull() ?: 3L,
                                        upiId = barberUpi,
                                        shopName = barberShopName.ifBlank { "${name.substringBefore(" ")}'s Barber Studio" },
                                        shopAddress = barberShopAddress.ifBlank { "Local Partner Hub" },
                                        skillsText = barberSkillsText,
                                        serviceIds = canonicalGroomingCatalog().map { it.id },
                                        serviceAreaIds = serviceAreas.map { it.id }
                                    )
                                    showBarberApply = false
                                },
                                enabled = !isBusy && name.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Submit Barber & Shop for Approval", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Partner & Operations Console",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (userAccount.role == "BARBER" || userAccount.role == "ADMIN") {
                                Button(
                                    onClick = {
                                        viewModel.switchPortal(ActivePortal.BARBER)
                                        onSwitchToBarberPortal()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Open Barber Studio")
                                }
                            }
                            if (userAccount.role == "ADMIN") {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.switchPortal(ActivePortal.ADMIN)
                                        onSwitchToAdminPortal()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Admin Console")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 9. Concierge Helpline + Support & Dispute Tickets
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Concierge Support & Helpline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001209900"))
                                try {
                                    context.startActivity(dialIntent)
                                } catch (_: Exception) {
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Helpline")
                        }
                    }

                    val ticketCategories = listOf(
                        "PAYMENT_DISPUTE" to "Payment Dispute",
                        "LATE_OR_NO_SHOW" to "Late / No-Show",
                        "SAFETY_CONCERN" to "Safety Concern",
                        "GENERAL" to "General Help"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ticketCategories) { (catCode, catLabel) ->
                            FilterChip(
                                selected = ticketCategory == catCode,
                                onClick = { ticketCategory = catCode },
                                label = { Text(catLabel) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = ticketBookingRef,
                        onValueChange = { ticketBookingRef = it },
                        label = { Text("Booking Reference (Optional, e.g. DC-20261026-1042)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ticketSubject,
                        onValueChange = { ticketSubject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ticketDesc,
                        onValueChange = { ticketDesc = it },
                        label = { Text("Details") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val cRole = if (userAccount.role == "BARBER") "BARBER" else "CUSTOMER"
                            viewModel.createSupportTicket(cRole, ticketBookingRef, ticketCategory, ticketSubject, ticketDesc)
                            ticketSubject = ""
                            ticketDesc = ""
                        },
                        enabled = !isBusy && ticketSubject.isNotBlank() && ticketDesc.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Support Ticket", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (tickets.isNotEmpty()) {
            item {
                Text(
                    text = "My Support Tickets (${tickets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(tickets, key = { it.id }) { t ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            StatusBadge(t.status)
                        }
                        Text(
                            text = "${t.category.replace("_", " ")} • Ref: ${t.bookingId.ifBlank { "General" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(t.description, style = MaterialTheme.typography.bodySmall)
                        if (t.adminResolution.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Admin Resolution: ${t.adminResolution}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }

        // 10. Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("account_sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of DoorCut", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Guest Account Hub shown on the `Account` tab prior to signing in so customers can immediately
 * manage Phone Permissions, Card/UPI Payment Options, Grooming Style Preferences, and Sign In
 * to unlock Previous Bookings.
 */
@Composable
fun GuestAccountHubScreen(
    detectedLocation: DetectedDoorLocation?,
    pendingDoorDraft: PendingDoorAddressDraft?,
    selectedServiceCount: Int,
    onNavigateToServices: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToDoorSetup: () -> Unit,
    onRefreshGps: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    LaunchedEffect(Unit) { CustomerPreferencesStore.ensureLoaded(context) }
    val prefs by CustomerPreferencesStore.state.collectAsStateWithLifecycle()

    var isSigningIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var permissionSnapshot by remember {
        mutableStateOf(PhonePermissionsHelper.inspectPermissions(context))
    }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var notificationFeedback by remember { mutableStateOf<String?>(null) }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionSnapshot = PhonePermissionsHelper.inspectPermissions(context)
        if (permissionSnapshot.hasAnyLocation) onRefreshGps()
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            capturedBitmap = bmp
            CustomerPreferencesStore.update(context) {
                it.copy(
                    referencePhotoAttached = true,
                    referencePhotoNote = "Camera hairstyle reference captured"
                )
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionSnapshot = PhonePermissionsHelper.inspectPermissions(context)
        if (granted) cameraPreviewLauncher.launch(null)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            CustomerPreferencesStore.update(context) {
                it.copy(
                    referencePhotoAttached = true,
                    referencePhotoNote = "Gallery hairstyle reference selected"
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_account_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                eyebrow = stringResource(R.string.account_eyebrow),
                title = stringResource(R.string.account_title),
                subtitle = stringResource(R.string.account_subtitle)
            )
        }

        // Google Sign-In & Previous Bookings Unlock Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EspressoHeaderBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_previous_bookings_section")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = BrassGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign In to Sync Previous Bookings & Receipts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "View completed home haircuts, download zero-fee receipts, rate master barbers, and rebook your exact haircut in one tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD6C8B6)
                    )
                    if (signInError != null) {
                        Text(
                            text = signInError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB4AB)
                        )
                    }
                    Button(
                        onClick = {
                            isSigningIn = true
                            signInError = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthSuccess()
                                },
                                onAuthError = { err ->
                                    isSigningIn = false
                                    signInError = err
                                },
                                scope = scope,
                                onAuthCancelled = { isSigningIn = false }
                            )
                        },
                        enabled = !isSigningIn,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrassGold,
                            contentColor = EspressoHeaderBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("account_google_sign_in_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = EspressoHeaderBg,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.btn_sign_in_google),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick Features Grid
        item {
            AccountQuickFeaturesGrid(
                selectedServiceCount = selectedServiceCount,
                savedDoorsCount = if (pendingDoorDraft != null) 1 else 0,
                bookingsCount = 0,
                onNavigateToServices = onNavigateToServices,
                onNavigateToCart = onNavigateToCart,
                onNavigateToAddresses = onNavigateToDoorSetup,
                onNavigateToBookings = onNavigateToCart,
                onNavigateToDoorSetup = onNavigateToDoorSetup
            )
        }

        // Phone Permissions Readiness
        item {
            PhonePermissionsAccountCard(
                snapshot = permissionSnapshot,
                notificationFeedback = notificationFeedback,
                onRequestAllPermissions = {
                    multiPermissionLauncher.launch(PhonePermissionsHelper.allFeaturePermissionsArray())
                },
                onSendTestNotification = {
                    val sent = DoorCutNotificationHelper.sendVisitNotification(
                        context = context,
                        title = "DoorCut Arrival Alert Ready",
                        message = "Notifications are enabled on your phone for live barber arrival updates."
                    )
                    notificationFeedback = if (sent) {
                        "Test arrival notification sent!"
                    } else {
                        "Please allow Notifications permission first."
                    }
                },
                onOpenSystemSettings = { PhonePermissionsHelper.openAppSettings(context) }
            )
        }

        // Doorstep Card / UPI / Cash Selector
        item {
            DoorstepCardAndPaymentSelector(
                selectedOption = prefs.paymentOption,
                savedCardLabel = prefs.savedCardLabel,
                savedCardLast4 = prefs.savedCardLast4,
                savedUpiId = prefs.savedUpiId,
                onSelectOption = { opt ->
                    CustomerPreferencesStore.update(context) { it.copy(paymentOption = opt) }
                },
                onUpdateCardDetails = { label, last4 ->
                    CustomerPreferencesStore.update(context) {
                        it.copy(savedCardLabel = label, savedCardLast4 = last4)
                    }
                },
                onUpdateUpiId = { upi ->
                    CustomerPreferencesStore.update(context) { it.copy(savedUpiId = upi) }
                },
                modifier = Modifier.testTag("account_payment_card_section")
            )
        }

        // Grooming Preferences + Camera Reference
        item {
            GroomingPreferencesCard(
                preferredCutStyle = prefs.preferredHaircutStyle,
                preferredOil = prefs.preferredOilType,
                quietSession = prefs.quietSession,
                floorSheetAndCape = prefs.floorSheetAndCapeRequired,
                referencePhotoAttached = prefs.referencePhotoAttached,
                referencePhotoNote = prefs.referencePhotoNote,
                capturedBitmap = capturedBitmap,
                onSelectCutStyle = { style ->
                    CustomerPreferencesStore.update(context) { it.copy(preferredHaircutStyle = style) }
                },
                onSelectOil = { oil ->
                    CustomerPreferencesStore.update(context) { it.copy(preferredOilType = oil) }
                },
                onToggleQuiet = { enabled ->
                    CustomerPreferencesStore.update(context) { it.copy(quietSession = enabled) }
                },
                onToggleFloorSheet = { enabled ->
                    CustomerPreferencesStore.update(context) { it.copy(floorSheetAndCapeRequired = enabled) }
                },
                onCaptureCameraReference = {
                    val snap = PhonePermissionsHelper.inspectPermissions(context)
                    permissionSnapshot = snap
                    if (snap.hasCamera) {
                        cameraPreviewLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onPickGalleryReference = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }
    }
}

@Composable
private fun AccountStatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFDF9E)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD6C8B6),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AccountQuickFeaturesGrid(
    selectedServiceCount: Int,
    savedDoorsCount: Int,
    bookingsCount: Int,
    onNavigateToServices: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToAddresses: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToDoorSetup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_features_grid"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "All DoorCut Features & Shortcuts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickFeatureTile(
                icon = Icons.Filled.ShoppingBag,
                title = "Cart & Card Pay",
                subtitle = if (selectedServiceCount > 0) "$selectedServiceCount selected" else "View cart & POS",
                onClick = onNavigateToCart,
                testTag = "account_tile_cart",
                modifier = Modifier.weight(1f)
            )
            QuickFeatureTile(
                icon = Icons.AutoMirrored.Filled.EventNote,
                title = "My Visits",
                subtitle = "$bookingsCount appointments",
                onClick = onNavigateToBookings,
                testTag = "account_tile_bookings",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickFeatureTile(
                icon = Icons.Filled.LocationOn,
                title = "Saved Doors",
                subtitle = "$savedDoorsCount home addresses",
                onClick = onNavigateToAddresses,
                testTag = "account_tile_addresses",
                modifier = Modifier.weight(1f)
            )
            QuickFeatureTile(
                icon = Icons.Filled.MyLocation,
                title = "GPS Door Lock",
                subtitle = "Detect coordinates",
                onClick = onNavigateToDoorSetup,
                testTag = "account_tile_gps_door",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickFeatureTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(EspressoHeaderBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BrassGold, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PreviousBookingSummaryCard(
    booking: Booking,
    onRepeatBooking: () -> Unit,
    onOpenReceipt: () -> Unit
) {
    Surface(
        onClick = onOpenReceipt,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_prev_booking_${booking.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${booking.dateKey} • ${CurrencyUtils.formatHourSlot(booking.startHour)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = booking.barberName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(booking.status)
            }

            Text(
                text = booking.serviceNamesSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${CurrencyUtils.formatRupees(booking.barberServicePayablePaise)} • ₹0 Platform Fee",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SageVerified
                )
                OutlinedButton(
                    onClick = onRepeatBooking,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("account_rebook_${booking.id}")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Book Again", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PhonePermissionsAccountCard(
    snapshot: com.example.util.PhonePermissionsSnapshot,
    notificationFeedback: String?,
    onRequestAllPermissions: () -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_permissions_section")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Phone Permissions & Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Required device permissions for GPS door & arrival notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(if (snapshot.allGrantedIncludingCamera) "ALL ACTIVE" else "${3 - snapshot.missingCount}/3 READY")
            }

            PermissionStatusRow(
                icon = Icons.Filled.LocationOn,
                title = "Precise & Coarse GPS Location",
                subtitle = "Detects exact house/street coordinates & ranks nearest barbers",
                granted = snapshot.hasAnyLocation
            )
            PermissionStatusRow(
                icon = Icons.Filled.NotificationsActive,
                title = "Live Barber Arrival Notifications",
                subtitle = "Alerts when barber accepts, starts trip, or rings your doorbell",
                granted = snapshot.hasNotifications
            )
            PermissionStatusRow(
                icon = Icons.Filled.CameraAlt,
                title = "Camera (Haircut Style Reference)",
                subtitle = "Optional — snap a reference hairstyle photo for your barber",
                granted = snapshot.hasCamera
            )

            if (notificationFeedback != null) {
                Text(
                    text = notificationFeedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = SageVerified,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!snapshot.allGrantedIncludingCamera) {
                    Button(
                        onClick = onRequestAllPermissions,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EspressoHeaderBg),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("account_grant_permissions_button")
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = BrassGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Allow Permissions", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onSendTestNotification,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("account_test_notification_button")
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Arrival Alert", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onOpenSystemSettings,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Phone App Settings", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settings")
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) SageVerified else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (granted) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SageVerified.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = SageVerified, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Allowed", style = MaterialTheme.typography.labelSmall, color = SageVerified, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                StatusBadge("ALLOW")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroomingPreferencesCard(
    preferredCutStyle: String,
    preferredOil: String,
    quietSession: Boolean,
    floorSheetAndCape: Boolean,
    referencePhotoAttached: Boolean,
    referencePhotoNote: String,
    capturedBitmap: Bitmap?,
    onSelectCutStyle: (String) -> Unit,
    onSelectOil: (String) -> Unit,
    onToggleQuiet: (Boolean) -> Unit,
    onToggleFloorSheet: (Boolean) -> Unit,
    onCaptureCameraReference: () -> Unit,
    onPickGalleryReference: () -> Unit
) {
    val cutStyles = listOf(
        "Normal Balanced Trim",
        "Traditional Neat Side-Part",
        "Mid Skin Fade & Crop",
        "Executive Scissor Layer"
    )
    val oilOptions = listOf(
        "Cooling Navratna Oil",
        "Pure Coconut Oil",
        "Warm Almond Oil",
        "No Oil / Dry Finish"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_grooming_preferences_section")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Personal Haircut & Hygiene Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automatically shared in your booking instructions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Default Haircut Finish", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cutStyles.forEach { style ->
                    FilterChip(
                        selected = preferredCutStyle == style,
                        onClick = { onSelectCutStyle(style) },
                        label = { Text(style) }
                    )
                }
            }

            Text("Champi Oil Preference", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                oilOptions.forEach { oil ->
                    FilterChip(
                        selected = preferredOil == oil,
                        onClick = { onSelectOil(oil) },
                        label = { Text(oil) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quiet / Relaxing Cut Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Minimal conversation after initial style check", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = quietSession, onCheckedChange = onToggleQuiet)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Zero-Mess Floor Sheet & Fresh Cape", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Barber lays disposable floor mat & sweeps after cut", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = floorSheetAndCape, onCheckedChange = onToggleFloorSheet)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text("Hairstyle Reference Photo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (capturedBitmap != null) {
                Image(
                    bitmap = capturedBitmap.asImageBitmap(),
                    contentDescription = "Captured hairstyle reference",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else if (referencePhotoAttached) {
                Text(
                    text = "✓ $referencePhotoNote",
                    style = MaterialTheme.typography.labelMedium,
                    color = SageVerified,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCaptureCameraReference,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("account_capture_style_camera_button")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera")
                }
                OutlinedButton(
                    onClick = onPickGalleryReference,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("account_pick_style_photo_button")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Photo Picker")
                }
            }
        }
    }
}
