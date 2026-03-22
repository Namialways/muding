package com.muding.android.presentation.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool
import kotlin.math.max

class EditorPathHitTester(
    private val textMeasurer: TextMeasurer
) {
    fun actualTextBounds(path: DrawingPath.TextPath): Rect {
        val layout = measureText(path)
        return Rect(
            left = path.position.x,
            top = path.position.y,
            right = path.position.x + layout.size.width,
            bottom = path.position.y + layout.size.height
        )
    }

    fun isInteractivePathHit(path: DrawingPath, offset: Offset, radius: Float): Boolean {
        return when (path) {
            is DrawingPath.TextPath -> {
                val localTouch = rotatePointAround(offset, path.position, -path.rotation)
                actualTextBounds(path)
                    .inflate(max(radius * 1.5f, path.fontSize * path.scale * 0.35f))
                    .contains(localTouch)
            }

            else -> isPathHit(path, offset, radius)
        }
    }

    fun hitIndexAt(
        paths: List<DrawingPath>,
        offset: Offset,
        radius: Float,
        tool: DrawingTool? = null
    ): Int? {
        for (index in paths.indices.reversed()) {
            val path = paths[index]
            if (tool != null && toolFor(path) != tool) continue
            if (isInteractivePathHit(path, offset, radius)) {
                return index
            }
        }
        return null
    }

    fun hitEditableIndexAt(
        paths: List<DrawingPath>,
        offset: Offset,
        radius: Float
    ): Int? {
        for (index in paths.indices.reversed()) {
            val path = paths[index]
            if (path is DrawingPath.PenPath) continue
            if (isInteractivePathHit(path, offset, radius)) {
                return index
            }
        }
        return null
    }

    fun measureText(path: DrawingPath.TextPath): TextLayoutResult {
        return textMeasurer.measure(
            text = path.text,
            style = TextStyle(
                color = path.color,
                fontSize = (path.fontSize * path.scale).sp
            )
        )
    }
}

@Composable
fun rememberEditorPathHitTester(): EditorPathHitTester {
    val textMeasurer = rememberTextMeasurer()
    return remember(textMeasurer) { EditorPathHitTester(textMeasurer) }
}
