package com.muding.android.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

enum class DrawingTool {
    PEN,
    ERASER,
    ARROW,
    RECTANGLE,
    CIRCLE,
    TEXT
}

sealed class DrawingPath {
    data class PenPath(
        val path: Path,
        val color: Color,
        val strokeWidth: Float,
        val points: List<Offset> = emptyList()
    ) : DrawingPath()

    data class ArrowPath(
        val start: Offset,
        val end: Offset,
        val color: Color,
        val strokeWidth: Float
    ) : DrawingPath()

    data class RectanglePath(
        val topLeft: Offset,
        val bottomRight: Offset,
        val color: Color,
        val strokeWidth: Float,
        val filled: Boolean = false
    ) : DrawingPath()

    data class CirclePath(
        val center: Offset,
        val radius: Float,
        val color: Color,
        val strokeWidth: Float,
        val filled: Boolean = false
    ) : DrawingPath()

    data class TextPath(
        val position: Offset,
        val text: String,
        val color: Color,
        val fontSize: Float,
        val scale: Float = 1f,
        val rotation: Float = 0f,
        val outlineEnabled: Boolean = false
    ) : DrawingPath()
}
