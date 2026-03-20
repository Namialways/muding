package com.pixpin.android.feature.pin.creation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.pixpin.android.core.model.PinImageAsset
import com.pixpin.android.core.model.PinSource
import com.pixpin.android.core.model.PinSourcePayload
import com.pixpin.android.core.model.PinSourceType
import com.pixpin.android.data.image.CachedImageRepository
import com.pixpin.android.data.settings.AppSettingsRepository
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.domain.usecase.PinHistorySourceType
import com.pixpin.android.presentation.editor.AnnotationEditorActivity
import com.pixpin.android.service.PinOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PinCreationCoordinator(
    private val settingsRepository: AppSettingsRepository,
    private val cachedImageRepository: CachedImageRepository
) {

    suspend fun persistBitmapAsset(bitmap: Bitmap, subDir: String, prefix: String): PinImageAsset {
        val width = bitmap.width.takeIf { it > 0 }
        val height = bitmap.height.takeIf { it > 0 }
        val uri = withContext(Dispatchers.IO) {
            cachedImageRepository.writePngToCache(bitmap, subDir, prefix)
        }
        return PinImageAsset(
            uri = uri.toString(),
            initialDisplayWidthPx = width,
            initialDisplayHeightPx = height
        )
    }

    fun resolveResultAction(forcedResultAction: CaptureResultAction? = null): CaptureResultAction {
        return forcedResultAction ?: settingsRepository.getCaptureResultAction()
    }

    fun createImageRequest(
        sourceType: PinSourceType,
        imageAsset: PinImageAsset,
        annotationSessionId: String? = null
    ): ImagePinCreationRequest {
        return ImagePinCreationRequest(
            source = PinSource(
                type = sourceType,
                payload = PinSourcePayload.ImageUri(imageAsset.uri)
            ),
            imageAsset = imageAsset,
            annotationSessionId = annotationSessionId
        )
    }

    fun launchFromImageRequest(
        context: Context,
        request: ImagePinCreationRequest,
        forcedResultAction: CaptureResultAction? = null,
        launchEditorInNewTask: Boolean = false
    ) {
        when (resolveResultAction(forcedResultAction)) {
            CaptureResultAction.PIN_DIRECTLY -> startPinOverlay(context, request)
            CaptureResultAction.OPEN_EDITOR -> {
                startEditor(
                    context = context,
                    request = EditorLaunchRequest(
                        imageUri = request.imageAsset.uri,
                        annotationSessionId = request.annotationSessionId
                    ),
                    launchInNewTask = launchEditorInNewTask
                )
            }
        }
    }

    fun createPinOverlayIntent(context: Context, request: ImagePinCreationRequest): Intent {
        return Intent(context, PinOverlayService::class.java).apply {
            putExtra(PinOverlayService.EXTRA_IMAGE_URI, request.imageAsset.uri)
            putExtra(PinOverlayService.EXTRA_ANNOTATION_SESSION_ID, request.annotationSessionId)
            putExtra(
                PinOverlayService.EXTRA_HISTORY_SOURCE,
                request.source.type.toHistorySourceType().value
            )
            putExtra(
                PinOverlayService.EXTRA_INITIAL_CONTENT_WIDTH_PX,
                request.imageAsset.initialDisplayWidthPx ?: 0
            )
            putExtra(
                PinOverlayService.EXTRA_INITIAL_CONTENT_HEIGHT_PX,
                request.imageAsset.initialDisplayHeightPx ?: 0
            )
        }
    }

    fun startPinOverlay(context: Context, request: ImagePinCreationRequest) {
        context.startService(createPinOverlayIntent(context, request))
    }

    fun createEditorIntent(
        context: Context,
        request: EditorLaunchRequest,
        launchInNewTask: Boolean = false
    ): Intent {
        return Intent(context, AnnotationEditorActivity::class.java).apply {
            if (launchInNewTask) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (!request.annotationSessionId.isNullOrBlank()) {
                putExtra(AnnotationEditorActivity.EXTRA_ANNOTATION_SESSION_ID, request.annotationSessionId)
            } else {
                putExtra(AnnotationEditorActivity.EXTRA_IMAGE_URI, request.imageUri)
            }
        }
    }

    fun startEditor(
        context: Context,
        request: EditorLaunchRequest,
        launchInNewTask: Boolean = false
    ) {
        context.startActivity(createEditorIntent(context, request, launchInNewTask))
    }
}

private fun PinSourceType.toHistorySourceType(): PinHistorySourceType {
    return when (this) {
        PinSourceType.SCREENSHOT -> PinHistorySourceType.SCREENSHOT
        PinSourceType.GALLERY_IMAGE -> PinHistorySourceType.GALLERY_IMAGE
        PinSourceType.CLIPBOARD_TEXT -> PinHistorySourceType.CLIPBOARD_TEXT
        PinSourceType.OCR_TEXT -> PinHistorySourceType.OCR_TEXT
        PinSourceType.HISTORY_RESTORE -> PinHistorySourceType.RESTORED_PIN
        PinSourceType.EDITOR_EXPORT -> PinHistorySourceType.EDITOR_EXPORT
    }
}
