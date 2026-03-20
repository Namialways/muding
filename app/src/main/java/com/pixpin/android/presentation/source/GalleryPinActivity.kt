package com.pixpin.android.presentation.source

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.pixpin.android.app.AppGraph
import com.pixpin.android.core.model.PinSourceType
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.feature.pin.creation.PinCreationCoordinator
import com.pixpin.android.service.FloatingBallService
import kotlinx.coroutines.launch

class GalleryPinActivity : ComponentActivity() {

    private lateinit var pinCreationCoordinator: PinCreationCoordinator
    private val finishToBackground: Boolean
        get() = intent.getBooleanExtra(EXTRA_FINISH_TO_BACKGROUND, false)
    private val restoreFloatingBall: Boolean
        get() = intent.getBooleanExtra(EXTRA_RESTORE_FLOATING_BALL, false)

    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) {
            finishFlow()
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            try {
                pinCreationCoordinator.launchFromSource(
                    context = this@GalleryPinActivity,
                    source = pinCreationCoordinator.createImageSource(
                        sourceType = PinSourceType.GALLERY_IMAGE,
                        uri = uri.toString()
                    ),
                    forcedResultAction = CaptureResultAction.PIN_DIRECTLY
                )
            } catch (e: Exception) {
                Toast.makeText(this@GalleryPinActivity, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                finishFlow()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)
        pickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun finishFlow() {
        if (restoreFloatingBall) {
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
