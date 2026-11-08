package com.zerowasteeats.app.presentation.screens.merchant.reservations

import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.merchant.FakeMerchantData
import com.zerowasteeats.app.data.merchant.LoadState
import com.zerowasteeats.app.data.merchant.MerchantDataError
import com.zerowasteeats.app.data.merchant.MerchantReservation
import com.zerowasteeats.app.data.merchant.MerchantReservationRepository
import com.zerowasteeats.app.data.merchant.RedeemResult
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import com.zerowasteeats.app.data.merchant.MerchantReservationStatus
import com.zerowasteeats.app.data.session.Session
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantReservationsTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), ZoneOffset.UTC)

    private val demo = Session.Merchant(
        userId = "demo-merchant",
        displayName = "Negocio Demo",
        email = FakeAuthRepository.DEMO_MERCHANT_EMAIL,
        businessName = "Panadería Demo"
    )

    // Demo reservations: 1 (PAN-4821) and 2 (PAN-7305) pending, 3 (PAN-1190) completed
    private fun repository(): MerchantReservationRepository =
        FakeMerchantData.repositoriesFor(demo, latencyMillis = 0, clock = clock).reservations

    /** A clock for the lock that the test moves by hand. */
    private class TestTime(var millis: Long = 1_000_000L)

    private fun confirmationWith(
        repository: MerchantReservationRepository,
        time: TestTime
    ) = PickupConfirmation(repository, PickupCodeAttempts(nowMillis = { time.millis }))

    private fun stateOf(repository: MerchantReservationRepository, tab: ReservationTab) =
        MerchantReservationListState(repository.reservations.value, tab)

    // --- Confirming a pickup ---

    @Test
    fun theRightCode_confirmsThatReservation() = runBlocking {
        val repo = repository()
        val confirmation = confirmationWith(repo, TestTime())

        val result = confirmation.confirm("demo-reservation-1", "pan-4821")

        assertTrue(result is PickupConfirmResult.Confirmed)
        val completed = repo.reservations.value.first { it.id == "demo-reservation-1" }
        assertEquals(MerchantReservationStatus.COMPLETED, completed.status)
    }

    @Test
    fun aWrongCode_isRefused_andSaysHowManyAttemptsAreLeft() = runBlocking {
        val repo = repository()
        val confirmation = confirmationWith(repo, TestTime())

        assertEquals(PickupConfirmResult.WrongCode(attemptsLeft = 4), confirmation.confirm("demo-reservation-1", "PAN-0000"))
        assertEquals(PickupConfirmResult.WrongCode(attemptsLeft = 3), confirmation.confirm("demo-reservation-1", "PAN-1111"))
        assertEquals(
            MerchantReservationStatus.PENDING,
            repo.reservations.value.first { it.id == "demo-reservation-1" }.status
        )
    }

    @Test
    fun anotherReservationsCode_doesNotConfirmThisOne() = runBlocking {
        val repo = repository()
        val confirmation = confirmationWith(repo, TestTime())

        // PAN-7305 is valid, but it belongs to reservation 2
        val result = confirmation.confirm("demo-reservation-1", "PAN-7305")

        assertTrue(result is PickupConfirmResult.WrongCode)
        assertTrue(repo.reservations.value.none { it.status == MerchantReservationStatus.COMPLETED && it.id != "demo-reservation-3" })
    }

    @Test
    fun theSameCodeTwice_failsTheSecondTime() = runBlocking {
        val repo = repository()
        val confirmation = confirmationWith(repo, TestTime())

        confirmation.confirm("demo-reservation-1", "PAN-4821")

        assertEquals(PickupConfirmResult.AlreadyCompleted, confirmation.confirm("demo-reservation-1", "PAN-4821"))
    }

    @Test
    fun fiveWrongCodesInARow_lockTheReservationForThirtySeconds() = runBlocking {
        val repo = repository()
        val time = TestTime()
        val confirmation = confirmationWith(repo, time)

        repeat(4) { confirmation.confirm("demo-reservation-1", "WRONG") }
        val fifth = confirmation.confirm("demo-reservation-1", "WRONG")

        assertEquals(PickupConfirmResult.Locked(untilMillis = time.millis + 30_000), fifth)

        // While locked even the right code is turned away, without reaching the repository
        time.millis += 29_000
        assertTrue(confirmation.confirm("demo-reservation-1", "PAN-4821") is PickupConfirmResult.Locked)
        assertEquals(
            MerchantReservationStatus.PENDING,
            repo.reservations.value.first { it.id == "demo-reservation-1" }.status
        )

        // Once the 30 seconds are over the right code works again
        time.millis += 1_000
        assertTrue(confirmation.confirm("demo-reservation-1", "PAN-4821") is PickupConfirmResult.Confirmed)
    }

    @Test
    fun afterTheLock_theCountStartsOverAtFive() = runBlocking {
        val time = TestTime()
        val confirmation = confirmationWith(repository(), time)
        repeat(5) { confirmation.confirm("demo-reservation-1", "WRONG") }
        time.millis += 30_000

        assertEquals(PickupConfirmResult.WrongCode(attemptsLeft = 4), confirmation.confirm("demo-reservation-1", "WRONG"))
    }

    @Test
    fun theLock_isPerReservation() = runBlocking {
        val repo = repository()
        val confirmation = confirmationWith(repo, TestTime())
        repeat(5) { confirmation.confirm("demo-reservation-1", "WRONG") }

        assertTrue(confirmation.confirm("demo-reservation-2", "PAN-7305") is PickupConfirmResult.Confirmed)
    }

    @Test
    fun aRightCode_clearsTheWrongOnesBefore() = runBlocking {
        val repo = repository()
        val attempts = PickupCodeAttempts(nowMillis = { 0L })
        val confirmation = PickupConfirmation(repo, attempts)
        repeat(3) { confirmation.confirm("demo-reservation-2", "WRONG") }

        confirmation.confirm("demo-reservation-2", "PAN-7305")

        assertEquals(5, attempts.attemptsLeft("demo-reservation-2"))
        assertNull(attempts.lockedUntil("demo-reservation-2"))
    }

    // --- Tabs and counts ---

    @Test
    fun counts_followTheStatusOfEachReservation() {
        val state = stateOf(repository(), ReservationTab.PENDING)

        assertEquals(2, state.counts.getValue(ReservationTab.PENDING))
        assertEquals(1, state.counts.getValue(ReservationTab.COMPLETED))
    }

    @Test
    fun pending_isSoonestFirst_andCompleted_isLatestFirst() {
        val repo = repository()

        assertEquals(
            listOf("demo-reservation-1", "demo-reservation-2"),
            stateOf(repo, ReservationTab.PENDING).visibleReservations.map { it.id }
        )
        assertEquals(
            listOf("demo-reservation-3"),
            stateOf(repo, ReservationTab.COMPLETED).visibleReservations.map { it.id }
        )
    }

    @Test
    fun confirming_movesTheReservationToCompleted_andUpdatesTheCounts() = runBlocking {
        val repo = repository()
        confirmationWith(repo, TestTime()).confirm("demo-reservation-1", "PAN-4821")

        val pending = stateOf(repo, ReservationTab.PENDING)
        val completed = stateOf(repo, ReservationTab.COMPLETED)

        assertEquals(listOf("demo-reservation-2"), pending.visibleReservations.map { it.id })
        assertTrue(completed.visibleReservations.any { it.id == "demo-reservation-1" })
        assertEquals(1, pending.counts.getValue(ReservationTab.PENDING))
        assertEquals(2, pending.counts.getValue(ReservationTab.COMPLETED))
    }

    @Test
    fun noReservations_meansEmptyTabsAndZeroCounts() {
        val state = MerchantReservationListState(emptyList(), ReservationTab.COMPLETED)

        assertTrue(state.visibleReservations.isEmpty())
        assertTrue(state.counts.values.all { it == 0 })
    }

    // --- Code input ---

    @Test
    fun codeInput_isUpperCase_keepsLettersDigitsAndDashes_andIsShort() {
        assertEquals("PAN-4821", normalizePickupCodeInput("pan-4821"))
        assertEquals("PAN4821", normalizePickupCodeInput("pan 48.21"))
        assertEquals("ABCDEFGHIJ", normalizePickupCodeInput("abcdefghijklmn"))
    }

    // --- The server's answer wins ---

    /** A repository that answers every redeem with [next], like the server would. */
    private class ServerAnswer(var next: RedeemResult) : MerchantReservationRepository {
        var calls = 0
        override val reservations = MutableStateFlow<List<MerchantReservation>>(emptyList())
        override val loadState = MutableStateFlow<LoadState>(LoadState.Loaded)
        override suspend fun refresh() = Unit
        override suspend fun redeem(reservationId: String, code: String): RedeemResult {
            calls++
            return next
        }
    }

    @Test
    fun theServersCount_isWhatIsShown() = runBlocking {
        val server = ServerAnswer(RedeemResult.WrongCode(attemptsLeft = 2))
        val confirmation = confirmationWith(server, TestTime())

        assertEquals(PickupConfirmResult.WrongCode(attemptsLeft = 2), confirmation.confirm("r", "ZW-0000"))
        // The local count does not run ahead of the server's: five more tries still reach it
        repeat(5) { confirmation.confirm("r", "ZW-0000") }
        assertEquals(6, server.calls)
    }

    @Test
    fun theServersLock_locksTheSheet_forAsLongAsItSays() = runBlocking {
        val time = TestTime()
        val server = ServerAnswer(RedeemResult.Locked(retryAfterMillis = 7 * 60_000L))
        val confirmation = confirmationWith(server, time)

        val result = confirmation.confirm("r", "ZW-0000")

        assertEquals(PickupConfirmResult.Locked(untilMillis = time.millis + 7 * 60_000L), result)
        // While locked nothing is sent
        confirmation.confirm("r", "ZW-7KQ3")
        assertEquals(1, server.calls)
        // Once it is over, codes go to the server again
        time.millis += 7 * 60_000L
        server.next = RedeemResult.WrongCode(attemptsLeft = 4)
        assertEquals(PickupConfirmResult.WrongCode(attemptsLeft = 4), confirmation.confirm("r", "ZW-0000"))
    }

    @Test
    fun aCodeThatNeverReachedTheServer_doesNotCount() = runBlocking {
        val server = ServerAnswer(RedeemResult.Failed(MerchantDataError.NETWORK))
        val attempts = PickupCodeAttempts(nowMillis = { 0L })
        val confirmation = PickupConfirmation(server, attempts)

        val result = confirmation.confirm("r", "ZW-7KQ3")

        assertEquals(PickupConfirmResult.Failed(MerchantDataError.NETWORK), result)
        assertEquals(attempts.maxFailures, attempts.attemptsLeft("r"))
    }

    @Test
    fun aFailedLoad_withNothingLoaded_takesThePlaceOfTheList() {
        val failed = MerchantReservationListState(
            emptyList(),
            ReservationTab.PENDING,
            LoadState.Failed(MerchantDataError.NETWORK)
        )
        val loading = MerchantReservationListState(emptyList(), ReservationTab.PENDING, LoadState.Loading)

        assertTrue(failed.showError)
        assertTrue(loading.showSkeleton)
        assertFalse(MerchantReservationListState(emptyList(), ReservationTab.PENDING).showSkeleton)
    }
}
