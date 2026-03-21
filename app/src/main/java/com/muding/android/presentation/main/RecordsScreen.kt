package com.muding.android.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muding.android.domain.usecase.PinHistoryRecord
import com.muding.android.domain.usecase.PinHistorySourceType
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordsScreen(
    modifier: Modifier = Modifier,
    snapshot: MainScreenSnapshot,
    recordsLoading: Boolean,
    onRefreshRecords: () -> Unit,
    onDeleteHistory: (PinHistoryRecord) -> Unit,
    onRestoreHistory: (PinHistoryRecord) -> Unit,
    onEditHistory: (PinHistoryRecord) -> Unit,
    onOpenStorageSettings: () -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf(RecordsFilter.ALL) }
    var selectedSort by rememberSaveable { mutableStateOf(RecordsSortOrder.NEWEST) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedRecordId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedRecord = snapshot.pinHistoryRecords.firstOrNull { it.id == selectedRecordId }
    LaunchedEffect(selectedRecordId, selectedRecord) {
        if (selectedRecordId != null && selectedRecord == null) {
            selectedRecordId = null
        }
    }

    if (selectedRecord != null) {
        RecordDetailScreen(
            modifier = modifier,
            record = selectedRecord,
            onBack = { selectedRecordId = null },
            onRestore = { onRestoreHistory(selectedRecord) },
            onEdit = { onEditHistory(selectedRecord) },
            onDelete = {
                onDeleteHistory(selectedRecord)
                selectedRecordId = null
            }
        )
        return
    }

    val filteredRecords = remember(snapshot.pinHistoryRecords, selectedFilter, selectedSort, searchQuery) {
        snapshot.pinHistoryRecords
            .asSequence()
            .filter { it.matches(selectedFilter) }
            .filter { it.matchesSearch(searchQuery) }
            .sortedWith(recordsComparator(selectedSort))
            .toList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(title = "记录中心")
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "贴图历史",
                    value = snapshot.pinHistoryRecords.size.toString(),
                    hint = "可恢复"
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "工程记录",
                    value = snapshot.sessionFiles.size.toString(),
                    hint = "可继续编辑"
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "最近关闭",
                    value = snapshot.recentClosedPinCount.toString(),
                    hint = "待恢复"
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "缓存占用",
                    value = formatFileSize(snapshot.runtimeStorage.totalBytes),
                    hint = "运行文件"
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        label = { Text("搜索记录") },
                        placeholder = { Text("名称、文字、来源") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onRefreshRecords, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("刷新")
                        }
                        OutlinedButton(onClick = onOpenStorageSettings, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Storage, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("设置")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("筛选", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecordsFilter.entries.forEach { filter ->
                            SelectablePill(
                                text = "${filter.title} ${snapshot.pinHistoryRecords.count { it.matches(filter) }}",
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter }
                            )
                        }
                    }
                    Text("排序", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecordsSortOrder.entries.forEach { sortOrder ->
                            SelectablePill(
                                text = sortOrder.title,
                                selected = selectedSort == sortOrder,
                                onClick = { selectedSort = sortOrder }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("结果", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${filteredRecords.size} 条",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (recordsLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (snapshot.pinHistoryRecords.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "当前还没有贴图记录",
                    description = "完成贴图后，历史会自动出现在这里。"
                )
            }
        } else if (filteredRecords.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "没有匹配结果",
                    description = "换一个关键词、筛选或排序试试。"
                )
            }
        } else {
            items(filteredRecords, key = { it.id }) { item ->
                PinHistoryRecordCard(
                    item = item,
                    onRestore = { onRestoreHistory(item) },
                    onOpenDetails = { selectedRecordId = item.id }
                )
            }
        }
    }
}

private fun PinHistoryRecord.matches(filter: RecordsFilter): Boolean {
    return when (filter) {
        RecordsFilter.ALL -> true
        RecordsFilter.IMAGES -> sourceType in setOf(
            PinHistorySourceType.SCREENSHOT,
            PinHistorySourceType.GALLERY_IMAGE,
            PinHistorySourceType.EDITOR_EXPORT,
            PinHistorySourceType.RESTORED_PIN
        )

        RecordsFilter.TEXT -> sourceType in setOf(
            PinHistorySourceType.CLIPBOARD_TEXT,
            PinHistorySourceType.OCR_TEXT
        )

        RecordsFilter.EDITABLE -> !annotationSessionId.isNullOrBlank()
    }
}

private fun PinHistoryRecord.matchesSearch(query: String): Boolean {
    val normalizedQuery = query.trim().lowercase(Locale.getDefault())
    if (normalizedQuery.isBlank()) return true
    val haystacks = listOfNotNull(
        displayName,
        textPreview,
        imageUri.substringAfterLast('/'),
        historySourceLabel(sourceType),
        formatTimestamp(createdAt)
    )
    return haystacks.any { it.lowercase(Locale.getDefault()).contains(normalizedQuery) }
}

private fun recordsComparator(sortOrder: RecordsSortOrder): Comparator<PinHistoryRecord> {
    return when (sortOrder) {
        RecordsSortOrder.NEWEST -> compareByDescending<PinHistoryRecord> { it.createdAt }
        RecordsSortOrder.OLDEST -> compareBy<PinHistoryRecord> { it.createdAt }
        RecordsSortOrder.SOURCE -> compareBy<PinHistoryRecord>({ historySourceLabel(it.sourceType) }, { -it.createdAt })
        RecordsSortOrder.EDITABLE -> compareByDescending<PinHistoryRecord> { !it.annotationSessionId.isNullOrBlank() }
            .thenByDescending { it.createdAt }
    }
}
