package com.zerowasteeats.app.presentation.screens.forgotpassword

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import com.zerowasteeats.app.data.auth.AuthValidators
import com.zerowasteeats.app.data.auth.ValidationError

/**
 * The email field and whether the link request went out. The email shows its error after the
 * person left the field or tried to send, as in the other auth forms.
 */
class ForgotPasswordFormState(
    email: String = "",
    errorShown: Boolean = false,
    isSent: Boolean = false
) {
    var email by mutableStateOf(email)
        private set

    private var errorShown by mutableStateOf(errorShown)

    /** True after a valid send, until the email is edited. */
    var isSent by mutableStateOf(isSent)
        private set

    val emailError: ValidationError?
        get() = if (errorShown) AuthValidators.validateEmail(email) else null

    fun onEmailChange(value: String) {
        email = value
        // The message was about the previous address
        isSent = false
    }

    fun onBlur() {
        errorShown = true
    }

    /**
     * Returns false when the email is not valid, leaving its error on screen. Otherwise marks
     * the request as sent. There is no account lookup, so the answer never depends on whether
     * the email exists: that would tell anyone which emails are registered.
     */
    fun submit(): Boolean {
        errorShown = true
        if (emailError != null) return false
        isSent = true
        return true
    }

    companion object {
        val Saver: Saver<ForgotPasswordFormState, Any> = Saver(
            save = { form -> listOf(form.email, form.errorShown, form.isSent) },
            restore = { saved ->
                val values = saved as List<*>
                ForgotPasswordFormState(
                    email = values[0] as String,
                    errorShown = values[1] as Boolean,
                    isSent = values[2] as Boolean
                )
            }
        )
    }
}
