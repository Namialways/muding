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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
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
import com.pixpin.android.presentation.editor.AnnotationEditorActivity
import com.pixpin.android.presentation.theme.PixPinTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
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
                        onMoveWindow = { dx, dy ->
                            val clamped = clampOverlayPosition(
                                currentX = params.x + dx.roundToInt(),
                                currentY = params.y + dy.roundToInt(),
                                view = this,
                                minVisiblePx = (resources.displayMetrics.density * 48f).roundToInt()
                            )
                            params.x = clamped.first
                            params.y = clamped.second
                            updateOverlayLayout(overlayId)
                        },
                        onClose = { removeOverlay(overlayId) },
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
        view: ComposeView,
        minVisiblePx: Int
    ): Pair<Int, Int> {
        val screen = getScreenBounds()
        val viewWidth = view.width.takeIf { it > 0 } ?: 320
        val viewHeight = view.height.takeIf { it > 0 } ?: 220
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
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_ANNOTATION_SESSION_ID = "extra_annotation_session_id"
    }
}

@Composable
private fun PinnedImageContent(
    bitmap: Bitmap,
    scaleMode: PinScaleMode,
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

    Box {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Pinned Image",
            modifier = Modifier
                .size(baseWidth, baseHeight)
                .graphicsLayer(
                    scaleX = if (scaleMode == PinScaleMode.LOCK_ASPECT) uniformScale else freeScaleX,
                    scaleY = if (scaleMode == PinScaleMode.LOCK_ASPECT) uniformScale else freeScaleY
                )
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
                contentDescription = "Edit",
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
                contentDescription = if (locked) "Unlock" else "Lock",
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
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = buildString {
                append(if (scaleMode == PinScaleMode.FREE_SCALE) "FREE" else "LOCK")
                append(" ? ")
                append(if (locked) "PINNED" else "MOVE")
            },
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        )
    }
}
