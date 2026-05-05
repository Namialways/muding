package com.muding.android.presentation.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.muding.android.domain.model.DrawingPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTextEditStateTest {

    @Test
    fun `commit creates text path for non-empty new draft`() {
        val result = buildEditorTextCommitResult(
            paths = emptyList(),
            targetIndex = null,
            position = Offset(12f, 24f),
            draft = "  hello  ",
            color = Color.Red,
            textSize = 30f,
            textOutlineEnabled = true
        )

        val add = result as EditorTextCommitResult.Add
        assertEquals(
            DrawingPath.TextPath(
                position = Offset(12f, 24f),
                text = "hello",
                color = Color.Red,
                fontSize = 30f,
                outlineEnabled = true
            ),
            add.path
        )
    }

    @Test
    fun `commit updates existing text path and keeps transform`() {
        val existing = DrawingPath.TextPath(
            position = Offset(4f, 8f),
            text = "old",
            color = Color.Blue,
            fontSize = 22f,
            scale = 1.5f,
            rotation = 12f,
            outlineEnabled = false
        )

        val result = buildEditorTextCommitResult(
            paths = listOf(existing),
            targetIndex = 0,
            position = Offset.Zero,
            draft = "new",
            color = Color.Green,
            textSize = 32f,
            textOutlineEnabled = true
        )

        val update = result as EditorTextCommitResult.Update
        assertEquals(0, update.index)
        assertEquals(
            existing.copy(
                text = "new",
                color = Color.Green,
                fontSize = 32f,
                outlineEnabled = true
            ),
            update.path
        )
    }

    @Test
    fun `commit removes existing text when draft is blank`() {
        val existing = DrawingPath.TextPath(
            position = Offset.Zero,
            text = "old",
            color = Color.Black,
            fontSize = 20f
        )

        val result = buildEditorTextCommitResult(
            paths = listOf(existing),
            targetIndex = 0,
            position = Offset.Zero,
            draft = "   ",
            color = Color.Black,
            textSize = 20f,
            textOutlineEnabled = false
        )

        assertEquals(EditorTextCommitResult.Remove(index = 0), result)
    }

    @Test
    fun `commit ignores blank new text`() {
        val result = buildEditorTextCommitResult(
            paths = emptyList(),
            targetIndex = null,
            position = Offset.Zero,
            draft = "\n",
            color = Color.Black,
            textSize = 20f,
            textOutlineEnabled = false
        )

        assertTrue(result is EditorTextCommitResult.None)
    }
}
