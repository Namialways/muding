package com.muding.android.feature.pin.source

import org.junit.Assert.assertEquals
import org.junit.Test

class PinDecodeTargetSizingTest {

    @Test
    fun galleryImportTarget_isBoundedToPinDisplayScale() {
        val target = PinDecodeTargetSizing.galleryImportTarget(
            screenWidthPx = 1080,
            screenHeightPx = 2400
        )

        assertEquals(1231, target.widthPx)
        assertEquals(1296, target.heightPx)
    }

    @Test
    fun overlayTarget_prefersRequestedInitialSize() {
        val target = PinDecodeTargetSizing.overlayDecodeTarget(
            screenWidthPx = 1080,
            screenHeightPx = 2400,
            preferredWidthPx = 820,
            preferredHeightPx = 540
        )

        assertEquals(820, target.widthPx)
        assertEquals(1080, target.heightPx)
    }

    @Test
    fun overlayTarget_fallsBackToBoundedScreenBasedSize() {
        val target = PinDecodeTargetSizing.overlayDecodeTarget(
            screenWidthPx = 1080,
            screenHeightPx = 2400,
            preferredWidthPx = null,
            preferredHeightPx = null
        )

        assertEquals(1512, target.widthPx)
        assertEquals(1440, target.heightPx)
    }
}
