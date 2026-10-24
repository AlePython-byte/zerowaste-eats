package com.zerowasteeats.app.presentation.screens.merchant.business

import com.zerowasteeats.app.data.merchant.MerchantProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantBusinessStateTest {

    private val complete = MerchantProfile(
        businessName = "Panadería Demo",
        description = "Pan y repostería artesanal.",
        address = "Cra. 27 #18-45",
        city = "Pasto",
        isVerified = true
    )

    @Test
    fun aCompleteProfile_showsEveryDetail_andIsVerified() {
        val state = MerchantBusinessState.from(complete)

        assertEquals("Panadería Demo", state.businessName)
        assertEquals("Pan y repostería artesanal.", state.description)
        assertEquals("Cra. 27 #18-45", state.address)
        assertEquals("Pasto", state.city)
        assertTrue(state.isVerified)
    }

    @Test
    fun aDetailLeftEmpty_isNull_soTheScreenSaysItIsMissing() {
        val state = MerchantBusinessState.from(complete.copy(description = "", address = "", city = ""))

        assertNull(state.description)
        assertNull(state.address)
        assertNull(state.city)
    }

    @Test
    fun aDetailOfOnlySpaces_countsAsMissing_andOthersAreTrimmed() {
        val state = MerchantBusinessState.from(complete.copy(description = "   ", city = "  Pasto "))

        assertNull(state.description)
        assertEquals("Pasto", state.city)
    }

    @Test
    fun aNewBusiness_startsInReview_withOnlyTheCityFilledIn() {
        val state = MerchantBusinessState.from(
            MerchantProfile("Panadería Sol", description = "", address = "", city = "Medellín", isVerified = false)
        )

        assertFalse(state.isVerified)
        assertEquals("Medellín", state.city)
        assertNull(state.description)
        assertNull(state.address)
    }
}
