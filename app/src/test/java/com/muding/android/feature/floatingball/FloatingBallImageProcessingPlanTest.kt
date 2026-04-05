package com.muding.android.feature.floatingball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FloatingBallImageProcessingPlanTest {

    @Test
    fun landscapeSource_createsCenteredSquareCropPlan() {
        val cacheDir = Files.createTempDirectory("floating-ball-plan").toFile()

        try {
            val ready = requireReady(
                FloatingBallImageProcessor.createProcessingPlan(
                    sourceWidth = 900,
                    sourceHeight = 500,
                    cacheDir = cacheDir
                )
            )

            assertEquals(200, ready.crop.cropLeft)
            assertEquals(0, ready.crop.cropTop)
            assertEquals(500, ready.crop.cropSize)
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    @Test
    fun processingPlan_normalizesOutputToFixedTargetSize() {
        val cacheDir = Files.createTempDirectory("floating-ball-plan").toFile()

        try {
            val ready = requireReady(
                FloatingBallImageProcessor.createProcessingPlan(
                    sourceWidth = 720,
                    sourceHeight = 1280,
                    cacheDir = cacheDir,
                    outputFileName = "custom_image_1.png"
                )
            )

            assertEquals(FloatingBallImageProcessor.TARGET_IMAGE_SIZE_PX, ready.crop.outputSizePx)
            assertEquals(
                File(
                    File(cacheDir, FloatingBallImageProcessor.CACHE_SUBDIRECTORY),
                    "custom_image_1.png"
                ).absolutePath,
                ready.outputFile.absolutePath
            )
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    @Test
    fun replacementPlan_keepsOnlyOneCurrentImageReference() {
        val cacheDir = Files.createTempDirectory("floating-ball-plan").toFile()
        val outputDir = File(cacheDir, FloatingBallImageProcessor.CACHE_SUBDIRECTORY).apply { mkdirs() }
        val staleFile = File(outputDir, "previous_custom.png").apply { writeText("old") }
        val currentFile = File(outputDir, "custom_image_100.png").apply { writeText("current") }

        try {
            val ready = requireReady(
                FloatingBallImageProcessor.createProcessingPlan(
                    sourceWidth = 640,
                    sourceHeight = 640,
                    cacheDir = cacheDir,
                    existingFiles = listOf(staleFile, currentFile),
                    outputFileName = "custom_image_200.png"
                )
            )

            assertEquals(
                File(outputDir, "custom_image_200.png").absolutePath,
                ready.outputFile.absolutePath
            )
            assertEquals(
                listOf(staleFile.absolutePath, currentFile.absolutePath).sorted(),
                ready.staleFiles.map { it.absolutePath }.sorted()
            )
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    @Test
    fun invalidSourceDimensions_returnRecoverableFailure() {
        val cacheDir = Files.createTempDirectory("floating-ball-plan").toFile()

        try {
            val failure = FloatingBallImageProcessor.createProcessingPlan(
                sourceWidth = 0,
                sourceHeight = 640,
                cacheDir = cacheDir
            ) as FloatingBallImageProcessor.ProcessingPlan.RecoverableFailure

            assertEquals(
                FloatingBallImageProcessor.FailureReason.INVALID_SOURCE_DIMENSIONS,
                failure.reason
            )
            assertTrue(failure.recoverable)
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    private fun requireReady(
        plan: FloatingBallImageProcessor.ProcessingPlan
    ): FloatingBallImageProcessor.ProcessingPlan.Ready {
        return plan as? FloatingBallImageProcessor.ProcessingPlan.Ready
            ?: throw AssertionError("Expected ready plan but was $plan")
    }
}
