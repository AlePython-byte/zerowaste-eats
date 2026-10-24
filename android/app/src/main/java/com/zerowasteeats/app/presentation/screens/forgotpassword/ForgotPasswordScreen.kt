package com.zerowasteeats.app.presentation.screens.forgotpassword

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppTextField
import com.zerowasteeats.app.presentation.components.common.entranceRise
import com.zerowasteeats.app.presentation.screens.auth.AuthLinkButton
import com.zerowasteeats.app.presentation.screens.auth.AuthScreenLayout
import com.zerowasteeats.app.presentation.screens.auth.message
import com.zerowasteeats.app.presentation.screens.auth.onFieldBlur
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/**
 * Asks for the email and shows the same confirmation whether or not an account exists. Nothing
 * is sent yet: there is no backend for it, so this is only the screen.
 */
@Composable
fun ForgotPasswordScreen(
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val form = rememberSaveable(saver = ForgotPasswordFormState.Saver) {
        ForgotPasswordFormState()
    }
    ForgotPasswordContent(form = form, onBackToSignIn = onBackToSignIn, modifier = modifier)
}

@Composable
fun ForgotPasswordContent(
    form: ForgotPasswordFormState,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emailFocus = remember { FocusRequester() }

    fun submit() {
        if (!form.submit()) emailFocus.requestFocus()
    }

    AuthScreenLayout(
        title = stringResource(R.string.forgot_password_title),
        modifier = modifier,
        subtitle = stringResource(R.string.forgot_password_subtitle)
    ) {
        AppTextField(
            value = form.email,
            onValueChange = form::onEmailChange,
            label = stringResource(R.string.auth_field_email),
            modifier = Modifier
                .focusRequester(emailFocus)
                .onFieldBlur(form::onBlur),
            error = form.emailError?.message(),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() }),
            contentType = ContentType.EmailAddress
        )

        if (form.isSent) {
            Surface(
                modifier = Modifier
                    .padding(top = AppSpacing.regular)
                    .fillMaxWidth()
                    .entranceRise()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(AppRadius.large),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = stringResource(R.string.forgot_password_sent),
                    modifier = Modifier.padding(AppSpacing.regular),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
        AppButton(
            text = stringResource(R.string.forgot_password_submit),
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth()
        )

        AuthLinkButton(
            text = stringResource(R.string.forgot_password_back_to_sign_in),
            onClick = onBackToSignIn,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ForgotPasswordPreviewContent(themeMode: ThemeMode, sent: Boolean) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        val form = remember {
            ForgotPasswordFormState().apply {
                if (sent) {
                    onEmailChange("ana@correo.co")
                    submit()
                }
            }
        }
        ForgotPasswordContent(form = form, onBackToSignIn = {})
    }
}

@Preview(name = "Light", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ForgotPasswordLightPreview() {
    ForgotPasswordPreviewContent(ThemeMode.LIGHT, sent = false)
}

@Preview(name = "Dark", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ForgotPasswordDarkPreview() {
    ForgotPasswordPreviewContent(ThemeMode.DARK, sent = false)
}

@Preview(name = "Sent light", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ForgotPasswordSentLightPreview() {
    ForgotPasswordPreviewContent(ThemeMode.LIGHT, sent = true)
}

@Preview(name = "Sent dark", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ForgotPasswordSentDarkPreview() {
    ForgotPasswordPreviewContent(ThemeMode.DARK, sent = true)
}
