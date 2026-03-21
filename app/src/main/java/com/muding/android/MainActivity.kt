package com.muding.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.muding.android.app.AppGraph
import com.muding.android.core.model.PinSourceType
import com.muding.android.data.repository.AnnotationSessionRepository
import com.muding.android.data.repository.PinHistoryRepository
import com.muding.android.data.repository.RecentPinRepository
import com.muding.android.data.repository.RuntimeStorageRepository
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.domain.usecase.PermissionHandler
import com.muding.android.domain.usecase.PinHistoryMetadata
import com.muding.android.feature.pin.creation.EditorLaunchRequest
import com.muding.android.feature.pin.creation.PinCreationCoordinator
import com.muding.android.presentation.main.MainScreen
import com.muding.android.presentation.main.MainScreenSnapshot
import com.muding.android.presentation.source.ClipboardTextPinActivity
import com.muding.android.presentation.source.GalleryOcrActivity
import com.muding.android.presentation.source.GalleryPinActivity
import com.muding.android.presentation.theme.MudingTheme
import com.muding.android.presentation.translation.TranslationSettingsActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHandler = PermissionHandler(this)
        settingsRepository = AppGraph.appSettingsRepository(this)
        annotationSessionRepository = AppGraph.annotationSessionRepository(this)
        pinHistoryRepository = AppGraph.pinHistoryRepository(this)
        recentPinRepository = AppGraph.recentPinRepository(this)
        runtimeStorageRepository = AppGraph.runtimeStorageRepository(this)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)
        val projectRecordSettings = settingsRepository.getProjectRecordSettings()
        val floatingBallSettings = settingsRepository.getFloatingBallSettings()
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
                    initialPinHistoryEnabled = pinHistorySettings.enabled,
                    initialMaxPinHistoryCount = pinHistorySettings.maxCount,
                    initialPinHistoryRetainDays = pinHistorySettings.retainDays,
                    initialSnapshot = MainScreenSnapshot.empty(),
                    onActionChanged = { action -> settingsRepository.setCaptureResultAction(action) },
                    onScaleModeChanged = { mode -> settingsRepository.setPinScaleMode(mode) },
                    onMaxSessionCountChanged = { count ->
                        settingsRepository.setMaxSessionCount(count)
                        pruneRecords()
                    },
                    onRetainDaysChanged = { days ->
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
                        refreshFloatingBallAppearance()
                    },
                    onFloatingBallOpacityChanged = { opacity ->
                        settingsRepository.setFloatingBallOpacity(opacity)
                        refreshFloatingBallAppearance()
                    },
                    onFloatingBallThemeChanged = { theme ->
                        settingsRepository.setFloatingBallTheme(theme)
                        refreshFloatingBallAppearance()
                    },
                    onPinHistoryEnabledChanged = { enabled ->
                        settingsRepository.setPinHistoryEnabled(enabled)
                    },
                    onMaxPinHistoryCountChanged = { count ->
                        settingsRepository.setMaxPinHistoryCount(count)
                        pruneRecords()
                    },
                    onPinHistoryRetainDaysChanged = { days ->
                        settingsRepository.setPinHistoryRetainDays(days)
                        pruneRecords()
                    },
                    onClearAllRecords = {
                        annotationSessionRepository.clearAll()
                        recentPinRepository.clear()
                        pinHistoryRepository.clear()
                    },
                    onClearImageCaches = {
                        runtimeStorageRepository.clearImageCaches()
                    },
                    onClearAllRuntimeFiles = {
                        runtimeStorageRepository.clearAllRuntimeFiles()
                    },
                    onClearPinHistory = {
                        pinHistoryRepository.clear()
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
                    onOpenTranslationSettings = {
                        openTranslationSettings()
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
            sessionFiles = annotationSessionRepository.listSessionFiles(),
            recentClosedPinCount = recentPinRepository.count(),
            pinHistoryRecords = pinHistoryRepository.list(),
            recordsDirectory = annotationSessionRepository.visibleDirectoryPath(),
            pinHistoryDirectory = pinHistoryRepository.visibleDirectoryPath(),
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

    private fun openTranslationSettings() {
        startActivity(Intent(this, TranslationSettingsActivity::class.java))
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
}
