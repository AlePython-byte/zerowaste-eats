package com.zerowasteeats.app.presentation.screens.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.zerowasteeats.app.data.auth.AuthError
import com.zerowasteeats.app.data.auth.AuthResult
import com.zerowasteeats.app.data.auth.SignUpRequest
import com.zerowasteeats.app.data.session.SessionManager
import kotlinx.coroutines.launch

/**
 * Holds the form and the request in flight, so both outlive a rotation or a language change,
 * password included. The form is also written to [savedStateHandle] without the password
 * (see [SignUpFormState.Saver]): if the process dies the person gets back the rest of what they
 * typed and has to type the password again. A successful sign up changes the session in
 * [SessionManager], and whoever shows this screen reacts to that.
 */
class SignUpViewModel(
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val form: SignUpFormState = savedStateHandle.get<Any>(FORM_KEY)
        ?.let(SignUpFormState.Saver::restore)
        ?: SignUpFormState()

    init {
        val saverScope = SaverScope { true }
        viewModelScope.launch {
            snapshotFlow { with(SignUpFormState.Saver) { saverScope.save(form) } }
                .collect { savedStateHandle[FORM_KEY] = it }
        }
    }

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
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass == SignUpViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SignUpViewModel(sessionManager, extras.createSavedStateHandle()) as T
    }
}

private const val FORM_KEY = "signUpForm"
