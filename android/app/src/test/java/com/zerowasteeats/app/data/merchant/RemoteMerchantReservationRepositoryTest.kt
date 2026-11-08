package com.zerowasteeats.app.data.merchant

import com.zerowasteeats.app.data.remote.createHttpClient
import com.zerowasteeats.app.data.remote.createRetrofit
import java.time.Instant
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteMerchantReservationRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: RemoteMerchantReservationRepository

    @Before
    fun setUp() {
        server.start()
        val api = createRetrofit(server.url("/api/v1/").toString(), createHttpClient())
            .create(MerchantReservationsApi::class.java)
        repository = RemoteMerchantReservationRepository(api)
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

    private fun reservationJson(id: String, status: String = "PENDING", quantity: Int = 2) = """
        {"id":"$id","status":"$status","quantity":$quantity,"unitPrice":6000,
         "totalPrice":${6000 * quantity},"createdAt":"2026-11-08T15:00:00.000Z","completedAt":null,
         "customer":{"displayName":"Laura Gómez"},
         "offer":{"id":"offer-1","title":"Pan $id","pickupStart":"2026-11-08T22:00:00.000Z",
           "pickupEnd":"2026-11-08T23:00:00.000Z","status":"AVAILABLE"}}
    """.trimIndent()

    private suspend fun loaded(vararg json: String) {
        respond(200, json.joinToString(",", "[", "]"))
        repository.refresh()
        server.takeRequest()
    }

    @Test
    fun theReservations_comeFromTheServer_withoutTheirCodes() = runBlocking {
        respond(200, "[${reservationJson("a")},${reservationJson("b", status = "COMPLETED")}]")

        repository.refresh()

        assertEquals("/api/v1/merchant/reservations", server.takeRequest().url.encodedPath)
        val (a, b) = repository.reservations.value
        assertEquals("Pan a", a.offerTitle)
        assertEquals("Laura Gómez", a.customerName)
        assertEquals(2, a.quantity)
        assertEquals("", a.code)
        assertEquals(Instant.parse("2026-11-08T22:00:00Z"), a.pickupTime)
        assertEquals(MerchantReservationStatus.PENDING, a.status)
        assertEquals(MerchantReservationStatus.COMPLETED, b.status)
        assertEquals(LoadState.Loaded, repository.loadState.value)
    }

    @Test
    fun cancelledReservations_areLeftOut_andConfirmedOnesArePending() = runBlocking {
        loaded(
            reservationJson("gone", status = "CANCELLED"),
            reservationJson("confirmed", status = "CONFIRMED"),
            reservationJson("ready", status = "READY_FOR_PICKUP")
        )

        assertEquals(listOf("confirmed", "ready"), repository.reservations.value.map { it.id })
        assertEquals(
            setOf(MerchantReservationStatus.PENDING),
            repository.reservations.value.map { it.status }.toSet()
        )
    }

    @Test
    fun aFailedLoad_keepsWhatWasLoaded_andSaysWhy() = runBlocking {
        loaded(reservationJson("a"))

        mapOf(
            503 to MerchantDataError.NETWORK,
            401 to MerchantDataError.SESSION,
            400 to MerchantDataError.UNEXPECTED
        ).forEach { (status, expected) ->
            error(status, "x")
            repository.refresh()
            assertEquals("HTTP $status", LoadState.Failed(expected), repository.loadState.value)
        }
        assertEquals(listOf("a"), repository.reservations.value.map { it.id })
    }

    @Test
    fun redeeming_sendsTheTypedCode_toThatReservation() = runBlocking {
        loaded(reservationJson("a"))
        respond(200, reservationJson("a", status = "COMPLETED"))

        repository.redeem("a", " zw-7kq3 ")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/merchant/reservations/a/redeem", request.url.encodedPath)
        assertEquals("""{"code":"zw-7kq3"}""", request.body!!.utf8())
    }

    @Test
    fun theRightCode_completesTheReservation_inTheList() = runBlocking {
        loaded(reservationJson("a"), reservationJson("b"))
        respond(200, reservationJson("a", status = "COMPLETED"))

        val result = repository.redeem("a", "ZW-7KQ3")

        assertEquals(MerchantReservationStatus.COMPLETED, (result as RedeemResult.Success).reservation.status)
        assertEquals(
            listOf(MerchantReservationStatus.COMPLETED, MerchantReservationStatus.PENDING),
            repository.reservations.value.map { it.status }
        )
    }

    @Test
    fun aWrongCode_saysHowManyTriesAreLeft() = runBlocking {
        error(400, "El código no corresponde a esta reserva. Te quedan 3 intentos.")
        assertEquals(RedeemResult.WrongCode(3), repository.redeem("a", "ZW-0000"))

        error(400, "El código no corresponde a esta reserva. Te queda 1 intento.")
        assertEquals(RedeemResult.WrongCode(1), repository.redeem("a", "ZW-0000"))
    }

    @Test
    fun aRejectedCode_withoutACount_isJustInvalid() = runBlocking {
        error(400, "code must be shorter than or equal to 16 characters")

        assertEquals(RedeemResult.InvalidCode, repository.redeem("a", "X".repeat(20)))
    }

    @Test
    fun anAlreadyRedeemedReservation_isSaid_andTheListIsLoadedAgain() = runBlocking {
        loaded(reservationJson("a"))
        error(409, "Ya canjeado: esta reserva ya se entregó.")
        respond(200, "[${reservationJson("a", status = "COMPLETED")}]")

        val result = repository.redeem("a", "ZW-7KQ3")

        assertEquals(RedeemResult.AlreadyRedeemed, result)
        assertEquals(MerchantReservationStatus.COMPLETED, repository.reservations.value.single().status)
    }

    @Test
    fun tooManyWrongCodes_locksForWhatTheServerSays() = runBlocking {
        error(429, "Demasiados intentos con un código incorrecto. Intenta de nuevo en 7 minutos.")
        assertEquals(RedeemResult.Locked(7 * 60_000L), repository.redeem("a", "ZW-0000"))

        error(429, "Demasiados intentos con un código incorrecto. Intenta de nuevo en 1 minuto.")
        assertEquals(RedeemResult.Locked(60_000L), repository.redeem("a", "ZW-0000"))
    }

    @Test
    fun failures_sayWhy() = runBlocking {
        error(401, "x")
        assertEquals(RedeemResult.Failed(MerchantDataError.SESSION), repository.redeem("a", "ZW-7KQ3"))

        error(500, "x")
        assertEquals(RedeemResult.Failed(MerchantDataError.NETWORK), repository.redeem("a", "ZW-7KQ3"))

        server.close()
        assertEquals(RedeemResult.Failed(MerchantDataError.NETWORK), repository.redeem("a", "ZW-7KQ3"))
    }
}
