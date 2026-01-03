package com.pixpin.android.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * 绘制工具类型
 */
enum class DrawingTool {
    PEN,        // 画笔
    ARROW,      // 箭头
    RECTANGLE,  // 矩形
    CIRCLE,     // 圆形
    TEXT        // 文字
}

/**
 * 绘制路径数据
 */
sealed class DrawingPath {
    data class PenPath(
        val path: Path,
        val color: Color,
        val strokeWidth: Float
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
        val fontSize: Float
    ) : DrawingPath()
}
