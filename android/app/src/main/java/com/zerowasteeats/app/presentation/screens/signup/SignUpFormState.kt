package com.zerowasteeats.app.presentation.screens.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.zerowasteeats.app.data.auth.AuthValidators
import com.zerowasteeats.app.data.auth.SignUpRequest
import com.zerowasteeats.app.data.auth.ValidationError
import com.zerowasteeats.app.data.session.UserRole

enum class SignUpStep { ROLE, DETAILS }

/** In the order they appear on screen. The last two are only asked of a business. */
enum class SignUpField { NAME, EMAIL, PASSWORD, BUSINESS_NAME, CITY }

/**
 * Everything the two sign up steps hold: the step, the chosen role and the fields. Errors work
 * as in [com.zerowasteeats.app.presentation.screens.signin.SignInFormState]: a field shows its
 * error only after it was left or the form was sent, and then follows each change.
 *
 * Going back to the first step keeps every value. [Saver] keeps them across rotation and
 * process death.
 */
class SignUpFormState(
    step: SignUpStep = SignUpStep.ROLE,
    role: UserRole = UserRole.CUSTOMER,
    displayName: String = "",
    email: String = "",
    password: String = "",
    businessName: String = "",
    city: String = "",
    shownFields: Set<SignUpField> = emptySet()
) {
    var step by mutableStateOf(step)
        private set
    var role by mutableStateOf(role)
        private set
    var displayName by mutableStateOf(displayName)
        private set
    var email by mutableStateOf(email)
        private set
    var password by mutableStateOf(password)
        private set
    var businessName by mutableStateOf(businessName)
        private set
    var city by mutableStateOf(city)
        private set

    private val shown: SnapshotStateList<SignUpField> = mutableStateListOf<SignUpField>().apply {
        addAll(shownFields)
    }

    val isMerchant: Boolean get() = role == UserRole.MERCHANT

    /** The fields this form asks for, given the chosen role. */
    val fields: List<SignUpField>
        get() = if (isMerchant) SignUpField.entries else SignUpField.entries - setOf(
            SignUpField.BUSINESS_NAME,
            SignUpField.CITY
        )

    fun errorOf(field: SignUpField): ValidationError? =
        if (field in shown) validate(field) else null

    fun onRoleSelected(value: UserRole) {
        role = value
    }

    fun onDisplayNameChange(value: String) {
        displayName = value
    }

    fun onEmailChange(value: String) {
        email = value
    }

    fun onPasswordChange(value: String) {
        password = value
    }

    fun onBusinessNameChange(value: String) {
        businessName = value
    }

    fun onCityChange(value: String) {
        city = value
    }

    /** The person left [field]: from now on it shows its error. */
    fun onBlur(field: SignUpField) {
        if (field !in shown) shown.add(field)
    }

    fun continueToDetails() {
        step = SignUpStep.DETAILS
    }

    /** Returns false when already on the first step, so the caller can let Back leave instead. */
    fun backToRole(): Boolean {
        if (step == SignUpStep.ROLE) return false
        step = SignUpStep.ROLE
        return true
    }

    /**
     * For the send button: every field shows its error. Returns the first field, in screen
     * order, that is not valid (to focus it), or null when the form can be sent.
     */
    fun validateDetails(): SignUpField? {
        fields.forEach(::onBlur)
        return fields.firstOrNull { errorOf(it) != null }
    }

    /** Call only after [validateDetails] returned null. */
    fun toRequest(): SignUpRequest = SignUpRequest(
        displayName = displayName.trim(),
        email = email.trim(),
        password = password,
        role = role,
        businessName = businessName.trim().takeIf { isMerchant },
        city = city.trim().takeIf { isMerchant }
    )

    private fun validate(field: SignUpField): ValidationError? = when (field) {
        SignUpField.NAME -> AuthValidators.validateName(displayName)
        SignUpField.EMAIL -> AuthValidators.validateEmail(email)
        SignUpField.PASSWORD -> AuthValidators.validateNewPassword(password)
        SignUpField.BUSINESS_NAME -> AuthValidators.validateRequired(businessName)
        SignUpField.CITY -> AuthValidators.validateRequired(city)
    }

    companion object {
        /**
         * Saves every value, the password too: the saved state lives in memory for rotation and
         * is not meant to be written anywhere else.
         */
        val Saver: Saver<SignUpFormState, Any> = Saver(
            save = { form -> form.toSavedList() },
            restore = { saved -> restoreFrom(saved) }
        )

        private fun SignUpFormState.toSavedList(): List<Any> = listOf(
            step.name,
            role.name,
            displayName,
            email,
            password,
            businessName,
            city,
            shown.map { it.name }
        )

        private fun restoreFrom(saved: Any): SignUpFormState {
            val values = saved as List<*>
            return SignUpFormState(
                step = SignUpStep.valueOf(values[0] as String),
                role = UserRole.valueOf(values[1] as String),
                displayName = values[2] as String,
                email = values[3] as String,
                password = values[4] as String,
                businessName = values[5] as String,
                city = values[6] as String,
                shownFields = (values[7] as List<*>)
                    .map { SignUpField.valueOf(it as String) }
                    .toSet()
            )
        }
    }
}
