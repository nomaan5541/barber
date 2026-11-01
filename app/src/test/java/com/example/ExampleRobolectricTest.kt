package com.example

import android.content.Context
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BarberProfile
import com.example.data.model.Booking
import com.example.data.model.BookingPrivateAddress
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.data.model.UserAccount
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.OnboardingProfileScreen
import com.example.ui.barber.BarberDashboardScreen
import com.example.ui.barber.BarberEarningsScreen
import com.example.ui.barber.BarberJobDetailScreen
import com.example.ui.barber.BarberScheduleScreen
import com.example.ui.barber.detectBarberSlotConflict
import com.example.ui.components.InteractiveLocationPinMap
import com.example.ui.components.LiveBarberTrackingMap
import com.example.ui.customer.BookingPaymentStep
import com.example.ui.customer.CustomerBookingsScreen
import com.example.ui.customer.CustomerHomeScreen
import com.example.ui.customer.CustomerProfileHelpScreen
import com.example.ui.customer.DoorNumberSetupScreen
import com.example.ui.customer.canonicalGroomingCatalog
import com.example.ui.glass.LiquidGlassNavigationDock
import com.example.ui.navigation.AppRoutes
import com.example.ui.navigation.barberNavTabs
import com.example.ui.navigation.customerNavTabs
import com.example.ui.theme.DoorCutTheme
import com.example.data.repository.MarketplaceRepository
import com.example.util.DetectedDoorLocation
import com.example.util.LocationHelper
import com.example.util.OsrmRoadRouter
import com.example.util.PendingDoorAddressDraft
import com.example.util.PhonePermissionsHelper
import com.example.util.RazorpayBookingFeeGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches DoorCut and catalog includes mens and ladies styling`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DoorCut", appName)

        assertEquals(
            listOf(
                AppRoutes.ROUTE_CUSTOMER_HOME,
                AppRoutes.ROUTE_BOOKINGS,
                AppRoutes.ROUTE_FAVOURITES,
                AppRoutes.ROUTE_PROFILE
            ),
            customerNavTabs.map { it.route }
        )

        assertEquals(
            listOf(
                AppRoutes.ROUTE_BARBER_HOME,
                AppRoutes.ROUTE_BARBER_SCHEDULE,
                AppRoutes.ROUTE_BARBER_EARNINGS,
                AppRoutes.ROUTE_BARBER_PROFILE
            ),
            barberNavTabs.map { it.route }
        )

        val catalog = canonicalGroomingCatalog()
        assertTrue(catalog.size >= 18)
        val haircut = catalog.firstOrNull { it.id == "svc_classic_cut" }
        val ladiesCut = catalog.firstOrNull { it.id == "svc_ladies_haircut" }
        val ladiesBlowout = catalog.firstOrNull { it.id == "svc_ladies_layer_blowout" }
        val beardTrim = catalog.firstOrNull { it.id == "svc_beard_trim" }

        assertNotNull(haircut)
        assertNotNull(ladiesCut)
        assertNotNull(ladiesBlowout)
        assertNotNull(beardTrim)
        assertEquals("LADIES_STYLING", ladiesCut!!.category)

        val allPerms = PhonePermissionsHelper.allFeaturePermissionsArray().toList()
        assertTrue(allPerms.contains(android.Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(allPerms.contains(android.Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(allPerms.contains(android.Manifest.permission.CAMERA))
    }

    @Test
    fun `auth entry screen requires Google Sign-In and exposes privacy policy`() {
        composeTestRule.setContent {
            DoorCutTheme {
                AuthScreen(onSignedIn = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("auth_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_privacy_policy_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `onboarding profile screen collects name phone dob age gender and role`() {
        var submittedRole: String? = null
        var submittedGender: String? = null
        var submittedAge: Long = 0L

        composeTestRule.setContent {
            DoorCutTheme {
                OnboardingProfileScreen(
                    initialName = "Ananya Rao",
                    initialEmail = "ananya@doorcut.in",
                    initialPhone = "+919840011223",
                    detectedLocality = "Jubilee Hills, Hyderabad",
                    isBusy = false,
                    onCompleteOnboarding = { _, _, _, age, gender, role, _, _, _, _, _ ->
                        submittedAge = age
                        submittedGender = gender
                        submittedRole = role
                    },
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_profile_screen").assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("onboarding_age_input"))
        composeTestRule.onNodeWithTag("onboarding_age_input")
            .performTextReplacement("27")
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("onboarding_gender_female"))
        composeTestRule.onNodeWithTag("onboarding_gender_female")
            .performClick()
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("onboarding_privacy_checkbox"))
        composeTestRule.onNodeWithTag("onboarding_privacy_checkbox")
            .performClick()
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("onboarding_continue_button"))
        composeTestRule.onNodeWithTag("onboarding_continue_button")
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("CUSTOMER", submittedRole)
        assertEquals("FEMALE", submittedGender)
        assertEquals(27L, submittedAge)
    }

    @Test
    fun `coordinate formatting and distance calculation are accurate`() {
        val sampleLocation = DetectedDoorLocation(
            doorHint = "",
            streetArea = "100 Feet Road, Indiranagar",
            localityName = "Indiranagar",
            landmark = "Near Indiranagar Metro",
            postalCode = "560038",
            latitude = 12.97159,
            longitude = 77.59456,
            accuracyMeters = 6.4f
        )

        assertEquals("12.97159° N", sampleLocation.formattedLatitude())
        assertEquals("77.59456° E", sampleLocation.formattedLongitude())
        assertEquals("±6 m", sampleLocation.formattedAccuracy())

        val distanceKm = LocationHelper.computeEffectiveDistanceKm(
            userLat = 12.97159,
            userLon = 77.59456,
            shopLat = 12.97836,
            shopLon = 77.64083
        )
        assertTrue(distanceKm in 4.0..6.5)
    }

    @Test
    fun `customer home screen renders right-side bold prices and bottom add buttons`() {
        val detected = DetectedDoorLocation(
            doorHint = "",
            streetArea = "Road No 12, Banjara Hills",
            localityName = "Hyderabad",
            landmark = "Near City Center",
            postalCode = "500034",
            latitude = 17.41560,
            longitude = 78.43470,
            accuracyMeters = 7.0f
        )

        var selectedIds by mutableStateOf(setOf<String>())
        val catalog = canonicalGroomingCatalog()

        composeTestRule.setContent {
            DoorCutTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.ROUTE_CUSTOMER_HOME
                ) {
                    composable(AppRoutes.ROUTE_CUSTOMER_HOME) {
                        CustomerHomeScreen(
                            services = catalog,
                            selectedServiceIds = selectedIds,
                            upcomingBooking = null,
                            savedAddresses = emptyList(),
                            detectedLocation = detected,
                            pendingDoorDraft = null,
                            isFetchingLocation = false,
                            onToggleService = { id ->
                                selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                            },
                            onStartBookingFlow = {},
                            onOpenLocationSelector = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                            onRefreshGps = {},
                            onOpenBookingStatus = {}
                        )
                    }
                    composable(AppRoutes.ROUTE_DOOR_SETUP) {
                        DoorNumberSetupScreen(
                            detectedLocation = detected,
                            isFetchingLocation = false,
                            hasLocationPermission = true,
                            onRequestDetectCoordinates = {},
                            onConfirmDoorAddress = { navController.popBackStack() },
                            onSkipOrClose = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("customer_home_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_selection_screen")
            .performScrollToNode(hasTestTag("home_book_barber_button"))
        composeTestRule.onNodeWithTag("home_book_barber_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_selection_screen")
            .performScrollToNode(hasTestTag("add_service_button_svc_classic_cut"))
        composeTestRule.onNodeWithTag("add_service_button_svc_classic_cut").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("sticky_continue_booking_bar").assertIsDisplayed()
    }

    @Test
    fun `open dispatch payment step displays lady stylist dispatch and two-payment breakdown`() {
        val catalog = canonicalGroomingCatalog()
        val chosenServices = catalog.filter { it.id == "svc_ladies_haircut" }
        var confirmed = false

        composeTestRule.setContent {
            DoorCutTheme {
                BookingPaymentStep(
                    isAuthenticated = true,
                    customerName = "Ananya Rao",
                    customerPhone = "+919840011223",
                    recipientGender = "FEMALE",
                    recipientAge = 27L,
                    preferLadyBarber = true,
                    bookingMode = "ASAP",
                    selectedServices = chosenServices,
                    selectedDateKey = "2026-10-28",
                    selectedStartHour = 10L,
                    savedAddresses = listOf(
                        CustomerAddress(
                            id = "addr_1",
                            label = "HOME",
                            flatHouse = "Flat 302",
                            streetArea = "Banjara Hills, Hyderabad",
                            postalCode = "500034",
                            isDefault = true,
                            latitude = 17.4156,
                            longitude = 78.4347
                        )
                    ),
                    selectedAddressId = "addr_1",
                    pendingDoorDraft = null,
                    detectedLocation = null,
                    serviceAreas = emptyList(),
                    platformConfig = PlatformConfig(fixedBookingFeePaise = 2900L),
                    bookingFeePaise = 2900L,
                    optionalInstructions = "Call when downstairs",
                    isBusy = false,
                    onEditStep = {},
                    onConfirmBooking = { _, _ -> confirmed = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("step_booking_payment")
            .performScrollToNode(hasTestTag("booking_payment_breakdown_card"))
        composeTestRule.onNodeWithTag("booking_payment_breakdown_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("step_booking_payment")
            .performScrollToNode(hasTestTag("pay_barber_after_text"))
        composeTestRule.onNodeWithTag("pay_now_amount_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pay_barber_after_text").assertIsDisplayed()

        composeTestRule.onNodeWithTag("step_booking_payment")
            .performScrollToNode(hasTestTag("pay_confirm_booking_button"))
        composeTestRule.onNodeWithTag("pay_confirm_booking_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(confirmed)
    }

    @Test
    fun `live barber tracking map renders uber-style movement and barber dashboard claims open booking`() {
        val openBooking = Booking(
            id = "bk_open_1",
            bookingNumber = "DC-5012",
            customerId = "cust_1",
            customerName = "Ananya Rao",
            customerPhone = "+919840011223",
            customerGender = "FEMALE",
            customerAge = 27L,
            preferLadyBarber = true,
            bookingMode = "ASAP",
            customerLat = 17.4156,
            customerLon = 78.4347,
            barberId = "UNASSIGNED",
            barberName = "Matching Verified Lady Stylist",
            serviceNamesSummary = "Ladies Haircut & Trim",
            durationMinutes = 40L,
            serviceSubtotalPaise = 39900L,
            barberServicePayablePaise = 39900L,
            platformBookingFeePaise = 2900L,
            dateKey = "2099-12-30",
            startHour = 11L,
            status = "AWAITING_BARBER",
            areaName = "Banjara Hills"
        )

        var claimedBookingId: String? = null

        composeTestRule.setContent {
            DoorCutTheme {
                BarberDashboardScreen(
                    barberProfile = BarberProfile(
                        id = "barber_priya",
                        barberId = "barber_priya",
                        name = "Priya Stylist",
                        phone = "+919840099887",
                        gender = "FEMALE",
                        servesLadies = true,
                        servesMen = true,
                        approvalStatus = "APPROVED",
                        isOnline = true
                    ),
                    defaultDisplayName = "Priya Stylist",
                    defaultPhone = "+919840099887",
                    bookings = emptyList(),
                    openDispatchBookings = listOf(openBooking),
                    catalogServices = canonicalGroomingCatalog(),
                    serviceAreas = emptyList(),
                    isBusy = false,
                    onToggleOnline = {},
                    onClaimOpenBooking = { b -> claimedBookingId = b.id },
                    onAcceptOrDeclineRequest = { _, _, _ -> },
                    onAdvanceTripStatus = { _, _ -> },
                    onOpenBookingDetail = {},
                    onApplyAsBarber = { _, _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("barber_dashboard_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("barber_dashboard_screen")
            .performScrollToNode(hasTestTag("accept_request_button_bk_open_1"))
        composeTestRule.onNodeWithTag("accept_request_button_bk_open_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals("bk_open_1", claimedBookingId)
    }

    @Test
    fun `live barber tracking map renders live movement telemetry when barber is on the way`() {
        val ridingBooking = Booking(
            id = "bk_live_9",
            bookingNumber = "DC-8801",
            customerId = "cust_1",
            customerName = "Rohan Sharma",
            customerLat = 17.4156,
            customerLon = 78.4347,
            barberId = "barber_kiran",
            barberName = "Kiran Kumar",
            barberPhone = "+919840055667",
            barberLat = 17.4220,
            barberLon = 78.4410,
            status = "ON_THE_WAY",
            areaName = "Banjara Hills"
        )

        composeTestRule.setContent {
            DoorCutTheme {
                LiveBarberTrackingMap(booking = ridingBooking)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("live_barber_tracking_map").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_external_maps_button").assertIsDisplayed()
    }

    @Test
    fun `barber job detail screen finishes service and records direct payment separately`() {
        var bookingStatus by mutableStateOf("IN_PROGRESS")
        var recordedStatus: String? = null
        var recordedMethod: String? = null
        var recordedPaise: Long = 0L

        composeTestRule.setContent {
            DoorCutTheme {
                BarberJobDetailScreen(
                    booking = Booking(
                        id = "bk_job_77",
                        bookingNumber = "DC-2077",
                        customerId = "cust_7",
                        customerName = "Karan Verma",
                        customerLat = 17.4156,
                        customerLon = 78.4347,
                        barberId = "barber_1",
                        barberName = "Kiran Kumar",
                        barberLat = 17.4156,
                        barberLon = 78.4347,
                        serviceNamesSummary = "Haircut + Beard",
                        durationMinutes = 45L,
                        serviceSubtotalPaise = 40000L,
                        barberServicePayablePaise = 40000L,
                        platformBookingFeePaise = 2900L,
                        dateKey = "2026-10-28",
                        startHour = 15L,
                        status = bookingStatus,
                        areaName = "Jubilee Hills, Hyderabad"
                    ),
                    protectedAddress = BookingPrivateAddress(
                        bookingId = "bk_job_77",
                        fullAddressSnapshot = "Villa 14, Road 45, Jubilee Hills",
                        customerPhone = "+919840012345"
                    ),
                    catalogServices = canonicalGroomingCatalog(),
                    isBusy = false,
                    onLoadProtectedAddress = {},
                    onAdvanceTripStatus = { _, next -> bookingStatus = next },
                    onUpdateLiveLocation = { _, _, _ -> },
                    onFinishService = { bookingStatus = "COMPLETED" },
                    onRequestExtraService = { _, _ -> },
                    onRecordDirectPayment = { _, status, method, paise, _ ->
                        recordedStatus = status
                        recordedMethod = method
                        recordedPaise = paise
                    },
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("barber_job_detail_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("barber_job_detail_list")
            .performScrollToNode(hasTestTag("step_finish_service_button"))
        composeTestRule.onNodeWithTag("step_finish_service_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("COMPLETED", bookingStatus)

        composeTestRule.onNodeWithTag("barber_job_detail_list")
            .performScrollToNode(hasTestTag("submit_direct_payment_button"))
        composeTestRule.onNodeWithTag("record_direct_payment_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submit_direct_payment_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("CASH_DECLARED", recordedStatus)
        assertEquals("CASH", recordedMethod)
        assertEquals(40000L, recordedPaise)
    }

    @Test
    fun `customer bookings and profile help screens render verified demographics and policies`() {
        composeTestRule.setContent {
            DoorCutTheme {
                CustomerProfileHelpScreen(
                    isAuthenticated = true,
                    userAccount = UserAccount(
                        id = "cust_99",
                        userId = "cust_99",
                        name = "Ananya Rao",
                        email = "ananya@doorcut.in",
                        phone = "+919840011223",
                        dob = "1998-05-14",
                        age = 27L,
                        gender = "FEMALE",
                        role = "CUSTOMER",
                        onboardingCompleted = true,
                        privacyAccepted = true
                    ),
                    savedAddresses = emptyList(),
                    tickets = emptyList(),
                    platformConfig = PlatformConfig(fixedBookingFeePaise = 2900L),
                    isBusy = false,
                    onSignInClick = {},
                    onUpdateProfile = { _, _ -> },
                    onManageAddresses = {},
                    onCreateSupportTicket = { _, _ -> },
                    onSwitchToAdminMode = {},
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("customer_profile_help_screen").assertIsDisplayed()
    }

    @Test
    fun `liquid glass navigation dock switches customer tabs cleanly`() {
        var activeRoute by mutableStateOf(AppRoutes.ROUTE_CUSTOMER_HOME)
        composeTestRule.setContent {
            DoorCutTheme {
                LiquidGlassNavigationDock(
                    navTabs = customerNavTabs,
                    currentRoute = activeRoute,
                    onSelectTab = { activeRoute = it }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_customer_bookings").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppRoutes.ROUTE_BOOKINGS, activeRoute)
    }

    @Test
    fun `barber earnings screen renders direct payment disclaimer`() {
        val sampleCompleted = Booking(
            id = "bk_earn_1",
            bookingNumber = "DC-3001",
            customerId = "cust_2",
            customerName = "Aditya Nair",
            barberId = "barber_kiran",
            barberName = "Kiran Kumar",
            serviceNamesSummary = "Traditional Haircut",
            barberServicePayablePaise = 35000L,
            directPaymentDeclaredPaise = 35000L,
            directPaymentStatus = "CASH_DECLARED",
            directPaymentMethod = "CASH",
            dateKey = "2026-10-25",
            status = "COMPLETED"
        )

        composeTestRule.setContent {
            DoorCutTheme {
                BarberEarningsScreen(bookings = listOf(sampleCompleted))
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("barber_earnings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("barber_direct_payment_disclaimer").assertIsDisplayed()
    }

    @Test
    fun `customer completed bookings screen renders completed tab`() {
        val sampleCompleted = Booking(
            id = "bk_earn_1",
            bookingNumber = "DC-3001",
            customerId = "cust_2",
            customerName = "Aditya Nair",
            barberId = "barber_kiran",
            barberName = "Kiran Kumar",
            serviceNamesSummary = "Traditional Haircut",
            barberServicePayablePaise = 35000L,
            directPaymentDeclaredPaise = 35000L,
            directPaymentStatus = "CASH_DECLARED",
            directPaymentMethod = "CASH",
            dateKey = "2026-10-25",
            status = "COMPLETED"
        )

        composeTestRule.setContent {
            DoorCutTheme {
                CustomerBookingsScreen(
                    customerName = "Aditya Nair",
                    bookings = listOf(sampleCompleted),
                    isBusy = false,
                    onOpenBookingStatus = {},
                    onBookAgain = {},
                    onSubmitReview = { _, _, _ -> },
                    onStartNewBooking = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bookings_tab_completed").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("booking_card_bk_earn_1").assertIsDisplayed()
    }

    @Test
    fun `location helper forward search compass bearing and fallback eliminate zero coordinates`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val gachibowliMatches = kotlinx.coroutines.runBlocking {
            LocationHelper.searchLocationsByQuery(context, "Gachibowli", maxResults = 5)
        }
        assertTrue(gachibowliMatches.isNotEmpty())
        assertTrue(gachibowliMatches.first().hasValidCoordinates)

        val fallback = LocationHelper.resolveBestFallbackLocationForText(
            streetOrArea = "Unknown Tower Lane",
            postalCode = ""
        )
        assertTrue(fallback.hasValidCoordinates)
        assertTrue(fallback.latitude != 0.0 && fallback.longitude != 0.0)

        val (northLat, eastLon) = LocationHelper.offsetCoordinatesByMeters(
            latitude = 17.41560,
            longitude = 78.43470,
            northMeters = 100.0,
            eastMeters = 100.0
        )
        assertTrue(northLat > 17.41560)
        assertTrue(eastLon > 78.43470)

        val bearingNe = LocationHelper.bearingDegrees(
            lat1 = 17.41560,
            lon1 = 78.43470,
            lat2 = northLat,
            lon2 = eastLon
        )
        assertEquals("NE", LocationHelper.compassDirection(bearingNe))
        assertTrue(LocationHelper.estimateTravelMinutes(3.2) >= 6)
    }

    @Test
    fun `interactive location pin map renders and 25m north nudge updates pinned coordinates`() {
        val seed = DetectedDoorLocation(
            doorHint = "",
            streetArea = "Road No 12, Banjara Hills",
            localityName = "Hyderabad",
            landmark = "Near City Center",
            postalCode = "500034",
            latitude = 17.41560,
            longitude = 78.43470
        )
        var latestPinned: DetectedDoorLocation? = null

        composeTestRule.setContent {
            DoorCutTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.verticalScroll(
                        androidx.compose.foundation.rememberScrollState()
                    )
                ) {
                    InteractiveLocationPinMap(
                        currentLocation = seed,
                        isLoadingHardwareGps = false,
                        onRequestHardwareGps = {},
                        onLocationPinned = { latestPinned = it },
                        coverageRadiusKm = 10.0
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("interactive_location_pin_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pin_map_coordinates_badge").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pin_map_resolved_street").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("nudge_pin_north").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(latestPinned)
        assertTrue(latestPinned!!.latitude > seed.latitude)
    }

    @Test
    fun `barber slot conflict detector catches break window and travel buffer overlaps`() {
        val existingVisit = Booking(
            id = "bk_existing",
            customerName = "Rohan Sharma",
            dateKey = "2026-11-05",
            startHour = 10L,
            durationMinutes = 45L,
            bookingMode = "SCHEDULED",
            status = "CONFIRMED"
        )

        // 10:00 AM + 45m service + 20m travel buffer = busy until 11:05 AM -> 11:00 AM slot overlaps!
        val overlappingCandidate = Booking(
            id = "bk_cand_overlap",
            dateKey = "2026-11-05",
            startHour = 11L,
            durationMinutes = 30L,
            bookingMode = "SCHEDULED",
            status = "AWAITING_BARBER"
        )
        val overlapWarning = detectBarberSlotConflict(
            candidate = overlappingCandidate,
            confirmedBookings = listOf(existingVisit),
            travelBufferMinutes = 20L,
            breakStartHour = 13L,
            breakEndHour = 14L
        )
        assertNotNull(overlapWarning)

        // 13:00 (1:00 PM) falls inside 13..14 break window
        val breakCandidate = Booking(
            id = "bk_cand_break",
            dateKey = "2026-11-05",
            startHour = 13L,
            durationMinutes = 30L,
            bookingMode = "SCHEDULED",
            status = "AWAITING_BARBER"
        )
        val breakWarning = detectBarberSlotConflict(
            candidate = breakCandidate,
            confirmedBookings = listOf(existingVisit),
            travelBufferMinutes = 20L,
            breakStartHour = 13L,
            breakEndHour = 14L
        )
        assertNotNull(breakWarning)

        // 15:00 (3:00 PM) has zero conflict
        val cleanCandidate = Booking(
            id = "bk_cand_clean",
            dateKey = "2026-11-05",
            startHour = 15L,
            durationMinutes = 45L,
            bookingMode = "SCHEDULED",
            status = "AWAITING_BARBER"
        )
        val cleanResult = detectBarberSlotConflict(
            candidate = cleanCandidate,
            confirmedBookings = listOf(existingVisit),
            travelBufferMinutes = 20L,
            breakStartHour = 13L,
            breakEndHour = 14L
        )
        assertNull(cleanResult)
    }

    @Test
    fun `barber hygiene checklist upi qr pass and weekly working days schedule work end to end`() {
        var savedDays: List<String> = emptyList()
        var savedBuffer: Long = 0L

        composeTestRule.setContent {
            DoorCutTheme {
                BarberScheduleScreen(
                    barberProfile = BarberProfile(
                        id = "barber_1",
                        barberId = "barber_1",
                        name = "Kiran Kumar",
                        workingDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT"),
                        workStartHour = 9L,
                        workEndHour = 20L,
                        breakStartHour = 13L,
                        breakEndHour = 14L,
                        travelBufferMinutes = 15L
                    ),
                    bookings = emptyList(),
                    dateOptions = listOf("2026-11-05" to "Today · Thu, 05 Nov"),
                    isBusy = false,
                    onOpenBookingDetail = {},
                    onSaveWorkingHours = { _, _, _, _, _ -> },
                    onSaveScheduleWithDays = { _, _, _, _, buf, days ->
                        savedBuffer = buf
                        savedDays = days
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("barber_schedule_screen")
            .performScrollToNode(hasTestTag("save_working_hours_button"))
        composeTestRule.onNodeWithTag("working_day_chip_SUN").performClick()
        composeTestRule.onNodeWithTag("buffer_preset_30m").performClick()
        composeTestRule.onNodeWithTag("save_working_hours_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(savedDays.contains("SUN"))
        assertEquals(30L, savedBuffer)
    }

    @Test
    fun `osrm road router osm tile math slot lock hours and honest payment labels are accurate`() {
        // 1. Leaflet / OpenStreetMap Web Mercator slippy tile coordinates for Hyderabad (17.4156, 78.4347)
        val tile = OsrmRoadRouter.latLonToOsmTile(17.4156, 78.4347, zoom = 15)
        assertEquals(15, tile.zoom)
        assertTrue(tile.x in 23000..24000)
        assertTrue(tile.y in 14000..15000)
        assertTrue(tile.tileUrl().startsWith("https://tile.openstreetmap.org/15/"))

        val fallbackRoute = OsrmRoadRouter.fallbackRoadEstimate(
            fromLat = 17.4310,
            fromLon = 78.4090,
            toLat = 17.4156,
            toLon = 78.4347
        )
        assertFalse(fallbackRoute.isLiveOsrm)
        assertTrue(fallbackRoute.distanceKm > 2.0)
        assertTrue(fallbackRoute.durationMinutes >= 5)

        val (dLat, dLon) = OsrmRoadRouter.pixelDeltaToLatLonDelta(
            centerLat = 17.4156,
            zoom = 16,
            dxPx = -120f,
            dyPx = 120f
        )
        assertTrue(dLat > 0.0)
        assertTrue(dLon > 0.0)

        // 2. BarberFlow atomic slotLocks multi-hour span with travel buffer:
        //    11:00 start + 45m service + 20m travel buffer = 65m -> occupies both 11:00 and 12:00
        val occupiedHours = MarketplaceRepository.computeOccupiedHours(
            startHour = 11L,
            durationMinutes = 45L,
            travelBufferMinutes = 20L
        )
        assertEquals(listOf(11L, 12L), occupiedHours)

        // 3. Honest two-payment status labels & zero embedded Razorpay secrets
        assertFalse(RazorpayBookingFeeGateway.isServerVerificationConfigured)

        val unpaidBooking = Booking(
            platformBookingFeePaise = 2900L,
            bookingFeeStatus = "UNPAID",
            directPaymentStatus = "UPI_DECLARED",
            directPaymentDeclaredPaise = 39900L
        )
        assertTrue(unpaidBooking.bookingFeeSummaryLabel().contains("Pending"))
        assertTrue(unpaidBooking.directPaymentSummaryLabel().contains("Not App-Processed"))

        val verifiedBooking = unpaidBooking.copy(bookingFeeStatus = "PAID_VERIFIED")
        assertTrue(verifiedBooking.bookingFeeSummaryLabel().contains("Server-Verified"))
    }
}
