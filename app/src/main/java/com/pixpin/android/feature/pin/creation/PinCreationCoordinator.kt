package com.pixpin.android.feature.pin.creation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.pixpin.android.core.model.PinImageAsset
import com.pixpin.android.core.model.PinSource
import com.pixpin.android.core.model.PinSourcePayload
import com.pixpin.android.core.model.PinSourceType
import com.pixpin.android.data.image.CachedImageRepository
import com.pixpin.android.data.settings.AppSettingsRepository
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.domain.usecase.PinHistoryMetadata
import com.pixpin.android.domain.usecase.PinHistorySourceType
import com.pixpin.android.feature.pin.source.PinSourceAssetResolver
import com.pixpin.android.presentation.editor.AnnotationEditorActivity
import com.pixpin.android.service.PinOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PinCreationCoordinator(
    private val appContext: Context,
    private val settingsRepository: AppSettingsRepository,
    private val cachedImageRepository: CachedImageRepository,
    private val pinSourceAssetResolver: PinSourceAssetResolver
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

    suspend fun resolveImageAsset(source: PinSource): PinImageAsset {
        return pinSourceAssetResolver.resolve(source)
    }

    suspend fun createRequest(
        source: PinSource,
        annotationSessionId: String? = null,
        preferredImageAsset: PinImageAsset? = null,
        preferredHistoryMetadata: PinHistoryMetadata? = null
    ): ImagePinCreationRequest {
        val imageAsset = preferredImageAsset ?: resolveImageAsset(source)
        return ImagePinCreationRequest(
            source = source,
            imageAsset = imageAsset,
            annotationSessionId = annotationSessionId,
            historyMetadata = preferredHistoryMetadata ?: buildHistoryMetadata(source, imageAsset)
        )
    }

    fun createImageSource(sourceType: PinSourceType, uri: String): PinSource {
        return PinSource(
            type = sourceType,
            payload = PinSourcePayload.ImageUri(uri)
        )
    }

    fun createImageRequest(
        sourceType: PinSourceType,
        imageAsset: PinImageAsset,
        annotationSessionId: String? = null,
        historyMetadata: PinHistoryMetadata = buildHistoryMetadata(
            source = createImageSource(sourceType, imageAsset.uri),
            imageAsset = imageAsset
        )
    ): ImagePinCreationRequest {
        return ImagePinCreationRequest(
            source = createImageSource(sourceType, imageAsset.uri),
            imageAsset = imageAsset,
            annotationSessionId = annotationSessionId,
            historyMetadata = historyMetadata
        )
    }

    fun createTextSource(sourceType: PinSourceType, text: String): PinSource {
        require(sourceType == PinSourceType.CLIPBOARD_TEXT || sourceType == PinSourceType.OCR_TEXT) {
            "Text sources are only supported for clipboard or OCR inputs."
        }
        return PinSource(
            type = sourceType,
            payload = PinSourcePayload.Text(text)
        )
    }

    suspend fun launchFromSource(
        context: Context,
        source: PinSource,
        annotationSessionId: String? = null,
        forcedResultAction: CaptureResultAction? = null,
        launchEditorInNewTask: Boolean = false,
        preferredImageAsset: PinImageAsset? = null,
        preferredHistoryMetadata: PinHistoryMetadata? = null
    ) {
        val request = createRequest(
            source = source,
            annotationSessionId = annotationSessionId,
            preferredImageAsset = preferredImageAsset,
            preferredHistoryMetadata = preferredHistoryMetadata
        )
        launchFromImageRequest(
            context = context,
            request = request,
            forcedResultAction = forcedResultAction,
            launchEditorInNewTask = launchEditorInNewTask
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
            putExtra(PinOverlayService.EXTRA_HISTORY_DISPLAY_NAME, request.historyMetadata.displayName)
            putExtra(PinOverlayService.EXTRA_HISTORY_TEXT_PREVIEW, request.historyMetadata.textPreview)
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

    private fun buildHistoryMetadata(
        source: PinSource,
        imageAsset: PinImageAsset
    ): PinHistoryMetadata {
        return PinHistoryMetadata(
            displayName = resolveDisplayName(source, imageAsset),
            textPreview = (source.payload as? PinSourcePayload.Text)?.text
                ?.trim()
                ?.replace(Regex("\\s+"), " ")
                ?.take(120),
            widthPx = imageAsset.initialDisplayWidthPx,
            heightPx = imageAsset.initialDisplayHeightPx
        )
    }

    private fun resolveDisplayName(source: PinSource, imageAsset: PinImageAsset): String {
        val payload = source.payload
        return when (source.type) {
            PinSourceType.SCREENSHOT -> "截图贴图"
            PinSourceType.GALLERY_IMAGE -> {
                val originalUri = (payload as? PinSourcePayload.ImageUri)?.uri
                resolveUriDisplayName(originalUri)
                    ?: fileNameFromUri(imageAsset.uri)
                    ?: "相册贴图"
            }

            PinSourceType.CLIPBOARD_TEXT -> buildTextDisplayName(
                prefix = "剪贴板",
                text = (payload as? PinSourcePayload.Text)?.text
            )

            PinSourceType.OCR_TEXT -> buildTextDisplayName(
                prefix = "OCR",
                text = (payload as? PinSourcePayload.Text)?.text
            )

            PinSourceType.HISTORY_RESTORE -> fileNameFromUri(imageAsset.uri) ?: "恢复贴图"
            PinSourceType.EDITOR_EXPORT -> "编辑后贴图"
        }
    }

    private fun buildTextDisplayName(prefix: String, text: String?): String {
        val normalized = text
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(18)
            .takeUnless { it.isNullOrBlank() }
        return normalized?.let { "$prefix：$it" } ?: "$prefix 文字"
    }

    private fun resolveUriDisplayName(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        return runCatching {
            appContext.contentResolver.query(
                Uri.parse(uriString),
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(columnIndex)
                } else {
                    null
                }
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun fileNameFromUri(uriString: String?): String? {
        return uriString
            ?.substringAfterLast('/')
            ?.substringBefore('?')
            ?.takeIf { it.isNotBlank() }
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
