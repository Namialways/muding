package com.muding.android.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.muding.android.app.AppGraph
import com.muding.android.data.repository.PinHistoryRepository
import com.muding.android.data.repository.RecentPinRepository
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.data.settings.OnboardingGuideProgress
import com.muding.android.feature.pin.creation.EditorLaunchRequest
import com.muding.android.feature.pin.creation.PinCreationCoordinator
import com.muding.android.feature.pin.source.PinDecodeTargetSizing
import com.muding.android.domain.usecase.ClosedPinRecord
import com.muding.android.domain.usecase.PinHistoryMetadata
import com.muding.android.domain.usecase.PinHistorySourceType
import com.muding.android.feature.onboarding.OnboardingGuideState
import com.muding.android.feature.pin.runtime.PinManagerContent
import com.muding.android.feature.pin.runtime.PinOverlayWindowController
import com.muding.android.presentation.theme.MudingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PinOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private val overlays = LinkedHashMap<String, PinOverlayWindowController>()
    private var managerView: ComposeView? = null
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var pinHistoryRepository: PinHistoryRepository
    private lateinit var recentPinRepository: RecentPinRepository
    private lateinit var pinCreationCoordinator: PinCreationCoordinator
    private var managerRefreshToken by mutableIntStateOf(0)
    private var pinHintView: ComposeView? = null
    private var pinHintParams: WindowManager.LayoutParams? = null
    private var pinHintAnchorOverlayId: String? = null
    private var onboardingGuideState = OnboardingGuideState.fromProgress(
        OnboardingGuideProgress(
            hasSeenHomeGuide = false,
            hasSeenFloatingBallHint = false,
            hasSeenPinOverlayHint = false,
            hasSeenEditorHint = false
        )
    )

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsRepository = AppGraph.appSettingsRepository(this)
        pinHistoryRepository = AppGraph.pinHistoryRepository(this)
        recentPinRepository = AppGraph.recentPinRepository(this)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)
        onboardingGuideState = OnboardingGuideState.fromProgress(settingsRepository.getOnboardingGuideProgress())
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RESTORE_LAST_CLOSED -> {
                restoreLastClosedOverlay()
                return START_NOT_STICKY
            }

            ACTION_OPEN_MANAGER -> {
                openManagerOverlay()
                return START_NOT_STICKY
            }
        }

        val imageUriString = intent?.getStringExtra(EXTRA_IMAGE_URI)
        val annotationSessionId = intent?.getStringExtra(EXTRA_ANNOTATION_SESSION_ID)
        val initialContentWidthPx = intent?.getIntExtra(EXTRA_INITIAL_CONTENT_WIDTH_PX, 0)?.takeIf { it > 0 }
        val initialContentHeightPx = intent?.getIntExtra(EXTRA_INITIAL_CONTENT_HEIGHT_PX, 0)?.takeIf { it > 0 }
        val historySourceType = PinHistorySourceType.fromValue(intent?.getStringExtra(EXTRA_HISTORY_SOURCE))
        val historyDisplayName = intent?.getStringExtra(EXTRA_HISTORY_DISPLAY_NAME)
        val historyTextPreview = intent?.getStringExtra(EXTRA_HISTORY_TEXT_PREVIEW)
        if (!imageUriString.isNullOrBlank()) {
            val imageUri = android.net.Uri.parse(imageUriString)
            serviceScope.launch {
                decodeBitmap(
                    uri = imageUri,
                    preferredWidth = initialContentWidthPx,
                    preferredHeight = initialContentHeightPx
                )?.let { bitmap ->
                    showPinOverlay(
                        bitmap = bitmap,
                        imageUriString = imageUriString,
                        annotationSessionId = annotationSessionId,
                        initialContentWidthPx = initialContentWidthPx,
                        initialContentHeightPx = initialContentHeightPx,
                        historySourceType = historySourceType,
                        historyMetadata = PinHistoryMetadata(
                            displayName = historyDisplayName,
                            textPreview = historyTextPreview,
                            widthPx = initialContentWidthPx,
                            heightPx = initialContentHeightPx
                        )
                    )
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun restoreLastClosedOverlay() {
        val record = recentPinRepository.popMostRecent() ?: return
        val historyRecord = pinHistoryRepository.list().firstOrNull {
            it.imageUri == record.imageUri && it.annotationSessionId == record.annotationSessionId
        }
        serviceScope.launch {
            decodeBitmap(android.net.Uri.parse(record.imageUri))?.let { bitmap ->
                showPinOverlay(
                    bitmap = bitmap,
                    imageUriString = record.imageUri,
                    annotationSessionId = record.annotationSessionId,
                    initialContentWidthPx = null,
                    initialContentHeightPx = null,
                    historySourceType = PinHistorySourceType.RESTORED_PIN,
                    historyMetadata = PinHistoryMetadata(
                        displayName = historyRecord?.displayName,
                        textPreview = historyRecord?.textPreview,
                        widthPx = historyRecord?.widthPx,
                        heightPx = historyRecord?.heightPx
                    )
                )
            }
        }
    }

    private suspend fun decodeBitmap(
        uri: android.net.Uri,
        preferredWidth: Int? = null,
        preferredHeight: Int? = null
    ): Bitmap? {
        return try {
            withContext(Dispatchers.IO) {
                val decodeTarget = PinDecodeTargetSizing.overlayDecodeTarget(
                    screenWidthPx = resources.displayMetrics.widthPixels,
                    screenHeightPx = resources.displayMetrics.heightPixels,
                    preferredWidthPx = preferredWidth,
                    preferredHeightPx = preferredHeight
                )
                val bounds = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, bounds)
                }
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(
                        width = bounds.outWidth,
                        height = bounds.outHeight,
                        targetWidth = decodeTarget.widthPx,
                        targetHeight = decodeTarget.heightPx
                    )
                }
                contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                }
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

    private fun showPinOverlay(
        bitmap: Bitmap,
        imageUriString: String,
        annotationSessionId: String?,
        initialContentWidthPx: Int?,
        initialContentHeightPx: Int?,
        historySourceType: PinHistorySourceType,
        historyMetadata: PinHistoryMetadata
    ) {
        if (historySourceType == PinHistorySourceType.RESTORED_PIN) {
            val existingOverlayId = overlays.entries.firstOrNull { (_, controller) ->
                controller.matches(imageUriString, annotationSessionId)
            }?.key
            if (existingOverlayId != null) {
                bitmap.recycle()
                bringOverlayToFront(existingOverlayId)
                overlays[existingOverlayId]?.shakeToHintRestore()
                return
            }
        }

        val overlayId = UUID.randomUUID().toString()
        val appearanceSettings = settingsRepository.getPinAppearanceSettings()
        val controller = PinOverlayWindowController(
            context = this,
            windowManager = windowManager,
            id = overlayId,
            bitmap = bitmap,
            imageUri = imageUriString,
            annotationSessionId = annotationSessionId,
            scaleMode = settingsRepository.getPinScaleMode(),
            defaultShadowEnabled = appearanceSettings.shadowEnabled,
            defaultCornerRadiusDp = appearanceSettings.cornerRadiusDp,
            initialContentWidthPx = initialContentWidthPx,
            initialContentHeightPx = initialContentHeightPx,
            initialX = 120 + overlays.size * 36,
            initialY = 220 + overlays.size * 36,
            onInteracted = { markPinOverlayHintSeen() },
            onFocusRequested = {
                bringOverlayToFront(overlayId)
            },
            onEditRequested = {
                pinCreationCoordinator.startEditor(
                    context = this@PinOverlayService,
                    request = EditorLaunchRequest(
                        imageUri = imageUriString,
                        annotationSessionId = annotationSessionId
                    ),
                    launchInNewTask = true
                )
                removeOverlay(overlayId)
            },
            onCloseRequested = {
                recentPinRepository.push(
                    ClosedPinRecord(
                        imageUri = imageUriString,
                        annotationSessionId = annotationSessionId
                    )
                )
                removeOverlay(overlayId)
            }
        )

        if (controller.attach()) {
            overlays[overlayId] = controller
            recordPinHistoryIfNeeded(
                imageUri = imageUriString,
                annotationSessionId = annotationSessionId,
                sourceType = historySourceType,
                metadata = historyMetadata.copy(
                    widthPx = historyMetadata.widthPx ?: initialContentWidthPx ?: bitmap.width,
                    heightPx = historyMetadata.heightPx ?: initialContentHeightPx ?: bitmap.height
                )
            )
            controller.overlayView.post { showPinOverlayHintIfNeeded(controller) }
        }
        notifyManagerChanged()
    }

    private fun removeOverlay(overlayId: String) {
        if (pinHintAnchorOverlayId == overlayId) {
            dismissPinOverlayHint()
        }
        val controller = overlays.remove(overlayId) ?: return
        controller.remove()
        notifyManagerChanged()
        if (overlays.isEmpty() && managerView == null) {
            stopSelf()
        }
    }

    private fun showPinOverlayHintIfNeeded(controller: PinOverlayWindowController) {
        if (!onboardingGuideState.shouldShowPinOverlayHint() || pinHintView != null) {
            return
        }
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PinOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PinOverlayService)
            setContent {
                MudingTheme {
                    UsageHintBubble(
                        headline = "贴图提示",
                        messages = listOf(
                            "长按贴图，进入编辑页",
                            "双击贴图，快速关闭"
                        ),
                        onDismiss = { markPinOverlayHintSeen() }
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
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
            x = 0
            y = 0
        }
        pinHintView = view
        pinHintParams = params
        pinHintAnchorOverlayId = controller.id
        try {
            windowManager.addView(view, params)
            view.post { updatePinOverlayHintPosition(controller.id) }
        } catch (_: Exception) {
            dismissPinOverlayHint()
        }
    }

    private fun updatePinOverlayHintPosition(overlayId: String) {
        val view = pinHintView ?: return
        val params = pinHintParams ?: return
        val controller = overlays[overlayId] ?: return
        val hintWidth = view.width.takeIf { it > 0 } ?: view.measuredWidth
        val hintHeight = view.height.takeIf { it > 0 } ?: view.measuredHeight
        if (hintWidth <= 0 || hintHeight <= 0) {
            return
        }
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val position = UsageHintPositioning.place(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            anchor = UsageHintPositioning.AnchorBounds(
                left = controller.overlayParams.x,
                top = controller.overlayParams.y,
                right = controller.overlayParams.x + controller.overlayParams.width.coerceAtLeast(1),
                bottom = controller.overlayParams.y + controller.overlayParams.height.coerceAtLeast(1)
            ),
            hintWidth = hintWidth,
            hintHeight = hintHeight,
            margin = (12 * resources.displayMetrics.density).toInt().coerceAtLeast(12)
        )
        params.x = position.x
        params.y = position.y
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun markPinOverlayHintSeen() {
        if (onboardingGuideState.shouldShowPinOverlayHint()) {
            onboardingGuideState = onboardingGuideState.markPinOverlayHintSeen()
            settingsRepository.setPinOverlayHintSeen(true)
        }
        dismissPinOverlayHint()
    }

    private fun dismissPinOverlayHint() {
        pinHintView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (_: Exception) {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {
                }
            }
        }
        pinHintView = null
        pinHintParams = null
        pinHintAnchorOverlayId = null
    }

    private fun rememberClosedOverlay(controller: PinOverlayWindowController) {
        recentPinRepository.push(controller.toClosedPinRecord())
    }

    private fun recordPinHistoryIfNeeded(
        imageUri: String,
        annotationSessionId: String?,
        sourceType: PinHistorySourceType,
        metadata: PinHistoryMetadata
    ) {
        val pinHistorySettings = settingsRepository.getPinHistorySettings()
        if (!pinHistorySettings.enabled) return
        pinHistoryRepository.save(
            imageUri = imageUri,
            annotationSessionId = annotationSessionId,
            sourceType = sourceType,
            metadata = metadata
        )
        pinHistoryRepository.prune(
            maxCount = pinHistorySettings.maxCount,
            maxDays = pinHistorySettings.retainDays
        )
        notifyManagerChanged()
    }

    private fun setOverlayVisible(overlayId: String, visible: Boolean) {
        overlays[overlayId]?.setVisible(visible)
        notifyManagerChanged()
    }

    private fun setAllOverlaysVisible(visible: Boolean) {
        overlays.keys.toList().forEach { setOverlayVisible(it, visible) }
    }

    private fun closeOverlayFromManager(overlayId: String) {
        val controller = overlays[overlayId] ?: return
        rememberClosedOverlay(controller)
        removeOverlay(overlayId)
    }

    private fun closeAllOverlays() {
        overlays.keys.toList().forEach { overlayId ->
            val controller = overlays[overlayId] ?: return@forEach
            rememberClosedOverlay(controller)
            removeOverlay(overlayId)
        }
        closeManagerOverlay()
    }

    private fun bringOverlayToFront(overlayId: String) {
        val controller = overlays[overlayId] ?: return
        val currentTopId = overlays.keys.lastOrNull()
        if (currentTopId == overlayId) {
            return
        }
        controller.bringToFront()
        overlays.remove(overlayId)
        overlays[overlayId] = controller
        notifyManagerChanged()
    }

    private fun openManagerOverlay() {
        if (managerView != null) {
            notifyManagerChanged()
            return
        }
        val params = WindowManager.LayoutParams(
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
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 140
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PinOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PinOverlayService)
            setContent {
                val refreshToken = managerRefreshToken
                val snapshots = overlays.values.map { it.snapshot() }
                MudingTheme {
                    PinManagerContent(
                        refreshToken = refreshToken,
                        overlays = snapshots,
                        onShowAll = { setAllOverlaysVisible(true) },
                        onHideAll = { setAllOverlaysVisible(false) },
                        onCloseAll = { closeAllOverlays() },
                        onCloseManager = { closeManagerOverlay() },
                        onToggleVisible = { id ->
                            val current = overlays[id]?.visible ?: return@PinManagerContent
                            setOverlayVisible(id, !current)
                        },
                        onCloseOne = { id -> closeOverlayFromManager(id) },
                        onFocusOne = { id -> bringOverlayToFront(id) }
                    )
                }
            }
        }
        managerView = view
        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {
            managerView = null
        }
    }

    private fun closeManagerOverlay() {
        val view = managerView ?: return
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
        }
        managerView = null
        if (overlays.isEmpty()) {
            stopSelf()
        }
    }

    private fun notifyManagerChanged() {
        managerRefreshToken++
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        dismissPinOverlayHint()
        managerView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (_: Exception) {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {
                }
            }
        }
        managerView = null
        overlays.values.toList().forEach { controller -> controller.remove() }
        overlays.clear()
    }

    companion object {
        const val ACTION_RESTORE_LAST_CLOSED = "com.muding.android.action.RESTORE_LAST_CLOSED"
        const val ACTION_OPEN_MANAGER = "com.muding.android.action.OPEN_MANAGER"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_ANNOTATION_SESSION_ID = "extra_annotation_session_id"
        const val EXTRA_HISTORY_SOURCE = "extra_history_source"
        const val EXTRA_HISTORY_DISPLAY_NAME = "extra_history_display_name"
        const val EXTRA_HISTORY_TEXT_PREVIEW = "extra_history_text_preview"
        const val EXTRA_INITIAL_CONTENT_WIDTH_PX = "extra_initial_content_width_px"
        const val EXTRA_INITIAL_CONTENT_HEIGHT_PX = "extra_initial_content_height_px"
    }
}
