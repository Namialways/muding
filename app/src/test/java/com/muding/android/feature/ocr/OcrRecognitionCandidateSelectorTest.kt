package com.muding.android.feature.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrRecognitionCandidateSelectorTest {

    @Test
    fun select_prefersCleanLatinUiTextOverNoisySymbolText() {
        val selected = OcrRecognitionCandidateSelector.select(
            listOf(
                candidate(
                    text = "|||| |||| [] [] 1111",
                    script = OcrRecognizerScript.CHINESE,
                    variant = OcrImageVariant.ORIGINAL
                ),
                candidate(
                    text = "Camera\nAll apps\nChrome",
                    script = OcrRecognizerScript.LATIN,
                    variant = OcrImageVariant.ORIGINAL
                )
            )
        )

        assertEquals("Camera\nAll apps\nChrome", selected.fullText)
    }

    @Test
    fun select_keepsCjkTextWhenCjkCharactersDominate() {
        val selected = OcrRecognitionCandidateSelector.select(
            listOf(
                candidate(
                    text = "\u76f8\u673a\n\u8bbe\u7f6e",
                    script = OcrRecognizerScript.CHINESE,
                    variant = OcrImageVariant.ORIGINAL
                ),
                candidate(
                    text = "lll O0",
                    script = OcrRecognizerScript.LATIN,
                    variant = OcrImageVariant.ORIGINAL
                )
            )
        )

        assertEquals("\u76f8\u673a\n\u8bbe\u7f6e", selected.fullText)
    }

    @Test
    fun select_prefersEnhancedVariantWhenQualityIsHigher() {
        val selected = OcrRecognitionCandidateSelector.select(
            listOf(
                candidate(
                    text = "C1ock",
                    script = OcrRecognizerScript.LATIN,
                    variant = OcrImageVariant.ORIGINAL
                ),
                candidate(
                    text = "Clock",
                    script = OcrRecognizerScript.LATIN,
                    variant = OcrImageVariant.ENHANCED
                )
            )
        )

        assertEquals("Clock", selected.fullText)
    }

    private fun candidate(
        text: String,
        script: OcrRecognizerScript,
        variant: OcrImageVariant
    ): OcrRecognitionCandidate {
        return OcrRecognitionCandidate(
            result = OcrResult(fullText = text, blocks = emptyList()),
            script = script,
            variant = variant
        )
    }
}
