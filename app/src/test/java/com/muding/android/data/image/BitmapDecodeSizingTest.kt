package com.muding.android.data.image

import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapDecodeSizingTest {

    @Test
    fun `keeps full size when source already fits target`() {
        val sampleSize = BitmapDecodeSizing.calculateInSampleSize(
            width = 1080,
            height = 2400,
            targetWidth = 2160,
            targetHeight = 4800
        )

        assertEquals(1, sampleSize)
    }

    @Test
    fun `samples large images by powers of two until close to target`() {
        val sampleSize = BitmapDecodeSizing.calculateInSampleSize(
            width = 8000,
            height = 6000,
            targetWidth = 2160,
            targetHeight = 4800
        )

        assertEquals(2, sampleSize)
    }

    @Test
    fun `crop preview target allows detail without decoding far beyond screen`() {
        val target = BitmapDecodeSizing.cropPreviewTarget(
            screenWidthPx = 1080,
            screenHeightPx = 2400
        )

        assertEquals(2160, target.widthPx)
        assertEquals(4800, target.heightPx)
    }
}
