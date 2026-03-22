package com.muding.android.presentation.editor

import androidx.compose.runtime.Immutable
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool

@Immutable
data class EditorToolPanelState(
    val currentTool: DrawingTool?,
    val selectedPath: DrawingPath?,
    val showStrokeControls: Boolean,
    val showTextControls: Boolean,
    val showFillControls: Boolean,
    val showEraserControls: Boolean
)

fun AnnotationViewModel.buildToolPanelState(): EditorToolPanelState {
    val selectedPath = selectedPath()
    val currentTool = currentTool.value
    return EditorToolPanelState(
        currentTool = currentTool,
        selectedPath = selectedPath,
        showStrokeControls = currentTool in setOf(
            DrawingTool.PEN,
            DrawingTool.ARROW,
            DrawingTool.RECTANGLE,
            DrawingTool.CIRCLE
        ) || selectedPath is DrawingPath.PenPath ||
            selectedPath is DrawingPath.ArrowPath ||
            selectedPath is DrawingPath.RectanglePath ||
            selectedPath is DrawingPath.CirclePath,
        showTextControls = currentTool == DrawingTool.TEXT || selectedPath is DrawingPath.TextPath,
        showFillControls = currentTool == DrawingTool.RECTANGLE ||
            currentTool == DrawingTool.CIRCLE ||
            selectedPath is DrawingPath.RectanglePath ||
            selectedPath is DrawingPath.CirclePath,
        showEraserControls = currentTool == DrawingTool.ERASER
    )
}

