package com.muding.android.presentation.main

import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.domain.usecase.PinHistoryRecord
import com.muding.android.domain.usecase.PinHistorySourceType
import com.muding.android.domain.usecase.PinScaleMode
import com.muding.android.domain.usecase.RuntimeStorageSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MainDestination(
    val title: String
) {
    HOME("首页"),
    RECORDS("记录"),
    SETTINGS("设置")
}

enum class SettingsSection(
    val title: String,
    val description: String
) {
    CAPTURE_AND_FLOATING(
        title = "截图与悬浮球",
        description = ""
    ),
    PIN_AND_INTERACTION(
        title = "贴图与交互",
        description = ""
    ),
    OCR_AND_TRANSLATION(
        title = "OCR 与翻译",
        description = ""
    ),
    STORAGE_AND_RECORDS(
        title = "存储与记录",
        description = ""
    )
}

enum class RecordsFilter(
    val title: String,
    val description: String
) {
    ALL("全部", ""),
    IMAGES("图片", ""),
    TEXT("文字", ""),
    EDITABLE("可继续编辑", "")
}

enum class RecordsSortOrder(
    val title: String
) {
    NEWEST("最新"),
    OLDEST("最早"),
    SOURCE("来源"),
    EDITABLE("可编辑优先")
}

data class MainScreenSnapshot(
    val sessionFileCount: Int,
    val recentClosedPinCount: Int,
    val pinHistoryRecords: List<PinHistoryRecord>,
    val runtimeStorage: RuntimeStorageSnapshot
) {
    companion object {
        fun empty(): MainScreenSnapshot {
            return MainScreenSnapshot(
                sessionFileCount = 0,
                recentClosedPinCount = 0,
                pinHistoryRecords = emptyList(),
                runtimeStorage = RuntimeStorageSnapshot(0, 0, 0, 0, 0, 0, 0)
            )
        }
    }
}

data class PinHistoryListItemUiModel(
    val id: String,
    val imageUri: String,
    val title: String,
    val createdAtLabel: String,
    val sourceLabel: String,
    val previewText: String?,
    val dimensionLabel: String?,
    val editable: Boolean,
    val rawRecord: PinHistoryRecord,
    val searchIndex: String
)

data class RecordsComputedState(
    val allRecords: List<PinHistoryListItemUiModel>,
    val filteredRecords: List<PinHistoryListItemUiModel>,
    val filterCounts: Map<RecordsFilter, Int>
) {
    companion object {
        fun empty(): RecordsComputedState {
            return RecordsComputedState(
                allRecords = emptyList(),
                filteredRecords = emptyList(),
                filterCounts = RecordsFilter.entries.associateWith { 0 }
            )
        }
    }
}

private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val systemZoneId: ZoneId = ZoneId.systemDefault()

fun formatTimestamp(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(systemZoneId)
        .format(timestampFormatter)
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

fun captureSettingsSummary(
    action: CaptureResultAction,
    permissionGranted: Boolean
): String {
    if (!permissionGranted) {
        return "等待悬浮窗授权"
    }
    return captureActionLabel(action)
}

fun captureActionLabel(action: CaptureResultAction): String {
    return when (action) {
        CaptureResultAction.PIN_DIRECTLY -> "截图后直接贴图"
        CaptureResultAction.OPEN_EDITOR -> "截图后进入编辑"
    }
}

fun pinInteractionSettingsSummary(scaleMode: PinScaleMode): String {
    return when (scaleMode) {
        PinScaleMode.LOCK_ASPECT -> "等比缩放"
        PinScaleMode.FREE_SCALE -> "自由缩放"
    }
}

fun storageSettingsSummary(pinHistoryEnabled: Boolean): String {
    return if (pinHistoryEnabled) {
        "自动清理开启"
    } else {
        "自动清理关闭"
    }
}

fun recordsCriteriaSummary(
    filter: RecordsFilter,
    sort: RecordsSortOrder,
    query: String
): String {
    val parts = mutableListOf(
        "分类：${filter.title}",
        "排序：${sort.title}"
    )
    query.trim().takeIf { it.isNotBlank() }?.let { trimmedQuery ->
        parts += "搜索 \"$trimmedQuery\""
    }
    return parts.joinToString(" · ")
}

fun recordsFilterButtonLabel(@Suppress("UNUSED_PARAMETER") filter: RecordsFilter): String {
    return "分类"
}

fun recordsSortButtonLabel(@Suppress("UNUSED_PARAMETER") sort: RecordsSortOrder): String {
    return "排序"
}

fun buildRecordsComputedState(
    records: List<PinHistoryRecord>,
    selectedFilter: RecordsFilter,
    selectedSort: RecordsSortOrder,
    searchQuery: String
): RecordsComputedState {
    val uiModels = records.map { it.toListItemUiModel() }
    val filterCounts = RecordsFilter.entries.associateWith { filter ->
        uiModels.count { it.matches(filter) }
    }
    val normalizedQuery = searchQuery.trim().lowercase(Locale.getDefault())
    val filteredRecords = uiModels.asSequence()
        .filter { it.matches(selectedFilter) }
        .filter { it.matchesSearch(normalizedQuery) }
        .sortedWith(recordsComparator(selectedSort))
        .toList()
    return RecordsComputedState(
        allRecords = uiModels,
        filteredRecords = filteredRecords,
        filterCounts = filterCounts
    )
}

private fun PinHistoryRecord.toListItemUiModel(): PinHistoryListItemUiModel {
    val resolvedTitle = displayName ?: imageUri.substringAfterLast('/')
    val resolvedSourceLabel = historySourceLabel(sourceType)
    val resolvedCreatedAtLabel = formatTimestamp(createdAt)
    val resolvedPreviewText = textPreview?.takeIf { it.isNotBlank() }
    val resolvedDimensionLabel = if (widthPx != null && heightPx != null) {
        "${widthPx}x${heightPx}"
    } else {
        null
    }
    val resolvedEditable = !annotationSessionId.isNullOrBlank()
    val normalizedSearchIndex = buildList {
        add(resolvedTitle)
        add(imageUri.substringAfterLast('/'))
        add(resolvedSourceLabel)
        add(resolvedCreatedAtLabel)
        resolvedPreviewText?.let(::add)
    }.joinToString(separator = "\n")
        .lowercase(Locale.getDefault())
    return PinHistoryListItemUiModel(
        id = id,
        imageUri = imageUri,
        title = resolvedTitle,
        createdAtLabel = resolvedCreatedAtLabel,
        sourceLabel = resolvedSourceLabel,
        previewText = resolvedPreviewText,
        dimensionLabel = resolvedDimensionLabel,
        editable = resolvedEditable,
        rawRecord = this,
        searchIndex = normalizedSearchIndex
    )
}

private fun PinHistoryListItemUiModel.matches(filter: RecordsFilter): Boolean {
    return when (filter) {
        RecordsFilter.ALL -> true
        RecordsFilter.IMAGES -> rawRecord.sourceType == PinHistorySourceType.SCREENSHOT ||
            rawRecord.sourceType == PinHistorySourceType.GALLERY_IMAGE ||
            rawRecord.sourceType == PinHistorySourceType.EDITOR_EXPORT ||
            rawRecord.sourceType == PinHistorySourceType.RESTORED_PIN

        RecordsFilter.TEXT -> rawRecord.sourceType == PinHistorySourceType.CLIPBOARD_TEXT ||
            rawRecord.sourceType == PinHistorySourceType.OCR_TEXT

        RecordsFilter.EDITABLE -> editable
    }
}

private fun PinHistoryListItemUiModel.matchesSearch(normalizedQuery: String): Boolean {
    if (normalizedQuery.isBlank()) return true
    return searchIndex.contains(normalizedQuery)
}

fun recordsComparator(sortOrder: RecordsSortOrder): Comparator<PinHistoryListItemUiModel> {
    return when (sortOrder) {
        RecordsSortOrder.NEWEST -> compareByDescending<PinHistoryListItemUiModel> { it.rawRecord.createdAt }
        RecordsSortOrder.OLDEST -> compareBy<PinHistoryListItemUiModel> { it.rawRecord.createdAt }
        RecordsSortOrder.SOURCE -> compareBy<PinHistoryListItemUiModel>({ it.sourceLabel }, { -it.rawRecord.createdAt })
        RecordsSortOrder.EDITABLE -> compareByDescending<PinHistoryListItemUiModel> { it.editable }
            .thenByDescending { it.rawRecord.createdAt }
    }
}
