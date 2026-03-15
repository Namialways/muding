package com.pixpin.android.domain.usecase

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import com.pixpin.android.domain.model.DrawingPath
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class AnnotationRenderer {

    fun render(
        originalBitmap: Bitmap,
        paths: List<DrawingPath>,
        sourceCanvasSize: Size,
        scaledDensity: Float
    ): Bitmap {
        val resultBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        val safeCanvasWidth = sourceCanvasSize.width.takeIf { it > 0f } ?: originalBitmap.width.toFloat()
        val safeCanvasHeight = sourceCanvasSize.height.takeIf { it > 0f } ?: originalBitmap.height.toFloat()
        val scaleX = originalBitmap.width / safeCanvasWidth
        val scaleY = originalBitmap.height / safeCanvasHeight
        val scaleAverage = (scaleX + scaleY) / 2f

        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        paths.forEach { path ->
            when (path) {
                is DrawingPath.PenPath -> {
                    paint.color = path.color.toArgb()
                    paint.strokeWidth = path.strokeWidth * scaleAverage
                    paint.style = Paint.Style.STROKE
                    val scaledPath = android.graphics.Path(path.path.asAndroidPath())
                    scaledPath.transform(
                        android.graphics.Matrix().apply {
                            setScale(scaleX, scaleY)
                        }
                    )
                    canvas.drawPath(scaledPath, paint)
                }

                is DrawingPath.ArrowPath -> {
                    paint.color = path.color.toArgb()
                    paint.strokeWidth = path.strokeWidth * scaleAverage
                    paint.style = Paint.Style.STROKE
                    drawArrow(
                        canvas = canvas,
                        paint = paint,
                        startX = path.start.x * scaleX,
                        startY = path.start.y * scaleY,
                        endX = path.end.x * scaleX,
                        endY = path.end.y * scaleY
                    )
                }

                is DrawingPath.RectanglePath -> {
                    paint.color = path.color.toArgb()
                    paint.strokeWidth = path.strokeWidth * scaleAverage
                    paint.style = if (path.filled) Paint.Style.FILL else Paint.Style.STROKE
                    canvas.drawRect(
                        path.topLeft.x * scaleX,
                        path.topLeft.y * scaleY,
                        path.bottomRight.x * scaleX,
                        path.bottomRight.y * scaleY,
                        paint
                    )
                }

                is DrawingPath.CirclePath -> {
                    paint.color = path.color.toArgb()
                    paint.strokeWidth = path.strokeWidth * scaleAverage
                    paint.style = if (path.filled) Paint.Style.FILL else Paint.Style.STROKE
                    canvas.drawCircle(
                        path.center.x * scaleX,
                        path.center.y * scaleY,
                        path.radius * scaleAverage,
                        paint
                    )
                }

                is DrawingPath.TextPath -> {
                    paint.color = path.color.toArgb()
                    paint.style = Paint.Style.FILL
                    paint.textSize = path.fontSize * path.scale * scaledDensity * scaleAverage
                    val baseline = -paint.fontMetrics.ascent
                    canvas.save()
                    canvas.translate(path.position.x * scaleX, path.position.y * scaleY)
                    canvas.rotate(path.rotation)
                    canvas.drawText(path.text, 0f, baseline, paint)
                    canvas.restore()
                }
            }
        }

        return resultBitmap
    }

    private fun drawArrow(
        canvas: Canvas,
        paint: Paint,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ) {
        val angle = atan2((endY - startY).toDouble(), (endX - startX).toDouble())
        val arrowHeadLength = 40
        val arrowHeadAngle = 0.4

        canvas.drawLine(startX, startY, endX, endY, paint)

        val x1 = (endX - arrowHeadLength * cos(angle - arrowHeadAngle)).toFloat()
        val y1 = (endY - arrowHeadLength * sin(angle - arrowHeadAngle)).toFloat()
        canvas.drawLine(endX, endY, x1, y1, paint)

        val x2 = (endX - arrowHeadLength * cos(angle + arrowHeadAngle)).toFloat()
        val y2 = (endY - arrowHeadLength * sin(angle + arrowHeadAngle)).toFloat()
        canvas.drawLine(endX, endY, x2, y2, paint)
    }
}
