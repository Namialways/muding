package com.muding.android.presentation.main

import com.muding.android.domain.usecase.RuntimeStorageSnapshot
import com.muding.android.feature.update.AppUpdateResult
import com.muding.android.feature.update.GitHubReleaseAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSupportModelsTest {

    @Test
    fun `permission support state highlights missing overlay permission`() {
        val state = PermissionSupportUiState.from(overlayPermissionGranted = false)

        assertEquals("需要授权", state.overlayStatusLabel)
        assertTrue(state.summary.contains("悬浮窗"))
        assertTrue(state.showOverlayPermissionAction)
    }

    @Test
    fun `update ui state describes available release`() {
        val state = UpdateCheckUiState.fromResult(
            AppUpdateResult.Available(
                latestVersionName = "v1.0.2",
                releaseUrl = "https://github.com/Namialways/muding/releases/tag/v1.0.2"
            )
        )

        assertEquals("发现新版本 v1.0.2", state.message)
        assertTrue(state.canOpenReleasePage)
        assertEquals("https://github.com/Namialways/muding/releases/tag/v1.0.2", state.openReleasePageUrl)
    }

    @Test
    fun `update ui state exposes downloadable apk asset`() {
        val asset = GitHubReleaseAsset(
            name = "muding-arm64-v8a.apk",
            downloadUrl = "https://github.com/download/muding-arm64-v8a.apk",
            sizeBytes = 54L * 1024L * 1024L
        )

        val state = UpdateCheckUiState.fromResult(
            AppUpdateResult.Available(
                latestVersionName = "v1.0.2",
                releaseUrl = "https://github.com/Namialways/muding/releases/tag/v1.0.2",
                apkAsset = asset
            )
        )

        assertTrue(state.canDownloadApk)
        assertEquals(asset, state.apkAsset)
        assertEquals("54.00 MB", state.apkSizeLabel)
    }

    @Test
    fun `diagnostic log includes version permission and storage`() {
        val content = buildDiagnosticLogContent(
            appVersionName = "1.0.1",
            permissionState = PermissionSupportUiState.from(overlayPermissionGranted = true),
            updateState = UpdateCheckUiState.idle(currentVersionName = "1.0.1"),
            snapshot = MainScreenSnapshot(
                sessionFileCount = 2,
                recentClosedPinCount = 1,
                pinHistoryRecords = emptyList(),
                runtimeStorage = RuntimeStorageSnapshot(
                    screenshotsCacheBytes = 1024,
                    pinnedCacheBytes = 2048,
                    shareCacheBytes = 0,
                    importCacheBytes = 0,
                    textPinCacheBytes = 0,
                    annotationSessionBytes = 512,
                    pinHistoryBytes = 256
                )
            )
        )

        assertTrue(content.contains("Version: 1.0.1"))
        assertTrue(content.contains("Overlay permission: 已授权"))
        assertTrue(content.contains("Image cache: 3.0 KB"))
        assertTrue(content.contains("Work records: 768 B"))
    }
}
