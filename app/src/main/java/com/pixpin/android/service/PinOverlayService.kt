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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
        val bitmap: Bitmap,
        val imageUri: String,
        val annotationSessionId: String?,
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
    private lateinit var captureFlowSettings: CaptureFlowSettings
    private var managerRefreshToken by mutableIntStateOf(0)

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
        if (intent?.action == ACTION_OPEN_MANAGER) {
            openManagerOverlay()
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
                        defaultShadowEnabled = captureFlowSettings.isPinShadowEnabledByDefault(),
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
            bitmap = bitmap,
            imageUri = imageUriString,
            annotationSessionId = annotationSessionId,
            visible = true
        )
        overlays[overlayId] = entry

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            overlays.remove(overlayId)
            bitmap.recycle()
            e.printStackTrace()
        }
        notifyManagerChanged()
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
        notifyManagerChanged()
        if (overlays.isEmpty() && managerView == null) {
            stopSelf()
        }
    }

    private fun rememberClosedOverlay(entry: OverlayEntry) {
        RecentPinStore.push(
            this,
            ClosedPinRecord(
                imageUri = entry.imageUri,
                annotationSessionId = entry.annotationSessionId
            )
        )
    }

    private fun setOverlayVisible(overlayId: String, visible: Boolean) {
        val entry = overlays[overlayId] ?: return
        entry.visible = visible
        entry.view.visibility = if (visible) View.VISIBLE else View.GONE
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
            windowManager.removeView(entry.view)
            windowManager.addView(entry.view, entry.params)
        } catch (_: Exception) {
        }
        entry.visible = true
        entry.view.visibility = View.VISIBLE
        overlays.remove(overlayId)
        overlays[overlayId] = entry
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
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        managerView = null
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
        const val ACTION_OPEN_MANAGER = "com.pixpin.android.action.OPEN_MANAGER"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_ANNOTATION_SESSION_ID = "extra_annotation_session_id"
    }
}

@Composable
private fun PinnedImageContent(
    bitmap: Bitmap,
    scaleMode: PinScaleMode,
    defaultShadowEnabled: Boolean,
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
    var controlsVisible by remember { mutableStateOf(false) }
    var shadowEnabled by remember { mutableStateOf(defaultShadowEnabled) }
    var cornerRadius by remember { mutableStateOf(0f) }
    var cornerControlsVisible by remember { mutableStateOf(false) }

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

    val controlsPreferOutside = displayWidth >= 180.dp && displayHeight >= 120.dp
    val topControlsSpace = if (controlsPreferOutside) 44.dp else 0.dp
    val bottomControlsSpace = if (controlsPreferOutside) 108.dp else 0.dp
    val containerWidth = displayWidth
    val containerHeight = displayHeight + topControlsSpace + bottomControlsSpace
    val imageShape = RoundedCornerShape(cornerRadius.dp)
    val glowShape = RoundedCornerShape((cornerRadius + 4f).dp)
    val glowBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF8DE7FF).copy(alpha = 0.95f),
            Color(0xFF54B5FF).copy(alpha = 0.9f),
            Color(0xFF377BFF).copy(alpha = 0.95f)
        )
    )

    Box(
        modifier = Modifier
            .requiredSize(containerWidth, containerHeight)
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
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = {
                        onClose()
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topControlsSpace)
                .requiredSize(displayWidth, displayHeight)
        ) {
            if (shadowEnabled) {
                Box(
                    modifier = Modifier
                        .requiredSize(displayWidth, displayHeight)
                        .shadow(
                            elevation = 28.dp,
                            shape = glowShape,
                            ambientColor = Color(0xFF3A93FF).copy(alpha = 0.65f),
                            spotColor = Color(0xFF70D6FF).copy(alpha = 0.72f)
                        )
                        .border(
                            width = 2.dp,
                            brush = glowBrush,
                            shape = glowShape
                        )
                )
            }
            Box(
                modifier = Modifier
                    .requiredSize(displayWidth, displayHeight)
                    .graphicsLayer {
                        shape = imageShape
                        clip = true
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "贴图",
                    modifier = Modifier.requiredSize(displayWidth, displayHeight)
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.requiredSize(containerWidth, containerHeight)) {
                val topButtonsModifier = if (controlsPreferOutside) {
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                } else {
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                }

                Row(
                    modifier = topButtonsModifier,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            controlsVisible = false
                            onEdit()
                        },
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
                        onClick = { locked = !locked },
                        modifier = Modifier.size(30.dp),
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
                        onClick = { shadowEnabled = !shadowEnabled },
                        modifier = Modifier.size(30.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (shadowEnabled) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Text(
                            text = "影",
                            color = if (shadowEnabled) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    FilledIconButton(
                        onClick = { cornerControlsVisible = !cornerControlsVisible },
                        modifier = Modifier.size(30.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (cornerControlsVisible) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "圆角设置",
                            tint = if (cornerControlsVisible) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            controlsVisible = false
                            cornerControlsVisible = false
                            onClose()
                        },
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

                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = if (controlsPreferOutside) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = buildString {
                                append(if (scaleMode == PinScaleMode.FREE_SCALE) "自由缩放" else "等比缩放")
                                append(" | ")
                                append(if (locked) "已锁定" else "可移动")
                                append(" | ")
                                append(if (shadowEnabled) "阴影已开" else "阴影已关")
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                        AnimatedVisibility(visible = cornerControlsVisible) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "圆角：${cornerRadius.roundToInt()}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Slider(
                                    value = cornerRadius,
                                    onValueChange = { cornerRadius = it },
                                    valueRange = 0f..48f,
                                    modifier = Modifier.width(displayWidth.coerceAtLeast(180.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

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
