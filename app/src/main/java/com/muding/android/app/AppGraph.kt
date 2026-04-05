package com.muding.android.app

import android.content.Context
import com.muding.android.data.image.CachedImageRepository
import com.muding.android.data.image.FileCachedImageRepository
import com.muding.android.data.image.ImageExportRepository
import com.muding.android.data.image.SystemImageExportRepository
import com.muding.android.data.repository.AnnotationSessionRepository
import com.muding.android.data.repository.FileAnnotationSessionRepository
import com.muding.android.data.repository.FilePinHistoryRepository
import com.muding.android.data.repository.PinHistoryRepository
import com.muding.android.data.repository.RecentPinRepository
import com.muding.android.data.repository.RuntimeStorageRepository
import com.muding.android.data.repository.SharedPreferencesRecentPinRepository
import com.muding.android.data.repository.SystemRuntimeStorageRepository
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.data.settings.SharedPreferencesAppSettingsRepository
import com.muding.android.feature.capture.BitmapCropper
import com.muding.android.feature.capture.CaptureFlowCoordinator
import com.muding.android.feature.floatingball.FloatingBallImageProcessor
import com.muding.android.feature.ocr.MlKitOcrEngine
import com.muding.android.feature.ocr.OcrEngine
import com.muding.android.feature.ocr.OcrFlowCoordinator
import com.muding.android.feature.pin.creation.PinCreationCoordinator
import com.muding.android.feature.pin.source.ImageUriPinSourceAdapter
import com.muding.android.feature.pin.source.PinSourceAssetResolver
import com.muding.android.feature.pin.source.TextBitmapRenderer
import com.muding.android.feature.pin.source.TextPinSourceAdapter
import com.muding.android.feature.translation.BaiduCloudTranslationEngine
import com.muding.android.feature.translation.CloudTranslationEngineRouter
import com.muding.android.feature.translation.DefaultLocalTranslationModelResetter
import com.muding.android.feature.translation.LocalTranslationModelManager
import com.muding.android.feature.translation.LocalTranslationModelResetter
import com.muding.android.feature.translation.MlKitLocalTranslationEngine
import com.muding.android.feature.translation.YoudaoCloudTranslationEngine

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

    fun pinSourceAssetResolver(context: Context): PinSourceAssetResolver {
        val appContext = context.applicationContext
        return PinSourceAssetResolver(
            adapters = listOf(
                ImageUriPinSourceAdapter(
                    context = appContext,
                    cachedImageRepository = cachedImageRepository(appContext)
                ),
                TextPinSourceAdapter(
                    cachedImageRepository = cachedImageRepository(appContext),
                    textBitmapRenderer = TextBitmapRenderer(appContext)
                )
            )
        )
    }

    fun pinCreationCoordinator(context: Context): PinCreationCoordinator {
        val appContext = context.applicationContext
        return PinCreationCoordinator(
            appContext = appContext,
            settingsRepository = appSettingsRepository(appContext),
            cachedImageRepository = cachedImageRepository(appContext),
            pinSourceAssetResolver = pinSourceAssetResolver(appContext)
        )
    }

    fun ocrEngine(): OcrEngine {
        return MlKitOcrEngine()
    }

    fun ocrFlowCoordinator(context: Context): OcrFlowCoordinator {
        return OcrFlowCoordinator(
            bitmapCropper = BitmapCropper(),
            ocrEngine = ocrEngine(),
            pinCreationCoordinator = pinCreationCoordinator(context)
        )
    }

    fun localTranslationModelManager(): LocalTranslationModelManager {
        return LocalTranslationModelManager()
    }

    fun localTranslationModelResetter(): LocalTranslationModelResetter {
        return DefaultLocalTranslationModelResetter(localTranslationModelManager())
    }

    fun localTranslationEngine(): MlKitLocalTranslationEngine {
        return MlKitLocalTranslationEngine(
            modelManager = localTranslationModelManager()
        )
    }

    fun cloudTranslationEngine(context: Context): CloudTranslationEngineRouter {
        val appContext = context.applicationContext
        val settingsRepository = appSettingsRepository(appContext)
        return CloudTranslationEngineRouter(
            settingsRepository = settingsRepository,
            baiduEngine = BaiduCloudTranslationEngine(settingsRepository),
            youdaoEngine = YoudaoCloudTranslationEngine(settingsRepository)
        )
    }

    fun captureFlowCoordinator(context: Context): CaptureFlowCoordinator {
        return CaptureFlowCoordinator(
            bitmapCropper = BitmapCropper(),
            pinCreationCoordinator = pinCreationCoordinator(context)
        )
    }

    fun floatingBallImageProcessor(context: Context): FloatingBallImageProcessor {
        return FloatingBallImageProcessor(context.applicationContext)
    }
}
