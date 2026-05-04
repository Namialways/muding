package com.muding.android.feature.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrImagePreprocessingPlanTest {

    @Test
    fun from_smallCropUpscalesAndIncludesEnhancedVariant() {
        val plan = OcrImagePreprocessingPlan.from(width = 180, height = 90)

        assertEquals(3f, plan.scaleFactor, 0.001f)
        assertTrue(plan.includeEnhancedVariant)
    }

    @Test
    fun from_mediumCropUsesModerateUpscale() {
        val plan = OcrImagePreprocessingPlan.from(width = 640, height = 260)

        assertEquals(2f, plan.scaleFactor, 0.001f)
        assertTrue(plan.includeEnhancedVariant)
    }

    @Test
    fun from_largeCropAvoidsExtraWork() {
        val plan = OcrImagePreprocessingPlan.from(width = 1440, height = 1080)

        assertEquals(1f, plan.scaleFactor, 0.001f)
        assertFalse(plan.includeEnhancedVariant)
    }

    @Test
    fun from_doesNotUpscaleBeyondLongEdgeLimit() {
        val plan = OcrImagePreprocessingPlan.from(width = 4000, height = 180)

        assertEquals(1f, plan.scaleFactor, 0.001f)
        assertTrue(plan.includeEnhancedVariant)
    }
}
