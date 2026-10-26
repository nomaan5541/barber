package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.R

/**
 * Canonical Jetpack Compose Navigation route IDs for DoorCut.
 * Customer Mode uses strictly 4 bottom tabs: Home, Bookings, Favourites, Profile.
 * Barber Mode uses strictly 4 bottom tabs: Home, Schedule, Earnings, Profile.
 */
object AppRoutes {
    // Customer 4 Bottom Tabs
    const val ROUTE_CUSTOMER_HOME = "customer_home"
    const val ROUTE_SERVICE_SELECTION = ROUTE_CUSTOMER_HOME // Alias kept for startDestination clarity
    const val ROUTE_BOOKINGS = "bookings"
    const val ROUTE_FAVOURITES = "favourites"
    const val ROUTE_PROFILE = "profile"

    // Customer Guided Flow & Detail Sub-Routes (No extra bottom nav items)
    const val ROUTE_BOOKING_FLOW = "booking_flow"
    const val ROUTE_BOOKING_STATUS = "booking_status/{bookingId}"
    const val ROUTE_DOOR_SETUP = "door_setup"
    const val ROUTE_ADDRESSES = "addresses"
    const val ROUTE_AUTH = "auth"

    // Barber 4 Bottom Tabs
    const val ROUTE_BARBER_HOME = "barber_home"
    const val ROUTE_BARBER_JOBS = ROUTE_BARBER_HOME
    const val ROUTE_BARBER_SCHEDULE = "barber_schedule"
    const val ROUTE_BARBER_EARNINGS = "barber_earnings"
    const val ROUTE_BARBER_PROFILE = "barber_profile"

    // Barber Detail Sub-Route
    const val ROUTE_BARBER_JOB_DETAIL = "barber_job/{bookingId}"

    // Admin Dashboard Routes
    const val ROUTE_ADMIN_OVERVIEW = "admin_overview"
    const val ROUTE_ADMIN_BARBERS = "admin_barbers"
    const val ROUTE_ADMIN_CATALOG = "admin_catalog"
    const val ROUTE_ADMIN_BOOKINGS = "admin_bookings"
    const val ROUTE_ADMIN_POLICIES = "admin_policies"

    fun bookingStatusRoute(bookingId: String): String = "booking_status/$bookingId"
    fun barberJobDetailRoute(bookingId: String): String = "barber_job/$bookingId"
}

data class NavTabSpec(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * Strictly 4 Customer Bottom Navigation items: Home, Bookings, Favourites, Profile.
 */
val customerNavTabs: List<NavTabSpec> = listOf(
    NavTabSpec(
        route = AppRoutes.ROUTE_CUSTOMER_HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_customer_home"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_BOOKINGS,
        labelRes = R.string.nav_bookings,
        selectedIcon = Icons.AutoMirrored.Filled.EventNote,
        unselectedIcon = Icons.AutoMirrored.Outlined.EventNote,
        testTag = "nav_customer_bookings"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_FAVOURITES,
        labelRes = R.string.nav_favourites,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        testTag = "nav_customer_favourites"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_PROFILE,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_customer_profile"
    )
)

/**
 * Strictly 4 Barber Bottom Navigation items: Home, Schedule, Earnings, Profile.
 */
val barberNavTabs: List<NavTabSpec> = listOf(
    NavTabSpec(
        route = AppRoutes.ROUTE_BARBER_HOME,
        labelRes = R.string.nav_barber_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_barber_home"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_BARBER_SCHEDULE,
        labelRes = R.string.nav_barber_schedule,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_barber_schedule"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_BARBER_EARNINGS,
        labelRes = R.string.nav_barber_earnings,
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_barber_earnings"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_BARBER_PROFILE,
        labelRes = R.string.nav_barber_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_barber_profile"
    )
)

val adminNavTabs: List<NavTabSpec> = listOf(
    NavTabSpec(
        route = AppRoutes.ROUTE_ADMIN_OVERVIEW,
        labelRes = R.string.nav_admin_overview,
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        testTag = "nav_admin_overview"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_ADMIN_BARBERS,
        labelRes = R.string.nav_admin_barbers,
        selectedIcon = Icons.Filled.Verified,
        unselectedIcon = Icons.Outlined.Verified,
        testTag = "nav_admin_barbers"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_ADMIN_CATALOG,
        labelRes = R.string.nav_admin_catalog,
        selectedIcon = Icons.Filled.ContentCut,
        unselectedIcon = Icons.Outlined.ContentCut,
        testTag = "nav_admin_catalog"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_ADMIN_BOOKINGS,
        labelRes = R.string.nav_admin_bookings,
        selectedIcon = Icons.Filled.Gavel,
        unselectedIcon = Icons.Outlined.Gavel,
        testTag = "nav_admin_bookings"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_ADMIN_POLICIES,
        labelRes = R.string.nav_admin_policies,
        selectedIcon = Icons.Filled.Policy,
        unselectedIcon = Icons.Outlined.Policy,
        testTag = "nav_admin_policies"
    )
)

fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
