package com.muding.android.data.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

class UriBitmapDecoder(
    private val contentResolver: ContentResolver
) {

    fun decodeSampled(
        uri: Uri,
        target: BitmapDecodeSizing.DecodeTarget
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = BitmapDecodeSizing.calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                targetWidth = target.widthPx,
                targetHeight = target.heightPx
            )
        }
        return contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }
    }
}
