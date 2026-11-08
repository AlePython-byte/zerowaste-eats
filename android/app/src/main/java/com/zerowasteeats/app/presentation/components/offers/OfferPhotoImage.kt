package com.zerowasteeats.app.presentation.components.offers

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.data.merchant.PhotoSelection
import com.zerowasteeats.app.data.merchant.PickedPhoto
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import java.io.File

/** Short and quiet: the photo settles in, it does not perform. */
private const val PHOTO_FADE_MILLIS = 200

/**
 * Draws an offer's photo, or the one being edited. Photos from the server, and photos on the
 * device, are loaded by Coil off the main thread at the size they are shown; until they are
 * ready, when there is none, or when one cannot be loaded, a plain surface of the theme fills
 * their place. They fade in only when the system animations are on.
 */
@Composable
fun OfferPhotoImage(
    photo: PhotoSelection?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    when (photo) {
        is OfferPhoto.Bundled -> Image(
            painter = painterResource(photo.drawableRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
        is OfferPhoto.Remote -> LoadedPhoto(photo.url, modifier, contentDescription, contentScale)
        is PickedPhoto -> LoadedPhoto(File(photo.path), modifier, contentDescription, contentScale)
        null -> Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun LoadedPhoto(
    source: Any,
    modifier: Modifier,
    contentDescription: String?,
    contentScale: ContentScale
) {
    val context = LocalContext.current
    val animate = rememberAnimationsEnabled()
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val request = remember(source, animate) {
        ImageRequest.Builder(context)
            .data(source)
            .crossfade(if (animate) PHOTO_FADE_MILLIS else 0)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        contentScale = contentScale
    )
}
