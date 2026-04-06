package com.muding.android.service

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageHintPositioningTest {

    @Test
    fun place_prefersRightSideWhenThereIsEnoughSpace() {
        val position = UsageHintPositioning.place(
            screenWidth = 1080,
            screenHeight = 1920,
            anchor = UsageHintPositioning.AnchorBounds(
                left = 120,
                top = 240,
                right = 200,
                bottom = 320
            ),
            hintWidth = 280,
            hintHeight = 120,
            margin = 24
        )

        assertEquals(224, position.x)
        assertEquals(220, position.y)
    }

    @Test
    fun place_fallsBackInsideScreenWhenRightSideWouldOverflow() {
        val position = UsageHintPositioning.place(
            screenWidth = 1080,
            screenHeight = 1920,
            anchor = UsageHintPositioning.AnchorBounds(
                left = 940,
                top = 1780,
                right = 1010,
                bottom = 1850
            ),
            hintWidth = 260,
            hintHeight = 140,
            margin = 24
        )

        assertEquals(656, position.x)
        assertEquals(1745, position.y)
    }
}
