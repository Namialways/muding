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
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max

class PinnedImageRenderView(context: Context) : View(context) {

    private val density = context.resources.displayMetrics.density
    private val glowPaddingPx = 18f * density

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
            invalidate()
        }

    var onMoveWindow: ((Float, Float) -> Unit)? = null
    var onScaleBy: ((Float) -> Unit)? = null
    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null

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
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onSingleTap?.invoke()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap?.invoke()
                return true
            }
        }
    )

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                onScaleBy?.invoke(detector.scaleFactor)
                return true
            }
        }
    )

    private var lastRawX = 0f
    private var lastRawY = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
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
                lastRawX = event.rawX
                lastRawY = event.rawY
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress && event.pointerCount == 1) {
                    val dx = event.rawX - lastRawX
                    val dy = event.rawY - lastRawY
                    if (dx != 0f || dy != 0f) {
                        onMoveWindow?.invoke(dx, dy)
                    }
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                }
            }
        }
        return true
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
        imageRect.set(
            glowPaddingPx,
            glowPaddingPx,
            widthF - glowPaddingPx,
            heightF - glowPaddingPx
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
}
