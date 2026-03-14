package com.pixpin.android.presentation.crop

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pixpin.android.domain.usecase.CacheImageStore
import com.pixpin.android.presentation.editor.AnnotationEditorActivity
import com.pixpin.android.presentation.theme.PixPinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class RegionCropActivity : ComponentActivity() {

    private lateinit var cacheImageStore: CacheImageStore
    private var sourceBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cacheImageStore = CacheImageStore(this)

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString.isNullOrBlank()) {
            Toast.makeText(this, "Unable to load screenshot", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val uri = Uri.parse(uriString)
            contentResolver.openInputStream(uri)?.use { input ->
                sourceBitmap = BitmapFactory.decodeStream(input)
            }
            if (sourceBitmap == null) throw IllegalStateException("Bitmap decode failed")
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load screenshot: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            PixPinTheme {
                CropOverlayScreen(
                    bitmap = sourceBitmap!!,
                    onCancel = { finish() },
                    onConfirm = { cropRect ->
                        cropAndOpenEditor(cropRect)
                    }
                )
            }
        }
    }

    private fun cropAndOpenEditor(cropRectInBitmap: Rect) {
        val bitmap = sourceBitmap ?: return
        lifecycleScope.launch {
            try {
                val cropped = withContext(Dispatchers.Default) {
                    val left = cropRectInBitmap.left.roundToInt().coerceIn(0, bitmap.width - 1)
                    val top = cropRectInBitmap.top.roundToInt().coerceIn(0, bitmap.height - 1)
                    val right = cropRectInBitmap.right.roundToInt().coerceIn(left + 1, bitmap.width)
                    val bottom = cropRectInBitmap.bottom.roundToInt().coerceIn(top + 1, bitmap.height)
                    Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                }

                val uri = withContext(Dispatchers.IO) {
                    cacheImageStore.writePngToCache(cropped, "screenshots", "region_capture")
                }
                cropped.recycle()

                val editorIntent = Intent(this@RegionCropActivity, AnnotationEditorActivity::class.java).apply {
                    putExtra(AnnotationEditorActivity.EXTRA_IMAGE_URI, uri.toString())
                }
                startActivity(editorIntent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@RegionCropActivity, "Region crop failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sourceBitmap?.recycle()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}

@Composable
private fun CropOverlayScreen(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Rect) -> Unit
) {
    var imageRectOnScreen by remember { mutableStateOf(Rect.Zero) }
    var start by remember { mutableStateOf<Offset?>(null) }
    var end by remember { mutableStateOf<Offset?>(null) }
    var dragging by remember { mutableStateOf(false) }

    val selectionRect = normalizedRect(start, end)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Crop Source",
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(imageRectOnScreen, dragging) {
                    detectTapGestures(onTap = { tapOffset ->
                        if (!dragging && imageRectOnScreen.contains(tapOffset)) {
                            onConfirm(Rect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()))
                        }
                    })
                }
                .pointerInput(imageRectOnScreen) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!imageRectOnScreen.contains(offset)) return@detectDragGestures
                            dragging = true
                            val p = clampOffsetToRect(offset, imageRectOnScreen)
                            start = p
                            end = p
                        },
                        onDrag = { change, _ ->
                            if (dragging) {
                                end = clampOffsetToRect(change.position, imageRectOnScreen)
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            val rect = normalizedRect(start, end)
                            if (dragging && rect != null && rect.width >= 20f && rect.height >= 20f) {
                                val cropRectInBitmap = toBitmapRect(rect, imageRectOnScreen, bitmap.width, bitmap.height)
                                onConfirm(cropRectInBitmap)
                            }
                            dragging = false
                            start = null
                            end = null
                        },
                        onDragCancel = {
                            dragging = false
                            start = null
                            end = null
                        }
                    )
                },
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            imageRectOnScreen = computeImageRect(size.width, size.height, bitmap.width.toFloat(), bitmap.height.toFloat())

            val shade = Color.Black.copy(alpha = 0.22f)
            if (selectionRect == null) {
                drawRect(shade)
            } else {
                drawRect(shade, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(size.width, selectionRect.top))
                drawRect(
                    shade,
                    topLeft = Offset(0f, selectionRect.top),
                    size = androidx.compose.ui.geometry.Size(selectionRect.left, selectionRect.height)
                )
                drawRect(
                    shade,
                    topLeft = Offset(selectionRect.right, selectionRect.top),
                    size = androidx.compose.ui.geometry.Size(size.width - selectionRect.right, selectionRect.height)
                )
                drawRect(
                    shade,
                    topLeft = Offset(0f, selectionRect.bottom),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height - selectionRect.bottom)
                )
                drawRect(
                    color = Color.White,
                    topLeft = selectionRect.topLeft,
                    size = selectionRect.size,
                    style = Stroke(width = 3f)
                )
            }
        }

        Text(
            text = "Tap for full screen, drag to select region",
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
                contentDescription = "Close",
                tint = Color.White
            )
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
