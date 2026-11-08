package com.zerowasteeats.app.presentation.navigation

import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.data.settings.DataMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoBadgeTest {

    private val customer = Session.Customer(
        userId = "c",
        displayName = "Comprador Demo",
        email = "comprador@demo.co"
    )
    private val merchant = Session.Merchant(
        userId = "m",
        displayName = "Negocio Demo",
        email = "negocio@demo.co",
        businessName = "Panadería Demo"
    )

    @Test
    fun signedInOnDemoData_showsTheBadge() {
        assertTrue(showsDemoBadge(customer, DataMode.DEMO))
        assertTrue(showsDemoBadge(merchant, DataMode.DEMO))
    }

    @Test
    fun liveData_neverShowsIt() {
        assertFalse(showsDemoBadge(customer, DataMode.LIVE))
        assertFalse(showsDemoBadge(merchant, DataMode.LIVE))
    }

    @Test
    fun signedOut_orStarting_doesNotShowIt() {
        assertFalse(showsDemoBadge(Session.SignedOut, DataMode.DEMO))
        assertFalse(showsDemoBadge(Session.Loading, DataMode.DEMO))
    }
}
