package com.muding.android.service

import org.junit.Assert.assertFalse
import org.junit.Test

class FloatingMenuOverlayPolicyTest {

    @Test
    fun `floating menu does not create a full screen touch blocker`() {
        assertFalse(FloatingMenuOverlayPolicy.shouldCreateFullScreenDismissLayer())
    }
}
