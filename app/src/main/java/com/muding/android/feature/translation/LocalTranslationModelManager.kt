package com.muding.android.feature.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalTranslationModelManager(
    private val remoteModelManager: RemoteModelManager = RemoteModelManager.getInstance()
) {

    suspend fun getDownloadedLanguageTags(): Set<String> {
        return withContext(Dispatchers.IO) {
            remoteModelManager
                .getDownloadedModels(TranslateRemoteModel::class.java)
                .awaitTask()
                .mapNotNull { model ->
                    model.language
                }
                .toSet()
        }
    }

    suspend fun isDownloaded(languageTag: String): Boolean {
        val mlKitTag = normalizeMlKitTag(languageTag) ?: return languageTag == "en"
        if (mlKitTag == TranslateLanguage.ENGLISH) {
            return true
        }
        return withContext(Dispatchers.IO) {
            remoteModelManager.isModelDownloaded(
                TranslateRemoteModel.Builder(mlKitTag).build()
            ).awaitTask()
        }
    }

    suspend fun download(languageTag: String, wifiOnly: Boolean) {
        val mlKitTag = normalizeMlKitTag(languageTag)
            ?: throw IllegalArgumentException("Unsupported local translation language: $languageTag")
        if (mlKitTag == TranslateLanguage.ENGLISH) {
            return
        }
        val conditions = DownloadConditions.Builder().apply {
            if (wifiOnly) {
                requireWifi()
            }
        }.build()
        withContext(Dispatchers.IO) {
            remoteModelManager.download(
                TranslateRemoteModel.Builder(mlKitTag).build(),
                conditions
            ).awaitTask()
        }
    }

    suspend fun delete(languageTag: String) {
        val mlKitTag = normalizeMlKitTag(languageTag)
            ?: throw IllegalArgumentException("Unsupported local translation language: $languageTag")
        if (mlKitTag == TranslateLanguage.ENGLISH) {
            return
        }
        withContext(Dispatchers.IO) {
            remoteModelManager.deleteDownloadedModel(
                TranslateRemoteModel.Builder(mlKitTag).build()
            ).awaitTask()
        }
    }

    private fun normalizeMlKitTag(languageTag: String): String? {
        return when (languageTag) {
            "en" -> TranslateLanguage.ENGLISH
            "zh" -> TranslateLanguage.CHINESE
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            else -> null
        }
    }
}
