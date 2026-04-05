package com.muding.android.presentation.main

import com.muding.android.domain.usecase.FLOATING_BALL_MAX_OPACITY
import com.muding.android.domain.usecase.FLOATING_BALL_MAX_SIZE_DP
import com.muding.android.domain.usecase.FLOATING_BALL_MIN_OPACITY
import com.muding.android.domain.usecase.FLOATING_BALL_MIN_SIZE_DP
import kotlin.math.roundToInt

internal fun floatingBallSizeDpFromProgress(progress: Int): Int {
    val normalized = progress.coerceIn(0, 100)
    val sizeRange = FLOATING_BALL_MAX_SIZE_DP - FLOATING_BALL_MIN_SIZE_DP
    return (FLOATING_BALL_MIN_SIZE_DP + (normalized / 100f) * sizeRange).roundToInt()
}

internal fun floatingBallSizeProgressFromDp(sizeDp: Int): Int {
    val clampedSize = sizeDp.coerceIn(FLOATING_BALL_MIN_SIZE_DP, FLOATING_BALL_MAX_SIZE_DP)
    val sizeRange = FLOATING_BALL_MAX_SIZE_DP - FLOATING_BALL_MIN_SIZE_DP
    return (((clampedSize - FLOATING_BALL_MIN_SIZE_DP).toFloat() / sizeRange) * 100).roundToInt()
}

internal fun floatingBallOpacityFromPercent(percent: Int): Float {
    return (percent.coerceIn(1, 100) / 100f).coerceIn(
        FLOATING_BALL_MIN_OPACITY,
        FLOATING_BALL_MAX_OPACITY
    )
}

internal fun floatingBallOpacityPercentFromAlpha(alpha: Float): Int {
    return (alpha.coerceIn(FLOATING_BALL_MIN_OPACITY, FLOATING_BALL_MAX_OPACITY) * 100).roundToInt()
}
