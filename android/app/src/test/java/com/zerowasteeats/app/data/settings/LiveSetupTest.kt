package com.zerowasteeats.app.data.settings

import com.zerowasteeats.app.data.auth.supabase.SupabaseAuthClient
import com.zerowasteeats.app.data.remote.createRetrofit
import com.zerowasteeats.app.data.session.InMemoryKeyValueStore
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSetupTest {

    private val client = OkHttpClient()

    @Test
    fun withoutEndpoints_nothingIsBuilt() {
        var built = false

        val setup = buildLiveSetup(configured = false) { built = true }

        assertEquals(LiveSetup.NotConfigured, setup)
        assertFalse(built)
        assertNull(setup.clientsOrNull)
    }

    @Test
    fun usableEndpoints_giveTheClients() {
        val setup = buildLiveSetup(configured = true) {
            SupabaseAuthClient.create("https://demo.supabase.co", "publishable", client)
        }

        assertTrue(setup is LiveSetup.Ready)
        assertTrue(setup.clientsOrNull != null)
    }

    @Test
    fun aQuotedSupabaseUrl_isInvalid_insteadOfCrashing() {
        val setup = buildLiveSetup(configured = true) {
            SupabaseAuthClient.create("\"https://demo.supabase.co\"", "publishable", client)
        }

        assertEquals(LiveSetup.Invalid, setup)
        assertNull(setup.clientsOrNull)
    }

    @Test
    fun aBackendUrlRetrofitRefuses_isInvalid_insteadOfCrashing() {
        assertEquals(
            LiveSetup.Invalid,
            buildLiveSetup(configured = true) { createRetrofit("http://10.0.2.2:3000/api/v1", client) }
        )
        assertEquals(
            LiveSetup.Invalid,
            buildLiveSetup(configured = true) { createRetrofit("not a url/", client) }
        )
    }

    @Test
    fun otherFailures_stillSurface() {
        val bug = IllegalStateException("a bug, not a configuration problem")

        val thrown = assertThrows(IllegalStateException::class.java) {
            buildLiveSetup(configured = true) { throw bug }
        }
        assertSame(bug, thrown)
    }

    @Test
    fun anInvalidSetup_startsTheAppInDemoMode_evenIfLiveWasChosenBefore() {
        val prefs = InMemoryKeyValueStore()
        DataModeStore(prefs).save(DataMode.LIVE)
        val setup = buildLiveSetup(configured = true) {
            SupabaseAuthClient.create("\"https://demo.supabase.co\"", "publishable", client)
        }

        val controller = DataModeController(
            store = DataModeStore(prefs),
            liveAvailable = setup is LiveSetup.Ready,
            isSignedIn = { false }
        )

        assertEquals(DataMode.DEMO, controller.mode.value)
        assertFalse(controller.canChange)
    }
}
