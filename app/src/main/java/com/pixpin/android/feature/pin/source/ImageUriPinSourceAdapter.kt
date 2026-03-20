package com.pixpin.android.feature.pin.source

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.pixpin.android.core.model.PinImageAsset
import com.pixpin.android.core.model.PinSource
import com.pixpin.android.core.model.PinSourcePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageUriPinSourceAdapter(
    private val context: Context
) : PinSourceAdapter {

    override fun supports(source: PinSource): Boolean {
        return source.payload is PinSourcePayload.ImageUri
    }

    override suspend fun resolve(source: PinSource): PinImageAsset {
        val uriString = (source.payload as PinSourcePayload.ImageUri).uri
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
}
