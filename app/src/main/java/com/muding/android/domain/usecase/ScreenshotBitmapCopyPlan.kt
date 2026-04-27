package com.muding.android.domain.usecase

data class ScreenshotBitmapCopyPlan(
    val intermediateWidth: Int,
    val requiresCroppedCopy: Boolean
) {
    companion object {
        fun from(
            width: Int,
            pixelStride: Int,
            rowStride: Int
        ): ScreenshotBitmapCopyPlan {
            val rowPadding = rowStride - pixelStride * width
            val intermediateWidth = width + rowPadding / pixelStride
            return ScreenshotBitmapCopyPlan(
                intermediateWidth = intermediateWidth,
                requiresCroppedCopy = intermediateWidth != width
            )
        }
    }
}
