package com.pixpin.android.feature.translation

interface TranslationEngine {
    suspend fun translate(text: String, targetLanguageTag: String): TranslationResult
}
