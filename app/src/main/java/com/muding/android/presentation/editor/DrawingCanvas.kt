package com.muding.android.presentation.editor

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.unit.sp
import com.muding.android.R
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val SHAPE_CREATION_THRESHOLD = 6f
private const val SELECTION_HIT_RADIUS = 18f
private const val HANDLE_HIT_RADIUS = 24f
private const val MIN_CIRCLE_RADIUS = 16f

private sealed interface PathResizeHandle {
    data object ArrowStart : PathResizeHandle
    data object ArrowEnd : PathResizeHandle
    data object RectTopLeft : PathResizeHandle
    data object RectTopRight : PathResizeHandle
    data object RectBottomLeft : PathResizeHandle
    data object RectBottomRight : PathResizeHandle
    data object CircleRadius : PathResizeHandle
}

private data class ActiveResizeState(
    val index: Int,
    val handle: PathResizeHandle
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    paths: List<DrawingPath>,
    currentTool: DrawingTool?,
    currentColor: Color,
    strokeWidth: Float,
    eraserSize: Float,
    eraserMode: EraserMode,
    textSize: Float,
    textOutlineEnabled: Boolean,
    shapeFilled: Boolean,
    selectedPathIndex: Int?,
    onPathAdded: (DrawingPath) -> Unit,
    onPathUpdated: (Int, DrawingPath) -> Unit,
    onPathReplaced: (Int, List<DrawingPath>) -> Unit,
    onPathRemoved: (Int) -> Unit,
    onPathSelectionChanged: (Int?, DrawingPath?) -> Unit,
    onCanvasSizeChanged: (Size) -> Unit
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    val currentPenPoints = remember { mutableStateListOf<Offset>() }
    var pathVersion by remember { mutableIntStateOf(0) }
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }
    var movingPathIndex by remember { mutableStateOf<Int?>(null) }
    var resizingState by remember { mutableStateOf<ActiveResizeState?>(null) }
    var showTextDialog by remember { mutableStateOf(false) }
    var textDraft by remember { mutableStateOf("") }
    var textTargetIndex by remember { mutableStateOf<Int?>(null) }
    var textTargetPosition by remember { mutableStateOf(Offset.Zero) }
    var eraserPreviewCenter by remember { mutableStateOf<Offset?>(null) }

    val textMeasurer = rememberTextMeasurer()

    fun hitIndexAt(offset: Offset, tool: DrawingTool? = null): Int? {
        for (index in paths.indices.reversed()) {
            val path = paths[index]
            if (tool != null && toolFor(path) != tool) continue
            if (isPathHit(path, offset, SELECTION_HIT_RADIUS)) {
                return index
            }
        }
        return null
    }

    fun hitEditableIndexAt(offset: Offset): Int? {
        for (index in paths.indices.reversed()) {
            val path = paths[index]
            if (path is DrawingPath.PenPath) continue
            if (isPathHit(path, offset, SELECTION_HIT_RADIUS)) {
                return index
            }
        }
        return null
    }

    fun measureText(path: DrawingPath.TextPath): TextLayoutResult {
        return textMeasurer.measure(
            text = path.text,
            style = TextStyle(
                color = path.color,
                fontSize = (path.fontSize * path.scale).sp
            )
        )
    }

    fun openTextDialog(index: Int?, position: Offset, initialText: String) {
        textTargetIndex = index
        textTargetPosition = position
        textDraft = initialText
        showTextDialog = true
    }

    fun resetDragState() {
        currentPath = null
        currentPenPoints.clear()
        startPoint = null
        endPoint = null
        movingPathIndex = null
        resizingState = null
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
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
            .pointerInput(currentTool, paths.size, selectedPathIndex, strokeWidth, currentColor, shapeFilled) {
                when (currentTool) {
                    DrawingTool.MOVE -> detectDragGestures(
                        onDragStart = { offset ->
                            val selectedIndex = selectedPathIndex
                            val selectedPath = selectedIndex?.let(paths::getOrNull)
                            val activeHandle = selectedPath?.let { resizeHandleHit(it, offset) }
                            if (selectedIndex != null && activeHandle != null) {
                                resizingState = ActiveResizeState(selectedIndex, activeHandle)
                                onPathSelectionChanged(selectedIndex, selectedPath)
                                return@detectDragGestures
                            }

                            if (selectedIndex != null &&
                                selectedPath != null &&
                                selectedPath !is DrawingPath.PenPath &&
                                isPathHit(selectedPath, offset, SELECTION_HIT_RADIUS)
                            ) {
                                movingPathIndex = selectedIndex
                                onPathSelectionChanged(selectedIndex, selectedPath)
                                return@detectDragGestures
                            }

                            val hitIndex = hitEditableIndexAt(offset)
                            val hitPath = hitIndex?.let(paths::getOrNull)
                            onPathSelectionChanged(hitIndex, hitPath)
                        },
                        onDrag = { change, dragAmount ->
                            val activeResize = resizingState
                            when {
                                activeResize != null -> {
                                    val path = paths.getOrNull(activeResize.index) ?: return@detectDragGestures
                                    onPathUpdated(
                                        activeResize.index,
                                        resizePath(
                                            path = path,
                                            handle = activeResize.handle,
                                            touch = change.position
                                        )
                                    )
                                }

                                movingPathIndex != null -> {
                                    val movingIndex = movingPathIndex ?: return@detectDragGestures
                                    val path = paths.getOrNull(movingIndex) ?: return@detectDragGestures
                                    onPathUpdated(movingIndex, movePath(path, dragAmount))
                                }

                                else -> Unit
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            val activeResize = resizingState
                            val movingIndex = movingPathIndex
                            when {
                                activeResize != null -> {
                                    onPathSelectionChanged(activeResize.index, paths.getOrNull(activeResize.index))
                                }

                                movingIndex != null -> {
                                    onPathSelectionChanged(movingIndex, paths.getOrNull(movingIndex))
                                }

                                else -> Unit
                            }
                            resetDragState()
                        },
                        onDragCancel = { resetDragState() }
                    )

                    DrawingTool.PEN -> detectDragGestures(
                        onDragStart = { offset ->
                            onPathSelectionChanged(null, null)
                            currentPath = Path().apply { moveTo(offset.x, offset.y) }
                            currentPenPoints.clear()
                            currentPenPoints.add(offset)
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
                            resetDragState()
                        },
                        onDragCancel = { resetDragState() }
                    )

                    DrawingTool.ERASER -> Unit

                    DrawingTool.ARROW, DrawingTool.RECTANGLE, DrawingTool.CIRCLE -> detectDragGestures(
                        onDragStart = { offset ->
                            onPathSelectionChanged(null, null)
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
                            if (start != null && end != null && (end - start).getDistance() >= SHAPE_CREATION_THRESHOLD) {
                                val newPath = when (currentTool) {
                                    DrawingTool.ARROW -> DrawingPath.ArrowPath(
                                        start = start,
                                        end = end,
                                        color = currentColor,
                                        strokeWidth = strokeWidth
                                    )

                                    DrawingTool.RECTANGLE -> DrawingPath.RectanglePath(
                                        topLeft = Offset(min(start.x, end.x), min(start.y, end.y)),
                                        bottomRight = Offset(max(start.x, end.x), max(start.y, end.y)),
                                        color = currentColor,
                                        strokeWidth = strokeWidth,
                                        filled = shapeFilled
                                    )

                                    DrawingTool.CIRCLE -> DrawingPath.CirclePath(
                                        center = start,
                                        radius = (end - start).getDistance(),
                                        color = currentColor,
                                        strokeWidth = strokeWidth,
                                        filled = shapeFilled
                                    )

                                    else -> null
                                }
                                newPath?.let(onPathAdded)
                            }
                            resetDragState()
                        },
                        onDragCancel = { resetDragState() }
                    )

                    DrawingTool.TEXT -> detectDragGestures(
                        onDragStart = { offset ->
                            val hitIndex = hitIndexAt(offset, DrawingTool.TEXT)
                            if (hitIndex != null) {
                                movingPathIndex = hitIndex
                                onPathSelectionChanged(hitIndex, paths.getOrNull(hitIndex))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val movingIndex = movingPathIndex
                            if (movingIndex != null) {
                                val path = paths.getOrNull(movingIndex) ?: return@detectDragGestures
                                onPathUpdated(movingIndex, movePath(path, dragAmount))
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            val movingIndex = movingPathIndex
                            if (movingIndex != null) {
                                onPathSelectionChanged(movingIndex, paths.getOrNull(movingIndex))
                            }
                            resetDragState()
                        },
                        onDragCancel = { resetDragState() }
                    )

                    null -> Unit
                }
            }
            .pointerInput(currentTool, paths.size, selectedPathIndex) {
                when (currentTool) {
                    DrawingTool.MOVE -> detectTapGestures(
                        onTap = { offset ->
                            val hitIndex = hitEditableIndexAt(offset)
                            onPathSelectionChanged(hitIndex, hitIndex?.let(paths::getOrNull))
                        },
                        onDoubleTap = { offset ->
                            val hitIndex = hitEditableIndexAt(offset)
                            val path = hitIndex?.let { paths.getOrNull(it) as? DrawingPath.TextPath }
                            if (path != null) {
                                onPathSelectionChanged(hitIndex, path)
                                openTextDialog(hitIndex, path.position, path.text)
                            }
                        }
                    )

                    DrawingTool.TEXT -> detectTapGestures(
                        onTap = { offset ->
                            val hitIndex = hitIndexAt(offset, DrawingTool.TEXT)
                            if (hitIndex != null) {
                                onPathSelectionChanged(hitIndex, paths.getOrNull(hitIndex))
                            } else {
                                val hadSelection = selectedPathIndex != null
                                onPathSelectionChanged(null, null)
                                if (!hadSelection) {
                                    openTextDialog(null, offset, "")
                                }
                            }
                        },
                        onDoubleTap = { offset ->
                            val hitIndex = hitIndexAt(offset, DrawingTool.TEXT)
                            val path = hitIndex?.let { paths.getOrNull(it) as? DrawingPath.TextPath }
                            if (path != null) {
                                onPathSelectionChanged(hitIndex, path)
                                openTextDialog(hitIndex, path.position, path.text)
                            }
                        }
                    )

                    DrawingTool.ARROW, DrawingTool.RECTANGLE, DrawingTool.CIRCLE -> {
                        detectTapGestures(
                            onTap = { onPathSelectionChanged(null, null) }
                        )
                    }

                    DrawingTool.PEN -> Unit
                    DrawingTool.ERASER -> Unit

                    null -> detectTapGestures(
                        onTap = { onPathSelectionChanged(null, null) }
                    )
                }
            }
            .pointerInput(currentTool, selectedPathIndex, paths.size) {
                val selectedText = selectedPathIndex?.let { paths.getOrNull(it) as? DrawingPath.TextPath }
                val selectedRectangle = selectedPathIndex?.let { paths.getOrNull(it) as? DrawingPath.RectanglePath }
                if (
                    (selectedText != null && currentTool == DrawingTool.MOVE) ||
                    (selectedRectangle != null && currentTool == DrawingTool.MOVE)
                ) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val index = selectedPathIndex
                        when (val path = paths.getOrNull(index)) {
                            is DrawingPath.TextPath -> {
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

                            is DrawingPath.RectanglePath -> {
                                if (pan == Offset.Zero && zoom == 1f && rotation == 0f) return@detectTransformGestures
                                onPathUpdated(
                                    index,
                                    transformRectangle(path, pan, zoom, rotation)
                                )
                            }

                            else -> Unit
                        }
                    }
                }
            }
    ) {
        paths.forEachIndexed { index, drawingPath ->
            when (drawingPath) {
                is DrawingPath.PenPath -> drawPath(
                    path = drawingPath.path,
                    color = drawingPath.color,
                    style = Stroke(
                        width = drawingPath.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                is DrawingPath.ArrowPath -> drawArrow(
                    start = drawingPath.start,
                    end = drawingPath.end,
                    color = drawingPath.color,
                    strokeWidth = drawingPath.strokeWidth
                )

                is DrawingPath.RectanglePath -> {
                    val center = rectangleCenter(drawingPath)
                    rotate(drawingPath.rotation, pivot = center) {
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
                }

                is DrawingPath.CirclePath -> drawCircle(
                    color = drawingPath.color,
                    radius = drawingPath.radius,
                    center = drawingPath.center,
                    style = if (drawingPath.filled) Fill else Stroke(width = drawingPath.strokeWidth)
                )

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
                        drawText(textLayoutResult = textLayout, topLeft = drawingPath.position)
                    }
                }
            }

            if (selectedPathIndex == index) {
                when (drawingPath) {
                    is DrawingPath.TextPath -> drawTextSelection(drawingPath, measureText(drawingPath))
                    else -> drawPathSelection(drawingPath)
                }
            }
        }

        if (pathVersion >= 0) {
            // Trigger redraw while free drawing.
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
                DrawingTool.RECTANGLE -> drawRect(
                    color = currentColor,
                    topLeft = Offset(min(start.x, end.x), min(start.y, end.y)),
                    size = Size(abs(end.x - start.x), abs(end.y - start.y)),
                    style = if (shapeFilled) Fill else Stroke(width = strokeWidth)
                )

                DrawingTool.CIRCLE -> drawCircle(
                    color = currentColor,
                    radius = (end - start).getDistance(),
                    center = start,
                    style = if (shapeFilled) Fill else Stroke(width = strokeWidth)
                )

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
            onDismissRequest = {
                textTargetIndex = null
                showTextDialog = false
            },
            title = {
                Text(
                    if (textTargetIndex == null) {
                        androidx.compose.ui.res.stringResource(R.string.editor_add_text)
                    } else {
                        androidx.compose.ui.res.stringResource(R.string.editor_edit_text)
                    }
                )
            },
            text = {
                OutlinedTextField(
                    value = textDraft,
                    onValueChange = { textDraft = it },
                    label = { Text(androidx.compose.ui.res.stringResource(R.string.editor_text_input_label)) },
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
                                if (selectedPathIndex == existingIndex) {
                                    onPathSelectionChanged(null, null)
                                }
                            } else {
                                val oldPath = paths.getOrNull(existingIndex) as? DrawingPath.TextPath
                                if (oldPath != null) {
                                    val updatedPath = oldPath.copy(
                                        text = text,
                                        color = currentColor,
                                        fontSize = textSize,
                                        outlineEnabled = textOutlineEnabled
                                    )
                                    onPathUpdated(existingIndex, updatedPath)
                                    onPathSelectionChanged(existingIndex, updatedPath)
                                }
                            }
                        } else if (text.isNotEmpty()) {
                            onPathAdded(
                                DrawingPath.TextPath(
                                    position = textTargetPosition,
                                    text = text,
                                    color = currentColor,
                                    fontSize = textSize,
                                    outlineEnabled = textOutlineEnabled
                                )
                            )
                        }
                        textTargetIndex = null
                        showTextDialog = false
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        textTargetIndex = null
                        showTextDialog = false
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.action_cancel))
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

private fun DrawScope.drawPathSelection(path: DrawingPath) {
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

private fun DrawScope.drawArrowSelection(path: DrawingPath.ArrowPath) {
    drawLine(
        color = Color.White.copy(alpha = 0.92f),
        start = path.start,
        end = path.end,
        strokeWidth = max(path.strokeWidth + 4f, 5f)
    )
    drawHandle(path.start, path.color)
    drawHandle(path.end, path.color)
}

private fun DrawScope.drawCircleSelection(path: DrawingPath.CirclePath) {
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = path.radius,
        center = path.center,
        style = Stroke(width = 2f)
    )
    drawHandle(Offset(path.center.x + path.radius, path.center.y), path.color)
}

private fun DrawScope.drawRectangleSelection(path: DrawingPath.RectanglePath) {
    val corners = rectangleCorners(path)
    val accent = path.color
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
    corners.forEach { handle ->
        drawHandle(handle, accent)
    }
}

private fun DrawScope.drawSelectionBounds(bounds: Rect, accent: Color) {
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

private fun DrawScope.drawHandle(center: Offset, accent: Color, radius: Float = 6f) {
    drawCircle(color = accent, radius = radius, center = center, style = Fill)
    drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = 1.5f))
}

private fun selectionAccentColor(path: DrawingPath): Color {
    return when (path) {
        is DrawingPath.PenPath -> path.color
        is DrawingPath.ArrowPath -> path.color
        is DrawingPath.RectanglePath -> path.color
        is DrawingPath.CirclePath -> path.color
        is DrawingPath.TextPath -> path.color
    }
}

private fun boundsForPath(path: DrawingPath): Rect? {
    return when (path) {
        is DrawingPath.PenPath -> {
            if (path.points.isEmpty()) {
                null
            } else {
                Rect(
                    path.points.minOf { it.x } - path.strokeWidth * 1.5f,
                    path.points.minOf { it.y } - path.strokeWidth * 1.5f,
                    path.points.maxOf { it.x } + path.strokeWidth * 1.5f,
                    path.points.maxOf { it.y } + path.strokeWidth * 1.5f
                )
            }
        }

        is DrawingPath.ArrowPath -> Rect(
            min(path.start.x, path.end.x) - path.strokeWidth * 2f,
            min(path.start.y, path.end.y) - path.strokeWidth * 2f,
            max(path.start.x, path.end.x) + path.strokeWidth * 2f,
            max(path.start.y, path.end.y) + path.strokeWidth * 2f
        )

        is DrawingPath.RectanglePath -> {
            val corners = rectangleCorners(path)
            Rect(
                left = corners.minOf { it.x },
                top = corners.minOf { it.y },
                right = corners.maxOf { it.x },
                bottom = corners.maxOf { it.y }
            )
        }
        is DrawingPath.CirclePath -> Rect(
            path.center.x - path.radius,
            path.center.y - path.radius,
            path.center.x + path.radius,
            path.center.y + path.radius
        )

        is DrawingPath.TextPath -> estimateTextBounds(path)
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

private fun isPathHit(path: DrawingPath, touch: Offset, radius: Float): Boolean {
    return when (path) {
        is DrawingPath.PenPath -> distanceToPolyline(touch, path.points) <= max(radius * 1.35f, path.strokeWidth * 2.8f)
        is DrawingPath.ArrowPath -> distanceToSegment(touch, path.start, path.end) <= max(radius * 1.25f, path.strokeWidth * 2.6f)
        is DrawingPath.RectanglePath -> isRectangleHit(path, touch, radius)
        is DrawingPath.CirclePath -> {
            val distance = (touch - path.center).getDistance()
            distance <= path.radius + max(radius, path.strokeWidth * 1.5f)
        }
        is DrawingPath.TextPath -> estimateTextBounds(path)
            .inflate(max(radius * 1.5f, path.fontSize * path.scale * 0.35f))
            .contains(touch)
    }
}

private fun resizeHandleHit(path: DrawingPath, touch: Offset): PathResizeHandle? {
    return when (path) {
        is DrawingPath.ArrowPath -> listOf(
            PathResizeHandle.ArrowStart to path.start,
            PathResizeHandle.ArrowEnd to path.end
        ).firstOrNull { (_, center) ->
            (center - touch).getDistance() <= HANDLE_HIT_RADIUS * 1.2f
        }?.first

        is DrawingPath.RectanglePath -> listOf(
            PathResizeHandle.RectTopLeft to rectangleCorners(path)[0],
            PathResizeHandle.RectTopRight to rectangleCorners(path)[1],
            PathResizeHandle.RectBottomRight to rectangleCorners(path)[2],
            PathResizeHandle.RectBottomLeft to rectangleCorners(path)[3]
        ).firstOrNull { (_, center) ->
            (center - touch).getDistance() <= HANDLE_HIT_RADIUS
        }?.first

        is DrawingPath.CirclePath -> {
            val handle = Offset(path.center.x + path.radius, path.center.y)
            PathResizeHandle.CircleRadius.takeIf {
                (handle - touch).getDistance() <= HANDLE_HIT_RADIUS
            }
        }

        else -> null
    }
}

private fun movePath(path: DrawingPath, delta: Offset): DrawingPath {
    return when (path) {
        is DrawingPath.PenPath -> {
            val movedPoints = path.points.map { it + delta }
            path.copy(path = buildComposePath(movedPoints), points = movedPoints)
        }
        is DrawingPath.ArrowPath -> path.copy(start = path.start + delta, end = path.end + delta)
        is DrawingPath.RectanglePath -> path.copy(topLeft = path.topLeft + delta, bottomRight = path.bottomRight + delta)
        is DrawingPath.CirclePath -> path.copy(center = path.center + delta)
        is DrawingPath.TextPath -> path.copy(position = path.position + delta)
    }
}

private fun resizePath(path: DrawingPath, handle: PathResizeHandle, touch: Offset): DrawingPath {
    return when {
        path is DrawingPath.ArrowPath && handle is PathResizeHandle.ArrowStart -> {
            path.copy(start = touch)
        }

        path is DrawingPath.ArrowPath && handle is PathResizeHandle.ArrowEnd -> {
            path.copy(end = touch)
        }

        path is DrawingPath.RectanglePath -> {
            val center = rectangleCenter(path)
            val localTouch = rotatePointAround(touch, center, -path.rotation)
            val currentTopLeft = path.topLeft
            val currentBottomRight = path.bottomRight
            val updatedTopLeft = when (handle) {
                PathResizeHandle.RectTopLeft,
                PathResizeHandle.RectBottomLeft -> Offset(localTouch.x, currentTopLeft.y)

                else -> currentTopLeft
            }
            val updatedBottomRight = when (handle) {
                PathResizeHandle.RectTopRight,
                PathResizeHandle.RectBottomRight -> Offset(localTouch.x, currentBottomRight.y)

                else -> currentBottomRight
            }
            val topY = when (handle) {
                PathResizeHandle.RectTopLeft,
                PathResizeHandle.RectTopRight -> localTouch.y

                else -> currentTopLeft.y
            }
            val bottomY = when (handle) {
                PathResizeHandle.RectBottomLeft,
                PathResizeHandle.RectBottomRight -> localTouch.y

                else -> currentBottomRight.y
            }

            path.copy(
                topLeft = Offset(
                    x = min(updatedTopLeft.x, updatedBottomRight.x),
                    y = min(topY, bottomY)
                ),
                bottomRight = Offset(
                    x = max(updatedTopLeft.x, updatedBottomRight.x),
                    y = max(topY, bottomY)
                ),
                rotation = path.rotation
            )
        }

        path is DrawingPath.CirclePath && handle is PathResizeHandle.CircleRadius -> {
            path.copy(radius = max((touch - path.center).getDistance(), MIN_CIRCLE_RADIUS))
        }

        else -> path
    }
}

private fun buildComposePath(points: List<Offset>): Path {
    return Path().apply {
        val first = points.firstOrNull() ?: return@apply
        moveTo(first.x, first.y)
        points.drop(1).forEach { point -> lineTo(point.x, point.y) }
    }
}

private fun rectangleCenter(path: DrawingPath.RectanglePath): Offset {
    return Offset(
        x = (path.topLeft.x + path.bottomRight.x) / 2f,
        y = (path.topLeft.y + path.bottomRight.y) / 2f
    )
}

private fun rectangleCorners(path: DrawingPath.RectanglePath): List<Offset> {
    val center = rectangleCenter(path)
    val corners = listOf(
        path.topLeft,
        Offset(path.bottomRight.x, path.topLeft.y),
        path.bottomRight,
        Offset(path.topLeft.x, path.bottomRight.y)
    )
    return corners.map { rotatePointAround(it, center, path.rotation) }
}

private fun rotatePointAround(point: Offset, pivot: Offset, rotationDegrees: Float): Offset {
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val translatedX = point.x - pivot.x
    val translatedY = point.y - pivot.y
    val rotatedX = (translatedX * cos(radians) - translatedY * sin(radians)).toFloat()
    val rotatedY = (translatedX * sin(radians) + translatedY * cos(radians)).toFloat()
    return Offset(rotatedX + pivot.x, rotatedY + pivot.y)
}

private fun isRectangleHit(path: DrawingPath.RectanglePath, touch: Offset, radius: Float): Boolean {
    val center = rectangleCenter(path)
    val localTouch = rotatePointAround(touch, center, -path.rotation)
    return Rect(path.topLeft, path.bottomRight).inflate(radius).contains(localTouch)
}

private fun transformRectangle(
    path: DrawingPath.RectanglePath,
    pan: Offset,
    zoom: Float,
    rotation: Float
): DrawingPath.RectanglePath {
    val center = rectangleCenter(path) + pan
    val halfWidth = ((path.bottomRight.x - path.topLeft.x) / 2f * zoom).coerceAtLeast(12f)
    val halfHeight = ((path.bottomRight.y - path.topLeft.y) / 2f * zoom).coerceAtLeast(12f)
    return path.copy(
        topLeft = Offset(center.x - halfWidth, center.y - halfHeight),
        bottomRight = Offset(center.x + halfWidth, center.y + halfHeight),
        rotation = normalizeRotation(path.rotation + rotation)
    )
}

private fun estimateTextBounds(path: DrawingPath.TextPath): Rect {
    val lines = path.text.split('\n')
    val widestLine = lines.maxOfOrNull { it.length } ?: 1
    val width = widestLine.coerceAtLeast(1) * path.fontSize * path.scale * 0.62f
    val height = lines.size.coerceAtLeast(1) * path.fontSize * path.scale * 1.35f
    return Rect(
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
    val projection = Offset(start.x + (clamped * segment.x), start.y + (clamped * segment.y))
    return (point - projection).getDistance()
}

private fun distanceToPolyline(point: Offset, points: List<Offset>): Float {
    if (points.isEmpty()) return Float.MAX_VALUE
    if (points.size == 1) return (point - points.first()).getDistance()

    var minDistance = Float.MAX_VALUE
    for (index in 0 until points.lastIndex) {
        minDistance = min(minDistance, distanceToSegment(point, points[index], points[index + 1]))
    }
    return minDistance
}

private fun toolFor(path: DrawingPath): DrawingTool {
    return when (path) {
        is DrawingPath.PenPath -> DrawingTool.PEN
        is DrawingPath.ArrowPath -> DrawingTool.ARROW
        is DrawingPath.RectanglePath -> DrawingTool.RECTANGLE
        is DrawingPath.CirclePath -> DrawingTool.CIRCLE
        is DrawingPath.TextPath -> DrawingTool.TEXT
    }
}

private fun DrawScope.drawTextSelection(path: DrawingPath.TextPath, layout: TextLayoutResult) {
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

private fun DrawScope.drawArrow(start: Offset, end: Offset, color: Color, strokeWidth: Float) {
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
