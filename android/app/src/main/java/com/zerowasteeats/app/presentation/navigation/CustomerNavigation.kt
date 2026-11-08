package com.zerowasteeats.app.presentation.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.screens.explore.ExploreScreen
import com.zerowasteeats.app.presentation.screens.explore.ExploreViewMode
import com.zerowasteeats.app.presentation.screens.explore.ExploreViewRequest
import com.zerowasteeats.app.presentation.screens.home.HomeScreen
import com.zerowasteeats.app.presentation.screens.offerdetail.OfferDetailScreen
import com.zerowasteeats.app.presentation.screens.orders.OrdersScreen
import com.zerowasteeats.app.presentation.screens.profile.ProfileScreen

/**
 * The customer space: its own nav controller, bottom bar and graph. Everything here is
 * remembered inside this composable, so dropping it (for example on sign out) discards
 * the whole customer back stack.
 */
@Composable
fun CustomerNavigation(
    session: Session.Customer,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Next to the nav controller: outlives Explore, but not the activity, so a consumed
    // request cannot come back after rotation or a language change
    val exploreViewRequest = remember { ExploreViewRequest() }

    TabScaffold(
        tabs = customerTabs,
        currentRoute = currentRoute,
        onTabClick = { tab -> navController.navigateToTab(tab.route) },
        accent = MaterialTheme.colorScheme.tertiary
    ) {
        // The content fills the area and runs under the bar; scrolling screens add
        // LocalBottomBarInset to their bottom padding so the last item can clear it.
        CustomerNavHost(
            navController = navController,
            exploreViewRequest = exploreViewRequest,
            session = session,
            onSignOut = onSignOut,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CustomerNavHost(
    navController: NavHostController,
    exploreViewRequest: ExploreViewRequest,
    session: Session.Customer,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Lets the offer photo travel between the card and the detail screen
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = CustomerDestination.HOME.route,
                modifier = Modifier.fillMaxSize()
            ) {
                sharedComposable(CustomerDestination.HOME.route) {
                    HomeScreen(
                        session = session,
                        onOfferClick = { offerId ->
                            navController.navigate("offer/$offerId")
                        },
                        onMapClick = {
                            exploreViewRequest.request(ExploreViewMode.MAP)
                            navController.navigateToTab(CustomerDestination.EXPLORE)
                        }
                    )
                }
                sharedComposable(CustomerDestination.EXPLORE.route) {
                    ExploreScreen(
                        session = session,
                        viewRequest = exploreViewRequest,
                        onOfferClick = { offerId ->
                            navController.navigate("offer/$offerId")
                        }
                    )
                }
                composable(CustomerDestination.ORDERS.route) {
                    OrdersScreen()
                }
                composable(CustomerDestination.PROFILE.route) {
                    ProfileScreen(
                        displayName = session.displayName,
                        email = session.email,
                        onSignOut = onSignOut
                    )
                }
                sharedComposable("offer/{offerId}") { backStackEntry ->
                    OfferDetailScreen(
                        session = session,
                        offerId = backStackEntry.arguments?.getString("offerId").orEmpty(),
                        onBackClick = { navController.popBackStack() },
                        // Reserving comes with the reservations
                        onReserveClick = {}
                    )
                }
            }
        }
    }
}
