package com.zerowasteeats.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.zerowasteeats.app.presentation.screens.explore.ExploreScreen
import com.zerowasteeats.app.presentation.screens.home.HomeScreen
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
            HomeScreen()
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
    }
}
