package com.zerowasteeats.app.presentation.screens.merchant.offers

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zerowasteeats.app.presentation.screens.merchant.editor.OfferSaveResult

/**
 * A one-shot message from the offer editor to the offer list: the editor posts what it just
 * saved, and the list shows it once in a snackbar through [consume]. Like ExploreViewRequest,
 * it lives above the nav graph, so it outlives the editor but not the business space.
 */
@Stable
class OfferSavedNotice {
    var pending by mutableStateOf<OfferSaveResult?>(null)
        private set

    fun post(result: OfferSaveResult) {
        pending = result
    }

    /** Returns the pending result, if any, and clears it. */
    fun consume(): OfferSaveResult? = pending.also { pending = null }
}
