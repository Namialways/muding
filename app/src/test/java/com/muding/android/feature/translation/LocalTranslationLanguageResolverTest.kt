package com.muding.android.feature.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocalTranslationLanguageResolverTest {

    @Test
    fun `maps spanish language id to downloadable local model`() {
        val resolved = LocalTranslationLanguageResolver.fromLanguageId("es")

        assertNotNull(resolved)
        assertEquals("es", resolved?.appTag)
    }

    @Test
    fun `uses spanish phrase cues when detector reports french`() {
        val resolved = LocalTranslationLanguageResolver.resolveSourceLanguage(
            text = "Ya te suscribiste a ChatGPT Plus",
            detectedLanguageCode = "fr"
        )

        assertEquals("es", resolved?.appTag)
    }

    @Test
    fun `missing model message names spanish model`() {
        val missingLanguage = TranslationLanguageCatalog.findByAppTag("es")

        val message = LocalTranslationLanguageResolver.missingModelMessage(
            missingLanguages = listOf(missingLanguage)
        )

        assertEquals("请先在翻译设置中下载西班牙语模型", message)
    }

    @Test
    fun `target missing model message suggests switching target language`() {
        val sourceLanguage = TranslationLanguageCatalog.findByAppTag("es")
        val targetLanguage = TranslationLanguageCatalog.findByAppTag("fr")

        val message = LocalTranslationLanguageResolver.missingModelMessage(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            sourceDownloaded = true,
            targetDownloaded = false
        )

        assertEquals(
            "当前目标语言为法语，但未下载法语模型。请切换到已下载的目标语言，或在翻译设置中下载法语模型。",
            message
        )
    }

    @Test
    fun `source missing model message explains detected source language`() {
        val sourceLanguage = TranslationLanguageCatalog.findByAppTag("es")
        val targetLanguage = TranslationLanguageCatalog.findByAppTag("zh")

        val message = LocalTranslationLanguageResolver.missingModelMessage(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            sourceDownloaded = false,
            targetDownloaded = true
        )

        assertEquals(
            "识别到源语言为西班牙语，但未下载西班牙语模型。请在翻译设置中下载该模型。",
            message
        )
    }

    @Test
    fun `unsupported language message does not ask user to download model`() {
        val resolved = LocalTranslationLanguageResolver.fromLanguageId("it")

        assertEquals(null, resolved)
        assertEquals(
            "当前识别语言暂不支持本地翻译",
            LocalTranslationLanguageResolver.unsupportedSourceLanguageMessage("it")
        )
    }
}
