package com.muding.android.service

object UsageHintPositioning {

    data class AnchorBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val width: Int
            get() = (right - left).coerceAtLeast(1)

        val height: Int
            get() = (bottom - top).coerceAtLeast(1)
    }

    data class HintPosition(
        val x: Int,
        val y: Int
    )

    fun place(
        screenWidth: Int,
        screenHeight: Int,
        anchor: AnchorBounds,
        hintWidth: Int,
        hintHeight: Int,
        margin: Int
    ): HintPosition {
        val safeHintWidth = hintWidth.coerceAtLeast(1)
        val safeHintHeight = hintHeight.coerceAtLeast(1)
        val safeMargin = margin.coerceAtLeast(0)
        val rightX = anchor.right + safeMargin
        val leftX = anchor.left - safeHintWidth - safeMargin
        val centeredY = anchor.top + ((anchor.height - safeHintHeight) / 2)
        val maxX = (screenWidth - safeHintWidth - safeMargin).coerceAtLeast(safeMargin)
        val maxY = (screenHeight - safeHintHeight - safeMargin).coerceAtLeast(safeMargin)

        val resolvedX = if (rightX + safeHintWidth <= screenWidth - safeMargin) {
            rightX
        } else {
            leftX.coerceIn(safeMargin, maxX)
        }
        val resolvedY = centeredY.coerceIn(safeMargin, maxY)
        return HintPosition(x = resolvedX, y = resolvedY)
    }
}
