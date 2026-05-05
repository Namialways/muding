package com.muding.android.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class PinHistorySourceType(val value: String) {
    SCREENSHOT("screenshot"),
    GALLERY_IMAGE("gallery_image"),
    CLIPBOARD_TEXT("clipboard_text"),
    OCR_TEXT("ocr_text"),
    EDITOR_EXPORT("editor_export"),
    RESTORED_PIN("restored_pin");

    companion object {
        fun fromValue(value: String?): PinHistorySourceType {
            return entries.firstOrNull { it.value == value } ?: SCREENSHOT
        }
    }
}

data class PinHistoryRecord(
    val id: String,
    val imageUri: String,
    val annotationSessionId: String?,
    val createdAt: Long,
    val sourceType: PinHistorySourceType,
    val displayName: String?,
    val textPreview: String?,
    val widthPx: Int?,
    val heightPx: Int?
)

data class PinHistoryMetadata(
    val displayName: String? = null,
    val textPreview: String? = null,
    val widthPx: Int? = null,
    val heightPx: Int? = null
)

object PinHistoryStore {
    private const val DIRECTORY_NAME = "pin_history"
    private const val ASSETS_DIRECTORY_NAME = "pin_history_assets"

    fun put(
        context: Context,
        imageUri: String,
        annotationSessionId: String?,
        sourceType: PinHistorySourceType,
        metadata: PinHistoryMetadata = PinHistoryMetadata()
    ): String {
        val recordId = UUID.randomUUID().toString()
        val persistentImageUri = persistImageForHistoryIfNeeded(
            context = context,
            imageUri = imageUri,
            recordId = recordId
        )
        val existing = list(context).firstOrNull {
            it.imageUri == persistentImageUri && it.annotationSessionId == annotationSessionId
        }
        if (existing != null) {
            delete(context, existing.id)
        }
        val record = PinHistoryRecord(
            id = recordId,
            imageUri = persistentImageUri,
            annotationSessionId = annotationSessionId,
            createdAt = System.currentTimeMillis(),
            sourceType = sourceType,
            displayName = metadata.displayName?.trim().takeUnless { it.isNullOrBlank() },
            textPreview = metadata.textPreview
                ?.trim()
                ?.replace(Regex("\\s+"), " ")
                ?.take(120)
                .takeUnless { it.isNullOrBlank() },
            widthPx = metadata.widthPx?.takeIf { it > 0 },
            heightPx = metadata.heightPx?.takeIf { it > 0 }
        )
        recordFile(context, record.id).writeText(serialize(record), Charsets.UTF_8)
        return record.id
    }

    fun list(context: Context): List<PinHistoryRecord> {
        return historyDirectory(context)
            .listFiles { file -> file.extension.equals("json", ignoreCase = true) }
            ?.mapNotNull { file ->
                runCatching {
                    deserialize(file.readText(Charsets.UTF_8))
                }.getOrNull()
            }
            ?.mapNotNull { record ->
                val repairedRecord = repairRecordImageIfNeeded(context, record)
                if (isImageAvailable(context, repairedRecord.imageUri)) {
                    repairedRecord
                } else {
                    delete(context, repairedRecord.id)
                    null
                }
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun get(context: Context, id: String): PinHistoryRecord? {
        val file = recordFile(context, id)
        if (!file.exists()) return null
        return runCatching { deserialize(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    fun delete(context: Context, id: String) {
        get(context, id)?.let { record ->
            deleteHistoryAssetIfOwned(context, record.imageUri)
        }
        recordFile(context, id).delete()
    }

    fun clear(context: Context) {
        historyDirectory(context).deleteRecursively()
        historyAssetsDirectory(context).deleteRecursively()
    }

    fun count(context: Context): Int = list(context).size

    fun prune(context: Context, maxCount: Int, maxDays: Int) {
        val now = System.currentTimeMillis()
        val cutoff = now - (maxDays.coerceAtLeast(1).toLong() * 24L * 60L * 60L * 1000L)
        val all = list(context)
        all.filter { it.createdAt < cutoff }.forEach { delete(context, it.id) }
        val remaining = list(context)
        if (remaining.size > maxCount.coerceAtLeast(1)) {
            remaining.drop(maxCount.coerceAtLeast(1)).forEach { delete(context, it.id) }
        }
    }

    fun visibleDirectoryPath(context: Context): String = historyDirectory(context).absolutePath

    private fun repairRecordImageIfNeeded(context: Context, record: PinHistoryRecord): PinHistoryRecord {
        val persistentImageUri = persistImageForHistoryIfNeeded(
            context = context,
            imageUri = record.imageUri,
            recordId = record.id
        )
        if (persistentImageUri == record.imageUri) {
            return record
        }
        val repairedRecord = record.copy(imageUri = persistentImageUri)
        recordFile(context, repairedRecord.id).writeText(serialize(repairedRecord), Charsets.UTF_8)
        return repairedRecord
    }

    private fun persistImageForHistoryIfNeeded(
        context: Context,
        imageUri: String,
        recordId: String
    ): String {
        val authority = "${context.packageName}.fileprovider"
        if (!PinHistoryImageUriPolicy.shouldPersistForHistory(imageUri, authority)) {
            return imageUri
        }
        val target = File(historyAssetsDirectory(context), "pin_history_$recordId.png")
        return try {
            context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return imageUri
            if (target.length() <= 0L) {
                target.delete()
                return imageUri
            }
            FileProvider.getUriForFile(context, authority, target).toString()
        } catch (_: Exception) {
            target.delete()
            imageUri
        }
    }

    private fun isImageAvailable(context: Context, imageUri: String): Boolean {
        if (imageUri.isBlank()) {
            return false
        }
        val internalFile = resolveInternalFileProviderFile(context, imageUri) ?: return true
        return internalFile.exists() && internalFile.length() > 0L
    }

    private fun deleteHistoryAssetIfOwned(context: Context, imageUri: String) {
        val authority = "${context.packageName}.fileprovider"
        if (!PinHistoryImageUriPolicy.isHistoryAssetUri(imageUri, authority)) {
            return
        }
        resolveInternalFileProviderFile(context, imageUri)?.delete()
    }

    private fun resolveInternalFileProviderFile(context: Context, imageUri: String): File? {
        val authority = "${context.packageName}.fileprovider"
        val path = PinHistoryImageUriPolicy.internalFileProviderPath(imageUri, authority) ?: return null
        val relativePath = path.relativeSegments.joinToString(File.separator)
        return when (path.rootName) {
            "records" -> File(recordsRoot(context), relativePath)
            "files_records" -> File(File(context.filesDir, "records"), relativePath)
            "cache" -> File(context.cacheDir, relativePath)
            "screenshots" -> File(File(context.cacheDir, "screenshots"), relativePath)
            "images" -> File(File(context.cacheDir, "images"), relativePath)
            else -> null
        }
    }

    private fun recordsRoot(context: Context): File {
        return context.getExternalFilesDir("records")
            ?: File(context.filesDir, "records").apply { mkdirs() }
    }

    private fun historyDirectory(context: Context): File {
        return File(recordsRoot(context), DIRECTORY_NAME).apply { mkdirs() }
    }

    private fun historyAssetsDirectory(context: Context): File {
        return File(recordsRoot(context), ASSETS_DIRECTORY_NAME).apply { mkdirs() }
    }

    private fun recordFile(context: Context, id: String): File {
        return File(historyDirectory(context), "$id.json")
    }

    private fun serialize(record: PinHistoryRecord): String {
        return JSONObject().apply {
            put("id", record.id)
            put("imageUri", record.imageUri)
            put("annotationSessionId", record.annotationSessionId ?: "")
            put("createdAt", record.createdAt)
            put("sourceType", record.sourceType.value)
            put("displayName", record.displayName ?: "")
            put("textPreview", record.textPreview ?: "")
            put("widthPx", record.widthPx ?: 0)
            put("heightPx", record.heightPx ?: 0)
        }.toString()
    }

    private fun deserialize(raw: String): PinHistoryRecord {
        val json = JSONObject(raw)
        return PinHistoryRecord(
            id = json.getString("id"),
            imageUri = json.getString("imageUri"),
            annotationSessionId = json.optString("annotationSessionId").ifBlank { null },
            createdAt = json.optLong("createdAt", 0L),
            sourceType = PinHistorySourceType.fromValue(json.optString("sourceType")),
            displayName = json.optString("displayName").ifBlank { null },
            textPreview = json.optString("textPreview").ifBlank { null },
            widthPx = json.optInt("widthPx", 0).takeIf { it > 0 },
            heightPx = json.optInt("heightPx", 0).takeIf { it > 0 }
        )
    }
}

internal object PinHistoryImageUriPolicy {

    data class InternalFileProviderPath(
        val rootName: String,
        val relativeSegments: List<String>
    )

    fun shouldPersistForHistory(imageUri: String, fileProviderAuthority: String): Boolean {
        if (imageUri.isBlank()) {
            return false
        }
        return !isHistoryAssetUri(imageUri, fileProviderAuthority)
    }

    fun isHistoryAssetUri(imageUri: String, fileProviderAuthority: String): Boolean {
        val path = internalFileProviderPath(imageUri, fileProviderAuthority) ?: return false
        return path.rootName in setOf("records", "files_records") &&
            path.relativeSegments.firstOrNull() == "pin_history_assets"
    }

    fun internalFileProviderPath(
        imageUri: String,
        fileProviderAuthority: String
    ): InternalFileProviderPath? {
        val prefix = "content://$fileProviderAuthority/"
        if (!imageUri.startsWith(prefix)) {
            return null
        }
        val path = imageUri
            .removePrefix(prefix)
            .substringBefore('?')
            .substringBefore('#')
        val segments = path.split('/').filter { it.isNotBlank() }
        val rootName = segments.firstOrNull() ?: return null
        return InternalFileProviderPath(
            rootName = rootName,
            relativeSegments = segments.drop(1)
        )
    }
}
