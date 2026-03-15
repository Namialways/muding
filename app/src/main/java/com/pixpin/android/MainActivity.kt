package com.pixpin.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixpin.android.domain.usecase.AnnotationSessionStore
import com.pixpin.android.domain.usecase.CaptureFlowSettings
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.domain.usecase.PermissionHandler
import com.pixpin.android.domain.usecase.PinScaleMode
import com.pixpin.android.domain.usecase.RecentPinStore
import com.pixpin.android.presentation.theme.PixPinTheme
import com.pixpin.android.service.FloatingBallService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var captureFlowSettings: CaptureFlowSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHandler = PermissionHandler(this)
        captureFlowSettings = CaptureFlowSettings(this)
        AnnotationSessionStore.prune(
            this,
            maxCount = captureFlowSettings.getMaxSessionCount(),
            maxDays = captureFlowSettings.getRetainDays()
        )

        setContent {
            PixPinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        hasOverlayPermission = permissionHandler.hasOverlayPermission(),
                        initialAction = captureFlowSettings.getResultAction(),
                        initialScaleMode = captureFlowSettings.getPinScaleMode(),
                        initialMaxSessionCount = captureFlowSettings.getMaxSessionCount(),
                        initialRetainDays = captureFlowSettings.getRetainDays(),
                        initialPinShadowEnabled = captureFlowSettings.isPinShadowEnabledByDefault(),
                        recordsDirectory = AnnotationSessionStore.visibleDirectoryPath(this),
                        initialSessionFiles = AnnotationSessionStore.listSessionFiles(this),
                        initialRecentClosedPinCount = RecentPinStore.count(this),
                        onActionChanged = { action -> captureFlowSettings.setResultAction(action) },
                        onScaleModeChanged = { mode -> captureFlowSettings.setPinScaleMode(mode) },
                        onMaxSessionCountChanged = { count ->
                            captureFlowSettings.setMaxSessionCount(count)
                            AnnotationSessionStore.prune(
                                this,
                                maxCount = captureFlowSettings.getMaxSessionCount(),
                                maxDays = captureFlowSettings.getRetainDays()
                            )
                        },
                        onRetainDaysChanged = { days ->
                            captureFlowSettings.setRetainDays(days)
                            AnnotationSessionStore.prune(
                                this,
                                maxCount = captureFlowSettings.getMaxSessionCount(),
                                maxDays = captureFlowSettings.getRetainDays()
                            )
                        },
                        onDefaultPinShadowChanged = { enabled ->
                            captureFlowSettings.setPinShadowEnabledByDefault(enabled)
                        },
                        onClearAllRecords = {
                            AnnotationSessionStore.clearAll(this)
                            RecentPinStore.clear(this)
                        },
                        onRefreshRecords = {
                            AnnotationSessionStore.prune(
                                this,
                                maxCount = captureFlowSettings.getMaxSessionCount(),
                                maxDays = captureFlowSettings.getRetainDays()
                            )
                            MainScreenSnapshot(
                                sessionFiles = AnnotationSessionStore.listSessionFiles(this),
                                recentClosedPinCount = RecentPinStore.count(this)
                            )
                        },
                        onRequestPermission = {
                            permissionHandler.requestOverlayPermission(this)
                        },
                        onStartService = {
                            startFloatingBallService()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionHandler.hasOverlayPermission()) {
            startFloatingBallService()
        }
    }

    private fun startFloatingBallService() {
        val intent = Intent(this, FloatingBallService::class.java)
        startService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionHandler.REQUEST_CODE_OVERLAY_PERMISSION) {
            if (permissionHandler.hasOverlayPermission()) {
                startFloatingBallService()
                moveTaskToBack(true)
            }
        }
    }
}

data class MainScreenSnapshot(
    val sessionFiles: List<com.pixpin.android.domain.usecase.AnnotationSessionFile>,
    val recentClosedPinCount: Int
)

@Composable
fun MainScreen(
    hasOverlayPermission: Boolean,
    initialAction: CaptureResultAction,
    initialScaleMode: PinScaleMode,
    initialMaxSessionCount: Int,
    initialRetainDays: Int,
    initialPinShadowEnabled: Boolean,
    recordsDirectory: String,
    initialSessionFiles: List<com.pixpin.android.domain.usecase.AnnotationSessionFile>,
    initialRecentClosedPinCount: Int,
    onActionChanged: (CaptureResultAction) -> Unit,
    onScaleModeChanged: (PinScaleMode) -> Unit,
    onMaxSessionCountChanged: (Int) -> Unit,
    onRetainDaysChanged: (Int) -> Unit,
    onDefaultPinShadowChanged: (Boolean) -> Unit,
    onClearAllRecords: () -> Unit,
    onRefreshRecords: () -> MainScreenSnapshot,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit
) {
    var permissionGranted by remember { mutableStateOf(hasOverlayPermission) }
    var selectedAction by remember { mutableStateOf(initialAction) }
    var selectedScaleMode by remember { mutableStateOf(initialScaleMode) }
    var maxSessionCount by remember { mutableIntStateOf(initialMaxSessionCount) }
    var retainDays by remember { mutableIntStateOf(initialRetainDays) }
    var defaultPinShadowEnabled by remember { mutableStateOf(initialPinShadowEnabled) }
    var sessionFiles by remember { mutableStateOf(initialSessionFiles) }
    var recentClosedPinCount by remember { mutableIntStateOf(initialRecentClosedPinCount) }

    fun refreshRecords() {
        val snapshot = onRefreshRecords()
        sessionFiles = snapshot.sessionFiles
        recentClosedPinCount = snapshot.recentClosedPinCount
    }

    LaunchedEffect(hasOverlayPermission) {
        permissionGranted = hasOverlayPermission
        refreshRecords()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Icon(
                imageVector = if (permissionGranted) Icons.Default.Check else Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.height(80.dp),
                tint = if (permissionGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
        }

        item {
            Text(
                text = if (permissionGranted) "PixPin 已就绪" else "需要授予悬浮窗权限",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }

        item {
            Text(
                text = if (permissionGranted) {
                    "悬浮球正在运行，点击即可截图。"
                } else {
                    "请先授予悬浮窗权限，才能使用悬浮截图。"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "选区完成后的操作",
                        style = MaterialTheme.typography.titleSmall
                    )
                    CaptureOptionRow(
                        title = "直接贴图到屏幕",
                        selected = selectedAction == CaptureResultAction.PIN_DIRECTLY,
                        onSelect = {
                            selectedAction = CaptureResultAction.PIN_DIRECTLY
                            onActionChanged(CaptureResultAction.PIN_DIRECTLY)
                        }
                    )
                    CaptureOptionRow(
                        title = "直接进入编辑页",
                        selected = selectedAction == CaptureResultAction.OPEN_EDITOR,
                        onSelect = {
                            selectedAction = CaptureResultAction.OPEN_EDITOR
                            onActionChanged(CaptureResultAction.OPEN_EDITOR)
                        }
                    )
                    Divider()
                    Text(
                        text = "贴图缩放方式",
                        style = MaterialTheme.typography.titleSmall
                    )
                    CaptureOptionRow(
                        title = "等比例缩放",
                        selected = selectedScaleMode == PinScaleMode.LOCK_ASPECT,
                        onSelect = {
                            selectedScaleMode = PinScaleMode.LOCK_ASPECT
                            onScaleModeChanged(PinScaleMode.LOCK_ASPECT)
                        }
                    )
                    CaptureOptionRow(
                        title = "自由缩放（宽高独立）",
                        selected = selectedScaleMode == PinScaleMode.FREE_SCALE,
                        onSelect = {
                            selectedScaleMode = PinScaleMode.FREE_SCALE
                            onScaleModeChanged(PinScaleMode.FREE_SCALE)
                        }
                    )
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "贴图默认阴影",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (defaultPinShadowEnabled) "新贴图默认带阴影" else "新贴图默认不带阴影",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = defaultPinShadowEnabled,
                            onCheckedChange = {
                                defaultPinShadowEnabled = it
                                onDefaultPinShadowChanged(it)
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "工程记录保留策略",
                        style = MaterialTheme.typography.titleSmall
                    )
                    NumberSettingRow(
                        title = "最多保留 $maxSessionCount 个工程",
                        value = maxSessionCount,
                        valueRange = 1..500,
                        onDecrease = {
                            maxSessionCount = (maxSessionCount - 1).coerceAtLeast(1)
                            onMaxSessionCountChanged(maxSessionCount)
                            refreshRecords()
                        },
                        onIncrease = {
                            maxSessionCount = (maxSessionCount + 1).coerceAtMost(500)
                            onMaxSessionCountChanged(maxSessionCount)
                            refreshRecords()
                        },
                        onApply = { value ->
                            maxSessionCount = value
                            onMaxSessionCountChanged(value)
                            refreshRecords()
                        }
                    )
                    NumberSettingRow(
                        title = "只保留最近 $retainDays 天",
                        value = retainDays,
                        valueRange = 1..365,
                        onDecrease = {
                            retainDays = (retainDays - 1).coerceAtLeast(1)
                            onRetainDaysChanged(retainDays)
                            refreshRecords()
                        },
                        onIncrease = {
                            retainDays = (retainDays + 1).coerceAtMost(365)
                            onRetainDaysChanged(retainDays)
                            refreshRecords()
                        },
                        onApply = { value ->
                            retainDays = value
                            onRetainDaysChanged(value)
                            refreshRecords()
                        }
                    )
                    OutlinedButton(
                        onClick = {
                            onClearAllRecords()
                            refreshRecords()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text("清空全部工程记录")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = "可见记录目录",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = recordsDirectory,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "工程文件数：${sessionFiles.size}    已关闭贴图恢复队列：$recentClosedPinCount",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = { refreshRecords() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("刷新记录列表")
                    }
                }
            }
        }

        if (sessionFiles.isEmpty()) {
            item {
                Text(
                    text = "当前还没有保存的工程记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                Text(
                    text = "最近的工程文件",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(sessionFiles, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.file.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = formatTimestamp(item.lastModified),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            if (!permissionGranted) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(text = "授予权限")
                }
            } else {
                OutlinedButton(
                    onClick = onStartService,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(text = "重启悬浮球")
                }
            }
        }

        item {
            Text(
                text = "授权完成后，应用会自动进入后台。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NumberSettingRow(
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
            OutlinedButton(onClick = onDecrease) { Text("-") }
            OutlinedTextField(
                value = input,
                onValueChange = { next ->
                    if (next.all { it.isDigit() } && next.length <= 3) {
                        input = next
                    }
                },
                modifier = Modifier.width(88.dp),
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
            OutlinedButton(onClick = onIncrease) { Text("+") }
            OutlinedButton(
                onClick = {
                    input.toIntOrNull()?.coerceIn(valueRange)?.let(onApply)
                    focusManager.clearFocus()
                }
            ) {
                Text("设置")
            }
        }
        Text(
            "支持直接输入具体数字",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CaptureOptionRow(
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

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
