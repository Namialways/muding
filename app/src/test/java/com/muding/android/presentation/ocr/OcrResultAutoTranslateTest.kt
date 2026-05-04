package com.muding.android.presentation.ocr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrResultAutoTranslateTest {

    @Test
    fun `initial auto translate starts only when enabled with text and not already started`() {
        assertTrue(
            shouldStartInitialAutoTranslate(
                initialAutoTranslate = true,
                autoTranslateStarted = false,
                text = "hello"
            )
        )
        assertFalse(
            shouldStartInitialAutoTranslate(
                initialAutoTranslate = true,
                autoTranslateStarted = true,
                text = "hello"
            )
        )
        assertFalse(
            shouldStartInitialAutoTranslate(
                initialAutoTranslate = false,
                autoTranslateStarted = false,
                text = "hello"
            )
        )
        assertFalse(
            shouldStartInitialAutoTranslate(
                initialAutoTranslate = true,
                autoTranslateStarted = false,
                text = " "
            )
        )
    }
}
