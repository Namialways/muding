package com.pixpin.android.domain.usecase

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class PinHistorySourceType(val value: String) {
    SCREENSHOT("screenshot"),
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
    val sourceType: PinHistorySourceType
)

object PinHistoryStore {
    private const val DIRECTORY_NAME = "pin_history"

    fun put(
        context: Context,
        imageUri: String,
        annotationSessionId: String?,
        sourceType: PinHistorySourceType
    ): String {
        val existing = list(context).firstOrNull {
            it.imageUri == imageUri && it.annotationSessionId == annotationSessionId
        }
        if (existing != null) {
            delete(context, existing.id)
        }
        val record = PinHistoryRecord(
            id = UUID.randomUUID().toString(),
            imageUri = imageUri,
            annotationSessionId = annotationSessionId,
            createdAt = System.currentTimeMillis(),
            sourceType = sourceType
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
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun get(context: Context, id: String): PinHistoryRecord? {
        val file = recordFile(context, id)
        if (!file.exists()) return null
        return runCatching { deserialize(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    fun delete(context: Context, id: String) {
        recordFile(context, id).delete()
    }

    fun clear(context: Context) {
        historyDirectory(context).listFiles()?.forEach { it.delete() }
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

    private fun historyDirectory(context: Context): File {
        val externalRoot = context.getExternalFilesDir("records") ?: context.filesDir
        return File(externalRoot, DIRECTORY_NAME).apply { mkdirs() }
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
        }.toString()
    }

    private fun deserialize(raw: String): PinHistoryRecord {
        val json = JSONObject(raw)
        return PinHistoryRecord(
            id = json.getString("id"),
            imageUri = json.getString("imageUri"),
            annotationSessionId = json.optString("annotationSessionId").ifBlank { null },
            createdAt = json.optLong("createdAt", 0L),
            sourceType = PinHistorySourceType.fromValue(json.optString("sourceType"))
        )
    }
}
