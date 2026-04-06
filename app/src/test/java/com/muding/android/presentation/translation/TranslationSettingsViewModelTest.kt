package com.muding.android.presentation.translation

import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.data.settings.CloudTranslationProvider
import com.muding.android.data.settings.FloatingBallSettings
import com.muding.android.data.settings.OnboardingGuideProgress
import com.muding.android.data.settings.PinAppearanceSettings
import com.muding.android.data.settings.PinHistorySettings
import com.muding.android.data.settings.ProjectRecordSettings
import com.muding.android.data.settings.TranslationSettings
import com.muding.android.domain.usecase.FloatingBallAppearanceMode
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.domain.usecase.FloatingBallTheme
import com.muding.android.domain.usecase.PinScaleMode
import com.muding.android.feature.translation.TranslationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationSettingsViewModelTest {

    @Test
    fun `ui state hides cloud credentials when provider disabled`() = runBlocking {
        val viewModel = TranslationSettingsViewModel(
            settingsRepository = FakeAppSettingsRepository(
                TranslationSettings(
                    localTargetLanguageTag = "ja",
                    localDownloadOnWifiOnly = true,
                    cloudProvider = CloudTranslationProvider.NONE,
                    baiduAppId = "",
                    baiduSecretKey = "",
                    youdaoAppKey = "",
                    youdaoAppSecret = ""
                )
            ),
            localModelGateway = FakeLocalModelGateway(downloadedLanguageTags = setOf("ja")),
            cloudVerifier = FakeCloudVerifier()
        )

        viewModel.refreshDownloadedModels()

        assertFalse(viewModel.uiState.showCloudCredentials)
        assertFalse(viewModel.uiState.showCloudVerificationAction)
        assertEquals("已下载", viewModel.uiState.localModelStatusLabel)
        assertEquals("删除当前模型", viewModel.uiState.localModelActionLabel)
    }

    @Test
    fun `ui state shows only youdao credentials when provider is youdao`() = runBlocking {
        val viewModel = TranslationSettingsViewModel(
            settingsRepository = FakeAppSettingsRepository(),
            localModelGateway = FakeLocalModelGateway(),
            cloudVerifier = FakeCloudVerifier()
        )

        viewModel.selectCloudProvider(CloudTranslationProvider.YOUDAO)

        assertTrue(viewModel.uiState.showCloudCredentials)
        assertTrue(viewModel.uiState.showYoudaoCredentials)
        assertFalse(viewModel.uiState.showBaiduCredentials)
        assertTrue(viewModel.uiState.showCloudVerificationAction)
    }

    @Test
    fun `ui state shows only baidu credentials when provider is baidu`() = runBlocking {
        val viewModel = TranslationSettingsViewModel(
            settingsRepository = FakeAppSettingsRepository(),
            localModelGateway = FakeLocalModelGateway(),
            cloudVerifier = FakeCloudVerifier()
        )

        viewModel.selectCloudProvider(CloudTranslationProvider.BAIDU)

        assertTrue(viewModel.uiState.showCloudCredentials)
        assertFalse(viewModel.uiState.showYoudaoCredentials)
        assertTrue(viewModel.uiState.showBaiduCredentials)
        assertTrue(viewModel.uiState.showCloudVerificationAction)
    }

    @Test
    fun `ui state treats english as built in local translation`() = runBlocking {
        val viewModel = TranslationSettingsViewModel(
            settingsRepository = FakeAppSettingsRepository(
                TranslationSettings(
                    localTargetLanguageTag = "en",
                    localDownloadOnWifiOnly = true,
                    cloudProvider = CloudTranslationProvider.NONE,
                    baiduAppId = "",
                    baiduSecretKey = "",
                    youdaoAppKey = "",
                    youdaoAppSecret = ""
                )
            ),
            localModelGateway = FakeLocalModelGateway(),
            cloudVerifier = FakeCloudVerifier()
        )

        viewModel.refreshDownloadedModels()

        assertEquals("内置可用", viewModel.uiState.localModelStatusLabel)
        assertNull(viewModel.uiState.localModelActionLabel)
    }

    @Test
    fun `clear transient messages resets local and cloud feedback when section is reopened`() = runBlocking {
        val viewModel = TranslationSettingsViewModel(
            settingsRepository = FakeAppSettingsRepository(),
            localModelGateway = FakeLocalModelGateway(downloadedLanguageTags = setOf("ja")),
            cloudVerifier = FakeCloudVerifier()
        )

        viewModel.refreshDownloadedModels()
        viewModel.selectCloudProvider(CloudTranslationProvider.YOUDAO)
        viewModel.updateYoudaoAppKey("demo-key")
        viewModel.updateYoudaoAppSecret("demo-secret")
        viewModel.performCurrentLocalModelAction()
        viewModel.saveAndVerifyCurrentProvider()

        assertTrue(viewModel.uiState.localMessage != null || viewModel.uiState.cloudMessage != null)

        viewModel.clearTransientMessages()

        assertNull(viewModel.uiState.localMessage)
        assertNull(viewModel.uiState.cloudMessage)
    }

    private class FakeCloudVerifier : CloudTranslationVerifier {
        override suspend fun verify(targetLanguageTag: String): TranslationResult {
            return TranslationResult(
                translatedText = "verified:$targetLanguageTag",
                providerLabel = "验证通过"
            )
        }
    }

    private class FakeLocalModelGateway(
        private var downloadedLanguageTags: Set<String> = emptySet()
    ) : LocalTranslationModelGateway {

        override suspend fun getDownloadedLanguageTags(): Set<String> = downloadedLanguageTags

        override suspend fun download(languageTag: String, wifiOnly: Boolean) {
            downloadedLanguageTags = downloadedLanguageTags + languageTag
        }

        override suspend fun delete(languageTag: String) {
            downloadedLanguageTags = downloadedLanguageTags - languageTag
        }
    }

    private class FakeAppSettingsRepository(
        private var translationSettings: TranslationSettings = TranslationSettings(
            localTargetLanguageTag = "ja",
            localDownloadOnWifiOnly = true,
            cloudProvider = CloudTranslationProvider.NONE,
            baiduAppId = "baidu-id",
            baiduSecretKey = "baidu-secret",
            youdaoAppKey = "youdao-key",
            youdaoAppSecret = "youdao-secret"
        )
    ) : AppSettingsRepository {

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
            return PinAppearanceSettings(shadowEnabled = true, cornerRadiusDp = 12f)
        }

        override fun isPinShadowEnabledByDefault(): Boolean = true

        override fun setPinShadowEnabledByDefault(enabled: Boolean) = Unit

        override fun getDefaultPinCornerRadiusDp(): Float = 12f

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

        override fun getTranslationSettings(): TranslationSettings = translationSettings

        override fun setLocalTranslationTargetLanguageTag(languageTag: String) {
            translationSettings = translationSettings.copy(localTargetLanguageTag = languageTag)
        }

        override fun setLocalTranslationDownloadOnWifiOnly(enabled: Boolean) {
            translationSettings = translationSettings.copy(localDownloadOnWifiOnly = enabled)
        }

        override fun setCloudTranslationProvider(provider: CloudTranslationProvider) {
            translationSettings = translationSettings.copy(cloudProvider = provider)
        }

        override fun setBaiduTranslationCredentials(appId: String, secretKey: String) {
            translationSettings = translationSettings.copy(
                baiduAppId = appId,
                baiduSecretKey = secretKey
            )
        }

        override fun setYoudaoTranslationCredentials(appKey: String, appSecret: String) {
            translationSettings = translationSettings.copy(
                youdaoAppKey = appKey,
                youdaoAppSecret = appSecret
            )
        }

        override fun resetAllSettings() = Unit
    }
}
