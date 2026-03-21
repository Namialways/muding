package com.pixpin.android.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.view.View
import com.pixpin.android.domain.usecase.PinScaleMode
import kotlin.math.max

class PinnedImageRenderView(context: Context) : View(context) {

    enum class DragResizeMode {
        RIGHT,
        BOTTOM,
        BOTTOM_RIGHT
    }

    private val density = context.resources.displayMetrics.density
    private val glowPaddingPx = 18f * density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()
    private val resizeHandleInsetPx = 28f * density

    var scaleMode: PinScaleMode = PinScaleMode.LOCK_ASPECT

    var bitmap: Bitmap? = null
        set(value) {
            field = value
            updateDrawMatrix()
            invalidate()
        }

    var cornerRadiusPx: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var shadowEnabled: Boolean = false
        set(value) {
            field = value
            setLayerType(if (value) LAYER_TYPE_SOFTWARE else LAYER_TYPE_NONE, null)
            updateDrawMatrix()
            invalidate()
        }

    var onMoveWindow: ((Float, Float) -> Unit)? = null
    var onScaleGesture: ((Float, Float, Float, Float) -> Unit)? = null
    var onDragResize: ((DragResizeMode, Float, Float) -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onFocusRequested: (() -> Unit)? = null

    private val drawMatrix = Matrix()
    private val imageRect = RectF()
    private val glowRect = RectF()
    private val clipPath = Path()

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val glowAmbientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(20f * density, BlurMaskFilter.Blur.NORMAL)
        color = 0x403890FF
    }
    private val glowSpreadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(10f * density, BlurMaskFilter.Blur.NORMAL)
        color = 0x704EA7FF
    }
    private val glowOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        maskFilter = BlurMaskFilter(7f * density, BlurMaskFilter.Blur.NORMAL)
    }
    private val glowMidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        maskFilter = BlurMaskFilter(3.5f * density, BlurMaskFilter.Blur.NORMAL)
    }
    private val glowInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                cancelPendingLongPress()
                if (shouldIgnoreTap()) return true
                onFocusRequested?.invoke()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                cancelPendingLongPress()
                if (shouldIgnoreTap()) return true
                suppressTapFor(240L)
                onDoubleTap?.invoke()
                return true
            }
        }
    )

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                scalingInProgress = true
                suppressTapFor(220L)
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                suppressTapFor(220L)
                val scaleFactorX: Float
                val scaleFactorY: Float
                if (scaleMode == PinScaleMode.FREE_SCALE) {
                    scaleFactorX = axisScaleFactor(detector.currentSpanX, detector.previousSpanX)
                    scaleFactorY = axisScaleFactor(detector.currentSpanY, detector.previousSpanY)
                } else {
                    scaleFactorX = detector.scaleFactor.coerceIn(0.7f, 1.45f)
                    scaleFactorY = scaleFactorX
                }
                onScaleGesture?.invoke(scaleFactorX, scaleFactorY, detector.focusX, detector.focusY)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                scalingInProgress = false
                suppressTapFor(220L)
            }
        }
    )

    init {
        gestureDetector.setIsLongpressEnabled(false)
    }

    private var lastRawX = 0f
    private var lastRawY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private var scalingInProgress = false
    private var dragExceededSlop = false
    private var longPressTriggered = false
    private var activeDragResizeMode: DragResizeMode? = null
    private var activePointerCount = 0
    private var suppressTapUntil = 0L
    private val longPressRunnable = Runnable {
        if (shouldIgnoreLongPress()) {
            return@Runnable
        }
        longPressTriggered = true
        suppressTapFor(320L)
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress?.invoke()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateImageRect()
        updateDrawMatrix()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentBitmap = bitmap ?: return
        val widthF = width.toFloat()
        val heightF = height.toFloat()
        if (widthF <= 0f || heightF <= 0f) return

        updateImageRect()
        val radius = cornerRadiusPx.coerceAtMost(minOf(widthF, heightF) / 2f)

        if (shadowEnabled) {
            drawBlueGlow(canvas, radius)
        }

        clipPath.reset()
        clipPath.addRoundRect(imageRect, radius, radius, Path.Direction.CW)

        val saveCount = canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(currentBitmap, drawMatrix, imagePaint)
        canvas.restoreToCount(saveCount)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerCount = event.pointerCount
                lastRawX = event.rawX
                lastRawY = event.rawY
                downRawX = event.rawX
                downRawY = event.rawY
                dragExceededSlop = false
                longPressTriggered = false
                activeDragResizeMode = resolveDragResizeMode(event.x, event.y)
                scheduleLongPress()
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                activePointerCount = event.pointerCount
                activeDragResizeMode = null
                cancelPendingLongPress()
            }

            MotionEvent.ACTION_MOVE -> {
                activePointerCount = event.pointerCount
                if (!scaleGestureDetector.isInProgress && event.pointerCount == 1) {
                    val dx = event.rawX - lastRawX
                    val dy = event.rawY - lastRawY
                    val totalDx = event.rawX - downRawX
                    val totalDy = event.rawY - downRawY
                    if ((totalDx * totalDx) + (totalDy * totalDy) >= touchSlop * touchSlop) {
                        dragExceededSlop = true
                        cancelPendingLongPress()
                        suppressTapFor(180L)
                    }
                    if (dx != 0f || dy != 0f) {
                        val resizeMode = activeDragResizeMode
                        if (resizeMode != null) {
                            onDragResize?.invoke(resizeMode, dx, dy)
                        } else {
                            onMoveWindow?.invoke(dx, dy)
                        }
                    }
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val shouldRequestFocus =
                    event.actionMasked == MotionEvent.ACTION_UP &&
                        !longPressTriggered &&
                        (dragExceededSlop || activeDragResizeMode != null)
                activePointerCount = 0
                cancelPendingLongPress()
                dragExceededSlop = false
                longPressTriggered = false
                activeDragResizeMode = null
                if (scalingInProgress || scaleGestureDetector.isInProgress) {
                    suppressTapFor(220L)
                }
                if (shouldRequestFocus) {
                    onFocusRequested?.invoke()
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                activePointerCount = event.pointerCount - 1
                cancelPendingLongPress()
                activeDragResizeMode = null
            }
        }
        return true
    }

    private fun shouldIgnoreTap(): Boolean {
        return scalingInProgress ||
            dragExceededSlop ||
            longPressTriggered ||
            System.currentTimeMillis() < suppressTapUntil
    }

    private fun suppressTapFor(durationMs: Long) {
        suppressTapUntil = maxOf(suppressTapUntil, System.currentTimeMillis() + durationMs)
    }

    private fun scheduleLongPress() {
        removeCallbacks(longPressRunnable)
        postDelayed(longPressRunnable, longPressTimeoutMs)
    }

    private fun cancelPendingLongPress() {
        removeCallbacks(longPressRunnable)
    }

    private fun shouldIgnoreLongPress(): Boolean {
        return scaleGestureDetector.isInProgress ||
            scalingInProgress ||
            dragExceededSlop ||
            activePointerCount > 1
    }

    private fun updateDrawMatrix() {
        val currentBitmap = bitmap ?: return
        if (width <= 0 || height <= 0) return
        updateImageRect()
        val src = RectF(0f, 0f, currentBitmap.width.toFloat(), currentBitmap.height.toFloat())
        drawMatrix.reset()
        drawMatrix.setRectToRect(src, imageRect, Matrix.ScaleToFit.FILL)
    }

    private fun updateImageRect() {
        val widthF = width.toFloat()
        val heightF = height.toFloat()
        if (widthF <= 0f || heightF <= 0f) return
        val padding = if (shadowEnabled) glowPaddingPx else 0f
        imageRect.set(
            padding,
            padding,
            widthF - padding,
            heightF - padding
        )
    }

    private fun drawBlueGlow(canvas: Canvas, radius: Float) {
        glowRect.set(
            imageRect.left,
            imageRect.top,
            imageRect.right,
            imageRect.bottom
        )

        canvas.drawRoundRect(
            glowRect,
            radius + 12f * density,
            radius + 12f * density,
            glowAmbientPaint
        )

        canvas.drawRoundRect(
            glowRect,
            radius + 7f * density,
            radius + 7f * density,
            glowSpreadPaint
        )

        glowOuterPaint.strokeWidth = max(6f * density, width * 0.012f)
        glowOuterPaint.color = 0xA35CB8FF.toInt()
        canvas.drawRoundRect(glowRect, radius + 6f * density, radius + 6f * density, glowOuterPaint)

        glowMidPaint.strokeWidth = max(2.5f * density, width * 0.005f)
        glowMidPaint.color = 0xE96FD0FF.toInt()
        canvas.drawRoundRect(glowRect, radius + 3f * density, radius + 3f * density, glowMidPaint)

        glowInnerPaint.strokeWidth = max(1.5f * density, width * 0.003f)
        glowInnerPaint.color = 0xFFB9F2FF.toInt()
        canvas.drawRoundRect(glowRect, radius, radius, glowInnerPaint)
    }

    private fun axisScaleFactor(currentSpan: Float, previousSpan: Float): Float {
        if (previousSpan <= 1f || currentSpan <= 1f) {
            return 1f
        }
        return (currentSpan / previousSpan).coerceIn(0.7f, 1.45f)
    }

    private fun resolveDragResizeMode(touchX: Float, touchY: Float): DragResizeMode? {
        val currentBitmap = bitmap ?: return null
        if (currentBitmap.width <= 0 || currentBitmap.height <= 0) {
            return null
        }
        val nearRight = touchX >= imageRect.right - resizeHandleInsetPx
        val nearBottom = touchY >= imageRect.bottom - resizeHandleInsetPx
        return when {
            nearRight && nearBottom -> DragResizeMode.BOTTOM_RIGHT
            scaleMode == PinScaleMode.FREE_SCALE && nearRight -> DragResizeMode.RIGHT
            scaleMode == PinScaleMode.FREE_SCALE && nearBottom -> DragResizeMode.BOTTOM
            else -> null
        }
    }
}
