package com.zerowasteeats.app.data.merchant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MerchantProfile(
    val businessName: String,
    val description: String,
    val address: String,
    val city: String,
    /** False until the business is reviewed. It does not block anything yet. */
    val isVerified: Boolean,
    /** Where the establishment is, when known; for the map pin of the business. */
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** What a business can change about itself; null leaves a field as it is. */
data class MerchantProfileChanges(
    val businessName: String? = null,
    val description: String? = null,
    val address: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * The signed-in business: the backend in live mode ([RemoteMerchantProfileRepository]),
 * memory in demo mode. Writes that cannot reach the server throw [MerchantDataException].
 */
interface MerchantProfileRepository {
    val profile: StateFlow<MerchantProfile>

    val loadState: StateFlow<LoadState>

    /** Loads the profile again; on failure the one already there stays. */
    suspend fun refresh()

    suspend fun update(changes: MerchantProfileChanges): MerchantProfile
}

/** Loads the profile if nothing asked for it yet, or if the last try failed. */
suspend fun MerchantProfileRepository.loadIfNeeded() {
    val state = loadState.value
    if (state == LoadState.Idle || state is LoadState.Failed) refresh()
}

class FakeMerchantProfileRepository(initialProfile: MerchantProfile) : MerchantProfileRepository {

    private val state = MutableStateFlow(initialProfile)

    override val profile: StateFlow<MerchantProfile> = state

    override val loadState: StateFlow<LoadState> = MutableStateFlow(LoadState.Loaded)

    override suspend fun refresh() = Unit

    override suspend fun update(changes: MerchantProfileChanges): MerchantProfile {
        state.value = state.value.let { current ->
            current.copy(
                businessName = changes.businessName ?: current.businessName,
                description = changes.description ?: current.description,
                address = changes.address ?: current.address,
                city = changes.city ?: current.city,
                latitude = changes.latitude ?: current.latitude,
                longitude = changes.longitude ?: current.longitude
            )
        }
        return state.value
    }
}
