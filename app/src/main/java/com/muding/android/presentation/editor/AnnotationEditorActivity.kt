package com.muding.android.presentation.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.lifecycleScope
import com.muding.android.R
import com.muding.android.app.AppGraph
import com.muding.android.core.model.PinImageAsset
import com.muding.android.core.model.PinSourceType
import com.muding.android.data.image.CachedImageRepository
import com.muding.android.data.image.ImageExportRepository
import com.muding.android.data.repository.AnnotationSessionRepository
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.domain.usecase.AnnotationRenderer
import com.muding.android.domain.usecase.AnnotationSession
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.feature.pin.creation.PinCreationCoordinator
import com.muding.android.presentation.crop.ImageCropActivity
import com.muding.android.presentation.theme.MudingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnotationEditorActivity : ComponentActivity() {

    private val viewModel: AnnotationViewModel by viewModels {
        AnnotationViewModel.factory(AppGraph.appSettingsRepository(applicationContext))
    }
    private lateinit var imageExportRepository: ImageExportRepository
    private lateinit var cachedImageRepository: CachedImageRepository
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var annotationSessionRepository: AnnotationSessionRepository
    private lateinit var pinCreationCoordinator: PinCreationCoordinator
    private var sourceImageUriString: String? = null
    private var capturedBitmap: Bitmap? = null
    private var editorCanvasSize: Size = Size.Zero
    private val annotationRenderer = AnnotationRenderer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageExportRepository = AppGraph.imageExportRepository(this)
        cachedImageRepository = AppGraph.cachedImageRepository(this)
        settingsRepository = AppGraph.appSettingsRepository(this)
        annotationSessionRepository = AppGraph.annotationSessionRepository(this)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)

        val sessionId = intent.getStringExtra(EXTRA_ANNOTATION_SESSION_ID)
        val restoredSession = sessionId?.let { annotationSessionRepository.get(it) }
        val uriString = restoredSession?.sourceImageUri ?: intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.editor_loading_failed), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        sourceImageUriString = uriString

        try {
            val uri = Uri.parse(uriString)
            contentResolver.openInputStream(uri)?.use { input ->
                capturedBitmap = BitmapFactory.decodeStream(input)
            }
            if (capturedBitmap == null) throw IllegalStateException("Bitmap decode failed")
            restoredSession?.let { session ->
                editorCanvasSize = session.canvasSize
                viewModel.replacePaths(session.paths)
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.editor_loading_failed_with_reason, e.message ?: ""),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        setContent {
            MudingTheme {
                AnnotationEditorScreen()
            }
        }
    }

    @Composable
    private fun AnnotationEditorScreen() {
        val bitmap = capturedBitmap ?: return
        val viewportState = remember { EditorViewportState() }
        val documentState = viewModel.buildDocumentState()
        val toolPanelState = viewModel.buildToolPanelState()
        val screenActions = remember(viewModel) { viewModel.buildScreenActions() }

        AnnotationEditorScreenContent(
            bitmap = bitmap,
            documentState = documentState,
            toolPanelState = toolPanelState,
            screenActions = screenActions,
            viewportState = viewportState,
            onClose = { closeScreenshotFlow() },
            onSave = { saveImage(bitmap) },
            onPin = { pinImage(bitmap) },
            onShare = { shareImage(bitmap) },
            onRecrop = { recropImage(bitmap) },
            onCanvasSizeChanged = { editorCanvasSize = it }
        )
    }

    private fun saveImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val bitmap = createAnnotatedBitmap(originalBitmap)
                imageExportRepository.saveToGallery(bitmap)
                Toast.makeText(this@AnnotationEditorActivity, R.string.screenshot_saved, Toast.LENGTH_SHORT).show()
                closeScreenshotFlow()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AnnotationEditorActivity,
                    getString(R.string.editor_save_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun pinImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    createAnnotatedBitmap(originalBitmap)
                }
                val uri = withContext(Dispatchers.IO) {
                    cachedImageRepository.writePngToCache(bitmap, "pinned", "pinned_image")
                }
                val sessionId = sourceImageUriString?.let { imageUri ->
                    annotationSessionRepository.save(
                        AnnotationSession(
                            sourceImageUri = imageUri,
                            canvasSize = editorCanvasSize,
                            paths = viewModel.paths.toList()
                        )
                    )
                }
                val projectRecordSettings = settingsRepository.getProjectRecordSettings()
                annotationSessionRepository.prune(
                    maxCount = projectRecordSettings.maxSessionCount,
                    maxDays = projectRecordSettings.retainDays
                )
                pinCreationCoordinator.startPinOverlay(
                    this@AnnotationEditorActivity,
                    pinCreationCoordinator.createImageRequest(
                        sourceType = PinSourceType.EDITOR_EXPORT,
                        imageAsset = PinImageAsset(uri = uri.toString()),
                        annotationSessionId = sessionId
                    )
                )
                Toast.makeText(this@AnnotationEditorActivity, R.string.image_pinned, Toast.LENGTH_SHORT).show()
                closeScreenshotFlow()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AnnotationEditorActivity,
                    getString(R.string.editor_pin_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun shareImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val bitmap = createAnnotatedBitmap(originalBitmap)
                imageExportRepository.shareImage(bitmap)
            } catch (e: Exception) {
                Toast.makeText(
                    this@AnnotationEditorActivity,
                    getString(R.string.editor_share_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun recropImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val current = withContext(Dispatchers.Default) {
                    createAnnotatedBitmap(originalBitmap)
                }
                val uri = withContext(Dispatchers.IO) {
                    cachedImageRepository.writePngToCache(current, "screenshots", "recrop")
                }
                current.recycle()

                val intent = Intent(this@AnnotationEditorActivity, ImageCropActivity::class.java).apply {
                    putExtra(ImageCropActivity.EXTRA_IMAGE_URI, uri.toString())
                    putExtra(ImageCropActivity.EXTRA_FORCE_RESULT_ACTION, CaptureResultAction.OPEN_EDITOR.value)
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AnnotationEditorActivity,
                    getString(R.string.editor_recrop_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun createAnnotatedBitmap(originalBitmap: Bitmap): Bitmap {
        return annotationRenderer.render(
            originalBitmap = originalBitmap,
            paths = viewModel.paths,
            sourceCanvasSize = editorCanvasSize,
            scaledDensity = resources.displayMetrics.scaledDensity
        )
    }

    private fun closeScreenshotFlow() {
        moveTaskToBack(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        capturedBitmap?.recycle()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_ANNOTATION_SESSION_ID = "extra_annotation_session_id"
    }
}
