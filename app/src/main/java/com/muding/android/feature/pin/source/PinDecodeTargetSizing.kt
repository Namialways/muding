package com.muding.android.feature.pin.source

import kotlin.math.max
import kotlin.math.roundToInt

data class PinDecodeTarget(
    val widthPx: Int,
    val heightPx: Int
)

object PinDecodeTargetSizing {

    fun galleryImportTarget(
        screenWidthPx: Int,
        screenHeightPx: Int
    ): PinDecodeTarget {
        val width = max((screenWidthPx * 1.14f).roundToInt(), 960)
        val height = max((screenHeightPx * 0.54f).roundToInt(), 960)
        return PinDecodeTarget(widthPx = width, heightPx = height)
    }

    fun overlayDecodeTarget(
        screenWidthPx: Int,
        screenHeightPx: Int,
        preferredWidthPx: Int?,
        preferredHeightPx: Int?
    ): PinDecodeTarget {
        val width = preferredWidthPx
            ?.coerceAtLeast((screenWidthPx * 0.6f).roundToInt().coerceAtLeast(720))
            ?: max((screenWidthPx * 1.4f).roundToInt(), 1280)
        val height = preferredHeightPx
            ?.coerceAtLeast((screenHeightPx * 0.45f).roundToInt().coerceAtLeast(720))
            ?: max((screenHeightPx * 0.6f).roundToInt(), 1440)
        return PinDecodeTarget(widthPx = width, heightPx = height)
    }
}
