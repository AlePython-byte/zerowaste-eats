package com.zerowasteeats.app.data.merchant

import com.zerowasteeats.app.presentation.model.OfferCategory
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** The backend's offers of the signed-in business. The access token travels in the client. */
interface MerchantOffersApi {
    @GET("merchant/offers")
    suspend fun list(@Query("status") status: String? = null): List<MerchantOfferResponse>

    @POST("merchant/offers")
    suspend fun create(@Body body: MerchantOfferBody): MerchantOfferResponse

    @PATCH("merchant/offers/{id}")
    suspend fun update(@Path("id") id: String, @Body body: MerchantOfferBody): MerchantOfferResponse

    @POST("merchant/offers/{id}/pause")
    suspend fun pause(@Path("id") id: String): MerchantOfferResponse

    @POST("merchant/offers/{id}/resume")
    suspend fun resume(@Path("id") id: String): MerchantOfferResponse

    @DELETE("merchant/offers/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}

@Serializable
data class MerchantOfferResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: String,
    val imageUrl: String? = null,
    val originalPrice: Long,
    val discountedPrice: Long,
    val quantityTotal: Int,
    val quantityAvailable: Int,
    val pickupStart: String,
    val pickupEnd: String,
    /** AVAILABLE, SOLD_OUT, PAUSED, EXPIRED (worked out by the server) or CANCELLED. */
    val status: String
)

/** What the app sends to create or edit an offer; the server keeps reserved units on its own. */
@Serializable
data class MerchantOfferBody(
    val title: String,
    /** Empty clears it. */
    val description: String,
    val category: String,
    val imageUrl: String? = null,
    val originalPrice: Long,
    val discountedPrice: Long,
    val quantityTotal: Int,
    val pickupStart: String,
    val pickupEnd: String
)

/**
 * The business's offers from the backend. They are loaded with [refresh] and kept in [offers],
 * which every write updates with the server's answer, so screens see what the server holds.
 * Writes that fail throw [MerchantDataException]; a change the server refuses because the offer
 * is no longer in that status answers false, and a missing offer answers null.
 */
class RemoteMerchantOfferRepository(private val api: MerchantOffersApi) : MerchantOfferRepository {

    private val state = MutableStateFlow<List<MerchantOffer>>(emptyList())
    private val load = MutableStateFlow<LoadState>(LoadState.Idle)

    override val offers: StateFlow<List<MerchantOffer>> = state
    override val loadState: StateFlow<LoadState> = load

    override suspend fun refresh() {
        load.value = LoadState.Loading
        load.value = try {
            state.value = api.list().mapNotNull { it.toOffer() }
            LoadState.Loaded
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadState.Failed(errorOf(e))
        }
    }

    override suspend fun createOffer(draft: MerchantOfferDraft): MerchantOffer {
        val created = write { api.create(draft.toBody(reserved = 0)) }.toOffer()
            ?: throw MerchantDataException(MerchantDataError.UNEXPECTED)
        state.update { listOf(created) + it }
        return created
    }

    override suspend fun updateOffer(id: String, draft: MerchantOfferDraft): MerchantOffer? {
        val reserved = state.value.firstOrNull { it.id == id }?.quantityReserved ?: 0
        val edited = writeOrNullOn(404) { api.update(id, draft.toBody(reserved)) } ?: run {
            state.update { offers -> offers.filterNot { it.id == id } }
            return null
        }
        return replace(edited)
    }

    override suspend fun pauseOffer(id: String): Boolean = changeStatus(id) { api.pause(id) }

    override suspend fun resumeOffer(id: String): Boolean = changeStatus(id) { api.resume(id) }

    override suspend fun deleteOffer(id: String): Boolean {
        val response = write { api.delete(id) }
        return when {
            response.isSuccessful || response.code() == 404 -> {
                state.update { offers -> offers.filterNot { it.id == id } }
                true
            }
            // It has reservations: it stays, and can be paused instead
            response.code() == 409 -> false
            else -> throw MerchantDataException(statusError(response.code()))
        }
    }

    /** A 409 means the offer is no longer in a status that allows it (it expired, say). */
    private suspend fun changeStatus(id: String, call: suspend () -> MerchantOfferResponse): Boolean {
        val changed = writeOrNullOn(409, 404) { call() } ?: run {
            // The server knows better: show what it holds now
            refresh()
            return false
        }
        replace(changed)
        return true
    }

    private fun replace(response: MerchantOfferResponse): MerchantOffer? {
        val offer = response.toOffer()
        state.update { offers ->
            if (offer == null) offers.filterNot { it.id == response.id }
            else offers.map { if (it.id == offer.id) offer else it }
        }
        return offer
    }

    private suspend fun <T> write(call: suspend () -> T): T = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw MerchantDataException(errorOf(e))
    }

    private suspend fun <T> writeOrNullOn(vararg codes: Int, call: suspend () -> T): T? = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        if (e.code() in codes) null else throw MerchantDataException(statusError(e.code()))
    } catch (e: Exception) {
        throw MerchantDataException(errorOf(e))
    }

    private fun errorOf(e: Exception): MerchantDataError = when (e) {
        is IOException -> MerchantDataError.NETWORK
        is HttpException -> statusError(e.code())
        else -> MerchantDataError.UNEXPECTED
    }

    private fun statusError(code: Int): MerchantDataError = when {
        code == 401 -> MerchantDataError.SESSION
        code == 429 || code >= 500 -> MerchantDataError.NETWORK
        else -> MerchantDataError.UNEXPECTED
    }

    private fun MerchantOfferDraft.toBody(reserved: Int) = MerchantOfferBody(
        title = title.trim(),
        description = description.trim(),
        category = category.name,
        // A demo photo has no address the server could serve
        imageUrl = (photo as? OfferPhoto.Remote)?.url,
        originalPrice = originalPrice,
        discountedPrice = discountedPrice,
        // The editor shows the units still free; reserved ones stay reserved
        quantityTotal = quantity + reserved,
        pickupStart = pickupStart.toString(),
        pickupEnd = pickupEnd.toString()
    )

    companion object {
        /** Null for an offer the app does not show (cancelled) or cannot read. */
        internal fun MerchantOfferResponse.toOffer(): MerchantOffer? {
            val appStatus = when (status) {
                "AVAILABLE" -> MerchantOfferStatus.ACTIVE
                "PAUSED" -> MerchantOfferStatus.PAUSED
                "SOLD_OUT" -> MerchantOfferStatus.SOLD_OUT
                "EXPIRED" -> MerchantOfferStatus.EXPIRED
                else -> return null
            }
            return try {
                MerchantOffer(
                    id = id,
                    title = title,
                    category = categoryOf(category),
                    originalPrice = originalPrice,
                    discountedPrice = discountedPrice,
                    quantity = quantityAvailable,
                    pickupStart = Instant.parse(pickupStart),
                    pickupEnd = Instant.parse(pickupEnd),
                    status = appStatus,
                    photo = imageUrl?.let(OfferPhoto::Remote),
                    description = description.orEmpty(),
                    quantityReserved = (quantityTotal - quantityAvailable).coerceAtLeast(0)
                )
            } catch (_: DateTimeParseException) {
                null
            } catch (_: SerializationException) {
                null
            }
        }

        /**
         * The app has four categories; the server also knows dairy and "other", which the app
         * never sends. Those show as prepared food rather than hiding the offer.
         */
        private fun categoryOf(name: String): OfferCategory =
            OfferCategory.entries.firstOrNull { it.name == name && it != OfferCategory.ALL }
                ?: OfferCategory.PREPARED_FOOD
    }
}
