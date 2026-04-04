package com.muding.android.presentation.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
            pinHistoryEnabled = pinHistoryEnabled,
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
    pinHistoryEnabled: Boolean,
    onOpenSection: (SettingsSection) -> Unit
) {
    val tokens = rememberMainUiTokens()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = tokens.spacing.pageGutter),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(
                title = "设置",
                description = "把常用配置收进更轻的入口里，只保留当前真正需要决策的状态。"
            )
        }

        item {
            SettingEntryRow(
                icon = Icons.Default.Camera,
                title = SettingsSection.CAPTURE_AND_FLOATING.title,
                value = captureSettingsSummary(
                    action = selectedAction,
                    permissionGranted = permissionGranted
                ),
                onClick = { onOpenSection(SettingsSection.CAPTURE_AND_FLOATING) }
            )
        }

        item {
            SettingEntryRow(
                icon = Icons.Default.Tune,
                title = SettingsSection.PIN_AND_INTERACTION.title,
                value = pinInteractionSettingsSummary(selectedScaleMode),
                onClick = { onOpenSection(SettingsSection.PIN_AND_INTERACTION) }
            )
        }

        item {
            SettingEntryRow(
                icon = Icons.Default.Translate,
                title = SettingsSection.OCR_AND_TRANSLATION.title,
                value = "本地与云翻译",
                onClick = { onOpenSection(SettingsSection.OCR_AND_TRANSLATION) }
            )
        }

        item {
            SettingEntryRow(
                icon = Icons.Default.Storage,
                title = SettingsSection.STORAGE_AND_RECORDS.title,
                value = storageSettingsSummary(pinHistoryEnabled),
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
    val tokens = rememberMainUiTokens()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = tokens.spacing.pageGutter),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(
                title = SettingsSection.CAPTURE_AND_FLOATING.title,
                description = "管理截图后的默认去向，以及悬浮球的显示方式。"
            )
        }

        item {
            SettingGroup(title = "截图结果") {
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

        item {
            SettingGroup(title = "运行状态") {
                InlineValueRow(
                    label = "悬浮窗权限",
                    value = if (permissionGranted) "已开启" else "未授权"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!permissionGranted) {
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("去授权")
                        }
                    }
                    OutlinedButton(
                        onClick = onStartService,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (permissionGranted) "刷新悬浮球" else "稍后重试")
                    }
                }
            }
        }

        item {
            SettingGroup(title = "悬浮球外观") {
                FloatingBallAppearancePreview(
                    sizeDp = floatingBallSizeDp,
                    opacity = floatingBallOpacity,
                    theme = floatingBallTheme
                )
                NumberSettingRow(
                    title = "大小（${floatingBallSizeDp}dp）",
                    value = floatingBallSizeDp,
                    valueRange = 44..96,
                    onDecrease = { onFloatingBallSizeChanged((floatingBallSizeDp - 2).coerceAtLeast(44)) },
                    onIncrease = { onFloatingBallSizeChanged((floatingBallSizeDp + 2).coerceAtMost(96)) },
                    onApply = { onFloatingBallSizeChanged(it.coerceIn(44, 96)) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "透明度（${(floatingBallOpacity * 100).toInt()}%）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = floatingBallOpacity,
                        onValueChange = onFloatingBallOpacityChanged,
                        valueRange = 0.4f..1f
                    )
                }
                CaptureOptionRow(
                    title = "蓝紫渐变",
                    selected = floatingBallTheme == FloatingBallTheme.BLUE_PURPLE,
                    onSelect = { onFloatingBallThemeChanged(FloatingBallTheme.BLUE_PURPLE) }
                )
                CaptureOptionRow(
                    title = "落日橙红",
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
    val tokens = rememberMainUiTokens()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = tokens.spacing.pageGutter),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(
                title = SettingsSection.PIN_AND_INTERACTION.title,
                description = "控制贴图默认的缩放方式和基础外观。"
            )
        }

        item {
            SettingGroup(title = "缩放方式") {
                CaptureOptionRow(
                    title = "等比缩放",
                    selected = selectedScaleMode == PinScaleMode.LOCK_ASPECT,
                    onSelect = { onScaleModeChanged(PinScaleMode.LOCK_ASPECT) }
                )
                CaptureOptionRow(
                    title = "自由缩放",
                    selected = selectedScaleMode == PinScaleMode.FREE_SCALE,
                    onSelect = { onScaleModeChanged(PinScaleMode.FREE_SCALE) }
                )
            }
        }

        item {
            SettingGroup(title = "默认外观") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("默认阴影", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = defaultPinShadowEnabled,
                        onCheckedChange = onDefaultPinShadowChanged
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "圆角（${defaultPinCornerRadiusDp.roundToInt()}dp）",
                        style = MaterialTheme.typography.bodyMedium
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

@Composable
private fun OcrAndTranslationSettingsSection(
    modifier: Modifier = Modifier,
    onOpenTranslationSettings: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = tokens.spacing.pageGutter),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(
                title = SettingsSection.OCR_AND_TRANSLATION.title,
                description = "把 OCR 和翻译配置收进一个入口，不在这里重复展示细节。"
            )
        }

        item {
            SettingGroup(title = "翻译设置") {
                Text(
                    text = "管理本地模型、云翻译密钥和默认引擎。",
                    style = MaterialTheme.typography.bodySmall,
                    color = rememberMainUiTokens().palette.body
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
    val tokens = rememberMainUiTokens()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = tokens.spacing.pageGutter),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            SectionHeader(
                title = SettingsSection.STORAGE_AND_RECORDS.title,
                description = "只保留和清理、保留策略直接相关的数据。"
            )
        }

        item {
            SettingGroup(title = "存储占用") {
                InlineValueRow("截图缓存", formatFileSize(snapshot.runtimeStorage.screenshotsCacheBytes))
                InlineValueRow("贴图缓存", formatFileSize(snapshot.runtimeStorage.pinnedCacheBytes))
                InlineValueRow("分享缓存", formatFileSize(snapshot.runtimeStorage.shareCacheBytes))
                InlineValueRow(
                    "记录文件",
                    formatFileSize(snapshot.runtimeStorage.annotationSessionBytes + snapshot.runtimeStorage.pinHistoryBytes)
                )
                InlineValueRow("总占用", formatFileSize(snapshot.runtimeStorage.totalBytes))
            }
        }

        item {
            SettingGroup(title = "记录保留") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("写入贴图历史", style = MaterialTheme.typography.bodyMedium)
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
                NumberSettingRow(
                    title = "工程记录最多保留 $maxSessionCount 项",
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

        item {
            SettingGroup(title = "清理操作") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StorageActionCard(
                        modifier = Modifier.weight(1f),
                        title = "清理图片缓存",
                        description = "清空截图、贴图和分享缓存",
                        onClick = onClearImageCaches
                    )
                    StorageActionCard(
                        modifier = Modifier.weight(1f),
                        title = "清空贴图历史",
                        description = "只删除贴图历史记录",
                        onClick = onClearPinHistory
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StorageActionCard(
                        modifier = Modifier.weight(1f),
                        title = "清空全部记录",
                        description = "删除贴图历史和工程记录",
                        onClick = onClearAllRecords,
                        danger = true
                    )
                    StorageActionCard(
                        modifier = Modifier.weight(1f),
                        title = "清理全部缓存",
                        description = "删除运行时生成的全部缓存",
                        onClick = onClearAllRuntimeFiles,
                        danger = true
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageActionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val tokens = rememberMainUiTokens()
    Card(
        modifier = modifier.clickable(onClick = onClick),
        border = BorderStroke(1.dp, tokens.palette.outline),
        colors = CardDefaults.cardColors(
            containerColor = if (danger) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.70f)
            } else {
                tokens.palette.surfaceMuted
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = if (danger) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        tokens.palette.accent
                    }
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (danger) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        tokens.palette.title
                    }
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (danger) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    tokens.palette.body
                }
            )
        }
    }
}
