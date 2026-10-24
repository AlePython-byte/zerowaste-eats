package com.zerowasteeats.app.presentation.screens.signin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zerowasteeats.app.data.auth.AuthValidators
import com.zerowasteeats.app.data.auth.ValidationError

enum class SignInField { EMAIL, PASSWORD }

/**
 * Values and field errors of the sign in form. A field shows an error only after the person
 * left it or tried to send the form; from then on it checks again on every change, so the
 * error goes away as soon as the value is fixed. Nothing is flagged while typing the first time.
 */
class SignInFormState {
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    private var emailShown by mutableStateOf(false)
    private var passwordShown by mutableStateOf(false)

    val emailError: ValidationError?
        get() = if (emailShown) AuthValidators.validateEmail(email) else null

    // On sign in the password only has to be filled in: its length rule is for new passwords
    val passwordError: ValidationError?
        get() = if (passwordShown) AuthValidators.validateRequired(password) else null

    fun onEmailChange(value: String) {
        email = value
    }

    fun onPasswordChange(value: String) {
        password = value
    }

    /** The person left [field]: from now on it shows its error. */
    fun onBlur(field: SignInField) {
        when (field) {
            SignInField.EMAIL -> emailShown = true
            SignInField.PASSWORD -> passwordShown = true
        }
    }

    /**
     * For the send button: every field shows its error. Returns the first field, in screen
     * order, that is not valid (to focus it), or null when the form can be sent.
     */
    fun validate(): SignInField? {
        emailShown = true
        passwordShown = true
        return when {
            emailError != null -> SignInField.EMAIL
            passwordError != null -> SignInField.PASSWORD
            else -> null
        }
    }

    fun fill(email: String, password: String) {
        this.email = email
        this.password = password
    }
}
