package com.muding.android.feature.update

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateModelsTest {

    @Test
    fun `newer semantic release is reported as available`() {
        val latest = GitHubLatestRelease(
            tagName = "v1.0.2",
            htmlUrl = "https://github.com/Namialways/muding/releases/tag/v1.0.2"
        )

        val result = AppUpdateModels.compare(
            currentVersionName = "1.0.1",
            latestRelease = latest
        )

        assertEquals(
            AppUpdateResult.Available(
                latestVersionName = "v1.0.2",
                releaseUrl = latest.htmlUrl
            ),
            result
        )
    }

    @Test
    fun `same release is reported as current`() {
        val result = AppUpdateModels.compare(
            currentVersionName = "1.0.0",
            latestRelease = GitHubLatestRelease(tagName = "v1.0.0", htmlUrl = "url")
        )

        assertEquals(AppUpdateResult.UpToDate(currentVersionName = "1.0.0"), result)
    }

    @Test
    fun `latest release json parser extracts tag and url`() {
        val release = AppUpdateModels.parseLatestRelease(
            """
            {
              "tag_name": "v1.2.3",
              "html_url": "https://github.com/Namialways/muding/releases/tag/v1.2.3"
            }
            """.trimIndent()
        )

        assertEquals("v1.2.3", release.tagName)
        assertEquals("https://github.com/Namialways/muding/releases/tag/v1.2.3", release.htmlUrl)
    }
}
