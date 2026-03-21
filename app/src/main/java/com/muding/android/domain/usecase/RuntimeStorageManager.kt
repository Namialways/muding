package com.muding.android.domain.usecase

import android.content.Context
import java.io.File

data class RuntimeStorageSnapshot(
    val screenshotsCacheBytes: Long,
    val pinnedCacheBytes: Long,
    val shareCacheBytes: Long,
    val annotationSessionBytes: Long,
    val pinHistoryBytes: Long
) {
    val totalBytes: Long
        get() = screenshotsCacheBytes + pinnedCacheBytes + shareCacheBytes +
            annotationSessionBytes + pinHistoryBytes
}

object RuntimeStorageManager {

    fun snapshot(context: Context): RuntimeStorageSnapshot {
        return RuntimeStorageSnapshot(
            screenshotsCacheBytes = directorySize(File(context.cacheDir, "screenshots")),
            pinnedCacheBytes = directorySize(File(context.cacheDir, "pinned")),
            shareCacheBytes = directorySize(File(context.cacheDir, "images")),
            annotationSessionBytes = directorySize(File(recordsRoot(context), "annotation_sessions")),
            pinHistoryBytes = directorySize(File(recordsRoot(context), "pin_history"))
        )
    }

    fun clearImageCaches(context: Context) {
        deleteDirectoryContents(File(context.cacheDir, "screenshots"))
        deleteDirectoryContents(File(context.cacheDir, "pinned"))
        deleteDirectoryContents(File(context.cacheDir, "images"))
    }

    fun clearRecordCaches(context: Context) {
        deleteDirectoryContents(File(recordsRoot(context), "annotation_sessions"))
        deleteDirectoryContents(File(recordsRoot(context), "pin_history"))
        RecentPinStore.clear(context)
    }

    fun clearAllRuntimeFiles(context: Context) {
        clearImageCaches(context)
        clearRecordCaches(context)
    }

    private fun recordsRoot(context: Context): File {
        return context.getExternalFilesDir("records") ?: context.filesDir
    }

    private fun directorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private fun deleteDirectoryContents(dir: File) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        }
    }
}
