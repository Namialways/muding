package com.muding.android.presentation.editor

import android.view.MotionEvent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
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


private sealed interface MoveGestureOutcome {
    data class Drag(val change: PointerInputChange, val overSlop: Offset) : MoveGestureOutcome
    data object Tap : MoveGestureOutcome
    data object LongPress : MoveGestureOutcome
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitMoveGestureOutcome(
    pointerId: PointerId,
    startPosition: Offset,
    touchSlopPx: Float,
    longPressTimeoutMs: Long
): MoveGestureOutcome {
    val deadlineNanos = System.nanoTime() + longPressTimeoutMs * 1_000_000L
    while (true) {
        val remainingMs = (deadlineNanos - System.nanoTime()) / 1_000_000L
        if (remainingMs <= 0) return MoveGestureOutcome.LongPress

        val event = withTimeoutOrNull(remainingMs) {
            awaitPointerEvent()
        } ?: return MoveGestureOutcome.LongPress

        val change = event.changes.firstOrNull { it.id == pointerId }
            ?: return MoveGestureOutcome.Tap
        if (!change.pressed) return MoveGestureOutcome.Tap

        val offset = change.position - startPosition
        if (offset.getDistance() > touchSlopPx) {
            return MoveGestureOutcome.Drag(change, offset)
        }
    }
}

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
    return this.pointerInput(Unit) {
        var lastTapTimeMs = 0L
        var lastTapPosition = Offset.Zero

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val downPos = down.position
            val downTimeMs = down.uptimeMillis
            val canvasDown = toCanvasOffset(downPos)
            interactionState.moveGestureDownOffset = canvasDown

            val outcome = awaitMoveGestureOutcome(
                pointerId = down.id,
                startPosition = downPos,
                touchSlopPx = viewConfiguration.touchSlop,
                longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
            )

            when (outcome) {
                is MoveGestureOutcome.Drag -> {
                    // 拖动识别成功，立即启动移动
                    handleMoveDragStart(
                        interactionState = interactionState,
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectedPathIndex = latestSelectedPathIndex.value,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        fallbackOffset = canvasDown
                    )
                    // 应用超出 touch slop 的初始偏移量
                    handleMoveDrag(
                        interactionState = interactionState,
                        paths = latestPaths.value,
                        callbacks = callbacks,
                        dragAmount = toCanvasDelta(outcome.overSlop),
                        touch = toCanvasOffset(outcome.change.position)
                    )
                    outcome.change.consume()

                    // 持续跟踪拖动
                    drag(outcome.change.id) { change ->
                        val pointerCount = currentEvent.changes.count { it.pressed }
                        if (pointerCount >= 2) {
                            val pan = currentEvent.calculatePan()
                            val zoom = currentEvent.calculateZoom()
                            val rotation = currentEvent.calculateRotation()
                            handleSelectionTransform(
                                interactionState = interactionState,
                                currentTool = DrawingTool.MOVE,
                                selectedPathIndex = latestSelectedPathIndex.value,
                                paths = latestPaths.value,
                                pan = toCanvasDelta(pan),
                                zoom = zoom,
                                rotation = rotation,
                                callbacks = callbacks
                            )
                        } else {
                            handleMoveDrag(
                                interactionState = interactionState,
                                paths = latestPaths.value,
                                callbacks = callbacks,
                                dragAmount = toCanvasDelta(change.positionChange()),
                                touch = toCanvasOffset(change.position)
                            )
                        }
                        change.consume()
                    }
                    handleMoveDragEnd(
                        interactionState = interactionState,
                        paths = latestPaths.value,
                        callbacks = callbacks
                    )
                    lastTapTimeMs = 0L
                }

                is MoveGestureOutcome.LongPress -> {
                    handleMoveTextEditRequest(
                        pathHitTester = pathHitTester,
                        paths = latestPaths.value,
                        selectionHitRadius = selectionHitRadius,
                        callbacks = callbacks,
                        textEditState = textEditState,
                        offset = canvasDown
                    )
                    // 消费后续事件直到手指抬起
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    interactionState.resetDragState()
                    lastTapTimeMs = 0L
                }

                is MoveGestureOutcome.Tap -> {
                    val isDoubleTap =
                        (downTimeMs - lastTapTimeMs) < viewConfiguration.doubleTapTimeoutMillis &&
                            (downPos - lastTapPosition).getDistance() < viewConfiguration.touchSlop * 2

                    if (isDoubleTap) {
                        handleMoveTextEditRequest(
                            pathHitTester = pathHitTester,
                            paths = latestPaths.value,
                            selectionHitRadius = selectionHitRadius,
                            callbacks = callbacks,
                            textEditState = textEditState,
                            offset = canvasDown
                        )
                        lastTapTimeMs = 0L
                    } else {
                        handleMoveTap(
                            pathHitTester = pathHitTester,
                            paths = latestPaths.value,
                            selectionHitRadius = selectionHitRadius,
                            callbacks = callbacks,
                            offset = canvasDown
                        )
                        lastTapTimeMs = downTimeMs
                        lastTapPosition = downPos
                    }
                    interactionState.resetDragState()
                }
            }
            interactionState.moveGestureDownOffset = null
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


