package com.muding.android.presentation.crop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropOverlayChromeLayoutTest {

    @Test
    fun actionButtonTouchTarget_meetsMobileMinimum() {
        assertTrue(CropOverlayChromeLayout.actionButtonTouchTargetDp >= 44)
    }

    @Test
    fun hintMaxWidth_reservesSpaceForCornerActions() {
        val width = CropOverlayChromeLayout.hintMaxWidthDp(screenWidthDp = 360)

        assertEquals(200, width)
    }
}
