package com.muding.android.domain.usecase

internal class ScreenshotCaptureRequestController(
    private val scheduler: Scheduler
) {

    fun interface Cancellable {
        fun cancel()
    }

    fun interface Scheduler {
        fun schedule(delayMs: Long, action: () -> Unit): Cancellable
    }

    enum class FrameDecision {
        IGNORE,
        DROP,
        CONSUME
    }

    private val lock = Any()
    private var pendingRequest: PendingRequest? = null

    fun startCapture(
        startDelayMs: Long,
        timeoutMs: Long,
        dropFirstFrame: Boolean,
        onReady: () -> Unit,
        onTimeout: () -> Unit
    ): Boolean {
        var invokeReady = false
        synchronized(lock) {
            if (pendingRequest != null) {
                return false
            }
            val request = PendingRequest(
                armed = startDelayMs <= 0L,
                dropFirstFrame = dropFirstFrame
            )
            request.timeoutToken = scheduler.schedule(timeoutMs) {
                val shouldTimeout = synchronized(lock) {
                    if (pendingRequest !== request) {
                        false
                    } else {
                        pendingRequest = null
                        request.armToken?.cancel()
                        request.armToken = null
                        true
                    }
                }
                if (shouldTimeout) {
                    onTimeout()
                }
            }
            if (startDelayMs > 0L) {
                request.armToken = scheduler.schedule(startDelayMs) {
                    val becameReady = synchronized(lock) {
                        if (pendingRequest !== request || request.armed) {
                            false
                        } else {
                            request.armed = true
                            request.armToken = null
                            true
                        }
                    }
                    if (becameReady) {
                        onReady()
                    }
                }
            } else {
                invokeReady = true
            }
            pendingRequest = request
        }
        if (invokeReady) {
            onReady()
        }
        return true
    }

    fun onFrameAvailable(): FrameDecision {
        synchronized(lock) {
            val request = pendingRequest ?: return FrameDecision.IGNORE
            if (!request.armed) {
                return FrameDecision.IGNORE
            }
            if (request.dropFirstFrame) {
                request.dropFirstFrame = false
                return FrameDecision.DROP
            }
            return FrameDecision.CONSUME
        }
    }

    fun completeCapture() {
        clear()
    }

    fun failCapture() {
        clear()
    }

    fun clear() {
        synchronized(lock) {
            pendingRequest?.timeoutToken?.cancel()
            pendingRequest?.armToken?.cancel()
            pendingRequest = null
        }
    }

    fun hasPendingCapture(): Boolean {
        synchronized(lock) {
            return pendingRequest != null
        }
    }

    private data class PendingRequest(
        var armed: Boolean,
        var dropFirstFrame: Boolean,
        var timeoutToken: Cancellable? = null,
        var armToken: Cancellable? = null
    )
}
