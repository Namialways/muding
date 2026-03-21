package com.pixpin.android.presentation.main

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
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixpin.android.domain.usecase.PinHistoryRecord
import com.pixpin.android.domain.usecase.PinHistorySourceType

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
    var selectedFilter by remember { mutableStateOf(RecordsFilter.ALL) }
    val filteredRecords = remember(snapshot.pinHistoryRecords, selectedFilter) {
        snapshot.pinHistoryRecords.filter { it.matches(selectedFilter) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(
                title = "记录中心"
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "贴图历史",
                    value = snapshot.pinHistoryRecords.size.toString(),
                    hint = "可恢复或继续编辑"
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "工程记录",
                    value = snapshot.sessionFiles.size.toString(),
                    hint = "用于编辑器回溯"
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "最近关闭",
                    value = snapshot.recentClosedPinCount.toString(),
                    hint = "等待恢复的贴图"
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "缓存占用",
                    value = formatFileSize(snapshot.runtimeStorage.totalBytes),
                    hint = "当前运行时文件"
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("记录操作", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onRefreshRecords, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("刷新记录")
                        }
                        OutlinedButton(onClick = onOpenStorageSettings, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Storage, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("记录设置")
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
                    Text("筛选记录", style = MaterialTheme.typography.titleMedium)
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
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("贴图历史", style = MaterialTheme.typography.titleLarge)
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
                    title = "当前筛选下没有记录",
                    description = "换一个筛选条件试试，或者先去创建新的贴图。"
                )
            }
        } else {
            items(filteredRecords, key = { it.id }) { item ->
                PinHistoryRecordCard(
                    item = item,
                    onRestore = { onRestoreHistory(item) },
                    onEdit = { onEditHistory(item) },
                    onDelete = { onDeleteHistory(item) }
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
