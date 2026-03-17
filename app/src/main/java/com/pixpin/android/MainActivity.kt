package com.pixpin.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Slider
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixpin.android.domain.usecase.AnnotationSessionFile
import com.pixpin.android.domain.usecase.AnnotationSessionStore
import com.pixpin.android.domain.usecase.CaptureFlowSettings
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.domain.usecase.FloatingBallTheme
import com.pixpin.android.domain.usecase.PermissionHandler
import com.pixpin.android.domain.usecase.PinHistoryRecord
import com.pixpin.android.domain.usecase.PinHistorySourceType
import com.pixpin.android.domain.usecase.PinHistoryStore
import com.pixpin.android.domain.usecase.PinScaleMode
import com.pixpin.android.domain.usecase.RecentPinStore
import com.pixpin.android.domain.usecase.RuntimeStorageManager
import com.pixpin.android.domain.usecase.RuntimeStorageSnapshot
import com.pixpin.android.presentation.editor.AnnotationEditorActivity
import com.pixpin.android.presentation.theme.floatingBallThemeColors
import com.pixpin.android.presentation.theme.PixPinTheme
import com.pixpin.android.service.FloatingBallService
import com.pixpin.android.service.PinOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                        initialFloatingBallSizeDp = captureFlowSettings.getFloatingBallSizeDp(),
                        initialFloatingBallOpacity = captureFlowSettings.getFloatingBallOpacity(),
                        initialFloatingBallTheme = captureFlowSettings.getFloatingBallTheme(),
                        initialPinHistoryEnabled = captureFlowSettings.isPinHistoryEnabled(),
                        initialMaxPinHistoryCount = captureFlowSettings.getMaxPinHistoryCount(),
                        initialPinHistoryRetainDays = captureFlowSettings.getPinHistoryRetainDays(),
                        initialSnapshot = MainScreenSnapshot.empty(),
                        onActionChanged = { action -> captureFlowSettings.setResultAction(action) },
                        onScaleModeChanged = { mode -> captureFlowSettings.setPinScaleMode(mode) },
                        onMaxSessionCountChanged = { count ->
                            captureFlowSettings.setMaxSessionCount(count)
                            pruneRecords()
                        },
                        onRetainDaysChanged = { days ->
                            captureFlowSettings.setRetainDays(days)
                            pruneRecords()
                        },
                        onDefaultPinShadowChanged = { enabled ->
                            captureFlowSettings.setPinShadowEnabledByDefault(enabled)
                        },
                        onFloatingBallSizeChanged = { size ->
                            captureFlowSettings.setFloatingBallSizeDp(size)
                            refreshFloatingBallAppearance()
                        },
                        onFloatingBallOpacityChanged = { opacity ->
                            captureFlowSettings.setFloatingBallOpacity(opacity)
                            refreshFloatingBallAppearance()
                        },
                        onFloatingBallThemeChanged = { theme ->
                            captureFlowSettings.setFloatingBallTheme(theme)
                            refreshFloatingBallAppearance()
                        },
                        onPinHistoryEnabledChanged = { enabled ->
                            captureFlowSettings.setPinHistoryEnabled(enabled)
                        },
                        onMaxPinHistoryCountChanged = { count ->
                            captureFlowSettings.setMaxPinHistoryCount(count)
                            pruneRecords()
                        },
                        onPinHistoryRetainDaysChanged = { days ->
                            captureFlowSettings.setPinHistoryRetainDays(days)
                            pruneRecords()
                        },
                        onClearAllRecords = {
                            AnnotationSessionStore.clearAll(this)
                            RecentPinStore.clear(this)
                            PinHistoryStore.clear(this)
                        },
                        onClearImageCaches = {
                            RuntimeStorageManager.clearImageCaches(this)
                        },
                        onClearAllRuntimeFiles = {
                            RuntimeStorageManager.clearAllRuntimeFiles(this)
                        },
                        onClearPinHistory = {
                            PinHistoryStore.clear(this)
                        },
                        onDeleteHistory = { record ->
                            PinHistoryStore.delete(this, record.id)
                        },
                        onRestoreHistory = { record ->
                            startService(
                                Intent(this, PinOverlayService::class.java).apply {
                                    putExtra(PinOverlayService.EXTRA_IMAGE_URI, record.imageUri)
                                    putExtra(PinOverlayService.EXTRA_ANNOTATION_SESSION_ID, record.annotationSessionId)
                                    putExtra(
                                        PinOverlayService.EXTRA_HISTORY_SOURCE,
                                        PinHistorySourceType.RESTORED_PIN.value
                                    )
                                }
                            )
                        },
                        onEditHistory = { record ->
                            startActivity(
                                Intent(this, AnnotationEditorActivity::class.java).apply {
                                    if (!record.annotationSessionId.isNullOrBlank()) {
                                        putExtra(
                                            AnnotationEditorActivity.EXTRA_ANNOTATION_SESSION_ID,
                                            record.annotationSessionId
                                        )
                                    } else {
                                        putExtra(AnnotationEditorActivity.EXTRA_IMAGE_URI, record.imageUri)
                                    }
                                }
                            )
                        },
                        onRefreshRecords = {
                            pruneRecords()
                            buildSnapshot()
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

    private fun pruneRecords() {
        AnnotationSessionStore.prune(
            this,
            maxCount = captureFlowSettings.getMaxSessionCount(),
            maxDays = captureFlowSettings.getRetainDays()
        )
        PinHistoryStore.prune(
            this,
            maxCount = captureFlowSettings.getMaxPinHistoryCount(),
            maxDays = captureFlowSettings.getPinHistoryRetainDays()
        )
    }

    private fun buildSnapshot(): MainScreenSnapshot {
        return MainScreenSnapshot(
            sessionFiles = AnnotationSessionStore.listSessionFiles(this),
            recentClosedPinCount = RecentPinStore.count(this),
            pinHistoryRecords = PinHistoryStore.list(this),
            recordsDirectory = AnnotationSessionStore.visibleDirectoryPath(this),
            pinHistoryDirectory = PinHistoryStore.visibleDirectoryPath(this),
            runtimeStorage = RuntimeStorageManager.snapshot(this)
        )
    }

    private fun startFloatingBallService() {
        val intent = Intent(this, FloatingBallService::class.java)
        startService(intent)
    }

    private fun refreshFloatingBallAppearance() {
        if (!permissionHandler.hasOverlayPermission()) {
            return
        }
        startService(
            Intent(this, FloatingBallService::class.java).apply {
                action = FloatingBallService.ACTION_REFRESH_FLOATING_BALL_APPEARANCE
            }
        )
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

private enum class SettingsTab {
    BASIC,
    HISTORY
}

data class MainScreenSnapshot(
    val sessionFiles: List<AnnotationSessionFile>,
    val recentClosedPinCount: Int,
    val pinHistoryRecords: List<PinHistoryRecord>,
    val recordsDirectory: String,
    val pinHistoryDirectory: String,
    val runtimeStorage: RuntimeStorageSnapshot
) {
    companion object {
        fun empty(): MainScreenSnapshot {
            return MainScreenSnapshot(
                sessionFiles = emptyList(),
                recentClosedPinCount = 0,
                pinHistoryRecords = emptyList(),
                recordsDirectory = "",
                pinHistoryDirectory = "",
                runtimeStorage = RuntimeStorageSnapshot(0, 0, 0, 0, 0)
            )
        }
    }
}

@Composable
private fun MainScreen(
    hasOverlayPermission: Boolean,
    initialAction: CaptureResultAction,
    initialScaleMode: PinScaleMode,
    initialMaxSessionCount: Int,
    initialRetainDays: Int,
    initialPinShadowEnabled: Boolean,
    initialFloatingBallSizeDp: Int,
    initialFloatingBallOpacity: Float,
    initialFloatingBallTheme: FloatingBallTheme,
    initialPinHistoryEnabled: Boolean,
    initialMaxPinHistoryCount: Int,
    initialPinHistoryRetainDays: Int,
    initialSnapshot: MainScreenSnapshot,
    onActionChanged: (CaptureResultAction) -> Unit,
    onScaleModeChanged: (PinScaleMode) -> Unit,
    onMaxSessionCountChanged: (Int) -> Unit,
    onRetainDaysChanged: (Int) -> Unit,
    onDefaultPinShadowChanged: (Boolean) -> Unit,
    onFloatingBallSizeChanged: (Int) -> Unit,
    onFloatingBallOpacityChanged: (Float) -> Unit,
    onFloatingBallThemeChanged: (FloatingBallTheme) -> Unit,
    onPinHistoryEnabledChanged: (Boolean) -> Unit,
    onMaxPinHistoryCountChanged: (Int) -> Unit,
    onPinHistoryRetainDaysChanged: (Int) -> Unit,
    onClearAllRecords: () -> Unit,
    onClearImageCaches: () -> Unit,
    onClearAllRuntimeFiles: () -> Unit,
    onClearPinHistory: () -> Unit,
    onDeleteHistory: (PinHistoryRecord) -> Unit,
    onRestoreHistory: (PinHistoryRecord) -> Unit,
    onEditHistory: (PinHistoryRecord) -> Unit,
    onRefreshRecords: () -> MainScreenSnapshot,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit
) {
    var currentTab by remember { mutableStateOf(SettingsTab.BASIC) }
    val scope = rememberCoroutineScope()
    var permissionGranted by remember { mutableStateOf(hasOverlayPermission) }
    var selectedAction by remember { mutableStateOf(initialAction) }
    var selectedScaleMode by remember { mutableStateOf(initialScaleMode) }
    var maxSessionCount by remember { mutableIntStateOf(initialMaxSessionCount) }
    var retainDays by remember { mutableIntStateOf(initialRetainDays) }
    var defaultPinShadowEnabled by remember { mutableStateOf(initialPinShadowEnabled) }
    var floatingBallSizeDp by remember { mutableIntStateOf(initialFloatingBallSizeDp) }
    var floatingBallOpacity by remember { mutableStateOf(initialFloatingBallOpacity) }
    var floatingBallTheme by remember { mutableStateOf(initialFloatingBallTheme) }
    var pinHistoryEnabled by remember { mutableStateOf(initialPinHistoryEnabled) }
    var maxPinHistoryCount by remember { mutableIntStateOf(initialMaxPinHistoryCount) }
    var pinHistoryRetainDays by remember { mutableIntStateOf(initialPinHistoryRetainDays) }
    var snapshot by remember { mutableStateOf(initialSnapshot) }
    var recordsLoading by remember { mutableStateOf(false) }

    fun refreshRecordsAsync() {
        scope.launch {
            recordsLoading = true
            snapshot = withContext(Dispatchers.IO) { onRefreshRecords() }
            recordsLoading = false
        }
    }

    fun runRecordsMutation(task: () -> Unit) {
        scope.launch {
            recordsLoading = true
            snapshot = withContext(Dispatchers.IO) {
                task()
                onRefreshRecords()
            }
            recordsLoading = false
        }
    }

    LaunchedEffect(hasOverlayPermission) {
        permissionGranted = hasOverlayPermission
        refreshRecordsAsync()
    }

    Scaffold(
        bottomBar = {
            BottomAppBar {
                NavigationBarItem(
                    selected = currentTab == SettingsTab.BASIC,
                    onClick = { currentTab = SettingsTab.BASIC },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("基础设置") }
                )
                NavigationBarItem(
                    selected = currentTab == SettingsTab.HISTORY,
                    onClick = { currentTab = SettingsTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("历史管理") }
                )
            }
        }
    ) { paddingValues ->
        when (currentTab) {
            SettingsTab.BASIC -> BasicSettingsTab(
                modifier = Modifier.padding(paddingValues),
                permissionGranted = permissionGranted,
                selectedAction = selectedAction,
                selectedScaleMode = selectedScaleMode,
                defaultPinShadowEnabled = defaultPinShadowEnabled,
                floatingBallSizeDp = floatingBallSizeDp,
                floatingBallOpacity = floatingBallOpacity,
                floatingBallTheme = floatingBallTheme,
                onActionChanged = {
                    selectedAction = it
                    onActionChanged(it)
                },
                onScaleModeChanged = {
                    selectedScaleMode = it
                    onScaleModeChanged(it)
                },
                onDefaultPinShadowChanged = {
                    defaultPinShadowEnabled = it
                    onDefaultPinShadowChanged(it)
                },
                onFloatingBallSizeChanged = {
                    floatingBallSizeDp = it
                    onFloatingBallSizeChanged(it)
                },
                onFloatingBallOpacityChanged = {
                    floatingBallOpacity = it
                    onFloatingBallOpacityChanged(it)
                },
                onFloatingBallThemeChanged = {
                    floatingBallTheme = it
                    onFloatingBallThemeChanged(it)
                },
                onRequestPermission = onRequestPermission,
                onStartService = onStartService
            )

            SettingsTab.HISTORY -> HistoryManagementTab(
                modifier = Modifier.padding(paddingValues),
                pinHistoryEnabled = pinHistoryEnabled,
                maxPinHistoryCount = maxPinHistoryCount,
                pinHistoryRetainDays = pinHistoryRetainDays,
                maxSessionCount = maxSessionCount,
                retainDays = retainDays,
                snapshot = snapshot,
                onPinHistoryEnabledChanged = {
                    pinHistoryEnabled = it
                    runRecordsMutation { onPinHistoryEnabledChanged(it) }
                },
                onMaxPinHistoryCountChanged = { value ->
                    maxPinHistoryCount = value
                    runRecordsMutation { onMaxPinHistoryCountChanged(value) }
                },
                onPinHistoryRetainDaysChanged = { value ->
                    pinHistoryRetainDays = value
                    runRecordsMutation { onPinHistoryRetainDaysChanged(value) }
                },
                onMaxSessionCountChanged = { value ->
                    maxSessionCount = value
                    runRecordsMutation { onMaxSessionCountChanged(value) }
                },
                onRetainDaysChanged = { value ->
                    retainDays = value
                    runRecordsMutation { onRetainDaysChanged(value) }
                },
                onClearPinHistory = {
                    runRecordsMutation { onClearPinHistory() }
                },
                onClearAllRecords = {
                    runRecordsMutation { onClearAllRecords() }
                },
                onClearImageCaches = {
                    runRecordsMutation { onClearImageCaches() }
                },
                onClearAllRuntimeFiles = {
                    runRecordsMutation { onClearAllRuntimeFiles() }
                },
                onDeleteHistory = {
                    runRecordsMutation { onDeleteHistory(it) }
                },
                onRestoreHistory = onRestoreHistory,
                onEditHistory = onEditHistory,
                onRefreshRecords = { refreshRecordsAsync() },
                recordsLoading = recordsLoading
            )
        }
    }
}
@Composable
private fun BasicSettingsTab(
    modifier: Modifier = Modifier,
    permissionGranted: Boolean,
    selectedAction: CaptureResultAction,
    selectedScaleMode: PinScaleMode,
    defaultPinShadowEnabled: Boolean,
    floatingBallSizeDp: Int,
    floatingBallOpacity: Float,
    floatingBallTheme: FloatingBallTheme,
    onActionChanged: (CaptureResultAction) -> Unit,
    onScaleModeChanged: (PinScaleMode) -> Unit,
    onDefaultPinShadowChanged: (Boolean) -> Unit,
    onFloatingBallSizeChanged: (Int) -> Unit,
    onFloatingBallOpacityChanged: (Float) -> Unit,
    onFloatingBallThemeChanged: (FloatingBallTheme) -> Unit,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit
) {
    LazyColumn(
        modifier = modifier
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
                    Text("选区完成后的操作", style = MaterialTheme.typography.titleSmall)
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
                    Divider()
                    Text("贴图缩放方式", style = MaterialTheme.typography.titleSmall)
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
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("贴图默认阴影", style = MaterialTheme.typography.titleSmall)
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
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("悬浮球个性化", style = MaterialTheme.typography.titleSmall)
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

        item {
            if (!permissionGranted) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("授予权限")
                }
            } else {
                OutlinedButton(
                    onClick = onStartService,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("重启悬浮球")
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
private fun HistoryManagementTab(
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
    onClearAllRuntimeFiles: () -> Unit,
    onDeleteHistory: (PinHistoryRecord) -> Unit,
    onRestoreHistory: (PinHistoryRecord) -> Unit,
    onEditHistory: (PinHistoryRecord) -> Unit,
    onRefreshRecords: () -> Unit,
    recordsLoading: Boolean
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("贴图历史策略", style = MaterialTheme.typography.titleSmall)
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
                        text = "贴图历史目录：${snapshot.pinHistoryDirectory}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("当前贴图历史数：${snapshot.pinHistoryRecords.size}", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onRefreshRecords,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("刷新")
                        }
                        OutlinedButton(
                            onClick = onClearPinHistory,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("清空贴图历史")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("工程记录策略", style = MaterialTheme.typography.titleSmall)
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
                        text = "工程目录：${snapshot.recordsDirectory}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "工程文件数：${snapshot.sessionFiles.size}    已关闭贴图恢复队列：${snapshot.recentClosedPinCount}",
                        style = MaterialTheme.typography.bodyMedium
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
                    Text("运行缓存清理", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "这些文件都属于应用运行时缓存，可主动删除，不会影响你的手机系统和相册里的正式图片。",
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

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("贴图历史记录", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "共 ${snapshot.pinHistoryRecords.size} 条",
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
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp,
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
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("当前还没有贴图历史", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "贴图后会自动出现在这里。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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

@Composable
private fun PinHistoryRecordCard(
    item: PinHistoryRecord,
    onRestore: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(historySourceLabel(item.sourceType), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = formatTimestamp(item.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (item.annotationSessionId.isNullOrBlank()) "图片历史" else "可继续编辑",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.annotationSessionId.isNullOrBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Text(
                text = item.imageUri,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
                    Text("重新贴图")
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("继续编辑")
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun FloatingBallAppearancePreview(
    sizeDp: Int,
    opacity: Float,
    theme: FloatingBallTheme
) {
    val colors = floatingBallThemeColors(theme)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(sizeDp.dp),
            shape = CircleShape,
            shadowElevation = 8.dp
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
        Text(
            text = "预览仅展示悬浮球外观，设置后会立即同步到当前悬浮球。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
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

private fun historySourceLabel(sourceType: PinHistorySourceType): String {
    return when (sourceType) {
        PinHistorySourceType.SCREENSHOT -> "截图直贴"
        PinHistorySourceType.EDITOR_EXPORT -> "编辑后贴图"
        PinHistorySourceType.RESTORED_PIN -> "历史恢复贴图"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    return when {
        bytes >= mb -> String.format(Locale.getDefault(), "%.2f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.getDefault(), "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}



