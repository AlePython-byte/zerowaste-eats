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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zerowasteeats.app.presentation.components.navigation.AppBottomBar
import com.zerowasteeats.app.presentation.screens.explore.ExploreViewRequest

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ZeroWasteEatsApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Next to the nav controller: outlives Explore, but not the activity, so a consumed
    // request cannot come back after rotation or a language change
    val exploreViewRequest = remember { ExploreViewRequest() }

    val isBottomBarVisible = customerTabs.any { it.route == currentRoute }

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
                    AppBottomBar(
                        tabs = customerTabs,
                        selectedRoute = currentRoute,
                        onTabClick = { tab -> navController.navigateToTab(tab.route) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // innerPadding reserves the bottom bar's height, so content ends above it
        // instead of flowing underneath. Screens only add a small trailing gap.
        AppNavigation(
            navController = navController,
            exploreViewRequest = exploreViewRequest,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    }
}
