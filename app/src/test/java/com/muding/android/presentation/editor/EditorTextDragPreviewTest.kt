package com.muding.android.presentation.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.muding.android.domain.model.DrawingPath
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTextDragPreviewTest {

    @Test
    fun `text drag updates preview path without mutating model immediately`() {
        val original = textPathAt(10f, 20f)
        val callbacks = CallbackRecorder()
        val interactionState = textDragState(original)

        handleTextDrag(
            interactionState = interactionState,
            paths = listOf(original),
            dragAmount = Offset(8f, 6f)
        )

        assertEquals(original.copy(position = Offset(18f, 26f)), interactionState.activePreviewPath)
        assertTrue(callbacks.updatedPaths.isEmpty())
    }

    @Test
    fun `text drag end commits preview path and keeps it selected`() {
        val original = textPathAt(10f, 20f)
        val preview = original.copy(position = Offset(18f, 26f))
        val callbacks = CallbackRecorder()
        val interactionState = textDragState(original).apply {
            activePreviewPath = preview
        }

        handleTextDragEnd(
            interactionState = interactionState,
            paths = listOf(original),
            callbacks = callbacks.callbacks
        )

        assertEquals(listOf(0 to preview), callbacks.updatedPaths)
        assertEquals(0, callbacks.selectedIndex)
        assertEquals(preview, callbacks.selectedPath)
    }

    private fun textDragState(path: DrawingPath.TextPath): EditorCanvasInteractionState {
        return EditorCanvasInteractionState(CoroutineScope(EmptyCoroutineContext)).apply {
            movingPathIndex = 0
            originalDragPath = path
            activePreviewPath = path
        }
    }

    private fun textPathAt(x: Float, y: Float): DrawingPath.TextPath {
        return DrawingPath.TextPath(
            position = Offset(x, y),
            text = "Hello",
            color = Color.Black,
            fontSize = 24f
        )
    }

    private class CallbackRecorder {
        val updatedPaths = mutableListOf<Pair<Int, DrawingPath>>()
        var selectedIndex: Int? = null
        var selectedPath: DrawingPath? = null

        val callbacks = EditorCanvasCallbacks(
            onPathAdded = {},
            onPathUpdated = { index, path -> updatedPaths += index to path },
            onPathSelectionChanged = { index, path ->
                selectedIndex = index
                selectedPath = path
            }
        )
    }
}
