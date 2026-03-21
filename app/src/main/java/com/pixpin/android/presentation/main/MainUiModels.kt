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
        description = "截图后默认行为、悬浮球外观和权限入口"
    ),
    PIN_AND_INTERACTION(
        title = "贴图与交互",
        description = "默认缩放模式、阴影和圆角等运行时风格"
    ),
    OCR_AND_TRANSLATION(
        title = "OCR 与翻译",
        description = "管理 OCR 相关入口和翻译模型、密钥配置"
    ),
    STORAGE_AND_RECORDS(
        title = "存储与记录",
        description = "历史保留策略、工程记录和运行缓存清理"
    )
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
