package com.zerowasteeats.app.presentation.screens.feed

import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.CatalogLoadState
import com.zerowasteeats.app.presentation.model.OfferUiModel

/** The offers near the customer, as Home and Explore show them, and how loading went. */
data class OfferFeedState(
    val offers: List<OfferUiModel> = emptyList(),
    val loadState: CatalogLoadState = CatalogLoadState.Idle
) {
    /** Placeholders while the first load runs: there is nothing real to show yet. */
    val showSkeletons: Boolean =
        offers.isEmpty() && (loadState == CatalogLoadState.Loading || loadState == CatalogLoadState.Idle)

    /**
     * The empty-results message only once the offers are known: after a failed first load
     * "no offers" would not be true, and the snackbar explains instead.
     */
    val showEmptyState: Boolean =
        loadState == CatalogLoadState.Loaded ||
            (loadState is CatalogLoadState.Failed && offers.isNotEmpty())

    /** Why the last load failed, while it is the latest news. */
    val loadError: CatalogError? = (loadState as? CatalogLoadState.Failed)?.error
}
