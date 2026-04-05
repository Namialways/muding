package com.muding.android.service

internal class FloatingBallCaptureLaunchController {

    sealed interface Decision {
        data object Ignore : Decision
        data object RequestPermission : Decision
        data class StartCapture(
            val startDelayMs: Long,
            val dropFirstFrame: Boolean
        ) : Decision
    }

    private var pendingLaunch = false

    fun requestCapture(hasActiveProjection: Boolean): Decision {
        if (pendingLaunch) {
            return Decision.Ignore
        }
        pendingLaunch = true
        return if (hasActiveProjection) {
            Decision.StartCapture(
                startDelayMs = ACTIVE_PROJECTION_START_DELAY_MS,
                dropFirstFrame = false
            )
        } else {
            Decision.RequestPermission
        }
    }

    fun onPermissionGranted(): Decision {
        if (!pendingLaunch) {
            return Decision.Ignore
        }
        return Decision.StartCapture(
            startDelayMs = FIRST_CAPTURE_AFTER_PERMISSION_START_DELAY_MS,
            dropFirstFrame = true
        )
    }

    fun onPermissionDenied() {
        pendingLaunch = false
    }

    fun onCaptureUiShown() {
        pendingLaunch = false
    }

    fun onCaptureFailed() {
        pendingLaunch = false
    }

    fun hasPendingLaunch(): Boolean = pendingLaunch

    companion object {
        private const val ACTIVE_PROJECTION_START_DELAY_MS = 0L
        private const val FIRST_CAPTURE_AFTER_PERMISSION_START_DELAY_MS = 0L
    }
}
