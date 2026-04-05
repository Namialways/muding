package com.muding.android.presentation.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPickerDraftStateTest {

    @Test
    fun `draft initializes from committed color`() {
        val draft = ColorPickerDraftState.fromColor(Color(0xFF7DDD28))

        assertEquals("#7DDD28", draft.hexText)
        assertEquals("125", draft.redText)
        assertEquals("221", draft.greenText)
        assertEquals("40", draft.blueText)
        assertEquals(0xFF7DDD28.toInt(), draft.previewColor.toArgb())
    }

    @Test
    fun `valid hex input updates preview color`() {
        val draft = ColorPickerDraftState.fromColor(Color(0xFFFF453A))
        val updated = draft.updateHex("#112233")

        assertEquals("#112233", updated.hexText)
        assertEquals("17", updated.redText)
        assertEquals("34", updated.greenText)
        assertEquals("51", updated.blueText)
        assertEquals(0xFF112233.toInt(), updated.previewColor.toArgb())
    }

    @Test
    fun `hex input without hash also updates preview color`() {
        val draft = ColorPickerDraftState.fromColor(Color(0xFFFF453A))
        val updated = draft.updateHex("ABCDEF")

        assertEquals("#ABCDEF", updated.hexText)
        assertEquals(0xFFABCDEF.toInt(), updated.previewColor.toArgb())
    }

    @Test
    fun `invalid hex input preserves last valid preview color`() {
        val draft = ColorPickerDraftState.fromColor(Color(0xFF7DDD28))
        val updated = draft.updateHex("#12")

        assertEquals("#12", updated.hexText)
        assertEquals(0xFF7DDD28.toInt(), updated.previewColor.toArgb())
        assertEquals("125", updated.redText)
        assertEquals("221", updated.greenText)
        assertEquals("40", updated.blueText)
    }

    @Test
    fun `partial numeric edits preserve field text until valid value can be applied`() {
        val draft = ColorPickerDraftState.fromColor(Color(0xFF7DDD28))
        val invalid = draft.updateRed("")

        assertEquals("", invalid.redText)
        assertEquals(0xFF7DDD28.toInt(), invalid.previewColor.toArgb())

        val valid = invalid.updateRed("12")

        assertEquals("12", valid.redText)
        assertEquals(0xFF0CDD28.toInt(), valid.previewColor.toArgb())
    }

    @Test
    fun `rgb values clamp to 0 255`() {
        val draft = ColorPickerDraftState.fromColor(Color(0xFF7DDD28))
        val updated = draft.updateBlue("999").updateGreen("-2")

        assertEquals("255", updated.blueText)
        assertEquals("0", updated.greenText)
        assertEquals(0xFF7D00FF.toInt(), updated.previewColor.toArgb())
    }
}
