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
import android.view.View
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.pixpin.android.app.AppGraph
import com.pixpin.android.data.repository.PinHistoryRepository
import com.pixpin.android.data.repository.RecentPinRepository
import com.pixpin.android.data.settings.AppSettingsRepository
import com.pixpin.android.domain.usecase.PinHistoryRecord
import com.pixpin.android.domain.usecase.PinHistorySourceType
import com.pixpin.android.domain.usecase.PinScaleMode
import com.pixpin.android.domain.usecase.ClosedPinRecord
import com.pixpin.android.presentation.editor.AnnotationEditorActivity
import com.pixpin.android.presentation.theme.PixPinTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class PinOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    class PinOverlayUiState(defaultShadowEnabled: Boolean) {
        var uniformScale by mutableStateOf(1f)
        var freeScaleX by mutableStateOf(1f)
        var freeScaleY by mutableStateOf(1f)
        var locked by mutableStateOf(false)
        var controlsVisible by mutableStateOf(false)
        var shadowEnabled by mutableStateOf(defaultShadowEnabled)
        var cornerRadius by mutableStateOf(0f)
        var cornerControlsVisible by mutableStateOf(false)
    }

    private data class OverlayEntry(
        val id: String,
        val imageView: ComposeView,
        val imageParams: WindowManager.LayoutParams,
        val controlsView: ComposeView,
        val controlsParams: WindowManager.LayoutParams,
        val bitmap: Bitmap,
        val imageUri: String,
        val annotationSessionId: String?,
        val uiState: PinOverlayUiState,
        var imageWidth: Int,
        var imageHeight: Int,
        var controlsWidth: Int,
        var controlsHeight: Int,
        var visible: Boolean
    )

    data class OverlaySnapshot(
        val id: String,
        val bitmap: Bitmap,
        val visible: Boolean
    )

    private lateinit var windowManager: WindowManager
    private val overlays = LinkedHashMap<String, OverlayEntry>()
    private var managerView: ComposeView? = null
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var pinHistoryRepository: PinHistoryRepository
    private lateinit var recentPinRepository: RecentPinRepository
    private var managerRefreshToken by mutableIntStateOf(0)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsRepository = AppGraph.appSettingsRepository(this)
        pinHistoryRepository = AppGraph.pinHistoryRepository(this)
        recentPinRepository = AppGraph.recentPinRepository(this)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESTORE_LAST_CLOSED) {
            restoreLastClosedOverlay()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_OPEN_MANAGER) {
            openManagerOverlay()
            return START_NOT_STICKY
        }
        val imageUriString = intent?.getStringExtra(EXTRA_IMAGE_URI)
        val annotationSessionId = intent?.getStringExtra(EXTRA_ANNOTATION_SESSION_ID)
        val historySourceType = PinHistorySourceType.fromValue(intent?.getStringExtra(EXTRA_HISTORY_SOURCE))
        if (!imageUriString.isNullOrBlank()) {
            val imageUri = android.net.Uri.parse(imageUriString)
            serviceScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(imageUri)?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    }
                    if (bitmap != null) {
                        showPinOverlay(
                            bitmap = bitmap,
                            imageUriString = imageUriString,
                            annotationSessionId = annotationSessionId,
                            scaleMode = settingsRepository.getPinScaleMode(),
                            historySourceType = historySourceType
                        )
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
        val record = recentPinRepository.popMostRecent() ?: return
        serviceScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(android.net.Uri.parse(record.imageUri))?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
                if (bitmap != null) {
                    showPinOverlay(
                        bitmap = bitmap,
                        imageUriString = record.imageUri,
                        annotationSessionId = record.annotationSessionId,
                        scaleMode = settingsRepository.getPinScaleMode(),
                        historySourceType = PinHistorySourceType.RESTORED_PIN
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
        scaleMode: PinScaleMode,
        historySourceType: PinHistorySourceType
    ) {
        val overlayId = UUID.randomUUID().toString()
        val uiState = PinOverlayUiState(settingsRepository.isPinShadowEnabledByDefault())
        val imageParams = WindowManager.LayoutParams(
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
        val controlsParams = WindowManager.LayoutParams(
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
            x = imageParams.x
            y = imageParams.y
        }

        val imageView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PinOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PinOverlayService)
            setContent {
                PixPinTheme {
                    PinImageOverlayContent(
                        bitmap = bitmap,
                        scaleMode = scaleMode,
                        uiState = uiState,
                        onImageSizeChanged = { width, height ->
                            val entry = overlays[overlayId] ?: return@PinImageOverlayContent
                            entry.imageWidth = width
                            entry.imageHeight = height
                            entry.imageParams.width = width
                            entry.imageParams.height = height
                            val clamped = clampOverlayPosition(
                                currentX = entry.imageParams.x,
                                currentY = entry.imageParams.y,
                                width = width,
                                height = height,
                                minVisiblePx = (resources.displayMetrics.density * 48f).roundToInt()
                            )
                            entry.imageParams.x = clamped.first
                            entry.imageParams.y = clamped.second
                            updateOverlayLayout(overlayId)
                            updateControlsLayout(overlayId)
                        },
                        onMoveWindow = { dx, dy ->
                            val entry = overlays[overlayId] ?: return@PinImageOverlayContent
                            val clamped = clampOverlayPosition(
                                currentX = entry.imageParams.x + dx.roundToInt(),
                                currentY = entry.imageParams.y + dy.roundToInt(),
                                width = entry.imageWidth.takeIf { it > 0 } ?: this.width,
                                height = entry.imageHeight.takeIf { it > 0 } ?: this.height,
                                minVisiblePx = (resources.displayMetrics.density * 48f).roundToInt()
                            )
                            entry.imageParams.x = clamped.first
                            entry.imageParams.y = clamped.second
                            updateOverlayLayout(overlayId)
                            updateControlsLayout(overlayId)
                        },
                        onToggleControls = {
                            val entry = overlays[overlayId] ?: return@PinImageOverlayContent
                            entry.uiState.controlsVisible = !entry.uiState.controlsVisible
                            if (!entry.uiState.controlsVisible) {
                                entry.uiState.cornerControlsVisible = false
                            }
                            updateControlsLayout(overlayId)
                        },
                        onClose = {
                            recentPinRepository.push(
                                ClosedPinRecord(
                                    imageUri = imageUriString,
                                    annotationSessionId = annotationSessionId
                                )
                            )
                            removeOverlay(overlayId)
                        }
                    )
                }
            }
        }

        val controlsView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PinOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PinOverlayService)
            setContent {
                PixPinTheme {
                    PinControlsOverlayContent(
                        uiState = uiState,
                        onControlsSizeChanged = { width, height ->
                            val entry = overlays[overlayId] ?: return@PinControlsOverlayContent
                            entry.controlsWidth = width
                            entry.controlsHeight = height
                            updateControlsLayout(overlayId)
                        },
                        onEdit = {
                            uiState.controlsVisible = false
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
                        },
                        onClose = {
                            uiState.controlsVisible = false
                            uiState.cornerControlsVisible = false
                            recentPinRepository.push(
                                ClosedPinRecord(
                                    imageUri = imageUriString,
                                    annotationSessionId = annotationSessionId
                                )
                            )
                            removeOverlay(overlayId)
                        }
                    )
                }
            }
        }

        val entry = OverlayEntry(
            id = overlayId,
            imageView = imageView,
            imageParams = imageParams,
            controlsView = controlsView,
            controlsParams = controlsParams,
            bitmap = bitmap,
            imageUri = imageUriString,
            annotationSessionId = annotationSessionId,
            uiState = uiState,
            imageWidth = 0,
            imageHeight = 0,
            controlsWidth = 0,
            controlsHeight = 0,
            visible = true
        )
        overlays[overlayId] = entry

        try {
            windowManager.addView(imageView, imageParams)
            windowManager.addView(controlsView, controlsParams)
            controlsView.visibility = View.GONE
            recordPinHistoryIfNeeded(
                imageUri = imageUriString,
                annotationSessionId = annotationSessionId,
                sourceType = historySourceType
            )
        } catch (e: Exception) {
            overlays.remove(overlayId)
            try {
                windowManager.removeView(imageView)
            } catch (_: Exception) {
            }
            try {
                windowManager.removeView(controlsView)
            } catch (_: Exception) {
            }
            bitmap.recycle()
            e.printStackTrace()
        }
        notifyManagerChanged()
    }

    private fun updateOverlayLayout(overlayId: String) {
        val entry = overlays[overlayId] ?: return
        try {
            windowManager.updateViewLayout(entry.imageView, entry.imageParams)
        } catch (_: Exception) {
        }
    }

    private fun updateControlsLayout(overlayId: String) {
        val entry = overlays[overlayId] ?: return
        val visible = entry.visible && entry.uiState.controlsVisible
        entry.controlsView.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) return
        entry.controlsParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        entry.controlsParams.height = WindowManager.LayoutParams.WRAP_CONTENT

        val screen = getScreenBounds()
        val spacing = (resources.displayMetrics.density * 8f).roundToInt()
        val minVisible = (resources.displayMetrics.density * 16f).roundToInt()
        val imageLeft = entry.imageParams.x
        val imageTop = entry.imageParams.y
        val imageWidth = entry.imageWidth.takeIf { it > 0 } ?: 0
        val imageHeight = entry.imageHeight.takeIf { it > 0 } ?: 0
        val controlsWidth = entry.controlsWidth.takeIf { it > 0 } ?: 220
        val controlsHeight = entry.controlsHeight.takeIf { it > 0 } ?: 56

        val centeredX = imageLeft + ((imageWidth - controlsWidth) / 2f).roundToInt()
        val minX = screen.left - controlsWidth + minVisible
        val maxX = screen.right - minVisible
        entry.controlsParams.x = centeredX.coerceIn(minX, maxX)

        val belowY = imageTop + imageHeight + spacing
        val aboveY = imageTop - controlsHeight - spacing
        entry.controlsParams.y = if (belowY + controlsHeight <= screen.bottom - spacing) {
            belowY
        } else {
            aboveY.coerceAtLeast(screen.top + spacing)
        }

        try {
            windowManager.updateViewLayout(entry.controlsView, entry.controlsParams)
        } catch (_: Exception) {
        }
    }

    private fun removeOverlay(overlayId: String) {
        val entry = overlays.remove(overlayId) ?: return
        try {
            windowManager.removeViewImmediate(entry.imageView)
        } catch (_: Exception) {
            try {
                windowManager.removeView(entry.imageView)
            } catch (_: Exception) {
            }
        }
        try {
            windowManager.removeViewImmediate(entry.controlsView)
        } catch (_: Exception) {
            try {
                windowManager.removeView(entry.controlsView)
            } catch (_: Exception) {
            }
        }
        entry.bitmap.recycle()
        notifyManagerChanged()
        if (overlays.isEmpty() && managerView == null) {
            stopSelf()
        }
    }

    private fun rememberClosedOverlay(entry: OverlayEntry) {
        recentPinRepository.push(
            ClosedPinRecord(
                imageUri = entry.imageUri,
                annotationSessionId = entry.annotationSessionId
            )
        )
    }

    private fun recordPinHistoryIfNeeded(
        imageUri: String,
        annotationSessionId: String?,
        sourceType: PinHistorySourceType
    ) {
        if (!settingsRepository.getPinHistorySettings().enabled) return
        pinHistoryRepository.save(
            imageUri = imageUri,
            annotationSessionId = annotationSessionId,
            sourceType = sourceType
        )
        val pinHistorySettings = settingsRepository.getPinHistorySettings()
        pinHistoryRepository.prune(
            maxCount = pinHistorySettings.maxCount,
            maxDays = pinHistorySettings.retainDays
        )
        notifyManagerChanged()
    }

    private fun setOverlayVisible(overlayId: String, visible: Boolean) {
        val entry = overlays[overlayId] ?: return
        entry.visible = visible
        entry.imageView.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) {
            entry.uiState.controlsVisible = false
            entry.uiState.cornerControlsVisible = false
        }
        updateControlsLayout(overlayId)
        notifyManagerChanged()
    }

    private fun setAllOverlaysVisible(visible: Boolean) {
        overlays.keys.toList().forEach { setOverlayVisible(it, visible) }
    }

    private fun closeOverlayFromManager(overlayId: String) {
        val entry = overlays[overlayId] ?: return
        rememberClosedOverlay(entry)
        removeOverlay(overlayId)
    }

    private fun closeAllOverlays() {
        overlays.keys.toList().forEach { overlayId ->
            val entry = overlays[overlayId] ?: return@forEach
            rememberClosedOverlay(entry)
            removeOverlay(overlayId)
        }
        closeManagerOverlay()
    }

    private fun bringOverlayToFront(overlayId: String) {
        val entry = overlays[overlayId] ?: return
        try {
            windowManager.removeViewImmediate(entry.imageView)
            windowManager.addView(entry.imageView, entry.imageParams)
            windowManager.removeViewImmediate(entry.controlsView)
            windowManager.addView(entry.controlsView, entry.controlsParams)
        } catch (_: Exception) {
        }
        entry.visible = true
        entry.imageView.visibility = View.VISIBLE
        overlays.remove(overlayId)
        overlays[overlayId] = entry
        updateControlsLayout(overlayId)
        notifyManagerChanged()
    }

    private fun openManagerOverlay() {
        if (managerView != null) {
            notifyManagerChanged()
            return
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 140
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PinOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PinOverlayService)
            setContent {
                val refreshToken = managerRefreshToken
                val snapshots = overlays.values.map {
                    OverlaySnapshot(
                        id = it.id,
                        bitmap = it.bitmap,
                        visible = it.visible
                    )
                }
                PixPinTheme {
                    PinManagerContent(
                        refreshToken = refreshToken,
                        overlays = snapshots,
                        onShowAll = { setAllOverlaysVisible(true) },
                        onHideAll = { setAllOverlaysVisible(false) },
                        onCloseAll = { closeAllOverlays() },
                        onCloseManager = { closeManagerOverlay() },
                        onToggleVisible = { id ->
                            val current = overlays[id]?.visible ?: return@PinManagerContent
                            setOverlayVisible(id, !current)
                        },
                        onCloseOne = { id -> closeOverlayFromManager(id) },
                        onFocusOne = { id -> bringOverlayToFront(id) }
                    )
                }
            }
        }
        managerView = view
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            managerView = null
            e.printStackTrace()
        }
    }

    private fun closeManagerOverlay() {
        val view = managerView ?: return
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
        }
        managerView = null
        if (overlays.isEmpty()) {
            stopSelf()
        }
    }

    private fun notifyManagerChanged() {
        managerRefreshToken++
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
        managerView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (_: Exception) {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {
                }
            }
        }
        managerView = null
        overlays.values.toList().forEach { entry ->
            try {
                windowManager.removeViewImmediate(entry.imageView)
            } catch (_: Exception) {
                try {
                    windowManager.removeView(entry.imageView)
                } catch (_: Exception) {
                }
            }
            try {
                windowManager.removeViewImmediate(entry.controlsView)
            } catch (_: Exception) {
                try {
                    windowManager.removeView(entry.controlsView)
                } catch (_: Exception) {
                }
            }
            entry.bitmap.recycle()
        }
        overlays.clear()
    }

    companion object {
        const val ACTION_RESTORE_LAST_CLOSED = "com.pixpin.android.action.RESTORE_LAST_CLOSED"
        const val ACTION_OPEN_MANAGER = "com.pixpin.android.action.OPEN_MANAGER"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_ANNOTATION_SESSION_ID = "extra_annotation_session_id"
        const val EXTRA_HISTORY_SOURCE = "extra_history_source"
    }
}

@Composable
private fun PinImageOverlayContent(
    bitmap: Bitmap,
    scaleMode: PinScaleMode,
    uiState: PinOverlayService.PinOverlayUiState,
    onImageSizeChanged: (Int, Int) -> Unit,
    onMoveWindow: (Float, Float) -> Unit,
    onToggleControls: () -> Unit,
    onClose: () -> Unit
) {
    val aspect = if (bitmap.height == 0) 1f else bitmap.width.toFloat() / bitmap.height.toFloat()
    val baseWidth = 260.dp
    val baseHeight = (baseWidth / aspect).coerceAtLeast(90.dp)

    fun resizeFreeScale(deltaX: Float, deltaY: Float) {
        val currentWidth = if (scaleMode == PinScaleMode.LOCK_ASPECT) {
            baseWidth.value * uiState.uniformScale
        } else {
            baseWidth.value * uiState.freeScaleX
        }
        val currentHeight = if (scaleMode == PinScaleMode.LOCK_ASPECT) {
            baseHeight.value * uiState.uniformScale
        } else {
            baseHeight.value * uiState.freeScaleY
        }
        val nextWidth = max(80f, currentWidth + deltaX)
        val nextHeight = max(48f, currentHeight + deltaY)
        uiState.freeScaleX = (nextWidth / baseWidth.value).coerceIn(0.2f, 6f)
        uiState.freeScaleY = (nextHeight / baseHeight.value).coerceIn(0.2f, 6f)
    }

    val displayWidth: Dp
    val displayHeight: Dp
    if (scaleMode == PinScaleMode.LOCK_ASPECT) {
        displayWidth = (baseWidth * uiState.uniformScale).coerceAtLeast(80.dp)
        displayHeight = (baseHeight * uiState.uniformScale).coerceAtLeast(48.dp)
    } else {
        displayWidth = (baseWidth * uiState.freeScaleX).coerceAtLeast(80.dp)
        displayHeight = (baseHeight * uiState.freeScaleY).coerceAtLeast(48.dp)
    }

    val renderPadding = 18.dp
    val imageLayerWidth = displayWidth + renderPadding * 2
    val imageLayerHeight = displayHeight + renderPadding * 2
    Box(
        modifier = Modifier
            .requiredSize(imageLayerWidth, imageLayerHeight)
            .onSizeChanged { size ->
                onImageSizeChanged(size.width, size.height)
            }
    ) {
        AndroidView(
            factory = { context ->
                PinnedImageRenderView(context).apply {
                    this.bitmap = bitmap
                    this.shadowEnabled = uiState.shadowEnabled
                    this.cornerRadiusPx = uiState.cornerRadius * context.resources.displayMetrics.density
                    this.onMoveWindow = { dx, dy ->
                        if (!uiState.locked) {
                            onMoveWindow(dx, dy)
                        }
                    }
                    this.onScaleBy = { scaleFactor ->
                        if (!uiState.locked && scaleFactor != 1f) {
                            if (scaleMode == PinScaleMode.LOCK_ASPECT) {
                                uiState.uniformScale = (uiState.uniformScale * scaleFactor).coerceIn(0.2f, 6f)
                            } else {
                                uiState.freeScaleX = (uiState.freeScaleX * scaleFactor).coerceIn(0.2f, 6f)
                                uiState.freeScaleY = (uiState.freeScaleY * scaleFactor).coerceIn(0.2f, 6f)
                            }
                        }
                    }
                    this.onSingleTap = {
                        onToggleControls()
                    }
                    this.onDoubleTap = {
                        onClose()
                    }
                }
            },
            update = { view ->
                view.bitmap = bitmap
                view.shadowEnabled = uiState.shadowEnabled
                view.cornerRadiusPx = uiState.cornerRadius * view.resources.displayMetrics.density
            },
            modifier = Modifier
                .align(Alignment.Center)
                .requiredSize(imageLayerWidth, imageLayerHeight)
        )

        if (scaleMode == PinScaleMode.FREE_SCALE && !uiState.locked) {
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
private fun PinControlsOverlayContent(
    uiState: PinOverlayService.PinOverlayUiState,
    onControlsSizeChanged: (Int, Int) -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = uiState.controlsVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .onSizeChanged { size ->
                    onControlsSizeChanged(size.width, size.height)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.76f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(30.dp),
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
                        onClick = { uiState.locked = !uiState.locked },
                        modifier = Modifier.size(30.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (uiState.locked) "解锁" else "锁定",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = { uiState.shadowEnabled = !uiState.shadowEnabled },
                        modifier = Modifier.size(30.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (uiState.shadowEnabled) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Text(
                            text = "影",
                            color = if (uiState.shadowEnabled) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    FilledIconButton(
                        onClick = { uiState.cornerControlsVisible = !uiState.cornerControlsVisible },
                        modifier = Modifier.size(30.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (uiState.cornerControlsVisible) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "圆角设置",
                            tint = if (uiState.cornerControlsVisible) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onClose,
                        modifier = Modifier.size(30.dp),
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
                }
            }

            AnimatedVisibility(visible = uiState.cornerControlsVisible) {
                Surface(
                    color = Color.Black.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "圆角：${uiState.cornerRadius.roundToInt()}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Slider(
                            value = uiState.cornerRadius,
                            onValueChange = { uiState.cornerRadius = it },
                            valueRange = 0f..48f,
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
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

@Composable
private fun PinManagerContent(
    refreshToken: Int,
    overlays: List<PinOverlayService.OverlaySnapshot>,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onCloseAll: () -> Unit,
    onCloseManager: () -> Unit,
    onToggleVisible: (String) -> Unit,
    onCloseOne: (String) -> Unit,
    onFocusOne: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        modifier = Modifier.width(320.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("贴图管理", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "当前贴图 ${overlays.size} 张",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledIconButton(
                    onClick = onCloseManager,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭管理面板")
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onShowAll, modifier = Modifier.weight(1f)) {
                    Text("全部显示")
                }
                OutlinedButton(onClick = onHideAll, modifier = Modifier.weight(1f)) {
                    Text("全部隐藏")
                }
                OutlinedButton(onClick = onCloseAll, modifier = Modifier.weight(1f)) {
                    Text("全部关闭")
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            if (overlays.isEmpty()) {
                Text(
                    text = "当前没有可管理的贴图。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(overlays, key = { it.id + refreshToken }) { overlay ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    bitmap = overlay.bitmap.asImageBitmap(),
                                    contentDescription = "贴图预览",
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "贴图 ${overlay.id.takeLast(4)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (overlay.visible) "当前可见" else "当前已隐藏",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(onClick = { onFocusOne(overlay.id) }) {
                                    Text("置顶")
                                }
                                OutlinedButton(onClick = { onToggleVisible(overlay.id) }) {
                                    Text(if (overlay.visible) "隐藏" else "显示")
                                }
                                FilledIconButton(
                                    onClick = { onCloseOne(overlay.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "关闭贴图")
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}
