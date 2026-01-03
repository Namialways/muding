package com.pixpin.android.presentation.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.pixpin.android.domain.model.DrawingPath
import com.pixpin.android.domain.model.DrawingTool
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 绘图画布组件
 */
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    paths: List<DrawingPath>,
    currentTool: DrawingTool,
    currentColor: androidx.compose.ui.graphics.Color,
    strokeWidth: Float,
    onPathAdded: (DrawingPath) -> Unit
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var pathVersion by remember { mutableStateOf(0) }
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(currentTool) {
                when (currentTool) {
                    DrawingTool.PEN -> {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                startPoint = offset
                            },
                            onDrag = { change, _ ->
                                currentPath?.lineTo(change.position.x, change.position.y)
                                // 强制触发重绘，让画笔跟手
                                pathVersion++
                            },
                            onDragEnd = {
                                currentPath?.let { path ->
                                    onPathAdded(
                                        DrawingPath.PenPath(
                                            path = Path().apply {
                                                addPath(path)
                                            },
                                            color = currentColor,
                                            strokeWidth = strokeWidth
                                        )
                                    )
                                }
                                currentPath = null
                                startPoint = null
                            }
                        )
                    }
                    DrawingTool.ARROW, DrawingTool.RECTANGLE, DrawingTool.CIRCLE -> {
                        detectDragGestures(
                            onDragStart = { offset ->
                                startPoint = offset
                                endPoint = offset
                            },
                            onDrag = { change, _ ->
                                endPoint = change.position
                            },
                            onDragEnd = {
                                val start = startPoint
                                val end = endPoint
                                if (start != null && end != null) {
                                    when (currentTool) {
                                        DrawingTool.ARROW -> {
                                            onPathAdded(
                                                DrawingPath.ArrowPath(
                                                    start = start,
                                                    end = end,
                                                    color = currentColor,
                                                    strokeWidth = strokeWidth
                                                )
                                            )
                                        }
                                        DrawingTool.RECTANGLE -> {
                                            onPathAdded(
                                                DrawingPath.RectanglePath(
                                                    topLeft = Offset(
                                                        minOf(start.x, end.x),
                                                        minOf(start.y, end.y)
                                                    ),
                                                    bottomRight = Offset(
                                                        maxOf(start.x, end.x),
                                                        maxOf(start.y, end.y)
                                                    ),
                                                    color = currentColor,
                                                    strokeWidth = strokeWidth
                                                )
                                            )
                                        }
                                        DrawingTool.CIRCLE -> {
                                            val radius = sqrt(
                                                (end.x - start.x) * (end.x - start.x) +
                                                (end.y - start.y) * (end.y - start.y)
                                            )
                                            onPathAdded(
                                                DrawingPath.CirclePath(
                                                    center = start,
                                                    radius = radius,
                                                    color = currentColor,
                                                    strokeWidth = strokeWidth
                                                )
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                                startPoint = null
                                endPoint = null
                            }
                        )
                    }
                    else -> {}
                }
            }
    ) {
        // 绘制所有已保存的路径
        paths.forEach { drawingPath ->
            when (drawingPath) {
                is DrawingPath.PenPath -> {
                    drawPath(
                        path = drawingPath.path,
                        color = drawingPath.color,
                        style = Stroke(
                            width = drawingPath.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                is DrawingPath.ArrowPath -> {
                    drawArrow(
                        start = drawingPath.start,
                        end = drawingPath.end,
                        color = drawingPath.color,
                        strokeWidth = drawingPath.strokeWidth
                    )
                }
                is DrawingPath.RectanglePath -> {
                    drawRect(
                        color = drawingPath.color,
                        topLeft = drawingPath.topLeft,
                        size = androidx.compose.ui.geometry.Size(
                            drawingPath.bottomRight.x - drawingPath.topLeft.x,
                            drawingPath.bottomRight.y - drawingPath.topLeft.y
                        ),
                        style = if (drawingPath.filled) Fill else Stroke(width = drawingPath.strokeWidth)
                    )
                }
                is DrawingPath.CirclePath -> {
                    drawCircle(
                        color = drawingPath.color,
                        radius = drawingPath.radius,
                        center = drawingPath.center,
                        style = if (drawingPath.filled) Fill else Stroke(width = drawingPath.strokeWidth)
                    )
                }
                is DrawingPath.TextPath -> {
                    val textLayoutResult = textMeasurer.measure(
                        text = drawingPath.text,
                        style = TextStyle(
                            color = drawingPath.color,
                            fontSize = drawingPath.fontSize.sp
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = drawingPath.position
                    )
                }
            }
        }

        // 绘制当前正在绘制的路径（pathVersion 用于触发重绘）
        if (pathVersion >= 0) {
            // no-op: 仅用于触发重组
        }
        currentPath?.let { path ->
            drawPath(
                path = path,
                color = currentColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // 绘制当前正在绘制的形状预览
        val start = startPoint
        val end = endPoint
        if (start != null && end != null) {
            when (currentTool) {
                DrawingTool.ARROW -> {
                    drawArrow(start, end, currentColor, strokeWidth)
                }
                DrawingTool.RECTANGLE -> {
                    drawRect(
                        color = currentColor,
                        topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                        size = androidx.compose.ui.geometry.Size(
                            maxOf(start.x, end.x) - minOf(start.x, end.x),
                            maxOf(start.y, end.y) - minOf(start.y, end.y)
                        ),
                        style = Stroke(width = strokeWidth)
                    )
                }
                DrawingTool.CIRCLE -> {
                    val radius = sqrt(
                        (end.x - start.x) * (end.x - start.x) +
                        (end.y - start.y) * (end.y - start.y)
                    )
                    drawCircle(
                        color = currentColor,
                        radius = radius,
                        center = start,
                        style = Stroke(width = strokeWidth)
                    )
                }
                else -> {}
            }
        }
    }
}

/**
 * 绘制箭头
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: Float
) {
    // 绘制线段
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // 计算箭头
    val arrowLength = 30f
    val arrowAngle = Math.PI / 6 // 30度
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())

    val arrowPoint1 = Offset(
        (end.x - arrowLength * cos(angle - arrowAngle)).toFloat(),
        (end.y - arrowLength * sin(angle - arrowAngle)).toFloat()
    )

    val arrowPoint2 = Offset(
        (end.x - arrowLength * cos(angle + arrowAngle)).toFloat(),
        (end.y - arrowLength * sin(angle + arrowAngle)).toFloat()
    )

    // 绘制箭头
    drawLine(color = color, start = end, end = arrowPoint1, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = end, end = arrowPoint2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}
