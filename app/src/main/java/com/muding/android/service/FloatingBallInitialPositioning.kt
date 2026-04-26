package com.muding.android.service

import com.muding.android.domain.usecase.FloatingBallLastPosition

data class FloatingBallInitialPosition(
    val x: Int,
    val y: Int
)

object FloatingBallInitialPositioning {
    fun resolve(
        screenWidth: Int,
        screenHeight: Int,
        ballSizePx: Int,
        savedPosition: FloatingBallLastPosition?
    ): FloatingBallInitialPosition {
        val maxX = (screenWidth - ballSizePx).coerceAtLeast(0)
        val maxY = (screenHeight - ballSizePx).coerceAtLeast(0)
        val x = savedPosition?.x?.coerceIn(0, maxX) ?: maxX
        val y = savedPosition?.y?.coerceIn(0, maxY) ?: (maxY / 2)
        return FloatingBallInitialPosition(x, y)
    }
}
