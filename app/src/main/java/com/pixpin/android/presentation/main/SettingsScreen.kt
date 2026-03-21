package com.pixpin.android.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
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
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.domain.usecase.FloatingBallTheme
import com.pixpin.android.domain.usecase.PinScaleMode
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("设置已重新分组", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "把高频工作流留在主页，把记录查看留在记录页，把低频配置拆成分类入口。后续继续扩功能时，这里也不会再堆成一整页。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SettingsCategoryCard(
                icon = Icons.Default.Camera,
                section = SettingsSection.CAPTURE_AND_FLOATING,
                summary = listOf(
                    if (permissionGranted) "悬浮权限已开启" else "悬浮权限待授权",
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
                    "OCR 结果页统一进入这里管理翻译能力",
                    "本地模型和百度/有道密钥都已单独隔离"
                ),
                onClick = { onOpenSection(SettingsSection.OCR_AND_TRANSLATION) }
            )
        }

        item {
            SettingsCategoryCard(
                icon = Icons.Default.Storage,
                section = SettingsSection.STORAGE_AND_RECORDS,
                summary = listOf(
                    if (pinHistoryEnabled) "贴图历史写入中" else "贴图历史已暂停写入",
                    "运行缓存 ${formatFileSize(snapshot.runtimeStorage.totalBytes)}"
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("截图结果去向", style = MaterialTheme.typography.titleMedium)
                    CaptureOptionRow(
                        title = "直接贴图到屏幕",
                        selected = selectedAction == CaptureResultAction.PIN_DIRECTLY,
                        onSelect = { onActionChanged(CaptureResultAction.PIN_DIRECTLY) }
                    )
                    CaptureOptionRow(
                        title = "直接进入编辑页",
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
                        text = if (permissionGranted) {
                            "悬浮窗权限已授权，你可以随时重启悬浮球。"
                        } else {
                            "请先完成悬浮窗授权，悬浮球和截图入口才能正常工作。"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!permissionGranted) {
                            Button(onClick = onRequestPermission, modifier = Modifier.weight(1f)) {
                                Text("授予权限")
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
                        Text("透明度（${(floatingBallOpacity * 100).toInt()}%）", style = MaterialTheme.typography.bodyMedium)
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("缩放与交互", style = MaterialTheme.typography.titleMedium)
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
                    Text(
                        text = "运行时交互已尽量收敛到极简模型，复杂编辑继续交给编辑器。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                text = if (defaultPinShadowEnabled) "新贴图默认带阴影" else "新贴图默认不带阴影",
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
                            text = if (defaultPinCornerRadiusDp <= 0.5f) {
                                "新贴图保持直角外观"
                            } else {
                                "新贴图默认使用圆角样式"
                            },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("OCR 与翻译", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "OCR 入口放在主页和悬浮球里，高级配置集中在这里管理。这样后续继续加 OCR 结果处理、翻译扩展时，不需要再把按钮塞回主页。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("贴图历史策略", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("启用贴图历史", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (pinHistoryEnabled) "已启用，新的贴图会进入历史记录。" else "已关闭，历史只读不再写入新记录。",
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
                        title = "最多保留 $maxPinHistoryCount 条贴图历史",
                        value = maxPinHistoryCount,
                        valueRange = 1..500,
                        onDecrease = { onMaxPinHistoryCountChanged((maxPinHistoryCount - 1).coerceAtLeast(1)) },
                        onIncrease = { onMaxPinHistoryCountChanged((maxPinHistoryCount + 1).coerceAtMost(500)) },
                        onApply = onMaxPinHistoryCountChanged
                    )
                    NumberSettingRow(
                        title = "只保留最近 $pinHistoryRetainDays 天的贴图历史",
                        value = pinHistoryRetainDays,
                        valueRange = 1..365,
                        onDecrease = { onPinHistoryRetainDaysChanged((pinHistoryRetainDays - 1).coerceAtLeast(1)) },
                        onIncrease = { onPinHistoryRetainDaysChanged((pinHistoryRetainDays + 1).coerceAtMost(365)) },
                        onApply = onPinHistoryRetainDaysChanged
                    )
                    Text(
                        text = "贴图历史目录：${snapshot.pinHistoryDirectory.ifBlank { "暂未生成" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onClearPinHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清空贴图历史")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("工程记录策略", style = MaterialTheme.typography.titleMedium)
                    NumberSettingRow(
                        title = "最多保留 $maxSessionCount 个工程",
                        value = maxSessionCount,
                        valueRange = 1..500,
                        onDecrease = { onMaxSessionCountChanged((maxSessionCount - 1).coerceAtLeast(1)) },
                        onIncrease = { onMaxSessionCountChanged((maxSessionCount + 1).coerceAtMost(500)) },
                        onApply = onMaxSessionCountChanged
                    )
                    NumberSettingRow(
                        title = "只保留最近 $retainDays 天",
                        value = retainDays,
                        valueRange = 1..365,
                        onDecrease = { onRetainDaysChanged((retainDays - 1).coerceAtLeast(1)) },
                        onIncrease = { onRetainDaysChanged((retainDays + 1).coerceAtMost(365)) },
                        onApply = onRetainDaysChanged
                    )
                    Text(
                        text = "工程目录：${snapshot.recordsDirectory.ifBlank { "暂未生成" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onClearAllRecords,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("清空全部记录")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("运行缓存清理", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "这些文件都属于应用运行时缓存，可主动删除，不会影响系统和相册里的正式图片。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("截图缓存：${formatFileSize(snapshot.runtimeStorage.screenshotsCacheBytes)}", style = MaterialTheme.typography.bodyMedium)
                    Text("贴图缓存：${formatFileSize(snapshot.runtimeStorage.pinnedCacheBytes)}", style = MaterialTheme.typography.bodyMedium)
                    Text("分享缓存：${formatFileSize(snapshot.runtimeStorage.shareCacheBytes)}", style = MaterialTheme.typography.bodyMedium)
                    Text("工程记录：${formatFileSize(snapshot.runtimeStorage.annotationSessionBytes)}", style = MaterialTheme.typography.bodyMedium)
                    Text("历史记录：${formatFileSize(snapshot.runtimeStorage.pinHistoryBytes)}", style = MaterialTheme.typography.bodyMedium)
                    Text("总占用：${formatFileSize(snapshot.runtimeStorage.totalBytes)}", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onClearImageCaches,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("清理图片缓存")
                        }
                        OutlinedButton(
                            onClick = onClearAllRecords,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("清理记录缓存")
                        }
                    }
                    OutlinedButton(
                        onClick = onClearAllRuntimeFiles,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清理全部运行缓存")
                    }
                }
            }
        }
    }
}
