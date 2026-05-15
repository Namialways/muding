package com.muding.android.feature.update

import org.json.JSONObject

data class GitHubReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

data class GitHubLatestRelease(
    val tagName: String,
    val htmlUrl: String,
    val assets: List<GitHubReleaseAsset> = emptyList()
)

sealed interface AppUpdateResult {
    data class Available(
        val latestVersionName: String,
        val releaseUrl: String,
        val apkAsset: GitHubReleaseAsset? = null
    ) : AppUpdateResult

    data class UpToDate(
        val currentVersionName: String
    ) : AppUpdateResult

    data class Failed(
        val message: String
    ) : AppUpdateResult
}

object AppUpdateModels {

    fun compare(
        currentVersionName: String,
        latestRelease: GitHubLatestRelease,
        supportedAbis: List<String> = emptyList()
    ): AppUpdateResult {
        return if (compareVersion(latestRelease.tagName, currentVersionName) > 0) {
            AppUpdateResult.Available(
                latestVersionName = latestRelease.tagName,
                releaseUrl = latestRelease.htmlUrl,
                apkAsset = AppUpdateAssetSelector.selectBestApk(
                    assets = latestRelease.assets,
                    supportedAbis = supportedAbis
                )
            )
        } else {
            AppUpdateResult.UpToDate(currentVersionName = currentVersionName)
        }
    }

    fun parseLatestRelease(json: String): GitHubLatestRelease {
        val root = JSONObject(json)
        val assets = root.optJSONArray("assets")
        return GitHubLatestRelease(
            tagName = root.optString("tag_name")
                .takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Missing tag_name"),
            htmlUrl = root.optString("html_url")
                .takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Missing html_url"),
            assets = buildList {
                if (assets == null) return@buildList
                for (index in 0 until assets.length()) {
                    val item = assets.optJSONObject(index) ?: continue
                    val name = item.optString("name").trim()
                    val downloadUrl = item.optString("browser_download_url").trim()
                    if (!name.endsWith(".apk", ignoreCase = true) || downloadUrl.isBlank()) {
                        continue
                    }
                    add(
                        GitHubReleaseAsset(
                            name = name,
                            downloadUrl = downloadUrl,
                            sizeBytes = item.optLong("size", 0L).coerceAtLeast(0L)
                        )
                    )
                }
            }
        )
    }

    private fun compareVersion(left: String, right: String): Int {
        val leftParts = parseVersionParts(left)
        val rightParts = parseVersionParts(right)
        val size = maxOf(leftParts.size, rightParts.size)
        repeat(size) { index ->
            val diff = leftParts.getOrElse(index) { 0 } - rightParts.getOrElse(index) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }

    private fun parseVersionParts(value: String): List<Int> {
        return value
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .split('.', '-', '_')
            .mapNotNull { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull()
            }
            .ifEmpty { listOf(0) }
    }

}

object AppUpdateAssetSelector {

    private val releaseAbiOrder = listOf("arm64-v8a", "armeabi-v7a")

    fun selectBestApk(
        assets: List<GitHubReleaseAsset>,
        supportedAbis: List<String>
    ): GitHubReleaseAsset? {
        val apkAssets = assets.filter { asset ->
            asset.name.endsWith(".apk", ignoreCase = true) &&
                asset.downloadUrl.startsWith("https://", ignoreCase = true)
        }
        if (apkAssets.isEmpty()) {
            return null
        }
        val preferredAbis = supportedAbis
            .filter { deviceAbi -> releaseAbiOrder.any { it.equals(deviceAbi, ignoreCase = true) } }
            .ifEmpty { releaseAbiOrder }
        preferredAbis.forEach { abi ->
            apkAssets.firstOrNull { asset ->
                asset.name.contains(abi, ignoreCase = true)
            }?.let { return it }
        }
        return apkAssets.firstOrNull { it.name.contains("universal", ignoreCase = true) }
            ?: apkAssets.firstOrNull()
    }
}
