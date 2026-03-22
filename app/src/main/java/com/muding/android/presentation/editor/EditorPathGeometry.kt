package com.muding.android.presentation.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val HANDLE_HIT_RADIUS = 24f
internal const val MIN_CIRCLE_RADIUS = 16f

sealed interface PathResizeHandle {
    data object ArrowStart : PathResizeHandle
    data object ArrowEnd : PathResizeHandle
    data object RectTopLeft : PathResizeHandle
    data object RectTopRight : PathResizeHandle
    data object RectBottomLeft : PathResizeHandle
    data object RectBottomRight : PathResizeHandle
    data object CircleRadius : PathResizeHandle
}

data class ActiveResizeState(
    val index: Int,
    val handle: PathResizeHandle
)

fun selectionAccentColor(path: DrawingPath): Color {
    return when (path) {
        is DrawingPath.PenPath -> path.color
        is DrawingPath.ArrowPath -> path.color
        is DrawingPath.RectanglePath -> path.color
        is DrawingPath.CirclePath -> path.color
        is DrawingPath.TextPath -> path.color
    }
}

fun boundsForPath(path: DrawingPath): Rect? {
    return when (path) {
        is DrawingPath.PenPath -> {
            if (path.points.isEmpty()) {
                null
            } else {
                Rect(
                    path.points.minOf { it.x } - path.strokeWidth * 1.5f,
                    path.points.minOf { it.y } - path.strokeWidth * 1.5f,
                    path.points.maxOf { it.x } + path.strokeWidth * 1.5f,
                    path.points.maxOf { it.y } + path.strokeWidth * 1.5f
                )
            }
        }

        is DrawingPath.ArrowPath -> Rect(
            min(path.start.x, path.end.x) - path.strokeWidth * 2f,
            min(path.start.y, path.end.y) - path.strokeWidth * 2f,
            max(path.start.x, path.end.x) + path.strokeWidth * 2f,
            max(path.start.y, path.end.y) + path.strokeWidth * 2f
        )

        is DrawingPath.RectanglePath -> {
            val corners = rectangleCorners(path)
            Rect(
                left = corners.minOf { it.x },
                top = corners.minOf { it.y },
                right = corners.maxOf { it.x },
                bottom = corners.maxOf { it.y }
            )
        }

        is DrawingPath.CirclePath -> Rect(
            path.center.x - path.radius,
            path.center.y - path.radius,
            path.center.x + path.radius,
            path.center.y + path.radius
        )

        is DrawingPath.TextPath -> estimateTextBounds(path)
    }
}

fun normalizeRotation(rotationDegrees: Float): Float {
    var normalized = rotationDegrees
    while (normalized <= -180f) normalized += 360f
    while (normalized > 180f) normalized -= 360f
    return normalized
}

fun isPathHit(path: DrawingPath, touch: Offset, radius: Float): Boolean {
    return when (path) {
        is DrawingPath.PenPath -> distanceToPolyline(touch, path.points) <= max(radius * 1.35f, path.strokeWidth * 2.8f)
        is DrawingPath.ArrowPath -> distanceToSegment(touch, path.start, path.end) <= max(radius * 1.25f, path.strokeWidth * 2.6f)
        is DrawingPath.RectanglePath -> isRectangleHit(path, touch, radius)
        is DrawingPath.CirclePath -> {
            val distance = (touch - path.center).getDistance()
            distance <= path.radius + max(radius, path.strokeWidth * 1.5f)
        }

        is DrawingPath.TextPath -> estimateTextBounds(path)
            .inflate(max(radius * 1.5f, path.fontSize * path.scale * 0.35f))
            .contains(touch)
    }
}

fun resizeHandleHit(path: DrawingPath, touch: Offset): PathResizeHandle? {
    return when (path) {
        is DrawingPath.ArrowPath -> listOf(
            PathResizeHandle.ArrowStart to path.start,
            PathResizeHandle.ArrowEnd to path.end
        ).firstOrNull { (_, center) ->
            (center - touch).getDistance() <= HANDLE_HIT_RADIUS * 1.2f
        }?.first

        is DrawingPath.RectanglePath -> null
        is DrawingPath.CirclePath -> null
        else -> null
    }
}

fun movePath(path: DrawingPath, delta: Offset): DrawingPath {
    return when (path) {
        is DrawingPath.PenPath -> {
            val movedPoints = path.points.map { it + delta }
            path.copy(path = buildComposePath(movedPoints), points = movedPoints)
        }

        is DrawingPath.ArrowPath -> path.copy(start = path.start + delta, end = path.end + delta)
        is DrawingPath.RectanglePath -> path.copy(topLeft = path.topLeft + delta, bottomRight = path.bottomRight + delta)
        is DrawingPath.CirclePath -> path.copy(center = path.center + delta)
        is DrawingPath.TextPath -> path.copy(position = path.position + delta)
    }
}

fun resizePath(path: DrawingPath, handle: PathResizeHandle, touch: Offset): DrawingPath {
    return when {
        path is DrawingPath.ArrowPath && handle is PathResizeHandle.ArrowStart -> {
            path.copy(start = touch)
        }

        path is DrawingPath.ArrowPath && handle is PathResizeHandle.ArrowEnd -> {
            path.copy(end = touch)
        }

        path is DrawingPath.RectanglePath -> {
            val center = rectangleCenter(path)
            val localTouch = rotatePointAround(touch, center, -path.rotation)
            val currentTopLeft = path.topLeft
            val currentBottomRight = path.bottomRight
            val updatedTopLeft = when (handle) {
                PathResizeHandle.RectTopLeft,
                PathResizeHandle.RectBottomLeft -> Offset(localTouch.x, currentTopLeft.y)

                else -> currentTopLeft
            }
            val updatedBottomRight = when (handle) {
                PathResizeHandle.RectTopRight,
                PathResizeHandle.RectBottomRight -> Offset(localTouch.x, currentBottomRight.y)

                else -> currentBottomRight
            }
            val topY = when (handle) {
                PathResizeHandle.RectTopLeft,
                PathResizeHandle.RectTopRight -> localTouch.y

                else -> currentTopLeft.y
            }
            val bottomY = when (handle) {
                PathResizeHandle.RectBottomLeft,
                PathResizeHandle.RectBottomRight -> localTouch.y

                else -> currentBottomRight.y
            }

            path.copy(
                topLeft = Offset(
                    x = min(updatedTopLeft.x, updatedBottomRight.x),
                    y = min(topY, bottomY)
                ),
                bottomRight = Offset(
                    x = max(updatedTopLeft.x, updatedBottomRight.x),
                    y = max(topY, bottomY)
                ),
                rotation = path.rotation
            )
        }

        path is DrawingPath.CirclePath && handle is PathResizeHandle.CircleRadius -> {
            path.copy(radius = max((touch - path.center).getDistance(), MIN_CIRCLE_RADIUS))
        }

        else -> path
    }
}

fun buildComposePath(points: List<Offset>): Path {
    return Path().apply {
        val first = points.firstOrNull() ?: return@apply
        moveTo(first.x, first.y)
        points.drop(1).forEach { point -> lineTo(point.x, point.y) }
    }
}

fun rectangleCenter(path: DrawingPath.RectanglePath): Offset {
    return Offset(
        x = (path.topLeft.x + path.bottomRight.x) / 2f,
        y = (path.topLeft.y + path.bottomRight.y) / 2f
    )
}

fun rectangleCorners(path: DrawingPath.RectanglePath): List<Offset> {
    val center = rectangleCenter(path)
    val corners = listOf(
        path.topLeft,
        Offset(path.bottomRight.x, path.topLeft.y),
        path.bottomRight,
        Offset(path.topLeft.x, path.bottomRight.y)
    )
    return corners.map { rotatePointAround(it, center, path.rotation) }
}

fun rotatePointAround(point: Offset, pivot: Offset, rotationDegrees: Float): Offset {
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val translatedX = point.x - pivot.x
    val translatedY = point.y - pivot.y
    val rotatedX = (translatedX * cos(radians) - translatedY * sin(radians)).toFloat()
    val rotatedY = (translatedX * sin(radians) + translatedY * cos(radians)).toFloat()
    return Offset(rotatedX + pivot.x, rotatedY + pivot.y)
}

fun isRectangleHit(path: DrawingPath.RectanglePath, touch: Offset, radius: Float): Boolean {
    val center = rectangleCenter(path)
    val localTouch = rotatePointAround(touch, center, -path.rotation)
    return Rect(path.topLeft, path.bottomRight).inflate(radius).contains(localTouch)
}

fun transformRectangle(
    path: DrawingPath.RectanglePath,
    pan: Offset,
    zoom: Float,
    rotation: Float
): DrawingPath.RectanglePath {
    val center = rectangleCenter(path) + pan
    val halfWidth = ((path.bottomRight.x - path.topLeft.x) / 2f * zoom).coerceAtLeast(12f)
    val halfHeight = ((path.bottomRight.y - path.topLeft.y) / 2f * zoom).coerceAtLeast(12f)
    return path.copy(
        topLeft = Offset(center.x - halfWidth, center.y - halfHeight),
        bottomRight = Offset(center.x + halfWidth, center.y + halfHeight),
        rotation = normalizeRotation(path.rotation + rotation)
    )
}

fun estimateTextBounds(path: DrawingPath.TextPath): Rect {
    val lines = path.text.split('\n')
    val widestLine = lines.maxOfOrNull { it.length } ?: 1
    val width = widestLine.coerceAtLeast(1) * path.fontSize * path.scale * 0.62f
    val height = lines.size.coerceAtLeast(1) * path.fontSize * path.scale * 1.35f
    return Rect(
        left = path.position.x,
        top = path.position.y,
        right = path.position.x + width,
        bottom = path.position.y + height
    )
}

fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
    val segment = end - start
    val lengthSquared = (segment.x * segment.x) + (segment.y * segment.y)
    if (lengthSquared == 0f) return (point - start).getDistance()
    val t = (((point.x - start.x) * segment.x) + ((point.y - start.y) * segment.y)) / lengthSquared
    val clamped = t.coerceIn(0f, 1f)
    val projection = Offset(start.x + (clamped * segment.x), start.y + (clamped * segment.y))
    return (point - projection).getDistance()
}

fun distanceToPolyline(point: Offset, points: List<Offset>): Float {
    if (points.isEmpty()) return Float.MAX_VALUE
    if (points.size == 1) return (point - points.first()).getDistance()

    var minDistance = Float.MAX_VALUE
    for (index in 0 until points.lastIndex) {
        minDistance = min(minDistance, distanceToSegment(point, points[index], points[index + 1]))
    }
    return minDistance
}

fun toolFor(path: DrawingPath): DrawingTool {
    return when (path) {
        is DrawingPath.PenPath -> DrawingTool.PEN
        is DrawingPath.ArrowPath -> DrawingTool.ARROW
        is DrawingPath.RectanglePath -> DrawingTool.RECTANGLE
        is DrawingPath.CirclePath -> DrawingTool.CIRCLE
        is DrawingPath.TextPath -> DrawingTool.TEXT
    }
}
