package com.zerowasteeats.app.data.merchant

import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMerchantReservationRepositoryTest {

    private val reservation = MerchantReservation(
        id = "r-1",
        offerTitle = "Croissants",
        customerName = "Laura Gómez",
        quantity = 2,
        pickupTime = Instant.parse("2026-10-24T23:15:00Z"),
        code = "PAN-4821",
        status = MerchantReservationStatus.PENDING
    )

    private fun repository() =
        FakeMerchantReservationRepository(listOf(reservation), latencyMillis = 0)

    @Test
    fun redeem_withTheRightCode_completesTheReservation() = runBlocking {
        val repo = repository()

        val result = repo.redeem("PAN-4821")

        assertTrue(result is RedeemResult.Success)
        assertEquals(
            MerchantReservationStatus.COMPLETED,
            (result as RedeemResult.Success).reservation.status
        )
        assertEquals(MerchantReservationStatus.COMPLETED, repo.reservations.value.single().status)
    }

    @Test
    fun redeem_ignoresCaseSpacesAndDashes() = runBlocking {
        assertTrue(repository().redeem("  pan 4821 ") is RedeemResult.Success)
        assertTrue(repository().redeem("pan4821") is RedeemResult.Success)
    }

    @Test
    fun redeem_withAWrongCode_isInvalid_andChangesNothing() = runBlocking {
        val repo = repository()

        assertEquals(RedeemResult.InvalidCode, repo.redeem("PAN-0000"))
        assertEquals(RedeemResult.InvalidCode, repo.redeem(""))
        assertEquals(RedeemResult.InvalidCode, repo.redeem("---"))
        assertEquals(MerchantReservationStatus.PENDING, repo.reservations.value.single().status)
    }

    @Test
    fun redeemById_withThatReservationsCode_completesIt() = runBlocking {
        val repo = repository()

        val result = repo.redeem("r-1", "pan-4821")

        assertTrue(result is RedeemResult.Success)
        assertEquals(MerchantReservationStatus.COMPLETED, repo.reservations.value.single().status)
    }

    @Test
    fun redeemById_withAnotherReservationsCode_isInvalid() = runBlocking {
        val other = reservation.copy(id = "r-2", code = "PAN-7305")
        val repo = FakeMerchantReservationRepository(listOf(reservation, other), latencyMillis = 0)

        assertEquals(RedeemResult.InvalidCode, repo.redeem("r-1", "PAN-7305"))
        assertTrue(repo.reservations.value.all { it.status == MerchantReservationStatus.PENDING })
    }

    @Test
    fun redeemById_ofAnUnknownReservation_isInvalid() = runBlocking {
        assertEquals(RedeemResult.InvalidCode, repository().redeem("r-9", "PAN-4821"))
    }

    @Test
    fun redeemById_twice_failsTheSecondTime() = runBlocking {
        val repo = repository()

        repo.redeem("r-1", "PAN-4821")

        assertEquals(RedeemResult.AlreadyRedeemed, repo.redeem("r-1", "PAN-4821"))
    }

    @Test
    fun redeem_theSameCodeTwice_failsTheSecondTime() = runBlocking {
        val repo = repository()

        assertTrue(repo.redeem("PAN-4821") is RedeemResult.Success)
        assertEquals(RedeemResult.AlreadyRedeemed, repo.redeem("pan-4821"))
    }
}
