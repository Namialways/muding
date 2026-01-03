package com.pixpin.android.presentation.editor

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.pixpin.android.domain.model.DrawingPath
import com.pixpin.android.domain.model.DrawingTool

/**
 * 标注编辑器 ViewModel
 */
class AnnotationViewModel : ViewModel() {

    // 当前选择的工具
    val currentTool = mutableStateOf(DrawingTool.PEN)

    // 当前颜色
    val currentColor = mutableStateOf(Color.Red)

    // 当前笔触宽度
    val strokeWidth = mutableStateOf(5f)

    // 绘制路径列表
    val paths = mutableStateListOf<DrawingPath>()

    // 撤销栈
    private val undoPaths = mutableStateListOf<DrawingPath>()

    // 可用颜色
    val availableColors = listOf(
        Color.Red,
        Color(0xFFFF6B00),  // Orange
        Color(0xFFFFD700),  // Gold
        Color(0xFF00FF00),  // Green
        Color(0xFF00BFFF),  // Deep Sky Blue
        Color.Blue,
        Color(0xFF9370DB),  // Medium Purple
        Color.Black,
        Color.White
    )

    /**
     * 添加路径
     */
    fun addPath(path: DrawingPath) {
        paths.add(path)
        undoPaths.clear() // 添加新路径后清空撤销栈
    }

    /**
     * 撤销
     */
    fun undo() {
        if (paths.isNotEmpty()) {
            val lastPath = paths.removeLast()
            undoPaths.add(lastPath)
        }
    }

    /**
     * 重做
     */
    fun redo() {
        if (undoPaths.isNotEmpty()) {
            val path = undoPaths.removeLast()
            paths.add(path)
        }
    }

    /**
     * 是否可以撤销
     */
    fun canUndo(): Boolean = paths.isNotEmpty()

    /**
     * 是否可以重做
     */
    fun canRedo(): Boolean = undoPaths.isNotEmpty()

    /**
     * 切换工具
     */
    fun selectTool(tool: DrawingTool) {
        currentTool.value = tool
    }

    /**
     * 选择颜色
     */
    fun selectColor(color: Color) {
        currentColor.value = color
    }

    /**
     * 清空所有路径
     */
    fun clear() {
        paths.clear()
        undoPaths.clear()
    }
}
