package com.pixpin.android.feature.translation

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MlKitLocalTranslationEngine(
    private val modelManager: LocalTranslationModelManager
) : TranslationEngine {

    override suspend fun translate(text: String, targetLanguageTag: String): TranslationResult {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) {
            throw TranslationException(
                type = TranslationFailureType.EMPTY_TEXT,
                providerLabel = "本地翻译"
            )
        }
        val targetOption = TranslationLanguageCatalog.findByAppTag(targetLanguageTag)
        val targetMlKitTag = targetOption.mlKitTag
            ?: throw TranslationException(
                type = TranslationFailureType.UNSUPPORTED_TARGET_LANGUAGE,
                providerLabel = "本地翻译"
            )
        val sourceMlKitTag = detectSourceLanguage(normalizedText)
        val sourceAppTag = TranslationLanguageCatalog.options
            .firstOrNull { it.mlKitTag == sourceMlKitTag }
            ?.appTag
            ?: "zh"
        if (!modelManager.isDownloaded(sourceAppTag) || !modelManager.isDownloaded(targetLanguageTag)) {
            throw TranslationException(
                type = TranslationFailureType.LOCAL_MODEL_MISSING,
                providerLabel = "本地翻译"
            )
        }
        if (sourceMlKitTag == targetMlKitTag) {
            return TranslationResult(
                translatedText = normalizedText,
                providerLabel = "本地翻译"
            )
        }
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceMlKitTag)
                .setTargetLanguage(targetMlKitTag)
                .build()
        )
        return try {
            val translated = withContext(Dispatchers.IO) {
                translator.translate(normalizedText).awaitTask()
            }
            TranslationResult(
                translatedText = translated,
                providerLabel = "本地翻译"
            )
        } catch (e: Exception) {
            throw TranslationErrorMessages.mapGenericThrowable(e, providerLabel = "本地翻译")
        } finally {
            translator.close()
        }
    }

    private suspend fun detectSourceLanguage(text: String): String {
        val identifier = LanguageIdentification.getClient()
        val languageCode = withContext(Dispatchers.IO) {
            identifier.identifyLanguage(text).awaitTask()
        }
        return when (languageCode) {
            "zh", "zh-CN", "zh-TW" -> TranslateLanguage.CHINESE
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            "en" -> TranslateLanguage.ENGLISH
            else -> TranslateLanguage.CHINESE
        }
    }
}
