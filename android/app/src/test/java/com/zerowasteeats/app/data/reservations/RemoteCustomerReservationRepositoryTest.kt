package com.zerowasteeats.app.data.reservations

import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.remote.createHttpClient
import com.zerowasteeats.app.data.remote.createRetrofit
import java.time.Instant
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteCustomerReservationRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: RemoteCustomerReservationRepository

    @Before
    fun setUp() {
        server.start()
        val api = createRetrofit(server.url("/api/v1/").toString(), createHttpClient())
            .create(ReservationsApi::class.java)
        repository = RemoteCustomerReservationRepository(api)
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

    private fun error(status: Int, message: String) =
        respond(status, """{"statusCode":$status,"message":"$message","error":"x"}""")

    @Test
    fun reserving_sendsTheOfferAndQuantity() = runBlocking {
        respond(201, reservationJson("r1"))

        repository.reserve("offer-1", 3)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/reservations", request.url.encodedPath)
        assertEquals("""{"offerId":"offer-1","quantity":3}""", request.body?.utf8())
    }

    @Test
    fun aReservation_comesBackWithItsCode_andHeadsTheList() = runBlocking {
        respond(201, reservationJson("r1", code = "ZW-7KQ3"))

        val result = repository.reserve("offer-1", 2)

        val reserved = (result as ReserveResult.Reserved).reservation
        assertEquals("ZW-7KQ3", reserved.code)
        assertEquals(CustomerReservationStatus.PENDING, reserved.status)
        assertEquals(2, reserved.quantity)
        assertEquals(12_000L, reserved.totalPrice)
        assertEquals(6_000L, reserved.unitPrice)
        assertEquals(12_000L, reserved.originalUnitPrice)
        assertEquals("Pan r1", reserved.offerTitle)
        assertEquals("Panadería Sol", reserved.businessName)
        assertEquals(Instant.parse("2026-11-08T22:00:00Z"), reserved.pickupStart)
        assertEquals(listOf(reserved), repository.reservations.value)
    }

    @Test
    fun theNewestReservation_goesFirst() = runBlocking {
        respond(201, reservationJson("r1"))
        respond(201, reservationJson("r2"))

        repository.reserve("offer-1", 1)
        repository.reserve("offer-1", 1)

        assertEquals(listOf("r2", "r1"), repository.reservations.value.map { it.id })
    }

    @Test
    fun noUnitsLeft_isAConflict_andNothingIsAdded() = runBlocking {
        error(409, "Ya no quedan unidades de esta oferta.")

        val result = repository.reserve("offer-1", 2)

        assertEquals(ReserveResult.NotEnoughUnits, result)
        assertTrue(repository.reservations.value.isEmpty())
    }

    @Test
    fun anOfferThatIsGone_isNotAvailable() = runBlocking {
        error(404, "Esta oferta ya no está disponible.")

        assertEquals(ReserveResult.NotAvailable, repository.reserve("offer-1", 1))
    }

    @Test
    fun failures_sayWhy() = runBlocking {
        mapOf(
            401 to CatalogError.SESSION,
            500 to CatalogError.NETWORK,
            429 to CatalogError.NETWORK,
            400 to CatalogError.UNEXPECTED
        ).forEach { (status, expected) ->
            error(status, "x")
            assertEquals("HTTP $status", ReserveResult.Failed(expected), repository.reserve("offer-1", 1))
        }
        assertTrue(repository.reservations.value.isEmpty())
    }

    @Test
    fun noConnection_isANetworkError() = runBlocking {
        server.close()

        assertEquals(ReserveResult.Failed(CatalogError.NETWORK), repository.reserve("offer-1", 1))
    }

    @Test
    fun theStatuses_readAsTheCustomerSeesThem() {
        mapOf(
            "PENDING" to CustomerReservationStatus.PENDING,
            "CONFIRMED" to CustomerReservationStatus.PENDING,
            "READY_FOR_PICKUP" to CustomerReservationStatus.READY_FOR_PICKUP,
            "COMPLETED" to CustomerReservationStatus.COMPLETED,
            "CANCELLED" to CustomerReservationStatus.CANCELLED
        ).forEach { (server, expected) ->
            respond(201, reservationJson("r-$server", status = server))
            val result = runBlocking { repository.reserve("offer-1", 1) }
            assertEquals(server, expected, (result as ReserveResult.Reserved).reservation.status)
        }
    }

    companion object {
        fun reservationJson(
            id: String,
            code: String = "ZW-AB23",
            status: String = "PENDING",
            quantity: Int = 2,
            createdAt: String = "2026-11-08T15:00:00.000Z",
            completedAt: String? = null
        ) = """
            {"id":"$id","code":"$code","status":"$status","quantity":$quantity,
             "unitPrice":6000,"totalPrice":${6000 * quantity},"createdAt":"$createdAt",
             "completedAt":${completedAt?.let { "\"$it\"" } ?: "null"},
             "offer":{"id":"offer-1","title":"Pan $id","category":"BAKERY","imageUrl":null,
               "originalPrice":12000,"pickupStart":"2026-11-08T22:00:00.000Z",
               "pickupEnd":"2026-11-08T23:00:00.000Z","status":"AVAILABLE"},
             "business":{"name":"Panadería Sol","isVerified":true,"establishmentName":"Sede centro",
               "address":"Cra. 27 #18-45","city":"Pasto","latitude":1.215,"longitude":-77.28}}
        """.trimIndent()
    }
}
