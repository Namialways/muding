package com.muding.android.feature.ocr

internal enum class OcrRecognizerScript {
    CHINESE,
    LATIN
}

internal enum class OcrImageVariant {
    ORIGINAL,
    UPSCALED,
    ENHANCED
}

internal data class OcrRecognitionCandidate(
    val result: OcrResult,
    val script: OcrRecognizerScript,
    val variant: OcrImageVariant
)

internal object OcrRecognitionCandidateSelector {

    fun select(candidates: List<OcrRecognitionCandidate>): OcrResult {
        return candidates
            .filter { it.result.hasText() }
            .maxByOrNull { qualityScore(it) }
            ?.result
            ?: OcrResult.EMPTY
    }

    private fun qualityScore(candidate: OcrRecognitionCandidate): Int {
        val text = candidate.result.normalizedText
        val usefulChars = text.count { !it.isWhitespace() }
        val wordLikeChars = text.count { it.isLetterOrDigit() || it.isCjkLike() }
        val cjkChars = text.count { it.isCjkLike() }
        val latinChars = text.count { it in 'A'..'Z' || it in 'a'..'z' }
        val symbolChars = text.count {
            !it.isWhitespace() && !it.isLetterOrDigit() && !it.isCjkLike()
        }
        val suspiciousChars = text.count { it in SUSPICIOUS_CHARS }
        val lineCount = candidate.result.blocks
            .flatMap { it.lines }
            .size
            .takeIf { it > 0 }
            ?: text.lineSequence().count { it.isNotBlank() }
        val wordCount = WORD_REGEX.findAll(text).count()
        val variantBonus = when (candidate.variant) {
            OcrImageVariant.ORIGINAL -> 0
            OcrImageVariant.UPSCALED -> 2
            OcrImageVariant.ENHANCED -> 4
        }
        val scriptBonus = when (candidate.script) {
            OcrRecognizerScript.CHINESE -> if (cjkChars > 0) 18 else 0
            OcrRecognizerScript.LATIN -> if (latinChars >= cjkChars) 12 else 0
        }

        return usefulChars * 3 +
            wordLikeChars * 8 +
            lineCount * 5 +
            wordCount * 7 +
            scriptBonus +
            variantBonus -
            symbolChars * 12 -
            suspiciousChars * 18
    }

    private fun Char.isCjkLike(): Boolean {
        return this in '\u3400'..'\u4dbf' ||
            this in '\u4e00'..'\u9fff' ||
            this in '\u3040'..'\u30ff' ||
            this in '\uac00'..'\ud7af'
    }

    private val WORD_REGEX = Regex("[A-Za-z0-9]{2,}|[\\u3400-\\u4dbf\\u4e00-\\u9fff\\u3040-\\u30ff\\uac00-\\ud7af]+")
    private val SUSPICIOUS_CHARS = setOf('|', '[', ']', '{', '}', '<', '>', '~', '^', '`')
}
