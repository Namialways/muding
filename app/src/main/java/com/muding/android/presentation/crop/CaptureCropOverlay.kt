package com.muding.android.presentation.crop

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muding.android.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun CaptureCropOverlay(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Rect) -> Unit
) {
    var imageRectOnScreen by remember { mutableStateOf(Rect.Zero) }
    var selectionRect by remember { mutableStateOf<Rect?>(null) }
    var dragMode by remember { mutableStateOf<CropDragMode?>(null) }
    var dragStartPoint by remember { mutableStateOf<Offset?>(null) }
    var dragStartSelection by remember { mutableStateOf<Rect?>(null) }
    val minSelectionSize = 32f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.crop_overlay_image_description),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(imageRectOnScreen) {
                    detectTapGestures(onTap = { tapOffset ->
                        if (!imageRectOnScreen.contains(tapOffset)) {
                            return@detectTapGestures
                        }
                        if (selectionRect == null) {
                            onConfirm(Rect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()))
                        } else if (selectionRect?.contains(tapOffset) == false) {
                            selectionRect = null
                        }
                    })
                }
                .pointerInput(imageRectOnScreen) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!imageRectOnScreen.contains(offset)) return@detectDragGestures
                            dragStartPoint = clampOffsetToRect(offset, imageRectOnScreen)
                            dragStartSelection = selectionRect
                            dragMode = resolveDragMode(
                                touch = dragStartPoint!!,
                                selection = selectionRect,
                                handleRadius = 28f
                            ) ?: CropDragMode.Create
                            if (dragMode == CropDragMode.Create) {
                                selectionRect = Rect(dragStartPoint!!, dragStartPoint!!)
                            }
                        },
                        onDrag = { change, _ ->
                            val startPoint = dragStartPoint ?: return@detectDragGestures
                            val startSelection = dragStartSelection
                            when (val mode = dragMode) {
                                CropDragMode.Create -> {
                                    selectionRect = normalizedRect(
                                        startPoint,
                                        clampOffsetToRect(change.position, imageRectOnScreen)
                                    )
                                }

                                CropDragMode.Move -> {
                                    val baseRect = startSelection ?: return@detectDragGestures
                                    selectionRect = moveRectWithinBounds(
                                        baseRect,
                                        change.position - startPoint,
                                        imageRectOnScreen
                                    )
                                }

                                is CropDragMode.Resize -> {
                                    val baseRect = startSelection ?: return@detectDragGestures
                                    selectionRect = resizeRectWithinBounds(
                                        original = baseRect,
                                        handle = mode.handle,
                                        touch = clampOffsetToRect(change.position, imageRectOnScreen),
                                        bounds = imageRectOnScreen,
                                        minSize = minSelectionSize
                                    )
                                }

                                null -> Unit
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            val rect = selectionRect
                            if (rect != null && (rect.width < minSelectionSize || rect.height < minSelectionSize)) {
                                selectionRect = null
                            }
                            dragMode = null
                            dragStartPoint = null
                            dragStartSelection = null
                        },
                        onDragCancel = {
                            dragMode = null
                            dragStartPoint = null
                            dragStartSelection = null
                        }
                    )
                },
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            imageRectOnScreen = computeImageRect(
                size.width,
                size.height,
                bitmap.width.toFloat(),
                bitmap.height.toFloat()
            )

            val shade = Color.Black.copy(alpha = 0.22f)
            val currentSelection = selectionRect
            if (currentSelection == null) {
                drawRect(shade)
            } else {
                drawRect(
                    shade,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, currentSelection.top)
                )
                drawRect(
                    shade,
                    topLeft = Offset(0f, currentSelection.top),
                    size = androidx.compose.ui.geometry.Size(currentSelection.left, currentSelection.height)
                )
                drawRect(
                    shade,
                    topLeft = Offset(currentSelection.right, currentSelection.top),
                    size = androidx.compose.ui.geometry.Size(size.width - currentSelection.right, currentSelection.height)
                )
                drawRect(
                    shade,
                    topLeft = Offset(0f, currentSelection.bottom),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height - currentSelection.bottom)
                )
                drawRect(
                    color = Color.White,
                    topLeft = currentSelection.topLeft,
                    size = currentSelection.size,
                    style = Stroke(width = 3f)
                )
                drawSelectionHandles(currentSelection)
            }
        }

        Text(
            text = stringResource(R.string.crop_overlay_hint),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )

        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_cancel),
                tint = Color.White
            )
        }

        if (selectionRect != null) {
            IconButton(
                onClick = {
                    val rect = selectionRect ?: return@IconButton
                    onConfirm(toBitmapRect(rect, imageRectOnScreen, bitmap.width, bitmap.height))
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.crop_overlay_confirm),
                    tint = Color.White
                )
            }
        }
    }
}

private fun computeImageRect(
    containerWidth: Float,
    containerHeight: Float,
    imageWidth: Float,
    imageHeight: Float
): Rect {
    val scale = min(containerWidth / imageWidth, containerHeight / imageHeight)
    val displayedWidth = imageWidth * scale
    val displayedHeight = imageHeight * scale
    val left = (containerWidth - displayedWidth) / 2f
    val top = (containerHeight - displayedHeight) / 2f
    return Rect(left, top, left + displayedWidth, top + displayedHeight)
}

private fun clampOffsetToRect(offset: Offset, rect: Rect): Offset {
    val x = offset.x.coerceIn(rect.left, rect.right)
    val y = offset.y.coerceIn(rect.top, rect.bottom)
    return Offset(x, y)
}

private fun normalizedRect(start: Offset?, end: Offset?): Rect? {
    if (start == null || end == null) return null
    return Rect(
        left = min(start.x, end.x),
        top = min(start.y, end.y),
        right = max(start.x, end.x),
        bottom = max(start.y, end.y)
    )
}

private sealed interface CropDragMode {
    data object Create : CropDragMode
    data object Move : CropDragMode
    data class Resize(val handle: CropHandle) : CropDragMode
}

private enum class CropHandle {
    TopLeft,
    Top,
    TopRight,
    Right,
    BottomRight,
    Bottom,
    BottomLeft,
    Left
}

private fun resolveDragMode(
    touch: Offset,
    selection: Rect?,
    handleRadius: Float
): CropDragMode? {
    val rect = selection ?: return null
    findTouchedHandle(rect, touch, handleRadius)?.let { return CropDragMode.Resize(it) }
    if (rect.contains(touch)) return CropDragMode.Move
    return null
}

private fun findTouchedHandle(rect: Rect, touch: Offset, radius: Float): CropHandle? {
    val handleCenters = mapOf(
        CropHandle.TopLeft to rect.topLeft,
        CropHandle.Top to Offset(rect.center.x, rect.top),
        CropHandle.TopRight to Offset(rect.right, rect.top),
        CropHandle.Right to Offset(rect.right, rect.center.y),
        CropHandle.BottomRight to rect.bottomRight,
        CropHandle.Bottom to Offset(rect.center.x, rect.bottom),
        CropHandle.BottomLeft to Offset(rect.left, rect.bottom),
        CropHandle.Left to Offset(rect.left, rect.center.y)
    )
    return handleCenters.entries.firstOrNull { (_, center) ->
        abs(center.x - touch.x) <= radius && abs(center.y - touch.y) <= radius
    }?.key
}

private fun moveRectWithinBounds(rect: Rect, delta: Offset, bounds: Rect): Rect {
    val width = rect.width
    val height = rect.height
    val newLeft = (rect.left + delta.x).coerceIn(bounds.left, bounds.right - width)
    val newTop = (rect.top + delta.y).coerceIn(bounds.top, bounds.bottom - height)
    return Rect(newLeft, newTop, newLeft + width, newTop + height)
}

private fun resizeRectWithinBounds(
    original: Rect,
    handle: CropHandle,
    touch: Offset,
    bounds: Rect,
    minSize: Float
): Rect {
    var left = original.left
    var top = original.top
    var right = original.right
    var bottom = original.bottom

    when (handle) {
        CropHandle.TopLeft -> {
            left = touch.x
            top = touch.y
        }
        CropHandle.Top -> top = touch.y
        CropHandle.TopRight -> {
            right = touch.x
            top = touch.y
        }
        CropHandle.Right -> right = touch.x
        CropHandle.BottomRight -> {
            right = touch.x
            bottom = touch.y
        }
        CropHandle.Bottom -> bottom = touch.y
        CropHandle.BottomLeft -> {
            left = touch.x
            bottom = touch.y
        }
        CropHandle.Left -> left = touch.x
    }

    left = left.coerceIn(bounds.left, right - minSize)
    top = top.coerceIn(bounds.top, bottom - minSize)
    right = right.coerceIn(left + minSize, bounds.right)
    bottom = bottom.coerceIn(top + minSize, bounds.bottom)

    return Rect(left, top, right, bottom)
}

private fun DrawScope.drawSelectionHandles(rect: Rect) {
    val handles = listOf(
        rect.topLeft,
        Offset(rect.center.x, rect.top),
        Offset(rect.right, rect.top),
        Offset(rect.right, rect.center.y),
        rect.bottomRight,
        Offset(rect.center.x, rect.bottom),
        Offset(rect.left, rect.bottom),
        Offset(rect.left, rect.center.y)
    )
    handles.forEach { center ->
        drawCircle(color = Color.White, radius = 10f, center = center)
        drawCircle(color = Color.Black, radius = 6f, center = center)
    }
}

private fun toBitmapRect(selection: Rect, imageRectOnScreen: Rect, bitmapWidth: Int, bitmapHeight: Int): Rect {
    val scaleX = bitmapWidth / imageRectOnScreen.width
    val scaleY = bitmapHeight / imageRectOnScreen.height

    val left = (selection.left - imageRectOnScreen.left) * scaleX
    val top = (selection.top - imageRectOnScreen.top) * scaleY
    val right = (selection.right - imageRectOnScreen.left) * scaleX
    val bottom = (selection.bottom - imageRectOnScreen.top) * scaleY

    return Rect(
        left = left.coerceIn(0f, bitmapWidth.toFloat()),
        top = top.coerceIn(0f, bitmapHeight.toFloat()),
        right = right.coerceIn(0f, bitmapWidth.toFloat()),
        bottom = bottom.coerceIn(0f, bitmapHeight.toFloat())
    )
}
