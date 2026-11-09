package com.zerowasteeats.app.presentation.components.common

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * False when the user turned system animations off (animator duration scale 0).
 * Decorative effects check this and skip themselves.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

/**
 * The app's motion language. Screens and components take their durations, easings and springs
 * from here instead of writing numbers of their own, so everything moves with the same rhythm.
 *
 * - Durations grow with the distance and the size of what moves: [FAST_MILLIS] for small state
 *   changes (colors, icons), [SHORT_MILLIS] for fades and reveals, [MEDIUM_MILLIS] for screens,
 *   sheets and entrances, [LONG_MILLIS] for celebrations.
 * - [Emphasized] decelerates hard, for what comes in; [EmphasizedAccelerate] speeds up, for what
 *   leaves; [Standard] is for what changes in place.
 * - Springs answer touch: [pressSpring] never bounces, [bounceSpring] and [celebrateSpring]
 *   bounce a little, only to reward an action (favorite, reservation done).
 *
 * Everything that uses these still checks [rememberAnimationsEnabled] and snaps when it is off.
 */
object AppMotion {
    const val FAST_MILLIS = 150
    const val SHORT_MILLIS = 200
    const val MEDIUM_MILLIS = 300
    const val LONG_MILLIS = 500

    /** Material 3 emphasized decelerate: for content that enters. */
    val Emphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Material 3 emphasized accelerate: for content that leaves. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** For content that changes in place. */
    val Standard: Easing = FastOutSlowInEasing

    /** Follows a finger: quick, no overshoot (card press). */
    fun <T> pressSpring(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    /** A small, lively overshoot: a toggle that answers (heart, selected map pin). */
    fun <T> bounceSpring(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

    /** A selection that pops out a little (map pin). */
    fun <T> selectSpring(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    /** A slow, generous bounce for an achievement (reservation check, logo). */
    fun <T> celebrateSpring(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

    /** Settles calmly into place with no overshoot (pickup code reveal). */
    fun <T> settleSpring(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Back to rest after a [bounceSpring]. */
    fun <T> restSpring(): SpringSpec<T> = spring()
}


/**
 * Modifier for an offer card inside a LazyColumn: it glides to its new position when the
 * list is filtered or reordered, and fades in with a slight rise the first time its id
 * shows up. [seen] is owned by the screen so cards scrolled back into view do not replay it.
 */
// The scope receiver is needed to reach animateItem, so this cannot extend Modifier
@Suppress("ModifierFactoryExtensionFunction")
@Composable
fun LazyItemScope.offerListItem(seen: MutableSet<String>, id: String): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    return Modifier
        .animateItem(
            fadeInSpec = null, // the entrance already fades the card in
            placementSpec = if (animationsEnabled) offerPlacementSpec() else null
        )
        .offerEntrance(seen, id, animationsEnabled)
}

/** Same as [offerListItem], for the cards of a LazyVerticalGrid. */
@Suppress("ModifierFactoryExtensionFunction")
@Composable
fun LazyGridItemScope.offerGridItem(seen: MutableSet<String>, id: String): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    return Modifier
        .animateItem(
            fadeInSpec = null,
            placementSpec = if (animationsEnabled) offerPlacementSpec() else null
        )
        .offerEntrance(seen, id, animationsEnabled)
}

private fun offerPlacementSpec(): FiniteAnimationSpec<IntOffset> =
    tween(AppMotion.MEDIUM_MILLIS, easing = AppMotion.Standard)

/** Fade-in with a slight rise, played once per id while it is not in [seen]. */
@Composable
private fun Modifier.offerEntrance(
    seen: MutableSet<String>,
    id: String,
    animationsEnabled: Boolean
): Modifier {
    val alreadySeen = remember { id in seen }
    val progress = remember { Animatable(if (alreadySeen || !animationsEnabled) 1f else 0f) }

    LaunchedEffect(Unit) {
        seen.add(id)
        if (progress.value < 1f) {
            progress.animateTo(1f, tween(AppMotion.MEDIUM_MILLIS, easing = AppMotion.Standard))
        }
    }

    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 16.dp.toPx()
    }
}

/**
 * Timeline of the animated logo, in milliseconds from its start (about 1.2 s in all). The bag
 * springs in on its own clock from 0; the other three steps follow this timeline, each starting
 * while the one before is still settling so the sequence reads as one movement.
 */
internal const val LOGO_HANDLE_START_MILLIS = AppMotion.MEDIUM_MILLIS
internal const val LOGO_HANDLE_MILLIS = AppMotion.MEDIUM_MILLIS + 100
internal const val LOGO_LEAF_START_MILLIS = 2 * AppMotion.MEDIUM_MILLIS
internal const val LOGO_LEAF_MILLIS = AppMotion.MEDIUM_MILLIS + 50
internal const val LOGO_HEART_START_MILLIS = LOGO_LEAF_START_MILLIS + LOGO_LEAF_MILLIS
internal const val LOGO_HEART_MILLIS = AppMotion.MEDIUM_MILLIS - 50
internal const val LOGO_TOTAL_MILLIS = LOGO_HEART_START_MILLIS + LOGO_HEART_MILLIS

/** One slow breath of the halo behind the logo: out and back. */
internal const val HALO_BREATH_MILLIS = 4000

/** Pause between one section's entrance and the next in [staggeredEntrance]. */
internal const val STAGGER_MILLIS = 60

/**
 * Fades the content in while it rises [rise] to its place, once, when it first shows, after
 * waiting [delayMillis]. Static from the start when [enabled] is false, when system animations
 * are off, and in previews, where effects do not run and the content would stay invisible.
 */
@Composable
fun Modifier.entranceRise(
    rise: Dp = 8.dp,
    delayMillis: Int = 0,
    enabled: Boolean = true
): Modifier {
    val animate = enabled && rememberAnimationsEnabled() && !LocalInspectionMode.current
    val progress = remember { Animatable(if (animate) 0f else 1f) }

    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            progress.animateTo(
                1f,
                tween(AppMotion.MEDIUM_MILLIS, delayMillis = delayMillis, easing = AppMotion.Standard)
            )
        }
    }

    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * rise.toPx()
    }
}

/**
 * [entranceRise] for the [index]-th section of a screen: each one starts [STAGGER_MILLIS] after
 * the one before, so they appear one after another. Later ones start together, so a long
 * screen does not keep the last section waiting.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int, enabled: Boolean = true): Modifier =
    entranceRise(delayMillis = index.coerceIn(0, MAX_STAGGERED_SECTIONS) * STAGGER_MILLIS, enabled = enabled)

private const val MAX_STAGGERED_SECTIONS = 6

/**
 * How far a screen travels on the shared axis: enough to say which way the person is going,
 * short enough that the screen never sweeps across the display.
 */
private val SHARED_AXIS_DISTANCE = 30.dp

/** The screen coming in waits this long, so it fades in over a screen that is already fading out. */
private const val FADE_THROUGH_DELAY_MILLIS = AppMotion.FAST_MILLIS / 2

/**
 * The screen transitions of the app, along a shared horizontal axis. Going forward, the new
 * screen comes in a short way from the end side while the old one drifts toward the start and
 * fades out quickly; going back mirrors it. Sibling screens (the bottom bar tabs) are neither
 * deeper nor shallower than each other, so they only fade through.
 *
 * The predictive back ones play while the person drags the back gesture, scrubbed by the
 * finger instead of by time. They move at an even pace with no delay, so the screen tracks the
 * finger from the first pixel, and they never scale: the leaving screen slides away from the
 * edge the gesture started at while the one underneath comes in from the other side.
 *
 * All of them are [EnterTransition.None] / [ExitTransition.None] when the system animations are
 * off, so screens swap at once.
 */
@Immutable
class ScreenTransitions internal constructor(
    val forwardEnter: EnterTransition,
    val forwardExit: ExitTransition,
    val backEnter: EnterTransition,
    val backExit: ExitTransition,
    val siblingEnter: EnterTransition,
    val siblingExit: ExitTransition,
    private val predictiveBackEnterFromLeft: EnterTransition,
    private val predictiveBackExitFromLeft: ExitTransition,
    private val predictiveBackEnterFromRight: EnterTransition,
    private val predictiveBackExitFromRight: ExitTransition,
    val predictiveSiblingEnter: EnterTransition,
    val predictiveSiblingExit: ExitTransition
) {
    /** The screen underneath, while a back gesture from the right edge (or the left) is dragged. */
    fun predictiveBackEnter(fromRightEdge: Boolean): EnterTransition =
        if (fromRightEdge) predictiveBackEnterFromRight else predictiveBackEnterFromLeft

    /** The screen being left, while a back gesture from the right edge (or the left) is dragged. */
    fun predictiveBackExit(fromRightEdge: Boolean): ExitTransition =
        if (fromRightEdge) predictiveBackExitFromRight else predictiveBackExitFromLeft
}

/** [ScreenTransitions] for the current layout direction, density and animation setting. */
@Composable
fun rememberScreenTransitions(): ScreenTransitions {
    val enabled = rememberAnimationsEnabled() && !LocalInspectionMode.current
    val distance = with(LocalDensity.current) { SHARED_AXIS_DISTANCE.roundToPx() }
    // "Forward" comes from the right in left-to-right languages and from the left otherwise
    val towardEnd = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -distance else distance
    return remember(enabled, towardEnd) { screenTransitions(enabled, towardEnd) }
}

/**
 * Builds the [ScreenTransitions]. [towardEnd] is the slide distance in pixels, signed so that
 * a positive offset points to the end side of the layout.
 */
internal fun screenTransitions(enabled: Boolean, towardEnd: Int): ScreenTransitions {
    if (!enabled) {
        return ScreenTransitions(
            forwardEnter = EnterTransition.None,
            forwardExit = ExitTransition.None,
            backEnter = EnterTransition.None,
            backExit = ExitTransition.None,
            siblingEnter = EnterTransition.None,
            siblingExit = ExitTransition.None,
            predictiveBackEnterFromLeft = EnterTransition.None,
            predictiveBackExitFromLeft = ExitTransition.None,
            predictiveBackEnterFromRight = EnterTransition.None,
            predictiveBackExitFromRight = ExitTransition.None,
            predictiveSiblingEnter = EnterTransition.None,
            predictiveSiblingExit = ExitTransition.None
        )
    }

    val slide = tween<IntOffset>(AppMotion.MEDIUM_MILLIS, easing = AppMotion.Emphasized)
    // The old screen is gone early, so the two never read as stacked text
    val vanish = fadeOut(tween(AppMotion.FAST_MILLIS, easing = AppMotion.EmphasizedAccelerate))
    val appear = fadeIn(
        tween(
            durationMillis = AppMotion.MEDIUM_MILLIS - FADE_THROUGH_DELAY_MILLIS,
            delayMillis = FADE_THROUGH_DELAY_MILLIS,
            easing = AppMotion.Emphasized
        )
    )
    val siblingAppear = fadeIn(
        tween(
            durationMillis = AppMotion.SHORT_MILLIS,
            delayMillis = FADE_THROUGH_DELAY_MILLIS,
            easing = AppMotion.Emphasized
        )
    )

    // Scrubbed by the finger: an even pace and no delay, so the progress of the gesture and of
    // the screens stay the same. The edges are physical (left, right), not start and end.
    val follow = tween<IntOffset>(AppMotion.MEDIUM_MILLIS, easing = LinearEasing)
    val followAppear = fadeIn(tween(AppMotion.MEDIUM_MILLIS, easing = LinearEasing))
    val followVanish = fadeOut(tween(AppMotion.MEDIUM_MILLIS, easing = LinearEasing))
    val distance = abs(towardEnd)

    return ScreenTransitions(
        forwardEnter = slideInHorizontally(slide) { towardEnd } + appear,
        forwardExit = slideOutHorizontally(slide) { -towardEnd } + vanish,
        backEnter = slideInHorizontally(slide) { -towardEnd } + appear,
        backExit = slideOutHorizontally(slide) { towardEnd } + vanish,
        siblingEnter = siblingAppear,
        siblingExit = vanish,
        // From the left edge the finger moves right: the leaving screen goes right with it
        predictiveBackEnterFromLeft = slideInHorizontally(follow) { -distance } + followAppear,
        predictiveBackExitFromLeft = slideOutHorizontally(follow) { distance } + followVanish,
        predictiveBackEnterFromRight = slideInHorizontally(follow) { distance } + followAppear,
        predictiveBackExitFromRight = slideOutHorizontally(follow) { -distance } + followVanish,
        predictiveSiblingEnter = followAppear,
        predictiveSiblingExit = followVanish
    )
}
