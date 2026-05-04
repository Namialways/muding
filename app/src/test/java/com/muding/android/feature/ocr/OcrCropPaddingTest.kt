package com.muding.android.feature.ocr

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrCropPaddingTest {

    @Test
    fun expand_addsSmallSafetyMarginAroundSelection() {
        val expanded = OcrCropPadding.expand(
            cropRect = Rect(left = 100f, top = 120f, right = 300f, bottom = 220f),
            bitmapWidth = 1080,
            bitmapHeight = 2400
        )

        assertEquals(94f, expanded.left, 0.001f)
        assertEquals(114f, expanded.top, 0.001f)
        assertEquals(306f, expanded.right, 0.001f)
        assertEquals(226f, expanded.bottom, 0.001f)
    }

    @Test
    fun expand_clampsToBitmapBounds() {
        val expanded = OcrCropPadding.expand(
            cropRect = Rect(left = 2f, top = 3f, right = 50f, bottom = 40f),
            bitmapWidth = 80,
            bitmapHeight = 60
        )

        assertEquals(0f, expanded.left, 0.001f)
        assertEquals(0f, expanded.top, 0.001f)
        assertEquals(56f, expanded.right, 0.001f)
        assertEquals(46f, expanded.bottom, 0.001f)
    }
}
