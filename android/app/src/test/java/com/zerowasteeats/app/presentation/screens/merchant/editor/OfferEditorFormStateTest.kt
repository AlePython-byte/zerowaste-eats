package com.zerowasteeats.app.presentation.screens.merchant.editor

import androidx.compose.runtime.saveable.SaverScope
import com.zerowasteeats.app.data.merchant.FakeMerchantOfferRepository
import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantOfferStatus
import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferEditorFormStateTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val today: LocalDate = LocalDate.of(2026, 10, 24)
    private val clock = Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), zone)

    /** A new form with nothing filled in, not even the default pickup window. */
    private fun emptyForm() = OfferEditorFormState(OfferEditorValues(), isNewOffer = true)

    private fun clockAt(time: String): Clock =
        Clock.fixed(Instant.parse("2026-10-24T${time}:00Z"), zone)

    private fun validForm() = OfferEditorFormState.newOffer(clock).apply {
        onTitleChange("Croissants del día")
        onCategorySelected(OfferCategory.BAKERY)
        onPhotoSelected(OfferPhoto.CROISSANT)
        onOriginalPriceChange("15000")
        onDiscountedPriceChange("8900")
        onQuantityChange("6")
        onPickupStartSelected(18 * 60)
        onPickupEndSelected(19 * 60 + 30)
    }

    private fun OfferEditorFormState.shownError(field: OfferField): OfferFieldError? {
        onBlur(field)
        return errorOf(field)
    }

    // --- When errors show ---

    @Test
    fun newForm_showsNoErrors_evenThoughItIsNotValid() {
        val form = OfferEditorFormState.newOffer(clock)

        OfferField.entries.forEach { assertNull(form.errorOf(it)) }
    }

    @Test
    fun typing_showsNoErrorBeforeLeavingTheField() {
        val form = OfferEditorFormState.newOffer(clock)

        form.onTitleChange("P")
        form.onOriginalPriceChange("0")

        assertNull(form.errorOf(OfferField.TITLE))
        assertNull(form.errorOf(OfferField.ORIGINAL_PRICE))
    }

    @Test
    fun afterLeavingAField_itsErrorFollowsEachChange() {
        val form = OfferEditorFormState.newOffer(clock)
        form.onTitleChange("P")
        form.onBlur(OfferField.TITLE)
        assertEquals(OfferFieldError.TITLE_TOO_SHORT, form.errorOf(OfferField.TITLE))

        form.onTitleChange("Pan")

        assertNull(form.errorOf(OfferField.TITLE))
    }

    @Test
    fun validate_showsEveryError_andReturnsTheFirstInScreenOrder() {
        val form = emptyForm()

        assertEquals(OfferField.TITLE, form.validate())
        assertEquals(OfferFieldError.REQUIRED, form.errorOf(OfferField.CATEGORY))
        assertEquals(OfferFieldError.REQUIRED, form.errorOf(OfferField.PICKUP_END))
    }

    @Test
    fun validate_pointsAtTheFirstInvalidField_skippingValidOnes() {
        val form = validForm().apply { onDiscountedPriceChange("20000") }

        assertEquals(OfferField.DISCOUNTED_PRICE, form.validate())
    }

    @Test
    fun aValidForm_validatesToNull() {
        assertNull(validForm().validate())
    }

    // --- Each rule ---

    @Test
    fun title_mustHaveThreeToEightyCharacters_ignoringSurroundingSpaces() {
        val form = validForm()

        form.onTitleChange("   ")
        assertEquals(OfferFieldError.REQUIRED, form.shownError(OfferField.TITLE))
        form.onTitleChange("  Pa ")
        assertEquals(OfferFieldError.TITLE_TOO_SHORT, form.shownError(OfferField.TITLE))
        form.onTitleChange(" Pan ")
        assertNull(form.shownError(OfferField.TITLE))
        form.onTitleChange("x".repeat(80))
        assertNull(form.shownError(OfferField.TITLE))
        form.onTitleChange("x".repeat(81))
        assertEquals(OfferFieldError.TITLE_TOO_LONG, form.shownError(OfferField.TITLE))
    }

    @Test
    fun categoryAndPhoto_mustBeChosen_andChoosingShowsTheResult() {
        val form = OfferEditorFormState.newOffer(clock)
        assertEquals(OfferFieldError.REQUIRED, form.shownError(OfferField.CATEGORY))
        assertEquals(OfferFieldError.REQUIRED, form.shownError(OfferField.PHOTO))

        form.onCategorySelected(OfferCategory.DESSERTS)
        form.onPhotoSelected(OfferPhoto.DESSERTS)

        assertNull(form.errorOf(OfferField.CATEGORY))
        assertNull(form.errorOf(OfferField.PHOTO))
    }

    @Test
    fun all_isNotACategory() {
        assertThrows(IllegalArgumentException::class.java) {
            OfferEditorFormState.newOffer(clock).onCategorySelected(OfferCategory.ALL)
        }
    }

    @Test
    fun prices_areRequired_andMustBeAboveZero() {
        val form = validForm()

        form.onOriginalPriceChange("")
        assertEquals(OfferFieldError.REQUIRED, form.shownError(OfferField.ORIGINAL_PRICE))
        form.onOriginalPriceChange("0")
        assertEquals(OfferFieldError.PRICE_NOT_POSITIVE, form.shownError(OfferField.ORIGINAL_PRICE))
        form.onDiscountedPriceChange("000")
        assertEquals(OfferFieldError.PRICE_NOT_POSITIVE, form.shownError(OfferField.DISCOUNTED_PRICE))
    }

    @Test
    fun discountedPrice_mustBeLowerThanTheOriginal() {
        val form = validForm()

        form.onDiscountedPriceChange("15000")
        assertEquals(OfferFieldError.DISCOUNT_NOT_LOWER, form.shownError(OfferField.DISCOUNTED_PRICE))
        form.onDiscountedPriceChange("16000")
        assertEquals(OfferFieldError.DISCOUNT_NOT_LOWER, form.shownError(OfferField.DISCOUNTED_PRICE))
        form.onDiscountedPriceChange("14999")
        assertNull(form.shownError(OfferField.DISCOUNTED_PRICE))
    }

    @Test
    fun discountedPrice_isNotComparedWhileTheOriginalIsMissing() {
        val form = validForm().apply { onOriginalPriceChange("") }

        assertNull(form.shownError(OfferField.DISCOUNTED_PRICE))
    }

    @Test
    fun priceInput_keepsDigitsOnly_withoutLeadingZeros_andAtMostNine() {
        val form = OfferEditorFormState.newOffer(clock)

        form.onOriginalPriceChange("$15.000")
        assertEquals("15000", form.values.originalPrice)
        form.onOriginalPriceChange("0089")
        assertEquals("89", form.values.originalPrice)
        form.onOriginalPriceChange("0")
        assertEquals("0", form.values.originalPrice)
        form.onOriginalPriceChange("12345678901")
        assertEquals("123456789", form.values.originalPrice)
    }

    @Test
    fun quantity_mustBeBetweenOneAndNinetyNine() {
        val form = validForm()

        form.onQuantityChange("")
        assertEquals(OfferFieldError.REQUIRED, form.shownError(OfferField.QUANTITY))
        form.onQuantityChange("0")
        assertEquals(OfferFieldError.QUANTITY_OUT_OF_RANGE, form.shownError(OfferField.QUANTITY))
        form.onQuantityChange("100")
        assertEquals(OfferFieldError.QUANTITY_OUT_OF_RANGE, form.shownError(OfferField.QUANTITY))
        form.onQuantityChange("99")
        assertNull(form.shownError(OfferField.QUANTITY))
    }

    @Test
    fun quantityStepper_staysWithinTheLimits() {
        val form = OfferEditorFormState.newOffer(clock)
        assertEquals("1", form.values.quantity)

        form.decrementQuantity()
        assertEquals("1", form.values.quantity)
        form.incrementQuantity()
        assertEquals("2", form.values.quantity)
        form.onQuantityChange("99")
        form.incrementQuantity()
        assertEquals("99", form.values.quantity)
        form.onQuantityChange("")
        form.incrementQuantity()
        assertEquals("1", form.values.quantity)
    }

    @Test
    fun pickupTimes_areRequired_andTheEndMustBeAfterTheStart() {
        val form = emptyForm()
        assertEquals(OfferFieldError.REQUIRED, form.shownError(OfferField.PICKUP_START))
        assertEquals(OfferFieldError.REQUIRED, form.shownError(OfferField.PICKUP_END))

        form.onPickupStartSelected(19 * 60)
        form.onPickupEndSelected(19 * 60)
        assertEquals(OfferFieldError.END_NOT_AFTER_START, form.errorOf(OfferField.PICKUP_END))

        form.onPickupEndSelected(18 * 60)
        assertEquals(OfferFieldError.END_NOT_AFTER_START, form.errorOf(OfferField.PICKUP_END))

        // Moving the start earlier fixes the end, and the error goes away by itself
        form.onPickupStartSelected(17 * 60)
        assertNull(form.errorOf(OfferField.PICKUP_END))
    }

    @Test
    fun pickupEnd_mustBeLaterThanNow_whenSaving() {
        val form = validForm() // Ends at 19:30

        assertEquals(OfferField.PICKUP_END, form.validate(nowMinutes = 19 * 60 + 30))
        assertEquals(OfferFieldError.END_IN_PAST, form.errorOf(OfferField.PICKUP_END))

        // Picking a later end clears it right away
        form.onPickupEndSelected(21 * 60)
        assertNull(form.errorOf(OfferField.PICKUP_END))
        assertNull(form.validate(nowMinutes = 19 * 60 + 30))
    }

    @Test
    fun pickupEnd_isNotComparedWithNow_untilASaveAttempt() {
        val form = validForm()
        form.onBlur(OfferField.PICKUP_END)

        assertNull(form.errorOf(OfferField.PICKUP_END))
        assertNull(form.validate())
    }

    // --- Default pickup window ---

    @Test
    fun defaultWindow_inTheMorning_startsAtTheNextHalfHour_forAnHour() {
        assertEquals(9 * 60 + 30 to 10 * 60 + 30, OfferEditorFormState.defaultPickupWindow(9 * 60 + 10))
    }

    @Test
    fun defaultWindow_inTheAfternoon_roundsUpToTheNextHour() {
        assertEquals(16 * 60 to 17 * 60, OfferEditorFormState.defaultPickupWindow(15 * 60 + 45))
    }

    @Test
    fun defaultWindow_onTheHalfHour_startsAtTheNextOne() {
        assertEquals(15 * 60 to 16 * 60, OfferEditorFormState.defaultPickupWindow(14 * 60 + 30))
    }

    @Test
    fun defaultWindow_at2310_isCutAtMidnight() {
        assertEquals(23 * 60 + 30 to 23 * 60 + 59, OfferEditorFormState.defaultPickupWindow(23 * 60 + 10))
    }

    @Test
    fun defaultWindow_at2350_startsNow_soItStillFits() {
        val (start, end) = OfferEditorFormState.defaultPickupWindow(23 * 60 + 50)

        assertEquals(23 * 60 + 50, start)
        assertEquals(23 * 60 + 59, end)
    }

    @Test
    fun defaultWindow_at2359_cannotFit_soSavingShowsTheUsualError() {
        assertEquals(23 * 60 + 58 to 23 * 60 + 59, OfferEditorFormState.defaultPickupWindow(23 * 60 + 59))

        val form = validForm().apply {
            onPickupStartSelected(23 * 60 + 58)
            onPickupEndSelected(23 * 60 + 59)
        }
        assertEquals(OfferField.PICKUP_END, form.validate(nowMinutes = 23 * 60 + 59))
        assertEquals(OfferFieldError.END_IN_PAST, form.errorOf(OfferField.PICKUP_END))
    }

    @Test
    fun newOffer_takesTheDefaultWindowFromTheClock_andIsUnchanged() {
        val morning = OfferEditorFormState.newOffer(clockAt("09:10"))
        val night = OfferEditorFormState.newOffer(clockAt("23:10"))

        assertEquals(9 * 60 + 30, morning.values.pickupStartMinutes)
        assertEquals(10 * 60 + 30, morning.values.pickupEndMinutes)
        assertEquals(23 * 60 + 59, night.values.pickupEndMinutes)
        assertFalse(morning.hasUnsavedChanges)
        assertFalse(morning.isPickupWindowChanged)
        assertNull(morning.errorOf(OfferField.PICKUP_START))
    }

    @Test
    fun aNewOffer_withItsDefaultWindow_passesTheTimeCheck() {
        val form = OfferEditorFormState.newOffer(clockAt("23:50")).apply {
            onTitleChange("Pan de la casa")
            onCategorySelected(OfferCategory.BAKERY)
            onPhotoSelected(OfferPhoto.CROISSANT)
            onOriginalPriceChange("12000")
            onDiscountedPriceChange("6000")
        }

        assertNull(form.validate(nowMinutes = 23 * 60 + 50))
    }

    // --- Editing an offer whose pickup is over ---

    @Test
    fun editingThePrice_ofAnOfferWhosePickupEnded_doesNotAskToChangeTheTime() {
        // Pickup 18:00 to 19:30, and now it is 21:00
        val form = OfferEditorFormState.fromOffer(sampleOffer(), zone).apply {
            onDiscountedPriceChange("7000")
            onQuantityChange("3")
        }

        assertNull(form.validate(nowMinutes = 21 * 60))
        assertNull(form.errorOf(OfferField.PICKUP_END))
    }

    @Test
    fun changingTheWindow_ofAnOfferWhosePickupEnded_mustEndLaterThanNow() {
        val form = OfferEditorFormState.fromOffer(sampleOffer(), zone).apply {
            onPickupEndSelected(20 * 60)
        }

        assertTrue(form.isPickupWindowChanged)
        assertEquals(OfferField.PICKUP_END, form.validate(nowMinutes = 21 * 60))
        assertEquals(OfferFieldError.END_IN_PAST, form.errorOf(OfferField.PICKUP_END))
    }

    @Test
    fun editingWithoutTouchingTheWindow_keepsTheOffersOwnPickupTimes() {
        val original = endedYesterday()
        val form = OfferEditorFormState.fromOffer(original, zone).apply { onDiscountedPriceChange("7000") }

        val draft = form.toDraft(today, zone, existingOffer = original)

        assertEquals(original.pickupStart, draft.pickupStart)
        assertEquals(original.pickupEnd, draft.pickupEnd)
        assertEquals(7_000L, draft.discountedPrice)
    }

    @Test
    fun editingTheWindow_movesItToToday() {
        val original = endedYesterday()
        val form = OfferEditorFormState.fromOffer(original, zone).apply { onPickupEndSelected(21 * 60) }

        val draft = form.toDraft(today, zone, existingOffer = original)

        assertEquals(Instant.parse("2026-10-24T18:00:00Z"), draft.pickupStart)
        assertEquals(Instant.parse("2026-10-24T21:00:00Z"), draft.pickupEnd)
    }

    @Test
    fun savingAPriceChange_ofAnEndedOffer_goesThroughTheRepository() = runBlocking {
        // The clock says 15:00, and this pickup ended yesterday
        val original = endedYesterday().copy(status = MerchantOfferStatus.EXPIRED)
        val repository = FakeMerchantOfferRepository(listOf(original), latencyMillis = 0, clock = clock)
        val form = OfferEditorFormState.fromOffer(original, zone).apply { onDiscountedPriceChange("7000") }
        assertNull(form.validate(nowMinutes = 15 * 60))

        val result = saveOffer(repository, original.id, form.toDraft(today, zone, original))

        assertEquals(OfferSaveResult.UPDATED, result)
        val saved = repository.offers.value.single()
        assertEquals(7_000L, saved.discountedPrice)
        assertEquals(original.pickupEnd, saved.pickupEnd)
        // The pickup is still over, so the offer stays expired
        assertEquals(MerchantOfferStatus.EXPIRED, saved.status)
    }

    private fun endedYesterday() = sampleOffer().copy(
        pickupStart = Instant.parse("2026-10-23T18:00:00Z"),
        pickupEnd = Instant.parse("2026-10-23T19:30:00Z")
    )

    // --- Discount percent ---

    @Test
    fun discountPercent_isComputedLive_roundedDown() {
        val form = validForm()

        assertEquals(40, form.discountPercent) // 15.000 -> 8.900 is 40.67 %

        form.onDiscountedPriceChange("7500")
        assertEquals(50, form.discountPercent)
    }

    @Test
    fun discountPercent_isNullUntilBothPricesMakeSense() {
        val form = OfferEditorFormState.newOffer(clock)
        assertNull(form.discountPercent)

        form.onOriginalPriceChange("10000")
        assertNull(form.discountPercent)
        form.onDiscountedPriceChange("12000")
        assertNull(form.discountPercent)
        form.onDiscountedPriceChange("0")
        assertNull(form.discountPercent)
    }

    // --- Unsaved changes ---

    @Test
    fun newForm_hasNoUnsavedChanges_untilSomethingIsTyped() {
        val form = OfferEditorFormState.newOffer(clock)
        assertFalse(form.hasUnsavedChanges)

        form.onTitleChange("P")
        assertTrue(form.hasUnsavedChanges)
    }

    @Test
    fun undoingAnEdit_byHand_leavesNoUnsavedChanges() {
        val form = OfferEditorFormState.newOffer(clock)

        form.onTitleChange("Pan")
        form.onTitleChange("")

        assertFalse(form.hasUnsavedChanges)
    }

    @Test
    fun editForm_startsUnchanged_andNoticesAChoiceChange() {
        val form = OfferEditorFormState.fromOffer(sampleOffer(), zone)
        assertFalse(form.hasUnsavedChanges)

        form.onPhotoSelected(OfferPhoto.BURGER)
        assertTrue(form.hasUnsavedChanges)
    }

    @Test
    fun leavingAField_isNotAChange() {
        val form = OfferEditorFormState.newOffer(clock)

        form.onBlur(OfferField.TITLE)
        form.validate()

        assertFalse(form.hasUnsavedChanges)
    }

    // --- Saver ---

    @Test
    fun saver_keepsTheValues_theInitialValues_andWhichErrorsAreShown() {
        val form = OfferEditorFormState.fromOffer(sampleOffer(), zone).apply {
            onTitleChange("Pa")
            onBlur(OfferField.TITLE)
            onPickupEndSelected(17 * 60)
        }

        val saved = with(OfferEditorFormState.Saver) { SaverScope { true }.save(form) }!!
        val restored = OfferEditorFormState.Saver.restore(saved)!!

        assertEquals(form.values, restored.values)
        assertEquals(form.initialValues, restored.initialValues)
        assertFalse(restored.isNewOffer)
        assertTrue(restored.hasUnsavedChanges)
        assertEquals(OfferFieldError.TITLE_TOO_SHORT, restored.errorOf(OfferField.TITLE))
        assertEquals(OfferFieldError.END_NOT_AFTER_START, restored.errorOf(OfferField.PICKUP_END))
        assertNull(restored.errorOf(OfferField.PHOTO))
    }

    @Test
    fun saver_keepsThePastPickupCheckOfTheLastSaveAttempt() {
        val form = validForm().apply { validate(nowMinutes = 20 * 60) }

        val saved = with(OfferEditorFormState.Saver) { SaverScope { true }.save(form) }!!
        val restored = OfferEditorFormState.Saver.restore(saved)!!

        assertEquals(OfferFieldError.END_IN_PAST, restored.errorOf(OfferField.PICKUP_END))
    }

    @Test
    fun saver_keepsAnEmptyNewForm() {
        val form = OfferEditorFormState.newOffer(clock)

        val saved = with(OfferEditorFormState.Saver) { SaverScope { true }.save(form) }!!
        val restored = OfferEditorFormState.Saver.restore(saved)!!

        assertEquals(form.values, restored.values)
        assertTrue(restored.isNewOffer)
        assertFalse(restored.hasUnsavedChanges)
    }

    // --- Draft, create and edit against the fake repository ---

    @Test
    fun toDraft_putsThePickupWindowOnTheGivenDay() {
        val draft = validForm().toDraft(today, zone)

        assertEquals("Croissants del día", draft.title)
        assertEquals(15_000L, draft.originalPrice)
        assertEquals(8_900L, draft.discountedPrice)
        assertEquals(6, draft.quantity)
        assertEquals(Instant.parse("2026-10-24T18:00:00Z"), draft.pickupStart)
        assertEquals(Instant.parse("2026-10-24T19:30:00Z"), draft.pickupEnd)
    }

    @Test
    fun saving_withoutAnId_createsAnActiveOffer() = runBlocking {
        val repository = FakeMerchantOfferRepository(latencyMillis = 0, clock = clock)

        val result = saveOffer(repository, offerId = null, draft = validForm().toDraft(today, zone))

        assertEquals(OfferSaveResult.CREATED, result)
        val offer = repository.offers.value.single()
        assertEquals("Croissants del día", offer.title)
        assertEquals(MerchantOfferStatus.ACTIVE, offer.status)
    }

    @Test
    fun saving_withAnId_editsThatOffer_andKeepsItsId() = runBlocking {
        val original = sampleOffer()
        val repository = FakeMerchantOfferRepository(listOf(original), latencyMillis = 0, clock = clock)
        val form = OfferEditorFormState.fromOffer(original, zone).apply {
            onTitleChange("Croissants rellenos")
            onDiscountedPriceChange("7000")
        }

        val result = saveOffer(repository, original.id, form.toDraft(today, zone))

        assertEquals(OfferSaveResult.UPDATED, result)
        val edited = repository.offers.value.single()
        assertEquals(original.id, edited.id)
        assertEquals("Croissants rellenos", edited.title)
        assertEquals(7_000L, edited.discountedPrice)
    }

    @Test
    fun saving_anOfferThatIsGone_reportsNotFound() = runBlocking {
        val repository = FakeMerchantOfferRepository(latencyMillis = 0, clock = clock)

        val result = saveOffer(repository, "gone", validForm().toDraft(today, zone))

        assertEquals(OfferSaveResult.NOT_FOUND, result)
        assertTrue(repository.offers.value.isEmpty())
    }

    @Test
    fun fromOffer_readsEveryField_back() {
        val form = OfferEditorFormState.fromOffer(sampleOffer(), zone)

        with(form.values) {
            assertEquals("Croissants del día", title)
            assertEquals(OfferCategory.BAKERY, category)
            assertEquals(OfferPhoto.CROISSANT, photo)
            assertEquals("15000", originalPrice)
            assertEquals("8900", discountedPrice)
            assertEquals("6", quantity)
            assertEquals(18 * 60, pickupStartMinutes)
            assertEquals(19 * 60 + 30, pickupEndMinutes)
        }
        assertNull(form.validate())
    }

    private fun sampleOffer() = MerchantOffer(
        id = "offer-1",
        title = "Croissants del día",
        category = OfferCategory.BAKERY,
        originalPrice = 15_000,
        discountedPrice = 8_900,
        quantity = 6,
        pickupStart = Instant.parse("2026-10-24T18:00:00Z"),
        pickupEnd = Instant.parse("2026-10-24T19:30:00Z"),
        status = MerchantOfferStatus.ACTIVE,
        photo = OfferPhoto.CROISSANT
    )
}
