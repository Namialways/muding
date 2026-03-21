package com.pixpin.android.presentation.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pixpin.android.app.AppGraph
import com.pixpin.android.core.model.PinSourceType
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.feature.pin.creation.PinCreationCoordinator
import com.pixpin.android.presentation.theme.PixPinTheme
import com.pixpin.android.service.FloatingBallService
import kotlinx.coroutines.launch

class OcrResultActivity : ComponentActivity() {

    private lateinit var pinCreationCoordinator: PinCreationCoordinator
    private val finishToBackground: Boolean
        get() = intent.getBooleanExtra(EXTRA_FINISH_TO_BACKGROUND, false)
    private val restoreFloatingBall: Boolean
        get() = intent.getBooleanExtra(EXTRA_RESTORE_FLOATING_BALL, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)
        val initialText = intent.getStringExtra(EXTRA_RECOGNIZED_TEXT).orEmpty()
        if (initialText.isBlank()) {
            Toast.makeText(this, "OCR 结果为空", Toast.LENGTH_SHORT).show()
            finishFlow()
            return
        }

        setContent {
            PixPinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OcrResultScreen(
                        initialText = initialText,
                        onCreateTextPin = { text -> createTextPin(text) },
                        onCopyText = { text -> copyText(text) },
                        onClose = { finishFlow() }
                    )
                }
            }
        }
    }

    private fun createTextPin(text: String) {
        lifecycleScope.launch {
            try {
                pinCreationCoordinator.launchFromSource(
                    context = this@OcrResultActivity,
                    source = pinCreationCoordinator.createTextSource(
                        sourceType = PinSourceType.OCR_TEXT,
                        text = text
                    ),
                    forcedResultAction = CaptureResultAction.PIN_DIRECTLY
                )
                finishFlow()
            } catch (e: Exception) {
                Toast.makeText(
                    this@OcrResultActivity,
                    "创建 OCR 文字贴图失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun copyText(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(
            ClipData.newPlainText("ocr_text", text)
        )
        Toast.makeText(this, "已复制 OCR 文本", Toast.LENGTH_SHORT).show()
    }

    private fun finishFlow() {
        if (restoreFloatingBall) {
            startService(
                FloatingBallService.createRestoreVisibilityIntent(this)
            )
        }
        if (finishToBackground) {
            moveTaskToBack(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                finish()
            }
            return
        }
        finish()
    }

    companion object {
        const val EXTRA_RECOGNIZED_TEXT = "extra_recognized_text"
        const val EXTRA_FINISH_TO_BACKGROUND = "extra_finish_to_background"
        const val EXTRA_RESTORE_FLOATING_BALL = "extra_restore_floating_ball"
    }
}

@Composable
private fun OcrResultScreen(
    initialText: String,
    onCreateTextPin: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onClose: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "OCR 结果",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "可以先修改识别结果，再决定复制文本或生成文字贴图。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            label = { Text("识别文本") }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onCreateTextPin(text.trim()) },
                modifier = Modifier.weight(1f),
                enabled = text.isNotBlank()
            ) {
                Text("生成文字贴图")
            }
            OutlinedButton(
                onClick = { onCopyText(text.trim()) },
                modifier = Modifier.weight(1f),
                enabled = text.isNotBlank()
            ) {
                Text("复制文本")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("关闭")
        }
    }
}
