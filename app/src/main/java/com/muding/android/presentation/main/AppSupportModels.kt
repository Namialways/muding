package com.muding.android.presentation.main

import com.muding.android.feature.update.AppUpdateResult

data class PermissionSupportUiState(
    val overlayPermissionGranted: Boolean,
    val overlayStatusLabel: String,
    val screenshotStatusLabel: String,
    val summary: String,
    val showOverlayPermissionAction: Boolean
) {
    companion object {
        fun from(overlayPermissionGranted: Boolean): PermissionSupportUiState {
            return PermissionSupportUiState(
                overlayPermissionGranted = overlayPermissionGranted,
                overlayStatusLabel = if (overlayPermissionGranted) "已授权" else "需要授权",
                screenshotStatusLabel = "截图时授权",
                summary = if (overlayPermissionGranted) {
                    "悬浮窗权限正常，截图授权会在首次截图时弹出。"
                } else {
                    "悬浮窗权限未开启，悬浮球无法显示。"
                },
                showOverlayPermissionAction = !overlayPermissionGranted
            )
        }
    }
}

data class UpdateCheckUiState(
    val message: String,
    val checking: Boolean = false,
    val releaseUrl: String? = null
) {
    val openReleasePageUrl: String?
        get() = releaseUrl
            ?.trim()
            ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }

    val canOpenReleasePage: Boolean
        get() = !openReleasePageUrl.isNullOrBlank()

    companion object {
        fun idle(currentVersionName: String): UpdateCheckUiState {
            return UpdateCheckUiState(message = "当前版本 $currentVersionName")
        }

        fun checking(): UpdateCheckUiState {
            return UpdateCheckUiState(message = "正在检查更新...", checking = true)
        }

        fun fromResult(result: AppUpdateResult): UpdateCheckUiState {
            return when (result) {
                is AppUpdateResult.Available -> UpdateCheckUiState(
                    message = "发现新版本 ${result.latestVersionName}",
                    releaseUrl = result.releaseUrl
                )

                is AppUpdateResult.UpToDate -> UpdateCheckUiState(
                    message = "已是最新版本 ${result.currentVersionName}"
                )

                is AppUpdateResult.Failed -> UpdateCheckUiState(
                    message = result.message.ifBlank { "检查更新失败" }
                )
            }
        }
    }
}

fun buildDiagnosticLogContent(
    appVersionName: String,
    permissionState: PermissionSupportUiState,
    updateState: UpdateCheckUiState,
    snapshot: MainScreenSnapshot
): String {
    return buildString {
        appendLine("Muding Diagnostic Log")
        appendLine("Version: $appVersionName")
        appendLine("Overlay permission: ${permissionState.overlayStatusLabel}")
        appendLine("Screenshot permission: ${permissionState.screenshotStatusLabel}")
        appendLine("Update state: ${updateState.message}")
        appendLine("Session files: ${snapshot.sessionFileCount}")
        appendLine("Recent closed pins: ${snapshot.recentClosedPinCount}")
        appendLine("Pin history records: ${snapshot.pinHistoryRecords.size}")
        appendLine("Image cache: ${formatFileSize(snapshot.runtimeStorage.imageCacheBytes)}")
        appendLine("Work records: ${formatFileSize(snapshot.runtimeStorage.recordBytes)}")
        appendLine("Total storage: ${formatFileSize(snapshot.runtimeStorage.totalBytes)}")
    }
}
