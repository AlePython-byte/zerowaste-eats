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
import com.zerowasteeats.app.data.photos.PhotoScaling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Draws an offer's photo. A device photo is read from its file off the main thread, sampled
 * down to about the size it is shown at; until then, or if the file is gone, a plain surface
 * of the theme fills its place.
 */
@Composable
fun OfferPhotoImage(
    photo: OfferPhoto,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    when (photo) {
        is OfferPhoto.Stock -> Image(
            painter = painterResource(photo.drawableRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
        is OfferPhoto.Device -> BoxWithConstraints(modifier = modifier) {
            val density = LocalDensity.current
            val shownPx = with(density) { maxOf(maxWidth, maxHeight).roundToPx() }
            val bitmap by produceState<ImageBitmap?>(null, photo.path, shownPx) {
                value = withContext(Dispatchers.IO) { decodeForDisplay(photo.path, shownPx) }
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
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
