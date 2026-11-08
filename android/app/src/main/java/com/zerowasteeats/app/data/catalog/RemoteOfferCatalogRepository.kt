package com.zerowasteeats.app.data.catalog

import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** The backend's offers for customers. The access token travels in the client. */
interface OfferCatalogApi {
    @GET("offers")
    suspend fun nearby(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radiusKm") radiusKm: Double,
        @Query("limit") limit: Int
    ): List<PublicOfferResponse>

    /** Both coordinates or none: they only add the distance. */
    @GET("offers/{id}")
    suspend fun detail(
        @Path("id") id: String,
        @Query("lat") latitude: Double?,
        @Query("lng") longitude: Double?
    ): PublicOfferResponse

    /** Saving twice is the same as saving once. */
    @PUT("favorites/{offerId}")
    suspend fun addFavorite(@Path("offerId") offerId: String): Response<Unit>

    /** Removing one that is not saved changes nothing. */
    @DELETE("favorites/{offerId}")
    suspend fun removeFavorite(@Path("offerId") offerId: String): Response<Unit>
}

@Serializable
data class PublicOfferResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: String,
    val imageUrl: String? = null,
    val originalPrice: Long,
    val discountedPrice: Long,
    val discountPercent: Int,
    val quantityAvailable: Int,
    val pickupStart: String,
    val pickupEnd: String,
    val distanceKm: Double? = null,
    val isFavorite: Boolean = false,
    val business: PublicBusinessResponse
)

@Serializable
data class PublicBusinessResponse(
    val name: String,
    val isVerified: Boolean = false,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * The offers from the backend: those on sale within [RADIUS_KM] of the person, and one offer by
 * id. Offers the app cannot read are left out rather than failing the whole list.
 */
class RemoteOfferCatalogRepository(
    private val api: OfferCatalogApi,
    clock: Clock = Clock.systemDefaultZone()
) : CachedOfferCatalog(clock) {

    override suspend fun fetchNearby(location: GeoPoint): List<CatalogOffer> =
        api.nearby(location.latitude, location.longitude, RADIUS_KM, LIMIT).mapNotNull { it.toOffer() }

    override suspend fun fetchOffer(id: String, location: GeoPoint?): CatalogOffer? = try {
        api.detail(id, location?.latitude, location?.longitude).toOffer()
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    }

    override suspend fun sendFavorite(id: String, favorite: Boolean): Boolean {
        val response = if (favorite) api.addFavorite(id) else api.removeFavorite(id)
        return when {
            response.isSuccessful -> true
            // Only an offer still on sale can be saved; removing never needs one
            response.code() == 404 -> !favorite
            else -> throw HttpException(response)
        }
    }

    companion object {
        /** Pasto fits well inside it, and nearby towns stay out. */
        const val RADIUS_KM = 10.0

        /** The most the server returns at once. */
        const val LIMIT = 50

        /** Null for an offer the app cannot read. */
        internal fun PublicOfferResponse.toOffer(): CatalogOffer? = try {
            CatalogOffer(
                id = id,
                title = title,
                description = description.orEmpty(),
                category = categoryOf(category),
                photo = imageUrl?.takeIf { it.isNotBlank() }?.let(OfferPhoto::Remote),
                originalPrice = originalPrice,
                discountedPrice = discountedPrice,
                discountPercent = discountPercent,
                quantityAvailable = quantityAvailable,
                pickupStart = Instant.parse(pickupStart),
                pickupEnd = Instant.parse(pickupEnd),
                distanceKm = distanceKm,
                isFavorite = isFavorite,
                business = CatalogBusiness(
                    name = business.name,
                    isVerified = business.isVerified,
                    address = business.address,
                    city = business.city,
                    latitude = business.latitude,
                    longitude = business.longitude
                )
            )
        } catch (_: DateTimeParseException) {
            null
        }

        /**
         * The app has four categories; the server also knows dairy and "other". Those show as
         * prepared food rather than hiding the offer, as on the business side.
         */
        private fun categoryOf(name: String): OfferCategory =
            OfferCategory.entries.firstOrNull { it.name == name && it != OfferCategory.ALL }
                ?: OfferCategory.PREPARED_FOOD
    }
}
