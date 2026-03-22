package com.muding.android.presentation.editor

import android.view.MotionEvent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.runtime.State
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool
import kotlin.math.PI
import kotlin.math.abs

fun Modifier.bindMoveModeGestures(
    latestPaths: State<List<DrawingPath>>,
    latestSelectedPathIndex: State<Int?>,
    toCanvasOffset: (Offset) -> Offset,
    toCanvasDelta: (Offset) -> Offset,
    interactionState: EditorCanvasInteractionState,
    pathHitTester: EditorPathHitTester,
    callbacks: EditorCanvasCallbacks,
    textEditState: EditorTextEditState,
    selectionHitRadius: Float
): Modifier {
    return this
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    handleMoveDragStart(
                        interactionState = interactionState,
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectedPathIndex = latestSelectedPathIndex.value,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        fallbackOffset = interactionState.moveGestureDownOffset ?: toCanvasOffset(offset)
                    )
                },
                onDrag = { change, dragAmount ->
                    handleMoveDrag(
                        interactionState = interactionState,
                        paths = latestPaths.value,
                        callbacks = callbacks,
                        dragAmount = toCanvasDelta(dragAmount),
                        touch = toCanvasOffset(change.position)
                    )
                    change.consume()
                },
                onDragEnd = {
                    handleMoveDragEnd(
                        interactionState = interactionState,
                        paths = latestPaths.value,
                        callbacks = callbacks
                    )
                },
                onDragCancel = { interactionState.resetDragState() }
            )
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    handleMoveTap(
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        offset = toCanvasOffset(offset)
                    )
                },
                onLongPress = { offset ->
                    handleMoveTextEditRequest(
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = toCanvasOffset(offset)
                    )
                },
                onDoubleTap = { offset ->
                    handleMoveTextEditRequest(
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = toCanvasOffset(offset)
                    )
                }
            )
        }
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, rotation ->
                handleSelectionTransform(
                    currentTool = DrawingTool.MOVE,
                    selectedPathIndex = latestSelectedPathIndex.value,
                    paths = latestPaths.value,
                    pan = toCanvasDelta(pan),
                    zoom = zoom,
                    rotation = rotation,
                    callbacks = callbacks
                )
            }
        }
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.bindCanvasInteropInput(
    currentTool: DrawingTool?,
    interactionState: EditorCanvasInteractionState,
    latestPaths: State<List<DrawingPath>>,
    toCanvasOffset: (Offset) -> Offset,
    eraserMode: EraserMode,
    eraserSize: Float,
    onPathReplaced: (Int, List<DrawingPath>) -> Unit,
    onPathRemoved: (Int) -> Unit
): Modifier {
    return pointerInteropFilter { event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (currentTool == DrawingTool.MOVE) {
                    interactionState.moveGestureDownOffset = toCanvasOffset(Offset(event.x, event.y))
                }
                if (currentTool != DrawingTool.ERASER) return@pointerInteropFilter false
                val touch = toCanvasOffset(Offset(event.x, event.y))
                interactionState.eraserPreviewCenter = touch
                erasePathAt(
                    touch = touch,
                    paths = latestPaths.value,
                    eraserMode = eraserMode,
                    eraserSize = eraserSize,
                    onPathReplaced = onPathReplaced,
                    onPathRemoved = onPathRemoved
                )
                true
            }

            MotionEvent.ACTION_MOVE -> {
                if (currentTool != DrawingTool.ERASER) return@pointerInteropFilter false
                val touch = toCanvasOffset(Offset(event.x, event.y))
                interactionState.eraserPreviewCenter = touch
                erasePathAt(
                    touch = touch,
                    paths = latestPaths.value,
                    eraserMode = eraserMode,
                    eraserSize = eraserSize,
                    onPathReplaced = onPathReplaced,
                    onPathRemoved = onPathRemoved
                )
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                interactionState.moveGestureDownOffset = null
                if (currentTool != DrawingTool.ERASER) return@pointerInteropFilter false
                interactionState.eraserPreviewCenter = null
                true
            }

            else -> false
        }
    }
}

fun Modifier.bindCanvasDragGestures(
    currentTool: DrawingTool?,
    latestPaths: State<List<DrawingPath>>,
    latestSelectedPathIndex: State<Int?>,
    currentColor: Color,
    strokeWidth: Float,
    shapeFilled: Boolean,
    toCanvasOffset: (Offset) -> Offset,
    toCanvasDelta: (Offset) -> Offset,
    interactionState: EditorCanvasInteractionState,
    pathHitTester: EditorPathHitTester,
    callbacks: EditorCanvasCallbacks,
    selectionHitRadius: Float,
    creationThreshold: Float
): Modifier {
    return pointerInput(currentTool, strokeWidth, currentColor, shapeFilled) {
        when (currentTool) {
            DrawingTool.MOVE -> Unit

            DrawingTool.PEN -> detectDragGestures(
                onDragStart = { offset ->
                    handlePenDragStart(
                        interactionState = interactionState,
                        callbacks = callbacks,
                        offset = toCanvasOffset(offset)
                    )
                },
                onDrag = { change, _ ->
                    handlePenDrag(interactionState, toCanvasOffset(change.position))
                    change.consume()
                },
                onDragEnd = {
                    handlePenDragEnd(
                        interactionState = interactionState,
                        currentColor = currentColor,
                        strokeWidth = strokeWidth,
                        callbacks = callbacks
                    )
                },
                onDragCancel = { interactionState.resetDragState() }
            )

            DrawingTool.ERASER -> Unit

            DrawingTool.ARROW, DrawingTool.RECTANGLE, DrawingTool.CIRCLE -> detectDragGestures(
                onDragStart = { offset ->
                    handleShapeDragStart(
                        interactionState = interactionState,
                        callbacks = callbacks,
                        offset = toCanvasOffset(offset)
                    )
                },
                onDrag = { change, _ ->
                    handleShapeDrag(interactionState, toCanvasOffset(change.position))
                    change.consume()
                },
                onDragEnd = {
                    handleShapeDragEnd(
                        interactionState = interactionState,
                        currentTool = currentTool,
                        currentColor = currentColor,
                        strokeWidth = strokeWidth,
                        shapeFilled = shapeFilled,
                        creationThreshold = creationThreshold,
                        callbacks = callbacks
                    )
                },
                onDragCancel = { interactionState.resetDragState() }
            )

            DrawingTool.TEXT -> detectDragGestures(
                onDragStart = { offset ->
                    handleTextDragStart(
                        interactionState = interactionState,
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        callbacks = callbacks,
                        selectionHitRadius = selectionHitRadius,
                        offset = toCanvasOffset(offset)
                    )
                },
                onDrag = { change, dragAmount ->
                    handleTextDrag(
                        interactionState = interactionState,
                        paths = latestPaths.value,
                        callbacks = callbacks,
                        dragAmount = toCanvasDelta(dragAmount)
                    )
                    change.consume()
                },
                onDragEnd = {
                    handleTextDragEnd(
                        interactionState = interactionState,
                        paths = latestPaths.value,
                        callbacks = callbacks
                    )
                },
                onDragCancel = { interactionState.resetDragState() }
            )

            null -> Unit
        }
    }
}

fun Modifier.bindCanvasTapGestures(
    currentTool: DrawingTool?,
    latestPaths: State<List<DrawingPath>>,
    latestSelectedPathIndex: State<Int?>,
    toCanvasOffset: (Offset) -> Offset,
    pathHitTester: EditorPathHitTester,
    callbacks: EditorCanvasCallbacks,
    textEditState: EditorTextEditState,
    selectionHitRadius: Float,
    onViewportResetRequested: () -> Unit
): Modifier {
    return pointerInput(currentTool) {
        when (currentTool) {
            DrawingTool.MOVE -> detectTapGestures(
                onTap = { offset ->
                    handleMoveTap(
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        offset = toCanvasOffset(offset)
                    )
                },
                onLongPress = { offset ->
                    handleMoveTextEditRequest(
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = toCanvasOffset(offset)
                    )
                },
                onDoubleTap = { offset ->
                    handleMoveTextEditRequest(
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = toCanvasOffset(offset)
                    )
                }
            )

            DrawingTool.TEXT -> detectTapGestures(
                onTap = { offset ->
                    handleTextToolTap(
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectionHitRadius = selectionHitRadius,
                        selectedPathIndex = latestSelectedPathIndex.value,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = toCanvasOffset(offset)
                    )
                },
                onDoubleTap = { offset ->
                    handleTextToolTap(
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectionHitRadius = selectionHitRadius,
                        selectedPathIndex = latestSelectedPathIndex.value,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = toCanvasOffset(offset)
                    )
                }
            )

            DrawingTool.ARROW, DrawingTool.RECTANGLE, DrawingTool.CIRCLE -> {
                detectTapGestures(
                    onTap = { callbacks.onPathSelectionChanged(null, null) }
                )
            }

            DrawingTool.PEN -> Unit
            DrawingTool.ERASER -> Unit

            null -> detectTapGestures(
                onTap = { callbacks.onPathSelectionChanged(null, null) },
                onDoubleTap = {
                    callbacks.onPathSelectionChanged(null, null)
                    onViewportResetRequested()
                }
            )
        }
    }
}

fun Modifier.bindCanvasTransformGestures(
    currentTool: DrawingTool?,
    latestSelectedPathIndex: State<Int?>,
    latestPaths: State<List<DrawingPath>>,
    toCanvasDelta: (Offset) -> Offset,
    callbacks: EditorCanvasCallbacks
): Modifier {
    return pointerInput(currentTool) {
        if (currentTool == DrawingTool.MOVE) {
            detectMultiTouchTransformGestures { pan, zoom, rotation ->
                handleSelectionTransform(
                    currentTool = currentTool,
                    selectedPathIndex = latestSelectedPathIndex.value,
                    paths = latestPaths.value,
                    pan = toCanvasDelta(pan),
                    zoom = zoom,
                    rotation = rotation,
                    callbacks = callbacks
                )
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectMultiTouchTransformGestures(
    onGesture: (pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var pastTouchSlop = false

        do {
            val event = awaitPointerEvent()
            val activePointers = event.changes.count { it.pressed }

            if (activePointers < 2) {
                if (pastTouchSlop) break
                continue
            }

            val panChange = event.calculatePan()
            val zoomChange = event.calculateZoom()
            val rotationChange = event.calculateRotation()

            if (!pastTouchSlop) {
                val panMotion = panChange.getDistance()
                val zoomMotion = abs(1f - zoomChange)
                val rotationMotion = abs(rotationChange * PI.toFloat() / 180f)
                if (panMotion > viewConfiguration.touchSlop ||
                    zoomMotion > 0.01f ||
                    rotationMotion > 0.01f
                ) {
                    pastTouchSlop = true
                }
            }

            if (pastTouchSlop) {
                onGesture(panChange, zoomChange, rotationChange)
                event.changes.forEach { change ->
                    if (change.positionChanged()) {
                        change.consume()
                    }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}
