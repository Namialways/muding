package com.muding.android.feature.update

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class GitHubReleaseUpdateChecker(
    private val latestReleaseUrl: String = LATEST_RELEASE_API_URL,
    private val supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList()
) {

    suspend fun check(currentVersionName: String): AppUpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                val json = fetchLatestReleaseJson()
                AppUpdateModels.compare(
                    currentVersionName = currentVersionName,
                    latestRelease = AppUpdateModels.parseLatestRelease(json),
                    supportedAbis = supportedAbis
                )
            } catch (e: Exception) {
                AppUpdateResult.Failed(e.message ?: "检查更新失败")
            }
        }
    }

    private fun fetchLatestReleaseJson(): String {
        val connection = (URL(latestReleaseUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        return try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("GitHub 返回 ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val PROJECT_PAGE_URL = "https://github.com/Namialways/muding"
        const val LATEST_RELEASE_PAGE_URL = "https://github.com/Namialways/muding/releases/latest"
        private const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/Namialways/muding/releases/latest"
    }
}
