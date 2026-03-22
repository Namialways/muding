package com.muding.android.presentation.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool
import kotlin.math.max
import kotlin.math.min

data class EditorCanvasCallbacks(
    val onPathAdded: (DrawingPath) -> Unit,
    val onPathUpdated: (Int, DrawingPath) -> Unit,
    val onPathSelectionChanged: (Int?, DrawingPath?) -> Unit
)

fun handleMoveDragStart(
    interactionState: EditorCanvasInteractionState,
    pathHitTester: EditorPathHitTester,
    paths: List<DrawingPath>,
    selectedPathIndex: Int?,
    selectionHitRadius: Float,
    callbacks: EditorCanvasCallbacks,
    fallbackOffset: Offset
) {
    val dragStartOffset = interactionState.moveGestureDownOffset ?: fallbackOffset
    val selectedPath = selectedPathIndex?.let(paths::getOrNull)
    val activeHandle = selectedPath?.let { resizeHandleHit(it, dragStartOffset) }
    if (selectedPathIndex != null && activeHandle != null) {
        interactionState.resizingState = ActiveResizeState(selectedPathIndex, activeHandle)
        callbacks.onPathSelectionChanged(selectedPathIndex, selectedPath)
        return
    }

    if (
        selectedPathIndex != null &&
        selectedPath != null &&
        selectedPath !is DrawingPath.PenPath &&
        pathHitTester.isInteractivePathHit(selectedPath, dragStartOffset, selectionHitRadius)
    ) {
        interactionState.movingPathIndex = selectedPathIndex
        callbacks.onPathSelectionChanged(selectedPathIndex, selectedPath)
        return
    }

    val hitIndex = pathHitTester.hitEditableIndexAt(paths, dragStartOffset, selectionHitRadius)
    callbacks.onPathSelectionChanged(hitIndex, hitIndex?.let(paths::getOrNull))
}

fun handleMoveDrag(
    interactionState: EditorCanvasInteractionState,
    paths: List<DrawingPath>,
    callbacks: EditorCanvasCallbacks,
    dragAmount: Offset,
    touch: Offset
) {
    val activeResize = interactionState.resizingState
    when {
        activeResize != null -> {
            val path = paths.getOrNull(activeResize.index) ?: return
            callbacks.onPathUpdated(
                activeResize.index,
                resizePath(path = path, handle = activeResize.handle, touch = touch)
            )
        }

        interactionState.movingPathIndex != null -> {
            val movingIndex = interactionState.movingPathIndex ?: return
            val path = paths.getOrNull(movingIndex) ?: return
            callbacks.onPathUpdated(movingIndex, movePath(path, dragAmount))
        }
    }
}

fun handleMoveDragEnd(
    interactionState: EditorCanvasInteractionState,
    paths: List<DrawingPath>,
    callbacks: EditorCanvasCallbacks
) {
    val activeResize = interactionState.resizingState
    val movingIndex = interactionState.movingPathIndex
    when {
        activeResize != null -> {
            callbacks.onPathSelectionChanged(activeResize.index, paths.getOrNull(activeResize.index))
        }

        movingIndex != null -> {
            callbacks.onPathSelectionChanged(movingIndex, paths.getOrNull(movingIndex))
        }
    }
    interactionState.resetDragState()
}

fun handlePenDragStart(
    interactionState: EditorCanvasInteractionState,
    callbacks: EditorCanvasCallbacks,
    offset: Offset
) {
    callbacks.onPathSelectionChanged(null, null)
    interactionState.currentPath = Path().apply { moveTo(offset.x, offset.y) }
    interactionState.currentPenPoints.clear()
    interactionState.currentPenPoints.add(offset)
}

fun handlePenDrag(
    interactionState: EditorCanvasInteractionState,
    position: Offset
) {
    interactionState.currentPath?.lineTo(position.x, position.y)
    interactionState.currentPenPoints.add(position)
    interactionState.pathVersion++
}

fun handlePenDragEnd(
    interactionState: EditorCanvasInteractionState,
    currentColor: Color,
    strokeWidth: Float,
    callbacks: EditorCanvasCallbacks
) {
    interactionState.currentPath?.let { path ->
        callbacks.onPathAdded(
            DrawingPath.PenPath(
                path = Path().apply { addPath(path) },
                color = currentColor,
                strokeWidth = strokeWidth,
                points = interactionState.currentPenPoints.toList()
            )
        )
    }
    interactionState.resetDragState()
}

fun handleShapeDragStart(
    interactionState: EditorCanvasInteractionState,
    callbacks: EditorCanvasCallbacks,
    offset: Offset
) {
    callbacks.onPathSelectionChanged(null, null)
    interactionState.startPoint = offset
    interactionState.endPoint = offset
}

fun handleShapeDrag(
    interactionState: EditorCanvasInteractionState,
    position: Offset
) {
    interactionState.endPoint = position
}

fun handleShapeDragEnd(
    interactionState: EditorCanvasInteractionState,
    currentTool: DrawingTool?,
    currentColor: Color,
    strokeWidth: Float,
    shapeFilled: Boolean,
    creationThreshold: Float,
    callbacks: EditorCanvasCallbacks
) {
    val start = interactionState.startPoint
    val end = interactionState.endPoint
    if (start != null && end != null && (end - start).getDistance() >= creationThreshold) {
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
        newPath?.let(callbacks.onPathAdded)
    }
    interactionState.resetDragState()
}

fun handleTextDragStart(
    interactionState: EditorCanvasInteractionState,
    pathHitTester: EditorPathHitTester,
    paths: List<DrawingPath>,
    callbacks: EditorCanvasCallbacks,
    selectionHitRadius: Float,
    offset: Offset
) {
    val hitIndex = pathHitTester.hitIndexAt(
        paths = paths,
        offset = offset,
        radius = selectionHitRadius,
        tool = DrawingTool.TEXT
    )
    if (hitIndex != null) {
        interactionState.movingPathIndex = hitIndex
        callbacks.onPathSelectionChanged(hitIndex, paths.getOrNull(hitIndex))
    }
}

fun handleTextDrag(
    interactionState: EditorCanvasInteractionState,
    paths: List<DrawingPath>,
    callbacks: EditorCanvasCallbacks,
    dragAmount: Offset
) {
    val movingIndex = interactionState.movingPathIndex ?: return
    val path = paths.getOrNull(movingIndex) ?: return
    callbacks.onPathUpdated(movingIndex, movePath(path, dragAmount))
}

fun handleTextDragEnd(
    interactionState: EditorCanvasInteractionState,
    paths: List<DrawingPath>,
    callbacks: EditorCanvasCallbacks
) {
    val movingIndex = interactionState.movingPathIndex
    if (movingIndex != null) {
        callbacks.onPathSelectionChanged(movingIndex, paths.getOrNull(movingIndex))
    }
    interactionState.resetDragState()
}

fun handleMoveTap(
    pathHitTester: EditorPathHitTester,
    paths: List<DrawingPath>,
    selectionHitRadius: Float,
    callbacks: EditorCanvasCallbacks,
    offset: Offset
) {
    val hitIndex = pathHitTester.hitEditableIndexAt(paths, offset, selectionHitRadius)
    callbacks.onPathSelectionChanged(hitIndex, hitIndex?.let(paths::getOrNull))
}

fun handleMoveTextEditRequest(
    pathHitTester: EditorPathHitTester,
    paths: List<DrawingPath>,
    selectionHitRadius: Float,
    callbacks: EditorCanvasCallbacks,
    textEditState: EditorTextEditState,
    offset: Offset
) {
    val hitIndex = pathHitTester.hitEditableIndexAt(paths, offset, selectionHitRadius)
    val path = hitIndex?.let { paths.getOrNull(it) as? DrawingPath.TextPath }
    if (path != null) {
        callbacks.onPathSelectionChanged(hitIndex, path)
        textEditState.open(hitIndex, path.position, path.text)
    }
}

fun handleTextToolTap(
    pathHitTester: EditorPathHitTester,
    paths: List<DrawingPath>,
    selectionHitRadius: Float,
    selectedPathIndex: Int?,
    callbacks: EditorCanvasCallbacks,
    textEditState: EditorTextEditState,
    offset: Offset
) {
    val hitIndex = pathHitTester.hitIndexAt(
        paths = paths,
        offset = offset,
        radius = selectionHitRadius,
        tool = DrawingTool.TEXT
    )
    val path = hitIndex?.let { paths.getOrNull(it) as? DrawingPath.TextPath }
    if (path != null) {
        callbacks.onPathSelectionChanged(hitIndex, path)
        textEditState.open(hitIndex, path.position, path.text)
    } else {
        val hadSelection = selectedPathIndex != null
        callbacks.onPathSelectionChanged(null, null)
        if (!hadSelection) {
            textEditState.open(null, offset, "")
        }
    }
}

fun handleSelectionTransform(
    currentTool: DrawingTool?,
    selectedPathIndex: Int?,
    paths: List<DrawingPath>,
    pan: Offset,
    zoom: Float,
    rotation: Float,
    callbacks: EditorCanvasCallbacks
) {
    if (currentTool != DrawingTool.MOVE) return
    val index = selectedPathIndex ?: return
    when (val path = paths.getOrNull(index)) {
        is DrawingPath.TextPath -> {
            if (pan == Offset.Zero && zoom == 1f && rotation == 0f) return
            callbacks.onPathUpdated(
                index,
                path.copy(
                    position = path.position + pan,
                    scale = (path.scale * zoom).coerceIn(0.5f, 6f),
                    rotation = normalizeRotation(path.rotation + rotation)
                )
            )
        }

        is DrawingPath.RectanglePath -> {
            if (pan == Offset.Zero && zoom == 1f && rotation == 0f) return
            callbacks.onPathUpdated(index, transformRectangle(path, pan, zoom, rotation))
        }

        is DrawingPath.CirclePath -> {
            if (pan == Offset.Zero && zoom == 1f) return
            callbacks.onPathUpdated(
                index,
                path.copy(
                    center = path.center + pan,
                    radius = (path.radius * zoom).coerceAtLeast(MIN_CIRCLE_RADIUS)
                )
            )
        }

        else -> Unit
    }
}
