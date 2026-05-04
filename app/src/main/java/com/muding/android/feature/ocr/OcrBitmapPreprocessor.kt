package com.muding.android.feature.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.roundToInt

internal data class PreparedOcrBitmap(
    val bitmap: Bitmap,
    val variant: OcrImageVariant,
    val ownsBitmap: Boolean
)

internal object OcrBitmapPreprocessor {

    fun prepare(source: Bitmap): List<PreparedOcrBitmap> {
        val plan = OcrImagePreprocessingPlan.from(source.width, source.height)
        val variants = mutableListOf(
            PreparedOcrBitmap(
                bitmap = source,
                variant = OcrImageVariant.ORIGINAL,
                ownsBitmap = false
            )
        )

        val scaled = if (plan.scaleFactor > 1f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * plan.scaleFactor).roundToInt(),
                (source.height * plan.scaleFactor).roundToInt(),
                true
            )
        } else {
            null
        }
        if (scaled != null) {
            variants += PreparedOcrBitmap(
                bitmap = scaled,
                variant = OcrImageVariant.UPSCALED,
                ownsBitmap = true
            )
        }

        if (plan.includeEnhancedVariant) {
            variants += PreparedOcrBitmap(
                bitmap = enhanceForText(scaled ?: source),
                variant = OcrImageVariant.ENHANCED,
                ownsBitmap = true
            )
        }

        return variants
    }

    fun recycleOwned(variants: List<PreparedOcrBitmap>) {
        variants.forEach { prepared ->
            if (prepared.ownsBitmap && !prepared.bitmap.isRecycled) {
                prepared.bitmap.recycle()
            }
        }
    }

    private fun enhanceForText(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val contrast = 1.28f
        val translate = 255f * (1f - contrast) / 2f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val matrix = ColorMatrix().apply {
            setSaturation(0f)
            postConcat(contrastMatrix)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        Canvas(output).drawBitmap(source, 0f, 0f, paint)
        return output
    }
}
