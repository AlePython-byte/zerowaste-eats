package com.zerowasteeats.app.presentation.screens.merchant.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantOfferDraft
import com.zerowasteeats.app.data.merchant.MerchantOfferRepository
import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The editor's fields, in the order they appear on screen. */
enum class OfferField {
    TITLE, CATEGORY, PHOTO, ORIGINAL_PRICE, DISCOUNTED_PRICE, QUANTITY, PICKUP_START, PICKUP_END
}

enum class OfferFieldError {
    REQUIRED,
    TITLE_TOO_SHORT,
    TITLE_TOO_LONG,
    PRICE_NOT_POSITIVE,
    DISCOUNT_NOT_LOWER,
    QUANTITY_OUT_OF_RANGE,
    END_NOT_AFTER_START,
    END_IN_PAST
}

/**
 * What the form holds, as typed. Prices and quantity are digit strings so a half-typed value
 * survives as it is; pickup times are minutes after midnight.
 */
data class OfferEditorValues(
    val title: String = "",
    val category: OfferCategory? = null,
    val photo: OfferPhoto? = null,
    val originalPrice: String = "",
    val discountedPrice: String = "",
    val quantity: String = OfferEditorFormState.MIN_QUANTITY.toString(),
    val pickupStartMinutes: Int? = null,
    val pickupEndMinutes: Int? = null
)

/**
 * The offer editor's form. Errors work as in the auth forms: a field shows its error only
 * after it was left (or, for a choice, after something was chosen) or the form was sent, and
 * then follows each change. [initialValues] is what the offer looked like when the editor
 * opened, to tell whether anything changed.
 */
class OfferEditorFormState(
    val initialValues: OfferEditorValues,
    values: OfferEditorValues = initialValues,
    shownFields: Set<OfferField> = emptySet(),
    endMustBeAfterMinutes: Int? = null
) {
    var values by mutableStateOf(values)
        private set

    private val shown = mutableStateListOf<OfferField>().apply { addAll(shownFields) }

    // The time of day of the last save attempt: the pickup is today, so it cannot end before it
    private var endMustBeAfterMinutes by mutableStateOf(endMustBeAfterMinutes)

    val hasUnsavedChanges: Boolean get() = values != initialValues

    /** Whole percent off, rounded down, once both prices make sense; null before that. */
    val discountPercent: Int?
        get() {
            val original = values.originalPrice.toLongOrNull() ?: return null
            val discounted = values.discountedPrice.toLongOrNull() ?: return null
            if (original <= 0 || discounted <= 0 || discounted >= original) return null
            return ((original - discounted) * 100 / original).toInt()
        }

    fun errorOf(field: OfferField): OfferFieldError? =
        if (field in shown) validate(field) else null

    fun onTitleChange(value: String) = update { copy(title = value) }

    fun onCategorySelected(value: OfferCategory) {
        require(value != OfferCategory.ALL) { "ALL is a filter, not a category" }
        update { copy(category = value) }
        show(OfferField.CATEGORY)
    }

    fun onPhotoSelected(value: OfferPhoto) {
        update { copy(photo = value) }
        show(OfferField.PHOTO)
    }

    fun onOriginalPriceChange(raw: String) = update { copy(originalPrice = priceDigits(raw)) }

    fun onDiscountedPriceChange(raw: String) = update { copy(discountedPrice = priceDigits(raw)) }

    fun onQuantityChange(raw: String) =
        update { copy(quantity = raw.filter { it.isDigit() }.take(MAX_QUANTITY_DIGITS)) }

    fun incrementQuantity() = stepQuantity(+1)

    fun decrementQuantity() = stepQuantity(-1)

    fun onPickupStartSelected(minutes: Int) {
        update { copy(pickupStartMinutes = minutes) }
        show(OfferField.PICKUP_START)
        // The end is checked against the start, so its error may have just appeared or gone
        if (values.pickupEndMinutes != null) show(OfferField.PICKUP_END)
    }

    fun onPickupEndSelected(minutes: Int) {
        update { copy(pickupEndMinutes = minutes) }
        show(OfferField.PICKUP_END)
    }

    /** The person left [field]: from now on it shows its error. */
    fun onBlur(field: OfferField) = show(field)

    /**
     * For the save button: every field shows its error. Returns the first field, in screen
     * order, that is not valid, or null when the offer can be saved. With [nowMinutes], the
     * time of day now, a pickup that has already ended today is an error too.
     */
    fun validate(nowMinutes: Int? = null): OfferField? {
        endMustBeAfterMinutes = nowMinutes
        OfferField.entries.forEach(::show)
        return OfferField.entries.firstOrNull { validate(it) != null }
    }

    /**
     * The offer to save, with the pickup window on [date]. Call only after [validate] returned
     * null; it fails on a form that is not valid.
     */
    fun toDraft(date: LocalDate, zone: ZoneId): MerchantOfferDraft = with(values) {
        MerchantOfferDraft(
            title = title.trim(),
            category = requireNotNull(category),
            originalPrice = originalPrice.toLong(),
            discountedPrice = discountedPrice.toLong(),
            quantity = quantity.toInt(),
            pickupStart = date.atTime(timeOf(requireNotNull(pickupStartMinutes))).atZone(zone).toInstant(),
            pickupEnd = date.atTime(timeOf(requireNotNull(pickupEndMinutes))).atZone(zone).toInstant(),
            photo = requireNotNull(photo)
        )
    }

    private fun validate(field: OfferField): OfferFieldError? = with(values) {
        when (field) {
            OfferField.TITLE -> {
                val length = title.trim().length
                when {
                    length == 0 -> OfferFieldError.REQUIRED
                    length < MIN_TITLE_LENGTH -> OfferFieldError.TITLE_TOO_SHORT
                    length > MAX_TITLE_LENGTH -> OfferFieldError.TITLE_TOO_LONG
                    else -> null
                }
            }
            OfferField.CATEGORY -> if (category == null) OfferFieldError.REQUIRED else null
            OfferField.PHOTO -> if (photo == null) OfferFieldError.REQUIRED else null
            OfferField.ORIGINAL_PRICE -> priceError(originalPrice)
            OfferField.DISCOUNTED_PRICE -> priceError(discountedPrice) ?: run {
                val original = originalPrice.toLongOrNull()
                // Only compared once the original price is itself valid
                if (original != null && original > 0 && discountedPrice.toLong() >= original) {
                    OfferFieldError.DISCOUNT_NOT_LOWER
                } else {
                    null
                }
            }
            OfferField.QUANTITY -> {
                val amount = quantity.toIntOrNull()
                when {
                    amount == null -> OfferFieldError.REQUIRED
                    amount !in MIN_QUANTITY..MAX_QUANTITY -> OfferFieldError.QUANTITY_OUT_OF_RANGE
                    else -> null
                }
            }
            OfferField.PICKUP_START -> if (pickupStartMinutes == null) OfferFieldError.REQUIRED else null
            OfferField.PICKUP_END -> when {
                pickupEndMinutes == null -> OfferFieldError.REQUIRED
                pickupStartMinutes != null && pickupEndMinutes <= pickupStartMinutes ->
                    OfferFieldError.END_NOT_AFTER_START
                endMustBeAfterMinutes.let { now -> now != null && pickupEndMinutes <= now } ->
                    OfferFieldError.END_IN_PAST
                else -> null
            }
        }
    }

    private fun priceError(digits: String): OfferFieldError? {
        val amount = digits.toLongOrNull() ?: return OfferFieldError.REQUIRED
        return if (amount <= 0) OfferFieldError.PRICE_NOT_POSITIVE else null
    }

    private fun stepQuantity(step: Int) {
        val current = values.quantity.toIntOrNull() ?: 0
        val next = (current + step).coerceIn(MIN_QUANTITY, MAX_QUANTITY)
        update { copy(quantity = next.toString()) }
        show(OfferField.QUANTITY)
    }

    private fun update(change: OfferEditorValues.() -> OfferEditorValues) {
        values = values.change()
    }

    private fun show(field: OfferField) {
        if (field !in shown) shown.add(field)
    }

    companion object {
        const val MIN_TITLE_LENGTH = 3
        const val MAX_TITLE_LENGTH = 80
        const val MIN_QUANTITY = 1
        const val MAX_QUANTITY = 99
        private const val MAX_QUANTITY_DIGITS = 3
        // Up to $999.999.999, far above any offer, and well inside a Long
        private const val MAX_PRICE_DIGITS = 9

        /** Keeps digits only, without leading zeros, so "0" is still there to be flagged. */
        internal fun priceDigits(raw: String): String {
            val digits = raw.filter { it.isDigit() }.take(MAX_PRICE_DIGITS)
            return if (digits.length > 1) digits.trimStart('0').ifEmpty { "0" } else digits
        }

        fun minutesOf(time: LocalTime): Int = time.hour * 60 + time.minute

        fun timeOf(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)

        fun newOffer() = OfferEditorFormState(OfferEditorValues())

        /** The form for editing [offer], with its pickup times read in [zone]. */
        fun fromOffer(offer: MerchantOffer, zone: ZoneId) = OfferEditorFormState(
            OfferEditorValues(
                title = offer.title,
                category = offer.category,
                photo = offer.photo,
                originalPrice = offer.originalPrice.toString(),
                discountedPrice = offer.discountedPrice.toString(),
                quantity = offer.quantity.toString(),
                pickupStartMinutes = minutesOf(offer.pickupStart.atZone(zone).toLocalTime()),
                pickupEndMinutes = minutesOf(offer.pickupEnd.atZone(zone).toLocalTime())
            )
        )

        /** Keeps both the values as opened and as typed, and which errors are on screen. */
        val Saver: Saver<OfferEditorFormState, Any> = Saver(
            save = { form ->
                arrayListOf(
                    form.initialValues.toSavedList(),
                    form.values.toSavedList(),
                    ArrayList(form.shown.map { it.name }),
                    form.endMustBeAfterMinutes ?: NO_TIME
                )
            },
            restore = { saved ->
                val parts = saved as List<*>
                OfferEditorFormState(
                    initialValues = valuesFrom(parts[0] as List<*>),
                    values = valuesFrom(parts[1] as List<*>),
                    shownFields = (parts[2] as List<*>).map { OfferField.valueOf(it as String) }.toSet(),
                    endMustBeAfterMinutes = (parts[3] as Int).takeIf { it != NO_TIME }
                )
            }
        )

        private fun OfferEditorValues.toSavedList(): ArrayList<Any> = arrayListOf(
            title,
            category?.name.orEmpty(),
            photo?.name.orEmpty(),
            originalPrice,
            discountedPrice,
            quantity,
            pickupStartMinutes ?: NO_TIME,
            pickupEndMinutes ?: NO_TIME
        )

        private fun valuesFrom(list: List<*>) = OfferEditorValues(
            title = list[0] as String,
            category = (list[1] as String).takeIf { it.isNotEmpty() }?.let(OfferCategory::valueOf),
            photo = (list[2] as String).takeIf { it.isNotEmpty() }?.let(OfferPhoto::valueOf),
            originalPrice = list[3] as String,
            discountedPrice = list[4] as String,
            quantity = list[5] as String,
            pickupStartMinutes = (list[6] as Int).takeIf { it != NO_TIME },
            pickupEndMinutes = (list[7] as Int).takeIf { it != NO_TIME }
        )

        private const val NO_TIME = -1
    }
}

enum class OfferSaveResult { CREATED, UPDATED, NOT_FOUND }

/** Creates the offer when [offerId] is null, otherwise edits that offer. */
suspend fun saveOffer(
    repository: MerchantOfferRepository,
    offerId: String?,
    draft: MerchantOfferDraft
): OfferSaveResult =
    if (offerId == null) {
        repository.createOffer(draft)
        OfferSaveResult.CREATED
    } else {
        if (repository.updateOffer(offerId, draft) != null) OfferSaveResult.UPDATED
        else OfferSaveResult.NOT_FOUND
    }
