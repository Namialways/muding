package com.muding.android.presentation.editor

import android.view.MotionEvent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.bindCanvasInteropInput(
    currentTool: DrawingTool?,
    interactionState: EditorCanvasInteractionState,
    paths: List<DrawingPath>,
    eraserMode: EraserMode,
    eraserSize: Float,
    onPathReplaced: (Int, List<DrawingPath>) -> Unit,
    onPathRemoved: (Int) -> Unit
): Modifier {
    return pointerInteropFilter { event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (currentTool == DrawingTool.MOVE) {
                    interactionState.moveGestureDownOffset = Offset(event.x, event.y)
                }
                if (currentTool != DrawingTool.ERASER) return@pointerInteropFilter false
                val touch = Offset(event.x, event.y)
                interactionState.eraserPreviewCenter = touch
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

            MotionEvent.ACTION_MOVE -> {
                if (currentTool != DrawingTool.ERASER) return@pointerInteropFilter false
                val touch = Offset(event.x, event.y)
                interactionState.eraserPreviewCenter = touch
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
    paths: List<DrawingPath>,
    selectedPathIndex: Int?,
    currentColor: Color,
    strokeWidth: Float,
    shapeFilled: Boolean,
    interactionState: EditorCanvasInteractionState,
    pathHitTester: EditorPathHitTester,
    callbacks: EditorCanvasCallbacks,
    selectionHitRadius: Float,
    creationThreshold: Float
): Modifier {
    return pointerInput(currentTool, paths.size, selectedPathIndex, strokeWidth, currentColor, shapeFilled) {
        when (currentTool) {
            DrawingTool.MOVE -> detectDragGestures(
                onDragStart = { offset ->
                    handleMoveDragStart(
                        interactionState = interactionState,
                        pathHitTester = pathHitTester,
                        paths = paths,
                        selectedPathIndex = selectedPathIndex,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        fallbackOffset = offset
                    )
                },
                onDrag = { change, dragAmount ->
                    handleMoveDrag(
                        interactionState = interactionState,
                        paths = paths,
                        callbacks = callbacks,
                        dragAmount = dragAmount,
                        touch = change.position
                    )
                    change.consume()
                },
                onDragEnd = {
                    handleMoveDragEnd(
                        interactionState = interactionState,
                        paths = paths,
                        callbacks = callbacks
                    )
                },
                onDragCancel = { interactionState.resetDragState() }
            )

            DrawingTool.PEN -> detectDragGestures(
                onDragStart = { offset ->
                    handlePenDragStart(
                        interactionState = interactionState,
                        callbacks = callbacks,
                        offset = offset
                    )
                },
                onDrag = { change, _ ->
                    handlePenDrag(interactionState, change.position)
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
                        offset = offset
                    )
                },
                onDrag = { change, _ ->
                    handleShapeDrag(interactionState, change.position)
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
                        paths = paths,
                        callbacks = callbacks,
                        selectionHitRadius = selectionHitRadius,
                        offset = offset
                    )
                },
                onDrag = { change, dragAmount ->
                    handleTextDrag(
                        interactionState = interactionState,
                        paths = paths,
                        callbacks = callbacks,
                        dragAmount = dragAmount
                    )
                    change.consume()
                },
                onDragEnd = {
                    handleTextDragEnd(
                        interactionState = interactionState,
                        paths = paths,
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
    paths: List<DrawingPath>,
    selectedPathIndex: Int?,
    pathHitTester: EditorPathHitTester,
    callbacks: EditorCanvasCallbacks,
    textEditState: EditorTextEditState,
    selectionHitRadius: Float,
    onViewportResetRequested: () -> Unit
): Modifier {
    return pointerInput(currentTool, paths.size, selectedPathIndex) {
        when (currentTool) {
            DrawingTool.MOVE -> detectTapGestures(
                onTap = { offset ->
                    handleMoveTap(
                        pathHitTester = pathHitTester,
                        paths = paths,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        offset = offset
                    )
                },
                onLongPress = { offset ->
                    handleMoveTextEditRequest(
                        pathHitTester = pathHitTester,
                        paths = paths,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = offset
                    )
                },
                onDoubleTap = { offset ->
                    handleMoveTextEditRequest(
                        pathHitTester = pathHitTester,
                        paths = paths,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = offset
                    )
                }
            )

            DrawingTool.TEXT -> detectTapGestures(
                onTap = { offset ->
                    handleTextToolTap(
                        pathHitTester = pathHitTester,
                        paths = paths,
                        selectionHitRadius = selectionHitRadius,
                        selectedPathIndex = selectedPathIndex,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = offset
                    )
                },
                onDoubleTap = { offset ->
                    handleTextToolTap(
                        pathHitTester = pathHitTester,
                        paths = paths,
                        selectionHitRadius = selectionHitRadius,
                        selectedPathIndex = selectedPathIndex,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = offset
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
    selectedPathIndex: Int?,
    paths: List<DrawingPath>,
    callbacks: EditorCanvasCallbacks
): Modifier {
    return pointerInput(currentTool, selectedPathIndex, paths.size) {
        val selectedText = selectedPathIndex?.let { paths.getOrNull(it) as? DrawingPath.TextPath }
        val selectedRectangle = selectedPathIndex?.let { paths.getOrNull(it) as? DrawingPath.RectanglePath }
        val selectedCircle = selectedPathIndex?.let { paths.getOrNull(it) as? DrawingPath.CirclePath }
        if (
            (selectedText != null && currentTool == DrawingTool.MOVE) ||
            (selectedRectangle != null && currentTool == DrawingTool.MOVE) ||
            (selectedCircle != null && currentTool == DrawingTool.MOVE)
        ) {
            detectTransformGestures { _, pan, zoom, rotation ->
                handleSelectionTransform(
                    currentTool = currentTool,
                    selectedPathIndex = selectedPathIndex,
                    paths = paths,
                    pan = pan,
                    zoom = zoom,
                    rotation = rotation,
                    callbacks = callbacks
                )
            }
        }
    }
}
