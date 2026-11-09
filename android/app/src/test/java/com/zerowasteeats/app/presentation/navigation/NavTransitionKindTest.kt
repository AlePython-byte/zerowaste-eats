package com.zerowasteeats.app.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavTransitionKindTest {

    private val tabs = customerTabs.map { it.route }.toSet()
    private val detail = "offer/{offerId}"

    private fun kind(from: String?, to: String?) =
        navTransitionKind(from, to, tabRoutes = tabs, fadeRoutes = setOf(detail))

    @Test
    fun betweenTabs_screensFade() {
        assertEquals(NavTransitionKind.Fade, kind("home", "explore"))
        assertEquals(NavTransitionKind.Fade, kind("orders", "home"))
    }

    @Test
    fun toAndFromTheSharedElementScreen_screensFade() {
        assertEquals(NavTransitionKind.Fade, kind("home", detail))
        assertEquals(NavTransitionKind.Fade, kind(detail, "explore"))
    }

    @Test
    fun intoADeeperScreen_screensSlide() {
        val merchantTabs = merchantTabs.map { it.route }.toSet()

        assertEquals(
            NavTransitionKind.Axis,
            navTransitionKind(
                from = MerchantDestination.OFFERS.route,
                to = MerchantDestination.EDIT_OFFER_ROUTE,
                tabRoutes = merchantTabs,
                fadeRoutes = emptySet()
            )
        )
    }

    @Test
    fun withAnUnknownRoute_screensSlide() {
        assertEquals(NavTransitionKind.Axis, kind(null, "home"))
    }
}
