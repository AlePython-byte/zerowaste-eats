package com.zerowasteeats.app.presentation.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.navigation.TabItem
import com.zerowasteeats.app.presentation.screens.merchant.MerchantTabPlaceholder
import com.zerowasteeats.app.presentation.screens.merchant.dashboard.MerchantDashboardScreen
import com.zerowasteeats.app.presentation.screens.merchant.offers.MerchantOfferEditPlaceholder
import com.zerowasteeats.app.presentation.screens.merchant.offers.MerchantOffersScreen

enum class MerchantDestination(
    val route: String,
    @param:StringRes val labelRes: Int
) {
    DASHBOARD(route = "merchant_dashboard", labelRes = R.string.merchant_tab_dashboard),
    OFFERS(route = "merchant_offers", labelRes = R.string.merchant_tab_offers),
    RESERVATIONS(route = "merchant_reservations", labelRes = R.string.merchant_tab_reservations),
    BUSINESS(route = "merchant_business", labelRes = R.string.merchant_tab_business);

    companion object {
        const val EDIT_OFFER_ID_ARG = "id"

        /** The offer editor. The id is optional: without it the editor starts a new offer. */
        const val EDIT_OFFER_ROUTE = "merchant_offer_edit?$EDIT_OFFER_ID_ARG={$EDIT_OFFER_ID_ARG}"

        fun editOfferRoute(offerId: String?): String =
            if (offerId == null) "merchant_offer_edit" else "merchant_offer_edit?$EDIT_OFFER_ID_ARG=${Uri.encode(offerId)}"
    }
}

/** The business's bottom bar tabs, in display order. */
val merchantTabs: List<TabItem> =
    MerchantDestination.entries.map { TabItem(it.route, it.labelRes) }

/**
 * The business space: its own nav controller, bottom bar and graph. Like [CustomerNavigation]
 * everything is remembered here, so dropping it (for example on sign out) discards the whole
 * back stack. The bar's accent is the secondary color, so the two spaces look apart at a glance.
 */
@Composable
fun MerchantNavigation(
    session: Session.Merchant,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    TabScaffold(
        tabs = merchantTabs,
        currentRoute = currentRoute,
        onTabClick = { tab -> navController.navigateToTab(tab.route) },
        accent = MaterialTheme.colorScheme.secondary
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MerchantDestination.DASHBOARD.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(MerchantDestination.DASHBOARD.route) {
                MerchantDashboardScreen(
                    session = session,
                    // The offer editor comes later; for now this leads to the offers tab
                    onPublishOffer = { navController.navigateToTab(MerchantDestination.OFFERS.route) }
                )
            }
            composable(MerchantDestination.OFFERS.route) {
                MerchantOffersScreen(
                    session = session,
                    onNewOffer = { navController.navigate(MerchantDestination.editOfferRoute(null)) },
                    onEditOffer = { id -> navController.navigate(MerchantDestination.editOfferRoute(id)) }
                )
            }
            // Not a tab, so the bottom bar is hidden while the editor is open
            composable(
                route = MerchantDestination.EDIT_OFFER_ROUTE,
                arguments = listOf(
                    navArgument(MerchantDestination.EDIT_OFFER_ID_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                MerchantOfferEditPlaceholder(onBack = { navController.popBackStack() })
            }
            composable(MerchantDestination.RESERVATIONS.route) {
                MerchantTabPlaceholder(
                    title = stringResource(R.string.merchant_tab_reservations),
                    emptyTitle = stringResource(R.string.merchant_reservations_empty_title),
                    emptyMessage = stringResource(R.string.merchant_reservations_empty_message)
                )
            }
            composable(MerchantDestination.BUSINESS.route) {
                MerchantTabPlaceholder(
                    title = session.businessName,
                    emptyTitle = stringResource(R.string.merchant_business_empty_title),
                    emptyMessage = stringResource(R.string.merchant_business_empty_message),
                    // TEMPORARY: replaced by the real business profile
                    signOut = {
                        AppButton(
                            text = stringResource(R.string.common_sign_out),
                            onClick = onSignOut,
                            style = AppButtonStyle.SECONDARY
                        )
                    }
                )
            }
        }
    }
}
