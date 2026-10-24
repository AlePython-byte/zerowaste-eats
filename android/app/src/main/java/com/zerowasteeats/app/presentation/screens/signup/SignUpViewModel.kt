package com.zerowasteeats.app.presentation.screens.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.auth.AuthError
import com.zerowasteeats.app.data.auth.AuthResult
import com.zerowasteeats.app.data.auth.SignUpRequest
import com.zerowasteeats.app.data.session.SessionManager
import kotlinx.coroutines.launch

/**
 * Keeps the request in flight alive across rotations. The form itself is saved by the screen
 * (see [SignUpFormState.Saver]). A successful sign up changes the session in [SessionManager],
 * and whoever shows this screen reacts to that.
 */
class SignUpViewModel(private val sessionManager: SessionManager) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    /** The reason the last attempt failed, for the form-level banner. */
    var error: AuthError? by mutableStateOf(null)
        private set

    fun clearError() {
        error = null
    }

    fun submit(request: SignUpRequest) {
        if (isLoading) return
        error = null
        isLoading = true
        viewModelScope.launch {
            val result = sessionManager.signUp(request)
            if (result is AuthResult.Failure) error = result.error
            isLoading = false
        }
    }
}

class SignUpViewModelFactory(
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == SignUpViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SignUpViewModel(sessionManager) as T
    }
}
