package com.zerowasteeats.app.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zerowasteeats.app.presentation.components.navigation.GlassBottomBar

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ZeroWasteEatsApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute in listOf(
        AppDestination.HOME.route,
        AppDestination.EXPLORE.route,
        AppDestination.ORDERS.route,
        AppDestination.PROFILE.route
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Handle edge-to-edge manually if needed
        containerColor = Color.Transparent, // Let screens draw their own background
        bottomBar = {
            if (isBottomBarVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Opaque backdrop so no scrolled content shows around the bar
                        .background(MaterialTheme.colorScheme.background)
                        .navigationBarsPadding(), // Keep above system nav bar
                    contentAlignment = Alignment.BottomCenter
                ) {
                    GlassBottomBar(
                        currentRoute = currentRoute,
                        onNavigateTo = { destination ->
                            navController.navigate(destination.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // innerPadding reserves the bottom bar's height, so content ends above it
        // instead of flowing underneath. Screens only add a small trailing gap.
        AppNavigation(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    }
}
