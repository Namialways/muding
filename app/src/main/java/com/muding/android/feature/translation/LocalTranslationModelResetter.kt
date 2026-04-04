package com.muding.android.feature.translation

interface LocalTranslationModelResetter {
    suspend fun clearDownloadedModels()
}

class DefaultLocalTranslationModelResetter(
    private val modelManager: LocalTranslationModelManager
) : LocalTranslationModelResetter {
    override suspend fun clearDownloadedModels() {
        modelManager.clearDownloadedModels()
    }
}
