package com.muding.android.presentation.ocr

import com.muding.android.data.settings.CloudTranslationProvider
import com.muding.android.data.settings.TranslationSettings
import com.muding.android.feature.translation.TranslationEngine
import com.muding.android.feature.translation.TranslationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Test

class OcrTranslationRoutingTest {

    @Test
    fun `uses local translation engine when cloud translation is disabled`() = runBlocking {
        val localEngine = RecordingEngine("local")
        val cloudEngine = RecordingEngine("cloud")

        val resolved = resolveOcrTranslationEngine(
            settings = TranslationSettings(
                localTargetLanguageTag = "ja",
                localDownloadOnWifiOnly = true,
                cloudProvider = CloudTranslationProvider.NONE,
                baiduAppId = "",
                baiduSecretKey = "",
                youdaoAppKey = "",
                youdaoAppSecret = ""
            ),
            localEngine = localEngine,
            cloudEngine = cloudEngine
        )

        assertSame(localEngine, resolved)
    }

    @Test
    fun `uses cloud translation engine when a cloud provider is configured`() = runBlocking {
        val localEngine = RecordingEngine("local")
        val cloudEngine = RecordingEngine("cloud")

        val resolved = resolveOcrTranslationEngine(
            settings = TranslationSettings(
                localTargetLanguageTag = "ja",
                localDownloadOnWifiOnly = true,
                cloudProvider = CloudTranslationProvider.YOUDAO,
                baiduAppId = "",
                baiduSecretKey = "",
                youdaoAppKey = "key",
                youdaoAppSecret = "secret"
            ),
            localEngine = localEngine,
            cloudEngine = cloudEngine
        )

        assertSame(cloudEngine, resolved)
    }

    private class RecordingEngine(
        private val label: String
    ) : TranslationEngine {
        override suspend fun translate(text: String, targetLanguageTag: String): TranslationResult {
            return TranslationResult(
                translatedText = "$label:$text->$targetLanguageTag",
                providerLabel = label
            )
        }
    }
}
