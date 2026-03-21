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
import com.muding.android.domain.usecase.CacheImageStore
import com.muding.android.domain.usecase.CaptureFlowSettings
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.presentation.editor.AnnotationEditorActivity
import com.muding.android.presentation.theme.MudingTheme
import com.muding.android.service.PinOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class RegionCropActivity : ComponentActivity() {

    private lateinit var cacheImageStore: CacheImageStore
    private lateinit var captureFlowSettings: CaptureFlowSettings
    private var sourceBitmap: Bitmap? = null
    private var forcedResultAction: CaptureResultAction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cacheImageStore = CacheImageStore(this)
        captureFlowSettings = CaptureFlowSettings(this)
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
            if (sourceBitmap == null) throw IllegalStateException("Bitmap decode failed")
        } catch (e: Exception) {
            Toast.makeText(this, "截图加载失败：${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MudingTheme {
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

                val uri = withContext(Dispatchers.IO) {
                    cacheImageStore.writePngToCache(cropped, "screenshots", "region_capture")
                }
                cropped.recycle()

                val resultAction = forcedResultAction ?: captureFlowSettings.getResultAction()
                if (resultAction == CaptureResultAction.PIN_DIRECTLY) {
                    val pinIntent = Intent(this@RegionCropActivity, PinOverlayService::class.java).apply {
                        putExtra(PinOverlayService.EXTRA_IMAGE_URI, uri.toString())
                        putExtra(PinOverlayService.EXTRA_HISTORY_SOURCE, com.muding.android.domain.usecase.PinHistorySourceType.SCREENSHOT.value)
                    }
                    startService(pinIntent)
                    closeScreenshotFlow()
                    return@launch
                }

                val editorIntent = Intent(this@RegionCropActivity, AnnotationEditorActivity::class.java).apply {
                    putExtra(AnnotationEditorActivity.EXTRA_IMAGE_URI, uri.toString())
                }
                startActivity(editorIntent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@RegionCropActivity, "区域裁剪失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun closeScreenshotFlow() {
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



