package com.pixpin.android.domain.usecase

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

/**
 * 截图管理器 - 负责屏幕截图功能
 */
class ScreenshotManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionCallback: MediaProjection.Callback? = null

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    /**
     * 获取截图权限的 Intent
     */
    fun createScreenCaptureIntent(): Intent = mediaProjectionManager.createScreenCaptureIntent()

    /**
     * 初始化 MediaProjection
     */
    fun initMediaProjection(resultCode: Int, data: Intent) {
        // 先释放旧的 projection，避免重复注册/资源泄露
        release()

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        // Android 14+（部分 ROM）要求在开始捕获前必须注册 callback
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                cleanUpCapturePipeline()
            }
        }
        mediaProjectionCallback = callback
        mediaProjection?.registerCallback(callback, Handler(Looper.getMainLooper()))
    }

    /**
     * 执行截图
     *
     * 说明：ImageReader 可能不会在固定 100ms 内就绪，尤其在高版本系统或性能一般的机器上。
     * 这里用 onImageAvailable + 超时兜底，提升成功率。
     */
    suspend fun captureScreen(): Bitmap = suspendCancellableCoroutine { continuation ->
        val projection = mediaProjection
        if (projection == null) {
            continuation.resumeWithException(IllegalStateException("MediaProjection is not initialized"))
            return@suspendCancellableCoroutine
        }

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        // 重新创建 pipeline（避免上次残留导致 BufferQueue abandoned）
        cleanUpCapturePipeline()

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val reader = imageReader!!

        val mainHandler = Handler(Looper.getMainLooper())

        fun failOnce(message: String, cause: Throwable? = null) {
            if (!continuation.isActive) return
            cleanUpCapturePipeline()
            if (cause != null) continuation.resumeWithException(Exception(message, cause))
            else continuation.resumeWithException(Exception(message))
        }

        val listener = ImageReader.OnImageAvailableListener {
            if (!continuation.isActive) return@OnImageAvailableListener

            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                if (image == null) return@OnImageAvailableListener

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

                cleanUpCapturePipeline()
                continuation.resume(cropped)
            } catch (e: Exception) {
                failOnce("Failed to capture image", e)
            } finally {
                try {
                    image?.close()
                } catch (_: Exception) {
                }
            }
        }

        reader.setOnImageAvailableListener(listener, mainHandler)

        try {
            virtualDisplay = projection.createVirtualDisplay(
                "PixPinScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            )
        } catch (e: Exception) {
            failOnce("createVirtualDisplay failed", e)
            return@suspendCancellableCoroutine
        }

        // 超时兜底：2 秒内没收到图像就失败（避免一直挂起）
        val timeoutRunnable = Runnable {
            failOnce("Failed to capture image (timeout)")
        }
        mainHandler.postDelayed(timeoutRunnable, 2000)

        continuation.invokeOnCancellation {
            try {
                mainHandler.removeCallbacks(timeoutRunnable)
            } catch (_: Exception) {
            }
            cleanUpCapturePipeline()
        }
    }

    /**
     * 只清理一次截图的管线资源（virtualDisplay / imageReader）。
     * 注意：不要在这里 stop MediaProjection，否则下次截图需要重新授权。
     */
    private fun cleanUpCapturePipeline() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null

        try {
            imageReader?.setOnImageAvailableListener(null, null)
        } catch (_: Exception) {
        }

        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null
    }

    /**
     * 释放 MediaProjection
     */
    fun release() {
        cleanUpCapturePipeline()

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
    }
}
