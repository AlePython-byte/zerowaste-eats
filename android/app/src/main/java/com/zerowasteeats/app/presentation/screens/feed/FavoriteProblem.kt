package com.zerowasteeats.app.presentation.screens.feed

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.FavoriteResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** A favorite that could not be changed, said once. */
sealed interface FavoriteProblem {
    /** The offer can no longer be saved; it now shows as not available. */
    data object NotAvailable : FavoriteProblem

    /** The change did not reach the server; trying again sets [favorite] on [offerId]. */
    data class Failed(
        val error: CatalogError,
        val offerId: String,
        val favorite: Boolean
    ) : FavoriteProblem
}

/** What to tell the person after trying to set [favorite] on [offerId]; null when it worked. */
fun FavoriteResult.problemFor(offerId: String, favorite: Boolean): FavoriteProblem? = when (this) {
    FavoriteResult.Saved -> null
    FavoriteResult.NotAvailable -> FavoriteProblem.NotAvailable
    is FavoriteResult.Failed -> FavoriteProblem.Failed(error, offerId, favorite)
}

/**
 * Says why a favorite did not change, replacing whatever snackbar was showing. A failed one can
 * be tried again with [onRetry].
 */
@Composable
fun FavoriteProblemSnackbar(
    problems: Flow<FavoriteProblem>,
    snackbarHostState: SnackbarHostState,
    onRetry: (offerId: String, favorite: Boolean) -> Unit
) {
    val notAvailable = stringResource(R.string.favorite_not_available)
    val messages = mapOf(
        CatalogError.NETWORK to stringResource(R.string.favorite_failed_network),
        CatalogError.SESSION to stringResource(R.string.catalog_session_failed),
        CatalogError.UNEXPECTED to stringResource(R.string.favorite_failed)
    )
    val retryLabel = stringResource(R.string.auth_retry)
    val currentOnRetry by rememberUpdatedState(onRetry)

    LaunchedEffect(problems) {
        problems.collect { problem ->
            launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                when (problem) {
                    FavoriteProblem.NotAvailable -> snackbarHostState.showSnackbar(notAvailable)
                    is FavoriteProblem.Failed -> {
                        val result = snackbarHostState.showSnackbar(
                            message = messages.getValue(problem.error),
                            actionLabel = retryLabel,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            currentOnRetry(problem.offerId, problem.favorite)
                        }
                    }
                }
            }
        }
    }
}
