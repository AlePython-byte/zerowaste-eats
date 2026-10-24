package com.zerowasteeats.app.data.merchant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MerchantProfile(
    val businessName: String,
    val description: String,
    val address: String,
    val city: String,
    /** False until the business is reviewed. It does not block anything yet. */
    val isVerified: Boolean
)

interface MerchantProfileRepository {
    val profile: StateFlow<MerchantProfile>
}

class FakeMerchantProfileRepository(initialProfile: MerchantProfile) : MerchantProfileRepository {

    override val profile: StateFlow<MerchantProfile> = MutableStateFlow(initialProfile)
}
