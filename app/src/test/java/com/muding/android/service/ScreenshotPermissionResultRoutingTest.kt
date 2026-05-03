package com.muding.android.service

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotPermissionResultRoutingTest {

    @Test
    fun `granted result requests capture after permission`() {
        val route = ScreenshotPermissionResultRouting.from(
            resultCode = Activity.RESULT_OK,
            hasResultData = true
        )

        assertEquals(Activity.RESULT_OK, route.resultCode)
        assertTrue(route.includeResultData)
        assertTrue(route.captureAfterPermission)
    }

    @Test
    fun `cancelled result notifies service without capture request`() {
        val route = ScreenshotPermissionResultRouting.from(
            resultCode = Activity.RESULT_CANCELED,
            hasResultData = false
        )

        assertEquals(Activity.RESULT_CANCELED, route.resultCode)
        assertFalse(route.includeResultData)
        assertFalse(route.captureAfterPermission)
    }
}
