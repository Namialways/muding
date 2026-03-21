package com.muding.android.feature.translation

interface TranslationEngine {
    suspend fun translate(text: String, targetLanguageTag: String): TranslationResult
}
