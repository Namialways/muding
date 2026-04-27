package com.muding.android.data.image

object BitmapDecodeSizing {

    data class DecodeTarget(
        val widthPx: Int,
        val heightPx: Int
    )

    fun calculateInSampleSize(
        width: Int,
        height: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        if (width <= 0 || height <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return 1
        }
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth > targetWidth * 2 || currentHeight > targetHeight * 2) {
            sampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    fun cropPreviewTarget(
        screenWidthPx: Int,
        screenHeightPx: Int
    ): DecodeTarget {
        return DecodeTarget(
            widthPx = (screenWidthPx * 2).coerceAtLeast(1440),
            heightPx = (screenHeightPx * 2).coerceAtLeast(1440)
        )
    }
}
