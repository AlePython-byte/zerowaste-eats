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
    fun withAnimationsOff_theBackGestureSwapsAtOnce() {
        val transitions = screenTransitions(enabled = false, towardEnd = 90)

        listOf(true, false).forEach { fromRight ->
            assertEquals(EnterTransition.None, transitions.predictiveBackEnter(fromRight))
            assertEquals(ExitTransition.None, transitions.predictiveBackExit(fromRight))
        }
        assertEquals(EnterTransition.None, transitions.predictiveSiblingEnter)
        assertEquals(ExitTransition.None, transitions.predictiveSiblingExit)
    }

    @Test
    fun theBackGesture_movesAwayFromTheEdgeItStartedAt() {
        val transitions = screenTransitions(enabled = true, towardEnd = 90)

        assertNotEquals(transitions.predictiveBackExit(true), transitions.predictiveBackExit(false))
        assertNotEquals(transitions.predictiveBackEnter(true), transitions.predictiveBackEnter(false))
    }

    @Test
    fun theBackGesture_isNotTheTimedBackTransition() {
        val transitions = screenTransitions(enabled = true, towardEnd = 90)

        // The gesture tracks the finger at an even pace; the timed one decelerates
        assertNotEquals(transitions.backExit, transitions.predictiveBackExit(false))
        assertNotEquals(transitions.backEnter, transitions.predictiveBackEnter(false))
    }

    @Test
    fun goingBack_mirrorsGoingForward() {
        val transitions = screenTransitions(enabled = true, towardEnd = 90)

        assertNotEquals(transitions.forwardEnter, transitions.backEnter)
        assertNotEquals(transitions.forwardExit, transitions.backExit)
    }
}
