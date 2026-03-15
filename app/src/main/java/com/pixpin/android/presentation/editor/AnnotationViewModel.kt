package com.pixpin.android.presentation.editor

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.pixpin.android.domain.model.DrawingPath
import com.pixpin.android.domain.model.DrawingTool

enum class EraserMode {
    OBJECT,
    PARTIAL
}

class AnnotationViewModel : ViewModel() {

    val currentTool = mutableStateOf(DrawingTool.PEN)
    val currentColor = mutableStateOf(Color.Red)
    val strokeWidth = mutableStateOf(5f)
    val eraserSize = mutableStateOf(28f)
    val eraserMode = mutableStateOf(EraserMode.OBJECT)
    val textSize = mutableStateOf(28f)
    val textOutlineEnabled = mutableStateOf(false)
    val selectedTextIndex = mutableStateOf<Int?>(null)

    val paths = mutableStateListOf<DrawingPath>()
    private val undoPaths = mutableStateListOf<DrawingPath>()

    val availableColors = listOf(
        Color.Red,
        Color(0xFFFF6B00),
        Color(0xFFFFD700),
        Color(0xFF00FF00),
        Color(0xFF00BFFF),
        Color.Blue,
        Color(0xFF9370DB),
        Color.Black,
        Color.White
    )

    fun addPath(path: DrawingPath): Int {
        paths.add(path)
        undoPaths.clear()
        return paths.lastIndex
    }

    fun replacePaths(newPaths: List<DrawingPath>) {
        paths.clear()
        paths.addAll(newPaths)
        undoPaths.clear()
        selectedTextIndex.value = null
    }

    fun updatePath(index: Int, path: DrawingPath) {
        if (index !in paths.indices) return
        paths[index] = path
        undoPaths.clear()
    }

    fun replacePath(index: Int, replacements: List<DrawingPath>) {
        if (index !in paths.indices) return
        paths.removeAt(index)
        paths.addAll(index, replacements)
        undoPaths.clear()
    }

    fun removePath(index: Int) {
        if (index !in paths.indices) return
        paths.removeAt(index)
        undoPaths.clear()
        if (selectedTextIndex.value == index) {
            selectedTextIndex.value = null
        } else if ((selectedTextIndex.value ?: -1) > index) {
            selectedTextIndex.value = selectedTextIndex.value?.minus(1)
        }
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            val lastPath = paths.removeLast()
            undoPaths.add(lastPath)
            if (selectedTextIndex.value == paths.size) {
                selectedTextIndex.value = null
            }
        }
    }

    fun redo() {
        if (undoPaths.isNotEmpty()) {
            val path = undoPaths.removeLast()
            paths.add(path)
        }
    }

    fun canUndo(): Boolean = paths.isNotEmpty()
    fun canRedo(): Boolean = undoPaths.isNotEmpty()

    fun selectTool(tool: DrawingTool) {
        currentTool.value = tool
    }

    fun selectColor(color: Color) {
        currentColor.value = color
        applyCurrentStyleToSelectedText()
    }

    fun selectTextSize(size: Float) {
        textSize.value = size.coerceIn(14f, 72f)
        applyCurrentStyleToSelectedText()
    }

    fun selectEraserSize(size: Float) {
        eraserSize.value = size.coerceIn(12f, 96f)
    }

    fun selectEraserMode(mode: EraserMode) {
        eraserMode.value = mode
    }

    fun selectTextOutlineEnabled(enabled: Boolean) {
        textOutlineEnabled.value = enabled
        applyCurrentStyleToSelectedText()
    }

    fun selectTextPath(index: Int?, path: DrawingPath.TextPath?) {
        selectedTextIndex.value = index
        if (path != null) {
            currentColor.value = path.color
            textSize.value = path.fontSize
            textOutlineEnabled.value = path.outlineEnabled
        }
    }

    fun clearTextSelection() {
        selectedTextIndex.value = null
    }

    private fun applyCurrentStyleToSelectedText() {
        val index = selectedTextIndex.value ?: return
        val path = paths.getOrNull(index) as? DrawingPath.TextPath ?: return
        paths[index] = path.copy(
            color = currentColor.value,
            fontSize = textSize.value,
            outlineEnabled = textOutlineEnabled.value
        )
    }

    fun clear() {
        paths.clear()
        undoPaths.clear()
    }
}
