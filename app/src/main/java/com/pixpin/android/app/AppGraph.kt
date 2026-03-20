package com.pixpin.android.app

import android.content.Context
import com.pixpin.android.data.image.CachedImageRepository
import com.pixpin.android.data.image.FileCachedImageRepository
import com.pixpin.android.data.image.ImageExportRepository
import com.pixpin.android.data.image.SystemImageExportRepository
import com.pixpin.android.data.repository.AnnotationSessionRepository
import com.pixpin.android.data.repository.FileAnnotationSessionRepository
import com.pixpin.android.data.repository.FilePinHistoryRepository
import com.pixpin.android.data.repository.PinHistoryRepository
import com.pixpin.android.data.repository.RecentPinRepository
import com.pixpin.android.data.repository.RuntimeStorageRepository
import com.pixpin.android.data.repository.SharedPreferencesRecentPinRepository
import com.pixpin.android.data.repository.SystemRuntimeStorageRepository
import com.pixpin.android.data.settings.AppSettingsRepository
import com.pixpin.android.data.settings.SharedPreferencesAppSettingsRepository
import com.pixpin.android.feature.capture.BitmapCropper
import com.pixpin.android.feature.capture.CaptureFlowCoordinator
import com.pixpin.android.feature.pin.creation.PinCreationCoordinator

object AppGraph {

    fun appSettingsRepository(context: Context): AppSettingsRepository {
        return SharedPreferencesAppSettingsRepository(context.applicationContext)
    }

    fun annotationSessionRepository(context: Context): AnnotationSessionRepository {
        return FileAnnotationSessionRepository(context.applicationContext)
    }

    fun pinHistoryRepository(context: Context): PinHistoryRepository {
        return FilePinHistoryRepository(context.applicationContext)
    }

    fun recentPinRepository(context: Context): RecentPinRepository {
        return SharedPreferencesRecentPinRepository(context.applicationContext)
    }

    fun runtimeStorageRepository(context: Context): RuntimeStorageRepository {
        return SystemRuntimeStorageRepository(context.applicationContext)
    }

    fun cachedImageRepository(context: Context): CachedImageRepository {
        return FileCachedImageRepository(context.applicationContext)
    }

    fun imageExportRepository(context: Context): ImageExportRepository {
        return SystemImageExportRepository(context.applicationContext)
    }

    fun pinCreationCoordinator(context: Context): PinCreationCoordinator {
        return PinCreationCoordinator(
            settingsRepository = appSettingsRepository(context),
            cachedImageRepository = cachedImageRepository(context)
        )
    }

    fun captureFlowCoordinator(context: Context): CaptureFlowCoordinator {
        return CaptureFlowCoordinator(
            bitmapCropper = BitmapCropper(),
            pinCreationCoordinator = pinCreationCoordinator(context)
        )
    }
}
