package com.pixpin.android.feature.pin.runtime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pixpin.android.domain.usecase.ClosedPinRecord
import com.pixpin.android.domain.usecase.PinScaleMode
import com.pixpin.android.presentation.theme.PixPinTheme
import com.pixpin.android.service.PinControlsOverlayContent
import com.pixpin.android.service.PinImageOverlayContent
import kotlin.math.roundToInt

class PinOverlayWindowController(
    private val context: Context,
    private val windowManager: WindowManager,
    lifecycleOwner: LifecycleOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    val id: String,
    val bitmap: Bitmap,
    val imageUri: String,
    val annotationSessionId: String?,
    scaleMode: PinScaleMode,
    defaultShadowEnabled: Boolean,
    initialX: Int,
    initialY: Int,
    private val onEditRequested: () -> Unit,
    private val onCloseRequested: () -> Unit
) {

    private val density = context.resources.displayMetrics.density

    val uiState = PinOverlayUiState(defaultShadowEnabled)
    var imageWidth: Int = 0
        private set
    var imageHeight: Int = 0
        private set
    var controlsWidth: Int = 0
        private set
    var controlsHeight: Int = 0
        private set
    var visible: Boolean = true
        private set

    val imageParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = initialX
        y = initialY
    }

    val controlsParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = imageParams.x
        y = imageParams.y
    }

    val imageView = ComposeView(context).apply {
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
        setContent {
            PixPinTheme {
                PinImageOverlayContent(
                    bitmap = bitmap,
                    scaleMode = scaleMode,
                    uiState = uiState,
                    onImageSizeChanged = { width, height ->
                        handleImageSizeChanged(width, height)
                    },
                    onMoveWindow = { dx, dy ->
                        moveBy(dx, dy)
                    },
                    onToggleControls = {
                        toggleControls()
                    },
                    onClose = {
                        onCloseRequested()
                    }
                )
            }
        }
    }

    val controlsView = ComposeView(context).apply {
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
        setContent {
            PixPinTheme {
                PinControlsOverlayContent(
                    uiState = uiState,
                    onControlsSizeChanged = { width, height ->
                        controlsWidth = width
                        controlsHeight = height
                        updateControlsLayout()
                    },
                    onEdit = {
                        uiState.controlsVisible = false
                        onEditRequested()
                    },
                    onClose = {
                        uiState.controlsVisible = false
                        uiState.cornerControlsVisible = false
                        onCloseRequested()
                    }
                )
            }
        }
    }

    fun attach(): Boolean {
        return try {
            windowManager.addView(imageView, imageParams)
            windowManager.addView(controlsView, controlsParams)
            controlsView.visibility = View.GONE
            true
        } catch (_: Exception) {
            detachWithoutRecycle()
            bitmap.recycle()
            false
        }
    }

    fun remove() {
        detachWithoutRecycle()
        bitmap.recycle()
    }

    fun toClosedPinRecord(): ClosedPinRecord {
        return ClosedPinRecord(
            imageUri = imageUri,
            annotationSessionId = annotationSessionId
        )
    }

    fun snapshot(): PinOverlaySnapshot {
        return PinOverlaySnapshot(
            id = id,
            bitmap = bitmap,
            visible = visible
        )
    }

    fun toggleControls() {
        uiState.controlsVisible = !uiState.controlsVisible
        if (!uiState.controlsVisible) {
            uiState.cornerControlsVisible = false
        }
        updateControlsLayout()
    }

    fun setVisible(visible: Boolean) {
        this.visible = visible
        imageView.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) {
            uiState.controlsVisible = false
            uiState.cornerControlsVisible = false
        }
        updateControlsLayout()
    }

    fun bringToFront() {
        try {
            windowManager.removeViewImmediate(imageView)
            windowManager.addView(imageView, imageParams)
            windowManager.removeViewImmediate(controlsView)
            windowManager.addView(controlsView, controlsParams)
        } catch (_: Exception) {
        }
        visible = true
        imageView.visibility = View.VISIBLE
        updateControlsLayout()
    }

    private fun handleImageSizeChanged(width: Int, height: Int) {
        imageWidth = width
        imageHeight = height
        imageParams.width = width
        imageParams.height = height
        val clamped = clampOverlayPosition(
            currentX = imageParams.x,
            currentY = imageParams.y,
            width = width,
            height = height,
            minVisiblePx = (density * 48f).roundToInt()
        )
        imageParams.x = clamped.first
        imageParams.y = clamped.second
        updateOverlayLayout()
        updateControlsLayout()
    }

    private fun moveBy(dx: Float, dy: Float) {
        val clamped = clampOverlayPosition(
            currentX = imageParams.x + dx.roundToInt(),
            currentY = imageParams.y + dy.roundToInt(),
            width = imageWidth.takeIf { it > 0 } ?: imageView.width,
            height = imageHeight.takeIf { it > 0 } ?: imageView.height,
            minVisiblePx = (density * 48f).roundToInt()
        )
        imageParams.x = clamped.first
        imageParams.y = clamped.second
        updateOverlayLayout()
        updateControlsLayout()
    }

    private fun updateOverlayLayout() {
        try {
            windowManager.updateViewLayout(imageView, imageParams)
        } catch (_: Exception) {
        }
    }

    private fun updateControlsLayout() {
        val shouldShowControls = visible && uiState.controlsVisible
        controlsView.visibility = if (shouldShowControls) View.VISIBLE else View.GONE
        if (!shouldShowControls) return

        controlsParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        controlsParams.height = WindowManager.LayoutParams.WRAP_CONTENT

        val screen = getScreenBounds()
        val spacing = (density * 8f).roundToInt()
        val minVisible = (density * 16f).roundToInt()
        val centeredX = imageParams.x + ((imageWidth - controlsWidth) / 2f).roundToInt()
        val minX = screen.left - controlsWidth + minVisible
        val maxX = screen.right - minVisible
        controlsParams.x = centeredX.coerceIn(minX, maxX)

        val belowY = imageParams.y + imageHeight + spacing
        val aboveY = imageParams.y - controlsHeight - spacing
        controlsParams.y = if (belowY + controlsHeight <= screen.bottom - spacing) {
            belowY
        } else {
            aboveY.coerceAtLeast(screen.top + spacing)
        }

        try {
            windowManager.updateViewLayout(controlsView, controlsParams)
        } catch (_: Exception) {
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

    private fun getScreenBounds(): Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            @Suppress("DEPRECATION")
            Rect().also { rect ->
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRectSize(rect)
            }
        }
    }

    private fun detachWithoutRecycle() {
        try {
            windowManager.removeViewImmediate(imageView)
        } catch (_: Exception) {
            try {
                windowManager.removeView(imageView)
            } catch (_: Exception) {
            }
        }
        try {
            windowManager.removeViewImmediate(controlsView)
        } catch (_: Exception) {
            try {
                windowManager.removeView(controlsView)
            } catch (_: Exception) {
            }
        }
    }
}
