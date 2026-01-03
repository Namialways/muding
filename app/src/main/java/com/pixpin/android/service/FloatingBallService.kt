package com.pixpin.android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.pixpin.android.MainActivity
import com.pixpin.android.R
import android.app.Activity
import androidx.lifecycle.lifecycleScope
import com.pixpin.android.domain.usecase.ScreenshotManager
import com.pixpin.android.presentation.editor.AnnotationEditorActivity
import com.pixpin.android.presentation.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class FloatingBallService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var floatingView: ComposeView? = null
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val lifecycleRegistry = LifecycleRegistry(this)

    private lateinit var screenshotManager: ScreenshotManager

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        screenshotManager = ScreenshotManager(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        showFloatingBall()
        
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_SCREENSHOT) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                screenshotManager.initMediaProjection(resultCode, resultData)
                lifecycleScope.launch {
                    try {
                        val bitmap = screenshotManager.captureScreen()
                        val uri = writeBitmapToCache(bitmap)

                        val editorIntent = Intent(this@FloatingBallService, AnnotationEditorActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra(AnnotationEditorActivity.EXTRA_IMAGE_URI, uri.toString())
                        }
                        startActivity(editorIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        // 无论成功与否，都释放 MediaProjection，以结束“分享屏幕”状态
                        screenshotManager.release()
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showFloatingBall() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingBallService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBallService)
            
            setContent {
                PixPinTheme {
                    FloatingBallContent(
                        onScreenshot = { handleScreenshot() },
                        onSettings = { openSettings() },
                        onExit = { stopSelf() },
                        onPositionChange = { offsetX, offsetY ->
                            params.x = offsetX.roundToInt()
                            params.y = offsetY.roundToInt()
                            try {
                                windowManager.updateViewLayout(this, params)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleScreenshot() {
        // Android 10+ 要求 MediaProjection 必须在声明了 mediaProjection 类型的前台服务中使用。
        // 这里我们先拉起一个透明 Activity 获取授权结果，然后把 resultCode/data 回传给本 Service。
        val intent = Intent(this, ScreenshotPermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PixPin 服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "PixPin 悬浮球服务通知"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
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

    private fun writeBitmapToCache(bitmap: android.graphics.Bitmap): android.net.Uri {
        val cacheDir = File(cacheDir, "screenshots")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, "capture_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        // 复用你现有的 FileProvider
        return androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        try {
            screenshotManager.release()
        } catch (_: Exception) {
        }
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        floatingView = null
    }

    companion object {
        private const val CHANNEL_ID = "floating_ball_service"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START_SCREENSHOT = "com.pixpin.android.action.START_SCREENSHOT"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
    }
}

@Composable
fun FloatingBallContent(
    onScreenshot: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    onPositionChange: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isExpanded = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        onPositionChange(offsetX, offsetY)
                    }
                )
            }
    ) {
        // 悬浮球主按钮
        FloatingBall(
            isExpanded = isExpanded,
            onClick = {
                if (!isExpanded) {
                    isExpanded = true
                } else {
                    onScreenshot()
                }
            }
        )

        // 展开的菜单
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FloatingMenu(
                onScreenshot = {
                    isExpanded = false
                    onScreenshot()
                },
                onSettings = {
                    isExpanded = false
                    onSettings()
                },
                onExit = {
                    isExpanded = false
                    onExit()
                },
                onDismiss = { isExpanded = false }
           )
        }
    }
}

@Composable
fun FloatingBall(isExpanded: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .size(if (isExpanded) 200.dp else 60.dp)
            .shadow(8.dp, CircleShape),
        shape = CircleShape,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            FloatingBallGradientStart,
                            FloatingBallGradientEnd
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Screenshot",
                tint = Color.White,
                modifier = Modifier.size(if (isExpanded) 80.dp else 32.dp)
            )
        }
    }
}

@Composable
fun FloatingMenu(
    onScreenshot: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
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
fun MenuButton(
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
