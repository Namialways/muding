package com.pixpin.android.feature.capture

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt

class BitmapCropper {

    fun crop(bitmap: Bitmap, cropRectInBitmap: Rect): Bitmap {
        val left = cropRectInBitmap.left.roundToInt().coerceIn(0, bitmap.width - 1)
        val top = cropRectInBitmap.top.roundToInt().coerceIn(0, bitmap.height - 1)
        val right = cropRectInBitmap.right.roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = cropRectInBitmap.bottom.roundToInt().coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }
}
