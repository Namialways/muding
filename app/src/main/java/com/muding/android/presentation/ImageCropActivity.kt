package com.muding.android.presentation.crop

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.lifecycleScope
import com.muding.android.R
import com.muding.android.app.AppGraph
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.feature.capture.CaptureDispatchRequest
import com.muding.android.feature.capture.CaptureFlowCoordinator
import com.muding.android.feature.ocr.OcrFlowCoordinator
import com.muding.android.presentation.ocr.OcrResultActivity
import com.muding.android.presentation.theme.MudingTheme
import com.muding.android.service.FloatingBallService
import kotlinx.coroutines.launch

class ImageCropActivity : ComponentActivity() {

    private lateinit var captureFlowCoordinator: CaptureFlowCoordinator
    private lateinit var ocrFlowCoordinator: OcrFlowCoordinator
    private var sourceBitmap: Bitmap? = null
    private var forcedResultAction: CaptureResultAction? = null
    private val flowMode: String
        get() = intent.getStringExtra(EXTRA_FLOW_MODE) ?: FLOW_MODE_PIN
    private val finishToBackground: Boolean
        get() = intent.getBooleanExtra(EXTRA_FINISH_TO_BACKGROUND, false)
    private val restoreFloatingBall: Boolean
        get() = intent.getBooleanExtra(EXTRA_RESTORE_FLOATING_BALL, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureFlowCoordinator = AppGraph.captureFlowCoordinator(this)
        ocrFlowCoordinator = AppGraph.ocrFlowCoordinator(this)
        forcedResultAction = intent.getStringExtra(EXTRA_FORCE_RESULT_ACTION)?.let {
            CaptureResultAction.fromValue(it)
        }

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.crop_image_load_failed), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(
                this,
                getString(R.string.crop_image_load_failed_with_reason, e.message ?: ""),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        setContent {
            MudingTheme {
                CaptureCropOverlay(
                    bitmap = sourceBitmap!!,
                    onCancel = { finishFlow(restoreBall = true) },
                    onConfirm = { cropRect ->
                        when (flowMode) {
                            FLOW_MODE_OCR -> ocrAndContinue(cropRect)
                            else -> cropAndContinue(cropRect)
                        }
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
                    finishFlow(restoreBall = true)
                    return@launch
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ImageCropActivity,
                    getString(R.string.crop_operation_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun ocrAndContinue(cropRectInBitmap: Rect) {
        val bitmap = sourceBitmap ?: return
        lifecycleScope.launch {
            try {
                val preparedResult = ocrFlowCoordinator.prepareTextPin(
                    bitmap = bitmap,
                    cropRectInBitmap = cropRectInBitmap
                )
                startActivity(
                    Intent(this@ImageCropActivity, OcrResultActivity::class.java).apply {
                        putExtra(OcrResultActivity.EXTRA_RECOGNIZED_TEXT, preparedResult.recognizedText)
                        putExtra(OcrResultActivity.EXTRA_FINISH_TO_BACKGROUND, finishToBackground)
                        putExtra(OcrResultActivity.EXTRA_RESTORE_FLOATING_BALL, restoreFloatingBall)
                    }
                )
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ImageCropActivity,
                    getString(R.string.crop_ocr_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun finishFlow(restoreBall: Boolean) {
        if (restoreBall && restoreFloatingBall) {
            startService(
                FloatingBallService.createRestoreVisibilityIntent(this)
            )
        }
        if (finishToBackground) {
            moveTaskToBack(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                finish()
            }
            return
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        sourceBitmap?.recycle()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_FORCE_RESULT_ACTION = "extra_force_result_action"
        const val EXTRA_FLOW_MODE = "extra_flow_mode"
        const val EXTRA_FINISH_TO_BACKGROUND = "extra_finish_to_background"
        const val EXTRA_RESTORE_FLOATING_BALL = "extra_restore_floating_ball"
        const val FLOW_MODE_PIN = "flow_mode_pin"
        const val FLOW_MODE_OCR = "flow_mode_ocr"
    }
}
