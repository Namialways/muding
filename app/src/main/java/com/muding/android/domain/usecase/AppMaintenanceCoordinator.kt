package com.muding.android.domain.usecase

import com.muding.android.data.repository.AnnotationSessionRepository
import com.muding.android.data.repository.PinHistoryRepository
import com.muding.android.data.repository.RecentPinRepository
import com.muding.android.data.repository.RuntimeStorageRepository
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.feature.translation.LocalTranslationModelResetter

class AppMaintenanceCoordinator(
    private val annotationSessionRepository: AnnotationSessionRepository,
    private val pinHistoryRepository: PinHistoryRepository,
    private val recentPinRepository: RecentPinRepository,
    private val runtimeStorageRepository: RuntimeStorageRepository,
    private val settingsRepository: AppSettingsRepository,
    private val localTranslationModelResetter: LocalTranslationModelResetter
) {

    fun clearWorkRecords() {
        annotationSessionRepository.clearAll()
        pinHistoryRepository.clear()
        recentPinRepository.clear()
        runtimeStorageRepository.clearImageCaches()
    }

    suspend fun resetApplication() {
        clearWorkRecords()
        settingsRepository.resetAllSettings()
        localTranslationModelResetter.clearDownloadedModels()
    }
}
