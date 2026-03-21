package com.muding.android.data.settings

import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.domain.usecase.FloatingBallTheme
import com.muding.android.domain.usecase.PinScaleMode

data class FloatingBallSettings(
    val sizeDp: Int,
    val opacity: Float,
    val theme: FloatingBallTheme
)

data class ProjectRecordSettings(
    val maxSessionCount: Int,
    val retainDays: Int
)

data class PinHistorySettings(
    val enabled: Boolean,
    val maxCount: Int,
    val retainDays: Int
)

data class PinAppearanceSettings(
    val shadowEnabled: Boolean,
    val cornerRadiusDp: Float
)

enum class CloudTranslationProvider {
    NONE,
    BAIDU,
    YOUDAO
}

data class TranslationSettings(
    val localTargetLanguageTag: String,
    val localDownloadOnWifiOnly: Boolean,
    val cloudProvider: CloudTranslationProvider,
    val baiduAppId: String,
    val baiduSecretKey: String,
    val youdaoAppKey: String,
    val youdaoAppSecret: String
)

interface AppSettingsRepository {
    fun getCaptureResultAction(): CaptureResultAction
    fun setCaptureResultAction(action: CaptureResultAction)

    fun getPinScaleMode(): PinScaleMode
    fun setPinScaleMode(mode: PinScaleMode)

    fun getProjectRecordSettings(): ProjectRecordSettings
    fun setMaxSessionCount(count: Int)
    fun setRetainDays(days: Int)

    fun getPinAppearanceSettings(): PinAppearanceSettings
    fun isPinShadowEnabledByDefault(): Boolean
    fun setPinShadowEnabledByDefault(enabled: Boolean)
    fun getDefaultPinCornerRadiusDp(): Float
    fun setDefaultPinCornerRadiusDp(radiusDp: Float)

    fun getFloatingBallSettings(): FloatingBallSettings
    fun setFloatingBallSizeDp(sizeDp: Int)
    fun setFloatingBallOpacity(opacity: Float)
    fun setFloatingBallTheme(theme: FloatingBallTheme)

    fun getPinHistorySettings(): PinHistorySettings
    fun setPinHistoryEnabled(enabled: Boolean)
    fun setMaxPinHistoryCount(count: Int)
    fun setPinHistoryRetainDays(days: Int)

    fun getTranslationSettings(): TranslationSettings
    fun setLocalTranslationTargetLanguageTag(languageTag: String)
    fun setLocalTranslationDownloadOnWifiOnly(enabled: Boolean)
    fun setCloudTranslationProvider(provider: CloudTranslationProvider)
    fun setBaiduTranslationCredentials(appId: String, secretKey: String)
    fun setYoudaoTranslationCredentials(appKey: String, appSecret: String)
}
