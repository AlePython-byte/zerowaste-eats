package com.zerowasteeats.app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zerowasteeats.app.presentation.components.navigation.AppBottomBar
import com.zerowasteeats.app.presentation.components.navigation.TabItem

/**
 * The frame shared by the customer and merchant spaces: the floating [AppBottomBar] under
 * the content, shown only while [currentRoute] is one of the [tabs]. [content] gets the padding
 * that reserves the bar's height, so it ends above the bar instead of flowing underneath.
 */
@Composable
fun TabScaffold(
    tabs: List<TabItem>,
    currentRoute: String?,
    onTabClick: (TabItem) -> Unit,
    accent: Color,
    content: @Composable (PaddingValues) -> Unit
) {
    val isBottomBarVisible = tabs.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Screens handle edge-to-edge themselves
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
                        tabs = tabs,
                        selectedRoute = currentRoute,
                        onTabClick = onTabClick,
                        accent = accent
                    )
                }
            }
        },
        content = content
    )
}
