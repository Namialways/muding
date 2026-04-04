package com.muding.android.presentation.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.domain.usecase.FloatingBallTheme
import com.muding.android.domain.usecase.PinScaleMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    selectedSection: SettingsSection?,
    permissionGranted: Boolean,
    selectedAction: CaptureResultAction,
    selectedScaleMode: PinScaleMode,
    defaultPinShadowEnabled: Boolean,
    defaultPinCornerRadiusDp: Float,
    floatingBallSizeDp: Int,
    floatingBallOpacity: Float,
    floatingBallTheme: FloatingBallTheme,
    pinHistoryEnabled: Boolean,
    maxPinHistoryCount: Int,
    pinHistoryRetainDays: Int,
    maxSessionCount: Int,
    retainDays: Int,
    snapshot: MainScreenSnapshot,
    onOpenSection: (SettingsSection) -> Unit,
    onActionChanged: (CaptureResultAction) -> Unit,
    onScaleModeChanged: (PinScaleMode) -> Unit,
    onDefaultPinShadowChanged: (Boolean) -> Unit,
    onDefaultPinCornerRadiusChanged: (Float) -> Unit,
    onFloatingBallSizeChanged: (Int) -> Unit,
    onFloatingBallOpacityChanged: (Float) -> Unit,
    onFloatingBallThemeChanged: (FloatingBallTheme) -> Unit,
    onPinHistoryEnabledChanged: (Boolean) -> Unit,
    onMaxPinHistoryCountChanged: (Int) -> Unit,
    onPinHistoryRetainDaysChanged: (Int) -> Unit,
    onMaxSessionCountChanged: (Int) -> Unit,
    onRetainDaysChanged: (Int) -> Unit,
    onClearPinHistory: () -> Unit,
    onClearAllRecords: () -> Unit,
    onClearImageCaches: () -> Unit,
    onClearAllRuntimeFiles: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenTranslationSettings: () -> Unit,
    onStartService: () -> Unit
) {
    when (selectedSection) {
        null -> SettingsOverviewScreen(
            modifier = modifier,
            permissionGranted = permissionGranted,
            selectedAction = selectedAction,
            selectedScaleMode = selectedScaleMode,
            floatingBallTheme = floatingBallTheme,
            pinHistoryEnabled = pinHistoryEnabled,
            snapshot = snapshot,
            onOpenSection = onOpenSection
        )

        SettingsSection.CAPTURE_AND_FLOATING -> CaptureAndFloatingSettingsSection(
            modifier = modifier,
            permissionGranted = permissionGranted,
            selectedAction = selectedAction,
            floatingBallSizeDp = floatingBallSizeDp,
            floatingBallOpacity = floatingBallOpacity,
            floatingBallTheme = floatingBallTheme,
            onActionChanged = onActionChanged,
            onFloatingBallSizeChanged = onFloatingBallSizeChanged,
            onFloatingBallOpacityChanged = onFloatingBallOpacityChanged,
            onFloatingBallThemeChanged = onFloatingBallThemeChanged,
            onRequestPermission = onRequestPermission,
            onStartService = onStartService
        )

        SettingsSection.PIN_AND_INTERACTION -> PinAndInteractionSettingsSection(
            modifier = modifier,
            selectedScaleMode = selectedScaleMode,
            defaultPinShadowEnabled = defaultPinShadowEnabled,
            defaultPinCornerRadiusDp = defaultPinCornerRadiusDp,
            onScaleModeChanged = onScaleModeChanged,
            onDefaultPinShadowChanged = onDefaultPinShadowChanged,
            onDefaultPinCornerRadiusChanged = onDefaultPinCornerRadiusChanged
        )

        SettingsSection.OCR_AND_TRANSLATION -> OcrAndTranslationSettingsSection(
            modifier = modifier,
            onOpenTranslationSettings = onOpenTranslationSettings
        )

        SettingsSection.STORAGE_AND_RECORDS -> StorageAndRecordsSettingsSection(
            modifier = modifier,
            pinHistoryEnabled = pinHistoryEnabled,
            maxPinHistoryCount = maxPinHistoryCount,
            pinHistoryRetainDays = pinHistoryRetainDays,
            maxSessionCount = maxSessionCount,
            retainDays = retainDays,
            snapshot = snapshot,
            onPinHistoryEnabledChanged = onPinHistoryEnabledChanged,
            onMaxPinHistoryCountChanged = onMaxPinHistoryCountChanged,
            onPinHistoryRetainDaysChanged = onPinHistoryRetainDaysChanged,
            onMaxSessionCountChanged = onMaxSessionCountChanged,
            onRetainDaysChanged = onRetainDaysChanged,
            onClearPinHistory = onClearPinHistory,
            onClearAllRecords = onClearAllRecords,
            onClearImageCaches = onClearImageCaches,
            onClearAllRuntimeFiles = onClearAllRuntimeFiles
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsOverviewScreen(
    modifier: Modifier = Modifier,
    permissionGranted: Boolean,
    selectedAction: CaptureResultAction,
    selectedScaleMode: PinScaleMode,
    floatingBallTheme: FloatingBallTheme,
    pinHistoryEnabled: Boolean,
    snapshot: MainScreenSnapshot,
    onOpenSection: (SettingsSection) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("设置中心", style = MaterialTheme.typography.headlineSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryPill(
                            text = if (permissionGranted) "悬浮权限已开启" else "悬浮权限待授权",
                            emphasized = true
                        )
                        SummaryPill(
                            text = if (pinHistoryEnabled) "贴图历史写入中" else "贴图历史已暂停"
                        )
                        SummaryPill(text = "缓存 ${formatFileSize(snapshot.runtimeStorage.totalBytes)}")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "贴图记录",
                    value = snapshot.pinHistoryRecords.size.toString(),
                    hint = "当前历史"
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "工程记录",
                    value = snapshot.sessionFileCount.toString(),
                    hint = "当前工程"
                )
            }
        }

        item {
            SettingsCategoryCard(
                icon = Icons.Default.Camera,
                section = SettingsSection.CAPTURE_AND_FLOATING,
                summary = listOf(
                    if (permissionGranted) "悬浮球可用" else "需要先授权",
                    if (selectedAction == CaptureResultAction.PIN_DIRECTLY) "截图后直接贴图" else "截图后进入编辑"
                ),
                onClick = { onOpenSection(SettingsSection.CAPTURE_AND_FLOATING) }
            )
        }

        item {
            SettingsCategoryCard(
                icon = Icons.Default.Tune,
                section = SettingsSection.PIN_AND_INTERACTION,
                summary = listOf(
                    if (selectedScaleMode == PinScaleMode.LOCK_ASPECT) "默认等比例缩放" else "默认自由缩放",
                    "悬浮球主题：${floatingBallThemeLabel(floatingBallTheme)}"
                ),
                onClick = { onOpenSection(SettingsSection.PIN_AND_INTERACTION) }
            )
        }

        item {
            SettingsCategoryCard(
                icon = Icons.Default.Translate,
                section = SettingsSection.OCR_AND_TRANSLATION,
                summary = listOf(
                    "OCR 与翻译设置",
                    "本地模型和云翻译分开管理"
                ),
                onClick = { onOpenSection(SettingsSection.OCR_AND_TRANSLATION) }
            )
        }

        item {
            SettingsCategoryCard(
                icon = Icons.Default.Storage,
                section = SettingsSection.STORAGE_AND_RECORDS,
                summary = listOf(
                    if (pinHistoryEnabled) "历史策略已开启" else "历史策略已暂停",
                    "总占用 ${formatFileSize(snapshot.runtimeStorage.totalBytes)}"
                ),
                onClick = { onOpenSection(SettingsSection.STORAGE_AND_RECORDS) }
            )
        }
    }
}

@Composable
private fun CaptureAndFloatingSettingsSection(
    modifier: Modifier = Modifier,
    permissionGranted: Boolean,
    selectedAction: CaptureResultAction,
    floatingBallSizeDp: Int,
    floatingBallOpacity: Float,
    floatingBallTheme: FloatingBallTheme,
    onActionChanged: (CaptureResultAction) -> Unit,
    onFloatingBallSizeChanged: (Int) -> Unit,
    onFloatingBallOpacityChanged: (Float) -> Unit,
    onFloatingBallThemeChanged: (FloatingBallTheme) -> Unit,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(title = "截图与悬浮球")
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("截图结果去向", style = MaterialTheme.typography.titleMedium)
                    CaptureOptionRow(
                        title = "截图后直接贴图到屏幕",
                        selected = selectedAction == CaptureResultAction.PIN_DIRECTLY,
                        onSelect = { onActionChanged(CaptureResultAction.PIN_DIRECTLY) }
                    )
                    CaptureOptionRow(
                        title = "截图后进入编辑器",
                        selected = selectedAction == CaptureResultAction.OPEN_EDITOR,
                        onSelect = { onActionChanged(CaptureResultAction.OPEN_EDITOR) }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("权限与运行状态", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (permissionGranted) "悬浮窗权限已开启。" else "请先完成悬浮窗授权。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!permissionGranted) {
                            Button(onClick = onRequestPermission, modifier = Modifier.weight(1f)) {
                                Text("去授权")
                            }
                        }
                        OutlinedButton(onClick = onStartService, modifier = Modifier.weight(1f)) {
                            Text("重启悬浮球")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("悬浮球外观", style = MaterialTheme.typography.titleMedium)
                    FloatingBallAppearancePreview(
                        sizeDp = floatingBallSizeDp,
                        opacity = floatingBallOpacity,
                        theme = floatingBallTheme
                    )
                    NumberSettingRow(
                        title = "悬浮球大小（${floatingBallSizeDp}dp）",
                        value = floatingBallSizeDp,
                        valueRange = 44..96,
                        onDecrease = { onFloatingBallSizeChanged((floatingBallSizeDp - 2).coerceAtLeast(44)) },
                        onIncrease = { onFloatingBallSizeChanged((floatingBallSizeDp + 2).coerceAtMost(96)) },
                        onApply = { onFloatingBallSizeChanged(it.coerceIn(44, 96)) }
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "透明度（${(floatingBallOpacity * 100).toInt()}%）",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = floatingBallOpacity,
                            onValueChange = onFloatingBallOpacityChanged,
                            valueRange = 0.4f..1f
                        )
                    }
                    Divider()
                    Text("主题配色", style = MaterialTheme.typography.titleSmall)
                    CaptureOptionRow(
                        title = "蓝紫渐变",
                        selected = floatingBallTheme == FloatingBallTheme.BLUE_PURPLE,
                        onSelect = { onFloatingBallThemeChanged(FloatingBallTheme.BLUE_PURPLE) }
                    )
                    CaptureOptionRow(
                        title = "日落橙红",
                        selected = floatingBallTheme == FloatingBallTheme.SUNSET,
                        onSelect = { onFloatingBallThemeChanged(FloatingBallTheme.SUNSET) }
                    )
                    CaptureOptionRow(
                        title = "青绿渐变",
                        selected = floatingBallTheme == FloatingBallTheme.EMERALD,
                        onSelect = { onFloatingBallThemeChanged(FloatingBallTheme.EMERALD) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinAndInteractionSettingsSection(
    modifier: Modifier = Modifier,
    selectedScaleMode: PinScaleMode,
    defaultPinShadowEnabled: Boolean,
    defaultPinCornerRadiusDp: Float,
    onScaleModeChanged: (PinScaleMode) -> Unit,
    onDefaultPinShadowChanged: (Boolean) -> Unit,
    onDefaultPinCornerRadiusChanged: (Float) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(title = "贴图与交互")
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("缩放模型", style = MaterialTheme.typography.titleMedium)
                    CaptureOptionRow(
                        title = "等比例缩放",
                        selected = selectedScaleMode == PinScaleMode.LOCK_ASPECT,
                        onSelect = { onScaleModeChanged(PinScaleMode.LOCK_ASPECT) }
                    )
                    CaptureOptionRow(
                        title = "自由缩放（宽高独立）",
                        selected = selectedScaleMode == PinScaleMode.FREE_SCALE,
                        onSelect = { onScaleModeChanged(PinScaleMode.FREE_SCALE) }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("贴图默认阴影", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (defaultPinShadowEnabled) "新贴图默认带阴影。" else "新贴图默认不带阴影。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = defaultPinShadowEnabled,
                            onCheckedChange = onDefaultPinShadowChanged
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "默认圆角（${defaultPinCornerRadiusDp.roundToInt()}dp）",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (defaultPinCornerRadiusDp <= 0.5f) "当前接近直角外观。" else "当前默认使用圆角外观。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = defaultPinCornerRadiusDp,
                            onValueChange = onDefaultPinCornerRadiusChanged,
                            valueRange = 0f..48f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrAndTranslationSettingsSection(
    modifier: Modifier = Modifier,
    onOpenTranslationSettings: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(title = "OCR 与翻译")
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("翻译设置", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = onOpenTranslationSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打开翻译设置")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorageAndRecordsSettingsSection(
    modifier: Modifier = Modifier,
    pinHistoryEnabled: Boolean,
    maxPinHistoryCount: Int,
    pinHistoryRetainDays: Int,
    maxSessionCount: Int,
    retainDays: Int,
    snapshot: MainScreenSnapshot,
    onPinHistoryEnabledChanged: (Boolean) -> Unit,
    onMaxPinHistoryCountChanged: (Int) -> Unit,
    onPinHistoryRetainDaysChanged: (Int) -> Unit,
    onMaxSessionCountChanged: (Int) -> Unit,
    onRetainDaysChanged: (Int) -> Unit,
    onClearPinHistory: () -> Unit,
    onClearAllRecords: () -> Unit,
    onClearImageCaches: () -> Unit,
    onClearAllRuntimeFiles: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(title = "存储与记录")
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "截图缓存",
                    value = formatFileSize(snapshot.runtimeStorage.screenshotsCacheBytes),
                    hint = "临时截图"
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "贴图缓存",
                    value = formatFileSize(snapshot.runtimeStorage.pinnedCacheBytes),
                    hint = "运行贴图"
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "分享缓存",
                    value = formatFileSize(snapshot.runtimeStorage.shareCacheBytes),
                    hint = "导出与分享"
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "记录文件",
                    value = formatFileSize(
                        snapshot.runtimeStorage.annotationSessionBytes + snapshot.runtimeStorage.pinHistoryBytes
                    ),
                    hint = "历史与工程"
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("记录保留", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryPill(
                            text = if (pinHistoryEnabled) "贴图历史开启" else "贴图历史关闭",
                            emphasized = true
                        )
                        SummaryPill(text = "贴图记录 ${snapshot.pinHistoryRecords.size}")
                        SummaryPill(text = "工程记录 ${snapshot.sessionFileCount}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("贴图历史写入", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (pinHistoryEnabled) "新贴图会继续进入历史。" else "暂停写入，只保留已有记录。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = pinHistoryEnabled,
                            onCheckedChange = onPinHistoryEnabledChanged
                        )
                    }
                    NumberSettingRow(
                        title = "贴图历史最多保留 $maxPinHistoryCount 条",
                        value = maxPinHistoryCount,
                        valueRange = 1..500,
                        onDecrease = { onMaxPinHistoryCountChanged((maxPinHistoryCount - 1).coerceAtLeast(1)) },
                        onIncrease = { onMaxPinHistoryCountChanged((maxPinHistoryCount + 1).coerceAtMost(500)) },
                        onApply = onMaxPinHistoryCountChanged
                    )
                    NumberSettingRow(
                        title = "贴图历史保留最近 $pinHistoryRetainDays 天",
                        value = pinHistoryRetainDays,
                        valueRange = 1..365,
                        onDecrease = { onPinHistoryRetainDaysChanged((pinHistoryRetainDays - 1).coerceAtLeast(1)) },
                        onIncrease = { onPinHistoryRetainDaysChanged((pinHistoryRetainDays + 1).coerceAtMost(365)) },
                        onApply = onPinHistoryRetainDaysChanged
                    )
                    Divider()
                    NumberSettingRow(
                        title = "工程记录最多保留 $maxSessionCount 个",
                        value = maxSessionCount,
                        valueRange = 1..500,
                        onDecrease = { onMaxSessionCountChanged((maxSessionCount - 1).coerceAtLeast(1)) },
                        onIncrease = { onMaxSessionCountChanged((maxSessionCount + 1).coerceAtMost(500)) },
                        onApply = onMaxSessionCountChanged
                    )
                    NumberSettingRow(
                        title = "工程记录保留最近 $retainDays 天",
                        value = retainDays,
                        valueRange = 1..365,
                        onDecrease = { onRetainDaysChanged((retainDays - 1).coerceAtLeast(1)) },
                        onIncrease = { onRetainDaysChanged((retainDays + 1).coerceAtMost(365)) },
                        onApply = onRetainDaysChanged
                    )
                }
            }
        }

        item {
            Text("清理操作", style = MaterialTheme.typography.titleMedium)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StorageActionCard(
                    modifier = Modifier.weight(1f),
                    title = "清理图片缓存",
                    description = "截图、贴图、分享缓存",
                    onClick = onClearImageCaches
                )
                StorageActionCard(
                    modifier = Modifier.weight(1f),
                    title = "清空贴图历史",
                    description = "只删除历史记录",
                    onClick = onClearPinHistory
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StorageActionCard(
                    modifier = Modifier.weight(1f),
                    title = "清空全部记录",
                    description = "删除历史和工程",
                    onClick = onClearAllRecords,
                    danger = true
                )
                StorageActionCard(
                    modifier = Modifier.weight(1f),
                    title = "清理全部缓存",
                    description = "删除运行时文件",
                    onClick = onClearAllRuntimeFiles,
                    danger = true
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StorageActionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (danger) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (danger) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
