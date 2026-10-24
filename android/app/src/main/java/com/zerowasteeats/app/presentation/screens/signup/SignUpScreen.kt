package com.zerowasteeats.app.presentation.screens.signup

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.R
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.auth.AuthError
import com.zerowasteeats.app.data.auth.SignUpRequest
import com.zerowasteeats.app.data.session.UserRole
import com.zerowasteeats.app.presentation.components.auth.RoleOptionCard
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppTextField
import com.zerowasteeats.app.presentation.components.common.ENTRANCE_MILLIS
import com.zerowasteeats.app.presentation.components.common.PasswordField
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.screens.auth.AuthErrorBanner
import com.zerowasteeats.app.presentation.screens.auth.AuthLinkButton
import com.zerowasteeats.app.presentation.screens.auth.AuthScreenLayout
import com.zerowasteeats.app.presentation.screens.auth.message
import com.zerowasteeats.app.presentation.screens.auth.onFieldBlur
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

private const val STEP_COUNT = 2

@Composable
fun SignUpScreen(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container =
        (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: SignUpViewModel = viewModel(
        factory = SignUpViewModelFactory(container.sessionManager)
    )
    SignUpContent(
        form = viewModel.form,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onFieldEdited = viewModel::clearError,
        onSubmit = viewModel::submit,
        onSignIn = onSignIn,
        modifier = modifier
    )
}

@Composable
fun SignUpContent(
    form: SignUpFormState,
    isLoading: Boolean,
    error: AuthError?,
    /** Something was typed, so the banner about the last attempt no longer applies. */
    onFieldEdited: () -> Unit,
    /** Called with a request that already passed validation. */
    onSubmit: (SignUpRequest) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val focusRequesters = remember { SignUpField.entries.associateWith { FocusRequester() } }

    // Back from the details returns to the role step, keeping what was typed
    BackHandler(enabled = form.step == SignUpStep.DETAILS) { form.backToRole() }

    fun submit() {
        val invalid = form.validateDetails()
        if (invalid != null) focusRequesters.getValue(invalid).requestFocus()
        else onSubmit(form.toRequest())
    }

    val stepNumber = form.step.ordinal + 1
    AuthScreenLayout(
        title = stringResource(
            if (form.step == SignUpStep.ROLE) R.string.signup_role_title
            else R.string.signup_details_title
        ),
        modifier = modifier,
        eyebrow = stringResource(R.string.signup_step_format, stepNumber, STEP_COUNT)
    ) {
        AnimatedContent(
            targetState = form.step,
            transitionSpec = { stepTransition(animationsEnabled) },
            label = "signUpStep"
        ) { step ->
            when (step) {
                SignUpStep.ROLE -> RoleStep(form = form)
                SignUpStep.DETAILS -> DetailsStep(
                    form = form,
                    isLoading = isLoading,
                    focusRequesters = focusRequesters,
                    onFieldEdited = onFieldEdited,
                    onSubmit = ::submit
                )
            }
        }

        if (form.step == SignUpStep.DETAILS) {
            AuthErrorBanner(
                error = error,
                onRetry = ::submit,
                isLoading = isLoading,
                modifier = Modifier.padding(top = AppSpacing.regular)
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.large))
        when (form.step) {
            SignUpStep.ROLE -> AppButton(
                text = stringResource(R.string.signup_continue),
                onClick = form::continueToDetails,
                modifier = Modifier.fillMaxWidth()
            )
            SignUpStep.DETAILS -> AppButton(
                text = stringResource(R.string.signup_submit),
                onClick = ::submit,
                modifier = Modifier.fillMaxWidth(),
                isLoading = isLoading
            )
        }

        AuthLinkButton(
            text = stringResource(R.string.signup_to_sign_in),
            onClick = onSignIn,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/** Short slide to the side, toward where the step comes from; none when animations are off. */
private fun AnimatedContentTransitionScope<SignUpStep>.stepTransition(
    animationsEnabled: Boolean
): ContentTransform {
    if (!animationsEnabled) {
        return (EnterTransition.None togetherWith ExitTransition.None)
            .using(SizeTransform(clip = false) { _, _ -> snap() })
    }
    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
    val transition = slideInHorizontally(tween(ENTRANCE_MILLIS)) { width -> direction * width / 4 } +
        fadeIn(tween(ENTRANCE_MILLIS)) togetherWith
        slideOutHorizontally(tween(ENTRANCE_MILLIS)) { width -> -direction * width / 4 } +
        fadeOut(tween(ENTRANCE_MILLIS))
    return transition.using(SizeTransform(clip = false) { _, _ -> tween(ENTRANCE_MILLIS) })
}

@Composable
private fun RoleStep(form: SignUpFormState) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        RoleOptionCard(
            title = stringResource(R.string.signup_role_customer_title),
            description = stringResource(R.string.signup_role_customer_description),
            icon = R.drawable.ic_role_customer,
            selected = form.role == UserRole.CUSTOMER,
            onClick = { form.onRoleSelected(UserRole.CUSTOMER) }
        )
        RoleOptionCard(
            title = stringResource(R.string.signup_role_merchant_title),
            description = stringResource(R.string.signup_role_merchant_description),
            icon = R.drawable.ic_role_merchant,
            selected = form.role == UserRole.MERCHANT,
            onClick = { form.onRoleSelected(UserRole.MERCHANT) }
        )
    }
}

@Composable
private fun DetailsStep(
    form: SignUpFormState,
    isLoading: Boolean,
    focusRequesters: Map<SignUpField, FocusRequester>,
    onFieldEdited: () -> Unit,
    onSubmit: () -> Unit
) {
    val lastField = form.fields.last()

    // Every field below differs only in value, label and keyboard, so they share these
    @Composable
    fun fieldModifier(field: SignUpField) = Modifier
        .focusRequester(focusRequesters.getValue(field))
        .onFieldBlur { form.onBlur(field) }

    fun imeAction(field: SignUpField) = if (field == lastField) ImeAction.Done else ImeAction.Next
    val submitOnDone = KeyboardActions(onDone = { onSubmit() })

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.regular)) {
        AppTextField(
            value = form.displayName,
            onValueChange = { form.onDisplayNameChange(it); onFieldEdited() },
            label = stringResource(R.string.signup_field_name),
            modifier = fieldModifier(SignUpField.NAME),
            error = form.errorOf(SignUpField.NAME)?.message(),
            enabled = !isLoading,
            imeAction = imeAction(SignUpField.NAME),
            keyboardActions = submitOnDone,
            contentType = ContentType.PersonFullName,
            capitalization = KeyboardCapitalization.Words
        )
        AppTextField(
            value = form.email,
            onValueChange = { form.onEmailChange(it); onFieldEdited() },
            label = stringResource(R.string.auth_field_email),
            modifier = fieldModifier(SignUpField.EMAIL),
            error = form.errorOf(SignUpField.EMAIL)?.message(),
            enabled = !isLoading,
            keyboardType = KeyboardType.Email,
            imeAction = imeAction(SignUpField.EMAIL),
            keyboardActions = submitOnDone,
            contentType = ContentType.EmailAddress
        )
        PasswordField(
            value = form.password,
            onValueChange = { form.onPasswordChange(it); onFieldEdited() },
            label = stringResource(R.string.auth_field_password),
            modifier = fieldModifier(SignUpField.PASSWORD),
            error = form.errorOf(SignUpField.PASSWORD)?.message(),
            enabled = !isLoading,
            imeAction = imeAction(SignUpField.PASSWORD),
            keyboardActions = submitOnDone,
            isNewPassword = true
        )
        if (form.isMerchant) {
            AppTextField(
                value = form.businessName,
                onValueChange = { form.onBusinessNameChange(it); onFieldEdited() },
                label = stringResource(R.string.signup_field_business_name),
                modifier = fieldModifier(SignUpField.BUSINESS_NAME),
                error = form.errorOf(SignUpField.BUSINESS_NAME)?.message(),
                enabled = !isLoading,
                imeAction = imeAction(SignUpField.BUSINESS_NAME),
                keyboardActions = submitOnDone,
                capitalization = KeyboardCapitalization.Words
            )
            AppTextField(
                value = form.city,
                onValueChange = { form.onCityChange(it); onFieldEdited() },
                label = stringResource(R.string.signup_field_city),
                modifier = fieldModifier(SignUpField.CITY),
                error = form.errorOf(SignUpField.CITY)?.message(),
                enabled = !isLoading,
                imeAction = imeAction(SignUpField.CITY),
                keyboardActions = submitOnDone,
                capitalization = KeyboardCapitalization.Words
            )
        }
    }
}

@Composable
private fun SignUpPreviewContent(
    themeMode: ThemeMode,
    details: Boolean
) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        val form = remember {
            SignUpFormState().apply {
                if (details) {
                    onRoleSelected(UserRole.MERCHANT)
                    continueToDetails()
                    onDisplayNameChange("Ana Pérez")
                    onEmailChange("ana@")
                    validateDetails()
                }
            }
        }
        SignUpContent(
            form = form,
            isLoading = false,
            error = if (details) AuthError.EMAIL_ALREADY_REGISTERED else null,
            onFieldEdited = {},
            onSubmit = {},
            onSignIn = {}
        )
    }
}

@Preview(name = "Role light", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SignUpRoleLightPreview() {
    SignUpPreviewContent(ThemeMode.LIGHT, details = false)
}

@Preview(name = "Role dark", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignUpRoleDarkPreview() {
    SignUpPreviewContent(ThemeMode.DARK, details = false)
}

@Preview(
    name = "Details light",
    widthDp = 380,
    heightDp = 1100,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SignUpDetailsLightPreview() {
    SignUpPreviewContent(ThemeMode.LIGHT, details = true)
}

@Preview(
    name = "Details dark",
    widthDp = 380,
    heightDp = 1100,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SignUpDetailsDarkPreview() {
    SignUpPreviewContent(ThemeMode.DARK, details = true)
}
