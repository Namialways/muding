package com.pixpin.android.presentation.source

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.pixpin.android.app.AppGraph
import com.pixpin.android.core.model.PinSourceType
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.feature.pin.creation.PinCreationCoordinator
import com.pixpin.android.service.FloatingBallService
import kotlinx.coroutines.launch

class ClipboardTextPinActivity : ComponentActivity() {

    private lateinit var pinCreationCoordinator: PinCreationCoordinator
    private val finishToBackground: Boolean
        get() = intent.getBooleanExtra(EXTRA_FINISH_TO_BACKGROUND, false)
    private val restoreFloatingBall: Boolean
        get() = intent.getBooleanExtra(EXTRA_RESTORE_FLOATING_BALL, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboardManager.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(this)
            ?.toString()
            ?.trim()
            .orEmpty()

        if (text.isBlank()) {
            Toast.makeText(this, "Clipboard has no text content", Toast.LENGTH_SHORT).show()
            finishFlow()
            return
        }

        lifecycleScope.launch {
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
                Toast.makeText(this@ClipboardTextPinActivity, "Create text pin failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                finishFlow()
            }
        }
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
