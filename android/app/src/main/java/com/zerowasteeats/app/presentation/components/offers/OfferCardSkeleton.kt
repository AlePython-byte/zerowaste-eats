package com.zerowasteeats.app.presentation.components.offers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.common.skeletonShimmer
import com.zerowasteeats.app.presentation.theme.AppSpacing

/**
 * The shape of an [OfferCard] while the offers load: the card and the lines of text at its
 * bottom, in the theme's quiet surface color. Hidden from screen readers; the screen announces
 * the loading instead.
 */
@Composable
fun OfferCardSkeleton(modifier: Modifier = Modifier) {
    SkeletonCard(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
    )
}

/** The shape of a [CompactOfferCard] while the offers load. */
@Composable
fun CompactOfferCardSkeleton(modifier: Modifier = Modifier) {
    SkeletonCard(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
    )
}

@Composable
private fun SkeletonCard(shape: RoundedCornerShape, modifier: Modifier) {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val lineShape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .clearAndSetSemantics {}
            .clip(shape)
            .skeletonShimmer()
            .background(surface)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(AppSpacing.large)
        ) {
            Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(lineShape).background(line))
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(0.8f).height(20.dp).clip(lineShape).background(line))
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(0.5f).height(16.dp).clip(lineShape).background(line))
        }
    }
}
