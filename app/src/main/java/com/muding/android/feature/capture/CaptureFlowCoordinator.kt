package com.muding.android.feature.capture

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import com.muding.android.core.model.PinSourceType
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.feature.pin.creation.EditorLaunchRequest
import com.muding.android.feature.pin.creation.ImagePinCreationRequest
import com.muding.android.feature.pin.creation.PinCreationCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CaptureDispatchRequest(
    val sourceType: PinSourceType = PinSourceType.SCREENSHOT,
    val cacheSubDir: String = "screenshots",
    val cachePrefix: String = "region_capture",
    val forcedResultAction: CaptureResultAction? = null,
    val launchEditorInNewTask: Boolean = false
)

data class PreparedCaptureResult(
    val resolvedAction: CaptureResultAction,
    val pinRequest: ImagePinCreationRequest,
    val editorRequest: EditorLaunchRequest,
    val launchEditorInNewTask: Boolean
)

class CaptureFlowCoordinator(
    private val bitmapCropper: BitmapCropper,
    private val pinCreationCoordinator: PinCreationCoordinator
) {

    suspend fun prepareCaptureResult(
        bitmap: Bitmap,
        cropRectInBitmap: Rect,
        request: CaptureDispatchRequest
    ): PreparedCaptureResult {
        val cropped = withContext(Dispatchers.Default) {
            bitmapCropper.crop(bitmap, cropRectInBitmap)
        }
        try {
            val imageAsset = pinCreationCoordinator.persistBitmapAsset(
                bitmap = cropped,
                subDir = request.cacheSubDir,
                prefix = request.cachePrefix
            )
            val pinRequest = pinCreationCoordinator.createRequest(
                source = pinCreationCoordinator.createImageSource(
                    sourceType = request.sourceType,
                    uri = imageAsset.uri
                ),
                preferredImageAsset = imageAsset
            )
            return PreparedCaptureResult(
                resolvedAction = pinCreationCoordinator.resolveResultAction(request.forcedResultAction),
                pinRequest = pinRequest,
                editorRequest = EditorLaunchRequest(imageUri = imageAsset.uri),
                launchEditorInNewTask = request.launchEditorInNewTask
            )
        } finally {
            cropped.recycle()
        }
    }

    fun dispatchPreparedResult(context: Context, preparedResult: PreparedCaptureResult) {
        when (preparedResult.resolvedAction) {
            CaptureResultAction.PIN_DIRECTLY -> {
                pinCreationCoordinator.startPinOverlay(context, preparedResult.pinRequest)
            }

            CaptureResultAction.OPEN_EDITOR -> {
                pinCreationCoordinator.startEditor(
                    context = context,
                    request = preparedResult.editorRequest,
                    launchInNewTask = preparedResult.launchEditorInNewTask
                )
            }
        }
    }
}
