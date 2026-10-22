package com.zerowasteeats.app.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Switches to a top-level tab the same way the bottom bar does: the back stack is popped
 * to the start destination, so Back from any tab returns to Home, and each tab keeps and
 * restores its own state (scroll, chosen view, etc.).
 */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        // Reselecting the current tab does not stack a second copy of it
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToTab(destination: AppDestination) = navigateToTab(destination.route)
