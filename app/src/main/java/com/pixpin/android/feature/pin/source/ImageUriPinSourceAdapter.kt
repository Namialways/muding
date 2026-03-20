package com.pixpin.android.feature.pin.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pixpin.android.core.model.PinImageAsset
import com.pixpin.android.core.model.PinSource
import com.pixpin.android.core.model.PinSourcePayload
import com.pixpin.android.core.model.PinSourceType
import com.pixpin.android.data.image.CachedImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

class ImageUriPinSourceAdapter(
    private val context: Context,
    private val cachedImageRepository: CachedImageRepository
) : PinSourceAdapter {

    override fun supports(source: PinSource): Boolean {
        return source.payload is PinSourcePayload.ImageUri
    }

    override suspend fun resolve(source: PinSource): PinImageAsset {
        val uriString = (source.payload as PinSourcePayload.ImageUri).uri
        if (source.type == PinSourceType.GALLERY_IMAGE) {
            return withContext(Dispatchers.IO) {
                importGalleryImage(uriString)
            }
        }
        val dimensions = withContext(Dispatchers.IO) {
            readImageDimensions(uriString)
        }
        return PinImageAsset(
            uri = uriString,
            initialDisplayWidthPx = dimensions.first,
            initialDisplayHeightPx = dimensions.second
        )
    }

    private fun readImageDimensions(uriString: String): Pair<Int?, Int?> {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
            Pair(
                options.outWidth.takeIf { it > 0 },
                options.outHeight.takeIf { it > 0 }
            )
        } catch (_: Exception) {
            Pair(null, null)
        }
    }

    private fun importGalleryImage(uriString: String): PinImageAsset {
        val bitmap = context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalStateException("Failed to decode gallery image")
        return try {
            val fittedSize = fitToScreen(bitmap.width, bitmap.height)
            val cachedUri = cachedImageRepository.writePngToCache(bitmap, "imports", "gallery_pin")
            PinImageAsset(
                uri = cachedUri.toString(),
                initialDisplayWidthPx = fittedSize.first,
                initialDisplayHeightPx = fittedSize.second
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun fitToScreen(width: Int, height: Int): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        val maxWidth = (metrics.widthPixels * 0.76f).roundToInt().coerceAtLeast(1)
        val maxHeight = (metrics.heightPixels * 0.6f).roundToInt().coerceAtLeast(1)
        if (width <= maxWidth && height <= maxHeight) {
            return Pair(width, height)
        }
        val scale = min(
            maxWidth / width.toFloat(),
            maxHeight / height.toFloat()
        ).coerceAtMost(1f)
        return Pair(
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1)
        )
    }
}
