package com.muding.android.service

import android.app.Activity

internal data class ScreenshotPermissionResultRoute(
    val resultCode: Int,
    val includeResultData: Boolean,
    val captureAfterPermission: Boolean
)

internal object ScreenshotPermissionResultRouting {

    fun from(
        resultCode: Int,
        hasResultData: Boolean
    ): ScreenshotPermissionResultRoute {
        val granted = resultCode == Activity.RESULT_OK && hasResultData
        return ScreenshotPermissionResultRoute(
            resultCode = resultCode,
            includeResultData = granted,
            captureAfterPermission = granted
        )
    }
}
