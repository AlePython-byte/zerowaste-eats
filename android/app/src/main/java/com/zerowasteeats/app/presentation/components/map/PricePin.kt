package com.zerowasteeats.app.presentation.components.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.theme.RescateButter
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateCream
import com.zerowasteeats.app.presentation.theme.RescateDiscountBadge
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/** Scale of the selected pin. */
const val SELECTED_PIN_SCALE = 1.15f

/**
 * Map pin showing an offer's [price]: a pill with a small tip pointing down at the spot.
 * Butter with carbon text; the [selected] pin is dark coral with cream text. The colors are
 * the same in both themes because the pin sits on the map, not on a themed surface.
 *
 * A map marker is drawn into a bitmap, so it cannot be scaled with a graphics layer (it would
 * be clipped). [scale] resizes the whole pin instead, by scaling the density it is laid out in.
 */
@Composable
fun PricePin(
    price: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    val density = LocalDensity.current
    val scaled = Density(density.density * scale, density.fontScale)
    val container = if (selected) RescateDiscountBadge else RescateButter
    val content = if (selected) RescateCream else RescateCarbon

    CompositionLocalProvider(LocalDensity provides scaled) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = container,
                // A thin edge keeps the butter pill apart from a light map
                border = BorderStroke(1.dp, RescateCarbon.copy(alpha = 0.35f)),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = price,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = content,
                    maxLines = 1
                )
            }
            Canvas(modifier = Modifier.size(width = 10.dp, height = 6.dp)) {
                val tip = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                }
                drawPath(tip, container)
            }
        }
    }
}

@Composable
private fun PricePinPreviewContent(background: Color) {
    ZeroWasteEatsTheme {
        Row(
            modifier = Modifier
                .background(background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            PricePin(price = "$8.900", selected = false)
            PricePin(price = "$19.000", selected = true, scale = SELECTED_PIN_SCALE)
        }
    }
}

@Preview(name = "On a light map", showBackground = true, backgroundColor = 0xFFE8E4DC)
@Composable
private fun PricePinLightMapPreview() {
    PricePinPreviewContent(Color(0xFFE8E4DC))
}

@Preview(name = "On a dark map", showBackground = true, backgroundColor = 0xFF1E1A16)
@Composable
private fun PricePinDarkMapPreview() {
    PricePinPreviewContent(Color(0xFF1E1A16))
}
