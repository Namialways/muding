package com.muding.android.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muding.android.domain.usecase.PinHistoryRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    val recordsState by produceState(
        initialValue = buildRecordsComputedState(
            records = snapshot.pinHistoryRecords,
            selectedFilter = selectedFilter,
            selectedSort = selectedSort,
            searchQuery = searchQuery
        ),
        snapshot.pinHistoryRecords,
        selectedFilter,
        selectedSort,
        searchQuery
    ) {
        value = withContext(Dispatchers.Default) {
            buildRecordsComputedState(
                records = snapshot.pinHistoryRecords,
                selectedFilter = selectedFilter,
                selectedSort = selectedSort,
                searchQuery = searchQuery
            )
        }
    }

    val selectedRecord = recordsState.allRecords.firstOrNull { it.id == selectedRecordId }?.rawRecord
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

    val tokens = rememberMainUiTokens()
    val criteriaSummary = recordsCriteriaSummary(
        filter = selectedFilter,
        sort = selectedSort,
        query = searchQuery
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = tokens.spacing.pageGutter),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(
                title = "记录",
                description = "把历史贴图当作工作列表来查找、恢复和继续编辑。"
            )
        }

        item {
            RecordsToolbar(
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                selectedSort = selectedSort,
                filterCounts = recordsState.filterCounts,
                onSearchQueryChange = { searchQuery = it },
                onSelectFilter = { selectedFilter = it },
                onSelectSort = { selectedSort = it },
                onRefreshRecords = onRefreshRecords,
                onOpenStorageSettings = onOpenStorageSettings
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "结果",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${recordsState.filteredRecords.size} 条记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.palette.body
                )
                Text(
                    text = criteriaSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.palette.body
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
                    title = "还没有贴图记录",
                    description = "完成一次贴图后，历史会自动出现在这里。"
                )
            }
        } else if (recordsState.filteredRecords.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "没有匹配结果",
                    description = "换个关键词，或者调整分类和排序试试。"
                )
            }
        } else {
            items(
                recordsState.filteredRecords,
                key = { item: PinHistoryListItemUiModel -> item.id }
            ) { item: PinHistoryListItemUiModel ->
                PinHistoryRecordCard(
                    item = item,
                    onRestore = { onRestoreHistory(item.rawRecord) },
                    onOpenDetails = { selectedRecordId = item.id }
                )
            }
        }
    }
}
