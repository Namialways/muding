package com.pixpin.android.service

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.animation.doOnEnd
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pixpin.android.MainActivity
import com.pixpin.android.R
import com.pixpin.android.app.AppGraph
import com.pixpin.android.core.model.PinSourceType
import com.pixpin.android.data.repository.RecentPinRepository
import com.pixpin.android.data.settings.AppSettingsRepository
import com.pixpin.android.domain.usecase.FloatingBallTheme
import com.pixpin.android.domain.usecase.ScreenshotManager
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.feature.capture.CaptureDispatchRequest
import com.pixpin.android.feature.capture.CaptureFlowCoordinator
import com.pixpin.android.feature.ocr.OcrFlowCoordinator
import com.pixpin.android.presentation.bridge.ForegroundLaunchBridgeActivity
import com.pixpin.android.presentation.crop.CaptureCropOverlay
import com.pixpin.android.presentation.ocr.OcrResultActivity
import com.pixpin.android.presentation.source.ClipboardTextPinActivity
import com.pixpin.android.presentation.source.GalleryOcrActivity
import com.pixpin.android.presentation.source.GalleryPinActivity
import com.pixpin.android.presentation.theme.floatingBallThemeColors
import com.pixpin.android.presentation.theme.PixPinTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class FloatingBallAppearance(
    val sizeDp: Int,
    val opacity: Float,
    val iconSizeDp: Int,
    val dragBoundPx: Int,
    val theme: FloatingBallTheme
)

private enum class CaptureEntryMode {
    PIN,
    OCR
}

class FloatingBallService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var floatingView: ComposeView? = null
    private var floatingBallParams: WindowManager.LayoutParams? = null
    private var floatingMenuView: ComposeView? = null
    private var floatingMenuParams: WindowManager.LayoutParams? = null
    private var floatingMenuDismissView: View? = null
    private var cropOverlayView: ComposeView? = null
    private var cropOverlayBitmap: Bitmap? = null
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val lifecycleRegistry = LifecycleRegistry(this)

    private lateinit var screenshotManager: ScreenshotManager
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var recentPinRepository: RecentPinRepository
    private lateinit var captureFlowCoordinator: CaptureFlowCoordinator
    private lateinit var ocrFlowCoordinator: OcrFlowCoordinator

    private var snapAnimator: ValueAnimator? = null
    private var snapRunnable: Runnable? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val floatingMenuExpanded = mutableStateOf(false)
    private val floatingBallX = mutableIntStateOf(100)
    private val floatingBallY = mutableIntStateOf(100)
    private var pendingCaptureMode: CaptureEntryMode = CaptureEntryMode.PIN

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        screenshotManager = ScreenshotManager(this)
        settingsRepository = AppGraph.appSettingsRepository(this)
        recentPinRepository = AppGraph.recentPinRepository(this)
        captureFlowCoordinator = AppGraph.captureFlowCoordinator(this)
        ocrFlowCoordinator = AppGraph.ocrFlowCoordinator(this)

        showFloatingBall()

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH_FLOATING_BALL_APPEARANCE -> {
                recreateFloatingBall()
            }

            ACTION_RESTORE_FLOATING_BALL_VISIBILITY -> {
                restoreFloatingBall()
            }

            ACTION_START_SCREENSHOT -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    floatingView?.visibility = View.GONE
                    createNotificationChannel()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            createNotification(),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, createNotification())
                    }
                    screenshotManager.initMediaProjection(resultCode, resultData)
                    captureAndShowCropOverlay()
                } else {
                    floatingView?.visibility = View.VISIBLE
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showFloatingBall() {
        val appearance = loadFloatingBallAppearance()
        floatingMenuExpanded.value = false
        val params = floatingBallParams ?: WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = floatingBallX.intValue
            y = floatingBallY.intValue
        }
        floatingBallParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingBallService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBallService)
            setContent {
                PixPinTheme {
                    FloatingBallContent(
                        appearance = appearance,
                        isExpanded = floatingMenuExpanded.value,
                        onExpandedChange = { setFloatingMenuExpanded(it) },
                        onScreenshot = { startStandardScreenshot() },
                        onPositionChange = { dx, dy ->
                            cancelSnap()
                            val display = windowManager.defaultDisplay
                            val size = Point()
                            display.getRealSize(size)

                            val nextX = (floatingBallX.intValue + dx.roundToInt())
                                .coerceIn(0, (size.x - appearance.dragBoundPx).coerceAtLeast(0))
                            val nextY = (floatingBallY.intValue + dy.roundToInt())
                                .coerceIn(0, (size.y - appearance.dragBoundPx).coerceAtLeast(0))
                            floatingBallX.intValue = nextX
                            floatingBallY.intValue = nextY

                            if (floatingMenuExpanded.value) {
                                return@FloatingBallContent
                            }

                            params.x = nextX
                            params.y = nextY

                            try {
                                windowManager.updateViewLayout(this, params)
                            } catch (_: Exception) {
                            }
                        },
                        onDragEnd = { scheduleSnapToEdge(params) }
                    )
                }
            }
        }
        floatingView = view

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun recreateFloatingBall() {
        cancelSnap()
        setFloatingMenuExpanded(false)
        floatingView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (_: Exception) {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {
                }
            }
        }
        floatingView = null
        showFloatingBall()
    }

    private fun loadFloatingBallAppearance(): FloatingBallAppearance {
        val density = resources.displayMetrics.density
        val floatingBallSettings = settingsRepository.getFloatingBallSettings()
        val sizeDp = floatingBallSettings.sizeDp
        return FloatingBallAppearance(
            sizeDp = sizeDp,
            opacity = floatingBallSettings.opacity,
            iconSizeDp = (sizeDp * 0.52f).roundToInt().coerceIn(24, 42),
            dragBoundPx = (sizeDp * density).roundToInt(),
            theme = floatingBallSettings.theme
        )
    }

    private fun handleScreenshot() {
        setFloatingMenuExpanded(false)
        if (screenshotManager.hasActiveProjection()) {
            floatingView?.visibility = View.GONE
            captureAndShowCropOverlay()
            return
        }

        val intent = Intent(this, ScreenshotPermissionActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        startActivity(intent)
    }

    private fun startStandardScreenshot() {
        pendingCaptureMode = CaptureEntryMode.PIN
        handleScreenshot()
    }

    private fun startScreenshotOcr() {
        pendingCaptureMode = CaptureEntryMode.OCR
        handleScreenshot()
    }

    private fun openGalleryPicker() {
        setFloatingMenuExpanded(false)
        floatingView?.visibility = View.GONE
        startActivity(
            Intent(this, GalleryPinActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra(GalleryPinActivity.EXTRA_FINISH_TO_BACKGROUND, true)
                putExtra(GalleryPinActivity.EXTRA_RESTORE_FLOATING_BALL, true)
            }
        )
    }

    private fun openGalleryOcr() {
        setFloatingMenuExpanded(false)
        floatingView?.visibility = View.GONE
        startActivity(
            Intent(this, GalleryOcrActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra(GalleryOcrActivity.EXTRA_FINISH_TO_BACKGROUND, true)
                putExtra(GalleryOcrActivity.EXTRA_RESTORE_FLOATING_BALL, true)
            }
        )
    }

    private fun openClipboardTextPin() {
        setFloatingMenuExpanded(false)
        floatingView?.visibility = View.GONE
        startActivity(
            Intent(this, ClipboardTextPinActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra(ClipboardTextPinActivity.EXTRA_FINISH_TO_BACKGROUND, true)
                putExtra(ClipboardTextPinActivity.EXTRA_RESTORE_FLOATING_BALL, true)
            }
        )
    }

    private fun captureAndShowCropOverlay() {
        lifecycleScope.launch {
            try {
                val bitmap = screenshotManager.captureScreen()
                showCropOverlay(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                screenshotManager.release()
                restoreFloatingBall()
            }
        }
    }

    private fun showCropOverlay(bitmap: Bitmap) {
        dismissCropOverlay(recycleBitmap = true, restoreFloatingBall = false)
        cropOverlayBitmap = bitmap
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        cropOverlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingBallService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBallService)
            setContent {
                PixPinTheme {
                    CaptureCropOverlay(
                        bitmap = bitmap,
                        onCancel = {
                            pendingCaptureMode = CaptureEntryMode.PIN
                            dismissCropOverlay(recycleBitmap = true, restoreFloatingBall = true)
                        },
                        onConfirm = { cropRect ->
                            completeCaptureSelection(cropRect)
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(cropOverlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            dismissCropOverlay(recycleBitmap = true, restoreFloatingBall = true)
        }
    }

    private fun completeCaptureSelection(cropRectInBitmap: Rect) {
        when (pendingCaptureMode) {
            CaptureEntryMode.PIN -> cropAndContinue(cropRectInBitmap)
            CaptureEntryMode.OCR -> ocrAndContinue(cropRectInBitmap)
        }
    }

    private fun cropAndContinue(cropRectInBitmap: Rect) {
        val bitmap = cropOverlayBitmap ?: return
        lifecycleScope.launch {
            try {
                val preparedResult = captureFlowCoordinator.prepareCaptureResult(
                    bitmap = bitmap,
                    cropRectInBitmap = cropRectInBitmap,
                    request = CaptureDispatchRequest(
                        sourceType = PinSourceType.SCREENSHOT,
                        launchEditorInNewTask = true
                    )
                )

                val launchBeforeDismiss =
                    preparedResult.resolvedAction == CaptureResultAction.OPEN_EDITOR
                if (launchBeforeDismiss) {
                    captureFlowCoordinator.dispatchPreparedResult(this@FloatingBallService, preparedResult)
                }
                dismissCropOverlay(recycleBitmap = true, restoreFloatingBall = false)
                if (!launchBeforeDismiss) {
                    captureFlowCoordinator.dispatchPreparedResult(this@FloatingBallService, preparedResult)
                }
                restoreFloatingBall()
            } catch (e: Exception) {
                e.printStackTrace()
                dismissCropOverlay(recycleBitmap = true, restoreFloatingBall = true)
            } finally {
                pendingCaptureMode = CaptureEntryMode.PIN
            }
        }
    }

    private fun ocrAndContinue(cropRectInBitmap: Rect) {
        val bitmap = cropOverlayBitmap ?: return
        lifecycleScope.launch {
            try {
                val preparedResult = ocrFlowCoordinator.prepareTextPin(
                    bitmap = bitmap,
                    cropRectInBitmap = cropRectInBitmap
                )
                startActivity(
                    ForegroundLaunchBridgeActivity.createIntent(
                        context = this@FloatingBallService,
                        targetIntent = Intent(this@FloatingBallService, OcrResultActivity::class.java).apply {
                            putExtra(OcrResultActivity.EXTRA_RECOGNIZED_TEXT, preparedResult.recognizedText)
                            putExtra(OcrResultActivity.EXTRA_FINISH_TO_BACKGROUND, true)
                            putExtra(OcrResultActivity.EXTRA_RESTORE_FLOATING_BALL, true)
                        }
                    )
                )
                dismissCropOverlay(recycleBitmap = true, restoreFloatingBall = false)
            } catch (e: Exception) {
                Toast.makeText(
                    this@FloatingBallService,
                    "OCR failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                dismissCropOverlay(recycleBitmap = true, restoreFloatingBall = true)
            } finally {
                pendingCaptureMode = CaptureEntryMode.PIN
            }
        }
    }

    private fun dismissCropOverlay(recycleBitmap: Boolean, restoreFloatingBall: Boolean) {
        cropOverlayView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (_: Exception) {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {
                }
            }
        }
        cropOverlayView = null
        if (recycleBitmap) {
            cropOverlayBitmap?.recycle()
            cropOverlayBitmap = null
        }
        if (restoreFloatingBall) {
            restoreFloatingBall()
        }
    }

    private fun restoreFloatingBall() {
        mainHandler.post {
            setFloatingMenuExpanded(false)
            floatingView?.visibility = View.VISIBLE
            floatingBallParams?.let { params ->
                params.x = floatingBallX.intValue
                params.y = floatingBallY.intValue
                try {
                    floatingView?.let { view ->
                        windowManager.updateViewLayout(view, params)
                    }
                } catch (_: Exception) {
                }
                scheduleSnapToEdge(params)
            }
        }
    }

    private fun restoreLastClosedPin() {
        if (!recentPinRepository.hasRecent()) {
            return
        }
        startService(
            Intent(this, PinOverlayService::class.java).apply {
                action = PinOverlayService.ACTION_RESTORE_LAST_CLOSED
            }
        )
    }

    private fun openPinManager() {
        startService(
            Intent(this, PinOverlayService::class.java).apply {
                action = PinOverlayService.ACTION_OPEN_MANAGER
            }
        )
    }

    private fun openSettings() {
        setFloatingMenuExpanded(false)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun setFloatingMenuExpanded(expanded: Boolean) {
        if (floatingMenuExpanded.value == expanded) {
            return
        }
        if (expanded) {
            cancelSnap()
        }
        floatingMenuExpanded.value = expanded
        if (expanded) {
            showFloatingMenuOverlay()
        } else {
            dismissFloatingMenuOverlay()
        }
    }

    private fun showFloatingMenuOverlay() {
        if (floatingMenuView != null || floatingMenuDismissView != null) {
            updateFloatingMenuPosition()
            return
        }

        val dismissParams = createOverlayLayoutParams(
            width = WindowManager.LayoutParams.MATCH_PARENT,
            height = WindowManager.LayoutParams.MATCH_PARENT,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )
        val dismissView = View(this).apply {
            isClickable = true
            setOnClickListener { setFloatingMenuExpanded(false) }
        }

        val menuParams = createOverlayLayoutParams(
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        val menuView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingBallService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBallService)
            setContent {
                PixPinTheme {
                    FloatingMenu(
                        onScreenshot = { startStandardScreenshot() },
                        onScreenshotOcr = { startScreenshotOcr() },
                        onGallery = { openGalleryPicker() },
                        onGalleryOcr = { openGalleryOcr() },
                        onClipboardText = { openClipboardTextPin() },
                        onRestorePin = { restoreLastClosedPin() },
                        onManagePins = { openPinManager() },
                        onSettings = { openSettings() },
                        onExit = { stopSelf() }
                    )
                }
            }
        }

        val appearance = loadFloatingBallAppearance()
        val marginPx = (12 * resources.displayMetrics.density).roundToInt()
        menuParams.x = floatingBallX.intValue + appearance.dragBoundPx + marginPx
        menuParams.y = floatingBallY.intValue
        menuView.alpha = 0f

        try {
            windowManager.addView(dismissView, dismissParams)
            floatingMenuDismissView = dismissView
            windowManager.addView(menuView, menuParams)
            floatingMenuView = menuView
            floatingMenuParams = menuParams
            menuView.post {
                updateFloatingMenuPosition()
                menuView.animate()
                    .alpha(1f)
                    .setDuration(120)
                    .start()
            }
        } catch (_: Exception) {
            dismissFloatingMenuOverlay()
            floatingMenuExpanded.value = false
        }
    }

    private fun dismissFloatingMenuOverlay() {
        floatingMenuView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (_: Exception) {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {
                }
            }
        }
        floatingMenuView = null
        floatingMenuParams = null

        floatingMenuDismissView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (_: Exception) {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {
                }
            }
        }
        floatingMenuDismissView = null
    }

    private fun updateFloatingMenuPosition() {
        val menuView = floatingMenuView ?: return
        val menuParams = floatingMenuParams ?: return
        val appearance = loadFloatingBallAppearance()
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        val screenWidth = size.x
        val screenHeight = size.y
        val menuWidth = menuView.width.takeIf { it > 0 } ?: menuView.measuredWidth
        val menuHeight = menuView.height.takeIf { it > 0 } ?: menuView.measuredHeight
        val targetPosition = computeFloatingMenuPosition(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            menuWidth = menuWidth,
            menuHeight = menuHeight,
            ballSizePx = appearance.dragBoundPx
        )

        menuParams.x = targetPosition.x
        menuParams.y = targetPosition.y
        try {
            windowManager.updateViewLayout(menuView, menuParams)
        } catch (_: Exception) {
        }
    }

    private fun computeFloatingMenuPosition(
        screenWidth: Int,
        screenHeight: Int,
        menuWidth: Int,
        menuHeight: Int,
        ballSizePx: Int
    ): Point {
        val marginPx = (12 * resources.displayMetrics.density).roundToInt()

        val preferredRightX = floatingBallX.intValue + ballSizePx + marginPx
        val preferredLeftX = floatingBallX.intValue - menuWidth - marginPx
        val targetX = if (menuWidth <= 0) {
            preferredRightX
        } else if (preferredRightX + menuWidth <= screenWidth - marginPx) {
            preferredRightX
        } else {
            preferredLeftX.coerceAtLeast(marginPx)
        }

        val centeredY = floatingBallY.intValue + (ballSizePx - menuHeight) / 2
        val targetY = if (menuHeight <= 0) {
            floatingBallY.intValue
        } else {
            centeredY.coerceIn(
                marginPx,
                (screenHeight - menuHeight - marginPx).coerceAtLeast(marginPx)
            )
        }
        return Point(targetX, targetY)
    }

    private fun createOverlayLayoutParams(
        width: Int,
        height: Int,
        flags: Int
    ): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "幕钉服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "幕钉悬浮截图服务"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.floating_ball_notification_title))
            .setContentText(getString(R.string.floating_ball_notification_message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun scheduleSnapToEdge(params: WindowManager.LayoutParams) {
        if (floatingMenuExpanded.value) {
            return
        }
        cancelSnap()
        val runnable = Runnable { animateToEdge(params) }
        snapRunnable = runnable
        mainHandler.postDelayed(runnable, 2500)
    }

    private fun cancelSnap() {
        snapRunnable?.let { mainHandler.removeCallbacks(it) }
        snapRunnable = null
        snapAnimator?.cancel()
        snapAnimator = null
    }

    private fun animateToEdge(params: WindowManager.LayoutParams) {
        if (floatingMenuExpanded.value) {
            return
        }
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        val screenWidth = size.x

        val viewWidth = (floatingView?.width?.takeIf { it > 0 }
            ?: params.width.takeIf { it > 0 }
            ?: loadFloatingBallAppearance().dragBoundPx)
        val currentX = params.x
        val targetX = if (currentX < (screenWidth - viewWidth) / 2) 0 else screenWidth - viewWidth

        snapAnimator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                floatingBallX.intValue = params.x
                try {
                    windowManager.updateViewLayout(floatingView, params)
                } catch (_: Exception) {
                }
            }
            doOnEnd {
                snapAnimator = null
            }
        }
        snapAnimator?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelSnap()
        setFloatingMenuExpanded(false)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        try {
            screenshotManager.release()
        } catch (_: Exception) {
        }
        dismissCropOverlay(recycleBitmap = true, restoreFloatingBall = false)
        floatingView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (e: Exception) {
                try {
                    windowManager.removeView(it)
                } catch (inner: Exception) {
                    inner.printStackTrace()
                }
            }
        }
        floatingView = null
    }

    companion object {
        private const val CHANNEL_ID = "floating_ball_service"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START_SCREENSHOT = "com.pixpin.android.action.START_SCREENSHOT"
        const val ACTION_REFRESH_FLOATING_BALL_APPEARANCE =
            "com.pixpin.android.action.REFRESH_FLOATING_BALL_APPEARANCE"
        const val ACTION_RESTORE_FLOATING_BALL_VISIBILITY =
            "com.pixpin.android.action.RESTORE_FLOATING_BALL_VISIBILITY"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        fun createRestoreVisibilityIntent(context: Context): Intent {
            return Intent(context, FloatingBallService::class.java).apply {
                action = ACTION_RESTORE_FLOATING_BALL_VISIBILITY
            }
        }
    }
}

@Composable
private fun FloatingBallContent(
    appearance: FloatingBallAppearance,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onScreenshot: () -> Unit,
    onPositionChange: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { onExpandedChange(false) },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragEnd() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    onPositionChange(dragAmount.x, dragAmount.y)
                }
            )
        }
    ) {
        FloatingBall(
            appearance = appearance,
            onLongClick = { onExpandedChange(!isExpanded) },
            onClick = {
                if (isExpanded) {
                    onExpandedChange(false)
                } else {
                    onScreenshot()
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FloatingBall(
    appearance: FloatingBallAppearance,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val colors = floatingBallThemeColors(appearance.theme)
    Surface(
        modifier = Modifier
            .size(appearance.sizeDp.dp)
            .shadow(8.dp, CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(appearance.opacity)
                .background(
                    Brush.linearGradient(
                        colors = listOf(colors.start, colors.end)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "截图",
                tint = Color.White,
                modifier = Modifier.size(appearance.iconSizeDp.dp)
            )
        }
    }
}

@Composable
private fun FloatingMenu(
    modifier: Modifier = Modifier,
    onScreenshot: () -> Unit,
    onScreenshotOcr: () -> Unit,
    onGallery: () -> Unit,
    onGalleryOcr: () -> Unit,
    onClipboardText: () -> Unit,
    onRestorePin: () -> Unit,
    onManagePins: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    Card(
        modifier = modifier
            .width(220.dp)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            MenuButton(
                icon = Icons.Default.Camera,
                text = "截图",
                onClick = onScreenshot
            )
            MenuButton(
                icon = Icons.Default.TextFields,
                text = "截图 OCR",
                onClick = onScreenshotOcr
            )
            MenuButton(
                icon = Icons.Default.PhotoLibrary,
                text = "相册贴图",
                onClick = onGallery
            )
            MenuButton(
                icon = Icons.Default.PhotoLibrary,
                text = "相册 OCR",
                onClick = onGalleryOcr
            )
            MenuButton(
                icon = Icons.Default.TextFields,
                text = "剪贴板文字贴图",
                onClick = onClipboardText
            )
            MenuButton(
                icon = Icons.Default.History,
                text = "恢复已关闭贴图",
                onClick = onRestorePin
            )
            MenuButton(
                icon = Icons.Default.ViewList,
                text = "贴图管理",
                onClick = onManagePins
            )
            MenuButton(
                icon = Icons.Default.Settings,
                text = "设置",
                onClick = onSettings
            )
            MenuButton(
                icon = Icons.Default.Close,
                text = "退出",
                onClick = onExit
            )
        }
    }
}

@Composable
private fun MenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text)
        }
    }
}




