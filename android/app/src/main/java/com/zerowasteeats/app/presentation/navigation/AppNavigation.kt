package com.zerowasteeats.app.presentation.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.data.MockOffers
import com.zerowasteeats.app.presentation.screens.explore.ExploreScreen
import com.zerowasteeats.app.presentation.screens.explore.ExploreViewMode
import com.zerowasteeats.app.presentation.screens.explore.ExploreViewRequest
import com.zerowasteeats.app.presentation.screens.home.HomeScreen
import com.zerowasteeats.app.presentation.screens.map.ExploreMapScreen
import com.zerowasteeats.app.presentation.screens.offerdetail.OfferDetailScreen
import com.zerowasteeats.app.presentation.screens.orders.OrdersScreen
import com.zerowasteeats.app.presentation.screens.profile.ProfileScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    exploreViewRequest: ExploreViewRequest,
    modifier: Modifier = Modifier
) {
    // Lets the offer photo travel between the card and the detail screen
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.HOME.route,
                modifier = Modifier.fillMaxSize()
            ) {
                sharedComposable(AppDestination.HOME.route) {
                    HomeScreen(
                        onOfferClick = { offerId ->
                            navController.navigate("offer/$offerId")
                        },
                        onMapClick = {
                            exploreViewRequest.request(ExploreViewMode.MAP)
                            navController.navigateToTab(AppDestination.EXPLORE)
                        }
                    )
                }
                sharedComposable(AppDestination.EXPLORE.route) {
                    ExploreScreen(
                        viewRequest = exploreViewRequest,
                        onOfferClick = { offerId ->
                            navController.navigate("offer/$offerId")
                        },
                        onFavoriteClick = { /* Handle favorite click temporarily */ }
                    )
                }
                sharedComposable("explore/map") {
                    ExploreMapScreen(
                        onBackClick = { navController.popBackStack() },
                        onOfferClick = { offerId ->
                            navController.navigate("offer/$offerId")
                        }
                    )
                }
                composable(AppDestination.ORDERS.route) {
                    OrdersScreen()
                }
                composable(AppDestination.PROFILE.route) {
                    ProfileScreen()
                }
                sharedComposable("offer/{offerId}") { backStackEntry ->
                    val offerId = backStackEntry.arguments?.getString("offerId")
                    val mockOffers = MockOffers.getList()
                    val offer = mockOffers.find { it.id == offerId }
            
                    if (offer != null) {
                        OfferDetailScreen(
                            offer = offer,
                            onBackClick = { navController.popBackStack() },
                            onFavoriteClick = { /* Handle favorite click temporarily */ },
                            onReserveClick = { /* Handle reserve click temporarily */ }
                        )
                    } else {
                        // Safe fallback state for missing offer
                        Text(
                            text = stringResource(R.string.navigation_offer_not_found),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}