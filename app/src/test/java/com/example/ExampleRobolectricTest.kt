package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.example.ui.customer.DoorNumberSetupScreen
import com.example.ui.customer.ServiceSelectionLandingScreen
import com.example.ui.navigation.AppRoutes
import com.example.ui.theme.DoorCutTheme
import com.example.util.DetectedDoorLocation
import com.example.util.LocationHelper
import com.example.util.PendingDoorAddressDraft
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
    fun `compose navigation sets service selection as landing page with service grid placeholders and routes to door setup`() {
        val detected = DetectedDoorLocation(
            doorHint = "",
            streetArea = "12th Main Road, Indiranagar",
            localityName = "Indiranagar",
            landmark = "Near 12th Main Park",
            postalCode = "560038",
            latitude = 12.97189,
            longitude = 77.64115,
            accuracyMeters = 7.0f
        )

        var selectedIds = setOf<String>()

        composeTestRule.setContent {
            DoorCutTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.ROUTE_SERVICE_SELECTION
                ) {
                    composable(AppRoutes.ROUTE_SERVICE_SELECTION) {
                        ServiceSelectionLandingScreen(
                            detectedLocation = detected,
                            isFetchingLocation = false,
                            pendingDoorDraft = null,
                            selectedServiceIds = selectedIds,
                            onToggleService = { id ->
                                selectedIds = if (selectedIds.contains(id)) {
                                    selectedIds - id
                                } else {
                                    selectedIds + id
                                }
                            },
                            onOpenDoorSetup = { navController.navigate(AppRoutes.ROUTE_DOOR_SETUP) },
                            onRefreshGps = {},
                            onProceedToSignIn = {},
                            onAuthSuccess = {}
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

        // Verify Service Selection is the immediate landing page with the Service Grid & placeholders
        composeTestRule.onNodeWithTag("service_selection_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_selection_grid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_card_svc_classic_cut").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_card_svc_skin_fade").assertIsDisplayed()

        // Navigate to Door Setup via Compose Navigation when tapping the top GPS/Door capsule
        composeTestRule.onNodeWithTag("home_address_selector").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("door_setup_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detected_coordinates_card").assertIsDisplayed()
    }
}
