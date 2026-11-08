package com.zerowasteeats.app.presentation.screens.merchant.editor

import androidx.compose.runtime.saveable.SaverScope
import com.zerowasteeats.app.data.ai.OfferSuggestion
import com.zerowasteeats.app.data.ai.SuggestionError
import com.zerowasteeats.app.data.ai.SuggestionResult
import com.zerowasteeats.app.presentation.model.OfferCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The editor's pure state around a suggestion: filling, marking, editing, saving. */
class OfferSuggestionFormTest {

    private val suggestion = OfferSuggestion(
        title = "Bolsa de pan del día",
        category = OfferCategory.BAKERY,
        description = "Pan blanco y mogollas de esta mañana.",
        discountPercent = 50,
        discountedPrice = 6000,
        confidence = 0.9,
        warnings = emptyList(),
        isExample = false
    )

    private fun form() = OfferEditorFormState(OfferEditorValues(), isNewOffer = true)

    @Test
    fun aSuggestion_fillsTheFields_andMarksThemAsSuggested() {
        val form = form().apply { onOriginalPriceChange("12000") }

        form.applySuggestion(suggestion)

        with(form.values) {
            assertEquals("Bolsa de pan del día", title)
            assertEquals(OfferCategory.BAKERY, category)
            assertEquals("Pan blanco y mogollas de esta mañana.", description)
            assertEquals("6000", discountedPrice)
            assertEquals("12000", originalPrice)
        }
        assertEquals(50, form.discountPercent)
        assertEquals(
            setOf(OfferField.TITLE, OfferField.CATEGORY, OfferField.DESCRIPTION, OfferField.DISCOUNTED_PRICE),
            form.suggestedFields
        )
    }

    @Test
    fun withoutAnOriginalPrice_thePriceIsLeftForTheMerchant() {
        val form = form()

        form.applySuggestion(suggestion.copy(discountedPrice = null))

        assertEquals("", form.values.discountedPrice)
        assertTrue(OfferField.DISCOUNTED_PRICE !in form.suggestedFields)
    }

    @Test
    fun aPriceThatIsNotBelowTheOriginal_isNotFilled() {
        val form = form().apply { onOriginalPriceChange("5000") }

        form.applySuggestion(suggestion.copy(discountedPrice = 6000))

        assertEquals("", form.values.discountedPrice)
    }

    @Test
    fun aCategoryTheAppDoesNotHave_leavesTheChoiceAlone() {
        val form = form().apply { onCategorySelected(OfferCategory.DESSERTS) }

        form.applySuggestion(suggestion.copy(category = null))

        assertEquals(OfferCategory.DESSERTS, form.values.category)
        assertTrue(OfferField.CATEGORY !in form.suggestedFields)
    }

    @Test
    fun editingAField_removesItsMark_andOnlyItsMark() {
        val form = form().apply {
            onOriginalPriceChange("12000")
            applySuggestion(suggestion)
        }

        form.onTitleChange("Pan del día")
        form.onDescriptionChange("Otra descripción")
        assertEquals(setOf(OfferField.CATEGORY, OfferField.DISCOUNTED_PRICE), form.suggestedFields)

        form.onCategorySelected(OfferCategory.PRODUCE)
        form.onDiscountedPriceChange("7000")
        assertTrue(form.suggestedFields.isEmpty())
    }

    @Test
    fun everythingStaysEditable_afterASuggestion() {
        val form = form().apply { applySuggestion(suggestion) }

        form.onTitleChange("Mi título")
        form.onDescriptionChange("Mi descripción")

        assertEquals("Mi título", form.values.title)
        assertEquals("Mi descripción", form.values.description)
    }

    @Test
    fun aSecondSuggestion_replacesTheMarks() {
        val form = form().apply {
            onOriginalPriceChange("12000")
            applySuggestion(suggestion)
            onTitleChange("Editado")
        }

        form.applySuggestion(suggestion.copy(title = "Otro", discountedPrice = null))

        assertEquals("Otro", form.values.title)
        assertEquals(setOf(OfferField.TITLE, OfferField.CATEGORY, OfferField.DESCRIPTION), form.suggestedFields)
    }

    @Test
    fun overlongTexts_areCutToTheEditorsLimits() {
        val form = form()

        form.applySuggestion(suggestion.copy(title = "t".repeat(200), description = "d".repeat(400)))

        assertEquals(OfferEditorFormState.MAX_TITLE_LENGTH, form.values.title.length)
        assertEquals(OfferEditorFormState.MAX_DESCRIPTION_LENGTH, form.values.description.length)
    }

    @Test
    fun anEmptySuggestedTitle_keepsWhatWasTyped() {
        val form = form().apply { onTitleChange("Lo mío") }

        form.applySuggestion(suggestion.copy(title = "  "))

        assertEquals("Lo mío", form.values.title)
        assertTrue(OfferField.TITLE !in form.suggestedFields)
    }

    @Test
    fun theDescription_isOptional_butHasALimit() {
        val form = form()

        form.validate()
        assertNull(form.errorOf(OfferField.DESCRIPTION))

        form.onDescriptionChange("x".repeat(OfferEditorFormState.MAX_DESCRIPTION_LENGTH + 1))
        assertEquals(OfferFieldError.DESCRIPTION_TOO_LONG, form.errorOf(OfferField.DESCRIPTION))
    }

    @Test
    fun theDescription_goesIntoTheSavedOffer_trimmed() {
        val form = OfferEditorFormState.newOffer().apply {
            onTitleChange("Pan del día")
            onDescriptionChange("  Pan de esta mañana.  ")
            onCategorySelected(OfferCategory.BAKERY)
            onPhotoSelected(com.zerowasteeats.app.data.merchant.OfferPhoto.Bundled.CROISSANT)
            onOriginalPriceChange("10000")
            onDiscountedPriceChange("5000")
            onPickupStartSelected(20 * 60)
            onPickupEndSelected(21 * 60)
        }

        assertNull(form.validate(nowMinutes = 8 * 60))
        assertEquals(
            "Pan de esta mañana.",
            form.toDraft(java.time.LocalDate.of(2026, 11, 7), java.time.ZoneOffset.UTC).description
        )
    }

    @Test
    fun theSaver_keepsTheDescription_andTheMarks() {
        val form = form().apply {
            onOriginalPriceChange("12000")
            applySuggestion(suggestion)
            onTitleChange("Editado")
        }

        val saved = with(OfferEditorFormState.Saver) { SaverScope { true }.save(form) }!!
        val restored = OfferEditorFormState.Saver.restore(saved)!!

        assertEquals(form.values, restored.values)
        assertEquals(form.suggestedFields, restored.suggestedFields)
    }

    // --- The card's state after a request ---

    @Test
    fun aSuccess_tellsWhetherItWasAnExample_andCarriesTheWarnings() {
        val state = suggestionStateAfter(
            SuggestionResult.Success(
                suggestion.copy(isExample = true, warnings = listOf(" Foto borrosa. ", ""), discountedPrice = null)
            )
        )

        assertEquals(
            SuggestionUiState.Applied(
                isExample = true,
                warnings = listOf("Foto borrosa."),
                discountPercent = 50,
                priceFilled = false
            ),
            state
        )
    }

    @Test
    fun aFailure_keepsItsReason() {
        SuggestionError.entries.forEach { error ->
            assertEquals(
                SuggestionUiState.Failed(error),
                suggestionStateAfter(SuggestionResult.Failure(error))
            )
        }
    }

    @Test
    fun thePriceSentWithThePhoto_isTheOriginalPriceTyped() {
        assertNull(originalPriceForSuggestion(form()))
        assertNull(originalPriceForSuggestion(form().apply { onOriginalPriceChange("0") }))
        assertEquals(12000L, originalPriceForSuggestion(form().apply { onOriginalPriceChange("12.000") }))
    }
}
