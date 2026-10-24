package com.zerowasteeats.app.presentation.screens.signin

import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.R
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.auth.AuthError
import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppTextField
import com.zerowasteeats.app.presentation.components.common.FormErrorBanner
import com.zerowasteeats.app.presentation.components.common.PasswordField
import com.zerowasteeats.app.presentation.screens.auth.AuthScreenLayout
import com.zerowasteeats.app.presentation.screens.auth.message
import com.zerowasteeats.app.presentation.screens.auth.onFieldBlur
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/** Debug-only helpers shown under the form. Null in release builds. */
class SignInDebugTools(
    val simulateNetworkError: Boolean,
    /** Null when the repository is not the fake, so there is nothing to switch. */
    val onSimulateNetworkErrorChange: ((Boolean) -> Unit)?,
    val onSignInAsCustomer: () -> Unit,
    val onSignInAsMerchant: () -> Unit
)

@Composable
fun SignInScreen(
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = (context.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: SignInViewModel = viewModel(
        factory = SignInViewModelFactory(container.sessionManager)
    )

    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val debugTools = if (isDebuggable) {
        val fake = container.authRepository as? FakeAuthRepository
        var simulateNetworkError by remember { mutableStateOf(fake?.simulateNetworkError == true) }
        SignInDebugTools(
            simulateNetworkError = simulateNetworkError,
            onSimulateNetworkErrorChange = fake?.let {
                { enabled ->
                    it.simulateNetworkError = enabled
                    simulateNetworkError = enabled
                }
            },
            onSignInAsCustomer = {
                viewModel.signInWithDemoAccount(FakeAuthRepository.DEMO_CUSTOMER_EMAIL)
            },
            onSignInAsMerchant = {
                viewModel.signInWithDemoAccount(FakeAuthRepository.DEMO_MERCHANT_EMAIL)
            }
        )
    } else {
        null
    }

    SignInContent(
        form = viewModel.form,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onForgotPassword = onForgotPassword,
        onSignUp = onSignUp,
        modifier = modifier,
        debugTools = debugTools
    )
}

@Composable
fun SignInContent(
    form: SignInFormState,
    isLoading: Boolean,
    error: AuthError?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    /** Sends the form; returns the first invalid field, to focus it, or null if it went out. */
    onSubmit: () -> SignInField?,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    debugTools: SignInDebugTools? = null
) {
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    fun submit() {
        when (onSubmit()) {
            SignInField.EMAIL -> emailFocus.requestFocus()
            SignInField.PASSWORD -> passwordFocus.requestFocus()
            null -> Unit
        }
    }

    AuthScreenLayout(
        title = stringResource(R.string.signin_title),
        modifier = modifier,
        subtitle = stringResource(R.string.signin_subtitle)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.regular)) {
            AppTextField(
                value = form.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.auth_field_email),
                modifier = Modifier
                    .focusRequester(emailFocus)
                    .onFieldBlur { form.onBlur(SignInField.EMAIL) },
                error = form.emailError?.message(),
                enabled = !isLoading,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                contentType = ContentType.EmailAddress
            )
            PasswordField(
                value = form.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.auth_field_password),
                modifier = Modifier
                    .focusRequester(passwordFocus)
                    .onFieldBlur { form.onBlur(SignInField.PASSWORD) },
                error = form.passwordError?.message(),
                enabled = !isLoading,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { submit() })
            )
        }

        TextButton(
            onClick = onForgotPassword,
            modifier = Modifier
                .align(Alignment.End)
                .defaultMinSize(minHeight = 48.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(
                text = stringResource(R.string.signin_forgot_password),
                style = MaterialTheme.typography.labelLarge
            )
        }

        FormErrorBanner(message = error?.message())
        if (error == AuthError.NETWORK) {
            TextButton(
                onClick = { submit() },
                enabled = !isLoading,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    text = stringResource(R.string.auth_retry),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.regular))
        AppButton(
            text = stringResource(R.string.signin_submit),
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            isLoading = isLoading
        )

        TextButton(
            onClick = onSignUp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .defaultMinSize(minHeight = 48.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(
                text = stringResource(R.string.signin_to_sign_up),
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (debugTools != null) {
            Spacer(modifier = Modifier.height(AppSpacing.large))
            DebugTools(debugTools, isLoading)
        }
    }
}

@Composable
private fun DebugTools(tools: SignInDebugTools, isLoading: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        Text(
            text = stringResource(R.string.signin_debug_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            OutlinedButton(
                onClick = tools.onSignInAsCustomer,
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.signin_debug_as_customer),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            OutlinedButton(
                onClick = tools.onSignInAsMerchant,
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.signin_debug_as_merchant),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        val onSimulateChange = tools.onSimulateNetworkErrorChange
        if (onSimulateChange != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.signin_debug_simulate_network_error),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = AppSpacing.regular),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = tools.simulateNetworkError,
                    onCheckedChange = onSimulateChange
                )
            }
        }
    }
}

@Composable
private fun SignInPreviewContent(
    themeMode: ThemeMode,
    withErrors: Boolean
) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        val form = remember {
            SignInFormState().apply {
                if (withErrors) {
                    onEmailChange("ana@")
                    validate()
                }
            }
        }
        SignInContent(
            form = form,
            isLoading = false,
            error = if (withErrors) AuthError.NETWORK else null,
            onEmailChange = form::onEmailChange,
            onPasswordChange = form::onPasswordChange,
            onSubmit = { form.validate() },
            onForgotPassword = {},
            onSignUp = {},
            debugTools = if (withErrors) {
                SignInDebugTools(
                    simulateNetworkError = true,
                    onSimulateNetworkErrorChange = {},
                    onSignInAsCustomer = {},
                    onSignInAsMerchant = {}
                )
            } else {
                null
            }
        )
    }
}

@Preview(name = "Light", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SignInLightPreview() {
    SignInPreviewContent(ThemeMode.LIGHT, withErrors = false)
}

@Preview(name = "Dark", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignInDarkPreview() {
    SignInPreviewContent(ThemeMode.DARK, withErrors = false)
}

@Preview(
    name = "Errors light",
    widthDp = 380,
    heightDp = 1000,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SignInErrorsLightPreview() {
    SignInPreviewContent(ThemeMode.LIGHT, withErrors = true)
}

@Preview(
    name = "Errors dark",
    widthDp = 380,
    heightDp = 1000,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SignInErrorsDarkPreview() {
    SignInPreviewContent(ThemeMode.DARK, withErrors = true)
}
