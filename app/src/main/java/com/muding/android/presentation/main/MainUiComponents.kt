package com.muding.android.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.muding.android.domain.usecase.FloatingBallTheme
import com.muding.android.domain.usecase.PinHistorySourceType
import com.muding.android.presentation.theme.floatingBallThemeColors

@Composable
private fun mainUiBorder(): BorderStroke {
    val tokens = rememberMainUiTokens()
    return BorderStroke(1.dp, tokens.palette.outline)
}

@Composable
private fun mainUiMutedCardColors() = CardDefaults.cardColors(
    containerColor = rememberMainUiTokens().palette.surfaceMuted
)

@Composable
private fun mainUiStrongCardColors() = CardDefaults.cardColors(
    containerColor = rememberMainUiTokens().palette.surfaceStrong
)

@Composable
fun SectionHeader(
    title: String,
    description: String? = null
) {
    val tokens = rememberMainUiTokens()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = tokens.palette.title
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.palette.body
            )
        }
    }
}

@Composable
fun SettingGroup(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = rememberMainUiTokens()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.corners.group),
        color = tokens.palette.surfaceStrong,
        border = BorderStroke(1.dp, tokens.palette.outline),
        tonalElevation = tokens.elevations.flat
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.contentPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.rowGap),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.palette.title
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.palette.body
                    )
                }
                content()
            }
        )
    }
}

@Composable
fun SettingEntryRow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(tokens.corners.group),
        color = tokens.palette.surfaceStrong,
        border = BorderStroke(1.dp, tokens.palette.outline),
        tonalElevation = tokens.elevations.flat
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.contentPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = tokens.palette.surfaceAccent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tokens.palette.accent
                    )
                }
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = tokens.palette.title
            )
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.palette.body
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowOutward,
                contentDescription = null,
                tint = tokens.palette.body
            )
        }
    }
}

@Composable
fun InlineValueRow(
    label: String,
    value: String
) {
    val tokens = rememberMainUiTokens()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.palette.body
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.palette.title
        )
    }
}

@Composable
fun HomeActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(tokens.corners.tile),
        border = mainUiBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.elevations.lifted),
        colors = mainUiStrongCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = tokens.palette.surfaceAccent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tokens.palette.accent
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = tokens.palette.title
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.palette.body
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsCategoryCard(
    icon: ImageVector,
    section: SettingsSection,
    summary: List<String>,
    onClick: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(tokens.corners.group),
        border = mainUiBorder(),
        colors = mainUiMutedCardColors()
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = tokens.palette.surfaceAccent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tokens.palette.accent
                        )
                    }
                }
                Text(
                    text = section.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.palette.title
                )
                Icon(
                    imageVector = Icons.Default.ArrowOutward,
                    contentDescription = null,
                    tint = tokens.palette.body
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                summary.forEachIndexed { index, line ->
                    SummaryPill(
                        text = line,
                        emphasized = index == 0
                    )
                }
            }
        }
    }
}

@Composable
fun MetricsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    hint: String
) {
    val tokens = rememberMainUiTokens()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.corners.group),
        border = mainUiBorder(),
        colors = mainUiMutedCardColors()
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.contentPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.palette.body
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = tokens.palette.title
            )
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.palette.body
            )
        }
    }
}

@Composable
fun SummaryPill(
    text: String,
    emphasized: Boolean = false
) {
    val tokens = rememberMainUiTokens()
    val containerColor = if (emphasized) {
        tokens.palette.surfaceAccent
    } else {
        tokens.palette.surfaceMuted
    }
    val contentColor = if (emphasized) {
        tokens.palette.accent
    } else {
        tokens.palette.body
    }
    Surface(
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, tokens.palette.outline)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Composable
fun SelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    Surface(
        shape = CircleShape,
        color = if (selected) {
            tokens.palette.accent
        } else {
            tokens.palette.surface
        },
        border = BorderStroke(1.dp, tokens.palette.outline),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                tokens.palette.body
            }
        )
    }
}

@Composable
fun FilterMenuButton(
    selectedFilter: RecordsFilter,
    filterCounts: Map<RecordsFilter, Int>,
    onSelect: (RecordsFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FilterList, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(recordsFilterButtonLabel(selectedFilter))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RecordsFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text("${filter.title} ${filterCounts[filter] ?: 0}") },
                    onClick = {
                        expanded = false
                        onSelect(filter)
                    }
                )
            }
        }
    }
}

@Composable
fun SortMenuButton(
    selectedSort: RecordsSortOrder,
    onSelect: (RecordsSortOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(recordsSortButtonLabel(selectedSort))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RecordsSortOrder.entries.forEach { sortOrder ->
                DropdownMenuItem(
                    text = { Text(sortOrder.title) },
                    onClick = {
                        expanded = false
                        onSelect(sortOrder)
                    }
                )
            }
        }
    }
}

@Composable
fun RecordsToolbar(
    searchQuery: String,
    selectedFilter: RecordsFilter,
    selectedSort: RecordsSortOrder,
    filterCounts: Map<RecordsFilter, Int>,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (RecordsFilter) -> Unit,
    onSelectSort: (RecordsSortOrder) -> Unit,
    onRefreshRecords: () -> Unit,
    onOpenStorageSettings: () -> Unit
) {
    SettingGroup(title = "搜索与筛选") {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            label = { Text("搜索记录") },
            placeholder = { Text("名称、文字、来源") }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterMenuButton(
                selectedFilter = selectedFilter,
                filterCounts = filterCounts,
                onSelect = onSelectFilter
            )
            SortMenuButton(
                selectedSort = selectedSort,
                onSelect = onSelectSort
            )
            OutlinedButton(onClick = onRefreshRecords) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("刷新")
            }
            OutlinedButton(onClick = onOpenStorageSettings) {
                Icon(Icons.Default.Storage, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("设置")
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String
) {
    val tokens = rememberMainUiTokens()
    Surface(
        shape = RoundedCornerShape(tokens.corners.card),
        tonalElevation = tokens.elevations.lifted,
        border = BorderStroke(1.dp, tokens.palette.outline),
        color = tokens.palette.surfaceStrong,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = tokens.palette.accent
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.palette.title
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.palette.body,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinHistoryRecordCard(
    item: PinHistoryListItemUiModel,
    onRestore: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.corners.card),
        border = mainUiBorder(),
        colors = mainUiStrongCardColors()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                RecordThumbnail(
                    imageUri = item.imageUri,
                    modifier = Modifier.size(84.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.palette.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.createdAtLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.palette.body
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryPill(text = item.sourceLabel, emphasized = true)
                        if (item.editable) {
                            SummaryPill(text = "可继续编辑")
                        }
                        item.dimensionLabel?.let { dimensionLabel ->
                            SummaryPill(text = dimensionLabel)
                        }
                    }
                    item.previewText?.let { preview ->
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.palette.body,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
                    Text("恢复贴图")
                }
                OutlinedButton(onClick = onOpenDetails, modifier = Modifier.weight(1f)) {
                    Text("查看详情")
                }
            }
        }
    }
}

@Composable
fun RecordThumbnail(
    imageUri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = rememberMainUiTokens()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(tokens.corners.thumbnail))
            .background(tokens.palette.surfaceMuted)
            .border(
                border = BorderStroke(1.dp, tokens.palette.outline),
                shape = RoundedCornerShape(tokens.corners.thumbnail)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri.isBlank()) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = tokens.palette.body
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.BrokenImage),
                fallback = rememberVectorPainter(Icons.Default.BrokenImage)
            )
        }
    }
}

@Composable
fun LabeledValueRow(
    label: String,
    value: String
) {
    val tokens = rememberMainUiTokens()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.palette.body
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.palette.title,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun FloatingBallAppearancePreview(
    sizeDp: Int,
    opacity: Float,
    theme: FloatingBallTheme
) {
    val colors = floatingBallThemeColors(theme)
    val tokens = rememberMainUiTokens()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(sizeDp.dp),
            shape = CircleShape,
            shadowElevation = tokens.elevations.preview,
            border = BorderStroke(1.dp, tokens.palette.outline),
            color = tokens.palette.surfaceStrong
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(opacity)
                    .background(
                        brush = Brush.linearGradient(listOf(colors.start, colors.end)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((sizeDp * 0.5f).dp)
                )
            }
        }
    }
}

@Composable
fun NumberSettingRow(
    title: String,
    value: Int,
    valueRange: IntRange,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onApply: (Int) -> Unit
) {
    var input by remember(value) { mutableStateOf(value.toString()) }
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactStepButton(label = "-", onClick = onDecrease)
            OutlinedTextField(
                value = input,
                onValueChange = { next ->
                    if (next.all { it.isDigit() } && next.length <= 3) {
                        input = next
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(
                    onDone = {
                        input.toIntOrNull()?.coerceIn(valueRange)?.let(onApply)
                        focusManager.clearFocus()
                    }
                )
            )
            CompactStepButton(label = "+", onClick = onIncrease)
        }
        TextButton(
            onClick = {
                input.toIntOrNull()?.coerceIn(valueRange)?.let(onApply)
                focusManager.clearFocus()
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("保存")
        }
    }
}

@Composable
private fun CompactStepButton(
    label: String,
    onClick: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    Surface(
        shape = CircleShape,
        color = tokens.palette.surfaceMuted,
        border = BorderStroke(1.dp, tokens.palette.outline),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = tokens.palette.body
            )
        }
    }
}

@Composable
fun CaptureOptionRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

fun floatingBallThemeLabel(theme: FloatingBallTheme): String {
    return when (theme) {
        FloatingBallTheme.BLUE_PURPLE -> "蓝紫渐变"
        FloatingBallTheme.SUNSET -> "日落橙红"
        FloatingBallTheme.EMERALD -> "青绿渐变"
    }
}

fun historySourceLabel(sourceType: PinHistorySourceType): String {
    return when (sourceType) {
        PinHistorySourceType.SCREENSHOT -> "截图直贴"
        PinHistorySourceType.GALLERY_IMAGE -> "相册贴图"
        PinHistorySourceType.CLIPBOARD_TEXT -> "剪贴板文字"
        PinHistorySourceType.OCR_TEXT -> "OCR 文字"
        PinHistorySourceType.EDITOR_EXPORT -> "编辑后贴图"
        PinHistorySourceType.RESTORED_PIN -> "历史恢复"
    }
}
