package com.muding.android.feature.ocr

import kotlin.math.max
import kotlin.math.min

internal data class OcrImagePreprocessingPlan(
    val scaleFactor: Float,
    val includeEnhancedVariant: Boolean
) {
    companion object {
        fun from(width: Int, height: Int): OcrImagePreprocessingPlan {
            if (width <= 0 || height <= 0) {
                return OcrImagePreprocessingPlan(
                    scaleFactor = 1f,
                    includeEnhancedVariant = false
                )
            }

            val shortestSide = min(width, height)
            val longestSide = max(width, height)
            val preferredScale = when {
                shortestSide < SMALL_TEXT_SIDE_PX -> 3f
                shortestSide < MEDIUM_TEXT_SIDE_PX -> 2f
                else -> 1f
            }
            val cappedScale = if (longestSide * preferredScale <= MAX_LONG_EDGE_PX) {
                preferredScale
            } else {
                1f
            }

            return OcrImagePreprocessingPlan(
                scaleFactor = cappedScale,
                includeEnhancedVariant = shortestSide < ENHANCED_VARIANT_SIDE_PX
            )
        }

        private const val SMALL_TEXT_SIDE_PX = 220
        private const val MEDIUM_TEXT_SIDE_PX = 520
        private const val ENHANCED_VARIANT_SIDE_PX = 900
        private const val MAX_LONG_EDGE_PX = 2400
    }
}
