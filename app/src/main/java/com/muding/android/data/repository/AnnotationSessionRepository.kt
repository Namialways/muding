package com.muding.android.data.repository

import com.muding.android.domain.usecase.AnnotationSession
import com.muding.android.domain.usecase.AnnotationSessionFile
import com.muding.android.domain.usecase.AnnotationSessionStore
import android.content.Context

interface AnnotationSessionRepository {
    fun save(session: AnnotationSession): String
    fun get(sessionId: String): AnnotationSession?
    fun listSessionFiles(): List<AnnotationSessionFile>
    fun count(): Int
    fun visibleDirectoryPath(): String
    fun clearAll()
    fun prune(maxCount: Int, maxDays: Int)
}

class FileAnnotationSessionRepository(
    private val context: Context
) : AnnotationSessionRepository {

    override fun save(session: AnnotationSession): String {
        return AnnotationSessionStore.put(context, session)
    }

    override fun get(sessionId: String): AnnotationSession? {
        return AnnotationSessionStore.get(context, sessionId)
    }

    override fun listSessionFiles(): List<AnnotationSessionFile> {
        return AnnotationSessionStore.listSessionFiles(context)
    }

    override fun count(): Int {
        return AnnotationSessionStore.sessionCount(context)
    }

    override fun visibleDirectoryPath(): String {
        return AnnotationSessionStore.visibleDirectoryPath(context)
    }

    override fun clearAll() {
        AnnotationSessionStore.clearAll(context)
    }

    override fun prune(maxCount: Int, maxDays: Int) {
        AnnotationSessionStore.prune(context, maxCount, maxDays)
    }
}
