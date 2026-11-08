package com.zerowasteeats.app.presentation.model

import com.zerowasteeats.app.data.catalog.CatalogOffer
import com.zerowasteeats.app.data.merchant.OfferPhoto
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** An offer as the customer screens show it. Holds data, not text: they format it. */
data class OfferUiModel(
    val id: String,
    val title: String,
    val merchantName: String,
    val photo: OfferPhoto?,
    /** In whole pesos. */
    val originalPrice: Long,
    val discountedPrice: Long,
    val discountPercent: Int,
    /** Null when the distance is unknown. */
    val distanceKm: Double?,
    val remainingQuantity: Int,
    val pickupWindow: PickupWindow,
    val isFavorite: Boolean,
    val category: OfferCategory,
    /** What the business wrote about the offer; null when it wrote nothing. */
    val description: String? = null,
    /** Where to pick it up: street address and city, ready to show. */
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** False once the server said it can no longer be reserved: it shows marked. */
    val isAvailable: Boolean = true
)

/** The offer for the screens, with its pickup day relative to today on [clock]. */
fun CatalogOffer.toUiModel(clock: Clock): OfferUiModel = OfferUiModel(
    id = id,
    title = title,
    merchantName = business.name,
    photo = photo,
    originalPrice = originalPrice,
    discountedPrice = discountedPrice,
    discountPercent = discountPercent,
    distanceKm = distanceKm,
    remainingQuantity = quantityAvailable,
    pickupWindow = pickupWindowOf(pickupStart, pickupEnd, clock),
    isFavorite = isFavorite,
    category = category,
    description = description.takeIf { it.isNotBlank() },
    address = addressOf(business.address, business.city),
    latitude = business.latitude,
    longitude = business.longitude,
    isAvailable = isAvailable
)

/** The window in the clock's time zone, named after the day it starts on. */
fun pickupWindowOf(start: Instant, end: Instant, clock: Clock): PickupWindow {
    val zone = clock.zone
    val startTime = start.atZone(zone)
    val today = LocalDate.now(clock)
    val day = when (startTime.toLocalDate()) {
        today -> PickupDay.Today
        today.plusDays(1) -> PickupDay.Tomorrow
        today.minusDays(1) -> PickupDay.Yesterday
        else -> PickupDay.On(startTime.toLocalDate())
    }
    return PickupWindow(day, startTime.toLocalTime(), end.atZone(zone).toLocalTime())
}

/** "Cra. 27 #18-45, Pasto", without the city twice when the address already names it. */
internal fun addressOf(address: String, city: String): String {
    val street = address.trim()
    val town = city.trim()
    return when {
        town.isEmpty() -> street
        street.isEmpty() -> town
        street.contains(town, ignoreCase = true) -> street
        else -> "$street, $town"
    }
}
