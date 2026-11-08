package com.zerowasteeats.app.presentation.components.offers

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.data.merchant.PhotoSelection
import com.zerowasteeats.app.data.merchant.PickedPhoto
import com.zerowasteeats.app.data.photos.PhotoScaling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val FILE_URL_PREFIX = "file://"

/**
 * Draws an offer's photo, or the one being edited. A photo on the device is read from its file
 * off the main thread, sampled down to about the size it is shown at. Until it is ready, when
 * there is no photo, or when the file is gone, a plain surface of the theme fills its place.
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
        is PickedPhoto -> LocalPhoto(photo.path, modifier, contentDescription, contentScale)
        is OfferPhoto.Remote ->
            if (photo.url.startsWith(FILE_URL_PREFIX)) {
                LocalPhoto(
                    photo.url.removePrefix(FILE_URL_PREFIX),
                    modifier,
                    contentDescription,
                    contentScale
                )
            } else {
                PhotoPlaceholder(modifier)
            }
        null -> PhotoPlaceholder(modifier)
    }
}

@Composable
private fun LocalPhoto(
    path: String,
    modifier: Modifier,
    contentDescription: String?,
    contentScale: ContentScale
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val shownPx = with(density) { maxOf(maxWidth, maxHeight).roundToPx() }
        val bitmap by produceState<ImageBitmap?>(null, path, shownPx) {
            value = withContext(Dispatchers.IO) { decodeForDisplay(path, shownPx) }
        }
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            PhotoPlaceholder(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PhotoPlaceholder(modifier: Modifier) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
}

private fun decodeForDisplay(path: String, shownPx: Int): ImageBitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        null
    } else {
        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoScaling.sampleSize(
                bounds.outWidth,
                bounds.outHeight,
                maxSide = shownPx.coerceAtLeast(1)
            )
        }
        BitmapFactory.decodeFile(path, options)?.asImageBitmap()
    }
} catch (_: OutOfMemoryError) {
    null
}
