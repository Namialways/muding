package com.muding.android.feature.translation

import com.google.mlkit.nl.languageid.LanguageIdentification
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
        val sourceLanguageCode = detectSourceLanguageCode(normalizedText)
        val sourceOption = LocalTranslationLanguageResolver.resolveSourceLanguage(
            text = normalizedText,
            detectedLanguageCode = sourceLanguageCode
        )
            ?: throw TranslationException(
                type = TranslationFailureType.UNSUPPORTED_SOURCE_LANGUAGE,
                providerLabel = "本地翻译",
                message = LocalTranslationLanguageResolver.unsupportedSourceLanguageMessage(sourceLanguageCode)
            )
        val sourceMlKitTag = sourceOption.mlKitTag
            ?: throw TranslationException(
                type = TranslationFailureType.UNSUPPORTED_SOURCE_LANGUAGE,
                providerLabel = "本地翻译",
                message = LocalTranslationLanguageResolver.unsupportedSourceLanguageMessage(sourceLanguageCode)
            )
        if (sourceMlKitTag == targetMlKitTag) {
            return TranslationResult(
                translatedText = normalizedText,
                providerLabel = "本地翻译"
            )
        }
        val sourceDownloaded = modelManager.isDownloaded(sourceOption.appTag)
        val targetDownloaded = modelManager.isDownloaded(targetLanguageTag)
        if (!sourceDownloaded || !targetDownloaded) {
            throw TranslationException(
                type = TranslationFailureType.LOCAL_MODEL_MISSING,
                providerLabel = "本地翻译",
                message = LocalTranslationLanguageResolver.missingModelMessage(
                    sourceLanguage = sourceOption,
                    targetLanguage = targetOption,
                    sourceDownloaded = sourceDownloaded,
                    targetDownloaded = targetDownloaded
                )
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

    private suspend fun detectSourceLanguageCode(text: String): String {
        val identifier = LanguageIdentification.getClient()
        return try {
            withContext(Dispatchers.IO) {
                identifier.identifyLanguage(text).awaitTask()
            }
        } finally {
            identifier.close()
        }
    }
}
