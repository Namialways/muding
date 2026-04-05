package com.muding.android.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotCaptureRequestControllerTest {

    @Test
    fun `startCapture schedules timeout and arms immediately when delay is zero`() {
        val scheduler = FakeScheduler()
        var readyCount = 0
        var timeoutCount = 0
        val controller = ScreenshotCaptureRequestController(scheduler)

        val started = controller.startCapture(
            startDelayMs = 0L,
            timeoutMs = 2_500L,
            dropFirstFrame = false,
            onReady = { readyCount += 1 },
            onTimeout = { timeoutCount += 1 }
        )

        assertTrue(started)
        assertTrue(controller.hasPendingCapture())
        assertEquals(1, readyCount)
        assertEquals(0, timeoutCount)
        assertEquals(listOf(2_500L), scheduler.activeDelays())
    }

    @Test
    fun `startCapture rejects a second request while one is pending`() {
        val scheduler = FakeScheduler()
        val controller = ScreenshotCaptureRequestController(scheduler)

        val firstStarted = controller.startCapture(
            startDelayMs = 0L,
            timeoutMs = 2_500L,
            dropFirstFrame = false,
            onReady = {},
            onTimeout = {}
        )
        val secondStarted = controller.startCapture(
            startDelayMs = 0L,
            timeoutMs = 2_500L,
            dropFirstFrame = false,
            onReady = {},
            onTimeout = {}
        )

        assertTrue(firstStarted)
        assertFalse(secondStarted)
        assertEquals(listOf(2_500L), scheduler.activeDelays())
    }

    @Test
    fun `dropFirstFrame waits until request is armed and consumes the second frame`() {
        val scheduler = FakeScheduler()
        var readyCount = 0
        val controller = ScreenshotCaptureRequestController(scheduler)

        controller.startCapture(
            startDelayMs = 180L,
            timeoutMs = 2_500L,
            dropFirstFrame = true,
            onReady = { readyCount += 1 },
            onTimeout = {}
        )

        assertEquals(ScreenshotCaptureRequestController.FrameDecision.IGNORE, controller.onFrameAvailable())

        scheduler.runTasksWithDelay(180L)

        assertEquals(1, readyCount)
        assertEquals(ScreenshotCaptureRequestController.FrameDecision.DROP, controller.onFrameAvailable())
        assertEquals(ScreenshotCaptureRequestController.FrameDecision.CONSUME, controller.onFrameAvailable())
    }

    @Test
    fun `timeout clears the pending request and prevents late ready callback`() {
        val scheduler = FakeScheduler()
        var readyCount = 0
        var timeoutCount = 0
        val controller = ScreenshotCaptureRequestController(scheduler)

        controller.startCapture(
            startDelayMs = 180L,
            timeoutMs = 120L,
            dropFirstFrame = false,
            onReady = { readyCount += 1 },
            onTimeout = { timeoutCount += 1 }
        )

        scheduler.runTasksWithDelay(120L)
        scheduler.runTasksWithDelay(180L)

        assertFalse(controller.hasPendingCapture())
        assertEquals(0, readyCount)
        assertEquals(1, timeoutCount)
        assertEquals(ScreenshotCaptureRequestController.FrameDecision.IGNORE, controller.onFrameAvailable())
    }

    @Test
    fun `clear cancels scheduled callbacks and ignores later frames`() {
        val scheduler = FakeScheduler()
        var readyCount = 0
        var timeoutCount = 0
        val controller = ScreenshotCaptureRequestController(scheduler)

        controller.startCapture(
            startDelayMs = 180L,
            timeoutMs = 2_500L,
            dropFirstFrame = false,
            onReady = { readyCount += 1 },
            onTimeout = { timeoutCount += 1 }
        )

        controller.clear()
        scheduler.runAllActiveTasks()

        assertFalse(controller.hasPendingCapture())
        assertEquals(0, readyCount)
        assertEquals(0, timeoutCount)
        assertEquals(ScreenshotCaptureRequestController.FrameDecision.IGNORE, controller.onFrameAvailable())
    }

    private class FakeScheduler : ScreenshotCaptureRequestController.Scheduler {

        private val scheduledTasks = mutableListOf<FakeTask>()

        override fun schedule(delayMs: Long, action: () -> Unit): ScreenshotCaptureRequestController.Cancellable {
            val task = FakeTask(delayMs = delayMs, action = action)
            scheduledTasks += task
            return ScreenshotCaptureRequestController.Cancellable {
                task.cancelled = true
            }
        }

        fun activeDelays(): List<Long> {
            return scheduledTasks.filterNot { it.cancelled }.map { it.delayMs }
        }

        fun runTasksWithDelay(delayMs: Long) {
            scheduledTasks
                .filter { !it.cancelled && it.delayMs == delayMs }
                .toList()
                .forEach { task ->
                    task.cancelled = true
                    task.action()
                }
        }

        fun runAllActiveTasks() {
            scheduledTasks
                .filterNot { it.cancelled }
                .toList()
                .forEach { task ->
                    task.cancelled = true
                    task.action()
                }
        }
    }

    private data class FakeTask(
        val delayMs: Long,
        val action: () -> Unit,
        var cancelled: Boolean = false
    )
}
