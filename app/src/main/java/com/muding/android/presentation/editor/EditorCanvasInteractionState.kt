package com.muding.android.presentation.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.rememberCoroutineScope
import com.muding.android.domain.model.DrawingPath
import kotlinx.coroutines.launch

@Stable
class EditorCanvasInteractionState(
    private val coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    var currentPath by mutableStateOf<Path?>(null)
    val currentPenPoints = mutableStateListOf<Offset>()
    var pathVersion by mutableIntStateOf(0)
    var startPoint by mutableStateOf<Offset?>(null)
    var endPoint by mutableStateOf<Offset?>(null)
    var movingPathIndex by mutableStateOf<Int?>(null)
    var resizingState by mutableStateOf<ActiveResizeState?>(null)
    var moveGestureDownOffset by mutableStateOf<Offset?>(null)
    var eraserPreviewCenter by mutableStateOf<Offset?>(null)
    var activePreviewPath by mutableStateOf<DrawingPath?>(null)
    var originalDragPath by mutableStateOf<DrawingPath?>(null)
    private var clearPreviewJob: kotlinx.coroutines.Job? = null

    fun resetDragState() {
        clearPreviewJob?.cancel()
        currentPath = null
        currentPenPoints.clear()
        startPoint = null
        endPoint = null
        movingPathIndex = null
        resizingState = null
        moveGestureDownOffset = null
        activePreviewPath = null
        originalDragPath = null
    }

    fun deferResetDragState() {
        currentPath = null
        currentPenPoints.clear()
        startPoint = null
        endPoint = null
        moveGestureDownOffset = null
        
        clearPreviewJob?.cancel()
        clearPreviewJob = coroutineScope.launch {
            kotlinx.coroutines.delay(50)
            movingPathIndex = null
            resizingState = null
            activePreviewPath = null
            originalDragPath = null
        }
    }
}

@Composable
fun rememberEditorCanvasInteractionState(): EditorCanvasInteractionState {
    val coroutineScope = rememberCoroutineScope()
    return remember { EditorCanvasInteractionState(coroutineScope) }
}
