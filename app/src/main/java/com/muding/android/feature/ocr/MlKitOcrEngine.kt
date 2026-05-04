package com.muding.android.feature.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MlKitOcrEngine : OcrEngine {

    private val chineseRecognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )
    private val latinRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    override suspend fun recognize(bitmap: Bitmap): OcrResult {
        val preparedBitmaps = OcrBitmapPreprocessor.prepare(bitmap)
        return try {
            val candidates = preparedBitmaps.flatMap { prepared ->
                listOf(
                    recognizeCandidate(
                        recognizer = chineseRecognizer,
                        bitmap = prepared.bitmap,
                        script = OcrRecognizerScript.CHINESE,
                        variant = prepared.variant
                    ),
                    recognizeCandidate(
                        recognizer = latinRecognizer,
                        bitmap = prepared.bitmap,
                        script = OcrRecognizerScript.LATIN,
                        variant = prepared.variant
                    )
                )
            }
            OcrRecognitionCandidateSelector.select(candidates)
        } finally {
            OcrBitmapPreprocessor.recycleOwned(preparedBitmaps)
        }
    }

    private suspend fun recognizeCandidate(
        recognizer: TextRecognizer,
        bitmap: Bitmap,
        script: OcrRecognizerScript,
        variant: OcrImageVariant
    ): OcrRecognitionCandidate {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = runCatching {
            recognizer.process(image).awaitResult().toOcrResult()
        }.getOrDefault(OcrResult.EMPTY)
        return OcrRecognitionCandidate(
            result = result,
            script = script,
            variant = variant
        )
    }

    private fun Text.toOcrResult(): OcrResult {
        return OcrResult(
            fullText = text,
            blocks = textBlocks.map { block ->
                OcrTextBlock(
                    text = block.text,
                    boundingBox = block.boundingBox,
                    lines = block.lines.map { line ->
                        OcrTextLine(
                            text = line.text,
                            boundingBox = line.boundingBox,
                            elements = line.elements.map { element ->
                                OcrTextElement(
                                    text = element.text,
                                    boundingBox = element.boundingBox
                                )
                            }
                        )
                    }
                )
            }
        )
    }
}
