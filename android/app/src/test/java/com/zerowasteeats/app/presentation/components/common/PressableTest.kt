package com.zerowasteeats.app.presentation.components.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PressableTest {

    @Test
    fun `at rest the element keeps its size`() {
        assertEquals(1f, pressedScale(progress = 0f, scaleWhenPressed = 0.98f), 0.0001f)
    }

    @Test
    fun `fully pressed it shrinks to the pressed scale`() {
        assertEquals(0.98f, pressedScale(progress = 1f, scaleWhenPressed = 0.98f), 0.0001f)
    }

    @Test
    fun `halfway through it is halfway to the pressed scale`() {
        assertEquals(0.99f, pressedScale(progress = 0.5f, scaleWhenPressed = 0.98f), 0.0001f)
    }

    @Test
    fun `the press is subtle and controls shrink a little more than cards`() {
        assertTrue(PRESSED_CARD_SCALE in 0.95f..0.99f)
        assertTrue(PRESSED_CONTROL_SCALE < PRESSED_CARD_SCALE)
    }
}
