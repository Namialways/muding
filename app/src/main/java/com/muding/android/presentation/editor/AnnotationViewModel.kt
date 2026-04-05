package com.muding.android.presentation.editor

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool

enum class EraserMode {
    OBJECT,
    PARTIAL
}

class AnnotationViewModel(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val currentTool = mutableStateOf<DrawingTool?>(null)
    val currentColor = mutableStateOf(Color(0xFFFF453A))
    val strokeWidth = mutableStateOf(6f)
    val eraserSize = mutableStateOf(28f)
    val eraserMode = mutableStateOf(EraserMode.OBJECT)
    val textSize = mutableStateOf(28f)
    val textOutlineEnabled = mutableStateOf(false)
    val shapeFilled = mutableStateOf(false)
    val selectedPathIndex = mutableStateOf<Int?>(null)

    val paths = mutableStateListOf<DrawingPath>()
    private val undoPaths = mutableStateListOf<DrawingPath>()
    private var colorCollections = EditorColorCollections()
    private val favoriteColorSwatches = mutableStateListOf<Color>()
    private val recentColorSwatches = mutableStateListOf<Color>()
    private val quickAccessColorSwatches = mutableStateListOf<Color>()
    val favoriteColors: List<Color>
        get() = favoriteColorSwatches
    val recentColors: List<Color>
        get() = recentColorSwatches
    val quickAccessColors: List<Color>
        get() = quickAccessColorSwatches

    init {
        colorCollections = EditorColorCollections(
            favorites = settingsRepository.getFavoriteEditorColors(),
            recents = settingsRepository.getRecentEditorColors()
        )
        syncColorSwatches()
        recentColors.firstOrNull()?.let { currentColor.value = it }
    }

    val selectedTextIndex: Int?
        get() = selectedPathIndex.value?.takeIf { paths.getOrNull(it) is DrawingPath.TextPath }

    fun selectedPath(): DrawingPath? = selectedPathIndex.value?.let(paths::getOrNull)

    fun addPath(path: DrawingPath): Int {
        paths.add(path)
        undoPaths.clear()
        return paths.lastIndex
    }

    fun replacePaths(newPaths: List<DrawingPath>) {
        paths.clear()
        paths.addAll(newPaths)
        undoPaths.clear()
        selectedPathIndex.value = null
    }

    fun updatePath(index: Int, path: DrawingPath) {
        if (index !in paths.indices) return
        paths[index] = path
        undoPaths.clear()
        if (selectedPathIndex.value == index) {
            syncControlsFromPath(path)
        }
    }

    fun replacePath(index: Int, replacements: List<DrawingPath>) {
        if (index !in paths.indices) return
        paths.removeAt(index)
        paths.addAll(index, replacements)
        undoPaths.clear()
        selectedPathIndex.value = null
    }

    fun removePath(index: Int) {
        if (index !in paths.indices) return
        paths.removeAt(index)
        undoPaths.clear()
        val selected = selectedPathIndex.value
        selectedPathIndex.value = when {
            selected == null -> null
            selected == index -> null
            selected > index -> selected - 1
            else -> selected
        }
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            undoPaths.add(paths.removeLast())
            selectedPathIndex.value = null
        }
    }

    fun redo() {
        if (undoPaths.isNotEmpty()) {
            paths.add(undoPaths.removeLast())
            selectedPathIndex.value = null
        }
    }

    fun canUndo(): Boolean = paths.isNotEmpty()
    fun canRedo(): Boolean = undoPaths.isNotEmpty()

    fun selectTool(tool: DrawingTool) {
        currentTool.value = if (currentTool.value == tool) null else tool
        if (currentTool.value == null) {
            selectedPathIndex.value = null
            return
        }
        val selected = selectedPath()
        if (selected != null &&
            currentTool.value != DrawingTool.MOVE &&
            toolFor(selected) != currentTool.value
        ) {
            selectedPathIndex.value = null
        }
    }

    fun selectColor(color: Color) {
        applyConfirmedColor(color)
    }

    fun applyConfirmedColor(color: Color) {
        val normalizedColor = Color(color.toArgb())
        currentColor.value = normalizedColor
        colorCollections = colorCollections.recordRecent(normalizedColor.toArgb())
        settingsRepository.setRecentEditorColors(colorCollections.recents)
        syncColorSwatches()
        applyCurrentStyleToSelectedPath()
    }

    fun toggleFavoriteColor(color: Color) {
        colorCollections = colorCollections.toggleFavorite(color.toArgb())
        settingsRepository.setFavoriteEditorColors(colorCollections.favorites)
        syncColorSwatches()
    }

    fun isFavoriteColor(color: Color): Boolean {
        val argb = color.toArgb()
        return favoriteColorSwatches.any { it.toArgb() == argb }
    }

    fun selectStrokeWidth(width: Float) {
        strokeWidth.value = width.coerceIn(2f, 32f)
        applyCurrentStyleToSelectedPath()
    }

    fun selectTextSize(size: Float) {
        textSize.value = size.coerceIn(14f, 72f)
        applyCurrentStyleToSelectedPath()
    }

    fun selectEraserSize(size: Float) {
        eraserSize.value = size.coerceIn(12f, 96f)
    }

    fun selectEraserMode(mode: EraserMode) {
        eraserMode.value = mode
    }

    fun selectTextOutlineEnabled(enabled: Boolean) {
        textOutlineEnabled.value = enabled
        applyCurrentStyleToSelectedPath()
    }

    fun selectShapeFilled(enabled: Boolean) {
        shapeFilled.value = enabled
        applyCurrentStyleToSelectedPath()
    }

    fun selectPath(index: Int?, path: DrawingPath?) {
        selectedPathIndex.value = index
        if (path != null) {
            syncControlsFromPath(path)
        }
    }

    fun clearSelection() {
        selectedPathIndex.value = null
    }

    fun deleteSelectedPath() {
        selectedPathIndex.value?.let(::removePath)
    }

    private fun syncControlsFromPath(path: DrawingPath) {
        when (path) {
            is DrawingPath.PenPath -> {
                currentColor.value = path.color
                strokeWidth.value = path.strokeWidth.coerceIn(2f, 32f)
            }

            is DrawingPath.ArrowPath -> {
                currentColor.value = path.color
                strokeWidth.value = path.strokeWidth.coerceIn(2f, 32f)
            }

            is DrawingPath.RectanglePath -> {
                currentColor.value = path.color
                strokeWidth.value = path.strokeWidth.coerceIn(2f, 32f)
                shapeFilled.value = path.filled
            }

            is DrawingPath.CirclePath -> {
                currentColor.value = path.color
                strokeWidth.value = path.strokeWidth.coerceIn(2f, 32f)
                shapeFilled.value = path.filled
            }

            is DrawingPath.TextPath -> {
                currentColor.value = path.color
                textSize.value = path.fontSize.coerceIn(14f, 72f)
                textOutlineEnabled.value = path.outlineEnabled
            }
        }
    }

    private fun applyCurrentStyleToSelectedPath() {
        val index = selectedPathIndex.value ?: return
        val path = paths.getOrNull(index) ?: return
        paths[index] = when (path) {
            is DrawingPath.PenPath -> path.copy(
                color = currentColor.value,
                strokeWidth = strokeWidth.value
            )

            is DrawingPath.ArrowPath -> path.copy(
                color = currentColor.value,
                strokeWidth = strokeWidth.value
            )

            is DrawingPath.RectanglePath -> path.copy(
                color = currentColor.value,
                strokeWidth = strokeWidth.value,
                filled = shapeFilled.value
            )

            is DrawingPath.CirclePath -> path.copy(
                color = currentColor.value,
                strokeWidth = strokeWidth.value,
                filled = shapeFilled.value
            )

            is DrawingPath.TextPath -> path.copy(
                color = currentColor.value,
                fontSize = textSize.value,
                outlineEnabled = textOutlineEnabled.value
            )
        }
    }

    private fun toolFor(path: DrawingPath): DrawingTool {
        return when (path) {
            is DrawingPath.PenPath -> DrawingTool.PEN
            is DrawingPath.ArrowPath -> DrawingTool.ARROW
            is DrawingPath.RectanglePath -> DrawingTool.RECTANGLE
            is DrawingPath.CirclePath -> DrawingTool.CIRCLE
            is DrawingPath.TextPath -> DrawingTool.TEXT
        }
    }

    fun clear() {
        paths.clear()
        undoPaths.clear()
        selectedPathIndex.value = null
    }

    private fun syncColorSwatches() {
        replaceColorSwatches(favoriteColorSwatches, colorCollections.favorites)
        replaceColorSwatches(recentColorSwatches, colorCollections.recents)
        replaceColorSwatches(quickAccessColorSwatches, colorCollections.quickAccessColors())
    }

    private fun replaceColorSwatches(target: MutableList<Color>, colors: List<Int>) {
        target.clear()
        target.addAll(colors.map(::Color))
    }

    companion object {
        fun factory(settingsRepository: AppSettingsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AnnotationViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return AnnotationViewModel(settingsRepository) as T
                }
            }
        }
    }
}
