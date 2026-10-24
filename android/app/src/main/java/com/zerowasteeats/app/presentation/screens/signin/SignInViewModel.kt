package com.zerowasteeats.app.presentation.screens.signin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.auth.AuthError
import com.zerowasteeats.app.data.auth.AuthResult
import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.session.SessionManager
import kotlinx.coroutines.launch

/**
 * Keeps the form and the request in flight alive across rotations. A successful sign in
 * changes the session in [SessionManager], and whoever shows this screen reacts to that.
 */
class SignInViewModel(private val sessionManager: SessionManager) : ViewModel() {

    val form = SignInFormState()

    var isLoading by mutableStateOf(false)
        private set

    /** The reason the last attempt failed, for the form-level banner. */
    var error: AuthError? by mutableStateOf(null)
        private set

    fun onEmailChange(value: String) {
        form.onEmailChange(value)
        error = null
    }

    fun onPasswordChange(value: String) {
        form.onPasswordChange(value)
        error = null
    }

    /** Sends the form. Returns the first invalid field, to focus it, or null if it was sent. */
    fun submit(): SignInField? {
        if (isLoading) return null
        form.validate()?.let { return it }

        error = null
        isLoading = true
        val email = form.email.trim()
        val password = form.password
        viewModelScope.launch {
            val result = sessionManager.signIn(email, password)
            if (result is AuthResult.Failure) error = result.error
            isLoading = false
        }
        return null
    }

    /** Debug shortcut: fills one of the fake's demo accounts and sends it. */
    fun signInWithDemoAccount(email: String) {
        if (isLoading) return
        form.fill(email, FakeAuthRepository.DEMO_PASSWORD)
        submit()
    }
}

class SignInViewModelFactory(
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == SignInViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SignInViewModel(sessionManager) as T
    }
}
