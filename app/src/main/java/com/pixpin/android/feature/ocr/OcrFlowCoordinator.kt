package com.pixpin.android.feature.ocr

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import com.pixpin.android.core.model.PinSourceType
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.feature.capture.BitmapCropper
import com.pixpin.android.feature.pin.creation.ImagePinCreationRequest
import com.pixpin.android.feature.pin.creation.PinCreationCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PreparedOcrTextPinResult(
    val recognizedText: String,
    val ocrResult: OcrResult,
    val pinRequest: ImagePinCreationRequest
)

class OcrFlowCoordinator(
    private val bitmapCropper: BitmapCropper,
    private val ocrEngine: OcrEngine,
    private val pinCreationCoordinator: PinCreationCoordinator
) {

    suspend fun prepareTextPin(
        bitmap: Bitmap,
        cropRectInBitmap: Rect
    ): PreparedOcrTextPinResult {
        val cropped = withContext(Dispatchers.Default) {
            bitmapCropper.crop(bitmap, cropRectInBitmap)
        }
        try {
            val ocrResult = ocrEngine.recognize(cropped)
            val recognizedText = ocrResult.normalizedText
            require(recognizedText.isNotBlank()) {
                "No text recognized"
            }
            val pinRequest = pinCreationCoordinator.createRequest(
                source = pinCreationCoordinator.createTextSource(
                    sourceType = PinSourceType.OCR_TEXT,
                    text = recognizedText
                )
            )
            return PreparedOcrTextPinResult(
                recognizedText = recognizedText,
                ocrResult = ocrResult,
                pinRequest = pinRequest
            )
        } finally {
            cropped.recycle()
        }
    }

    fun dispatchTextPin(
        context: Context,
        preparedResult: PreparedOcrTextPinResult,
        forcedResultAction: CaptureResultAction = CaptureResultAction.PIN_DIRECTLY
    ) {
        pinCreationCoordinator.launchFromImageRequest(
            context = context,
            request = preparedResult.pinRequest,
            forcedResultAction = forcedResultAction
        )
    }
}
