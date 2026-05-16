package com.muding.android.feature.translation

object LocalTranslationLanguageResolver {

    fun resolveSourceLanguage(
        text: String,
        detectedLanguageCode: String
    ): TranslationLanguageOption? {
        if (hasSpanishPhraseCue(text)) {
            return fromAppTag("es")
        }
        return fromLanguageId(detectedLanguageCode)
    }

    fun fromLanguageId(languageCode: String): TranslationLanguageOption? {
        val normalizedCode = languageCode.trim().lowercase()
        val appTag = when (normalizedCode) {
            "zh", "zh-cn", "zh-tw" -> "zh"
            "en" -> "en"
            "ja" -> "ja"
            "ko" -> "ko"
            "fr" -> "fr"
            "de" -> "de"
            "es" -> "es"
            else -> null
        }
        return appTag?.let(::fromAppTag)
    }

    fun fromAppTag(languageTag: String): TranslationLanguageOption? {
        return TranslationLanguageCatalog.options.firstOrNull { it.appTag == languageTag }
    }

    fun fromMlKitTag(mlKitTag: String): TranslationLanguageOption? {
        return TranslationLanguageCatalog.options.firstOrNull { it.mlKitTag == mlKitTag }
    }

    fun missingModelMessage(
        sourceLanguage: TranslationLanguageOption,
        targetLanguage: TranslationLanguageOption,
        sourceDownloaded: Boolean,
        targetDownloaded: Boolean
    ): String {
        return when {
            !sourceDownloaded && targetDownloaded -> {
                "识别到源语言为${sourceLanguage.displayName}，但未下载${sourceLanguage.displayName}模型。请在翻译设置中下载该模型。"
            }

            sourceDownloaded && !targetDownloaded -> {
                "当前目标语言为${targetLanguage.displayName}，但未下载${targetLanguage.displayName}模型。请切换到已下载的目标语言，或在翻译设置中下载${targetLanguage.displayName}模型。"
            }

            !sourceDownloaded && !targetDownloaded -> {
                "识别到源语言为${sourceLanguage.displayName}，当前目标语言为${targetLanguage.displayName}。请在翻译设置中下载${sourceLanguage.displayName}和${targetLanguage.displayName}模型，或切换到已下载的目标语言。"
            }

            else -> missingModelMessage(emptyList())
        }
    }

    fun missingModelMessage(missingLanguages: List<TranslationLanguageOption>): String {
        val names = missingLanguages
            .distinctBy { it.appTag }
            .joinToString("、") { it.displayName }
        return if (names.isBlank()) {
            "请先在翻译设置中下载对应语言模型"
        } else {
            "请先在翻译设置中下载${names}模型"
        }
    }

    fun unsupportedSourceLanguageMessage(languageCode: String): String {
        return if (languageCode == "und") {
            "未能识别文本语言，暂不支持本地翻译"
        } else {
            "当前识别语言暂不支持本地翻译"
        }
    }

    private fun hasSpanishPhraseCue(text: String): Boolean {
        val words = Regex("[\\p{L}]+")
            .findAll(text.lowercase())
            .map { it.value }
            .toSet()
        val strongSpanishWords = setOf(
            "suscribiste",
            "suscrito",
            "suscribirse",
            "suscripcion",
            "suscripción",
            "estás",
            "estas",
            "estoy",
            "esta",
            "está"
        )
        if (words.any { it in strongSpanishWords }) {
            return true
        }
        return "ya" in words && "te" in words
    }
}
