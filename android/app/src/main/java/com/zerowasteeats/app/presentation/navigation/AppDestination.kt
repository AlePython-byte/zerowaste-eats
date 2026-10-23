package com.zerowasteeats.app.presentation.navigation

import androidx.annotation.StringRes
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.navigation.TabItem

enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int
) {
    HOME(
        route = "home",
        labelRes = R.string.nav_home
    ),
    EXPLORE(
        route = "explore",
        labelRes = R.string.nav_explore
    ),
    ORDERS(
        route = "orders",
        labelRes = R.string.nav_orders
    ),
    PROFILE(
        route = "profile",
        labelRes = R.string.nav_profile
    )
}

/** The customer's bottom bar tabs, in display order. */
val customerTabs: List<TabItem> = AppDestination.entries.map { TabItem(it.route, it.labelRes) }
