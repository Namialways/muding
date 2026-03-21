package com.pixpin.android.presentation.source

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.pixpin.android.presentation.crop.ImageCropActivity
import com.pixpin.android.service.FloatingBallService

class GalleryOcrActivity : ComponentActivity() {

    private val finishToBackground: Boolean
        get() = intent.getBooleanExtra(EXTRA_FINISH_TO_BACKGROUND, false)
    private val restoreFloatingBall: Boolean
        get() = intent.getBooleanExtra(EXTRA_RESTORE_FLOATING_BALL, false)

    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) {
            finishFlow(restoreBall = true)
            return@registerForActivityResult
        }
        startActivity(
            Intent(this, ImageCropActivity::class.java).apply {
                putExtra(ImageCropActivity.EXTRA_IMAGE_URI, uri.toString())
                putExtra(ImageCropActivity.EXTRA_FLOW_MODE, ImageCropActivity.FLOW_MODE_OCR)
                putExtra(ImageCropActivity.EXTRA_FINISH_TO_BACKGROUND, finishToBackground)
                putExtra(ImageCropActivity.EXTRA_RESTORE_FLOATING_BALL, restoreFloatingBall)
            }
        )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
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

    companion object {
        const val EXTRA_FINISH_TO_BACKGROUND = "extra_finish_to_background"
        const val EXTRA_RESTORE_FLOATING_BALL = "extra_restore_floating_ball"
    }
}
