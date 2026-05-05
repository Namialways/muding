package com.muding.android.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muding.android.domain.model.DrawingPath
import kotlin.math.roundToInt

@Stable
class EditorTextEditState {
    var isEditing by mutableStateOf(false)
        private set
    var draft by mutableStateOf("")
    var targetIndex by mutableStateOf<Int?>(null)
        private set
    var targetPosition by mutableStateOf(Offset.Zero)
        private set
    var editSessionId by mutableIntStateOf(0)
        private set

    fun open(index: Int?, position: Offset, initialText: String) {
        targetIndex = index
        targetPosition = position
        draft = initialText
        isEditing = true
        editSessionId += 1
    }

    fun moveEditor(delta: Offset) {
        targetPosition += delta
    }

    fun dismiss() {
        targetIndex = null
        isEditing = false
        draft = ""
    }
}

sealed interface EditorTextCommitResult {
    data object None : EditorTextCommitResult
    data class Add(val path: DrawingPath.TextPath) : EditorTextCommitResult
    data class Update(val index: Int, val path: DrawingPath.TextPath) : EditorTextCommitResult
    data class Remove(val index: Int) : EditorTextCommitResult
}

internal fun buildEditorTextCommitResult(
    paths: List<DrawingPath>,
    targetIndex: Int?,
    position: Offset,
    draft: String,
    color: Color,
    textSize: Float,
    textOutlineEnabled: Boolean
): EditorTextCommitResult {
    val text = draft.trim()
    if (targetIndex != null) {
        if (text.isEmpty()) {
            return EditorTextCommitResult.Remove(targetIndex)
        }
        val oldPath = paths.getOrNull(targetIndex) as? DrawingPath.TextPath
            ?: return EditorTextCommitResult.None
        return EditorTextCommitResult.Update(
            index = targetIndex,
            path = oldPath.copy(
                text = text,
                color = color,
                fontSize = textSize,
                outlineEnabled = textOutlineEnabled
            )
        )
    }
    if (text.isEmpty()) {
        return EditorTextCommitResult.None
    }
    return EditorTextCommitResult.Add(
        DrawingPath.TextPath(
            position = position,
            text = text,
            color = color,
            fontSize = textSize,
            outlineEnabled = textOutlineEnabled
        )
    )
}

@Composable
fun rememberEditorTextEditState(): EditorTextEditState {
    return remember { EditorTextEditState() }
}

@Composable
fun EditorInlineTextEditor(
    modifier: Modifier = Modifier,
    state: EditorTextEditState,
    canvasSize: Size,
    paths: List<DrawingPath>,
    selectedPathIndex: Int?,
    currentColor: Color,
    textSize: Float,
    textOutlineEnabled: Boolean,
    onPathAdded: (DrawingPath) -> Unit,
    onPathUpdated: (Int, DrawingPath) -> Unit,
    onPathRemoved: (Int) -> Unit,
    onPathSelectionChanged: (Int?, DrawingPath?) -> Unit
) {
    if (!state.isEditing) return

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember(state.editSessionId) { FocusRequester() }

    fun commit() {
        when (
            val result = buildEditorTextCommitResult(
                paths = paths,
                targetIndex = state.targetIndex,
                position = state.targetPosition,
                draft = state.draft,
                color = currentColor,
                textSize = textSize,
                textOutlineEnabled = textOutlineEnabled
            )
        ) {
            is EditorTextCommitResult.Add -> onPathAdded(result.path)
            is EditorTextCommitResult.Update -> {
                onPathUpdated(result.index, result.path)
                onPathSelectionChanged(result.index, result.path)
            }
            is EditorTextCommitResult.Remove -> {
                onPathRemoved(result.index)
                if (selectedPathIndex == result.index) {
                    onPathSelectionChanged(null, null)
                }
            }
            EditorTextCommitResult.None -> Unit
        }
        state.dismiss()
        keyboardController?.hide()
    }

    LaunchedEffect(state.editSessionId) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.editSessionId) {
                    detectTapGestures(onTap = { commit() })
                }
        )
        InlineTextEditorField(
            state = state,
            canvasSize = canvasSize,
            currentColor = currentColor,
            textSize = textSize,
            focusRequester = focusRequester,
            onDone = { commit() },
            onCancel = {
                state.dismiss()
                keyboardController?.hide()
            }
        )
    }
}

@Composable
private fun InlineTextEditorField(
    state: EditorTextEditState,
    canvasSize: Size,
    currentColor: Color,
    textSize: Float,
    focusRequester: FocusRequester,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(8.dp)
    val minWidth = 112.dp
    val maxWidth = with(density) {
        val remainingPx = (canvasSize.width - state.targetPosition.x - 20f).coerceAtLeast(160f)
        remainingPx.toDp().coerceAtMost(280.dp)
    }
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val outlineColor = currentColor.copy(alpha = 0.86f)

    Column(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = state.targetPosition.x.roundToInt(),
                    y = state.targetPosition.y.roundToInt()
                )
            }
            .widthIn(min = minWidth, max = maxWidth)
            .background(surfaceColor, shape)
            .border(1.dp, outlineColor, shape)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicTextField(
            value = state.draft,
            onValueChange = { state.draft = it },
            modifier = Modifier
                .focusRequester(focusRequester)
                .defaultMinSize(minWidth = 96.dp, minHeight = 44.dp)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            textStyle = TextStyle(
                color = currentColor,
                fontSize = textSize.sp
            ),
            cursorBrush = SolidColor(currentColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            maxLines = 5
        )

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(
                onClick = onDone,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "完成"
                )
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消"
                )
            }
        }
    }
}
