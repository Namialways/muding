package com.pixpin.android.presentation.main

import com.pixpin.android.domain.usecase.AnnotationSessionFile
import com.pixpin.android.domain.usecase.PinHistoryRecord
import com.pixpin.android.domain.usecase.RuntimeStorageSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainDestination(
    val title: String
) {
    HOME("主页"),
    RECORDS("记录"),
    SETTINGS("设置")
}

enum class SettingsSection(
    val title: String,
    val description: String
) {
    CAPTURE_AND_FLOATING(
        title = "截图与悬浮球",
        description = "管理截图后的默认动作、悬浮球外观和权限状态。"
    ),
    PIN_AND_INTERACTION(
        title = "贴图与交互",
        description = "管理贴图的缩放模型、阴影、圆角和默认交互体验。"
    ),
    OCR_AND_TRANSLATION(
        title = "OCR 与翻译",
        description = "管理 OCR 入口、本地模型和云翻译配置。"
    ),
    STORAGE_AND_RECORDS(
        title = "存储与记录",
        description = "管理历史保留策略、工程记录和运行时缓存。"
    )
}

enum class RecordsFilter(
    val title: String,
    val description: String
) {
    ALL("全部", "查看所有贴图历史"),
    IMAGES("图片", "截图、相册和恢复后的图片贴图"),
    TEXT("文字", "剪贴板文字和 OCR 文字贴图"),
    EDITABLE("可继续编辑", "包含编辑工程的贴图记录")
}

data class MainScreenSnapshot(
    val sessionFiles: List<AnnotationSessionFile>,
    val recentClosedPinCount: Int,
    val pinHistoryRecords: List<PinHistoryRecord>,
    val recordsDirectory: String,
    val pinHistoryDirectory: String,
    val runtimeStorage: RuntimeStorageSnapshot
) {
    companion object {
        fun empty(): MainScreenSnapshot {
            return MainScreenSnapshot(
                sessionFiles = emptyList(),
                recentClosedPinCount = 0,
                pinHistoryRecords = emptyList(),
                recordsDirectory = "",
                pinHistoryDirectory = "",
                runtimeStorage = RuntimeStorageSnapshot(0, 0, 0, 0, 0)
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    return when {
        bytes >= mb -> String.format(Locale.getDefault(), "%.2f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.getDefault(), "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}
