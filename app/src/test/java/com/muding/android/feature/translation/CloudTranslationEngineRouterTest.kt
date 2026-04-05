package com.muding.android.feature.translation

import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.data.settings.CloudTranslationProvider
import com.muding.android.data.settings.FloatingBallSettings
import com.muding.android.data.settings.PinAppearanceSettings
import com.muding.android.data.settings.PinHistorySettings
import com.muding.android.data.settings.ProjectRecordSettings
import com.muding.android.data.settings.TranslationSettings
import com.muding.android.domain.usecase.FloatingBallAppearanceMode
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.domain.usecase.FloatingBallTheme
import com.muding.android.domain.usecase.PinScaleMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudTranslationEngineRouterTest {

    @Test
    fun `router delegates to baidu engine`() = runBlocking {
        val baiduEngine = RecordingEngine("百度翻译")
        val youdaoEngine = RecordingEngine("有道翻译")
        val router = CloudTranslationEngineRouter(
            settingsRepository = FakeAppSettingsRepository(CloudTranslationProvider.BAIDU),
            baiduEngine = baiduEngine,
            youdaoEngine = youdaoEngine
        )

        val result = router.translate("hello", "en")

        assertEquals("百度翻译", result.providerLabel)
        assertEquals(1, baiduEngine.callCount)
        assertEquals(0, youdaoEngine.callCount)
    }

    @Test
    fun `router delegates to youdao engine`() = runBlocking {
        val baiduEngine = RecordingEngine("百度翻译")
        val youdaoEngine = RecordingEngine("有道翻译")
        val router = CloudTranslationEngineRouter(
            settingsRepository = FakeAppSettingsRepository(CloudTranslationProvider.YOUDAO),
            baiduEngine = baiduEngine,
            youdaoEngine = youdaoEngine
        )

        val result = router.translate("hello", "en")

        assertEquals("有道翻译", result.providerLabel)
        assertEquals(0, baiduEngine.callCount)
        assertEquals(1, youdaoEngine.callCount)
    }

    @Test
    fun `router requires provider selection`() = runBlocking {
        val router = CloudTranslationEngineRouter(
            settingsRepository = FakeAppSettingsRepository(CloudTranslationProvider.NONE),
            baiduEngine = RecordingEngine("百度翻译"),
            youdaoEngine = RecordingEngine("有道翻译")
        )

        val result = runCatching {
            router.translate("hello", "en")
        }

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? TranslationException
        assertEquals(TranslationFailureType.CLOUD_PROVIDER_NOT_SELECTED, exception?.type)
        assertEquals("请先在翻译设置中选择云翻译服务商", TranslationErrorMessages.resolve(exception!!))
    }

    private class RecordingEngine(
        private val providerLabel: String
    ) : TranslationEngine {

        var callCount: Int = 0
            private set

        override suspend fun translate(text: String, targetLanguageTag: String): TranslationResult {
            callCount += 1
            return TranslationResult(
                translatedText = "$providerLabel:$text->$targetLanguageTag",
                providerLabel = providerLabel
            )
        }
    }

    private class FakeAppSettingsRepository(
        private val provider: CloudTranslationProvider
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
                cloudProvider = provider,
                baiduAppId = "baidu-id",
                baiduSecretKey = "baidu-secret",
                youdaoAppKey = "youdao-key",
                youdaoAppSecret = "youdao-secret"
            )
        }

        override fun setLocalTranslationTargetLanguageTag(languageTag: String) = Unit

        override fun setLocalTranslationDownloadOnWifiOnly(enabled: Boolean) = Unit

        override fun setCloudTranslationProvider(provider: CloudTranslationProvider) = Unit

        override fun setBaiduTranslationCredentials(appId: String, secretKey: String) = Unit

        override fun setYoudaoTranslationCredentials(appKey: String, appSecret: String) = Unit

        override fun resetAllSettings() = Unit
    }
}
