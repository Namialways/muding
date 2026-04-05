package com.muding.android.presentation.editor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool

@Immutable
data class EditorDocumentState(
    val paths: List<DrawingPath>,
    val currentTool: DrawingTool?,
    val currentColor: Color,
    val favoriteColors: List<Color>,
    val recentColors: List<Color>,
    val quickAccessColors: List<Color>,
    val strokeWidth: Float,
    val eraserSize: Float,
    val eraserMode: EraserMode,
    val textSize: Float,
    val textOutlineEnabled: Boolean,
    val shapeFilled: Boolean,
    val selectedPathIndex: Int?,
    val canUndo: Boolean,
    val canRedo: Boolean
)

data class EditorScreenActions(
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onDeleteSelection: () -> Unit,
    val onSelectTool: (DrawingTool) -> Unit,
    val onSelectColor: (Color) -> Unit,
    val onApplyColor: (Color) -> Unit,
    val onToggleFavoriteColor: (Color) -> Unit,
    val onSelectStrokeWidth: (Float) -> Unit,
    val onSelectTextSize: (Float) -> Unit,
    val onSelectTextOutlineEnabled: (Boolean) -> Unit,
    val onSelectShapeFilled: (Boolean) -> Unit,
    val onSelectEraserSize: (Float) -> Unit,
    val onSelectEraserMode: (EraserMode) -> Unit,
    val onAddPath: (DrawingPath) -> Unit,
    val onUpdatePath: (Int, DrawingPath) -> Unit,
    val onReplacePath: (Int, List<DrawingPath>) -> Unit,
    val onRemovePath: (Int) -> Unit,
    val onSelectPath: (Int?, DrawingPath?) -> Unit
)

fun AnnotationViewModel.buildDocumentState(): EditorDocumentState {
    return EditorDocumentState(
        paths = paths.toList(),
        currentTool = currentTool.value,
        currentColor = currentColor.value,
        favoriteColors = favoriteColors.toList(),
        recentColors = recentColors.toList(),
        quickAccessColors = quickAccessColors.toList(),
        strokeWidth = strokeWidth.value,
        eraserSize = eraserSize.value,
        eraserMode = eraserMode.value,
        textSize = textSize.value,
        textOutlineEnabled = textOutlineEnabled.value,
        shapeFilled = shapeFilled.value,
        selectedPathIndex = selectedPathIndex.value,
        canUndo = canUndo(),
        canRedo = canRedo()
    )
}

fun AnnotationViewModel.buildScreenActions(): EditorScreenActions {
    return EditorScreenActions(
        onUndo = ::undo,
        onRedo = ::redo,
        onDeleteSelection = ::deleteSelectedPath,
        onSelectTool = ::selectTool,
        onSelectColor = ::selectColor,
        onApplyColor = ::applyConfirmedColor,
        onToggleFavoriteColor = ::toggleFavoriteColor,
        onSelectStrokeWidth = ::selectStrokeWidth,
        onSelectTextSize = ::selectTextSize,
        onSelectTextOutlineEnabled = ::selectTextOutlineEnabled,
        onSelectShapeFilled = ::selectShapeFilled,
        onSelectEraserSize = ::selectEraserSize,
        onSelectEraserMode = ::selectEraserMode,
        onAddPath = { path ->
            val index = addPath(path)
            if (path is DrawingPath.TextPath) {
                selectPath(index, path)
            } else {
                clearSelection()
            }
        },
        onUpdatePath = ::updatePath,
        onReplacePath = ::replacePath,
        onRemovePath = ::removePath,
        onSelectPath = ::selectPath
    )
}
