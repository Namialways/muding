package com.muding.android.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingBallSettingSliderMappingsTest {

    @Test
    fun sizeProgressToDp_mapsEndpointsAndMidpoint() {
        assertEquals(30, floatingBallSizeDpFromProgress(0))
        assertEquals(45, floatingBallSizeDpFromProgress(50))
        assertEquals(60, floatingBallSizeDpFromProgress(100))
    }

    @Test
    fun sizeDpToProgress_mapsBackIntoZeroToHundredRange() {
        assertEquals(0, floatingBallSizeProgressFromDp(30))
        assertEquals(53, floatingBallSizeProgressFromDp(46))
        assertEquals(100, floatingBallSizeProgressFromDp(60))
    }

    @Test
    fun opacityPercentToAlpha_mapsOneToHundredPercentRange() {
        assertEquals(0.01f, floatingBallOpacityFromPercent(1), 0.0001f)
        assertEquals(0.5f, floatingBallOpacityFromPercent(50), 0.0001f)
        assertEquals(1f, floatingBallOpacityFromPercent(100), 0.0001f)
    }

    @Test
    fun alphaToOpacityPercent_mapsStoredAlphaBackToPercent() {
        assertEquals(1, floatingBallOpacityPercentFromAlpha(0.01f))
        assertEquals(92, floatingBallOpacityPercentFromAlpha(0.92f))
        assertEquals(100, floatingBallOpacityPercentFromAlpha(1f))
    }
}
