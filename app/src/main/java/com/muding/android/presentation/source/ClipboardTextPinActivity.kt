package com.muding.android.presentation.source

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.muding.android.app.AppGraph
import com.muding.android.core.model.PinSourceType
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.feature.pin.creation.PinCreationCoordinator
import com.muding.android.service.FloatingBallService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClipboardTextPinActivity : ComponentActivity() {

    private lateinit var pinCreationCoordinator: PinCreationCoordinator
    private var handled = false
    private val finishToBackground: Boolean
        get() = intent.getBooleanExtra(EXTRA_FINISH_TO_BACKGROUND, false)
    private val restoreFloatingBall: Boolean
        get() = intent.getBooleanExtra(EXTRA_RESTORE_FLOATING_BALL, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)
    }

    override fun onPostResume() {
        super.onPostResume()
        if (handled) {
            return
        }
        handled = true
        processClipboardText()
    }

    private fun processClipboardText() {
        lifecycleScope.launch {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = awaitClipboardText(clipboardManager)
            if (text.isNullOrBlank()) {
                Toast.makeText(
                    this@ClipboardTextPinActivity,
                    "剪贴板中没有可用文本",
                    Toast.LENGTH_SHORT
                ).show()
                finishFlow()
                return@launch
            }
            try {
                pinCreationCoordinator.launchFromSource(
                    context = this@ClipboardTextPinActivity,
                    source = pinCreationCoordinator.createTextSource(
                        sourceType = PinSourceType.CLIPBOARD_TEXT,
                        text = text
                    ),
                    forcedResultAction = CaptureResultAction.PIN_DIRECTLY
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@ClipboardTextPinActivity,
                    "创建文字贴图失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                finishFlow()
            }
        }
    }

    private suspend fun awaitClipboardText(clipboardManager: ClipboardManager): String? {
        repeat(3) { attempt ->
            readClipboardText(clipboardManager)?.let { return it }
            if (attempt < 2) {
                delay(120)
            }
        }
        return null
    }

    private fun readClipboardText(clipboardManager: ClipboardManager): String? {
        if (!clipboardManager.hasPrimaryClip()) {
            return null
        }
        val clip = clipboardManager.primaryClip ?: return null
        for (index in 0 until clip.itemCount) {
            val item = clip.getItemAt(index)
            val directText = item.text?.toString()?.trim()
            if (!directText.isNullOrBlank()) {
                return directText
            }
            val coercedText = item.coerceToText(this)?.toString()?.trim()
            if (!coercedText.isNullOrBlank()) {
                return coercedText
            }
        }
        return null
    }

    private fun finishFlow() {
        if (restoreFloatingBall) {
            startService(
                FloatingBallService.createRestoreVisibilityIntent(this)
            )
        }
        if (finishToBackground) {
            moveTaskToBack(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                finish()
            }
            return
        }
        finish()
    }

    companion object {
        const val EXTRA_FINISH_TO_BACKGROUND = "extra_finish_to_background"
        const val EXTRA_RESTORE_FLOATING_BALL = "extra_restore_floating_ball"
    }
}
