package com.muding.android.domain.usecase

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScreenshotManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionCallback: MediaProjection.Callback? = null
    private var projectionActive: Boolean = false

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var captureWidth: Int = 0
    private var captureHeight: Int = 0
    private var captureDensity: Int = 0

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private val mainHandler = Handler(Looper.getMainLooper())

    fun createScreenCaptureIntent(): Intent = mediaProjectionManager.createScreenCaptureIntent()

    fun initMediaProjection(resultCode: Int, data: Intent) {
        release()

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                releaseCapturePipeline()
                projectionActive = false
                mediaProjection = null
                mediaProjectionCallback = null
            }
        }
        mediaProjectionCallback = callback
        mediaProjection?.registerCallback(callback, mainHandler)
        projectionActive = mediaProjection != null

        if (projectionActive) {
            setupCapturePipelineOnce()
        }
    }

    fun hasActiveProjection(): Boolean = mediaProjection != null && projectionActive

    suspend fun captureScreen(
        startDelayMs: Long = 150,
        dropFirstFrame: Boolean = false,
        timeoutMs: Long = 2500
    ): Bitmap = suspendCancellableCoroutine { continuation ->
        val reader = imageReader
        if (!hasActiveProjection() || reader == null || captureWidth <= 0 || captureHeight <= 0) {
            continuation.resumeWithException(IllegalStateException("Capture pipeline is not ready"))
            return@suspendCancellableCoroutine
        }

        fun failOnce(message: String, cause: Throwable? = null) {
            if (!continuation.isActive) return
            if (cause != null) continuation.resumeWithException(Exception(message, cause))
            else continuation.resumeWithException(Exception(message))
        }

        val timeoutAt = System.currentTimeMillis() + timeoutMs.coerceAtLeast(500)
        var dropped = false

        val pollRunnable = object : Runnable {
            override fun run() {
                if (!continuation.isActive) return

                var image: Image? = null
                try {
                    image = reader.acquireLatestImage()
                    if (image != null) {
                        if (dropFirstFrame && !dropped) {
                            dropped = true
                            image.close()
                            image = null
                        } else {
                            val bitmap = imageToBitmap(image)
                            continuation.resume(bitmap)
                            return
                        }
                    }
                } catch (e: Exception) {
                    failOnce("Failed to capture image", e)
                    return
                } finally {
                    try {
                        image?.close()
                    } catch (_: Exception) {
                    }
                }

                if (System.currentTimeMillis() >= timeoutAt) {
                    failOnce("Failed to capture image (timeout)")
                    return
                }

                mainHandler.postDelayed(this, 33)
            }
        }

        mainHandler.postDelayed(pollRunnable, startDelayMs.coerceAtLeast(0))

        continuation.invokeOnCancellation {
            mainHandler.removeCallbacks(pollRunnable)
        }
    }

    private fun setupCapturePipelineOnce() {
        if (virtualDisplay != null && imageReader != null) return

        val projection = mediaProjection ?: throw IllegalStateException("MediaProjection not initialized")

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        captureWidth = metrics.widthPixels
        captureHeight = metrics.heightPixels
        captureDensity = metrics.densityDpi

        imageReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 3)
        val reader = imageReader ?: throw IllegalStateException("ImageReader init failed")

        try {
            virtualDisplay = projection.createVirtualDisplay(
                "MudingScreenCapture",
                captureWidth,
                captureHeight,
                captureDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            )
        } catch (e: Exception) {
            releaseCapturePipeline()
            throw Exception("createVirtualDisplay failed", e)
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * captureWidth

        val tmpBitmap = Bitmap.createBitmap(
            captureWidth + rowPadding / pixelStride,
            captureHeight,
            Bitmap.Config.ARGB_8888
        )
        tmpBitmap.copyPixelsFromBuffer(buffer)

        val cropped = Bitmap.createBitmap(tmpBitmap, 0, 0, captureWidth, captureHeight)
        tmpBitmap.recycle()
        return cropped
    }

    private fun clearImageListener() {
        try {
            imageReader?.setOnImageAvailableListener(null, null)
        } catch (_: Exception) {
        }
    }

    private fun releaseCapturePipeline() {
        clearImageListener()

        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null

        captureWidth = 0
        captureHeight = 0
        captureDensity = 0
    }

    fun release() {
        releaseCapturePipeline()

        val projection = mediaProjection
        val callback = mediaProjectionCallback
        if (projection != null && callback != null) {
            try {
                projection.unregisterCallback(callback)
            } catch (_: Exception) {
            }
        }
        mediaProjectionCallback = null

        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        mediaProjection = null
        projectionActive = false
    }
}
