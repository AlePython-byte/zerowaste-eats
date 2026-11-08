package com.zerowasteeats.app.data.merchant

import com.zerowasteeats.app.data.remote.createHttpClient
import com.zerowasteeats.app.data.remote.createRetrofit
import com.zerowasteeats.app.presentation.screens.merchant.business.MerchantBusinessState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteMerchantProfileRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: RemoteMerchantProfileRepository

    private val fromSession = MerchantProfile(
        businessName = "Tienda Ana",
        description = "",
        address = "",
        city = "Pasto",
        isVerified = false
    )

    @Before
    fun setUp() {
        server.start()
        val api = createRetrofit(server.url("/api/v1/").toString(), createHttpClient())
            .create(MerchantProfileApi::class.java)
        repository = RemoteMerchantProfileRepository(api, fromSession)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(status: Int, body: String = "") = server.enqueue(
        MockResponse.Builder()
            .code(status)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
    )

    private val profileJson = """
        {"id":"p1","businessName":"Panadería La Esquina","description":"Pan artesanal",
         "isVerified":true,
         "establishment":{"id":"e1","name":"La Esquina","address":"Calle 18 # 25-30","city":"Pasto",
                          "latitude":1.2141,"longitude":-77.2783}}
    """.trimIndent()

    @Test
    fun showsWhatTheSessionKnows_untilTheServerAnswers() {
        assertEquals(fromSession, repository.profile.value)
        assertEquals(LoadState.Idle, repository.loadState.value)
    }

    @Test
    fun loadsTheBusiness_withItsEstablishmentAndLocation() = runBlocking {
        respond(200, profileJson)

        repository.refresh()

        assertEquals(
            MerchantProfile(
                businessName = "Panadería La Esquina",
                description = "Pan artesanal",
                address = "Calle 18 # 25-30",
                city = "Pasto",
                isVerified = true,
                latitude = 1.2141,
                longitude = -77.2783
            ),
            repository.profile.value
        )
        assertEquals(LoadState.Loaded, repository.loadState.value)
        assertEquals("/api/v1/merchant/profile", server.takeRequest().url.encodedPath)
    }

    @Test
    fun aBusinessWithoutEstablishment_hasNoAddressOrLocation() = runBlocking {
        respond(200, """{"businessName":"Tienda Ana","description":null,"isVerified":false,"establishment":null}""")

        repository.refresh()

        with(repository.profile.value) {
            assertEquals("", address)
            assertEquals("", description)
            assertNull(latitude)
        }
    }

    @Test
    fun aFailedLoad_keepsTheProfileShown_andSaysWhy() = runBlocking {
        respond(503)
        repository.refresh()
        assertEquals(LoadState.Failed(MerchantDataError.NETWORK), repository.loadState.value)
        assertEquals(fromSession, repository.profile.value)

        respond(401)
        repository.refresh()
        assertEquals(LoadState.Failed(MerchantDataError.SESSION), repository.loadState.value)

        respond(404, """{"statusCode":404}""")
        repository.refresh()
        assertEquals(LoadState.Failed(MerchantDataError.UNEXPECTED), repository.loadState.value)
    }

    @Test
    fun loadIfNeeded_loadsOnce_andRetriesAfterAFailure() = runBlocking {
        respond(500)
        repository.loadIfNeeded()
        respond(200, profileJson)
        repository.loadIfNeeded()
        repository.loadIfNeeded()

        assertEquals(2, server.requestCount)
        assertEquals(LoadState.Loaded, repository.loadState.value)
    }

    @Test
    fun update_sendsOnlyWhatChanges_andKeepsTheAnswer() = runBlocking {
        respond(200, profileJson)

        val updated = repository.update(
            MerchantProfileChanges(address = " Calle 18 # 25-30 ", latitude = 1.2141, longitude = -77.2783)
        )

        val body = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertFalse("businessName" in body)
        assertFalse("description" in body)
        val place = body["establishment"]!!.jsonObject
        assertEquals("Calle 18 # 25-30", place["address"]!!.jsonPrimitive.content)
        assertEquals(1.2141, place["latitude"]!!.jsonPrimitive.double, 0.0)
        assertFalse("city" in place)
        assertEquals(updated, repository.profile.value)
    }

    @Test
    fun update_withoutPlaceChanges_sendsNoEstablishment() = runBlocking {
        respond(200, profileJson)

        repository.update(MerchantProfileChanges(businessName = "Nuevo nombre"))

        val body = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertEquals(setOf("businessName"), body.keys)
    }

    @Test
    fun aFailedUpdate_throwsWithItsReason_andChangesNothing() {
        server.close()

        val failure = assertThrows(MerchantDataException::class.java) {
            runBlocking { repository.update(MerchantProfileChanges(city = "Ipiales")) }
        }

        assertEquals(MerchantDataError.NETWORK, failure.error)
        assertEquals(fromSession, repository.profile.value)
    }

    @Test
    fun theBusinessTab_showsALoadErrorOnlyWhenTheLoadFailed() {
        assertNull(MerchantBusinessState.from(fromSession, LoadState.Loading).loadError)
        assertNull(MerchantBusinessState.from(fromSession, LoadState.Loaded).loadError)
        assertEquals(
            MerchantDataError.NETWORK,
            MerchantBusinessState.from(fromSession, LoadState.Failed(MerchantDataError.NETWORK)).loadError
        )
        assertTrue(MerchantBusinessState.from(fromSession).loadError == null)
    }
}
