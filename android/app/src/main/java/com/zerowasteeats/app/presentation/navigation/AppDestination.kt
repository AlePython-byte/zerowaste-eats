package com.zerowasteeats.app.presentation.navigation

import androidx.annotation.StringRes
import com.zerowasteeats.app.R

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
