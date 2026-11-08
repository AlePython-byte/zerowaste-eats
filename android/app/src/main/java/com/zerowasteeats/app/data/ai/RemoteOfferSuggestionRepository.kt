package com.zerowasteeats.app.data.ai

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** The backend's offer suggestion endpoint. The access token travels in the client. */
interface AiSuggestionApi {
    @Multipart
    @POST("ai/offer-suggestions")
    suspend fun suggest(
        @Part photo: MultipartBody.Part,
        /** Skipped when null. */
        @Part("precioOriginal") originalPrice: RequestBody?
    ): OfferSuggestionResponse
}

@Serializable
data class OfferSuggestionResponse(
    val title: String,
    val category: String,
    val description: String,
    val suggestedDiscountPercent: Int,
    val suggestedDiscountedPrice: Long? = null,
    val confidence: Double,
    val warnings: List<String> = emptyList(),
    /** "mock" or "claude". */
    val source: String
)

/**
 * Asks the backend for a suggestion: the photo as `foto` and, when it is one the server
 * accepts, the original price as `precioOriginal`. Failures come back as [SuggestionResult.Failure];
 * the server's own error text is never shown, the editor has its own in each language.
 */
class RemoteOfferSuggestionRepository(private val api: AiSuggestionApi) : OfferSuggestionRepository {

    override suspend fun suggest(photo: File, originalPrice: Long?): SuggestionResult = try {
        val part = MultipartBody.Part.createFormData(
            PHOTO_FIELD,
            photo.name,
            photo.asRequestBody(JPEG)
        )
        // Outside the server's range the price would be refused along with the photo
        val price = originalPrice
            ?.takeIf { it in MIN_PRICE..MAX_PRICE }
            ?.toString()
            ?.toRequestBody(TEXT)
        SuggestionResult.Success(api.suggest(part, price).toSuggestion())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        SuggestionResult.Failure(errorFor(e.code()))
    } catch (_: IOException) {
        SuggestionResult.Failure(SuggestionError.NETWORK)
    } catch (_: SerializationException) {
        SuggestionResult.Failure(SuggestionError.UNEXPECTED)
    } catch (_: IllegalArgumentException) {
        SuggestionResult.Failure(SuggestionError.UNEXPECTED)
    }

    private fun errorFor(status: Int): SuggestionError = when (status) {
        400, 413, 415 -> SuggestionError.INVALID_PHOTO
        429 -> SuggestionError.LIMIT_REACHED
        502, 503, 504 -> SuggestionError.UNAVAILABLE
        else -> SuggestionError.UNEXPECTED
    }

    private fun OfferSuggestionResponse.toSuggestion() = OfferSuggestion(
        title = title,
        category = categoryFromServer(category),
        description = description,
        discountPercent = suggestedDiscountPercent,
        discountedPrice = suggestedDiscountedPrice,
        confidence = confidence,
        warnings = warnings,
        isExample = source != SOURCE_MODEL
    )

    companion object {
        const val PHOTO_FIELD = "foto"
        private const val SOURCE_MODEL = "claude"
        /** The server's accepted range for the original price, in pesos. */
        const val MIN_PRICE = 500L
        const val MAX_PRICE = 10_000_000L
        private val JPEG = "image/jpeg".toMediaType()
        private val TEXT = "text/plain".toMediaType()
    }
}
