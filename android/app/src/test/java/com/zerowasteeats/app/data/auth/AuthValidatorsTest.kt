package com.zerowasteeats.app.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorsTest {

    @Test
    fun name_acceptsTwoToEightyCharacters() {
        assertNull(AuthValidators.validateName("Al"))
        assertNull(AuthValidators.validateName("a".repeat(80)))
    }

    @Test
    fun name_rejectsEmptyShortAndLong() {
        assertEquals(ValidationError.REQUIRED, AuthValidators.validateName(""))
        assertEquals(ValidationError.REQUIRED, AuthValidators.validateName("   "))
        assertEquals(ValidationError.NAME_TOO_SHORT, AuthValidators.validateName("A"))
        assertEquals(ValidationError.NAME_TOO_LONG, AuthValidators.validateName("a".repeat(81)))
    }

    @Test
    fun name_ignoresSurroundingSpaces() {
        assertEquals(ValidationError.NAME_TOO_SHORT, AuthValidators.validateName("  A  "))
        assertNull(AuthValidators.validateName("  Ana  "))
        assertNull(AuthValidators.validateName(" " + "a".repeat(80) + " "))
    }

    @Test
    fun email_acceptsCommonAddresses() {
        assertNull(AuthValidators.validateEmail("comprador@demo.co"))
        assertNull(AuthValidators.validateEmail("ana.perez+rescate@correo.com.co"))
        assertNull(AuthValidators.validateEmail("  ana@correo.com  "))
    }

    @Test
    fun email_rejectsMalformedAddresses() {
        assertEquals(ValidationError.REQUIRED, AuthValidators.validateEmail(""))
        assertEquals(ValidationError.REQUIRED, AuthValidators.validateEmail("   "))
        listOf("ana", "ana@", "@correo.com", "ana@correo", "ana@correo.", "ana@@correo.com", "ana perez@correo.com", "ana@correo.c")
            .forEach { assertEquals(it, ValidationError.EMAIL_INVALID, AuthValidators.validateEmail(it)) }
    }

    @Test
    fun email_rejectsAddressesOverTheLengthLimit() {
        val tooLong = "a".repeat(250) + "@b.co"
        assertEquals(ValidationError.EMAIL_INVALID, AuthValidators.validateEmail(tooLong))
    }

    @Test
    fun newPassword_needsAtLeastEightCharacters() {
        assertEquals(ValidationError.REQUIRED, AuthValidators.validateNewPassword(""))
        assertEquals(ValidationError.PASSWORD_TOO_SHORT, AuthValidators.validateNewPassword("1234567"))
        assertNull(AuthValidators.validateNewPassword("12345678"))
    }

    @Test
    fun newPassword_countsSpaces() {
        assertNull(AuthValidators.validateNewPassword("        "))
    }

    @Test
    fun required_rejectsBlankOnly() {
        assertEquals(ValidationError.REQUIRED, AuthValidators.validateRequired(""))
        assertEquals(ValidationError.REQUIRED, AuthValidators.validateRequired("   "))
        assertNull(AuthValidators.validateRequired("Pasto"))
    }
}
