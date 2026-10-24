package com.zerowasteeats.app.presentation.screens.signup

import androidx.compose.runtime.saveable.SaverScope
import com.zerowasteeats.app.data.auth.ValidationError
import com.zerowasteeats.app.data.session.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignUpFormStateTest {

    private fun filledCustomer() = SignUpFormState().apply {
        onDisplayNameChange("Ana Pérez")
        onEmailChange("ana@correo.co")
        onPasswordChange("clave1234")
    }

    private fun filledMerchant() = filledCustomer().apply {
        onRoleSelected(UserRole.MERCHANT)
        onBusinessNameChange("Panadería Sol")
        onCityChange("Medellín")
    }

    @Test
    fun newForm_startsAtTheRoleStepAsCustomer_withNoErrors() {
        val form = SignUpFormState()

        assertEquals(SignUpStep.ROLE, form.step)
        assertEquals(UserRole.CUSTOMER, form.role)
        SignUpField.entries.forEach { assertNull(form.errorOf(it)) }
    }

    @Test
    fun continueAndBack_moveBetweenSteps_andKeepEveryValue() {
        val form = filledMerchant()

        form.continueToDetails()
        assertEquals(SignUpStep.DETAILS, form.step)

        assertTrue(form.backToRole())
        assertEquals(SignUpStep.ROLE, form.step)
        assertEquals(UserRole.MERCHANT, form.role)
        assertEquals("Ana Pérez", form.displayName)
        assertEquals("ana@correo.co", form.email)
        assertEquals("clave1234", form.password)
        assertEquals("Panadería Sol", form.businessName)
        assertEquals("Medellín", form.city)
    }

    @Test
    fun backOnTheFirstStep_isLeftToTheCaller() {
        assertFalse(SignUpFormState().backToRole())
    }

    @Test
    fun typing_showsNoErrorBeforeLeavingTheField() {
        val form = SignUpFormState()

        form.onEmailChange("ana@")
        form.onPasswordChange("123")

        assertNull(form.errorOf(SignUpField.EMAIL))
        assertNull(form.errorOf(SignUpField.PASSWORD))
    }

    @Test
    fun leavingAField_showsOnlyThatFieldsError_andThenFollowsChanges() {
        val form = SignUpFormState()
        form.onPasswordChange("123")

        form.onBlur(SignUpField.PASSWORD)

        assertEquals(ValidationError.PASSWORD_TOO_SHORT, form.errorOf(SignUpField.PASSWORD))
        assertNull(form.errorOf(SignUpField.NAME))

        form.onPasswordChange("12345678")
        assertNull(form.errorOf(SignUpField.PASSWORD))
    }

    @Test
    fun name_mustHaveAtLeastTwoCharacters() {
        val form = SignUpFormState()
        form.onDisplayNameChange(" A ")

        form.onBlur(SignUpField.NAME)

        assertEquals(ValidationError.NAME_TOO_SHORT, form.errorOf(SignUpField.NAME))
    }

    @Test
    fun customerForm_doesNotAskForBusinessFields() {
        val form = filledCustomer()

        assertEquals(
            listOf(SignUpField.NAME, SignUpField.EMAIL, SignUpField.PASSWORD),
            form.fields
        )
        assertNull(form.validateDetails())
    }

    @Test
    fun merchantForm_requiresBusinessNameAndCity() {
        val form = filledCustomer().apply { onRoleSelected(UserRole.MERCHANT) }

        assertEquals(SignUpField.BUSINESS_NAME, form.validateDetails())
        assertEquals(ValidationError.REQUIRED, form.errorOf(SignUpField.BUSINESS_NAME))
        assertEquals(ValidationError.REQUIRED, form.errorOf(SignUpField.CITY))

        form.onBusinessNameChange("Panadería Sol")
        assertEquals(SignUpField.CITY, form.validateDetails())

        form.onCityChange("Medellín")
        assertNull(form.validateDetails())
    }

    @Test
    fun validateDetails_showsEveryError_andReturnsTheFirstInvalidFieldInScreenOrder() {
        val form = SignUpFormState()

        assertEquals(SignUpField.NAME, form.validateDetails())
        assertEquals(ValidationError.REQUIRED, form.errorOf(SignUpField.NAME))
        assertEquals(ValidationError.REQUIRED, form.errorOf(SignUpField.EMAIL))
        assertEquals(ValidationError.REQUIRED, form.errorOf(SignUpField.PASSWORD))
    }

    @Test
    fun validateDetails_pointsAtTheFirstInvalidFieldOnly() {
        val form = SignUpFormState().apply {
            onDisplayNameChange("Ana Pérez")
            onEmailChange("no-es-correo")
        }

        assertEquals(SignUpField.EMAIL, form.validateDetails())
    }

    @Test
    fun customerRequest_trimsTextAndDropsBusinessFields() {
        val form = SignUpFormState().apply {
            onDisplayNameChange("  Ana Pérez ")
            onEmailChange(" ana@correo.co ")
            onPasswordChange(" clave1234 ")
            // Left over from trying the business option, then switching back
            onBusinessNameChange("Panadería Sol")
            onCityChange("Medellín")
        }

        val request = form.toRequest()

        assertEquals("Ana Pérez", request.displayName)
        assertEquals("ana@correo.co", request.email)
        assertEquals(" clave1234 ", request.password)
        assertEquals(UserRole.CUSTOMER, request.role)
        assertNull(request.businessName)
        assertNull(request.city)
    }

    @Test
    fun merchantRequest_carriesBusinessNameAndCity() {
        val request = filledMerchant().toRequest()

        assertEquals(UserRole.MERCHANT, request.role)
        assertEquals("Panadería Sol", request.businessName)
        assertEquals("Medellín", request.city)
    }

    private fun SignUpFormState.saved(): Any =
        with(SignUpFormState.Saver) { SaverScope { true }.save(this@saved) }!!

    @Test
    fun saver_roundTripsEveryValueExceptThePassword_theStep_andWhichErrorsAreShown() {
        val form = filledMerchant().apply {
            onCityChange("")
            continueToDetails()
            validateDetails()
        }

        val restored = SignUpFormState.Saver.restore(form.saved())!!

        assertEquals(SignUpStep.DETAILS, restored.step)
        assertEquals(UserRole.MERCHANT, restored.role)
        assertEquals("Ana Pérez", restored.displayName)
        assertEquals("ana@correo.co", restored.email)
        assertEquals("Panadería Sol", restored.businessName)
        assertEquals("", restored.city)
        assertEquals(ValidationError.REQUIRED, restored.errorOf(SignUpField.CITY))
        assertNull(restored.errorOf(SignUpField.NAME))
    }

    @Test
    fun saver_leavesThePasswordOutOfTheSavedState() {
        val form = filledCustomer().apply { onPasswordChange("una-clave-secreta") }

        val saved = form.saved()

        assertFalse(saved.toString().contains("una-clave-secreta"))
        assertEquals("", SignUpFormState.Saver.restore(saved)!!.password)
    }

    @Test
    fun saver_savesOnlyTypesThatSavedStateHandleAccepts() {
        val saved = filledMerchant().apply { validateDetails() }.saved()

        assertTrue(saved is java.util.ArrayList<*>)
        assertTrue((saved as java.util.ArrayList<*>).last() is java.util.ArrayList<*>)
    }
}
