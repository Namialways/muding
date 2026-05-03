package com.muding.android.service

import android.content.Intent

internal object ScreenshotPermissionResultIntentFactory {

    fun create(
        resultCode: Int,
        resultData: Intent?
    ): Intent {
        val route = ScreenshotPermissionResultRouting.from(
            resultCode = resultCode,
            hasResultData = resultData != null
        )
        return Intent().apply {
            action = FloatingBallService.ACTION_START_SCREENSHOT
            putExtra(FloatingBallService.EXTRA_RESULT_CODE, route.resultCode)
            if (route.includeResultData && resultData != null) {
                putExtra(FloatingBallService.EXTRA_RESULT_DATA, resultData)
                putExtra(FloatingBallService.EXTRA_CAPTURE_AFTER_PERMISSION, route.captureAfterPermission)
            }
        }
    }
}
