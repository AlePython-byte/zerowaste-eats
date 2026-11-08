package com.zerowasteeats.app.data.catalog

import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Instant

/** A place on the map, in degrees. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** The centre of Pasto: where offers are looked for when the person's location is unknown. */
val PASTO_CENTER = GeoPoint(1.2136, -77.2811)

/** The business behind an offer, and where to pick it up. */
data class CatalogBusiness(
    val name: String,
    val isVerified: Boolean,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
)

/** An offer a customer can browse and reserve. */
data class CatalogOffer(
    val id: String,
    val title: String,
    /** Empty when the business wrote none. */
    val description: String,
    val category: OfferCategory,
    val photo: OfferPhoto?,
    val originalPrice: Long,
    val discountedPrice: Long,
    val discountPercent: Int,
    val quantityAvailable: Int,
    val pickupStart: Instant,
    val pickupEnd: Instant,
    /** From the person to the business, as the server measured it; null when unknown. */
    val distanceKm: Double?,
    val isFavorite: Boolean,
    val business: CatalogBusiness
)

/** Why the offers could not be read. */
enum class CatalogError {
    NETWORK,

    /** The server no longer accepts the session: signing in again fixes it. */
    SESSION,
    UNEXPECTED
}

/** Where loading the nearby offers stands. */
sealed interface CatalogLoadState {
    /** Nothing asked yet. */
    data object Idle : CatalogLoadState
    data object Loading : CatalogLoadState
    data object Loaded : CatalogLoadState

    /** The last load failed; whatever was loaded before is still there. */
    data class Failed(val error: CatalogError) : CatalogLoadState
}

/** What came of asking for one offer. */
sealed interface OfferLookup {
    data class Found(val offer: CatalogOffer) : OfferLookup

    /** Sold out, paused, over or removed: customers can no longer reserve it. */
    data object NotAvailable : OfferLookup

    data class Failed(val error: CatalogError) : OfferLookup
}
