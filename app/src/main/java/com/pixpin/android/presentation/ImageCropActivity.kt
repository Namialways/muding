package com.pixpin.android.presentation.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.lifecycleScope
import com.pixpin.android.app.AppGraph
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.feature.capture.CaptureDispatchRequest
import com.pixpin.android.feature.capture.CaptureFlowCoordinator
import com.pixpin.android.presentation.theme.PixPinTheme
import kotlinx.coroutines.launch

class ImageCropActivity : ComponentActivity() {

    private lateinit var captureFlowCoordinator: CaptureFlowCoordinator
    private var sourceBitmap: Bitmap? = null
    private var forcedResultAction: CaptureResultAction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureFlowCoordinator = AppGraph.captureFlowCoordinator(this)
        forcedResultAction = intent.getStringExtra(EXTRA_FORCE_RESULT_ACTION)?.let {
            CaptureResultAction.fromValue(it)
        }

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString.isNullOrBlank()) {
            Toast.makeText(this, "Unable to load image", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val uri = Uri.parse(uriString)
            contentResolver.openInputStream(uri)?.use { input ->
                sourceBitmap = BitmapFactory.decodeStream(input)
            }
            if (sourceBitmap == null) {
                throw IllegalStateException("Bitmap decode failed")
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Image load failed: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            PixPinTheme {
                CaptureCropOverlay(
                    bitmap = sourceBitmap!!,
                    onCancel = { finish() },
                    onConfirm = { cropRect ->
                        cropAndContinue(cropRect)
                    }
                )
            }
        }
    }

    private fun cropAndContinue(cropRectInBitmap: Rect) {
        val bitmap = sourceBitmap ?: return
        lifecycleScope.launch {
            try {
                val preparedResult = captureFlowCoordinator.prepareCaptureResult(
                    bitmap = bitmap,
                    cropRectInBitmap = cropRectInBitmap,
                    request = CaptureDispatchRequest(
                        forcedResultAction = forcedResultAction
                    )
                )
                captureFlowCoordinator.dispatchPreparedResult(this@ImageCropActivity, preparedResult)
                if (preparedResult.resolvedAction == CaptureResultAction.PIN_DIRECTLY) {
                    closeFlow()
                    return@launch
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@ImageCropActivity, "Crop failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun closeFlow() {
        moveTaskToBack(true)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sourceBitmap?.recycle()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_FORCE_RESULT_ACTION = "extra_force_result_action"
    }
}
