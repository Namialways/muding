package com.pixpin.android.data.repository

import android.content.Context
import com.pixpin.android.domain.usecase.PinHistoryMetadata
import com.pixpin.android.domain.usecase.PinHistoryRecord
import com.pixpin.android.domain.usecase.PinHistorySourceType
import com.pixpin.android.domain.usecase.PinHistoryStore

interface PinHistoryRepository {
    fun save(
        imageUri: String,
        annotationSessionId: String?,
        sourceType: PinHistorySourceType,
        metadata: PinHistoryMetadata = PinHistoryMetadata()
    ): String
    fun list(): List<PinHistoryRecord>
    fun get(id: String): PinHistoryRecord?
    fun delete(id: String)
    fun clear()
    fun prune(maxCount: Int, maxDays: Int)
    fun visibleDirectoryPath(): String
}

class FilePinHistoryRepository(
    private val context: Context
) : PinHistoryRepository {

    override fun save(
        imageUri: String,
        annotationSessionId: String?,
        sourceType: PinHistorySourceType,
        metadata: PinHistoryMetadata
    ): String {
        return PinHistoryStore.put(context, imageUri, annotationSessionId, sourceType, metadata)
    }

    override fun list(): List<PinHistoryRecord> = PinHistoryStore.list(context)

    override fun get(id: String): PinHistoryRecord? = PinHistoryStore.get(context, id)

    override fun delete(id: String) {
        PinHistoryStore.delete(context, id)
    }

    override fun clear() {
        PinHistoryStore.clear(context)
    }

    override fun prune(maxCount: Int, maxDays: Int) {
        PinHistoryStore.prune(context, maxCount, maxDays)
    }

    override fun visibleDirectoryPath(): String {
        return PinHistoryStore.visibleDirectoryPath(context)
    }
}
