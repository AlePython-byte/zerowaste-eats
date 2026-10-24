package com.zerowasteeats.app.presentation.components.settings

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/** The short question before signing out, for the customer's profile and the business's. */
@Composable
fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.profile_sign_out_title)) },
        text = { Text(text = stringResource(R.string.profile_sign_out_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.common_sign_out),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_cancel),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        containerColor = SurfaceTokens.surface
    )
}

@Composable
private fun SignOutDialogPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SignOutDialog(onConfirm = {}, onDismiss = {})
        }
    }
}

@Preview(name = "Light", widthDp = 380, heightDp = 400, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SignOutDialogLightPreview() {
    SignOutDialogPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, heightDp = 400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignOutDialogDarkPreview() {
    SignOutDialogPreviewContent(ThemeMode.DARK)
}
