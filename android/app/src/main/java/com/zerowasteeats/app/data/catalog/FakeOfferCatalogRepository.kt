package com.zerowasteeats.app.data.catalog

import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay

/**
 * Sample offers for demo mode, kept in memory: nothing leaves the device. Every call waits
 * [latencyMillis] to look like a real request, and the distance is worked out here the way the
 * server does it.
 */
class FakeOfferCatalogRepository(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS
) : CachedOfferCatalog(clock) {

    private val offers: List<CatalogOffer> = demoOffers(clock)

    // The demo customer's saved offers, while the app runs
    private val favorites: MutableSet<String> =
        offers.filter { it.isFavorite }.mapTo(mutableSetOf()) { it.id }

    override suspend fun fetchNearby(location: GeoPoint): List<CatalogOffer> {
        delay(latencyMillis)
        return offers.map { it.measuredFrom(location).withSavedState() }.sortedBy { it.distanceKm }
    }

    override suspend fun fetchOffer(id: String, location: GeoPoint?): CatalogOffer? {
        delay(latencyMillis)
        val offer = offers.firstOrNull { it.id == id }?.withSavedState() ?: return null
        return if (location == null) offer else offer.measuredFrom(location)
    }

    override suspend fun sendFavorite(id: String, favorite: Boolean): Boolean {
        delay(latencyMillis)
        synchronized(favorites) {
            if (favorite) {
                if (offers.none { it.id == id }) return false
                favorites += id
            } else {
                favorites -= id
            }
        }
        return true
    }

    private fun CatalogOffer.withSavedState() =
        copy(isFavorite = synchronized(favorites) { id in favorites })

    private fun CatalogOffer.measuredFrom(from: GeoPoint) = copy(
        distanceKm = roundToTenth(
            distanceKm(from, GeoPoint(business.latitude, business.longitude))
        )
    )

    companion object {
        const val DEFAULT_LATENCY_MILLIS = 600L

        private const val EARTH_RADIUS_KM = 6371.0

        /** Haversine distance, as the server measures it. */
        internal fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
            val dLat = Math.toRadians(to.latitude - from.latitude)
            val dLng = Math.toRadians(to.longitude - from.longitude)
            val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
                sin(dLng / 2).pow(2)
            return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
        }

        private fun roundToTenth(km: Double): Double = (km * 10).roundToLong() / 10.0

        /** [daysFromToday] at [hour]:[minute] in the clock's time zone. */
        private fun at(clock: Clock, daysFromToday: Long, hour: Int, minute: Int = 0): Instant =
            clock.instant().atZone(clock.zone).toLocalDate()
                .plusDays(daysFromToday)
                .atTime(LocalTime.of(hour, minute))
                .atZone(clock.zone)
                .toInstant()

        /**
         * The demo offers. Businesses and addresses are made up, and the coordinates are only
         * placed around Pasto to fill the map: they are not real establishments.
         */
        internal fun demoOffers(clock: Clock): List<CatalogOffer> = listOf(
            CatalogOffer(
                id = "demo-catalog-1",
                title = "Croissants del día",
                description = "Croissants de mantequilla horneados esta mañana. Caliéntalos unos minutos para que queden crujientes.",
                category = OfferCategory.BAKERY,
                photo = OfferPhoto.Bundled.CROISSANT,
                originalPrice = 15_000,
                discountedPrice = 8_900,
                discountPercent = 41,
                quantityAvailable = 2,
                pickupStart = at(clock, 0, 18),
                pickupEnd = at(clock, 0, 19, 30),
                distanceKm = null,
                isFavorite = false,
                business = CatalogBusiness("Panadería Artesanal", true, "Cra. 27 #18-45", "Pasto", 1.2150, -77.2800)
            ),
            CatalogOffer(
                id = "demo-catalog-2",
                title = "Bowl saludable de salmón",
                description = "Bowl de arroz, salmón y verduras preparado hoy. Mantenlo refrigerado y consúmelo esta noche.",
                category = OfferCategory.PREPARED_FOOD,
                photo = OfferPhoto.Bundled.SALMON_BOWL,
                originalPrice = 32_000,
                discountedPrice = 19_000,
                discountPercent = 41,
                quantityAvailable = 1,
                pickupStart = at(clock, 0, 20),
                pickupEnd = at(clock, 0, 21),
                distanceKm = null,
                isFavorite = true,
                business = CatalogBusiness("Green Vida", false, "Calle 18 #25-30", "Pasto", 1.2120, -77.2830)
            ),
            CatalogOffer(
                id = "demo-catalog-3",
                title = "Caja de postres surtidos",
                description = "Selección de postres de la vitrina de hoy: tortas, brownies y galletas. Consúmelos hoy mismo.",
                category = OfferCategory.DESSERTS,
                photo = OfferPhoto.Bundled.DESSERTS,
                originalPrice = 20_000,
                discountedPrice = 9_900,
                discountPercent = 51,
                quantityAvailable = 3,
                pickupStart = at(clock, 1, 10),
                pickupEnd = at(clock, 1, 12),
                distanceKm = null,
                isFavorite = false,
                business = CatalogBusiness("Dulce Rincón", true, "Cra. 24 #15-12", "Pasto", 1.2170, -77.2780)
            ),
            CatalogOffer(
                id = "demo-catalog-4",
                title = "Hamburguesa artesanal con papas",
                description = "Hamburguesa con pan artesanal y papas, preparada al momento. Mejor recién recogida.",
                category = OfferCategory.PREPARED_FOOD,
                photo = OfferPhoto.Bundled.BURGER,
                originalPrice = 24_000,
                discountedPrice = 14_400,
                discountPercent = 40,
                quantityAvailable = 1,
                pickupStart = at(clock, 0, 19),
                pickupEnd = at(clock, 0, 20, 30),
                distanceKm = null,
                isFavorite = false,
                business = CatalogBusiness("Burger Taller", false, "Calle 20 #29-40", "Pasto", 1.2105, -77.2790)
            ),
            CatalogOffer(
                id = "demo-catalog-5",
                title = "Canasta de verduras frescas",
                description = "Verduras frescas con pequeños detalles de apariencia, perfectas para sopas, jugos o salteados.",
                category = OfferCategory.PRODUCE,
                photo = OfferPhoto.Bundled.VEGETABLES,
                originalPrice = 15_000,
                discountedPrice = 7_500,
                discountPercent = 50,
                quantityAvailable = 4,
                pickupStart = at(clock, 1, 8),
                pickupEnd = at(clock, 1, 10),
                distanceKm = null,
                isFavorite = false,
                business = CatalogBusiness("Mercado Verde", true, "Cra. 30 #12-05", "Pasto", 1.2185, -77.2840)
            )
        )
    }
}
