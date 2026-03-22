package com.muding.android.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class ScreenshotPermissionActivity : ComponentActivity() {

    private val mediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, FloatingBallService::class.java).apply {
                action = FloatingBallService.ACTION_START_SCREENSHOT
                putExtra(FloatingBallService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(FloatingBallService.EXTRA_RESULT_DATA, result.data)
                putExtra(FloatingBallService.EXTRA_CAPTURE_AFTER_PERMISSION, true)
            }
            moveTaskToBack(true)
            finish()
            overridePendingTransition(0, 0)
            mainHandler.postDelayed(
                { applicationContext.startService(serviceIntent) },
                PERMISSION_RETURN_SETTLE_DELAY_MS
            )
            return@registerForActivityResult
        }
        moveTaskToBack(true)
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    companion object {
        private const val PERMISSION_RETURN_SETTLE_DELAY_MS = 260L
    }
}
