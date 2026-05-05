package com.muding.android.feature.pin.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinOverlaySizingTest {

    @Test
    fun `near fullscreen pin starts smaller than the screen`() {
        val size = PinOverlaySizing.calculateBaseContentSize(
            bitmapWidth = 1080,
            bitmapHeight = 2400,
            requestedWidth = 1080,
            requestedHeight = 2400,
            screenWidth = 1080,
            screenHeight = 2400
        )

        assertEquals(842, size.first)
        assertEquals(1872, size.second)
        assertTrue(size.first < 1080)
        assertTrue(size.second < 2400)
    }

    @Test
    fun `full width screenshot pin starts visually distinct from the screen`() {
        val size = PinOverlaySizing.calculateBaseContentSize(
            bitmapWidth = 1080,
            bitmapHeight = 800,
            requestedWidth = 1080,
            requestedHeight = 800,
            screenWidth = 1080,
            screenHeight = 2400
        )

        assertEquals(886, size.first)
        assertEquals(656, size.second)
    }

    @Test
    fun `small pin keeps its requested display size`() {
        val size = PinOverlaySizing.calculateBaseContentSize(
            bitmapWidth = 420,
            bitmapHeight = 360,
            requestedWidth = 420,
            requestedHeight = 360,
            screenWidth = 1080,
            screenHeight = 2400
        )

        assertEquals(420, size.first)
        assertEquals(360, size.second)
    }

    @Test
    fun `oversized pin remains covering the screen instead of parking as a sliver`() {
        val position = PinOverlaySizing.clampOverlayPosition(
            currentX = 1032,
            currentY = 2352,
            overlayWidth = 3000,
            overlayHeight = 4000,
            screenLeft = 0,
            screenTop = 0,
            screenRight = 1080,
            screenBottom = 2400,
            minVisiblePx = 48
        )

        assertEquals(0, position.first)
        assertEquals(0, position.second)
    }
}
