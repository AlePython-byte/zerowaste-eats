package com.zerowasteeats.app.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import com.zerowasteeats.app.presentation.components.common.ScreenTransitions
import com.zerowasteeats.app.presentation.components.common.rememberScreenTransitions

/** How a nav host moves from one destination to another. */
internal enum class NavTransitionKind {
    /** One screen deeper or shallower than the other: they slide on the shared axis. */
    Axis,

    /** Side by side (tabs), or led by a shared element: they only fade through. */
    Fade
}

/**
 * Two tabs are siblings, so switching between them only fades. A screen in [fadeRoutes] has a
 * shared element that already carries the eye from one screen to the other, so a slide on top
 * of it would compete with it. Everything else slides.
 */
internal fun navTransitionKind(
    from: String?,
    to: String?,
    tabRoutes: Set<String>,
    fadeRoutes: Set<String>
): NavTransitionKind = when {
    from in tabRoutes && to in tabRoutes -> NavTransitionKind.Fade
    from in fadeRoutes || to in fadeRoutes -> NavTransitionKind.Fade
    else -> NavTransitionKind.Axis
}

/**
 * The four NavHost transition callbacks, built on the app's [ScreenTransitions]. Pass them
 * straight to the NavHost parameters of the same name.
 */
internal class NavTransitions(
    private val transitions: ScreenTransitions,
    private val tabRoutes: Set<String>,
    private val fadeRoutes: Set<String>
) {
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.kind() = navTransitionKind(
        from = initialState.destination.route,
        to = targetState.destination.route,
        tabRoutes = tabRoutes,
        fadeRoutes = fadeRoutes
    )

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when (kind()) {
            NavTransitionKind.Axis -> transitions.forwardEnter
            NavTransitionKind.Fade -> transitions.siblingEnter
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when (kind()) {
            NavTransitionKind.Axis -> transitions.forwardExit
            NavTransitionKind.Fade -> transitions.siblingExit
        }
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when (kind()) {
            NavTransitionKind.Axis -> transitions.backEnter
            NavTransitionKind.Fade -> transitions.siblingEnter
        }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when (kind()) {
            NavTransitionKind.Axis -> transitions.backExit
            NavTransitionKind.Fade -> transitions.siblingExit
        }
    }
}

/** [NavTransitions] for a nav host whose bottom bar tabs are [tabRoutes]. */
@Composable
internal fun rememberNavTransitions(
    tabRoutes: Set<String>,
    fadeRoutes: Set<String> = emptySet()
): NavTransitions {
    val transitions = rememberScreenTransitions()
    return remember(transitions, tabRoutes, fadeRoutes) {
        NavTransitions(transitions, tabRoutes, fadeRoutes)
    }
}
