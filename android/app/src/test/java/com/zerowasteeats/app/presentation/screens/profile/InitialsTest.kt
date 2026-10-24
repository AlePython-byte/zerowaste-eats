package com.zerowasteeats.app.presentation.screens.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {

    @Test
    fun twoWords_takeTheFirstLetterOfEach() {
        assertEquals("CD", initialsOf("Comprador Demo"))
    }

    @Test
    fun moreWords_takeTheFirstAndLast() {
        assertEquals("AP", initialsOf("Ana María Pérez"))
    }

    @Test
    fun oneWord_takesOneLetter_inUpperCase() {
        assertEquals("A", initialsOf("ana"))
    }

    @Test
    fun extraSpaces_areIgnored() {
        assertEquals("LG", initialsOf("  Luis   Gómez "))
    }

    @Test
    fun blankName_givesNoInitials() {
        assertEquals("", initialsOf("   "))
    }
}
