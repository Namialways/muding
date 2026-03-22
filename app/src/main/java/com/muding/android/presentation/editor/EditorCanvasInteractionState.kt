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

@Stable
class EditorCanvasInteractionState {
    var currentPath by mutableStateOf<Path?>(null)
    val currentPenPoints = mutableStateListOf<Offset>()
    var pathVersion by mutableIntStateOf(0)
    var startPoint by mutableStateOf<Offset?>(null)
    var endPoint by mutableStateOf<Offset?>(null)
    var movingPathIndex by mutableStateOf<Int?>(null)
    var resizingState by mutableStateOf<ActiveResizeState?>(null)
    var moveGestureDownOffset by mutableStateOf<Offset?>(null)
    var eraserPreviewCenter by mutableStateOf<Offset?>(null)

    fun resetDragState() {
        currentPath = null
        currentPenPoints.clear()
        startPoint = null
        endPoint = null
        movingPathIndex = null
        resizingState = null
        moveGestureDownOffset = null
    }
}

@Composable
fun rememberEditorCanvasInteractionState(): EditorCanvasInteractionState {
    return remember { EditorCanvasInteractionState() }
}
