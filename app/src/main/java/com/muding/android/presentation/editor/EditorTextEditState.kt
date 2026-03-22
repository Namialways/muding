package com.muding.android.presentation.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.muding.android.R
import com.muding.android.domain.model.DrawingPath

@Stable
class EditorTextEditState {
    var isVisible by mutableStateOf(false)
    var draft by mutableStateOf("")
    var targetIndex by mutableStateOf<Int?>(null)
    var targetPosition by mutableStateOf(Offset.Zero)

    fun open(index: Int?, position: Offset, initialText: String) {
        targetIndex = index
        targetPosition = position
        draft = initialText
        isVisible = true
    }

    fun dismiss() {
        targetIndex = null
        isVisible = false
    }
}

@Composable
fun rememberEditorTextEditState(): EditorTextEditState {
    return remember { EditorTextEditState() }
}

@Composable
fun EditorTextEditDialog(
    state: EditorTextEditState,
    paths: List<DrawingPath>,
    selectedPathIndex: Int?,
    currentColor: androidx.compose.ui.graphics.Color,
    textSize: Float,
    textOutlineEnabled: Boolean,
    onPathAdded: (DrawingPath) -> Unit,
    onPathUpdated: (Int, DrawingPath) -> Unit,
    onPathRemoved: (Int) -> Unit,
    onPathSelectionChanged: (Int?, DrawingPath?) -> Unit
) {
    if (!state.isVisible) return

    AlertDialog(
        onDismissRequest = { state.dismiss() },
        title = {
            Text(
                if (state.targetIndex == null) {
                    stringResource(R.string.editor_add_text)
                } else {
                    stringResource(R.string.editor_edit_text)
                }
            )
        },
        text = {
            OutlinedTextField(
                value = state.draft,
                onValueChange = { state.draft = it },
                label = { Text(stringResource(R.string.editor_text_input_label)) },
                singleLine = false
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val text = state.draft.trim()
                    val existingIndex = state.targetIndex
                    if (existingIndex != null) {
                        if (text.isEmpty()) {
                            onPathRemoved(existingIndex)
                            if (selectedPathIndex == existingIndex) {
                                onPathSelectionChanged(null, null)
                            }
                        } else {
                            val oldPath = paths.getOrNull(existingIndex) as? DrawingPath.TextPath
                            if (oldPath != null) {
                                val updatedPath = oldPath.copy(
                                    text = text,
                                    color = currentColor,
                                    fontSize = textSize,
                                    outlineEnabled = textOutlineEnabled
                                )
                                onPathUpdated(existingIndex, updatedPath)
                                onPathSelectionChanged(existingIndex, updatedPath)
                            }
                        }
                    } else if (text.isNotEmpty()) {
                        onPathAdded(
                            DrawingPath.TextPath(
                                position = state.targetPosition,
                                text = text,
                                color = currentColor,
                                fontSize = textSize,
                                outlineEnabled = textOutlineEnabled
                            )
                        )
                    }
                    state.dismiss()
                }
            ) {
                Text(stringResource(R.string.action_done))
            }
        },
        dismissButton = {
            TextButton(onClick = { state.dismiss() }) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
