package com.muding.android.feature.translation

import com.muding.android.data.settings.CloudTranslationProvider

data class TranslationLanguageOption(
    val appTag: String,
    val displayName: String,
    val mlKitTag: String?,
    val baiduCode: String,
    val youdaoCode: String
)

data class TranslationResult(
    val translatedText: String,
    val providerLabel: String
)

object TranslationLanguageCatalog {
    val options = listOf(
        TranslationLanguageOption("en", "英语", "en", "en", "en"),
        TranslationLanguageOption("zh", "中文", "zh", "zh", "zh-CHS"),
        TranslationLanguageOption("ja", "日语", "ja", "jp", "ja"),
        TranslationLanguageOption("ko", "韩语", "ko", "kor", "ko"),
        TranslationLanguageOption("fr", "法语", "fr", "fra", "fr"),
        TranslationLanguageOption("de", "德语", "de", "de", "de")
    )

    fun findByAppTag(tag: String): TranslationLanguageOption {
        return options.firstOrNull { it.appTag == tag } ?: options.first()
    }
}

fun CloudTranslationProvider.displayName(): String {
    return when (this) {
        CloudTranslationProvider.NONE -> "不使用云翻译"
        CloudTranslationProvider.BAIDU -> "百度翻译"
        CloudTranslationProvider.YOUDAO -> "有道智云"
    }
}
