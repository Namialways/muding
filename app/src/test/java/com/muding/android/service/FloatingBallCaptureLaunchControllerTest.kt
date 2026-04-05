package com.muding.android.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingBallCaptureLaunchControllerTest {

    @Test
    fun `active projection capture starts immediately and blocks duplicates until shown`() {
        val controller = FloatingBallCaptureLaunchController()

        val firstDecision = controller.requestCapture(hasActiveProjection = true)
        val secondDecision = controller.requestCapture(hasActiveProjection = true)

        assertEquals(
            FloatingBallCaptureLaunchController.Decision.StartCapture(
                startDelayMs = 0L,
                dropFirstFrame = false
            ),
            firstDecision
        )
        assertEquals(FloatingBallCaptureLaunchController.Decision.Ignore, secondDecision)
        assertTrue(controller.hasPendingLaunch())

        controller.onCaptureUiShown()

        assertFalse(controller.hasPendingLaunch())
    }

    @Test
    fun `permission flow keeps launch pending until permission result then starts without extra delay`() {
        val controller = FloatingBallCaptureLaunchController()

        val permissionDecision = controller.requestCapture(hasActiveProjection = false)
        val grantedDecision = controller.onPermissionGranted()

        assertEquals(FloatingBallCaptureLaunchController.Decision.RequestPermission, permissionDecision)
        assertEquals(
            FloatingBallCaptureLaunchController.Decision.StartCapture(
                startDelayMs = 0L,
                dropFirstFrame = true
            ),
            grantedDecision
        )
        assertTrue(controller.hasPendingLaunch())
    }

    @Test
    fun `permission denial clears pending launch and allows retry`() {
        val controller = FloatingBallCaptureLaunchController()

        controller.requestCapture(hasActiveProjection = false)
        controller.onPermissionDenied()
        val retryDecision = controller.requestCapture(hasActiveProjection = true)

        assertTrue(controller.hasPendingLaunch())
        assertEquals(
            FloatingBallCaptureLaunchController.Decision.StartCapture(
                startDelayMs = 0L,
                dropFirstFrame = false
            ),
            retryDecision
        )
    }

    @Test
    fun `capture failure clears pending launch and allows retry`() {
        val controller = FloatingBallCaptureLaunchController()

        controller.requestCapture(hasActiveProjection = true)
        controller.onCaptureFailed()
        val retryDecision = controller.requestCapture(hasActiveProjection = true)

        assertEquals(
            FloatingBallCaptureLaunchController.Decision.StartCapture(
                startDelayMs = 0L,
                dropFirstFrame = false
            ),
            retryDecision
        )
    }
}
