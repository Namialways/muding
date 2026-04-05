package com.muding.android.data.settings

import android.content.Context
import com.muding.android.domain.usecase.CaptureFlowSettings
import com.muding.android.domain.usecase.FloatingBallAppearanceMode
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.domain.usecase.FloatingBallTheme
import com.muding.android.domain.usecase.PinScaleMode
import com.muding.android.feature.translation.AndroidKeystoreSecretStore
import com.muding.android.feature.translation.LegacyTranslationSecretSource
import com.muding.android.feature.translation.TranslationCredentialStore

class SharedPreferencesAppSettingsRepository(context: Context) : AppSettingsRepository {

    private val settings = CaptureFlowSettings(context)
    private val credentialStore = TranslationCredentialStore(
        AndroidKeystoreSecretStore(context.applicationContext)
    )

    init {
        credentialStore.migrateFromLegacy(
            object : LegacyTranslationSecretSource {
                override fun readBaiduAppId(): String = settings.getBaiduTranslationAppId()
                override fun readBaiduSecretKey(): String = settings.getBaiduTranslationSecretKey()
                override fun readYoudaoAppKey(): String = settings.getYoudaoTranslationAppKey()
                override fun readYoudaoAppSecret(): String = settings.getYoudaoTranslationAppSecret()
                override fun clearLegacySecrets() {
                    settings.clearLegacyTranslationCredentials()
                }
            }
        )
    }

    override fun getCaptureResultAction(): CaptureResultAction = settings.getResultAction()

    override fun setCaptureResultAction(action: CaptureResultAction) {
        settings.setResultAction(action)
    }

    override fun getFavoriteEditorColors(): List<Int> = settings.getFavoriteEditorColors()

    override fun setFavoriteEditorColors(colors: List<Int>) {
        settings.setFavoriteEditorColors(colors)
    }

    override fun getRecentEditorColors(): List<Int> = settings.getRecentEditorColors()

    override fun setRecentEditorColors(colors: List<Int>) {
        settings.setRecentEditorColors(colors)
    }

    override fun getPinScaleMode(): PinScaleMode = settings.getPinScaleMode()

    override fun setPinScaleMode(mode: PinScaleMode) {
        settings.setPinScaleMode(mode)
    }

    override fun getProjectRecordSettings(): ProjectRecordSettings {
        return ProjectRecordSettings(
            maxSessionCount = settings.getMaxSessionCount(),
            retainDays = settings.getRetainDays()
        )
    }

    override fun setMaxSessionCount(count: Int) {
        settings.setMaxSessionCount(count)
    }

    override fun setRetainDays(days: Int) {
        settings.setRetainDays(days)
    }

    override fun getPinAppearanceSettings(): PinAppearanceSettings {
        return PinAppearanceSettings(
            shadowEnabled = settings.isPinShadowEnabledByDefault(),
            cornerRadiusDp = settings.getDefaultPinCornerRadiusDp()
        )
    }

    override fun isPinShadowEnabledByDefault(): Boolean {
        return settings.isPinShadowEnabledByDefault()
    }

    override fun setPinShadowEnabledByDefault(enabled: Boolean) {
        settings.setPinShadowEnabledByDefault(enabled)
    }

    override fun getDefaultPinCornerRadiusDp(): Float {
        return settings.getDefaultPinCornerRadiusDp()
    }

    override fun setDefaultPinCornerRadiusDp(radiusDp: Float) {
        settings.setDefaultPinCornerRadiusDp(radiusDp)
    }

    override fun getFloatingBallSettings(): FloatingBallSettings {
        return FloatingBallSettings(
            sizeDp = settings.getFloatingBallSizeDp(),
            opacity = settings.getFloatingBallOpacity(),
            theme = settings.getFloatingBallTheme(),
            appearanceMode = settings.getFloatingBallAppearanceMode(),
            customImageUri = settings.getFloatingBallCustomImageUri()
        )
    }

    override fun setFloatingBallSizeDp(sizeDp: Int) {
        settings.setFloatingBallSizeDp(sizeDp)
    }

    override fun setFloatingBallOpacity(opacity: Float) {
        settings.setFloatingBallOpacity(opacity)
    }

    override fun setFloatingBallTheme(theme: FloatingBallTheme) {
        settings.setFloatingBallTheme(theme)
    }

    override fun setFloatingBallAppearanceMode(mode: FloatingBallAppearanceMode) {
        settings.setFloatingBallAppearanceMode(mode)
    }

    override fun setFloatingBallCustomImageUri(uri: String?) {
        settings.setFloatingBallCustomImageUri(uri)
    }

    override fun getPinHistorySettings(): PinHistorySettings {
        return PinHistorySettings(
            enabled = settings.isPinHistoryEnabled(),
            maxCount = settings.getMaxPinHistoryCount(),
            retainDays = settings.getPinHistoryRetainDays()
        )
    }

    override fun setPinHistoryEnabled(enabled: Boolean) {
        settings.setPinHistoryEnabled(enabled)
    }

    override fun setMaxPinHistoryCount(count: Int) {
        settings.setMaxPinHistoryCount(count)
    }

    override fun setPinHistoryRetainDays(days: Int) {
        settings.setPinHistoryRetainDays(days)
    }

    override fun getTranslationSettings(): TranslationSettings {
        val credentials = credentialStore.getCredentials()
        return TranslationSettings(
            localTargetLanguageTag = settings.getLocalTranslationTargetLanguageTag(),
            localDownloadOnWifiOnly = settings.isLocalTranslationDownloadOnWifiOnly(),
            cloudProvider = settings.getCloudTranslationProvider(),
            baiduAppId = credentials.baiduAppId,
            baiduSecretKey = credentials.baiduSecretKey,
            youdaoAppKey = credentials.youdaoAppKey,
            youdaoAppSecret = credentials.youdaoAppSecret
        )
    }

    override fun setLocalTranslationTargetLanguageTag(languageTag: String) {
        settings.setLocalTranslationTargetLanguageTag(languageTag)
    }

    override fun setLocalTranslationDownloadOnWifiOnly(enabled: Boolean) {
        settings.setLocalTranslationDownloadOnWifiOnly(enabled)
    }

    override fun setCloudTranslationProvider(provider: CloudTranslationProvider) {
        settings.setCloudTranslationProvider(provider)
    }

    override fun setBaiduTranslationCredentials(appId: String, secretKey: String) {
        credentialStore.saveBaiduCredentials(appId, secretKey)
        settings.clearLegacyTranslationCredentials()
    }

    override fun setYoudaoTranslationCredentials(appKey: String, appSecret: String) {
        credentialStore.saveYoudaoCredentials(appKey, appSecret)
        settings.clearLegacyTranslationCredentials()
    }

    override fun resetAllSettings() {
        credentialStore.clearAllCredentials()
        settings.clearAll()
    }
}
