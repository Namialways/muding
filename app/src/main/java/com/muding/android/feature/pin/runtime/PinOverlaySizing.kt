package com.muding.android.feature.pin.runtime

import kotlin.math.roundToInt

object PinOverlaySizing {

    private const val NEAR_FULLSCREEN_FRACTION = 0.94f
    private const val DISTINCT_INITIAL_WIDTH_FRACTION = 0.82f
    private const val DISTINCT_INITIAL_HEIGHT_FRACTION = 0.78f

    fun calculateBaseContentSize(
        bitmapWidth: Int,
        bitmapHeight: Int,
        requestedWidth: Int?,
        requestedHeight: Int?,
        screenWidth: Int,
        screenHeight: Int
    ): Pair<Int, Int> {
        val safeScreenWidth = screenWidth.coerceAtLeast(1)
        val safeScreenHeight = screenHeight.coerceAtLeast(1)
        var width = requestedWidth?.takeIf { it > 0 } ?: bitmapWidth.coerceAtLeast(1)
        var height = requestedHeight?.takeIf { it > 0 } ?: bitmapHeight.coerceAtLeast(1)

        val nearFullscreen = width >= (safeScreenWidth * NEAR_FULLSCREEN_FRACTION).roundToInt() ||
            height >= (safeScreenHeight * NEAR_FULLSCREEN_FRACTION).roundToInt()
        val maxWidth = if (nearFullscreen) {
            (safeScreenWidth * DISTINCT_INITIAL_WIDTH_FRACTION).roundToInt().coerceAtLeast(1)
        } else {
            safeScreenWidth
        }
        val maxHeight = if (nearFullscreen) {
            (safeScreenHeight * DISTINCT_INITIAL_HEIGHT_FRACTION).roundToInt().coerceAtLeast(1)
        } else {
            safeScreenHeight
        }

        if (height > maxHeight) {
            val scale = maxHeight / height.toFloat()
            height = maxHeight
            width = (width * scale).roundToInt().coerceAtLeast(1)
        }
        if (width > maxWidth) {
            val scale = maxWidth / width.toFloat()
            width = maxWidth
            height = (height * scale).roundToInt().coerceAtLeast(1)
        }
        return width to height
    }

    fun clampOverlayPosition(
        currentX: Int,
        currentY: Int,
        overlayWidth: Int,
        overlayHeight: Int,
        screenLeft: Int,
        screenTop: Int,
        screenRight: Int,
        screenBottom: Int,
        minVisiblePx: Int
    ): Pair<Int, Int> {
        val viewWidth = overlayWidth.coerceAtLeast(1)
        val viewHeight = overlayHeight.coerceAtLeast(1)
        val screenWidth = (screenRight - screenLeft).coerceAtLeast(1)
        val screenHeight = (screenBottom - screenTop).coerceAtLeast(1)
        val safeMinVisiblePx = minVisiblePx.coerceAtLeast(1)

        val minX: Int
        val maxX: Int
        if (viewWidth > screenWidth) {
            minX = screenRight - viewWidth
            maxX = screenLeft
        } else {
            minX = screenLeft - viewWidth + safeMinVisiblePx
            maxX = screenRight - safeMinVisiblePx
        }

        val minY: Int
        val maxY: Int
        if (viewHeight > screenHeight) {
            minY = screenBottom - viewHeight
            maxY = screenTop
        } else {
            minY = screenTop - viewHeight + safeMinVisiblePx
            maxY = screenBottom - safeMinVisiblePx
        }

        return currentX.coerceIn(minX, maxX) to currentY.coerceIn(minY, maxY)
    }
}
