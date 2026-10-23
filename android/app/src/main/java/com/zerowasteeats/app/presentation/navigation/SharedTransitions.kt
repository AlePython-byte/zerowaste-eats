package com.zerowasteeats.app.presentation.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled

/** Provided by [CustomerNavigation]; null outside of it (previews, tests). */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/**
 * The nav destination's enter/exit scope. navigation-compose 2.10 has no
 * LocalNavAnimatedContentScope, so [sharedComposable] exposes the scope it receives.
 */
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Like [composable], but lets the destination's content join shared element transitions. */
fun NavGraphBuilder.sharedComposable(
    route: String,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(route) { backStackEntry ->
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            content(backStackEntry)
        }
    }
}

/**
 * Shares an offer photo between the offer card and the offer detail hero, so the photo
 * expands into the detail when navigating and shrinks back on return.
 * No-op outside the nav host or when system animations are off.
 */
@Composable
fun Modifier.sharedOfferImage(offerId: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedVisibilityScope.current ?: return this
    if (!rememberAnimationsEnabled()) return this

    return with(sharedScope) {
        this@sharedOfferImage.sharedElement(
            sharedContentState = rememberSharedContentState(key = "offer-image-$offerId"),
            animatedVisibilityScope = animatedScope
        )
    }
}
