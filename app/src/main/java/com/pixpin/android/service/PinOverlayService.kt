package com.pixpin.android.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import kotlin.math.roundToInt

class PinOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
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
        if (!imageUriString.isNullOrBlank()) {
            val imageUri = android.net.Uri.parse(imageUriString)
            serviceScope.launch {
                try {
                    val bitmap = contentResolver.openInputStream(imageUri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                    if (bitmap != null) {
                        showPinOverlay(bitmap, imageUriString, captureFlowSettings.getPinScaleMode())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showPinOverlay(bitmap: Bitmap, imageUriString: String, scaleMode: PinScaleMode) {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

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
            x = 120
            y = 220
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PinOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PinOverlayService)

            setContent {
                PixPinTheme {
                    PinnedImageContent(
                        bitmap = bitmap,
                        scaleMode = scaleMode,
                        onMoveWindow = { dx, dy ->
                            params.x += dx.roundToInt()
                            params.y += dy.roundToInt()
                            try {
                                windowManager.updateViewLayout(this, params)
                            } catch (_: Exception) {
                            }
                        },
                        onClose = { stopSelf() },
                        onEdit = {
                            val editorIntent = Intent(this@PinOverlayService, AnnotationEditorActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra(AnnotationEditorActivity.EXTRA_IMAGE_URI, imageUriString)
                            }
                            startActivity(editorIntent)
                            stopSelf()
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        overlayView = null
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
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
                .pointerInput(scaleMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Pan always moves pinned image window.
                        if (pan.x != 0f || pan.y != 0f) {
                            onMoveWindow(pan.x, pan.y)
                        }

                        if (zoom != 1f) {
                            if (scaleMode == PinScaleMode.LOCK_ASPECT) {
                                uniformScale = (uniformScale * zoom).coerceIn(0.2f, 6f)
                            } else {
                                // Free mode still supports pinch scaling on both axes.
                                freeScaleX = (freeScaleX * zoom).coerceIn(0.2f, 6f)
                                freeScaleY = (freeScaleY * zoom).coerceIn(0.2f, 6f)
                            }
                        }
                    }
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
            text = if (scaleMode == PinScaleMode.FREE_SCALE) "FREE" else "LOCK",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        )
    }
}
