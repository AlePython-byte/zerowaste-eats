package com.zerowasteeats.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.navigation.AppBottomBar
import com.zerowasteeats.app.presentation.components.navigation.TabItem

/**
 * Height the floating bottom bar covers at the bottom of the content: the bar itself, its
 * margins and the system navigation bar. Zero when the bar is hidden (and in previews).
 *
 * The content of a tab fills the whole area and runs under the bar. A list, grid or column
 * that scrolls adds this to its bottom content padding, so its last item can rise clear of
 * the bar. A floating button or a snackbar uses [aboveBottomBar].
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

/**
 * Lifts a floating button or a snackbar above the bar. A screen's own Scaffold does not know
 * about the bar, which now floats over the content instead of reserving room for it.
 */
@Composable
fun Modifier.aboveBottomBar(): Modifier = padding(bottom = LocalBottomBarInset.current)

/**
 * Counts the screens that want the bar's fade off. A screen that must end above the bar
 * instead of running under it (the map) holds it off with [SuppressBottomBarFade].
 */
@Stable
private class BottomBarFadeState {
    var suppressions by mutableIntStateOf(0)
}

private val LocalBottomBarFade = compositionLocalOf<BottomBarFadeState?> { null }

/**
 * While this is in the composition the bar sits on a plain background instead of a fade, for
 * a screen that stops above the bar (like the map, which must not be covered by a gradient).
 * That screen pads its own bottom with [LocalBottomBarInset].
 */
@Composable
fun SuppressBottomBarFade() {
    val state = LocalBottomBarFade.current
    DisposableEffect(state) {
        state?.let { it.suppressions++ }
        onDispose { state?.let { it.suppressions-- } }
    }
}

/** How far above the bar the fade starts. */
private val FadeHeight = 28.dp

/** The fade is fully opaque this far into the bar's own top margin, before the bar itself. */
private val FadeOverlapIntoBar = 12.dp

/**
 * The frame shared by the customer and merchant spaces: the floating [AppBottomBar] over the
 * content, shown only while [currentRoute] is one of the [tabs].
 *
 * [content] fills the whole area and scrolls under the bar; a soft fade of the theme's
 * background (clear at the top, opaque by the bar) keeps the cut from showing as a line. The
 * bar's real height is measured here, once, and handed down as [LocalBottomBarInset]; this is
 * the only place that knows it.
 */
@Composable
fun TabScaffold(
    tabs: List<TabItem>,
    currentRoute: String?,
    onTabClick: (TabItem) -> Unit,
    accent: Color,
    content: @Composable () -> Unit
) {
    val isBottomBarVisible = tabs.any { it.route == currentRoute }
    val fadeState = remember { BottomBarFadeState() }
    val background = MaterialTheme.colorScheme.background
    val fadeEnabled = fadeState.suppressions == 0

    // Measures the bar first, as Scaffold does, so the content gets the right inset from its
    // very first frame instead of jumping after a second pass
    SubcomposeLayout(modifier = Modifier.fillMaxSize()) { constraints ->
        val fadePx = FadeHeight.roundToPx()
        val barPlaceable = if (isBottomBarVisible) {
            subcompose(TabScaffoldSlot.Bar) {
                BottomBarOverlay(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onTabClick = onTabClick,
                    accent = accent,
                    background = background,
                    fadeEnabled = fadeEnabled
                )
            }.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        } else {
            null
        }
        // The fade above the bar is drawing, not height the content needs to clear
        val fadeInBar = if (fadeEnabled) fadePx else 0
        val inset: Dp = barPlaceable?.let { (it.height - fadeInBar).coerceAtLeast(0).toDp() } ?: 0.dp

        val contentPlaceable = subcompose(TabScaffoldSlot.Content) {
            CompositionLocalProvider(
                LocalBottomBarInset provides inset,
                LocalBottomBarFade provides fadeState
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // The bar's area counts as taken, as it did when the bar reserved it
                        .then(
                            if (isBottomBarVisible) Modifier.consumeWindowInsets(PaddingValues(bottom = inset))
                            else Modifier
                        )
                ) { content() }
            }
        }.first().measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceable.place(0, 0)
            barPlaceable?.place(0, constraints.maxHeight - barPlaceable.height)
        }
    }
}

private enum class TabScaffoldSlot { Content, Bar }

@Composable
private fun BottomBarOverlay(
    tabs: List<TabItem>,
    currentRoute: String?,
    onTabClick: (TabItem) -> Unit,
    accent: Color,
    background: Color,
    fadeEnabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (fadeEnabled) {
                    // Same color at every stop, only the alpha changes, so no grey band shows
                    // between a transparent start and the opaque end
                    val fadeEnd = ((FadeHeight + FadeOverlapIntoBar).toPx() / size.height).coerceIn(0f, 1f)
                    drawRect(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to background.copy(alpha = 0f),
                                fadeEnd * 0.35f to background.copy(alpha = 0.2f),
                                fadeEnd * 0.7f to background.copy(alpha = 0.72f),
                                fadeEnd to background,
                                1f to background
                            )
                        )
                    )
                } else {
                    // The content stops above the bar: a plain backdrop, as the bar always had
                    drawRect(background)
                }
            }
            .padding(top = if (fadeEnabled) FadeHeight else 0.dp)
            .navigationBarsPadding(), // Keep above system nav bar
        contentAlignment = Alignment.BottomCenter
    ) {
        AppBottomBar(
            tabs = tabs,
            selectedRoute = currentRoute,
            onTabClick = onTabClick,
            accent = accent
        )
    }
}
