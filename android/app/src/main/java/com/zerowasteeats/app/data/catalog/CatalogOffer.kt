package com.zerowasteeats.app.data.catalog

import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A place on the map, in degrees. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** The centre of Pasto: where offers are looked for when the person's location is unknown. */
val PASTO_CENTER = GeoPoint(1.2136, -77.2811)

private const val EARTH_RADIUS_KM = 6371.0

/** Haversine distance in kilometres, as the server measures it. */
internal fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLng = Math.toRadians(to.longitude - from.longitude)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
        sin(dLng / 2).pow(2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
}

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
    val business: CatalogBusiness,
    /**
     * False once the server said customers can no longer reserve it (sold out, paused, over).
     * It stays on screen, marked, until the next load leaves it out.
     */
    val isAvailable: Boolean = true
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

/** What came of saving or removing a favorite. */
sealed interface FavoriteResult {
    data object Saved : FavoriteResult

    /** The offer can no longer be saved: it is marked as not available. */
    data object NotAvailable : FavoriteResult

    /** The change did not reach the server; the heart is back as it was. */
    data class Failed(val error: CatalogError) : FavoriteResult
}
