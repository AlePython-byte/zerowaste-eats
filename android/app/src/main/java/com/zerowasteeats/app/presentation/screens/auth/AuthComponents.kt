package com.zerowasteeats.app.presentation.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.auth.AuthError
import com.zerowasteeats.app.presentation.components.common.FormErrorBanner

/** Text link of the auth screens: a 48 dp touch target in the text accent color. */
@Composable
fun AuthLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.tertiary
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Form-level banner for [error]. A network failure also offers a retry, since sending the
 * same form again may just work.
 */
@Composable
fun AuthErrorBanner(
    error: AuthError?,
    onRetry: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FormErrorBanner(message = error?.message())
        if (error == AuthError.NETWORK) {
            AuthLinkButton(
                text = stringResource(R.string.auth_retry),
                onClick = onRetry,
                enabled = !isLoading
            )
        }
    }
}
