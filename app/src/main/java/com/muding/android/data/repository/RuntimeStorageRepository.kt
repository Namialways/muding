package com.muding.android.data.repository

import android.content.Context
import com.muding.android.domain.usecase.RuntimeStorageManager
import com.muding.android.domain.usecase.RuntimeStorageSnapshot

interface RuntimeStorageRepository {
    fun snapshot(): RuntimeStorageSnapshot
    fun clearImageCaches()
    fun clearRecordCaches()
    fun clearAllRuntimeFiles()
}

class SystemRuntimeStorageRepository(
    private val context: Context
) : RuntimeStorageRepository {

    override fun snapshot(): RuntimeStorageSnapshot = RuntimeStorageManager.snapshot(context)

    override fun clearImageCaches() {
        RuntimeStorageManager.clearImageCaches(context)
    }

    override fun clearRecordCaches() {
        RuntimeStorageManager.clearRecordCaches(context)
    }

    override fun clearAllRuntimeFiles() {
        RuntimeStorageManager.clearAllRuntimeFiles(context)
    }
}
