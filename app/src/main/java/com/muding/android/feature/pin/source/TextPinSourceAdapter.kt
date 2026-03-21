package com.muding.android.feature.pin.source

import android.graphics.Bitmap
import com.muding.android.core.model.PinImageAsset
import com.muding.android.core.model.PinSource
import com.muding.android.core.model.PinSourcePayload
import com.muding.android.core.model.PinSourceType
import com.muding.android.data.image.CachedImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TextPinSourceAdapter(
    private val cachedImageRepository: CachedImageRepository,
    private val textBitmapRenderer: TextBitmapRenderer
) : PinSourceAdapter {

    override fun supports(source: PinSource): Boolean {
        return source.payload is PinSourcePayload.Text
    }

    override suspend fun resolve(source: PinSource): PinImageAsset {
        val payload = source.payload as PinSourcePayload.Text
        val bitmap = withContext(Dispatchers.Default) {
            textBitmapRenderer.render(payload.text)
        }
        return try {
            persistBitmap(source.type, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun persistBitmap(sourceType: PinSourceType, bitmap: Bitmap): PinImageAsset {
        val prefix = when (sourceType) {
            PinSourceType.CLIPBOARD_TEXT -> "clipboard_text_pin"
            PinSourceType.OCR_TEXT -> "ocr_text_pin"
            else -> "text_pin"
        }
        val uri = withContext(Dispatchers.IO) {
            cachedImageRepository.writePngToCache(bitmap, "text_pins", prefix)
        }
        return PinImageAsset(
            uri = uri.toString(),
            initialDisplayWidthPx = bitmap.width,
            initialDisplayHeightPx = bitmap.height
        )
    }
}
