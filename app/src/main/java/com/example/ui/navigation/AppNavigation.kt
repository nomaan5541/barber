package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.R

/**
 * Canonical Jetpack Compose Navigation route IDs for DoorCut.
 * [ROUTE_SERVICE_SELECTION] is the unconditional startDestination (landing page).
 */
object AppRoutes {
    const val ROUTE_SERVICE_SELECTION = "service_selection"
    const val ROUTE_DOOR_SETUP = "door_setup"
    const val ROUTE_BOOKINGS = "bookings"
    const val ROUTE_CART = "cart"
    const val ROUTE_ADDRESSES = "addresses"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_AUTH = "auth"

    // Barber Studio Portal Routes
    const val ROUTE_BARBER_JOBS = "barber_jobs"
    const val ROUTE_BARBER_SCHEDULE = "barber_schedule"
    const val ROUTE_BARBER_EARNINGS = "barber_earnings"

    // Admin Operations Portal Routes
    const val ROUTE_ADMIN_OVERVIEW = "admin_overview"
    const val ROUTE_ADMIN_BARBERS = "admin_barbers"
    const val ROUTE_ADMIN_CATALOG = "admin_catalog"
    const val ROUTE_ADMIN_BOOKINGS = "admin_bookings"
    const val ROUTE_ADMIN_POLICIES = "admin_policies"
}

data class NavTabSpec(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val customerNavTabs: List<NavTabSpec> = listOf(
    NavTabSpec(
        route = AppRoutes.ROUTE_SERVICE_SELECTION,
        labelRes = R.string.nav_discover,
        selectedIcon = Icons.Filled.ContentCut,
        unselectedIcon = Icons.Outlined.ContentCut,
        testTag = "nav_customer_discover"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_BOOKINGS,
        labelRes = R.string.nav_bookings,
        selectedIcon = Icons.AutoMirrored.Filled.EventNote,
        unselectedIcon = Icons.AutoMirrored.Outlined.EventNote,
        testTag = "nav_customer_bookings"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_CART,
        labelRes = R.string.nav_cart,
        selectedIcon = Icons.Filled.ShoppingBag,
        unselectedIcon = Icons.Outlined.ShoppingBag,
        testTag = "nav_customer_cart"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_ADDRESSES,
        labelRes = R.string.nav_addresses,
        selectedIcon = Icons.Filled.LocationOn,
        unselectedIcon = Icons.Outlined.LocationOn,
        testTag = "nav_customer_addresses"
    ),
    NavTabSpec(
        route = AppRoutes.ROUTE_PROFILE,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_customer_profile"
    )
)

val barberNavTabs: List<NavTabSpec> = listOf(
    NavTabSpec(
        route = AppRoutes.ROUTE_BARBER_JOBS,
        labelRes = R.string.nav_barber_jobs,
        selectedIcon = Icons.Filled.Work,
        unselectedIcon = Icons.Outlined.WorkOutline,
        testTag = "nav_barber_jobs"
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
        route = AppRoutes.ROUTE_PROFILE,
        labelRes = R.string.nav_profile,
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
