package com.muding.android.presentation.main

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.domain.usecase.FloatingBallAppearanceMode
import com.muding.android.domain.usecase.FloatingBallTheme
import com.muding.android.domain.usecase.PinScaleMode
import com.muding.android.presentation.translation.TranslationSettingsPage
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
    floatingBallAppearanceMode: FloatingBallAppearanceMode,
    floatingBallCustomImageUri: String?,
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
    onFloatingBallUseDefaultAppearance: () -> Unit,
    onFloatingBallCustomImageModeSelected: () -> Unit,
    onChooseFloatingBallCustomImage: () -> Unit,
    onPinHistoryEnabledChanged: (Boolean) -> Unit,
    onPinHistoryRetentionChanged: (Int, Int) -> Unit,
    onProjectRecordRetentionChanged: (Int, Int) -> Unit,
    onClearWorkRecords: () -> Unit,
    onResetApplication: () -> Unit
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
            selectedAction = selectedAction,
            floatingBallSizeDp = floatingBallSizeDp,
            floatingBallOpacity = floatingBallOpacity,
            floatingBallTheme = floatingBallTheme,
            floatingBallAppearanceMode = floatingBallAppearanceMode,
            floatingBallCustomImageUri = floatingBallCustomImageUri,
            onActionChanged = onActionChanged,
            onFloatingBallSizeChanged = onFloatingBallSizeChanged,
            onFloatingBallOpacityChanged = onFloatingBallOpacityChanged,
            onFloatingBallThemeChanged = onFloatingBallThemeChanged,
            onFloatingBallUseDefaultAppearance = onFloatingBallUseDefaultAppearance,
            onFloatingBallCustomImageModeSelected = onFloatingBallCustomImageModeSelected,
            onChooseFloatingBallCustomImage = onChooseFloatingBallCustomImage
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
            modifier = modifier
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
            onPinHistoryRetentionChanged = onPinHistoryRetentionChanged,
            onProjectRecordRetentionChanged = onProjectRecordRetentionChanged,
            onClearWorkRecords = onClearWorkRecords,
            onResetApplication = onResetApplication
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
    selectedAction: CaptureResultAction,
    floatingBallSizeDp: Int,
    floatingBallOpacity: Float,
    floatingBallTheme: FloatingBallTheme,
    floatingBallAppearanceMode: FloatingBallAppearanceMode,
    floatingBallCustomImageUri: String?,
    onActionChanged: (CaptureResultAction) -> Unit,
    onFloatingBallSizeChanged: (Int) -> Unit,
    onFloatingBallOpacityChanged: (Float) -> Unit,
    onFloatingBallThemeChanged: (FloatingBallTheme) -> Unit,
    onFloatingBallUseDefaultAppearance: () -> Unit,
    onFloatingBallCustomImageModeSelected: () -> Unit,
    onChooseFloatingBallCustomImage: () -> Unit
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
            var appearanceDraft by remember(floatingBallSizeDp, floatingBallOpacity) {
                mutableStateOf(
                    FloatingBallAppearanceDraft.fromCommitted(
                        sizeDp = floatingBallSizeDp,
                        opacity = floatingBallOpacity
                    )
                )
            }

            SettingGroup(title = "悬浮球外观") {
                FloatingBallAppearancePreview(
                    sizeDp = appearanceDraft.previewSizeDp,
                    opacity = appearanceDraft.previewOpacity,
                    theme = floatingBallTheme,
                    appearanceMode = floatingBallAppearanceMode,
                    customImageUri = floatingBallCustomImageUri
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelectablePill(
                        text = "默认样式",
                        selected = floatingBallAppearanceMode == FloatingBallAppearanceMode.THEME,
                        onClick = onFloatingBallUseDefaultAppearance
                    )
                    SelectablePill(
                        text = "自定义图片",
                        selected = floatingBallAppearanceMode == FloatingBallAppearanceMode.CUSTOM_IMAGE,
                        onClick = onFloatingBallCustomImageModeSelected
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "大小（${appearanceDraft.sizeProgress}/100）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = appearanceDraft.sizeProgress.toFloat(),
                        onValueChange = { progress ->
                            appearanceDraft = appearanceDraft.updateSizeProgress(progress.roundToInt())
                        },
                        onValueChangeFinished = {
                            appearanceDraft.sizeCommitOrNull(floatingBallSizeDp)
                                ?.let(onFloatingBallSizeChanged)
                        },
                        valueRange = 0f..100f,
                        steps = 99
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "透明度（${appearanceDraft.opacityPercent}%）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = appearanceDraft.opacityPercent.toFloat(),
                        onValueChange = { percent ->
                            appearanceDraft = appearanceDraft.updateOpacityPercent(percent.roundToInt())
                        },
                        onValueChangeFinished = {
                            appearanceDraft.opacityCommitOrNull(floatingBallOpacity)
                                ?.let(onFloatingBallOpacityChanged)
                        },
                        valueRange = 1f..100f,
                        steps = 98
                    )
                }
                if (floatingBallAppearanceMode == FloatingBallAppearanceMode.CUSTOM_IMAGE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onChooseFloatingBallCustomImage,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("选择图片")
                        }
                        OutlinedButton(
                            onClick = onFloatingBallUseDefaultAppearance,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("恢复默认样式")
                        }
                    }
                } else {
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
            var cornerRadiusDraft by remember(defaultPinCornerRadiusDp) {
                mutableStateOf(DeferredFloatSettingDraft.fromCommitted(defaultPinCornerRadiusDp))
            }

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
                        text = "圆角（${cornerRadiusDraft.previewValue.roundToInt()}dp）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = cornerRadiusDraft.previewValue,
                        onValueChange = {
                            cornerRadiusDraft = cornerRadiusDraft.update(it)
                        },
                        onValueChangeFinished = {
                            cornerRadiusDraft.commitOrNull(defaultPinCornerRadiusDp)
                                ?.let(onDefaultPinCornerRadiusChanged)
                        },
                        valueRange = 0f..48f
                    )
                }
            }
        }
    }
}

@Composable
private fun OcrAndTranslationSettingsSection(
    modifier: Modifier = Modifier
) {
    TranslationSettingsPage(
        modifier = modifier,
        title = SettingsSection.OCR_AND_TRANSLATION.title,
        description = "直接管理目标语言、本地模型和云翻译服务。"
    )
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
    onPinHistoryRetentionChanged: (Int, Int) -> Unit,
    onProjectRecordRetentionChanged: (Int, Int) -> Unit,
    onClearWorkRecords: () -> Unit,
    onResetApplication: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    val dialogState = remember { StorageMaintenanceDialogState() }
    var retentionSheet by remember { mutableStateOf<RecordRetentionSheetModel?>(null) }
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
                InlineValueRow("图片缓存", formatFileSize(snapshot.runtimeStorage.imageCacheBytes))
                InlineValueRow("工作记录", formatFileSize(snapshot.runtimeStorage.recordBytes))
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
                CompactSettingValueRow(
                    title = "贴图历史",
                    value = formatRecordRetentionSummary(
                        count = maxPinHistoryCount,
                        days = pinHistoryRetainDays,
                        itemUnit = RecordRetentionTarget.PIN_HISTORY.itemUnit
                    ),
                    onClick = {
                        retentionSheet = buildRecordRetentionSheetModel(
                            target = RecordRetentionTarget.PIN_HISTORY,
                            count = maxPinHistoryCount,
                            days = pinHistoryRetainDays
                        )
                    }
                )
                CompactSettingValueRow(
                    title = "工作记录",
                    value = formatRecordRetentionSummary(
                        count = maxSessionCount,
                        days = retainDays,
                        itemUnit = RecordRetentionTarget.WORK_RECORDS.itemUnit
                    ),
                    onClick = {
                        retentionSheet = buildRecordRetentionSheetModel(
                            target = RecordRetentionTarget.WORK_RECORDS,
                            count = maxSessionCount,
                            days = retainDays
                        )
                    }
                )
            }
        }

        item {
            SettingGroup(title = "清理操作") {
                StorageActionCard(
                    title = "清空工作记录",
                    description = "删除贴图历史、可继续编辑数据和临时图片缓存",
                    onClick = { dialogState.request(StorageMaintenanceAction.CLEAR_WORK_RECORDS) }
                )
                StorageActionCard(
                    title = "恢复初始状态",
                    description = "清空工作记录，并重置软件设置、翻译配置和本地翻译模型",
                    onClick = { dialogState.request(StorageMaintenanceAction.RESET_APPLICATION) },
                    danger = true
                )
            }
        }
    }

    retentionSheet?.let { sheetModel ->
        RecordRetentionBottomSheet(
            model = sheetModel,
            onDismiss = { retentionSheet = null },
            onApply = { count, days ->
                when (sheetModel.target) {
                    RecordRetentionTarget.PIN_HISTORY -> onPinHistoryRetentionChanged(count, days)
                    RecordRetentionTarget.WORK_RECORDS -> onProjectRecordRetentionChanged(count, days)
                }
                retentionSheet = null
            }
        )
    }

    if (dialogState.pendingAction != null) {
        AlertDialog(
            onDismissRequest = dialogState::dismiss,
            title = { Text(dialogState.dialogTitle.orEmpty()) },
            text = { Text(dialogState.dialogMessage.orEmpty()) },
            confirmButton = {
                Button(
                    onClick = {
                        dialogState.confirm { action ->
                            when (action) {
                                StorageMaintenanceAction.CLEAR_WORK_RECORDS -> onClearWorkRecords()
                                StorageMaintenanceAction.RESET_APPLICATION -> onResetApplication()
                            }
                        }
                    }
                ) {
                    Text(dialogState.confirmLabel.orEmpty())
                }
            },
            dismissButton = {
                OutlinedButton(onClick = dialogState::dismiss) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecordRetentionBottomSheet(
    model: RecordRetentionSheetModel,
    onDismiss: () -> Unit,
    onApply: (Int, Int) -> Unit
) {
    var pendingCount by remember(model) { mutableStateOf(model.count) }
    var pendingDays by remember(model) { mutableStateOf(model.days) }
    var editingCustomCount by remember(model) { mutableStateOf(model.count !in model.countOptions) }
    var editingCustomDays by remember(model) { mutableStateOf(model.days !in model.dayOptions) }
    val countConfig = remember(model.target) {
        buildRetentionCustomEditorConfig(
            target = model.target,
            field = RetentionCustomField.COUNT
        )
    }
    val dayConfig = remember(model.target) {
        buildRetentionCustomEditorConfig(
            target = model.target,
            field = RetentionCustomField.DAYS
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SectionHeader(
                title = model.target.title,
                description = "选择更合适的保留数量和保留天数。"
            )
            RetentionOptionSection(
                title = "保留数量",
                options = model.countOptions,
                selectedValue = pendingCount,
                suffix = model.target.itemUnit,
                isCustomSelected = editingCustomCount,
                onSelectPreset = {
                    pendingCount = it
                    editingCustomCount = false
                },
                onSelectCustom = { editingCustomCount = true }
            )
            if (editingCustomCount) {
                RetentionCustomEditor(
                    title = "自定义数量",
                    value = pendingCount,
                    config = countConfig,
                    onValueChange = { pendingCount = it }
                )
            }
            RetentionOptionSection(
                title = "保留天数",
                options = model.dayOptions,
                selectedValue = pendingDays,
                suffix = "天",
                isCustomSelected = editingCustomDays,
                onSelectPreset = {
                    pendingDays = it
                    editingCustomDays = false
                },
                onSelectCustom = { editingCustomDays = true }
            )
            if (editingCustomDays) {
                RetentionCustomEditor(
                    title = "自定义天数",
                    value = pendingDays,
                    config = dayConfig,
                    onValueChange = { pendingDays = it }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { onApply(pendingCount, pendingDays) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("完成")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RetentionOptionSection(
    title: String,
    options: List<Int>,
    selectedValue: Int,
    suffix: String,
    isCustomSelected: Boolean,
    onSelectPreset: (Int) -> Unit,
    onSelectCustom: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = rememberMainUiTokens().palette.title
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = !isCustomSelected && option == selectedValue,
                    onClick = { onSelectPreset(option) },
                    label = { Text("$option$suffix") }
                )
            }
            FilterChip(
                selected = isCustomSelected,
                onClick = onSelectCustom,
                label = { Text("自定义") }
            )
        }
    }
}

@Composable
private fun RetentionCustomEditor(
    title: String,
    value: Int,
    config: RetentionCustomEditorConfig,
    onValueChange: (Int) -> Unit
) {
    val tokens = rememberMainUiTokens()
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.corners.card),
        border = BorderStroke(1.dp, tokens.palette.outline),
        colors = CardDefaults.cardColors(containerColor = tokens.palette.surfaceMuted)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.palette.title
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = tokens.palette.surfaceAccent)
                ) {
                    Text(
                        text = "$value${config.suffix}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.palette.accent
                    )
                }
                RetentionAdjustButton(
                    label = "-",
                    onClick = {
                        onValueChange((value - 1).coerceAtLeast(config.valueRange.first))
                    }
                )
                RetentionAdjustButton(
                    label = "+",
                    onClick = {
                        onValueChange((value + 1).coerceAtMost(config.valueRange.last))
                    }
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = {
                    onValueChange(snapRetentionSliderValue(it, config.valueRange))
                },
                valueRange = config.valueRange.first.toFloat()..config.valueRange.last.toFloat(),
                steps = config.sliderSteps
            )
        }
    }
}

@Composable
private fun RetentionAdjustButton(
    label: String,
    onClick: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.CircleShape,
        border = BorderStroke(1.dp, tokens.palette.outline),
        colors = CardDefaults.cardColors(containerColor = tokens.palette.surfaceStrong)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = tokens.palette.body
            )
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
