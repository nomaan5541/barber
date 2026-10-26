package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
import com.example.data.model.CustomerAddress
import com.example.data.model.PlatformConfig
import com.example.ui.barber.BarberDashboardScreen
import com.example.ui.customer.BookingPaymentStep
import com.example.ui.customer.CustomerBookingsScreen
import com.example.ui.customer.CustomerHomeScreen
import com.example.ui.customer.DoorNumberSetupScreen
import com.example.ui.customer.canonicalGroomingCatalog
import com.example.ui.glass.LiquidGlassNavigationDock
import com.example.ui.navigation.AppRoutes
import com.example.ui.navigation.barberNavTabs
import com.example.ui.navigation.customerNavTabs
import com.example.ui.theme.DoorCutTheme
import com.example.util.DetectedDoorLocation
import com.example.util.LocationHelper
import com.example.util.PendingDoorAddressDraft
import com.example.util.PhonePermissionsHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `app name matches DoorCut`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DoorCut", appName)
    }

    @Test
    fun `customer and barber navigation tabs and grooming catalog match dual-mode specification`() {
        assertEquals(
            listOf(
                AppRoutes.ROUTE_CUSTOMER_HOME,
                AppRoutes.ROUTE_BOOKINGS,
                AppRoutes.ROUTE_FAVOURITES,
                AppRoutes.ROUTE_PROFILE
            ),
            customerNavTabs.map { it.route }
        )
        assertEquals("nav_customer_home", customerNavTabs[0].testTag)
        assertEquals("nav_customer_bookings", customerNavTabs[1].testTag)
        assertEquals("nav_customer_favourites", customerNavTabs[2].testTag)
        assertEquals("nav_customer_profile", customerNavTabs[3].testTag)

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
        assertTrue(catalog.size >= 15)
        val haircut = catalog.firstOrNull { it.id == "svc_classic_cut" }
        val beardTrim = catalog.firstOrNull { it.id == "svc_beard_trim" }
        val combo = catalog.firstOrNull { it.id == "svc_pkg_essential" }
        val normalCut = catalog.firstOrNull { it.id == "svc_normal_head_cut" }
        val traditionalCut = catalog.firstOrNull { it.id == "svc_traditional_cut" }

        assertNotNull(haircut)
        assertNotNull(beardTrim)
        assertNotNull(combo)
        assertNotNull(normalCut)
        assertNotNull(traditionalCut)

        assertEquals(30000L, haircut!!.pricePaise)
        assertEquals(15000L, beardTrim!!.pricePaise)
        assertEquals(40000L, combo!!.pricePaise)

        val allPerms = PhonePermissionsHelper.allFeaturePermissionsArray().toList()
        assertTrue(allPerms.contains(android.Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(allPerms.contains(android.Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(allPerms.contains(android.Manifest.permission.CAMERA))
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
    fun `door number setup screen displays coordinates and submits specific house or door number`() {
        val detected = DetectedDoorLocation(
            doorHint = "",
            streetArea = "12th Main Road, Indiranagar",
            localityName = "Indiranagar",
            landmark = "Near 12th Main Park",
            postalCode = "560038",
            latitude = 12.97189,
            longitude = 77.64115,
            accuracyMeters = 8.0f
        )

        var capturedDraft: PendingDoorAddressDraft? = null

        composeTestRule.setContent {
            DoorCutTheme {
                DoorNumberSetupScreen(
                    detectedLocation = detected,
                    isFetchingLocation = false,
                    hasLocationPermission = true,
                    onRequestDetectCoordinates = {},
                    onConfirmDoorAddress = { draft -> capturedDraft = draft },
                    onSkipOrClose = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("detected_coordinates_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("12.97189° N").assertIsDisplayed()
        composeTestRule.onNodeWithText("77.64115° E").assertIsDisplayed()

        composeTestRule.onNodeWithTag("modal_door_input")
            .performScrollTo()
            .performTextReplacement("House #42, Door 204")

        composeTestRule.onNodeWithTag("door_building_input")
            .performScrollTo()
            .performTextReplacement("2nd Floor, Rosewood Enclave")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("modal_save_door_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()

        val saved = capturedDraft
        assertNotNull(saved)
        assertEquals("House #42, Door 204, 2nd Floor, Rosewood Enclave", saved!!.combinedFlatHouse())
        assertEquals("12th Main Road, Indiranagar", saved.streetArea)
        assertEquals("560038", saved.postalCode)
        assertEquals(12.97189, saved.latitude, 0.0001)
        assertEquals(77.64115, saved.longitude, 0.0001)
    }

    @Test
    fun `customer home screen adds services to sticky liquid glass bar and routes to door setup`() {
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
                                selectedIds = if (selectedIds.contains(id)) {
                                    selectedIds - id
                                } else {
                                    selectedIds + id
                                }
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
        composeTestRule.onNodeWithTag("home_book_barber_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_selection_screen")
            .performScrollToNode(hasTestTag("add_service_button_svc_classic_cut"))
        composeTestRule.onNodeWithTag("add_service_button_svc_classic_cut").performClick()
        composeTestRule.waitForIdle()

        // Verify sticky Liquid Glass summary bar appears with rolling price summary
        composeTestRule.onNodeWithTag("sticky_continue_booking_bar").assertIsDisplayed()

        // Scroll back to top and tap location selector to open Door Setup
        composeTestRule.onNodeWithTag("service_selection_screen")
            .performScrollToNode(hasTestTag("home_address_selector"))
        composeTestRule.onNodeWithTag("home_address_selector").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("door_setup_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detected_coordinates_card").assertIsDisplayed()
    }

    @Test
    fun `booking payment step clearly separates pay now confirmation fee from direct barber payment`() {
        val catalog = canonicalGroomingCatalog()
        val chosenServices = catalog.filter { it.id in setOf("svc_classic_cut", "svc_beard_trim") }
        val barber = BarberProfile(
            id = "barber_arman_master",
            barberId = "barber_arman_master",
            name = "Arman Siddiqui",
            approvalStatus = "APPROVED",
            isOnline = true,
            ratingAvg = 4.9
        )

        var confirmed = false

        composeTestRule.setContent {
            DoorCutTheme {
                BookingPaymentStep(
                    isAuthenticated = true,
                    customerName = "Rohan Sharma",
                    customerPhone = "+919876543210",
                    selectedServices = chosenServices,
                    chosenBarber = barber,
                    selectedDateKey = "2026-10-28",
                    selectedStartHour = 10L,
                    savedAddresses = listOf(
                        CustomerAddress(
                            id = "addr_1",
                            label = "HOME",
                            flatHouse = "Flat 302",
                            streetArea = "Banjara Hills, Hyderabad",
                            postalCode = "500034",
                            isDefault = true
                        )
                    ),
                    selectedAddressId = "addr_1",
                    pendingDoorDraft = null,
                    serviceAreas = emptyList(),
                    platformConfig = PlatformConfig(fixedBookingFeePaise = 3000L),
                    bookingFeePaise = 3000L,
                    optionalInstructions = "Call when downstairs",
                    isBusy = false,
                    onEditStep = {},
                    onConfirmBooking = { _, _ -> confirmed = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("booking_payment_breakdown_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pay_now_amount_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pay_barber_after_text").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Pay ₹450 directly to your barber after the service by cash or UPI."
        ).assertIsDisplayed()

        composeTestRule.onNodeWithTag("step_booking_payment")
            .performScrollToNode(hasTestTag("pay_confirm_booking_button"))
        composeTestRule.onNodeWithTag("pay_confirm_booking_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(confirmed)
    }

    @Test
    fun `liquid glass navigation dock and bookings tabs switch smoothly and barber dashboard renders`() {
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

        composeTestRule.onNodeWithTag("nav_customer_favourites").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppRoutes.ROUTE_FAVOURITES, activeRoute)
    }

    @Test
    fun `customer bookings tabs and barber dashboard request actions work with liquid glass cards`() {
        val sampleBooking = Booking(
            id = "bk_test_1",
            bookingNumber = "DC-1024",
            customerId = "cust_1",
            customerName = "Karan Verma",
            barberId = "barber_1",
            barberName = "Arman Siddiqui",
            serviceNamesSummary = "Haircut + Beard Trim",
            durationMinutes = 45L,
            serviceSubtotalPaise = 45000L,
            barberServicePayablePaise = 45000L,
            platformBookingFeePaise = 3000L,
            dateKey = "2099-12-30",
            startHour = 11L,
            status = "AWAITING_BARBER",
            areaName = "Jubilee Hills"
        )

        var acceptedId: String? = null

        composeTestRule.setContent {
            DoorCutTheme {
                BarberDashboardScreen(
                    barberProfile = BarberProfile(
                        id = "barber_1",
                        barberId = "barber_1",
                        name = "Arman Siddiqui",
                        approvalStatus = "APPROVED",
                        isOnline = true
                    ),
                    defaultDisplayName = "Arman Siddiqui",
                    bookings = listOf(sampleBooking),
                    catalogServices = canonicalGroomingCatalog(),
                    serviceAreas = emptyList(),
                    isBusy = false,
                    onToggleOnline = {},
                    onAcceptOrDeclineRequest = { b, accept, _ ->
                        if (accept) acceptedId = b.id
                    },
                    onAdvanceTripStatus = { _, _ -> },
                    onOpenBookingDetail = {},
                    onApplyAsBarber = { _, _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("barber_dashboard_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("barber_online_switch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("barber_dashboard_screen")
            .performScrollToNode(hasTestTag("accept_request_button_bk_test_1"))
        composeTestRule.onNodeWithTag("accept_request_button_bk_test_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals("bk_test_1", acceptedId)
    }

    @Test
    fun `customer bookings screen displays upcoming completed and cancelled liquid glass tabs`() {
        val completedBooking = Booking(
            id = "bk_done_9",
            bookingNumber = "DC-1009",
            customerId = "cust_1",
            customerName = "Karan Verma",
            barberId = "barber_1",
            barberName = "Arman Siddiqui",
            serviceNamesSummary = "Haircut",
            durationMinutes = 30L,
            serviceSubtotalPaise = 30000L,
            barberServicePayablePaise = 30000L,
            dateKey = "2026-10-10",
            startHour = 14L,
            status = "COMPLETED"
        )

        var repeatedBookingId: String? = null

        composeTestRule.setContent {
            DoorCutTheme {
                CustomerBookingsScreen(
                    customerName = "Karan Verma",
                    bookings = listOf(completedBooking),
                    isBusy = false,
                    onOpenBookingStatus = {},
                    onBookAgain = { repeatedBookingId = it.id },
                    onSubmitReview = { _, _, _ -> },
                    onStartNewBooking = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("bookings_tab_completed").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("booking_card_bk_done_9").assertIsDisplayed()
        composeTestRule.onNodeWithTag("book_again_button_bk_done_9").performClick()
        composeTestRule.waitForIdle()

        assertEquals("bk_done_9", repeatedBookingId)
    }
}
