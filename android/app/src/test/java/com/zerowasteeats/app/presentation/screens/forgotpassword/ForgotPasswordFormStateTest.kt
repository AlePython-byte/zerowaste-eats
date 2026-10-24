package com.zerowasteeats.app.presentation.screens.forgotpassword

import com.zerowasteeats.app.data.auth.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgotPasswordFormStateTest {

    @Test
    fun typing_showsNoErrorBeforeLeavingTheField() {
        val form = ForgotPasswordFormState()

        form.onEmailChange("ana@")

        assertNull(form.emailError)
    }

    @Test
    fun leavingTheField_showsTheError() {
        val form = ForgotPasswordFormState()
        form.onEmailChange("ana@")

        form.onBlur()

        assertEquals(ValidationError.EMAIL_INVALID, form.emailError)
    }

    @Test
    fun submitWithInvalidEmail_isRefused_andNothingIsSent() {
        val form = ForgotPasswordFormState()

        assertFalse(form.submit())

        assertEquals(ValidationError.REQUIRED, form.emailError)
        assertFalse(form.isSent)
    }

    @Test
    fun submitWithValidEmail_marksTheRequestAsSent() {
        val form = ForgotPasswordFormState()
        form.onEmailChange("ana@correo.co")

        assertTrue(form.submit())

        assertTrue(form.isSent)
        assertNull(form.emailError)
    }

    @Test
    fun editingTheEmailAfterSending_clearsTheSentMessage() {
        val form = ForgotPasswordFormState()
        form.onEmailChange("ana@correo.co")
        form.submit()

        form.onEmailChange("otra@correo.co")

        assertFalse(form.isSent)
    }
}
