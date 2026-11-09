package com.zerowasteeats.app.presentation.screens.offerdetail

import com.zerowasteeats.app.presentation.components.common.AppMotion
import com.zerowasteeats.app.presentation.components.common.STAGGER_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Test

class ReservedCodeRevealTest {

    @Test
    fun `the reveal lasts until the last character has appeared`() {
        val expected = (6 * STAGGER_MILLIS + AppMotion.SHORT_MILLIS).toFloat()

        assertEquals(expected, codeRevealMillis(length = 7), 0.001f)
    }

    @Test
    fun `an empty code takes as long as a single character`() {
        assertEquals(AppMotion.SHORT_MILLIS.toFloat(), codeRevealMillis(length = 0), 0.001f)
    }

    @Test
    fun `nothing has appeared before the reveal begins`() {
        assertEquals(0f, codeCharacterFraction(elapsedMillis = 0f, index = 0), 0.001f)
        assertEquals(0f, codeCharacterFraction(elapsedMillis = 0f, index = 3), 0.001f)
    }

    @Test
    fun `characters appear one after another`() {
        val halfway = AppMotion.SHORT_MILLIS / 2f

        assertEquals(0.5f, codeCharacterFraction(halfway, index = 0), 0.001f)
        assertEquals(0f, codeCharacterFraction(STAGGER_MILLIS - 1f, index = 1), 0.001f)
    }

    @Test
    fun `every character is complete at the end`() {
        val end = codeRevealMillis(length = 7)

        repeat(7) { index -> assertEquals(1f, codeCharacterFraction(end, index), 0.001f) }
    }
}
