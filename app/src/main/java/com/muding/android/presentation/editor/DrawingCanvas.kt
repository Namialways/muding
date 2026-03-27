package com.muding.android.presentation.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.drawText
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool
import kotlin.math.abs
import kotlin.math.min

private const val SHAPE_CREATION_THRESHOLD = 6f
private const val SELECTION_HIT_RADIUS = 18f

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
    viewportScale: Float,
    viewportOffset: Offset,
    onPathAdded: (DrawingPath) -> Unit,
    onPathUpdated: (Int, DrawingPath) -> Unit,
    onPathReplaced: (Int, List<DrawingPath>) -> Unit,
    onPathRemoved: (Int) -> Unit,
    onPathSelectionChanged: (Int?, DrawingPath?) -> Unit,
    onCanvasSizeChanged: (Size) -> Unit,
    onViewportResetRequested: () -> Unit
) {
    val interactionState = rememberEditorCanvasInteractionState()
    val textEditState = rememberEditorTextEditState()
    val pathHitTester = rememberEditorPathHitTester()
    val localCanvasSize = remember { mutableStateOf(Size.Zero) }
    val latestPaths = rememberUpdatedState(paths)
    val latestSelectedPathIndex = rememberUpdatedState(selectedPathIndex)
    val callbacks = remember(
        onPathAdded,
        onPathUpdated,
        onPathSelectionChanged
    ) {
        EditorCanvasCallbacks(
            onPathAdded = onPathAdded,
            onPathUpdated = onPathUpdated,
            onPathSelectionChanged = onPathSelectionChanged
        )
    }

    fun toCanvasOffset(rawOffset: Offset): Offset {
        if (viewportScale == 1f && viewportOffset == Offset.Zero) return rawOffset
        val pivot = Offset(localCanvasSize.value.width / 2f, localCanvasSize.value.height / 2f)
        val scale = viewportScale.coerceAtLeast(0.01f)
        return Offset(
            x = ((rawOffset.x - viewportOffset.x - pivot.x) / scale) + pivot.x,
            y = ((rawOffset.y - viewportOffset.y - pivot.y) / scale) + pivot.y
        )
    }

    fun toCanvasDelta(rawDelta: Offset): Offset {
        val scale = viewportScale.coerceAtLeast(0.01f)
        return Offset(rawDelta.x / scale, rawDelta.y / scale)
    }

    val canvasModifier = modifier
        .fillMaxSize()
        .onSizeChanged {
            val size = Size(it.width.toFloat(), it.height.toFloat())
            localCanvasSize.value = size
            onCanvasSizeChanged(size)
        }
        .bindCanvasInteropInput(
            currentTool = currentTool,
            interactionState = interactionState,
            latestPaths = latestPaths,
            toCanvasOffset = ::toCanvasOffset,
            eraserMode = eraserMode,
            eraserSize = eraserSize,
            onPathReplaced = onPathReplaced,
            onPathRemoved = onPathRemoved
        )
        .let { baseModifier ->
            if (currentTool == DrawingTool.MOVE) {
                baseModifier.bindMoveModeGestures(
                    latestPaths = latestPaths,
                    latestSelectedPathIndex = latestSelectedPathIndex,
                    toCanvasOffset = ::toCanvasOffset,
                    toCanvasDelta = ::toCanvasDelta,
                    interactionState = interactionState,
                    pathHitTester = pathHitTester,
                    callbacks = callbacks,
                    textEditState = textEditState,
                    selectionHitRadius = SELECTION_HIT_RADIUS
                )
            } else {
                baseModifier
                    .bindCanvasDragGestures(
                        currentTool = currentTool,
                        latestPaths = latestPaths,
                        latestSelectedPathIndex = latestSelectedPathIndex,
                        currentColor = currentColor,
                        strokeWidth = strokeWidth,
                        shapeFilled = shapeFilled,
                        toCanvasOffset = ::toCanvasOffset,
                        toCanvasDelta = ::toCanvasDelta,
                        interactionState = interactionState,
                        pathHitTester = pathHitTester,
                        callbacks = callbacks,
                        selectionHitRadius = SELECTION_HIT_RADIUS,
                        creationThreshold = SHAPE_CREATION_THRESHOLD
                    )
                    .bindCanvasTapGestures(
                        currentTool = currentTool,
                        latestPaths = latestPaths,
                        latestSelectedPathIndex = latestSelectedPathIndex,
                        toCanvasOffset = ::toCanvasOffset,
                        pathHitTester = pathHitTester,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        selectionHitRadius = SELECTION_HIT_RADIUS,
                        onViewportResetRequested = onViewportResetRequested
                    )
            }
        }

    fun androidx.compose.ui.graphics.drawscope.DrawScope.renderDrawingPath(
        drawingPath: DrawingPath,
        isSelected: Boolean
    ) {
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
                val textLayout = pathHitTester.measureText(drawingPath)
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

        if (isSelected) {
            when (drawingPath) {
                is DrawingPath.TextPath -> drawTextSelection(drawingPath, pathHitTester.measureText(drawingPath))
                else -> drawPathSelection(drawingPath)
            }
        }
    }

    Canvas(modifier = canvasModifier) {
        paths.forEachIndexed { index, drawingPath ->
            val isActive = index == interactionState.movingPathIndex || index == interactionState.resizingState?.index
            if (!isActive) {
                renderDrawingPath(
                    drawingPath = drawingPath,
                    isSelected = selectedPathIndex == index
                )
            }
        }

        interactionState.activePreviewPath?.let { previewPath ->
            val activeIndex = interactionState.movingPathIndex ?: interactionState.resizingState?.index
            val isSelected = activeIndex != null && activeIndex == selectedPathIndex
            renderDrawingPath(previewPath, isSelected)
        }

        if (interactionState.pathVersion >= 0) {
            // Trigger redraw while free drawing.
        }

        interactionState.currentPath?.let { path ->
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

        val start = interactionState.startPoint
        val end = interactionState.endPoint
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
            interactionState.eraserPreviewCenter?.let { center ->
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

    EditorTextEditDialog(
        state = textEditState,
        paths = paths,
        selectedPathIndex = selectedPathIndex,
        currentColor = currentColor,
        textSize = textSize,
        textOutlineEnabled = textOutlineEnabled,
        onPathAdded = onPathAdded,
        onPathUpdated = onPathUpdated,
        onPathRemoved = onPathRemoved,
        onPathSelectionChanged = onPathSelectionChanged
    )
}
