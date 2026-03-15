package com.pixpin.android.domain.usecase

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import com.pixpin.android.domain.model.DrawingPath
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class AnnotationSession(
    val sourceImageUri: String,
    val canvasSize: Size,
    val paths: List<DrawingPath>
)

object AnnotationSessionStore {
    private val sessions = ConcurrentHashMap<String, AnnotationSession>()

    fun put(session: AnnotationSession): String {
        val id = UUID.randomUUID().toString()
        sessions[id] = session.copy(paths = session.paths.map { it.deepCopy() })
        return id
    }

    fun get(sessionId: String): AnnotationSession? {
        val session = sessions[sessionId] ?: return null
        return session.copy(paths = session.paths.map { it.deepCopy() })
    }
}

private fun DrawingPath.deepCopy(): DrawingPath {
    return when (this) {
        is DrawingPath.PenPath -> copy(path = Path().apply { addPath(path) })
        is DrawingPath.ArrowPath -> copy()
        is DrawingPath.RectanglePath -> copy()
        is DrawingPath.CirclePath -> copy()
        is DrawingPath.TextPath -> copy()
    }
}
