package com.zerowasteeats.app.presentation.components.common

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScreenTransitionsTest {

    @Test
    fun withAnimationsOff_everyScreenSwapsAtOnce() {
        val transitions = screenTransitions(enabled = false, towardEnd = 90)

        listOf(transitions.forwardEnter, transitions.backEnter, transitions.siblingEnter)
            .forEach { assertEquals(EnterTransition.None, it) }
        listOf(transitions.forwardExit, transitions.backExit, transitions.siblingExit)
            .forEach { assertEquals(ExitTransition.None, it) }
    }

    @Test
    fun withAnimationsOn_everyScreenAnimates() {
        val transitions = screenTransitions(enabled = true, towardEnd = 90)

        listOf(transitions.forwardEnter, transitions.backEnter, transitions.siblingEnter)
            .forEach { assertNotEquals(EnterTransition.None, it) }
        listOf(transitions.forwardExit, transitions.backExit, transitions.siblingExit)
            .forEach { assertNotEquals(ExitTransition.None, it) }
    }

    @Test
    fun goingBack_mirrorsGoingForward() {
        val transitions = screenTransitions(enabled = true, towardEnd = 90)

        assertNotEquals(transitions.forwardEnter, transitions.backEnter)
        assertNotEquals(transitions.forwardExit, transitions.backExit)
    }
}
