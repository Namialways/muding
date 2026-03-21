package com.pixpin.android.feature.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
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
        val image = InputImage.fromBitmap(bitmap, 0)
        val chineseResult = runCatching {
            chineseRecognizer.process(image).awaitResult().toOcrResult()
        }.getOrDefault(OcrResult.EMPTY)

        if (chineseResult.score() >= MIN_PREFERRED_SCORE) {
            return chineseResult
        }

        val latinResult = runCatching {
            latinRecognizer.process(image).awaitResult().toOcrResult()
        }.getOrDefault(OcrResult.EMPTY)

        return if (latinResult.score() > chineseResult.score()) {
            latinResult
        } else {
            chineseResult
        }
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

    companion object {
        private const val MIN_PREFERRED_SCORE = 4
    }
}
