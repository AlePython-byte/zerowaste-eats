package com.zerowasteeats.app.presentation.components.brand

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.common.LOGO_TOTAL_MILLIS
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.theme.RescateButter
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateCream
import com.zerowasteeats.app.presentation.theme.RescateDiscountBadge
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import kotlinx.coroutines.launch

/**
 * The brand logo, drawn from [LogoPaths]. With [animated] it plays once: the bag springs in,
 * the coral handle draws itself from end to end, the leaf grows from its base and the heart
 * beats a single time (about 1.2 s). Without it, when system animations are off and in
 * previews, it is the finished logo, the same picture as the `ic_logo` vector.
 *
 * The leaf is painted in [cutoutColor], so it looks like a hole in the bag: pass the color
 * behind the logo. Once played it stays complete across rotations.
 */
@Composable
fun ZeroWasteLogo(
    animated: Boolean,
    modifier: Modifier = Modifier,
    cutoutColor: Color = MaterialTheme.colorScheme.background
) {
    val shapes = remember { LogoShapes() }
    val playAnimation = animated && rememberAnimationsEnabled() && !LocalInspectionMode.current
    // Survives rotation and process death, so coming back never replays the intro
    var played by rememberSaveable { mutableStateOf(false) }
    val animate = playAnimation && !played

    val bagScale = remember { Animatable(if (animate) 0f else 1f) }
    val timeline = remember { Animatable(if (animate) 0f else LOGO_TOTAL_MILLIS.toFloat()) }

    LaunchedEffect(animate) {
        if (animate) {
            // The bag has a clock of its own; the timeline drives the other three steps
            launch {
                bagScale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
            timeline.animateTo(
                LOGO_TOTAL_MILLIS.toFloat(),
                tween(LOGO_TOTAL_MILLIS, easing = LinearEasing)
            )
            played = true
        }
    }

    Canvas(modifier = modifier.aspectRatio(1f)) {
        // Read in the draw phase: the animation redraws, it does not recompose
        val frame = LogoFrame.at(timeline.value)
        drawLogo(shapes, bagScale.value, frame, cutoutColor)
    }
}

/** The logo's paths as Compose paths, built once. */
private class LogoShapes {
    val bag: Path = addPathNodes("${LogoPaths.BAG} ${LogoPaths.EYELETS}").toPath()
        .apply { fillType = PathFillType.EvenOdd }
    val leaf: Path = LogoPaths.leaf.toPath()
    val handle: Path = LogoPaths.handle.toPath()
    val handleMeasure = PathMeasure().apply { setPath(handle, false) }
    val handleLength: Float = handleMeasure.length
    val handleSegment = Path()
}

private val LeafBase = Offset(41.5f, 88f)
private val BagCentre = Offset(54f, 68f)
private val HeartCentre = Offset(54f, 30f)

private fun DrawScope.drawLogo(shapes: LogoShapes, bagScale: Float, frame: LogoFrame, cutoutColor: Color) {
    val unit = size.minDimension / LogoPaths.VIEWPORT
    scale(unit, unit, pivot = Offset.Zero) {
        // 1. The bag
        scale(bagScale, bagScale, pivot = BagCentre) {
            drawPath(shapes.bag, RescateButter, style = Fill)
        }

        // 3. The leaf, in the color behind the logo so it reads as a cut-out
        if (frame.leafProgress > 0f && bagScale > 0f) {
            scale(bagScale, bagScale, pivot = BagCentre) {
                rotate(LogoFrame.LEAF_START_ANGLE * (1f - frame.leafProgress), pivot = LeafBase) {
                    scale(frame.leafProgress, frame.leafProgress, pivot = LeafBase) {
                        drawPath(shapes.leaf, cutoutColor, style = Fill)
                    }
                }
            }
        }

        // 2. The handle, cut along its length; 4. beating once at the end
        if (frame.handleProgress > 0f) {
            shapes.handleSegment.rewind()
            shapes.handleMeasure.getSegment(
                0f,
                shapes.handleLength * frame.handleProgress,
                shapes.handleSegment,
                startWithMoveTo = true
            )
            scale(frame.heartScale, frame.heartScale, pivot = HeartCentre) {
                drawPath(
                    shapes.handleSegment,
                    RescateDiscountBadge,
                    style = Stroke(
                        width = LogoPaths.HANDLE_STROKE_WIDTH,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
private fun ZeroWasteLogoPreviewContent(background: Color, dark: Boolean) {
    ZeroWasteEatsTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
        Box(modifier = Modifier.padding(16.dp)) {
            ZeroWasteLogo(
                animated = false,
                modifier = Modifier.size(96.dp),
                cutoutColor = background
            )
        }
    }
}

@Preview(name = "Light", backgroundColor = 0xFFF7F1E8, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ZeroWasteLogoLightPreview() {
    ZeroWasteLogoPreviewContent(RescateCream, dark = false)
}

@Preview(name = "Dark", backgroundColor = 0xFF14120F, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ZeroWasteLogoDarkPreview() {
    ZeroWasteLogoPreviewContent(RescateCarbon, dark = true)
}
