package com.zerowasteeats.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.zerowasteeats.app.presentation.data.MockOffers
import com.zerowasteeats.app.presentation.screens.explore.ExploreScreen
import com.zerowasteeats.app.presentation.screens.home.HomeScreen
import com.zerowasteeats.app.presentation.screens.offerdetail.OfferDetailScreen
import com.zerowasteeats.app.presentation.screens.orders.OrdersScreen
import com.zerowasteeats.app.presentation.screens.profile.ProfileScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.HOME.route,
        modifier = modifier
    ) {
        composable(AppDestination.HOME.route) {
            HomeScreen(
                onOfferClick = { offerId ->
                    navController.navigate("offer/$offerId")
                }
            )
        }
        composable(AppDestination.EXPLORE.route) {
            ExploreScreen()
        }
        composable(AppDestination.ORDERS.route) {
            OrdersScreen()
        }
        composable(AppDestination.PROFILE.route) {
            ProfileScreen()
        }
        composable("offer/{offerId}") { backStackEntry ->
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
                    text = "Oferta no encontrada",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}