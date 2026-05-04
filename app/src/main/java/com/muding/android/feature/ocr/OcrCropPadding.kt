package com.muding.android.feature.ocr

import androidx.compose.ui.geometry.Rect
import kotlin.math.min

internal object OcrCropPadding {

    fun expand(
        cropRect: Rect,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Rect {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) {
            return cropRect
        }
        val shortestSide = min(cropRect.width, cropRect.height).coerceAtLeast(0f)
        val paddingPx = (shortestSide * PADDING_RATIO)
            .coerceAtLeast(MIN_PADDING_PX)
            .coerceAtMost(MAX_PADDING_PX)
        return Rect(
            left = (cropRect.left - paddingPx).coerceAtLeast(0f),
            top = (cropRect.top - paddingPx).coerceAtLeast(0f),
            right = (cropRect.right + paddingPx).coerceAtMost(bitmapWidth.toFloat()),
            bottom = (cropRect.bottom + paddingPx).coerceAtMost(bitmapHeight.toFloat())
        )
    }

    private const val PADDING_RATIO = 0.04f
    private const val MIN_PADDING_PX = 6f
    private const val MAX_PADDING_PX = 24f
}
