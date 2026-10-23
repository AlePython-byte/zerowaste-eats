package com.zerowasteeats.app.presentation.components.navigation

import androidx.annotation.StringRes

/**
 * One tab of [AppBottomBar]. Screen readers read [labelRes] unless
 * [contentDescriptionRes] gives a longer description.
 */
data class TabItem(
    val route: String,
    @StringRes val labelRes: Int,
    @StringRes val contentDescriptionRes: Int? = null
)
