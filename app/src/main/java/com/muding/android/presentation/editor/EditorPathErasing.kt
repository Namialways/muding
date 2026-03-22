package com.muding.android.presentation.editor

import androidx.compose.ui.geometry.Offset
import com.muding.android.domain.model.DrawingPath

fun erasePathAt(
    touch: Offset,
    paths: List<DrawingPath>,
    eraserMode: EraserMode,
    eraserSize: Float,
    onPathReplaced: (Int, List<DrawingPath>) -> Unit,
    onPathRemoved: (Int) -> Unit
) {
    for (index in paths.indices.reversed()) {
        val path = paths[index]
        if (eraserMode == EraserMode.PARTIAL && path is DrawingPath.PenPath) {
            val replacement = erasePenPathPartially(path, touch, eraserSize / 2f)
            if (replacement != null) {
                onPathReplaced(index, replacement)
                return
            }
        } else if (isPathHit(path, touch, eraserSize / 2f)) {
            onPathRemoved(index)
            return
        }
    }
}

fun erasePenPathPartially(
    path: DrawingPath.PenPath,
    touch: Offset,
    radius: Float
): List<DrawingPath.PenPath>? {
    if (path.points.none { (it - touch).getDistance() <= radius }) return null

    val keptSegments = mutableListOf<List<Offset>>()
    var currentSegment = mutableListOf<Offset>()
    path.points.forEach { point ->
        if ((point - touch).getDistance() <= radius) {
            if (currentSegment.size >= 2) {
                keptSegments += currentSegment.toList()
            }
            currentSegment = mutableListOf()
        } else {
            currentSegment.add(point)
        }
    }
    if (currentSegment.size >= 2) {
        keptSegments += currentSegment.toList()
    }

    return keptSegments.map { segment ->
        DrawingPath.PenPath(
            path = buildComposePath(segment),
            color = path.color,
            strokeWidth = path.strokeWidth,
            points = segment
        )
    }
}
