package com.zerowasteeats.app.presentation.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.auth.AuthError
import com.zerowasteeats.app.data.auth.AuthValidators
import com.zerowasteeats.app.data.auth.ValidationError

/** The text under a field for each validation failure. */
@Composable
fun ValidationError.message(): String = when (this) {
    ValidationError.REQUIRED -> stringResource(R.string.validation_required)
    ValidationError.NAME_TOO_SHORT ->
        pluralStringResource(
            R.plurals.validation_name_too_short,
            AuthValidators.NAME_MIN_LENGTH,
            AuthValidators.NAME_MIN_LENGTH
        )
    ValidationError.NAME_TOO_LONG ->
        pluralStringResource(
            R.plurals.validation_name_too_long,
            AuthValidators.NAME_MAX_LENGTH,
            AuthValidators.NAME_MAX_LENGTH
        )
    ValidationError.EMAIL_INVALID -> stringResource(R.string.validation_email_invalid)
    ValidationError.PASSWORD_TOO_SHORT ->
        pluralStringResource(
            R.plurals.validation_password_too_short,
            AuthValidators.PASSWORD_MIN_LENGTH,
            AuthValidators.PASSWORD_MIN_LENGTH
        )
}

/** The text of the form-level banner for each way a sign in or sign up can fail. */
@Composable
fun AuthError.message(): String = when (this) {
    AuthError.INVALID_CREDENTIALS -> stringResource(R.string.auth_error_invalid_credentials)
    AuthError.EMAIL_ALREADY_REGISTERED -> stringResource(R.string.auth_error_email_registered)
    AuthError.WEAK_PASSWORD -> stringResource(R.string.auth_error_weak_password)
    AuthError.PROFILE_INCOMPLETE -> stringResource(R.string.auth_error_profile_incomplete)
    AuthError.NETWORK -> stringResource(R.string.auth_error_network)
    AuthError.UNEXPECTED -> stringResource(R.string.auth_error_unexpected)
}

/** Errors where sending the same thing again may just work, so the banner offers a retry. */
val AuthError.isRetryable: Boolean
    get() = this == AuthError.NETWORK ||
        this == AuthError.PROFILE_INCOMPLETE ||
        this == AuthError.UNEXPECTED
