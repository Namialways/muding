package com.muding.android.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotBitmapCopyPlanTest {

    @Test
    fun `does not require cropped copy when image row has no padding`() {
        val plan = ScreenshotBitmapCopyPlan.from(
            width = 1080,
            pixelStride = 4,
            rowStride = 4320
        )

        assertEquals(1080, plan.intermediateWidth)
        assertFalse(plan.requiresCroppedCopy)
    }

    @Test
    fun `requires cropped copy when row padding expands bitmap width`() {
        val plan = ScreenshotBitmapCopyPlan.from(
            width = 1080,
            pixelStride = 4,
            rowStride = 4352
        )

        assertEquals(1088, plan.intermediateWidth)
        assertTrue(plan.requiresCroppedCopy)
    }
}
