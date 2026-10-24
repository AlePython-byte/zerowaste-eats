package com.zerowasteeats.app.presentation.screens.signin

import com.zerowasteeats.app.data.auth.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignInFormStateTest {

    @Test
    fun newForm_showsNoErrors() {
        val form = SignInFormState()

        assertNull(form.emailError)
        assertNull(form.passwordError)
    }

    @Test
    fun typing_showsNoErrorBeforeLeavingTheField() {
        val form = SignInFormState()

        form.onEmailChange("ana@")
        form.onPasswordChange("x")

        assertNull(form.emailError)
        assertNull(form.passwordError)
    }

    @Test
    fun leavingAField_showsOnlyThatFieldsError() {
        val form = SignInFormState()
        form.onEmailChange("ana@")

        form.onBlur(SignInField.EMAIL)

        assertEquals(ValidationError.EMAIL_INVALID, form.emailError)
        assertNull(form.passwordError)
    }

    @Test
    fun leavingAnEmptyField_isRequired() {
        val form = SignInFormState()

        form.onBlur(SignInField.PASSWORD)

        assertEquals(ValidationError.REQUIRED, form.passwordError)
    }

    @Test
    fun afterLeavingTheField_theErrorFollowsEachChange() {
        val form = SignInFormState()
        form.onEmailChange("ana@")
        form.onBlur(SignInField.EMAIL)

        form.onEmailChange("ana@correo.co")
        assertNull(form.emailError)

        form.onEmailChange("ana@correo")
        assertEquals(ValidationError.EMAIL_INVALID, form.emailError)
    }

    @Test
    fun password_onlyHasToBeFilledIn() {
        val form = SignInFormState()
        form.onPasswordChange("a")

        form.onBlur(SignInField.PASSWORD)

        assertNull(form.passwordError)
    }

    @Test
    fun email_ignoresSurroundingSpaces() {
        val form = SignInFormState()
        form.onEmailChange("  ana@correo.co ")

        form.onBlur(SignInField.EMAIL)

        assertNull(form.emailError)
    }

    @Test
    fun validate_showsEveryError_andReturnsTheFirstInvalidField() {
        val form = SignInFormState()

        val first = form.validate()

        assertEquals(SignInField.EMAIL, first)
        assertEquals(ValidationError.REQUIRED, form.emailError)
        assertEquals(ValidationError.REQUIRED, form.passwordError)
    }

    @Test
    fun validate_skipsAValidEmail_andPointsAtThePassword() {
        val form = SignInFormState()
        form.onEmailChange("ana@correo.co")

        assertEquals(SignInField.PASSWORD, form.validate())
        assertNull(form.emailError)
    }

    @Test
    fun validate_returnsNullWhenTheFormCanBeSent() {
        val form = SignInFormState()
        form.onEmailChange("ana@correo.co")
        form.onPasswordChange("Demo1234")

        assertNull(form.validate())
    }

    @Test
    fun fill_setsBothValues() {
        val form = SignInFormState()

        form.fill("ana@correo.co", "Demo1234")

        assertEquals("ana@correo.co", form.email)
        assertEquals("Demo1234", form.password)
        assertNull(form.validate())
    }
}
