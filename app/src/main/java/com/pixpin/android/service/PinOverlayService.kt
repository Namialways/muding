package com.pixpin.android.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.Rect as AndroidRect
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pixpin.android.domain.usecase.CaptureFlowSettings
import com.pixpin.android.domain.usecase.PinScaleMode
import com.pixpin.android.domain.usecase.RecentPinStore
import com.pixpin.android.domain.usecase.ClosedPinRecord
import com.pixpin.android.presentation.editor.AnnotationEditorActivity
import com.pixpin.android.presentation.theme.PixPinTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class PinOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private data class OverlayEntry(
        val id: String,
        val view: ComposeView,
        val params: WindowManager.LayoutParams,
        val bitmap: Bitmap
    )

    private lateinit var windowManager: WindowManager
    private val overlays = LinkedHashMap<String, OverlayEntry>()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var captureFlowSettings: CaptureFlowSettings

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        captureFlowSettings = CaptureFlowSettings(this)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESTORE_LAST_CLOSED) {
            restoreLastClosedOverlay()
            return START_NOT_STICKY
        }
        val imageUriString = intent?.getStringExtra(EXTRA_IMAGE_URI)
        val annotationSessionId = intent?.getStringExtra(EXTRA_ANNOTATION_SESSION_ID)
        if (!imageUriString.isNullOrBlank()) {
            val imageUri = android.net.Uri.parse(imageUriString)
            serviceScope.launch {
                try {
                    val bitmap = contentResolver.openInputStream(imageUri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                    if (bitmap != null) {
                        showPinOverlay(bitmap, imageUriString, annotationSessionId, captureFlowSettings.getPinScaleMode())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun restoreLastClosedOverlay() {
        val record = RecentPinStore.popMostRecent(this) ?: return
        serviceScope.launch {
            try {
                val bitmap = contentResolver.openInputStream(android.net.Uri.parse(record.imageUri))?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
                if (bitmap != null) {
                    showPinOverlay(
                        bitmap = bitmap,
                        imageUriString = record.imageUri,
                        annotationSessionId = record.annotationSessionId,
                        scaleMode = captureFlowSettings.getPinScaleMode()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showPinOverlay(
        bitmap: Bitmap,
        imageUriString: String,
        annotationSessionId: String?,
        scaleMode: PinScaleMode
    ) {
        val overlayId = UUID.randomUUID().toString()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 120 + overlays.size * 36
            y = 220 + overlays.size * 36
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PinOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PinOverlayService)
            setContent {
                PixPinTheme {
                    PinnedImageContent(
                        bitmap = bitmap,
                        scaleMode = scaleMode,
                        onContentSizeChanged = { width, height ->
                            params.width = width
                            params.height = height
                            val clamped = clampOverlayPosition(
                                currentX = params.x,
                                currentY = params.y,
                                width = width,
                                height = height,
                                minVisiblePx = (resources.displayMetrics.density * 48f).roundToInt()
                            )
                            params.x = clamped.first
                            params.y = clamped.second
                            updateOverlayLayout(overlayId)
                        },
                        onMoveWindow = { dx, dy ->
                            val clamped = clampOverlayPosition(
                                currentX = params.x + dx.roundToInt(),
                                currentY = params.y + dy.roundToInt(),
                                width = params.width.takeIf { it > 0 } ?: this.width,
                                height = params.height.takeIf { it > 0 } ?: this.height,
                                minVisiblePx = (resources.displayMetrics.density * 48f).roundToInt()
                            )
                            params.x = clamped.first
                            params.y = clamped.second
                            updateOverlayLayout(overlayId)
                        },
                        onClose = {
                            RecentPinStore.push(
                                this@PinOverlayService,
                                ClosedPinRecord(
                                    imageUri = imageUriString,
                                    annotationSessionId = annotationSessionId
                                )
                            )
                            removeOverlay(overlayId)
                        },
                        onEdit = {
                            val editorIntent = Intent(this@PinOverlayService, AnnotationEditorActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                if (!annotationSessionId.isNullOrBlank()) {
                                    putExtra(AnnotationEditorActivity.EXTRA_ANNOTATION_SESSION_ID, annotationSessionId)
                                } else {
                                    putExtra(AnnotationEditorActivity.EXTRA_IMAGE_URI, imageUriString)
                                }
                            }
                            startActivity(editorIntent)
                            removeOverlay(overlayId)
                        }
                    )
                }
            }
        }

        val entry = OverlayEntry(
            id = overlayId,
            view = composeView,
            params = params,
            bitmap = bitmap
        )
        overlays[overlayId] = entry

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            overlays.remove(overlayId)
            bitmap.recycle()
            e.printStackTrace()
        }
    }

    private fun updateOverlayLayout(overlayId: String) {
        val entry = overlays[overlayId] ?: return
        try {
            windowManager.updateViewLayout(entry.view, entry.params)
        } catch (_: Exception) {
        }
    }

    private fun removeOverlay(overlayId: String) {
        val entry = overlays.remove(overlayId) ?: return
        try {
            windowManager.removeView(entry.view)
        } catch (_: Exception) {
        }
        entry.bitmap.recycle()
        if (overlays.isEmpty()) {
            stopSelf()
        }
    }

    private fun clampOverlayPosition(
        currentX: Int,
        currentY: Int,
        width: Int,
        height: Int,
        minVisiblePx: Int
    ): Pair<Int, Int> {
        val screen = getScreenBounds()
        val viewWidth = width.takeIf { it > 0 } ?: 320
        val viewHeight = height.takeIf { it > 0 } ?: 220
        val minX = screen.left - viewWidth + minVisiblePx
        val maxX = screen.right - minVisiblePx
        val minY = screen.top - viewHeight + minVisiblePx
        val maxY = screen.bottom - minVisiblePx
        return currentX.coerceIn(minX, maxX) to currentY.coerceIn(minY, maxY)
    }

    private fun getScreenBounds(): AndroidRect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            @Suppress("DEPRECATION")
            AndroidRect().also { rect ->
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRectSize(rect)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        overlays.values.toList().forEach { entry ->
            try {
                windowManager.removeView(entry.view)
            } catch (_: Exception) {
            }
            entry.bitmap.recycle()
        }
        overlays.clear()
    }

    companion object {
        const val ACTION_RESTORE_LAST_CLOSED = "com.pixpin.android.action.RESTORE_LAST_CLOSED"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_ANNOTATION_SESSION_ID = "extra_annotation_session_id"
    }
}

@Composable
private fun PinnedImageContent(
    bitmap: Bitmap,
    scaleMode: PinScaleMode,
    onContentSizeChanged: (Int, Int) -> Unit,
    onMoveWindow: (Float, Float) -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit
) {
    val aspect = if (bitmap.height == 0) 1f else bitmap.width.toFloat() / bitmap.height.toFloat()
    val baseWidth = 260.dp
    val baseHeight = (baseWidth / aspect).coerceAtLeast(90.dp)

    var uniformScale by remember { mutableStateOf(1f) }
    var freeScaleX by remember { mutableStateOf(1f) }
    var freeScaleY by remember { mutableStateOf(1f) }
    var locked by remember { mutableStateOf(false) }

    fun resetScale() {
        uniformScale = 1f
        freeScaleX = 1f
        freeScaleY = 1f
    }

    fun resizeFreeScale(deltaX: Float, deltaY: Float) {
        val currentWidth = if (scaleMode == PinScaleMode.LOCK_ASPECT) {
            baseWidth.value * uniformScale
        } else {
            baseWidth.value * freeScaleX
        }
        val currentHeight = if (scaleMode == PinScaleMode.LOCK_ASPECT) {
            baseHeight.value * uniformScale
        } else {
            baseHeight.value * freeScaleY
        }
        val nextWidth = max(80f, currentWidth + deltaX)
        val nextHeight = max(48f, currentHeight + deltaY)
        freeScaleX = (nextWidth / baseWidth.value).coerceIn(0.2f, 6f)
        freeScaleY = (nextHeight / baseHeight.value).coerceIn(0.2f, 6f)
    }

    val displayWidth: Dp
    val displayHeight: Dp
    if (scaleMode == PinScaleMode.LOCK_ASPECT) {
        displayWidth = (baseWidth * uniformScale).coerceAtLeast(80.dp)
        displayHeight = (baseHeight * uniformScale).coerceAtLeast(48.dp)
    } else {
        displayWidth = (baseWidth * freeScaleX).coerceAtLeast(80.dp)
        displayHeight = (baseHeight * freeScaleY).coerceAtLeast(48.dp)
    }

    Box(
        modifier = Modifier
            .requiredSize(displayWidth, displayHeight)
            .onSizeChanged { size ->
                onContentSizeChanged(size.width, size.height)
            }
            .pointerInput(scaleMode, locked) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (!locked && (pan.x != 0f || pan.y != 0f)) {
                        onMoveWindow(pan.x, pan.y)
                    }
                    if (!locked && zoom != 1f) {
                        if (scaleMode == PinScaleMode.LOCK_ASPECT) {
                            uniformScale = (uniformScale * zoom).coerceIn(0.2f, 6f)
                        } else {
                            freeScaleX = (freeScaleX * zoom).coerceIn(0.2f, 6f)
                            freeScaleY = (freeScaleY * zoom).coerceIn(0.2f, 6f)
                        }
                    }
                }
            }
            .pointerInput(locked) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!locked) resetScale()
                    }
                )
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "贴图",
            modifier = Modifier.requiredSize(displayWidth, displayHeight)
        )

        FilledIconButton(
            onClick = onEdit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(30.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "编辑",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }

        FilledIconButton(
            onClick = { locked = !locked },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .size(30.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(
                imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (locked) "解锁" else "锁定",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }

        FilledIconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(30.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "关闭",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = buildString {
                append(if (scaleMode == PinScaleMode.FREE_SCALE) "自由缩放" else "等比缩放")
                append(" | ")
                append(if (locked) "已锁定" else "可移动")
            },
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        )

        if (scaleMode == PinScaleMode.FREE_SCALE && !locked) {
            FreeScaleHandle(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset { IntOffset(10, 0) },
                onDragDelta = { dx, _ -> resizeFreeScale(dx, 0f) }
            )
            FreeScaleHandle(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, 10) },
                onDragDelta = { _, dy -> resizeFreeScale(0f, dy) }
            )
            FreeScaleHandle(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset { IntOffset(10, 10) },
                onDragDelta = { dx, dy -> resizeFreeScale(dx, dy) }
            )
        }
    }
}

@Composable
private fun FreeScaleHandle(
    modifier: Modifier = Modifier,
    onDragDelta: (Float, Float) -> Unit
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .background(Color.White.copy(alpha = 0.95f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    onDragDelta(dragAmount.x, dragAmount.y)
                    change.consume()
                }
            }
    )
}
