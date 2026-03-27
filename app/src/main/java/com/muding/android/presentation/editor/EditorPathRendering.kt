package com.muding.android.presentation.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import com.muding.android.domain.model.DrawingPath
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawOutlinedText(
    textLayout: TextLayoutResult,
    topLeft: Offset,
    outlineColor: Color
) {
    val offsets = listOf(
        Offset(-2f, 0f),
        Offset(2f, 0f),
        Offset(0f, -2f),
        Offset(0f, 2f),
        Offset(-1.5f, -1.5f),
        Offset(-1.5f, 1.5f),
        Offset(1.5f, -1.5f),
        Offset(1.5f, 1.5f)
    )
    offsets.forEach { delta ->
        drawText(
            textLayoutResult = textLayout,
            topLeft = topLeft + delta,
            color = outlineColor
        )
    }
}

fun DrawScope.drawPathSelection(path: DrawingPath) {
    when (path) {
        is DrawingPath.ArrowPath -> drawArrowSelection(path)
        is DrawingPath.RectanglePath -> drawRectangleSelection(path)
        is DrawingPath.CirclePath -> drawCircleSelection(path)
        else -> {
            val bounds = boundsForPath(path) ?: return
            drawSelectionBounds(bounds, selectionAccentColor(path))
        }
    }
}

fun DrawScope.drawArrowSelection(path: DrawingPath.ArrowPath) {
    drawLine(
        color = Color.White.copy(alpha = 0.92f),
        start = path.start,
        end = path.end,
        strokeWidth = max(path.strokeWidth + 4f, 5f)
    )
    drawHandle(path.start, path.color)
    drawHandle(path.end, path.color)
}

fun DrawScope.drawCircleSelection(path: DrawingPath.CirclePath) {
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = path.radius,
        center = path.center,
        style = Stroke(width = 2f)
    )
    val handles = listOf(
        Offset(path.center.x, path.center.y - path.radius),
        Offset(path.center.x, path.center.y + path.radius),
        Offset(path.center.x - path.radius, path.center.y),
        Offset(path.center.x + path.radius, path.center.y)
    )
    handles.forEach { pos ->
        drawHandle(pos, path.color)
    }
}

fun DrawScope.drawRectangleSelection(path: DrawingPath.RectanglePath) {
    val corners = rectangleCorners(path)
    val strokeColor = Color.White.copy(alpha = 0.95f)
    corners.zipWithNext().forEach { (start, end) ->
        drawLine(
            color = strokeColor,
            start = start,
            end = end,
            strokeWidth = 2f
        )
    }
    drawLine(
        color = strokeColor,
        start = corners.last(),
        end = corners.first(),
        strokeWidth = 2f
    )

    val center = rectangleCenter(path)
    val tl = path.topLeft
    val br = path.bottomRight
    val handles = listOf(
        tl,
        Offset(br.x, tl.y),
        br,
        Offset(tl.x, br.y),
        Offset((tl.x + br.x) / 2f, tl.y),
        Offset((tl.x + br.x) / 2f, br.y),
        Offset(tl.x, (tl.y + br.y) / 2f),
        Offset(br.x, (tl.y + br.y) / 2f)
    ).map { rotatePointAround(it, center, path.rotation) }

    handles.forEach { pos ->
        drawHandle(pos, path.color)
    }
}

fun DrawScope.drawSelectionBounds(bounds: Rect, accent: Color) {
    drawRect(
        color = Color.White.copy(alpha = 0.95f),
        topLeft = bounds.topLeft,
        size = bounds.size,
        style = Stroke(width = 2f)
    )
    val handleRadius = 6f
    val handles = listOf(
        bounds.topLeft,
        Offset(bounds.right, bounds.top),
        Offset(bounds.left, bounds.bottom),
        bounds.bottomRight
    )
    handles.forEach { handle ->
        drawHandle(handle, accent, handleRadius)
    }
}

fun DrawScope.drawHandle(center: Offset, accent: Color, radius: Float = 6f) {
    drawCircle(color = accent, radius = radius, center = center, style = Fill)
    drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = 1.5f))
}

fun outlineColorFor(color: Color): Color {
    val luminance = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
    return if (luminance > 0.6f) Color.Black.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f)
}

fun DrawScope.drawTextSelection(path: DrawingPath.TextPath, layout: TextLayoutResult) {
    rotate(path.rotation, pivot = path.position) {
        drawSelectionBounds(
            bounds = Rect(
                left = path.position.x,
                top = path.position.y,
                right = path.position.x + layout.size.width,
                bottom = path.position.y + layout.size.height
            ),
            accent = path.color
        )
    }
}

fun DrawScope.drawArrow(start: Offset, end: Offset, color: Color, strokeWidth: Float) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    val arrowLength = 30f
    val arrowAngle = PI / 6
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())

    val arrowPoint1 = Offset(
        (end.x - arrowLength * cos(angle - arrowAngle)).toFloat(),
        (end.y - arrowLength * sin(angle - arrowAngle)).toFloat()
    )
    val arrowPoint2 = Offset(
        (end.x - arrowLength * cos(angle + arrowAngle)).toFloat(),
        (end.y - arrowLength * sin(angle + arrowAngle)).toFloat()
    )

    drawLine(color = color, start = end, end = arrowPoint1, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = end, end = arrowPoint2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}
