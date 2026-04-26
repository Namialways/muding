package com.muding.android.service

import com.muding.android.domain.usecase.FloatingBallLastPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingBallInitialPositioningTest {

    @Test
    fun resolve_placesBallAtRightCenterWhenSavedPositionIsMissing() {
        val position = FloatingBallInitialPositioning.resolve(
            screenWidth = 1080,
            screenHeight = 1920,
            ballSizePx = 100,
            savedPosition = null
        )

        assertEquals(980, position.x)
        assertEquals(910, position.y)
    }

    @Test
    fun resolve_restoresSavedPositionWhenAvailable() {
        val position = FloatingBallInitialPositioning.resolve(
            screenWidth = 1080,
            screenHeight = 1920,
            ballSizePx = 100,
            savedPosition = FloatingBallLastPosition(x = 240, y = 680)
        )

        assertEquals(240, position.x)
        assertEquals(680, position.y)
    }

    @Test
    fun resolve_clampsSavedPositionInsideScreen() {
        val position = FloatingBallInitialPositioning.resolve(
            screenWidth = 1080,
            screenHeight = 1920,
            ballSizePx = 100,
            savedPosition = FloatingBallLastPosition(x = 1400, y = -80)
        )

        assertEquals(980, position.x)
        assertEquals(0, position.y)
    }
}
