package com.pixpin.android.presentation.editor

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.pixpin.android.domain.model.DrawingPath
import com.pixpin.android.domain.model.DrawingTool
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    paths: List<DrawingPath>,
    currentTool: DrawingTool,
    currentColor: Color,
    strokeWidth: Float,
    eraserSize: Float,
    eraserMode: EraserMode,
    textSize: Float,
    textOutlineEnabled: Boolean,
    selectedTextIndex: Int?,
    onPathAdded: (DrawingPath) -> Unit,
    onPathUpdated: (Int, DrawingPath) -> Unit,
    onPathReplaced: (Int, List<DrawingPath>) -> Unit,
    onPathRemoved: (Int) -> Unit,
    onTextSelectionChanged: (Int?, DrawingPath.TextPath?) -> Unit,
    onCanvasSizeChanged: (Size) -> Unit
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    val currentPenPoints = remember { mutableStateListOf<Offset>() }
    var pathVersion by remember { mutableIntStateOf(0) }
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }
    var showTextDialog by remember { mutableStateOf(false) }
    var textDraft by remember { mutableStateOf("") }
    var textTargetIndex by remember { mutableStateOf<Int?>(null) }
    var textTargetPosition by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var eraserPreviewCenter by remember { mutableStateOf<Offset?>(null) }

    val textMeasurer = rememberTextMeasurer()

    fun measureText(path: DrawingPath.TextPath): TextLayoutResult {
        return textMeasurer.measure(
            text = path.text,
            style = TextStyle(
                color = path.color,
                fontSize = (path.fontSize * path.scale).sp
            )
        )
    }

    fun hitTextIndexAt(offset: Offset): Int? {
        for (i in paths.indices.reversed()) {
            val path = paths[i]
            if (path is DrawingPath.TextPath) {
                if (isPointInsideText(path, measureText(path), offset)) {
                    return i
                }
            }
        }
        return null
    }

    fun openTextDialog(index: Int?, position: Offset, initialText: String) {
        textTargetIndex = index
        textTargetPosition = position
        textDraft = initialText
        showTextDialog = true
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                canvasSize = it
                onCanvasSizeChanged(Size(it.width.toFloat(), it.height.toFloat()))
            }
            .pointerInteropFilter { event ->
                if (currentTool != DrawingTool.ERASER) return@pointerInteropFilter false
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        val touch = Offset(event.x, event.y)
                        eraserPreviewCenter = touch
                        erasePathAt(
                            touch = touch,
                            paths = paths,
                            eraserMode = eraserMode,
                            eraserSize = eraserSize,
                            onPathReplaced = onPathReplaced,
                            onPathRemoved = onPathRemoved
                        )
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        eraserPreviewCenter = null
                        true
                    }

                    else -> false
                }
            }
            .pointerInput(currentTool, paths.size, selectedTextIndex) {
                when (currentTool) {
                    DrawingTool.PEN -> {
                        detectDragGestures(
                            onDragStart = { offset ->
                                onTextSelectionChanged(null, null)
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                currentPenPoints.clear()
                                currentPenPoints.add(offset)
                                startPoint = offset
                            },
                            onDrag = { change, _ ->
                                currentPath?.lineTo(change.position.x, change.position.y)
                                currentPenPoints.add(change.position)
                                pathVersion++
                                change.consume()
                            },
                            onDragEnd = {
                                currentPath?.let { path ->
                                    onPathAdded(
                                        DrawingPath.PenPath(
                                            path = Path().apply { addPath(path) },
                                            color = currentColor,
                                            strokeWidth = strokeWidth,
                                            points = currentPenPoints.toList()
                                        )
                                    )
                                }
                                currentPath = null
                                currentPenPoints.clear()
                                startPoint = null
                            }
                        )
                    }

                    DrawingTool.ERASER -> Unit

                    DrawingTool.ARROW, DrawingTool.RECTANGLE, DrawingTool.CIRCLE -> {
                        detectDragGestures(
                            onDragStart = { offset ->
                                onTextSelectionChanged(null, null)
                                startPoint = offset
                                endPoint = offset
                            },
                            onDrag = { change, _ ->
                                endPoint = change.position
                                change.consume()
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
                                                    topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                                                    bottomRight = Offset(maxOf(start.x, end.x), maxOf(start.y, end.y)),
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

                                        else -> Unit
                                    }
                                }
                                startPoint = null
                                endPoint = null
                            }
                        )
                    }

                    DrawingTool.TEXT -> Unit
                }
            }
            .pointerInput(currentTool, selectedTextIndex, paths.size) {
                if (currentTool == DrawingTool.TEXT) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val index = selectedTextIndex ?: return@detectTransformGestures
                        val path = paths.getOrNull(index) as? DrawingPath.TextPath ?: return@detectTransformGestures
                        if (pan == Offset.Zero && zoom == 1f && rotation == 0f) return@detectTransformGestures
                        onPathUpdated(
                            index,
                            path.copy(
                                position = path.position + pan,
                                scale = (path.scale * zoom).coerceIn(0.5f, 6f),
                                rotation = normalizeRotation(path.rotation + rotation)
                            )
                        )
                    }
                }
            }
            .pointerInput(currentTool, paths.size) {
                if (currentTool == DrawingTool.TEXT) {
                    detectTapGestures(
                        onTap = { offset ->
                            val hitIndex = hitTextIndexAt(offset)
                            if (hitIndex != null) {
                                onTextSelectionChanged(hitIndex, paths.getOrNull(hitIndex) as? DrawingPath.TextPath)
                            } else {
                                onTextSelectionChanged(null, null)
                                openTextDialog(null, offset, "")
                            }
                        },
                        onDoubleTap = { offset ->
                            val hitIndex = hitTextIndexAt(offset)
                            val path = hitIndex?.let { paths.getOrNull(it) as? DrawingPath.TextPath }
                            if (path != null) {
                                onTextSelectionChanged(hitIndex, path)
                                openTextDialog(hitIndex, path.position, path.text)
                            }
                        }
                    )
                }
            }
    ) {
        paths.forEachIndexed { index, drawingPath ->
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
                        size = Size(
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
                    val textLayout = measureText(drawingPath)
                    rotate(drawingPath.rotation, pivot = drawingPath.position) {
                        if (drawingPath.outlineEnabled) {
                            drawOutlinedText(
                                textLayout = textLayout,
                                topLeft = drawingPath.position,
                                outlineColor = outlineColorFor(drawingPath.color)
                            )
                        }
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = drawingPath.position
                        )
                    }

                    if (selectedTextIndex == index && currentTool == DrawingTool.TEXT) {
                        drawTextSelection(drawingPath, textLayout)
                    }
                }
            }
        }

        if (pathVersion >= 0) {
            // Trigger recomposition during free draw.
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

        val start = startPoint
        val end = endPoint
        if (start != null && end != null) {
            when (currentTool) {
                DrawingTool.ARROW -> drawArrow(start, end, currentColor, strokeWidth)
                DrawingTool.RECTANGLE -> {
                    drawRect(
                        color = currentColor,
                        topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                        size = Size(
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

                else -> Unit
            }
        }

        if (currentTool == DrawingTool.ERASER) {
            eraserPreviewCenter?.let { center ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = eraserSize / 2f,
                    center = center,
                    style = Fill
                )
                drawCircle(
                    color = Color.White,
                    radius = eraserSize / 2f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
        }
    }

    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text(if (textTargetIndex == null) "Add text" else "Edit text") },
            text = {
                OutlinedTextField(
                    value = textDraft,
                    onValueChange = { textDraft = it },
                    label = { Text("Text") },
                    singleLine = false
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val text = textDraft.trim()
                        val existingIndex = textTargetIndex
                        if (existingIndex != null) {
                            if (text.isEmpty()) {
                                onPathRemoved(existingIndex)
                                if (selectedTextIndex == existingIndex) onTextSelectionChanged(null, null)
                            } else {
                                val oldPath = paths.getOrNull(existingIndex) as? DrawingPath.TextPath
                                if (oldPath != null) {
                                    val updatedPath = oldPath.copy(
                                        text = text,
                                        color = currentColor,
                                        fontSize = textSize,
                                        outlineEnabled = oldPath.outlineEnabled
                                    )
                                    onPathUpdated(
                                        existingIndex,
                                        updatedPath
                                    )
                                    onTextSelectionChanged(existingIndex, updatedPath)
                                }
                            }
                        } else if (text.isNotEmpty()) {
                            val newPath = DrawingPath.TextPath(
                                position = textTargetPosition,
                                text = text,
                                color = currentColor,
                                fontSize = textSize,
                                outlineEnabled = textOutlineEnabled
                            )
                            onPathAdded(newPath)
                        }
                        textTargetIndex = null
                        showTextDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    textTargetIndex = null
                    showTextDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun DrawScope.drawOutlinedText(
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

private fun outlineColorFor(color: Color): Color {
    val luminance = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
    return if (luminance > 0.6f) Color.Black.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f)
}

private fun normalizeRotation(rotationDegrees: Float): Float {
    var normalized = rotationDegrees
    while (normalized <= -180f) normalized += 360f
    while (normalized > 180f) normalized -= 360f
    return normalized
}

private fun erasePathAt(
    touch: Offset,
    paths: List<DrawingPath>,
    eraserMode: EraserMode,
    eraserSize: Float,
    onPathReplaced: (Int, List<DrawingPath>) -> Unit,
    onPathRemoved: (Int) -> Unit
) {
    for (index in paths.indices.reversed()) {
        val path = paths[index]
        if (eraserMode == EraserMode.PARTIAL && path is DrawingPath.PenPath) {
            val replacement = erasePenPathPartially(path, touch, eraserSize / 2f)
            if (replacement != null) {
                onPathReplaced(index, replacement)
                return
            }
        } else if (isPathHit(path, touch, eraserSize / 2f)) {
            onPathRemoved(index)
            return
        }
    }
}

private fun erasePenPathPartially(
    path: DrawingPath.PenPath,
    touch: Offset,
    radius: Float
): List<DrawingPath.PenPath>? {
    if (path.points.none { (it - touch).getDistance() <= radius }) return null

    val keptSegments = mutableListOf<List<Offset>>()
    var currentSegment = mutableListOf<Offset>()
    path.points.forEach { point ->
        if ((point - touch).getDistance() <= radius) {
            if (currentSegment.size >= 2) {
                keptSegments += currentSegment.toList()
            }
            currentSegment = mutableListOf()
        } else {
            currentSegment.add(point)
        }
    }
    if (currentSegment.size >= 2) {
        keptSegments += currentSegment.toList()
    }

    return keptSegments.map { segment ->
        DrawingPath.PenPath(
            path = buildComposePath(segment),
            color = path.color,
            strokeWidth = path.strokeWidth,
            points = segment
        )
    }
}

private fun isPathHit(
    path: DrawingPath,
    touch: Offset,
    radius: Float
): Boolean {
    return when (path) {
        is DrawingPath.PenPath -> {
            path.points.any { point ->
                (point - touch).getDistance() <= maxOf(radius, path.strokeWidth * 2f)
            }
        }

        is DrawingPath.ArrowPath -> {
            distanceToSegment(touch, path.start, path.end) <= maxOf(radius, path.strokeWidth * 2f)
        }

        is DrawingPath.RectanglePath -> {
            val rect = androidx.compose.ui.geometry.Rect(path.topLeft, path.bottomRight)
            rect.inflate(radius).contains(touch)
        }

        is DrawingPath.CirclePath -> {
            kotlin.math.abs((touch - path.center).getDistance() - path.radius) <= maxOf(radius, path.strokeWidth * 2f) ||
                (touch - path.center).getDistance() <= path.radius
        }

        is DrawingPath.TextPath -> {
            estimateTextBounds(path).inflate(radius).contains(touch)
        }
    }
}

private fun buildComposePath(points: List<Offset>): Path {
    return Path().apply {
        val first = points.firstOrNull() ?: return@apply
        moveTo(first.x, first.y)
        points.drop(1).forEach { point -> lineTo(point.x, point.y) }
    }
}

private fun estimateTextBounds(path: DrawingPath.TextPath): androidx.compose.ui.geometry.Rect {
    val lines = path.text.split('\n')
    val widestLine = lines.maxOfOrNull { it.length } ?: 1
    val width = widestLine.coerceAtLeast(1) * path.fontSize * path.scale * 0.62f
    val height = lines.size.coerceAtLeast(1) * path.fontSize * path.scale * 1.35f
    return androidx.compose.ui.geometry.Rect(
        left = path.position.x,
        top = path.position.y,
        right = path.position.x + width,
        bottom = path.position.y + height
    )
}

private fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
    val segment = end - start
    val lengthSquared = (segment.x * segment.x) + (segment.y * segment.y)
    if (lengthSquared == 0f) return (point - start).getDistance()
    val t = (((point.x - start.x) * segment.x) + ((point.y - start.y) * segment.y)) / lengthSquared
    val clamped = t.coerceIn(0f, 1f)
    val projection = Offset(
        start.x + (clamped * segment.x),
        start.y + (clamped * segment.y)
    )
    return (point - projection).getDistance()
}

private fun isPointInsideText(
    path: DrawingPath.TextPath,
    layout: TextLayoutResult,
    point: Offset
): Boolean {
    val local = toLocalTextPoint(path, point)
    return local.x in 0f..layout.size.width.toFloat() &&
        local.y in 0f..layout.size.height.toFloat()
}

private fun toLocalTextPoint(path: DrawingPath.TextPath, point: Offset): Offset {
    val translated = point - path.position
    val radians = (-path.rotation * PI / 180f).toFloat()
    val rotatedX = translated.x * cos(radians) - translated.y * sin(radians)
    val rotatedY = translated.x * sin(radians) + translated.y * cos(radians)
    return Offset(rotatedX, rotatedY)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTextSelection(
    path: DrawingPath.TextPath,
    layout: TextLayoutResult
) {
    val rectColor = Color.White.copy(alpha = 0.9f)
    val cornerColor = path.color
    val width = layout.size.width.toFloat()
    val height = layout.size.height.toFloat()
    rotate(path.rotation, pivot = path.position) {
        drawRect(
            color = rectColor,
            topLeft = path.position,
            size = Size(width, height),
            style = Stroke(width = 2f)
        )

        val cornerSize = 8f
        val corners = listOf(
            path.position,
            path.position + Offset(width, 0f),
            path.position + Offset(0f, height),
            path.position + Offset(width, height)
        )
        corners.forEach { corner ->
            drawCircle(
                color = cornerColor,
                radius = cornerSize,
                center = corner,
                style = Fill
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    val arrowLength = 30f
    val arrowAngle = Math.PI / 6
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
