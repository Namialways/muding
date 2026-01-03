package com.pixpin.android.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.media.projection.MediaProjectionManager

/**
 * 一个透明的 Activity，专门用于请求 MediaProjection 权限。
 * 它会启动权限请求，并将结果通过 Intent 发送给 FloatingBallService。
 */
class ScreenshotPermissionActivity : ComponentActivity() {

    private val mediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // 将权限结果发送给服务
            val serviceIntent = Intent(this, FloatingBallService::class.java).apply {
                action = FloatingBallService.ACTION_START_SCREENSHOT
                putExtra(FloatingBallService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(FloatingBallService.EXTRA_RESULT_DATA, result.data)
            }
            startService(serviceIntent)
        }
        // 无论成功与否，都关闭这个透明的 Activity
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启动截屏权限请求
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}
