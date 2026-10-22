package com.zerowasteeats.app.presentation.screens.explore

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A one-shot request, made from outside Explore, to open it in a given view mode.
 *
 * It lives above the nav graph so it survives until Explore is composed, even if Explore
 * has never been opened. Explore applies it once through [consume], which clears it, so
 * it never fires again when the user comes back to Explore later.
 */
@Stable
class ExploreViewRequest {
    var pending by mutableStateOf<ExploreViewMode?>(null)
        private set

    fun request(mode: ExploreViewMode) {
        pending = mode
    }

    /** Returns the pending mode, if any, and clears it. */
    fun consume(): ExploreViewMode? = pending.also { pending = null }
}
