package com.muding.android.feature.floatingball

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class FloatingBallImageProcessor(
    private val context: Context
) {

    suspend fun process(sourceUri: Uri): ProcessResult = withContext(Dispatchers.IO) {
        val bounds = readImageBounds(sourceUri)
        if (bounds == null) {
            return@withContext ProcessResult.RecoverableFailure(FailureReason.DECODE_FAILED)
        }
        val sampledBitmap = decodeSampledBitmap(sourceUri, bounds.width, bounds.height)
            ?: return@withContext ProcessResult.RecoverableFailure(FailureReason.DECODE_FAILED)

        val outputDir = File(context.cacheDir, CACHE_SUBDIRECTORY)
        val plan = createProcessingPlan(
            sourceWidth = sampledBitmap.width,
            sourceHeight = sampledBitmap.height,
            cacheDir = context.cacheDir,
            existingFiles = outputDir.listFiles()?.toList().orEmpty()
        )
        if (plan is ProcessingPlan.RecoverableFailure) {
            sampledBitmap.recycle()
            return@withContext ProcessResult.RecoverableFailure(plan.reason)
        }

        val readyPlan = plan as ProcessingPlan.Ready
        var croppedBitmap: Bitmap? = null
        var normalizedBitmap: Bitmap? = null
        try {
            croppedBitmap = Bitmap.createBitmap(
                sampledBitmap,
                readyPlan.crop.cropLeft,
                readyPlan.crop.cropTop,
                readyPlan.crop.cropSize,
                readyPlan.crop.cropSize
            )
            normalizedBitmap = Bitmap.createScaledBitmap(
                croppedBitmap,
                readyPlan.crop.outputSizePx,
                readyPlan.crop.outputSizePx,
                true
            )
            readyPlan.outputFile.parentFile?.mkdirs()
            readyPlan.staleFiles.forEach { it.delete() }
            FileOutputStream(readyPlan.outputFile).use { out ->
                normalizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            ProcessResult.Success(
                imageUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    readyPlan.outputFile
                ).toString()
            )
        } catch (_: Exception) {
            ProcessResult.RecoverableFailure(FailureReason.WRITE_FAILED)
        } finally {
            if (croppedBitmap != null && croppedBitmap !== sampledBitmap) {
                croppedBitmap.recycle()
            }
            normalizedBitmap?.recycle()
            sampledBitmap.recycle()
        }
    }

    fun clearCurrentImage() {
        File(context.cacheDir, CACHE_SUBDIRECTORY)
            .listFiles()
            ?.forEach { it.delete() }
    }

    private fun readImageBounds(sourceUri: Uri): ImageBounds? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                null
            } else {
                ImageBounds(width = options.outWidth, height = options.outHeight)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBitmap(
        sourceUri: Uri,
        width: Int,
        height: Int
    ): Bitmap? {
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = width,
                height = height,
                targetWidth = TARGET_IMAGE_SIZE_PX,
                targetHeight = TARGET_IMAGE_SIZE_PX
            )
        }
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        if (width <= 0 || height <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return 1
        }
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth > targetWidth * 2 || currentHeight > targetHeight * 2) {
            sampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private data class ImageBounds(
        val width: Int,
        val height: Int
    )

    sealed interface ProcessResult {
        data class Success(val imageUri: String) : ProcessResult
        data class RecoverableFailure(val reason: FailureReason) : ProcessResult
    }

    sealed interface ProcessingPlan {
        data class Ready(
            val crop: SquareCropPlan,
            val outputFile: File,
            val staleFiles: List<File>
        ) : ProcessingPlan

        data class RecoverableFailure(
            val reason: FailureReason,
            val recoverable: Boolean = true
        ) : ProcessingPlan
    }

    data class SquareCropPlan(
        val cropLeft: Int,
        val cropTop: Int,
        val cropSize: Int,
        val outputSizePx: Int
    )

    enum class FailureReason {
        INVALID_SOURCE_DIMENSIONS,
        DECODE_FAILED,
        WRITE_FAILED
    }

    companion object {
        internal const val CACHE_SUBDIRECTORY = "floating_ball"
        internal const val OUTPUT_FILE_NAME = "custom_image.png"
        internal const val TARGET_IMAGE_SIZE_PX = 256

        internal fun createProcessingPlan(
            sourceWidth: Int,
            sourceHeight: Int,
            cacheDir: File,
            existingFiles: List<File> = emptyList()
        ): ProcessingPlan {
            if (sourceWidth <= 0 || sourceHeight <= 0) {
                return ProcessingPlan.RecoverableFailure(FailureReason.INVALID_SOURCE_DIMENSIONS)
            }
            val cropSize = min(sourceWidth, sourceHeight)
            val outputDir = File(cacheDir, CACHE_SUBDIRECTORY)
            val outputFile = File(outputDir, OUTPUT_FILE_NAME)
            return ProcessingPlan.Ready(
                crop = SquareCropPlan(
                    cropLeft = (sourceWidth - cropSize) / 2,
                    cropTop = (sourceHeight - cropSize) / 2,
                    cropSize = cropSize,
                    outputSizePx = TARGET_IMAGE_SIZE_PX
                ),
                outputFile = outputFile,
                staleFiles = existingFiles.filterNot { candidate ->
                    candidate.absoluteFile == outputFile.absoluteFile
                }
            )
        }
    }
}
