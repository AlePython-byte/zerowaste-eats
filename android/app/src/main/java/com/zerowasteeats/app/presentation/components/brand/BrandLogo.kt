package com.zerowasteeats.app.presentation.components.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateCream

/**
 * The brand logo. The leaf is a hole in the bag, so it takes the color of whatever is behind
 * the logo. Pass a [contentDescription] when the logo is the only thing naming the app.
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescription: String? = null
) {
    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = contentDescription,
        modifier = modifier.size(size)
    )
}

@Composable
private fun BrandLogoSizes(background: Color) {
    Row(
        modifier = Modifier
            .background(background)
            .padding(AppSpacing.regular),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.regular),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandLogo(size = 24.dp)
        BrandLogo(size = 48.dp)
        BrandLogo(size = 96.dp)
    }
}

@Preview(name = "On cream")
@Composable
private fun BrandLogoCreamPreview() {
    BrandLogoSizes(RescateCream)
}

@Preview(name = "On carbon")
@Composable
private fun BrandLogoCarbonPreview() {
    BrandLogoSizes(RescateCarbon)
}
