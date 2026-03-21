package com.pixpin.android.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixpin.android.domain.usecase.PinHistoryRecord

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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(
                title = "记录总览",
                description = "记录页只负责查看和操作内容，不再承担低频配置和缓存维护。"
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "贴图历史",
                    value = snapshot.pinHistoryRecords.size.toString(),
                    hint = "条"
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "工程记录",
                    value = snapshot.sessionFiles.size.toString(),
                    hint = "个"
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "恢复队列",
                    value = snapshot.recentClosedPinCount.toString(),
                    hint = "个"
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "运行占用",
                    value = formatFileSize(snapshot.runtimeStorage.totalBytes),
                    hint = "缓存总量"
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("记录说明", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "贴图历史目录：${snapshot.pinHistoryDirectory.ifBlank { "暂未生成" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "工程目录：${snapshot.recordsDirectory.ifBlank { "暂未生成" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onRefreshRecords, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("刷新记录")
                        }
                        OutlinedButton(onClick = onOpenStorageSettings, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Storage, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("去记录设置")
                        }
                    }
                }
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("贴图历史", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "共 ${snapshot.pinHistoryRecords.size} 条，可重新贴图或继续编辑。",
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
                    title = "当前还没有贴图历史",
                    description = "贴图后会自动出现在这里。"
                )
            }
        } else {
            items(snapshot.pinHistoryRecords, key = { it.id }) { item ->
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
