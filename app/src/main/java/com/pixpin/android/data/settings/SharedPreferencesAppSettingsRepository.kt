package com.pixpin.android.data.settings

import android.content.Context
import com.pixpin.android.domain.usecase.CaptureFlowSettings
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.domain.usecase.FloatingBallTheme
import com.pixpin.android.domain.usecase.PinScaleMode

class SharedPreferencesAppSettingsRepository(context: Context) : AppSettingsRepository {

    private val settings = CaptureFlowSettings(context)

    override fun getCaptureResultAction(): CaptureResultAction = settings.getResultAction()

    override fun setCaptureResultAction(action: CaptureResultAction) {
        settings.setResultAction(action)
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
            theme = settings.getFloatingBallTheme()
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
}
