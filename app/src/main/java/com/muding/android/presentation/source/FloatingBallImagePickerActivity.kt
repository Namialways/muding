package com.muding.android.presentation.source

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.muding.android.app.AppGraph
import com.muding.android.domain.usecase.FloatingBallAppearanceMode
import com.muding.android.feature.floatingball.FloatingBallImageProcessor
import com.muding.android.service.FloatingBallService
import kotlinx.coroutines.launch

class FloatingBallImagePickerActivity : ComponentActivity() {

    private lateinit var imageProcessor: FloatingBallImageProcessor
    private val finishToBackground: Boolean
        get() = intent.getBooleanExtra(EXTRA_FINISH_TO_BACKGROUND, false)
    private val restoreFloatingBall: Boolean
        get() = intent.getBooleanExtra(EXTRA_RESTORE_FLOATING_BALL, false)

    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) {
            setResult(RESULT_CANCELED)
            finishFlow()
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            when (val result = imageProcessor.process(uri)) {
                is FloatingBallImageProcessor.ProcessResult.Success -> {
                    val settingsRepository = AppGraph.appSettingsRepository(this@FloatingBallImagePickerActivity)
                    settingsRepository.setFloatingBallCustomImageUri(result.imageUri)
                    settingsRepository.setFloatingBallAppearanceMode(FloatingBallAppearanceMode.CUSTOM_IMAGE)
                    startService(
                        Intent(
                            this@FloatingBallImagePickerActivity,
                            FloatingBallService::class.java
                        ).apply {
                            action = FloatingBallService.ACTION_REFRESH_FLOATING_BALL_APPEARANCE
                        }
                    )
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(EXTRA_CUSTOM_IMAGE_URI, result.imageUri)
                    )
                }

                is FloatingBallImageProcessor.ProcessResult.RecoverableFailure -> {
                    Toast.makeText(
                        this@FloatingBallImagePickerActivity,
                        "导入悬浮球图片失败",
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_CANCELED)
                }
            }
            finishFlow()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageProcessor = AppGraph.floatingBallImageProcessor(this)
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
        const val EXTRA_CUSTOM_IMAGE_URI = "extra_custom_image_uri"
    }
}
