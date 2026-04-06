package com.muding.android.domain.usecase

import com.muding.android.data.repository.AnnotationSessionRepository
import com.muding.android.data.repository.PinHistoryRepository
import com.muding.android.data.repository.RecentPinRepository
import com.muding.android.data.repository.RuntimeStorageRepository
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.data.settings.CloudTranslationProvider
import com.muding.android.data.settings.FloatingBallSettings
import com.muding.android.data.settings.OnboardingGuideProgress
import com.muding.android.data.settings.PinAppearanceSettings
import com.muding.android.data.settings.PinHistorySettings
import com.muding.android.data.settings.ProjectRecordSettings
import com.muding.android.data.settings.TranslationSettings
import com.muding.android.feature.translation.LocalTranslationModelResetter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppMaintenanceCoordinatorTest {

    @Test
    fun `clearWorkRecords removes work data and keeps app settings`() {
        val annotationSessions = FakeAnnotationSessionRepository()
        val pinHistory = FakePinHistoryRepository()
        val recentPins = FakeRecentPinRepository()
        val runtimeStorage = FakeRuntimeStorageRepository()
        val settings = FakeAppSettingsRepository()
        val localModels = FakeLocalTranslationModelResetter()
        val coordinator = AppMaintenanceCoordinator(
            annotationSessionRepository = annotationSessions,
            pinHistoryRepository = pinHistory,
            recentPinRepository = recentPins,
            runtimeStorageRepository = runtimeStorage,
            settingsRepository = settings,
            localTranslationModelResetter = localModels
        )

        coordinator.clearWorkRecords()

        assertEquals(1, annotationSessions.clearAllCallCount)
        assertEquals(1, pinHistory.clearCallCount)
        assertEquals(1, recentPins.clearCallCount)
        assertEquals(1, runtimeStorage.clearImageCachesCallCount)
        assertEquals(0, settings.resetAllSettingsCallCount)
        assertEquals(0, localModels.clearDownloadedModelsCallCount)
    }

    @Test
    fun `resetApplication clears work data settings and local translation models`() {
        val annotationSessions = FakeAnnotationSessionRepository()
        val pinHistory = FakePinHistoryRepository()
        val recentPins = FakeRecentPinRepository()
        val runtimeStorage = FakeRuntimeStorageRepository()
        val settings = FakeAppSettingsRepository()
        val localModels = FakeLocalTranslationModelResetter()
        val coordinator = AppMaintenanceCoordinator(
            annotationSessionRepository = annotationSessions,
            pinHistoryRepository = pinHistory,
            recentPinRepository = recentPins,
            runtimeStorageRepository = runtimeStorage,
            settingsRepository = settings,
            localTranslationModelResetter = localModels
        )

        runBlocking {
            coordinator.resetApplication()
        }

        assertEquals(1, annotationSessions.clearAllCallCount)
        assertEquals(1, pinHistory.clearCallCount)
        assertEquals(1, recentPins.clearCallCount)
        assertEquals(1, runtimeStorage.clearImageCachesCallCount)
        assertEquals(1, settings.resetAllSettingsCallCount)
        assertEquals(1, localModels.clearDownloadedModelsCallCount)
    }

    private class FakeAnnotationSessionRepository : AnnotationSessionRepository {
        var clearAllCallCount = 0

        override fun save(session: AnnotationSession): String = "session"

        override fun get(sessionId: String): AnnotationSession? = null

        override fun listSessionFiles(): List<AnnotationSessionFile> = emptyList()

        override fun count(): Int = 0

        override fun visibleDirectoryPath(): String = ""

        override fun clearAll() {
            clearAllCallCount += 1
        }

        override fun prune(maxCount: Int, maxDays: Int) = Unit
    }

    private class FakePinHistoryRepository : PinHistoryRepository {
        var clearCallCount = 0

        override fun save(
            imageUri: String,
            annotationSessionId: String?,
            sourceType: PinHistorySourceType,
            metadata: PinHistoryMetadata
        ): String = "history"

        override fun list(): List<PinHistoryRecord> = emptyList()

        override fun get(id: String): PinHistoryRecord? = null

        override fun delete(id: String) = Unit

        override fun clear() {
            clearCallCount += 1
        }

        override fun prune(maxCount: Int, maxDays: Int) = Unit

        override fun visibleDirectoryPath(): String = ""
    }

    private class FakeRecentPinRepository : RecentPinRepository {
        var clearCallCount = 0

        override fun push(record: ClosedPinRecord) = Unit

        override fun popMostRecent(): ClosedPinRecord? = null

        override fun hasRecent(): Boolean = false

        override fun count(): Int = 0

        override fun clear() {
            clearCallCount += 1
        }
    }

    private class FakeRuntimeStorageRepository : RuntimeStorageRepository {
        var clearImageCachesCallCount = 0

        override fun snapshot(): RuntimeStorageSnapshot = RuntimeStorageSnapshot(
            screenshotsCacheBytes = 0,
            pinnedCacheBytes = 0,
            shareCacheBytes = 0,
            importCacheBytes = 0,
            textPinCacheBytes = 0,
            annotationSessionBytes = 0,
            pinHistoryBytes = 0
        )

        override fun clearImageCaches() {
            clearImageCachesCallCount += 1
        }

        override fun clearRecordCaches() = Unit

        override fun clearAllRuntimeFiles() = Unit
    }

    private class FakeLocalTranslationModelResetter : LocalTranslationModelResetter {
        var clearDownloadedModelsCallCount = 0

        override suspend fun clearDownloadedModels() {
            clearDownloadedModelsCallCount += 1
        }
    }

    private class FakeAppSettingsRepository : AppSettingsRepository {
        var resetAllSettingsCallCount = 0

        override fun getCaptureResultAction(): CaptureResultAction = CaptureResultAction.OPEN_EDITOR

        override fun setCaptureResultAction(action: CaptureResultAction) = Unit

        override fun getFavoriteEditorColors(): List<Int> = emptyList()

        override fun setFavoriteEditorColors(colors: List<Int>) = Unit

        override fun getRecentEditorColors(): List<Int> = emptyList()

        override fun setRecentEditorColors(colors: List<Int>) = Unit

        override fun getPinScaleMode(): PinScaleMode = PinScaleMode.LOCK_ASPECT

        override fun setPinScaleMode(mode: PinScaleMode) = Unit

        override fun getProjectRecordSettings(): ProjectRecordSettings {
            return ProjectRecordSettings(maxSessionCount = 50, retainDays = 7)
        }

        override fun setMaxSessionCount(count: Int) = Unit

        override fun setRetainDays(days: Int) = Unit

        override fun getPinAppearanceSettings(): PinAppearanceSettings {
            return PinAppearanceSettings(shadowEnabled = true, cornerRadiusDp = 0f)
        }

        override fun isPinShadowEnabledByDefault(): Boolean = true

        override fun setPinShadowEnabledByDefault(enabled: Boolean) = Unit

        override fun getDefaultPinCornerRadiusDp(): Float = 0f

        override fun setDefaultPinCornerRadiusDp(radiusDp: Float) = Unit

        override fun getFloatingBallSettings(): FloatingBallSettings {
            return FloatingBallSettings(
                sizeDp = 60,
                opacity = 0.92f,
                theme = FloatingBallTheme.BLUE_PURPLE,
                appearanceMode = FloatingBallAppearanceMode.THEME,
                customImageUri = null
            )
        }

        override fun setFloatingBallSizeDp(sizeDp: Int) = Unit

        override fun setFloatingBallOpacity(opacity: Float) = Unit

        override fun setFloatingBallTheme(theme: FloatingBallTheme) = Unit

        override fun setFloatingBallAppearanceMode(mode: FloatingBallAppearanceMode) = Unit

        override fun setFloatingBallCustomImageUri(uri: String?) = Unit

        override fun getOnboardingGuideProgress(): OnboardingGuideProgress {
            return OnboardingGuideProgress(
                hasSeenHomeGuide = false,
                hasSeenFloatingBallHint = false,
                hasSeenPinOverlayHint = false,
                hasSeenEditorHint = false
            )
        }

        override fun setHomeOnboardingGuideSeen(seen: Boolean) = Unit

        override fun setFloatingBallHintSeen(seen: Boolean) = Unit

        override fun setPinOverlayHintSeen(seen: Boolean) = Unit

        override fun setEditorHintSeen(seen: Boolean) = Unit

        override fun getPinHistorySettings(): PinHistorySettings {
            return PinHistorySettings(enabled = true, maxCount = 50, retainDays = 14)
        }

        override fun setPinHistoryEnabled(enabled: Boolean) = Unit

        override fun setMaxPinHistoryCount(count: Int) = Unit

        override fun setPinHistoryRetainDays(days: Int) = Unit

        override fun getTranslationSettings(): TranslationSettings {
            return TranslationSettings(
                localTargetLanguageTag = "en",
                localDownloadOnWifiOnly = true,
                cloudProvider = CloudTranslationProvider.NONE,
                baiduAppId = "",
                baiduSecretKey = "",
                youdaoAppKey = "",
                youdaoAppSecret = ""
            )
        }

        override fun setLocalTranslationTargetLanguageTag(languageTag: String) = Unit

        override fun setLocalTranslationDownloadOnWifiOnly(enabled: Boolean) = Unit

        override fun setCloudTranslationProvider(provider: CloudTranslationProvider) = Unit

        override fun setBaiduTranslationCredentials(appId: String, secretKey: String) = Unit

        override fun setYoudaoTranslationCredentials(appKey: String, appSecret: String) = Unit

        override fun resetAllSettings() {
            resetAllSettingsCallCount += 1
        }
    }
}
