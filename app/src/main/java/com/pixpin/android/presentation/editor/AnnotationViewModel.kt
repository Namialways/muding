package com.pixpin.android.presentation.editor

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.pixpin.android.domain.model.DrawingPath
import com.pixpin.android.domain.model.DrawingTool

class AnnotationViewModel : ViewModel() {

    val currentTool = mutableStateOf(DrawingTool.PEN)
    val currentColor = mutableStateOf(Color.Red)
    val strokeWidth = mutableStateOf(5f)
    val textSize = mutableStateOf(28f)

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

    fun addPath(path: DrawingPath) {
        paths.add(path)
        undoPaths.clear()
    }

    fun replacePaths(newPaths: List<DrawingPath>) {
        paths.clear()
        paths.addAll(newPaths)
        undoPaths.clear()
    }

    fun updatePath(index: Int, path: DrawingPath) {
        if (index !in paths.indices) return
        paths[index] = path
        undoPaths.clear()
    }

    fun removePath(index: Int) {
        if (index !in paths.indices) return
        paths.removeAt(index)
        undoPaths.clear()
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            val lastPath = paths.removeLast()
            undoPaths.add(lastPath)
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
    }

    fun selectTextSize(size: Float) {
        textSize.value = size.coerceIn(14f, 72f)
    }

    fun clear() {
        paths.clear()
        undoPaths.clear()
    }
}
