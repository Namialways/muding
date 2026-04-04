package com.muding.android.service

internal class ProjectionSessionTimeoutController(
    private val timeoutMs: Long,
    private val scheduler: Scheduler,
    private val onTimeout: () -> Unit
) {

    fun interface Cancellable {
        fun cancel()
    }

    fun interface Scheduler {
        fun schedule(delayMs: Long, action: () -> Unit): Cancellable
    }

    private var sessionActive = false
    private var pendingTimeout: Cancellable? = null

    fun startSession() {
        sessionActive = true
        scheduleTimeout()
    }

    fun recordActivity() {
        if (!sessionActive) {
            return
        }
        scheduleTimeout()
    }

    fun clearSession() {
        sessionActive = false
        pendingTimeout?.cancel()
        pendingTimeout = null
    }

    private fun scheduleTimeout() {
        pendingTimeout?.cancel()
        pendingTimeout = scheduler.schedule(timeoutMs) {
            if (!sessionActive) {
                return@schedule
            }
            pendingTimeout = null
            sessionActive = false
            onTimeout()
        }
    }
}
