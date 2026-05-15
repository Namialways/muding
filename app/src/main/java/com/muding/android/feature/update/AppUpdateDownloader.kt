package com.muding.android.feature.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.DEFAULT_BUFFER_SIZE

data class AppUpdateDownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long
) {
    val fraction: Float?
        get() = if (totalBytes > 0L) {
            (bytesDownloaded / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
}

sealed interface AppUpdateDownloadResult {
    data class Success(val filePath: String) : AppUpdateDownloadResult
    data class Failed(val message: String) : AppUpdateDownloadResult
}

class AppUpdateDownloader(
    private val context: Context
) {

    suspend fun download(
        asset: GitHubReleaseAsset,
        onProgress: suspend (AppUpdateDownloadProgress) -> Unit
    ): AppUpdateDownloadResult {
        return withContext(Dispatchers.IO) {
            val targetFile = prepareTargetFile(asset.name)
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(asset.downloadUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12_000
                    readTimeout = 20_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/octet-stream")
                    setRequestProperty("User-Agent", "Muding-Android")
                }
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("GitHub 返回 ${connection.responseCode}")
                }
                val totalBytes = connection.contentLengthLong
                    .takeIf { it > 0L }
                    ?: asset.sizeBytes.takeIf { it > 0L }
                    ?: -1L
                dispatchProgress(onProgress, bytesDownloaded = 0L, totalBytes = totalBytes)

                var downloadedBytes = 0L
                var nextProgressAt = PROGRESS_CHUNK_BYTES
                connection.inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read.toLong()
                            if (downloadedBytes >= nextProgressAt) {
                                dispatchProgress(onProgress, downloadedBytes, totalBytes)
                                nextProgressAt = downloadedBytes + PROGRESS_CHUNK_BYTES
                            }
                        }
                    }
                }
                if (targetFile.length() <= 0L) {
                    throw IllegalStateException("安装包为空")
                }
                dispatchProgress(onProgress, targetFile.length(), totalBytes)
                AppUpdateDownloadResult.Success(filePath = targetFile.absolutePath)
            } catch (e: CancellationException) {
                targetFile.delete()
                throw e
            } catch (e: Exception) {
                targetFile.delete()
                AppUpdateDownloadResult.Failed(e.message ?: "下载失败")
            } finally {
                connection?.disconnect()
            }
        }
    }

    private suspend fun dispatchProgress(
        onProgress: suspend (AppUpdateDownloadProgress) -> Unit,
        bytesDownloaded: Long,
        totalBytes: Long
    ) {
        withContext(Dispatchers.Main.immediate) {
            onProgress(
                AppUpdateDownloadProgress(
                    bytesDownloaded = bytesDownloaded,
                    totalBytes = totalBytes
                )
            )
        }
    }

    private fun prepareTargetFile(assetName: String): File {
        val directory = File(context.cacheDir, UPDATE_CACHE_DIR).apply { mkdirs() }
        directory.listFiles()
            ?.filter { it.isFile && it.extension.equals("apk", ignoreCase = true) }
            ?.forEach { it.delete() }
        val safeName = assetName
            .substringAfterLast('/')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .takeIf { it.endsWith(".apk", ignoreCase = true) }
            ?: DEFAULT_APK_NAME
        return File(directory, safeName)
    }

    companion object {
        private const val UPDATE_CACHE_DIR = "updates"
        private const val DEFAULT_APK_NAME = "muding-update.apk"
        private const val PROGRESS_CHUNK_BYTES = 256L * 1024L
    }
}
