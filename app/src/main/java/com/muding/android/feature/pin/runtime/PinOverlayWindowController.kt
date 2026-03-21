package com.muding.android.feature.pin.runtime

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.animation.doOnEnd
import com.muding.android.domain.usecase.ClosedPinRecord
import com.muding.android.domain.usecase.PinScaleMode
import com.muding.android.service.PinnedImageRenderView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class PinOverlayWindowController(
    private val context: Context,
    private val windowManager: WindowManager,
    val id: String,
    val bitmap: Bitmap,
    val imageUri: String,
    val annotationSessionId: String?,
    scaleMode: PinScaleMode,
    defaultShadowEnabled: Boolean,
    defaultCornerRadiusDp: Float,
    initialContentWidthPx: Int?,
    initialContentHeightPx: Int?,
    initialX: Int,
    initialY: Int,
    private val onFocusRequested: () -> Unit,
    private val onEditRequested: () -> Unit,
    private val onCloseRequested: () -> Unit
) {

    private val density = context.resources.displayMetrics.density
    private val scaleMode = scaleMode
    private val requestedInitialContentWidthPx = initialContentWidthPx
    private val requestedInitialContentHeightPx = initialContentHeightPx
    private val baseContentSize = calculateBaseContentSize()
    private val runtimeState = PinOverlayRuntimeState(
        scaleMode = scaleMode,
        shadowEnabled = defaultShadowEnabled,
        cornerRadiusDp = defaultCornerRadiusDp
    )

    private var attached = false
    private var overlayWidth: Int = 0
        private set
    private var overlayHeight: Int = 0
        private set
    private var layoutUpdateScheduled = false
    private var shakeAnimator: ValueAnimator? = null

    val overlayParams = WindowManager.LayoutParams(
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

    val overlayView = PinnedImageRenderView(context).apply {
        this.bitmap = this@PinOverlayWindowController.bitmap
        this.scaleMode = scaleMode
        this.shadowEnabled = runtimeState.shadowEnabled
        this.cornerRadiusPx = runtimeState.cornerRadiusDp * density
        onFocusRequested = {
            onFocusRequested()
        }
        onMoveWindow = { dx, dy ->
            moveBy(dx, dy)
        }
        onScaleGesture = { scaleFactorX, scaleFactorY, focusX, focusY ->
            applyScaleGesture(scaleFactorX, scaleFactorY, focusX, focusY)
        }
        onDragResize = { mode, dx, dy ->
            applyDragResize(mode, dx, dy)
        }
        onLongPress = {
            onEditRequested()
        }
        onDoubleTap = {
            onCloseRequested()
        }
    }

    private val layoutUpdateRunnable = Runnable {
        layoutUpdateScheduled = false
        if (!attached || !overlayView.isAttachedToWindow) {
            return@Runnable
        }
        try {
            windowManager.updateViewLayout(overlayView, overlayParams)
        } catch (_: Exception) {
        }
    }

    var visible: Boolean = true
        private set

    init {
        applyOverlaySize(calculateOverlaySize())
    }

    fun attach(): Boolean {
        return try {
            windowManager.addView(overlayView, overlayParams)
            attached = true
            true
        } catch (_: Exception) {
            detachWithoutRecycle()
            bitmap.recycle()
            false
        }
    }

    fun remove() {
        attached = false
        shakeAnimator?.cancel()
        shakeAnimator = null
        overlayView.removeCallbacks(layoutUpdateRunnable)
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

    fun setVisible(visible: Boolean) {
        this.visible = visible
        overlayView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun bringToFront() {
        try {
            windowManager.removeViewImmediate(overlayView)
            windowManager.addView(overlayView, overlayParams)
            attached = true
        } catch (_: Exception) {
        }
        visible = true
        overlayView.visibility = View.VISIBLE
    }

    fun matches(imageUri: String, annotationSessionId: String?): Boolean {
        return this.imageUri == imageUri && this.annotationSessionId == annotationSessionId
    }

    fun shakeToHintRestore() {
        if (!attached || !overlayView.isAttachedToWindow) {
            return
        }
        shakeAnimator?.cancel()
        val baseX = overlayParams.x
        val densityOffset = (12f * density).roundToInt().coerceAtLeast(8)
        shakeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320L
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val offset = when {
                    progress < 0.16f -> densityOffset
                    progress < 0.33f -> -densityOffset
                    progress < 0.5f -> (densityOffset * 0.75f).roundToInt()
                    progress < 0.66f -> -(densityOffset * 0.75f).roundToInt()
                    progress < 0.83f -> (densityOffset * 0.4f).roundToInt()
                    else -> 0
                }
                overlayParams.x = baseX + offset
                try {
                    windowManager.updateViewLayout(overlayView, overlayParams)
                } catch (_: Exception) {
                }
            }
            doOnEnd {
                overlayParams.x = baseX
                try {
                    windowManager.updateViewLayout(overlayView, overlayParams)
                } catch (_: Exception) {
                }
                shakeAnimator = null
            }
            start()
        }
    }

    private fun moveBy(dx: Float, dy: Float) {
        if (dx == 0f && dy == 0f) {
            return
        }
        val clamped = clampOverlayPosition(
            currentX = overlayParams.x + dx.roundToInt(),
            currentY = overlayParams.y + dy.roundToInt(),
            width = overlayWidth.takeIf { it > 0 } ?: overlayParams.width,
            height = overlayHeight.takeIf { it > 0 } ?: overlayParams.height
        )
        overlayParams.x = clamped.first
        overlayParams.y = clamped.second
        scheduleOverlayLayoutUpdate()
    }

    private fun applyScaleGesture(
        scaleFactorX: Float,
        scaleFactorY: Float,
        focusX: Float,
        focusY: Float
    ) {
        if (scaleFactorX == 1f && scaleFactorY == 1f) {
            return
        }
        val oldWidth = overlayWidth.takeIf { it > 0 } ?: overlayParams.width
        val oldHeight = overlayHeight.takeIf { it > 0 } ?: overlayParams.height
        runtimeState.applyScaleGesture(scaleFactorX, scaleFactorY)
        val nextSize = calculateOverlaySize()
        repositionForFocus(oldWidth, oldHeight, nextSize.viewWidth, nextSize.viewHeight, focusX, focusY)
        applyOverlaySize(nextSize)
        scheduleOverlayLayoutUpdate()
    }

    private fun applyDragResize(
        mode: PinnedImageRenderView.DragResizeMode,
        dx: Float,
        dy: Float
    ) {
        val currentContentWidth = (baseContentSize.first * runtimeState.currentScaleX()).roundToInt()
        val currentContentHeight = (baseContentSize.second * runtimeState.currentScaleY()).roundToInt()
        when (scaleMode) {
            PinScaleMode.LOCK_ASPECT -> {
                val dominantDelta = if (abs(dx) >= abs(dy)) dx else dy
                val baseReference = max(currentContentWidth, currentContentHeight).coerceAtLeast(1)
                val nextScale = runtimeState.currentScaleX() * (1f + dominantDelta / baseReference)
                runtimeState.setUniformScale(nextScale)
            }

            PinScaleMode.FREE_SCALE -> {
                val targetWidth = when (mode) {
                    PinnedImageRenderView.DragResizeMode.RIGHT,
                    PinnedImageRenderView.DragResizeMode.BOTTOM_RIGHT -> currentContentWidth + dx.roundToInt()
                    PinnedImageRenderView.DragResizeMode.BOTTOM -> currentContentWidth
                }
                val targetHeight = when (mode) {
                    PinnedImageRenderView.DragResizeMode.BOTTOM,
                    PinnedImageRenderView.DragResizeMode.BOTTOM_RIGHT -> currentContentHeight + dy.roundToInt()
                    PinnedImageRenderView.DragResizeMode.RIGHT -> currentContentHeight
                }
                runtimeState.setFreeScale(
                    scaleX = targetWidth / baseContentSize.first.toFloat(),
                    scaleY = targetHeight / baseContentSize.second.toFloat()
                )
            }
        }
        applyOverlaySize(calculateOverlaySize())
        scheduleOverlayLayoutUpdate()
    }

    private fun repositionForFocus(
        oldWidth: Int,
        oldHeight: Int,
        newWidth: Int,
        newHeight: Int,
        focusX: Float,
        focusY: Float
    ) {
        val safeOldWidth = oldWidth.coerceAtLeast(1)
        val safeOldHeight = oldHeight.coerceAtLeast(1)
        val ratioX = (focusX / safeOldWidth.toFloat()).coerceIn(0f, 1f)
        val ratioY = (focusY / safeOldHeight.toFloat()).coerceIn(0f, 1f)
        val absoluteFocusX = overlayParams.x + focusX
        val absoluteFocusY = overlayParams.y + focusY
        overlayParams.x = (absoluteFocusX - newWidth * ratioX).roundToInt()
        overlayParams.y = (absoluteFocusY - newHeight * ratioY).roundToInt()
    }

    private fun applyOverlaySize(size: OverlaySize) {
        overlayWidth = size.viewWidth
        overlayHeight = size.viewHeight
        overlayParams.width = size.viewWidth
        overlayParams.height = size.viewHeight
        overlayView.shadowEnabled = runtimeState.shadowEnabled
        overlayView.cornerRadiusPx = runtimeState.cornerRadiusDp * density
        val clamped = clampOverlayPosition(
            currentX = overlayParams.x,
            currentY = overlayParams.y,
            width = size.viewWidth,
            height = size.viewHeight
        )
        overlayParams.x = clamped.first
        overlayParams.y = clamped.second
    }

    private fun scheduleOverlayLayoutUpdate() {
        if (!attached || !overlayView.isAttachedToWindow || layoutUpdateScheduled) {
            return
        }
        layoutUpdateScheduled = true
        overlayView.postOnAnimation(layoutUpdateRunnable)
    }

    private fun calculateOverlaySize(): OverlaySize {
        val padding = if (runtimeState.shadowEnabled) (18f * density).roundToInt() else 0
        val contentWidth = (baseContentSize.first * runtimeState.currentScaleX()).roundToInt()
            .coerceAtLeast((80f * density).roundToInt())
        val contentHeight = (baseContentSize.second * runtimeState.currentScaleY()).roundToInt()
            .coerceAtLeast((48f * density).roundToInt())
        return OverlaySize(
            contentWidth = contentWidth,
            contentHeight = contentHeight,
            viewWidth = contentWidth + padding * 2,
            viewHeight = contentHeight + padding * 2
        )
    }

    private fun calculateBaseContentSize(): Pair<Int, Int> {
        val requestedWidth = requestedInitialContentWidthPx?.takeIf { it > 0 }
        val requestedHeight = requestedInitialContentHeightPx?.takeIf { it > 0 }
        val screen = getScreenBounds()
        val maxWidth = screen.width().coerceAtLeast(1)
        val maxHeight = screen.height().coerceAtLeast(1)
        var width = requestedWidth ?: bitmap.width.coerceAtLeast(1)
        var height = requestedHeight ?: bitmap.height.coerceAtLeast(1)
        if (height > maxHeight) {
            val scale = maxHeight / height.toFloat()
            height = maxHeight
            width = (width * scale).roundToInt().coerceAtLeast(1)
        }
        if (width > maxWidth) {
            val scale = maxWidth / width.toFloat()
            width = maxWidth
            height = (height * scale).roundToInt().coerceAtLeast(1)
        }
        return width to height
    }

    private fun clampOverlayPosition(
        currentX: Int,
        currentY: Int,
        width: Int,
        height: Int
    ): Pair<Int, Int> {
        val screen = getScreenBounds()
        val minVisiblePx = (density * 48f).roundToInt()
        val viewWidth = width.coerceAtLeast(1)
        val viewHeight = height.coerceAtLeast(1)
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
            windowManager.removeViewImmediate(overlayView)
        } catch (_: Exception) {
            try {
                windowManager.removeView(overlayView)
            } catch (_: Exception) {
            }
        }
    }

    private data class OverlaySize(
        val contentWidth: Int,
        val contentHeight: Int,
        val viewWidth: Int,
        val viewHeight: Int
    )
}
