package com.muding.android.feature.update

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateModelsTest {

    @Test
    fun `newer semantic release is reported as available`() {
        val latest = GitHubLatestRelease(
            tagName = "v1.0.2",
            htmlUrl = "https://github.com/Namialways/muding/releases/tag/v1.0.2",
            assets = listOf(
                GitHubReleaseAsset(
                    name = "muding-arm64-v8a.apk",
                    downloadUrl = "https://github.com/download/muding-arm64-v8a.apk",
                    sizeBytes = 12_345L
                )
            )
        )

        val result = AppUpdateModels.compare(
            currentVersionName = "1.0.1",
            latestRelease = latest
        )

        assertEquals(
            AppUpdateResult.Available(
                latestVersionName = "v1.0.2",
                releaseUrl = latest.htmlUrl,
                apkAsset = latest.assets.first()
            ),
            result
        )
    }

    @Test
    fun `same release is reported as current`() {
        val result = AppUpdateModels.compare(
            currentVersionName = "1.0.0",
            latestRelease = GitHubLatestRelease(tagName = "v1.0.0", htmlUrl = "url", assets = emptyList())
        )

        assertEquals(AppUpdateResult.UpToDate(currentVersionName = "1.0.0"), result)
    }

    @Test
    fun `latest release json parser extracts tag and url`() {
        val release = AppUpdateModels.parseLatestRelease(
            """
            {
              "tag_name": "v1.2.3",
              "html_url": "https://github.com/Namialways/muding/releases/tag/v1.2.3",
              "assets": [
                {
                  "name": "muding-arm64-v8a.apk",
                  "browser_download_url": "https://github.com/Namialways/muding/releases/download/v1.2.3/muding-arm64-v8a.apk",
                  "size": 53874688
                },
                {
                  "name": "Source code (zip)",
                  "browser_download_url": "https://github.com/Namialways/muding/archive/refs/tags/v1.2.3.zip",
                  "size": 1024
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("v1.2.3", release.tagName)
        assertEquals("https://github.com/Namialways/muding/releases/tag/v1.2.3", release.htmlUrl)
        assertEquals(
            listOf(
                GitHubReleaseAsset(
                    name = "muding-arm64-v8a.apk",
                    downloadUrl = "https://github.com/Namialways/muding/releases/download/v1.2.3/muding-arm64-v8a.apk",
                    sizeBytes = 53874688L
                )
            ),
            release.assets
        )
    }

    @Test
    fun `apk asset selector prefers the first supported abi in device order`() {
        val assets = listOf(
            GitHubReleaseAsset(
                name = "muding-armeabi-v7a.apk",
                downloadUrl = "https://github.com/download/muding-armeabi-v7a.apk",
                sizeBytes = 10L
            ),
            GitHubReleaseAsset(
                name = "muding-arm64-v8a.apk",
                downloadUrl = "https://github.com/download/muding-arm64-v8a.apk",
                sizeBytes = 20L
            )
        )

        val selected = AppUpdateAssetSelector.selectBestApk(
            assets = assets,
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a")
        )

        assertEquals(assets[1], selected)
    }

    @Test
    fun `apk asset selector falls back to available 32 bit build`() {
        val v7a = GitHubReleaseAsset(
            name = "muding-armeabi-v7a.apk",
            downloadUrl = "https://github.com/download/muding-armeabi-v7a.apk",
            sizeBytes = 10L
        )

        val selected = AppUpdateAssetSelector.selectBestApk(
            assets = listOf(v7a),
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a")
        )

        assertEquals(v7a, selected)
    }
}
