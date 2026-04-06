package com.muding.android.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.muding.android.data.settings.OnboardingGuideProgress
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.domain.usecase.FloatingBallAppearanceMode
import com.muding.android.domain.usecase.FloatingBallTheme
import com.muding.android.domain.usecase.PinHistoryRecord
import com.muding.android.domain.usecase.PinScaleMode
import com.muding.android.feature.onboarding.OnboardingGuideState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    hasOverlayPermission: Boolean,
    initialAction: CaptureResultAction,
    initialScaleMode: PinScaleMode,
    initialMaxSessionCount: Int,
    initialRetainDays: Int,
    initialPinShadowEnabled: Boolean,
    initialPinCornerRadiusDp: Float,
    initialFloatingBallSizeDp: Int,
    initialFloatingBallOpacity: Float,
    initialFloatingBallTheme: FloatingBallTheme,
    initialFloatingBallAppearanceMode: FloatingBallAppearanceMode,
    initialFloatingBallCustomImageUri: String?,
    initialOnboardingGuideProgress: OnboardingGuideProgress,
    initialPinHistoryEnabled: Boolean,
    initialMaxPinHistoryCount: Int,
    initialPinHistoryRetainDays: Int,
    initialSnapshot: MainScreenSnapshot,
    onActionChanged: (CaptureResultAction) -> Unit,
    onScaleModeChanged: (PinScaleMode) -> Unit,
    onProjectRecordRetentionChanged: (Int, Int) -> Unit,
    onDefaultPinShadowChanged: (Boolean) -> Unit,
    onDefaultPinCornerRadiusChanged: (Float) -> Unit,
    onFloatingBallSizeChanged: (Int) -> Unit,
    onFloatingBallOpacityChanged: (Float) -> Unit,
    onFloatingBallThemeChanged: (FloatingBallTheme) -> Unit,
    onFloatingBallAppearanceCommitted: (FloatingBallAppearanceMode, String?) -> Unit,
    onChooseFloatingBallCustomImage: () -> Unit,
    onHomeOnboardingGuideSeen: () -> Unit,
    onPinHistoryEnabledChanged: (Boolean) -> Unit,
    onPinHistoryRetentionChanged: (Int, Int) -> Unit,
    onClearWorkRecords: () -> Unit,
    onResetApplication: () -> Unit,
    onDeleteHistory: (PinHistoryRecord) -> Unit,
    onRestoreHistory: (PinHistoryRecord) -> Unit,
    onEditHistory: (PinHistoryRecord) -> Unit,
    onRefreshRecords: () -> MainScreenSnapshot,
    onRequestPermission: () -> Unit,
    onOpenGalleryPin: () -> Unit,
    onOpenGalleryOcr: () -> Unit,
    onOpenClipboardTextPin: () -> Unit,
    onStartService: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
    var currentSettingsSection by remember { mutableStateOf<SettingsSection?>(null) }
    val scope = rememberCoroutineScope()
    var permissionGranted by remember { mutableStateOf(hasOverlayPermission) }
    var selectedAction by remember { mutableStateOf(initialAction) }
    var selectedScaleMode by remember { mutableStateOf(initialScaleMode) }
    var maxSessionCount by remember { mutableIntStateOf(initialMaxSessionCount) }
    var retainDays by remember { mutableIntStateOf(initialRetainDays) }
    var defaultPinShadowEnabled by remember { mutableStateOf(initialPinShadowEnabled) }
    var defaultPinCornerRadiusDp by remember { mutableStateOf(initialPinCornerRadiusDp) }
    var floatingBallSizeDp by remember { mutableIntStateOf(initialFloatingBallSizeDp) }
    var floatingBallOpacity by remember { mutableStateOf(initialFloatingBallOpacity) }
    var floatingBallTheme by remember { mutableStateOf(initialFloatingBallTheme) }
    var floatingBallAppearanceMode by remember(initialFloatingBallAppearanceMode) {
        mutableStateOf(initialFloatingBallAppearanceMode)
    }
    var floatingBallCustomImageUri by remember(initialFloatingBallCustomImageUri) {
        mutableStateOf(initialFloatingBallCustomImageUri)
    }
    var onboardingGuideState by remember(initialOnboardingGuideProgress) {
        mutableStateOf(OnboardingGuideState.fromProgress(initialOnboardingGuideProgress))
    }
    var pinHistoryEnabled by remember { mutableStateOf(initialPinHistoryEnabled) }
    var maxPinHistoryCount by remember { mutableIntStateOf(initialMaxPinHistoryCount) }
    var pinHistoryRetainDays by remember { mutableIntStateOf(initialPinHistoryRetainDays) }
    var snapshot by remember { mutableStateOf(initialSnapshot) }
    var recordsLoading by remember { mutableStateOf(false) }
    val isPreview = LocalInspectionMode.current
    val tokens = rememberMainUiTokens()

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

    fun applyFloatingBallAppearanceTransition(
        transition: FloatingBallAppearanceSourceState.Transition
    ) {
        floatingBallAppearanceMode = transition.state.mode
        floatingBallCustomImageUri = transition.state.customImageUri
        when (val effect = transition.effect) {
            FloatingBallAppearanceSourceState.Effect.RequestImagePicker -> onChooseFloatingBallCustomImage()
            is FloatingBallAppearanceSourceState.Effect.CommitAppearance -> {
                onFloatingBallAppearanceCommitted(effect.mode, effect.customImageUri)
            }

            null -> Unit
        }
    }

    LaunchedEffect(hasOverlayPermission) {
        permissionGranted = hasOverlayPermission
        if (!isPreview) {
            refreshRecordsAsync()
        }
    }

    BackHandler(
        enabled = currentDestination == MainDestination.SETTINGS && currentSettingsSection != null
    ) {
        currentSettingsSection = null
    }

    val topBarTitle = when {
        currentDestination == MainDestination.SETTINGS && currentSettingsSection != null ->
            currentSettingsSection?.title.orEmpty()

        else -> currentDestination.title
    }

    Scaffold(
        containerColor = tokens.palette.pageBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        color = tokens.palette.title
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.palette.pageBackground,
                    titleContentColor = tokens.palette.title,
                    navigationIconContentColor = tokens.palette.title
                ),
                navigationIcon = {
                    if (currentDestination == MainDestination.SETTINGS && currentSettingsSection != null) {
                        IconButton(onClick = { currentSettingsSection = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = tokens.palette.surfaceStrong,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentDestination == MainDestination.HOME,
                    onClick = { currentDestination = MainDestination.HOME },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = tokens.palette.surfaceAccent
                    ),
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = currentDestination == MainDestination.RECORDS,
                    onClick = { currentDestination = MainDestination.RECORDS },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = tokens.palette.surfaceAccent
                    ),
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("记录") }
                )
                NavigationBarItem(
                    selected = currentDestination == MainDestination.SETTINGS,
                    onClick = { currentDestination = MainDestination.SETTINGS },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = tokens.palette.surfaceAccent
                    ),
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("设置") }
                )
            }
        }
    ) { paddingValues ->
        when (currentDestination) {
            MainDestination.HOME -> HomeDashboardScreen(
                modifier = Modifier.padding(paddingValues),
                permissionGranted = permissionGranted,
                selectedAction = selectedAction,
                selectedScaleMode = selectedScaleMode,
                defaultPinShadowEnabled = defaultPinShadowEnabled,
                defaultPinCornerRadiusDp = defaultPinCornerRadiusDp,
                floatingBallTheme = floatingBallTheme,
                floatingBallAppearanceMode = floatingBallAppearanceMode,
                floatingBallSizeDp = floatingBallSizeDp,
                showFirstRunGuide = onboardingGuideState.shouldShowHomeGuide(
                    isOnHomeDestination = currentDestination == MainDestination.HOME
                ),
                onOpenGalleryPin = onOpenGalleryPin,
                onOpenGalleryOcr = onOpenGalleryOcr,
                onOpenClipboardTextPin = onOpenClipboardTextPin,
                onOpenTranslationSettings = {
                    currentDestination = MainDestination.SETTINGS
                    currentSettingsSection = SettingsSection.OCR_AND_TRANSLATION
                },
                onRequestPermission = onRequestPermission,
                onStartService = onStartService,
                onOpenSettings = { currentDestination = MainDestination.SETTINGS },
                onDismissFirstRunGuide = {
                    onboardingGuideState = onboardingGuideState.markHomeGuideSeen()
                    onHomeOnboardingGuideSeen()
                }
            )

            MainDestination.RECORDS -> RecordsScreen(
                modifier = Modifier.padding(paddingValues),
                snapshot = snapshot,
                recordsLoading = recordsLoading,
                onRefreshRecords = { refreshRecordsAsync() },
                onDeleteHistory = { runRecordsMutation { onDeleteHistory(it) } },
                onRestoreHistory = onRestoreHistory,
                onEditHistory = onEditHistory,
                onOpenStorageSettings = {
                    currentDestination = MainDestination.SETTINGS
                    currentSettingsSection = SettingsSection.STORAGE_AND_RECORDS
                }
            )

            MainDestination.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(paddingValues),
                selectedSection = currentSettingsSection,
                permissionGranted = permissionGranted,
                selectedAction = selectedAction,
                selectedScaleMode = selectedScaleMode,
                defaultPinShadowEnabled = defaultPinShadowEnabled,
                defaultPinCornerRadiusDp = defaultPinCornerRadiusDp,
                floatingBallSizeDp = floatingBallSizeDp,
                floatingBallOpacity = floatingBallOpacity,
                floatingBallTheme = floatingBallTheme,
                floatingBallAppearanceMode = floatingBallAppearanceMode,
                floatingBallCustomImageUri = floatingBallCustomImageUri,
                pinHistoryEnabled = pinHistoryEnabled,
                maxPinHistoryCount = maxPinHistoryCount,
                pinHistoryRetainDays = pinHistoryRetainDays,
                maxSessionCount = maxSessionCount,
                retainDays = retainDays,
                snapshot = snapshot,
                onOpenSection = { currentSettingsSection = it },
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
                onDefaultPinCornerRadiusChanged = {
                    defaultPinCornerRadiusDp = it
                    onDefaultPinCornerRadiusChanged(it)
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
                onFloatingBallUseDefaultAppearance = {
                    applyFloatingBallAppearanceTransition(
                        FloatingBallAppearanceSourceState(
                            mode = floatingBallAppearanceMode,
                            customImageUri = floatingBallCustomImageUri
                        ).restoreDefault()
                    )
                },
                onFloatingBallCustomImageModeSelected = {
                    applyFloatingBallAppearanceTransition(
                        FloatingBallAppearanceSourceState(
                            mode = floatingBallAppearanceMode,
                            customImageUri = floatingBallCustomImageUri
                        ).selectCustomImageMode()
                    )
                },
                onChooseFloatingBallCustomImage = onChooseFloatingBallCustomImage,
                onPinHistoryEnabledChanged = {
                    pinHistoryEnabled = it
                    runRecordsMutation { onPinHistoryEnabledChanged(it) }
                },
                onPinHistoryRetentionChanged = { count, days ->
                    maxPinHistoryCount = count
                    pinHistoryRetainDays = days
                    runRecordsMutation { onPinHistoryRetentionChanged(count, days) }
                },
                onProjectRecordRetentionChanged = { count, days ->
                    maxSessionCount = count
                    retainDays = days
                    runRecordsMutation { onProjectRecordRetentionChanged(count, days) }
                },
                onClearWorkRecords = { runRecordsMutation { onClearWorkRecords() } },
                onResetApplication = {
                    recordsLoading = true
                    onResetApplication()
                }
            )
        }
    }
}
