package com.pixpin.android.domain.usecase

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import com.pixpin.android.domain.model.DrawingPath
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 负责将标注数据渲染到 Bitmap 上的工具类
 */
class AnnotationRenderer {

    fun render(originalBitmap: Bitmap, paths: List<DrawingPath>): Bitmap {
        val resultBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)

        // 1. 先绘制原始图片
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // 2. 绘制所有标注
        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        paths.forEach { path ->
            when (path) {
                is DrawingPath.PenPath -> {
                    paint.color = path.color.toArgb()
                    paint.strokeWidth = path.strokeWidth
                    paint.style = Paint.Style.STROKE
                    canvas.drawPath(path.path.asAndroidPath(), paint)
                }
                is DrawingPath.ArrowPath -> {
                    paint.color = path.color.toArgb()
                    paint.strokeWidth = path.strokeWidth
                    paint.style = Paint.Style.STROKE
                    drawArrow(canvas, paint, path.start.x, path.start.y, path.end.x, path.end.y)
                }
                is DrawingPath.RectanglePath -> {
                    paint.color = path.color.toArgb()
                    paint.strokeWidth = path.strokeWidth
                    paint.style = if (path.filled) Paint.Style.FILL else Paint.Style.STROKE
                    canvas.drawRect(
                        path.topLeft.x,
                        path.topLeft.y,
                        path.bottomRight.x,
                        path.bottomRight.y,
                        paint
                    )
                }
                is DrawingPath.CirclePath -> {
                    paint.color = path.color.toArgb()
                    paint.strokeWidth = path.strokeWidth
                    paint.style = if (path.filled) Paint.Style.FILL else Paint.Style.STROKE
                    canvas.drawCircle(
                        path.center.x,
                        path.center.y,
                        path.radius,
                        paint
                    )
                }
                is DrawingPath.TextPath -> {
                    paint.color = path.color.toArgb()
                    paint.textSize = path.fontSize
                    paint.style = Paint.Style.FILL
                    canvas.drawText(
                        path.text,
                        path.position.x,
                        path.position.y,
                        paint
                    )
                }
            }
        }

        return resultBitmap
    }

    private fun drawArrow(canvas: Canvas, paint: Paint, startX: Float, startY: Float, endX: Float, endY: Float) {
        val angle = atan2((endY - startY).toDouble(), (endX - startX).toDouble())
        val arrowHeadLength = 40
        val arrowHeadAngle = 0.4 // radians

        canvas.drawLine(startX, startY, endX, endY, paint)

        val x1 = (endX - arrowHeadLength * cos(angle - arrowHeadAngle)).toFloat()
        val y1 = (endY - arrowHeadLength * sin(angle - arrowHeadAngle)).toFloat()
        canvas.drawLine(endX, endY, x1, y1, paint)

        val x2 = (endX - arrowHeadLength * cos(angle + arrowHeadAngle)).toFloat()
        val y2 = (endY - arrowHeadLength * sin(angle + arrowHeadAngle)).toFloat()
        canvas.drawLine(endX, endY, x2, y2, paint)
    }
}

