package com.pixpin.android.domain.usecase

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.pixpin.android.domain.model.DrawingPath
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class AnnotationSession(
    val sourceImageUri: String,
    val canvasSize: Size,
    val paths: List<DrawingPath>
)

data class AnnotationSessionFile(
    val id: String,
    val file: File,
    val lastModified: Long
)

object AnnotationSessionStore {
    private const val DIRECTORY_NAME = "annotation_sessions"
    private val memoryCache = ConcurrentHashMap<String, AnnotationSession>()

    fun put(context: Context, session: AnnotationSession): String {
        val id = UUID.randomUUID().toString()
        val copied = session.copy(paths = session.paths.map { it.deepCopy() })
        memoryCache[id] = copied
        sessionFile(context, id).writeText(serializeSession(copied), Charsets.UTF_8)
        touchFile(context, id)
        return id
    }

    fun get(context: Context, sessionId: String): AnnotationSession? {
        val memory = memoryCache[sessionId]
        if (memory != null) {
            touchFile(context, sessionId)
            return memory.copy(paths = memory.paths.map { it.deepCopy() })
        }
        val file = sessionFile(context, sessionId)
        if (!file.exists()) return null
        val restored = deserializeSession(file.readText(Charsets.UTF_8))
        if (restored != null) {
            memoryCache[sessionId] = restored
            touchFile(context, sessionId)
            return restored.copy(paths = restored.paths.map { it.deepCopy() })
        }
        return null
    }

    fun visibleDirectoryPath(context: Context): String {
        return sessionsDirectory(context).absolutePath
    }

    fun sessionCount(context: Context): Int {
        return listSessionFiles(context).size
    }

    fun clearAll(context: Context) {
        listSessionFiles(context).forEach { entry ->
            entry.file.delete()
            memoryCache.remove(entry.id)
        }
    }

    fun prune(context: Context, maxCount: Int, maxDays: Int) {
        val now = System.currentTimeMillis()
        val cutoff = now - (maxDays.coerceAtLeast(1).toLong() * 24L * 60L * 60L * 1000L)
        val all = listSessionFiles(context)
        all.filter { it.lastModified < cutoff }.forEach { entry ->
            entry.file.delete()
            memoryCache.remove(entry.id)
        }
        val remaining = listSessionFiles(context)
        if (remaining.size > maxCount) {
            remaining.drop(maxCount.coerceAtLeast(1)).forEach { entry ->
                entry.file.delete()
                memoryCache.remove(entry.id)
            }
        }
    }

    fun listSessionFiles(context: Context): List<AnnotationSessionFile> {
        return sessionsDirectory(context)
            .listFiles { file -> file.extension.equals("json", ignoreCase = true) }
            ?.mapNotNull { file ->
                val id = file.nameWithoutExtension.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                AnnotationSessionFile(id = id, file = file, lastModified = file.lastModified())
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    private fun sessionsDirectory(context: Context): File {
        val externalRoot = context.getExternalFilesDir("records") ?: context.filesDir
        return File(externalRoot, DIRECTORY_NAME).apply { mkdirs() }
    }

    private fun sessionFile(context: Context, sessionId: String): File {
        return File(sessionsDirectory(context), "$sessionId.json")
    }

    private fun touchFile(context: Context, sessionId: String) {
        sessionFile(context, sessionId).setLastModified(System.currentTimeMillis())
    }

    private fun serializeSession(session: AnnotationSession): String {
        return JSONObject().apply {
            put("sourceImageUri", session.sourceImageUri)
            put(
                "canvasSize",
                JSONObject().apply {
                    put("width", session.canvasSize.width)
                    put("height", session.canvasSize.height)
                }
            )
            put("paths", JSONArray().apply {
                session.paths.forEach { put(serializePath(it)) }
            })
        }.toString()
    }

    private fun deserializeSession(raw: String): AnnotationSession? {
        val root = JSONObject(raw)
        val sizeJson = root.getJSONObject("canvasSize")
        val pathsJson = root.getJSONArray("paths")
        val paths = buildList {
            for (i in 0 until pathsJson.length()) {
                deserializePath(pathsJson.getJSONObject(i))?.let(::add)
            }
        }
        return AnnotationSession(
            sourceImageUri = root.getString("sourceImageUri"),
            canvasSize = Size(
                width = sizeJson.getDouble("width").toFloat(),
                height = sizeJson.getDouble("height").toFloat()
            ),
            paths = paths
        )
    }

    private fun serializePath(path: DrawingPath): JSONObject {
        return when (path) {
            is DrawingPath.PenPath -> JSONObject().apply {
                put("type", "pen")
                put("color", path.color.toArgb())
                put("strokeWidth", path.strokeWidth)
                put("points", JSONArray().apply {
                    path.points.forEach { point ->
                        put(JSONObject().apply {
                            put("x", point.x)
                            put("y", point.y)
                        })
                    }
                })
            }

            is DrawingPath.ArrowPath -> JSONObject().apply {
                put("type", "arrow")
                put("color", path.color.toArgb())
                put("strokeWidth", path.strokeWidth)
                put("start", serializeOffset(path.start))
                put("end", serializeOffset(path.end))
            }

            is DrawingPath.RectanglePath -> JSONObject().apply {
                put("type", "rectangle")
                put("color", path.color.toArgb())
                put("strokeWidth", path.strokeWidth)
                put("filled", path.filled)
                put("topLeft", serializeOffset(path.topLeft))
                put("bottomRight", serializeOffset(path.bottomRight))
            }

            is DrawingPath.CirclePath -> JSONObject().apply {
                put("type", "circle")
                put("color", path.color.toArgb())
                put("strokeWidth", path.strokeWidth)
                put("filled", path.filled)
                put("center", serializeOffset(path.center))
                put("radius", path.radius)
            }

            is DrawingPath.TextPath -> JSONObject().apply {
                put("type", "text")
                put("color", path.color.toArgb())
                put("position", serializeOffset(path.position))
                put("text", path.text)
                put("fontSize", path.fontSize)
                put("scale", path.scale)
                put("rotation", path.rotation)
                put("outlineEnabled", path.outlineEnabled)
            }
        }
    }

    private fun deserializePath(json: JSONObject): DrawingPath? {
        return when (json.getString("type")) {
            "pen" -> {
                val points = mutableListOf<Offset>()
                val pointsJson = json.getJSONArray("points")
                for (i in 0 until pointsJson.length()) {
                    points += deserializeOffset(pointsJson.getJSONObject(i))
                }
                DrawingPath.PenPath(
                    path = composePathFromPoints(points),
                    color = Color(json.getInt("color")),
                    strokeWidth = json.getDouble("strokeWidth").toFloat(),
                    points = points
                )
            }

            "arrow" -> DrawingPath.ArrowPath(
                start = deserializeOffset(json.getJSONObject("start")),
                end = deserializeOffset(json.getJSONObject("end")),
                color = Color(json.getInt("color")),
                strokeWidth = json.getDouble("strokeWidth").toFloat()
            )

            "rectangle" -> DrawingPath.RectanglePath(
                topLeft = deserializeOffset(json.getJSONObject("topLeft")),
                bottomRight = deserializeOffset(json.getJSONObject("bottomRight")),
                color = Color(json.getInt("color")),
                strokeWidth = json.getDouble("strokeWidth").toFloat(),
                filled = json.optBoolean("filled", false)
            )

            "circle" -> DrawingPath.CirclePath(
                center = deserializeOffset(json.getJSONObject("center")),
                radius = json.getDouble("radius").toFloat(),
                color = Color(json.getInt("color")),
                strokeWidth = json.getDouble("strokeWidth").toFloat(),
                filled = json.optBoolean("filled", false)
            )

            "text" -> DrawingPath.TextPath(
                position = deserializeOffset(json.getJSONObject("position")),
                text = json.getString("text"),
                color = Color(json.getInt("color")),
                fontSize = json.getDouble("fontSize").toFloat(),
                scale = json.optDouble("scale", 1.0).toFloat(),
                rotation = json.optDouble("rotation", 0.0).toFloat(),
                outlineEnabled = json.optBoolean("outlineEnabled", false)
            )

            else -> null
        }
    }

    private fun serializeOffset(offset: Offset): JSONObject {
        return JSONObject().apply {
            put("x", offset.x)
            put("y", offset.y)
        }
    }

    private fun deserializeOffset(json: JSONObject): Offset {
        return Offset(
            x = json.getDouble("x").toFloat(),
            y = json.getDouble("y").toFloat()
        )
    }
}

private fun composePathFromPoints(points: List<Offset>): Path {
    return Path().apply {
        val first = points.firstOrNull() ?: return@apply
        moveTo(first.x, first.y)
        points.drop(1).forEach { point ->
            lineTo(point.x, point.y)
        }
    }
}

private fun DrawingPath.deepCopy(): DrawingPath {
    return when (this) {
        is DrawingPath.PenPath -> copy(path = composePathFromPoints(points), points = points.toList())
        is DrawingPath.ArrowPath -> copy()
        is DrawingPath.RectanglePath -> copy()
        is DrawingPath.CirclePath -> copy()
        is DrawingPath.TextPath -> copy()
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
