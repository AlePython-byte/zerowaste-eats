package com.zerowasteeats.app.data.catalog

import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import retrofit2.HttpException

/**
 * The offers customers browse: the backend in live mode ([RemoteOfferCatalogRepository]), sample
 * offers in demo mode ([FakeOfferCatalogRepository]). One instance serves every screen of a
 * signed-in customer, so an offer looks the same in the lists and in its detail.
 */
interface OfferCatalogRepository {
    /** The offers around the person, nearest first, as the last load returned them. */
    val nearby: StateFlow<List<CatalogOffer>>

    val loadState: StateFlow<CatalogLoadState>

    /** Loads the offers around [location] again; on failure the ones already loaded stay. */
    suspend fun refresh(location: GeoPoint)

    /**
     * Loads the offers if nothing was loaded yet, the last try failed, or they are older than a
     * minute: showing a screen again is a natural moment to catch up.
     */
    suspend fun refreshIfStale(location: suspend () -> GeoPoint)

    /** What is known of offer [id] right now, from a list or from an earlier [lookUp]. */
    fun observe(id: String): Flow<CatalogOffer?>

    /** Asks for offer [id] again; [location] only adds the distance. */
    suspend fun lookUp(id: String, location: GeoPoint?): OfferLookup
}

/**
 * What both implementations share: the offers they know, by id, so a lookup and a list update
 * the same offer, and the bookkeeping of loading. Subclasses only fetch.
 */
abstract class CachedOfferCatalog(private val clock: Clock) : OfferCatalogRepository {

    private val nearbyState = MutableStateFlow<List<CatalogOffer>>(emptyList())
    private val known = MutableStateFlow<Map<String, CatalogOffer>>(emptyMap())
    private val load = MutableStateFlow<CatalogLoadState>(CatalogLoadState.Idle)

    @Volatile
    private var loadedAt: Instant? = null

    override val nearby: StateFlow<List<CatalogOffer>> = nearbyState
    override val loadState: StateFlow<CatalogLoadState> = load

    /** The offers on sale around [location], nearest first. */
    protected abstract suspend fun fetchNearby(location: GeoPoint): List<CatalogOffer>

    /** Offer [id], or null when customers can no longer reserve it. */
    protected abstract suspend fun fetchOffer(id: String, location: GeoPoint?): CatalogOffer?

    override suspend fun refresh(location: GeoPoint) {
        load.value = CatalogLoadState.Loading
        load.value = try {
            val offers = fetchNearby(location)
            known.update { it + offers.associateBy(CatalogOffer::id) }
            nearbyState.value = offers
            loadedAt = clock.instant()
            CatalogLoadState.Loaded
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CatalogLoadState.Failed(errorOf(e))
        }
    }

    override suspend fun refreshIfStale(location: suspend () -> GeoPoint) {
        val state = load.value
        if (state == CatalogLoadState.Loading) return
        val last = loadedAt
        val stale = state != CatalogLoadState.Loaded || last == null ||
            Duration.between(last, clock.instant()) > MAX_AGE
        if (stale) refresh(location())
    }

    override fun observe(id: String): Flow<CatalogOffer?> =
        known.map { it[id] }.distinctUntilChanged()

    override suspend fun lookUp(id: String, location: GeoPoint?): OfferLookup = try {
        val offer = fetchOffer(id, location)
        if (offer == null) {
            // The server knows better: it no longer belongs in the list
            nearbyState.update { offers -> offers.filterNot { it.id == id } }
            known.update { it - id }
            OfferLookup.NotAvailable
        } else {
            val previous = known.value[id]
            // Without a location the server sends no distance; keep the one the list had
            val merged = offer.copy(distanceKm = offer.distanceKm ?: previous?.distanceKm)
            put(merged)
            OfferLookup.Found(merged)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        OfferLookup.Failed(errorOf(e))
    }

    /** Stores [offer] and replaces it in the list, where it keeps its place. */
    protected fun put(offer: CatalogOffer) {
        known.update { it + (offer.id to offer) }
        nearbyState.update { offers -> offers.map { if (it.id == offer.id) offer else it } }
    }

    private fun errorOf(e: Exception): CatalogError = when (e) {
        is IOException -> CatalogError.NETWORK
        is HttpException -> when {
            e.code() == 401 -> CatalogError.SESSION
            e.code() == 429 || e.code() >= 500 -> CatalogError.NETWORK
            else -> CatalogError.UNEXPECTED
        }
        else -> CatalogError.UNEXPECTED
    }

    private companion object {
        val MAX_AGE: Duration = Duration.ofMinutes(1)
    }
}
