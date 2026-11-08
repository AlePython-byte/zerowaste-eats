package com.zerowasteeats.app.data.auth

import com.zerowasteeats.app.data.session.InMemoryKeyValueStore
import com.zerowasteeats.app.data.session.PrefsSessionStore
import com.zerowasteeats.app.data.session.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsTokenStoreTest {

    private val data = InMemoryKeyValueStore()
    private val store = PrefsTokenStore(data)
    private val tokens = AuthTokens("access-secret", "refresh-secret", 1_900_000_000)

    @Test
    fun load_isNullWhenNothingWasSaved() {
        assertNull(store.load())
    }

    @Test
    fun save_thenLoad_returnsTheSameTokens_alsoForANewStore() {
        store.save(tokens)

        assertEquals(tokens, store.load())
        assertEquals(tokens, PrefsTokenStore(data).load())
    }

    @Test
    fun clear_forgetsEverything() {
        store.save(tokens)

        store.clear()

        assertNull(store.load())
        assertTrue(data.contents.isEmpty())
    }

    @Test
    fun halfWrittenTokens_areDiscarded() {
        val broken = InMemoryKeyValueStore(mapOf("access_token" to "only-this"))

        assertNull(PrefsTokenStore(broken).load())
        assertTrue(broken.contents.isEmpty())
    }

    @Test
    fun toString_neverShowsTheTokens() {
        val text = tokens.toString()

        assertFalse(text.contains("access-secret"))
        assertFalse(text.contains("refresh-secret"))
    }

    @Test
    fun theSessionStore_stillKeepsNoToken() {
        val sessionData = InMemoryKeyValueStore()
        PrefsSessionStore(sessionData).save(
            Session.Merchant("u1", "Luis", "l@t.co", "Tienda Luis", "Pasto")
        )

        val keys = sessionData.contents.keys
        assertTrue(keys.none { it.contains("token", ignoreCase = true) })
        assertTrue(sessionData.contents.values.none { it.contains("secret") })
        assertTrue(PrefsSessionStore.FILE_NAME != PrefsTokenStore.FILE_NAME)
    }
}
