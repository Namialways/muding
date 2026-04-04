package com.muding.android.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionSessionTimeoutControllerTest {

    @Test
    fun `startSession schedules timeout using configured delay`() {
        val scheduler = FakeScheduler()
        var timeoutCount = 0
        val controller = ProjectionSessionTimeoutController(
            timeoutMs = 120_000L,
            scheduler = scheduler,
            onTimeout = { timeoutCount += 1 }
        )

        controller.startSession()

        assertEquals(listOf(120_000L), scheduler.activeDelays())
        assertEquals(0, timeoutCount)
    }

    @Test
    fun `recordActivity replaces previous timeout with a fresh one`() {
        val scheduler = FakeScheduler()
        val controller = ProjectionSessionTimeoutController(
            timeoutMs = 120_000L,
            scheduler = scheduler,
            onTimeout = {}
        )

        controller.startSession()
        val firstTask = scheduler.lastTask()

        controller.recordActivity()

        assertTrue(firstTask.cancelled)
        assertEquals(listOf(120_000L), scheduler.activeDelays())
        assertEquals(2, scheduler.allTasks.size)
    }

    @Test
    fun `clearSession cancels pending timeout and prevents callback`() {
        val scheduler = FakeScheduler()
        var timeoutCount = 0
        val controller = ProjectionSessionTimeoutController(
            timeoutMs = 120_000L,
            scheduler = scheduler,
            onTimeout = { timeoutCount += 1 }
        )

        controller.startSession()
        val scheduledTask = scheduler.lastTask()

        controller.clearSession()
        scheduler.runActiveTasks()

        assertTrue(scheduledTask.cancelled)
        assertFalse(scheduler.hasActiveTasks())
        assertEquals(0, timeoutCount)
    }

    private class FakeScheduler : ProjectionSessionTimeoutController.Scheduler {

        val allTasks = mutableListOf<FakeTask>()

        override fun schedule(delayMs: Long, action: () -> Unit): ProjectionSessionTimeoutController.Cancellable {
            val task = FakeTask(delayMs = delayMs, action = action)
            allTasks += task
            return ProjectionSessionTimeoutController.Cancellable {
                task.cancelled = true
            }
        }

        fun lastTask(): FakeTask = allTasks.last()

        fun activeDelays(): List<Long> {
            return allTasks.filterNot { it.cancelled }.map { it.delayMs }
        }

        fun hasActiveTasks(): Boolean = allTasks.any { !it.cancelled }

        fun runActiveTasks() {
            allTasks.filterNot { it.cancelled }.toList().forEach { task ->
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
