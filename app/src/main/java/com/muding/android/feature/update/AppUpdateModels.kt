package com.muding.android.feature.update

data class GitHubLatestRelease(
    val tagName: String,
    val htmlUrl: String
)

sealed interface AppUpdateResult {
    data class Available(
        val latestVersionName: String,
        val releaseUrl: String
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
        latestRelease: GitHubLatestRelease
    ): AppUpdateResult {
        return if (compareVersion(latestRelease.tagName, currentVersionName) > 0) {
            AppUpdateResult.Available(
                latestVersionName = latestRelease.tagName,
                releaseUrl = latestRelease.htmlUrl
            )
        } else {
            AppUpdateResult.UpToDate(currentVersionName = currentVersionName)
        }
    }

    fun parseLatestRelease(json: String): GitHubLatestRelease {
        return GitHubLatestRelease(
            tagName = extractJsonString(json, "tag_name")
                ?: throw IllegalArgumentException("Missing tag_name"),
            htmlUrl = extractJsonString(json, "html_url")
                ?: throw IllegalArgumentException("Missing html_url")
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

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = """"$key"\s*:\s*"([^"]+)"""".toRegex()
        return pattern.find(json)?.groupValues?.getOrNull(1)
    }
}
