package com.muding.android.presentation.main

import kotlin.math.abs

internal data class FloatingBallAppearanceDraft(
    val sizeProgress: Int,
    val opacityPercent: Int
) {
    val previewSizeDp: Int
        get() = floatingBallSizeDpFromProgress(sizeProgress)

    val previewOpacity: Float
        get() = floatingBallOpacityFromPercent(opacityPercent)

    fun updateSizeProgress(progress: Int): FloatingBallAppearanceDraft {
        return copy(sizeProgress = progress.coerceIn(0, 100))
    }

    fun updateOpacityPercent(percent: Int): FloatingBallAppearanceDraft {
        return copy(opacityPercent = percent.coerceIn(1, 100))
    }

    fun sizeCommitOrNull(committedSizeDp: Int): Int? {
        val candidate = previewSizeDp
        return candidate.takeIf { it != committedSizeDp }
    }

    fun opacityCommitOrNull(committedOpacity: Float): Float? {
        val candidate = previewOpacity
        return candidate.takeIf { abs(it - committedOpacity) > 0.0001f }
    }

    companion object {
        fun fromCommitted(sizeDp: Int, opacity: Float): FloatingBallAppearanceDraft {
            return FloatingBallAppearanceDraft(
                sizeProgress = floatingBallSizeProgressFromDp(sizeDp),
                opacityPercent = floatingBallOpacityPercentFromAlpha(opacity)
            )
        }
    }
}
