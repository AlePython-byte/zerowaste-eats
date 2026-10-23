package com.zerowasteeats.app.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsSessionStoreTest {

    private val customer = Session.Customer("u1", "Ana Pérez", "ana@correo.co")
    private val merchant = Session.Merchant("u2", "Luis", "luis@tienda.co", "Tienda Luis")

    @Test
    fun load_emptyStoreIsSignedOut() {
        assertEquals(Session.SignedOut, PrefsSessionStore(InMemoryKeyValueStore()).load())
    }

    @Test
    fun customer_survivesANewStoreOverTheSameData() {
        val data = InMemoryKeyValueStore()
        PrefsSessionStore(data).save(customer)

        assertEquals(customer, PrefsSessionStore(data).load())
    }

    @Test
    fun merchant_survivesANewStoreOverTheSameData() {
        val data = InMemoryKeyValueStore()
        PrefsSessionStore(data).save(merchant)

        assertEquals(merchant, PrefsSessionStore(data).load())
    }

    @Test
    fun save_persistsIdentityOnly() {
        val data = InMemoryKeyValueStore()

        PrefsSessionStore(data).save(merchant)

        assertEquals(
            setOf("user_id", "role", "display_name", "email", "business_name"),
            data.contents.keys
        )
        assertFalse(data.contents.keys.any { it.contains("password", ignoreCase = true) || it.contains("token", ignoreCase = true) })
    }

    @Test
    fun save_customerLeavesNoBusinessNameBehind() {
        val data = InMemoryKeyValueStore()
        val store = PrefsSessionStore(data)
        store.save(merchant)

        store.save(customer)

        assertFalse(data.contents.containsKey("business_name"))
        assertEquals(customer, store.load())
    }

    @Test
    fun save_signedOutAndLoadingEraseTheSession() {
        val data = InMemoryKeyValueStore()
        val store = PrefsSessionStore(data)

        store.save(customer)
        store.save(Session.SignedOut)
        assertTrue(data.contents.isEmpty())

        store.save(merchant)
        store.save(Session.Loading)
        assertTrue(data.contents.isEmpty())
    }

    @Test
    fun clear_signsOut() {
        val data = InMemoryKeyValueStore()
        val store = PrefsSessionStore(data)
        store.save(customer)

        store.clear()

        assertEquals(Session.SignedOut, store.load())
        assertTrue(data.contents.isEmpty())
    }

    @Test
    fun load_unknownRoleIsDiscarded() {
        val data = InMemoryKeyValueStore(
            mapOf("user_id" to "u1", "role" to "ADMIN", "display_name" to "Ana", "email" to "a@b.co")
        )

        assertEquals(Session.SignedOut, PrefsSessionStore(data).load())
        assertTrue(data.contents.isEmpty())
    }

    @Test
    fun load_missingFieldIsDiscarded() {
        val data = InMemoryKeyValueStore(
            mapOf("user_id" to "u1", "role" to "CUSTOMER", "display_name" to "Ana")
        )

        assertEquals(Session.SignedOut, PrefsSessionStore(data).load())
        assertTrue(data.contents.isEmpty())
    }

    @Test
    fun load_merchantWithoutBusinessNameIsDiscarded() {
        val data = InMemoryKeyValueStore(
            mapOf("user_id" to "u2", "role" to "MERCHANT", "display_name" to "Luis", "email" to "l@t.co")
        )

        assertEquals(Session.SignedOut, PrefsSessionStore(data).load())
        assertTrue(data.contents.isEmpty())
    }

    @Test
    fun load_blankUserIdIsDiscarded() {
        val data = InMemoryKeyValueStore(
            mapOf("user_id" to "", "role" to "CUSTOMER", "display_name" to "Ana", "email" to "a@b.co")
        )

        assertEquals(Session.SignedOut, PrefsSessionStore(data).load())
    }
}
