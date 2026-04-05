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
import android.os.HandlerThread
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScreenshotManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionCallback: MediaProjection.Callback? = null
    private var projectionActive: Boolean = false

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    private var captureWidth: Int = 0
    private var captureHeight: Int = 0
    private var captureDensity: Int = 0

    private val stateLock = Any()
    private var pendingContinuation: CancellableContinuation<Bitmap>? = null

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val captureRequestController = ScreenshotCaptureRequestController(
        scheduler = ScreenshotCaptureRequestController.Scheduler { delayMs, action ->
            val handler = synchronized(stateLock) { captureHandler }
                ?: throw IllegalStateException("Capture handler not initialized")
            val runnable = Runnable(action)
            handler.postDelayed(runnable, delayMs)
            ScreenshotCaptureRequestController.Cancellable {
                handler.removeCallbacks(runnable)
            }
        }
    )
    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        handleImageAvailable(reader)
    }

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
        val handlerReady = synchronized(stateLock) { captureHandler != null }
        if (!hasActiveProjection() || imageReader == null || !handlerReady || captureWidth <= 0 || captureHeight <= 0) {
            continuation.resumeWithException(IllegalStateException("Capture pipeline is not ready"))
            return@suspendCancellableCoroutine
        }

        val registered = synchronized(stateLock) {
            if (pendingContinuation != null) {
                false
            } else {
                pendingContinuation = continuation
                true
            }
        }
        if (!registered) {
            continuation.resumeWithException(IllegalStateException("Capture already in progress"))
            return@suspendCancellableCoroutine
        }

        val started = try {
            captureRequestController.startCapture(
                startDelayMs = startDelayMs.coerceAtLeast(0L),
                timeoutMs = timeoutMs.coerceAtLeast(500L),
                dropFirstFrame = dropFirstFrame,
                onReady = { armImageListener() },
                onTimeout = { failPendingCapture("Failed to capture image (timeout)") }
            )
        } catch (e: Exception) {
            clearPendingContinuation()
            continuation.resumeWithException(Exception("Failed to prepare capture", e))
            return@suspendCancellableCoroutine
        }

        if (!started) {
            clearPendingContinuation()
            continuation.resumeWithException(IllegalStateException("Capture already in progress"))
            return@suspendCancellableCoroutine
        }

        continuation.invokeOnCancellation {
            cancelPendingCapture()
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

        ensureCaptureThread()
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

    private fun ensureCaptureThread() {
        if (synchronized(stateLock) { captureHandler != null }) {
            return
        }
        val thread = HandlerThread("MudingScreenCapture").apply { start() }
        val handler = Handler(thread.looper)
        synchronized(stateLock) {
            captureThread = thread
            captureHandler = handler
        }
    }

    private fun armImageListener() {
        val handler = synchronized(stateLock) { captureHandler }
        val reader = imageReader
        if (!hasActiveProjection() || handler == null || reader == null) {
            failPendingCapture("Capture pipeline is not ready")
            return
        }
        handler.post {
            if (!hasActiveProjection()) {
                failPendingCapture("Capture pipeline is not ready")
                return@post
            }
            val latestReader = imageReader
            if (latestReader == null) {
                failPendingCapture("Capture pipeline is not ready")
                return@post
            }
            clearPendingImages(latestReader)
            try {
                latestReader.setOnImageAvailableListener(imageAvailableListener, handler)
            } catch (e: Exception) {
                failPendingCapture("Failed to capture image", e)
            }
        }
    }

    private fun handleImageAvailable(reader: ImageReader) {
        var image: Image? = null
        try {
            image = reader.acquireLatestImage() ?: return
            when (captureRequestController.onFrameAvailable()) {
                ScreenshotCaptureRequestController.FrameDecision.IGNORE -> return
                ScreenshotCaptureRequestController.FrameDecision.DROP -> return
                ScreenshotCaptureRequestController.FrameDecision.CONSUME -> {
                    val bitmap = imageToBitmap(image)
                    captureRequestController.completeCapture()
                    clearImageListener()
                    completePendingCapture(bitmap)
                }
            }
        } catch (e: Exception) {
            captureRequestController.failCapture()
            clearImageListener()
            failPendingCapture("Failed to capture image", e)
        } finally {
            try {
                image?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun clearPendingImages(reader: ImageReader) {
        while (true) {
            val staleImage = try {
                reader.acquireLatestImage()
            } catch (_: Exception) {
                null
            } ?: break
            try {
                staleImage.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val width = captureWidth
        val height = captureHeight
        require(width > 0 && height > 0) { "Capture size is invalid" }

        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val tmpBitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        tmpBitmap.copyPixelsFromBuffer(buffer)

        val cropped = Bitmap.createBitmap(tmpBitmap, 0, 0, width, height)
        tmpBitmap.recycle()
        return cropped
    }

    private fun completePendingCapture(bitmap: Bitmap) {
        val continuation = clearPendingContinuation()
        if (continuation != null && continuation.isActive) {
            continuation.resume(bitmap)
        } else {
            bitmap.recycle()
        }
    }

    private fun failPendingCapture(message: String, cause: Throwable? = null) {
        captureRequestController.clear()
        clearImageListener()
        val continuation = clearPendingContinuation()
        if (continuation != null && continuation.isActive) {
            if (cause != null) {
                continuation.resumeWithException(Exception(message, cause))
            } else {
                continuation.resumeWithException(Exception(message))
            }
        }
    }

    private fun cancelPendingCapture() {
        captureRequestController.clear()
        clearImageListener()
        clearPendingContinuation()
    }

    private fun clearPendingContinuation(): CancellableContinuation<Bitmap>? {
        synchronized(stateLock) {
            val continuation = pendingContinuation
            pendingContinuation = null
            return continuation
        }
    }

    private fun clearImageListener() {
        try {
            imageReader?.setOnImageAvailableListener(null, null)
        } catch (_: Exception) {
        }
    }

    private fun releaseCapturePipeline() {
        failPendingCapture("Capture pipeline released")

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

        releaseCaptureThread()

        captureWidth = 0
        captureHeight = 0
        captureDensity = 0
    }

    private fun releaseCaptureThread() {
        val thread: HandlerThread?
        val handler: Handler?
        synchronized(stateLock) {
            thread = captureThread
            handler = captureHandler
            captureThread = null
            captureHandler = null
        }
        try {
            handler?.removeCallbacksAndMessages(null)
        } catch (_: Exception) {
        }
        try {
            thread?.quitSafely()
        } catch (_: Exception) {
        }
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
