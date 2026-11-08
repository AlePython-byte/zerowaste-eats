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

/**
 * Says that loading failed, with a way to try again. It waits for an action when there is
 * nothing else on screen ([hasContent] false), and stays a while otherwise. A new failure after
 * a retry is said again, since the error goes away while the retry runs.
 */
@Composable
fun LoadFailureSnackbar(
    error: CatalogError?,
    hasContent: Boolean,
    snackbarHostState: SnackbarHostState,
    messageFor: (CatalogError) -> String,
    retryLabel: String,
    onRetry: () -> Unit
) {
    val currentHasContent by rememberUpdatedState(hasContent)
    val currentMessageFor by rememberUpdatedState(messageFor)
    val currentOnRetry by rememberUpdatedState(onRetry)

    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            message = currentMessageFor(error),
            actionLabel = retryLabel,
            duration = if (currentHasContent) SnackbarDuration.Long else SnackbarDuration.Indefinite
        )
        if (result == SnackbarResult.ActionPerformed) currentOnRetry()
    }
}

/** [LoadFailureSnackbar] for the nearby offers, with their messages. */
@Composable
fun CatalogLoadFailureSnackbar(
    state: OfferFeedState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit
) {
    val messages = mapOf(
        CatalogError.NETWORK to stringResource(R.string.catalog_load_failed_network),
        CatalogError.SESSION to stringResource(R.string.catalog_session_failed),
        CatalogError.UNEXPECTED to stringResource(R.string.catalog_load_failed)
    )
    LoadFailureSnackbar(
        error = state.loadError,
        hasContent = state.offers.isNotEmpty(),
        snackbarHostState = snackbarHostState,
        messageFor = messages::getValue,
        retryLabel = stringResource(R.string.auth_retry),
        onRetry = onRetry
    )
}
