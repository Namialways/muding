package com.muding.android.presentation.main

import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.domain.usecase.PinScaleMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MainUiModelsTest {

    @Test
    fun captureSettingsSummary_returnsDirectPinLabelWhenReady() {
        val summary = captureSettingsSummary(
            action = CaptureResultAction.PIN_DIRECTLY,
            permissionGranted = true
        )

        assertEquals("截图后直接贴图", summary)
    }

    @Test
    fun captureSettingsSummary_returnsPermissionStateWhenMissingPermission() {
        val summary = captureSettingsSummary(
            action = CaptureResultAction.OPEN_EDITOR,
            permissionGranted = false
        )

        assertEquals("等待悬浮窗授权", summary)
    }

    @Test
    fun pinInteractionSettingsSummary_returnsSelectedScaleModeLabel() {
        val summary = pinInteractionSettingsSummary(PinScaleMode.LOCK_ASPECT)

        assertEquals("等比缩放", summary)
    }

    @Test
    fun storageSettingsSummary_returnsEnabledRetentionState() {
        val summary = storageSettingsSummary(pinHistoryEnabled = true)

        assertEquals("自动清理开启", summary)
    }
}
