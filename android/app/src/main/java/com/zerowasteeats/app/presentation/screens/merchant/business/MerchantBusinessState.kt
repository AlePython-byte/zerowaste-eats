package com.zerowasteeats.app.presentation.screens.merchant.business

import com.zerowasteeats.app.data.merchant.LoadState
import com.zerowasteeats.app.data.merchant.MerchantDataError
import com.zerowasteeats.app.data.merchant.MerchantProfile

/**
 * What the business tab shows. A detail the person has not filled in is null, so the screen
 * can say so in a muted tone instead of leaving a blank.
 */
data class MerchantBusinessState(
    val businessName: String,
    val isVerified: Boolean,
    val description: String?,
    val address: String?,
    val city: String?,
    /** Why the profile could not be loaded from the server; null when it could, or loads. */
    val loadError: MerchantDataError? = null
) {
    companion object {
        fun from(profile: MerchantProfile, load: LoadState = LoadState.Loaded) = MerchantBusinessState(
            businessName = profile.businessName,
            isVerified = profile.isVerified,
            description = profile.description.filledOrNull(),
            address = profile.address.filledOrNull(),
            city = profile.city.filledOrNull(),
            loadError = (load as? LoadState.Failed)?.error
        )

        private fun String.filledOrNull(): String? = trim().takeIf { it.isNotEmpty() }
    }
}
