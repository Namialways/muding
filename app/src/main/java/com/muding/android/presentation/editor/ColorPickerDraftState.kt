package com.muding.android.presentation.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

data class ColorPickerDraftState(
    val previewColor: Color,
    val hexText: String,
    val redText: String,
    val greenText: String,
    val blueText: String
) {

    fun updateHex(input: String): ColorPickerDraftState {
        val parsedArgb = parseHexArgb(input) ?: return copy(hexText = input)
        return fromArgb(parsedArgb)
    }

    fun updateColor(color: Color): ColorPickerDraftState = fromColor(color)

    fun updateRed(input: String): ColorPickerDraftState = updateRgb(redText = input)

    fun updateGreen(input: String): ColorPickerDraftState = updateRgb(greenText = input)

    fun updateBlue(input: String): ColorPickerDraftState = updateRgb(blueText = input)

    private fun updateRgb(
        redText: String = this.redText,
        greenText: String = this.greenText,
        blueText: String = this.blueText
    ): ColorPickerDraftState {
        val red = parseRgbComponent(redText)
        val green = parseRgbComponent(greenText)
        val blue = parseRgbComponent(blueText)

        if (red == null || green == null || blue == null) {
            return copy(
                redText = redText,
                greenText = greenText,
                blueText = blueText
            )
        }

        return fromArgb(argbOf(red, green, blue))
    }

    companion object {
        fun fromColor(color: Color): ColorPickerDraftState = fromArgb(color.toArgb())

        private fun fromArgb(argb: Int): ColorPickerDraftState {
            return ColorPickerDraftState(
                previewColor = Color(argb),
                hexText = hexStringOf(argb),
                redText = ((argb shr 16) and 0xFF).toString(),
                greenText = ((argb shr 8) and 0xFF).toString(),
                blueText = (argb and 0xFF).toString()
            )
        }

        private fun parseHexArgb(input: String): Int? {
            val sanitized = input.removePrefix("#")
            if (sanitized.length != 6) return null
            val rgb = sanitized.toIntOrNull(radix = 16) ?: return null
            return 0xFF000000.toInt() or rgb
        }

        private fun parseRgbComponent(input: String): Int? {
            return input.toIntOrNull()?.coerceIn(0, 255)
        }

        private fun argbOf(red: Int, green: Int, blue: Int): Int {
            return 0xFF000000.toInt() or (red shl 16) or (green shl 8) or blue
        }

        private fun hexStringOf(argb: Int): String {
            return String.format(Locale.US, "#%06X", argb and 0x00FFFFFF)
        }
    }
}
