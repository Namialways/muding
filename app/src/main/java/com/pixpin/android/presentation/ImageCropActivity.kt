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
import com.pixpin.android.core.model.PinSourceType
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.feature.pin.creation.EditorLaunchRequest
import com.pixpin.android.feature.pin.creation.PinCreationCoordinator
import com.pixpin.android.presentation.crop.CaptureCropOverlay
import com.pixpin.android.presentation.theme.PixPinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ImageCropActivity : ComponentActivity() {

    private lateinit var pinCreationCoordinator: PinCreationCoordinator
    private var sourceBitmap: Bitmap? = null
    private var forcedResultAction: CaptureResultAction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)
        forcedResultAction = intent.getStringExtra(EXTRA_FORCE_RESULT_ACTION)?.let {
            when (it) {
                CaptureResultAction.PIN_DIRECTLY.value -> CaptureResultAction.PIN_DIRECTLY
                CaptureResultAction.OPEN_EDITOR.value -> CaptureResultAction.OPEN_EDITOR
                else -> null
            }
        }

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString.isNullOrBlank()) {
            Toast.makeText(this, "无法加载截图", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "截图加载失败：${e.message}", Toast.LENGTH_SHORT).show()
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
                val cropped = withContext(Dispatchers.Default) {
                    val left = cropRectInBitmap.left.roundToInt().coerceIn(0, bitmap.width - 1)
                    val top = cropRectInBitmap.top.roundToInt().coerceIn(0, bitmap.height - 1)
                    val right = cropRectInBitmap.right.roundToInt().coerceIn(left + 1, bitmap.width)
                    val bottom = cropRectInBitmap.bottom.roundToInt().coerceIn(top + 1, bitmap.height)
                    Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                }

                val imageAsset = pinCreationCoordinator.persistBitmapAsset(cropped, "screenshots", "region_capture")
                cropped.recycle()

                val resultAction = pinCreationCoordinator.resolveResultAction(forcedResultAction)
                val request = pinCreationCoordinator.createImageRequest(
                    sourceType = PinSourceType.SCREENSHOT,
                    imageAsset = imageAsset
                )
                if (resultAction == CaptureResultAction.PIN_DIRECTLY) {
                    pinCreationCoordinator.startPinOverlay(this@ImageCropActivity, request)
                    closeFlow()
                    return@launch
                }

                pinCreationCoordinator.startEditor(
                    context = this@ImageCropActivity,
                    request = EditorLaunchRequest(imageUri = imageAsset.uri)
                )
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@ImageCropActivity, "区域裁剪失败：${e.message}", Toast.LENGTH_SHORT).show()
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
