package com.zerowasteeats.app.presentation.navigation

import com.zerowasteeats.app.data.session.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AppSpaceTest {

    private val customer = Session.Customer("c-1", "Ana", "ana@correo.co")
    private val merchant = Session.Merchant("m-1", "Luis", "luis@correo.co", "Panadería Sol")

    @Test
    fun loading_showsOnlyTheBackground() {
        assertEquals(AppSpace.Loading, appSpaceFor(Session.Loading))
    }

    @Test
    fun signedOut_showsTheAuthSpace() {
        assertEquals(AppSpace.Auth, appSpaceFor(Session.SignedOut))
    }

    @Test
    fun customer_showsTheCustomerSpace_withTheirSession() {
        assertEquals(AppSpace.Customer(customer), appSpaceFor(customer))
    }

    @Test
    fun merchant_showsTheMerchantSpace_withTheirSession() {
        assertEquals(AppSpace.Merchant(merchant), appSpaceFor(merchant))
    }

    @Test
    fun anotherAccountOfTheSameRole_getsAnotherKey() {
        val other = Session.Customer("c-2", "Bea", "bea@correo.co")

        assertNotEquals(appSpaceFor(customer).key, appSpaceFor(other).key)
    }

    @Test
    fun sameUserIdInDifferentRoles_getsDifferentKeys() {
        val sameIdMerchant = merchant.copy(userId = customer.userId)

        assertNotEquals(appSpaceFor(customer).key, appSpaceFor(sameIdMerchant).key)
    }

    @Test
    fun theSameAccount_keepsItsKey_soRecompositionsDoNotRebuildTheSpace() {
        assertEquals(appSpaceFor(customer).key, appSpaceFor(customer.copy()).key)
    }

    @Test
    fun everySpaceKindHasItsOwnKey() {
        val keys = listOf(
            appSpaceFor(Session.Loading),
            appSpaceFor(Session.SignedOut),
            appSpaceFor(customer),
            appSpaceFor(merchant)
        ).map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }
}
