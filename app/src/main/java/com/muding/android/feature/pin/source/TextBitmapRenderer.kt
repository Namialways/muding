package com.muding.android.feature.pin.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.ceil
import kotlin.math.roundToInt

class TextBitmapRenderer(
    context: Context
) {

    private val resources = context.resources

    fun render(text: String): Bitmap {
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val scaledDensity = displayMetrics.scaledDensity
        val horizontalPadding = (16f * density).roundToInt()
        val verticalPadding = (14f * density).roundToInt()
        val minContentWidth = (48f * density).roundToInt()
        val maxContentWidth = (displayMetrics.widthPixels * 0.78f).roundToInt().coerceAtLeast(minContentWidth)
        val content = text.trim().ifBlank { " " }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            textSize = 16f * scaledDensity
        }
        val desiredContentWidth = ceil(Layout.getDesiredWidth(content, textPaint).toDouble())
            .roundToInt()
            .coerceAtLeast(minContentWidth)
            .coerceAtMost(maxContentWidth)
        val layout = StaticLayout.Builder
            .obtain(content, 0, content.length, textPaint, desiredContentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()

        val bitmapWidth = (layout.width + horizontalPadding * 2).coerceAtLeast(1)
        val bitmapHeight = (layout.height + verticalPadding * 2).coerceAtLeast(1)
        val radius = 14f * density
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D7DCE5")
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
        }

        canvas.drawRoundRect(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat(), radius, radius, backgroundPaint)
        canvas.drawRoundRect(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat(), radius, radius, borderPaint)
        canvas.save()
        canvas.translate(horizontalPadding.toFloat(), verticalPadding.toFloat())
        layout.draw(canvas)
        canvas.restore()
        return bitmap
    }
}
