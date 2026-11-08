package com.zerowasteeats.app.data.merchant

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

/** The backend's profile of the signed-in business. The access token travels in the client. */
interface MerchantProfileApi {
    @GET("merchant/profile")
    suspend fun get(): BusinessProfileResponse

    @PATCH("merchant/profile")
    suspend fun update(@Body body: BusinessProfileBody): BusinessProfileResponse
}

@Serializable
data class BusinessProfileResponse(
    val businessName: String,
    val description: String? = null,
    val isVerified: Boolean = false,
    /** Null for a business that has no establishment yet. */
    val establishment: EstablishmentResponse? = null
)

@Serializable
data class EstablishmentResponse(
    val name: String,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
)

/** Only the fields that change are sent; absent ones stay as they are on the server. */
@Serializable
data class BusinessProfileBody(
    val businessName: String? = null,
    val description: String? = null,
    val establishment: EstablishmentBody? = null
)

@Serializable
data class EstablishmentBody(
    val address: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * The business's profile from the backend. Until it loads, [profile] holds what the session
 * already knows (the name and city given at sign up), so the screen is never blank.
 */
class RemoteMerchantProfileRepository(
    private val api: MerchantProfileApi,
    initialProfile: MerchantProfile
) : MerchantProfileRepository {

    private val state = MutableStateFlow(initialProfile)
    private val load = MutableStateFlow<LoadState>(LoadState.Idle)

    override val profile: StateFlow<MerchantProfile> = state
    override val loadState: StateFlow<LoadState> = load

    override suspend fun refresh() {
        load.value = LoadState.Loading
        load.value = try {
            state.value = api.get().toProfile()
            LoadState.Loaded
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadState.Failed(errorOf(e))
        }
    }

    override suspend fun update(changes: MerchantProfileChanges): MerchantProfile {
        val place = EstablishmentBody(
            address = changes.address?.trim(),
            city = changes.city?.trim(),
            latitude = changes.latitude,
            longitude = changes.longitude
        )
        val body = BusinessProfileBody(
            businessName = changes.businessName?.trim(),
            description = changes.description?.trim(),
            establishment = place.takeIf { it != EstablishmentBody() }
        )
        val updated = try {
            api.update(body).toProfile()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw MerchantDataException(errorOf(e))
        }
        state.value = updated
        return updated
    }

    private fun errorOf(e: Exception): MerchantDataError = when (e) {
        is IOException -> MerchantDataError.NETWORK
        is HttpException -> when {
            e.code() == 401 -> MerchantDataError.SESSION
            e.code() == 429 || e.code() >= 500 -> MerchantDataError.NETWORK
            else -> MerchantDataError.UNEXPECTED
        }
        else -> MerchantDataError.UNEXPECTED
    }

    private fun BusinessProfileResponse.toProfile() = MerchantProfile(
        businessName = businessName,
        description = description.orEmpty(),
        address = establishment?.address.orEmpty(),
        city = establishment?.city.orEmpty(),
        isVerified = isVerified,
        latitude = establishment?.latitude,
        longitude = establishment?.longitude
    )
}
