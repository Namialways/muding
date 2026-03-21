package com.muding.android.feature.ocr

import android.graphics.Rect

data class OcrTextElement(
    val text: String,
    val boundingBox: Rect?
)

data class OcrTextLine(
    val text: String,
    val boundingBox: Rect?,
    val elements: List<OcrTextElement>
)

data class OcrTextBlock(
    val text: String,
    val boundingBox: Rect?,
    val lines: List<OcrTextLine>
)

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrTextBlock>
) {
    val normalizedText: String
        get() = fullText.trim()

    fun hasText(): Boolean = normalizedText.isNotBlank()

    fun score(): Int = normalizedText.count { !it.isWhitespace() }

    companion object {
        val EMPTY = OcrResult(
            fullText = "",
            blocks = emptyList()
        )
    }
}
