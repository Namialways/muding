package com.muding.android.presentation.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrResultFloatingBallRestorePolicyTest {

    @Test
    fun onStop_restoresFloatingBallWhenActivityLeavesForeground() {
        var restoreCount = 0
        val policy = OcrResultFloatingBallRestorePolicy(
            shouldRestoreFloatingBall = true,
            restoreFloatingBall = { restoreCount++ }
        )

        policy.onStop(isChangingConfigurations = false)

        assertEquals(1, restoreCount)
    }

    @Test
    fun onStop_doesNotRestoreForConfigurationChanges() {
        var restoreCount = 0
        val policy = OcrResultFloatingBallRestorePolicy(
            shouldRestoreFloatingBall = true,
            restoreFloatingBall = { restoreCount++ }
        )

        policy.onStop(isChangingConfigurations = true)

        assertEquals(0, restoreCount)
    }

    @Test
    fun restoreNow_onlyRestoresOnce() {
        var restoreCount = 0
        val policy = OcrResultFloatingBallRestorePolicy(
            shouldRestoreFloatingBall = true,
            restoreFloatingBall = { restoreCount++ }
        )

        policy.restoreNow()
        policy.onStop(isChangingConfigurations = false)
        policy.restoreNow()

        assertEquals(1, restoreCount)
    }

    @Test
    fun restoreNow_doesNothingWhenRestoreFlagIsFalse() {
        var restoreCount = 0
        val policy = OcrResultFloatingBallRestorePolicy(
            shouldRestoreFloatingBall = false,
            restoreFloatingBall = { restoreCount++ }
        )

        policy.restoreNow()
        policy.onStop(isChangingConfigurations = false)

        assertEquals(0, restoreCount)
    }
}
