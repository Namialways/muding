package com.pixpin.android.data.settings

import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.domain.usecase.FloatingBallTheme
import com.pixpin.android.domain.usecase.PinScaleMode

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

interface AppSettingsRepository {
    fun getCaptureResultAction(): CaptureResultAction
    fun setCaptureResultAction(action: CaptureResultAction)

    fun getPinScaleMode(): PinScaleMode
    fun setPinScaleMode(mode: PinScaleMode)

    fun getProjectRecordSettings(): ProjectRecordSettings
    fun setMaxSessionCount(count: Int)
    fun setRetainDays(days: Int)

    fun isPinShadowEnabledByDefault(): Boolean
    fun setPinShadowEnabledByDefault(enabled: Boolean)

    fun getFloatingBallSettings(): FloatingBallSettings
    fun setFloatingBallSizeDp(sizeDp: Int)
    fun setFloatingBallOpacity(opacity: Float)
    fun setFloatingBallTheme(theme: FloatingBallTheme)

    fun getPinHistorySettings(): PinHistorySettings
    fun setPinHistoryEnabled(enabled: Boolean)
    fun setMaxPinHistoryCount(count: Int)
    fun setPinHistoryRetainDays(days: Int)
}
