package com.muding.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.muding.android.app.AppGraph
import com.muding.android.core.model.PinSourceType
import com.muding.android.data.repository.AnnotationSessionRepository
import com.muding.android.data.repository.PinHistoryRepository
import com.muding.android.data.repository.RecentPinRepository
import com.muding.android.data.repository.RuntimeStorageRepository
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.data.settings.FloatingBallSettings
import com.muding.android.data.settings.OnboardingGuideProgress
import com.muding.android.domain.usecase.AppMaintenanceCoordinator
import com.muding.android.domain.usecase.FloatingBallAppearanceMode
import com.muding.android.domain.usecase.FloatingBallTheme
import com.muding.android.domain.usecase.PermissionHandler
import com.muding.android.domain.usecase.PinHistoryMetadata
import com.muding.android.feature.floatingball.FloatingBallImageProcessor
import com.muding.android.feature.pin.creation.EditorLaunchRequest
import com.muding.android.feature.pin.creation.PinCreationCoordinator
import com.muding.android.presentation.main.MainScreen
import com.muding.android.presentation.main.MainScreenSnapshot
import com.muding.android.presentation.source.ClipboardTextPinActivity
import com.muding.android.presentation.source.FloatingBallImagePickerActivity
import com.muding.android.presentation.source.GalleryOcrActivity
import com.muding.android.presentation.source.GalleryPinActivity
import com.muding.android.presentation.theme.MudingTheme
import com.muding.android.service.FloatingBallService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var annotationSessionRepository: AnnotationSessionRepository
    private lateinit var pinHistoryRepository: PinHistoryRepository
    private lateinit var recentPinRepository: RecentPinRepository
    private lateinit var runtimeStorageRepository: RuntimeStorageRepository
    private lateinit var pinCreationCoordinator: PinCreationCoordinator
    private lateinit var appMaintenanceCoordinator: AppMaintenanceCoordinator
    private lateinit var floatingBallImageProcessor: FloatingBallImageProcessor

    private var floatingBallSettings by mutableStateOf(defaultFloatingBallSettings())
    private var onboardingGuideProgress by mutableStateOf(defaultOnboardingGuideProgress())

    private val floatingBallImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            floatingBallSettings = settingsRepository.getFloatingBallSettings()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHandler = PermissionHandler(this)
        settingsRepository = AppGraph.appSettingsRepository(this)
        annotationSessionRepository = AppGraph.annotationSessionRepository(this)
        pinHistoryRepository = AppGraph.pinHistoryRepository(this)
        recentPinRepository = AppGraph.recentPinRepository(this)
        runtimeStorageRepository = AppGraph.runtimeStorageRepository(this)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)
        floatingBallImageProcessor = AppGraph.floatingBallImageProcessor(this)
        appMaintenanceCoordinator = AppMaintenanceCoordinator(
            annotationSessionRepository = annotationSessionRepository,
            pinHistoryRepository = pinHistoryRepository,
            recentPinRepository = recentPinRepository,
            runtimeStorageRepository = runtimeStorageRepository,
            settingsRepository = settingsRepository,
            localTranslationModelResetter = AppGraph.localTranslationModelResetter()
        )
        val projectRecordSettings = settingsRepository.getProjectRecordSettings()
        floatingBallSettings = settingsRepository.getFloatingBallSettings()
        onboardingGuideProgress = settingsRepository.getOnboardingGuideProgress()
        val pinHistorySettings = settingsRepository.getPinHistorySettings()

        setContent {
            MudingTheme {
                MainScreen(
                    hasOverlayPermission = permissionHandler.hasOverlayPermission(),
                    initialAction = settingsRepository.getCaptureResultAction(),
                    initialScaleMode = settingsRepository.getPinScaleMode(),
                    initialMaxSessionCount = projectRecordSettings.maxSessionCount,
                    initialRetainDays = projectRecordSettings.retainDays,
                    initialPinShadowEnabled = settingsRepository.isPinShadowEnabledByDefault(),
                    initialPinCornerRadiusDp = settingsRepository.getDefaultPinCornerRadiusDp(),
                    initialFloatingBallSizeDp = floatingBallSettings.sizeDp,
                    initialFloatingBallOpacity = floatingBallSettings.opacity,
                    initialFloatingBallTheme = floatingBallSettings.theme,
                    initialFloatingBallAppearanceMode = floatingBallSettings.appearanceMode,
                    initialFloatingBallCustomImageUri = floatingBallSettings.customImageUri,
                    initialOnboardingGuideProgress = onboardingGuideProgress,
                    initialPinHistoryEnabled = pinHistorySettings.enabled,
                    initialMaxPinHistoryCount = pinHistorySettings.maxCount,
                    initialPinHistoryRetainDays = pinHistorySettings.retainDays,
                    initialSnapshot = MainScreenSnapshot.empty(),
                    onActionChanged = { action -> settingsRepository.setCaptureResultAction(action) },
                    onScaleModeChanged = { mode -> settingsRepository.setPinScaleMode(mode) },
                    onProjectRecordRetentionChanged = { count, days ->
                        settingsRepository.setMaxSessionCount(count)
                        settingsRepository.setRetainDays(days)
                        pruneRecords()
                    },
                    onDefaultPinShadowChanged = { enabled ->
                        settingsRepository.setPinShadowEnabledByDefault(enabled)
                    },
                    onDefaultPinCornerRadiusChanged = { radiusDp ->
                        settingsRepository.setDefaultPinCornerRadiusDp(radiusDp)
                    },
                    onFloatingBallSizeChanged = { size ->
                        settingsRepository.setFloatingBallSizeDp(size)
                        floatingBallSettings = floatingBallSettings.copy(sizeDp = size)
                        refreshFloatingBallAppearance()
                    },
                    onFloatingBallOpacityChanged = { opacity ->
                        settingsRepository.setFloatingBallOpacity(opacity)
                        floatingBallSettings = floatingBallSettings.copy(opacity = opacity)
                        refreshFloatingBallAppearance()
                    },
                    onFloatingBallThemeChanged = { theme ->
                        settingsRepository.setFloatingBallTheme(theme)
                        floatingBallSettings = floatingBallSettings.copy(theme = theme)
                        refreshFloatingBallAppearance()
                    },
                    onFloatingBallAppearanceCommitted = { mode, customImageUri ->
                        settingsRepository.setFloatingBallAppearanceMode(mode)
                        settingsRepository.setFloatingBallCustomImageUri(customImageUri)
                        if (mode == FloatingBallAppearanceMode.THEME && customImageUri == null) {
                            floatingBallImageProcessor.clearCurrentImage()
                        }
                        floatingBallSettings = floatingBallSettings.copy(
                            appearanceMode = mode,
                            customImageUri = customImageUri
                        )
                        refreshFloatingBallAppearance()
                    },
                    onChooseFloatingBallCustomImage = {
                        openFloatingBallImagePicker()
                    },
                    onHomeOnboardingGuideSeen = {
                        settingsRepository.setHomeOnboardingGuideSeen(true)
                        onboardingGuideProgress = onboardingGuideProgress.copy(hasSeenHomeGuide = true)
                    },
                    onPinHistoryEnabledChanged = { enabled ->
                        settingsRepository.setPinHistoryEnabled(enabled)
                    },
                    onPinHistoryRetentionChanged = { count, days ->
                        settingsRepository.setMaxPinHistoryCount(count)
                        settingsRepository.setPinHistoryRetainDays(days)
                        pruneRecords()
                    },
                    onClearWorkRecords = {
                        appMaintenanceCoordinator.clearWorkRecords()
                    },
                    onResetApplication = {
                        lifecycleScope.launch {
                            appMaintenanceCoordinator.resetApplication()
                            recreate()
                        }
                    },
                    onDeleteHistory = { record ->
                        pinHistoryRepository.delete(record.id)
                    },
                    onRestoreHistory = { record ->
                        lifecycleScope.launch {
                            val request = pinCreationCoordinator.createRequest(
                                source = pinCreationCoordinator.createImageSource(
                                    sourceType = PinSourceType.HISTORY_RESTORE,
                                    uri = record.imageUri
                                ),
                                annotationSessionId = record.annotationSessionId,
                                preferredHistoryMetadata = PinHistoryMetadata(
                                    displayName = record.displayName,
                                    textPreview = record.textPreview,
                                    widthPx = record.widthPx,
                                    heightPx = record.heightPx
                                )
                            )
                            pinCreationCoordinator.startPinOverlay(this@MainActivity, request)
                        }
                    },
                    onEditHistory = { record ->
                        pinCreationCoordinator.startEditor(
                            this,
                            EditorLaunchRequest(
                                imageUri = record.imageUri,
                                annotationSessionId = record.annotationSessionId
                            )
                        )
                    },
                    onRefreshRecords = {
                        pruneRecords()
                        buildSnapshot()
                    },
                    onRequestPermission = {
                        permissionHandler.requestOverlayPermission(this)
                    },
                    onOpenGalleryPin = {
                        openGalleryPinPicker()
                    },
                    onOpenGalleryOcr = {
                        openGalleryOcr()
                    },
                    onOpenClipboardTextPin = {
                        openClipboardTextPin()
                    },
                    onStartService = {
                        startFloatingBallService()
                    }
                )
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
        val projectRecordSettings = settingsRepository.getProjectRecordSettings()
        val pinHistorySettings = settingsRepository.getPinHistorySettings()
        annotationSessionRepository.prune(
            maxCount = projectRecordSettings.maxSessionCount,
            maxDays = projectRecordSettings.retainDays
        )
        pinHistoryRepository.prune(
            maxCount = pinHistorySettings.maxCount,
            maxDays = pinHistorySettings.retainDays
        )
    }

    private fun buildSnapshot(): MainScreenSnapshot {
        return MainScreenSnapshot(
            sessionFileCount = annotationSessionRepository.count(),
            recentClosedPinCount = recentPinRepository.count(),
            pinHistoryRecords = pinHistoryRepository.list(),
            runtimeStorage = runtimeStorageRepository.snapshot()
        )
    }

    private fun startFloatingBallService() {
        val intent = Intent(this, FloatingBallService::class.java)
        startService(intent)
    }

    private fun openGalleryPinPicker() {
        startActivity(Intent(this, GalleryPinActivity::class.java))
    }

    private fun openGalleryOcr() {
        startActivity(Intent(this, GalleryOcrActivity::class.java))
    }

    private fun openClipboardTextPin() {
        startActivity(Intent(this, ClipboardTextPinActivity::class.java))
    }

    private fun openFloatingBallImagePicker() {
        floatingBallImagePickerLauncher.launch(
            Intent(this, FloatingBallImagePickerActivity::class.java)
        )
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

    @Deprecated("Overlay permission flow still uses onActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionHandler.REQUEST_CODE_OVERLAY_PERMISSION) {
            if (permissionHandler.hasOverlayPermission()) {
                startFloatingBallService()
                moveTaskToBack(true)
            }
        }
    }

    private companion object {
        fun defaultFloatingBallSettings(): FloatingBallSettings {
            return FloatingBallSettings(
                sizeDp = 46,
                opacity = 0.92f,
                theme = FloatingBallTheme.BLUE_PURPLE,
                appearanceMode = FloatingBallAppearanceMode.THEME,
                customImageUri = null
            )
        }

        fun defaultOnboardingGuideProgress(): OnboardingGuideProgress {
            return OnboardingGuideProgress(
                hasSeenHomeGuide = false,
                hasSeenFloatingBallHint = false,
                hasSeenPinOverlayHint = false,
                hasSeenEditorHint = false
            )
        }
    }
}
