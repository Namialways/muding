package com.muding.android.presentation.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.muding.android.app.AppGraph
import com.muding.android.core.model.PinSourceType
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.data.settings.CloudTranslationProvider
import com.muding.android.data.settings.TranslationSettings
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.feature.pin.creation.PinCreationCoordinator
import com.muding.android.feature.translation.TranslationEngine
import com.muding.android.feature.translation.TranslationErrorMessages
import com.muding.android.feature.translation.TranslationLanguageCatalog
import com.muding.android.feature.translation.TranslationLanguageOption
import com.muding.android.feature.translation.TranslationResult
import com.muding.android.presentation.theme.MudingTheme
import com.muding.android.service.FloatingBallService
import kotlinx.coroutines.launch

class OcrResultActivity : ComponentActivity() {

    private lateinit var pinCreationCoordinator: PinCreationCoordinator
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var localTranslationEngine: TranslationEngine
    private lateinit var cloudTranslationEngine: TranslationEngine
    private val finishToBackground: Boolean
        get() = intent.getBooleanExtra(EXTRA_FINISH_TO_BACKGROUND, false)
    private val restoreFloatingBall: Boolean
        get() = intent.getBooleanExtra(EXTRA_RESTORE_FLOATING_BALL, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)
        settingsRepository = AppGraph.appSettingsRepository(this)
        localTranslationEngine = AppGraph.localTranslationEngine()
        cloudTranslationEngine = AppGraph.cloudTranslationEngine(this)
        val initialText = intent.getStringExtra(EXTRA_RECOGNIZED_TEXT).orEmpty()
        val initialTargetLanguageTag = settingsRepository.getTranslationSettings().localTargetLanguageTag

        if (initialText.isBlank()) {
            Toast.makeText(this, "OCR 结果为空", Toast.LENGTH_SHORT).show()
            finishFlow()
            return
        }

        setContent {
            MudingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OcrResultScreen(
                        initialText = initialText,
                        initialTargetLanguageTag = initialTargetLanguageTag,
                        onCreateTextPin = { text -> createTextPin(text) },
                        onCopyText = { text -> copyText(text) },
                        onTranslate = { text, targetLanguageTag, onComplete ->
                            val settings = settingsRepository.getTranslationSettings()
                            val translationEngine = resolveOcrTranslationEngine(
                                settings = settings,
                                localEngine = localTranslationEngine,
                                cloudEngine = cloudTranslationEngine
                            )
                            translateWithEngine(text, targetLanguageTag, translationEngine, onComplete)
                        },
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
                    "创建 OCR 文字贴图失败：${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun copyText(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("ocr_text", text))
        Toast.makeText(this, "已复制 OCR 文本", Toast.LENGTH_SHORT).show()
    }

    private fun translateWithEngine(
        text: String,
        targetLanguageTag: String,
        engine: TranslationEngine,
        onComplete: (String) -> Unit
    ) {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) {
            Toast.makeText(this, "没有可翻译的文本", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val result = translateOcrTextWithTarget(normalizedText, targetLanguageTag, engine)
                onComplete(result.translatedText)
                Toast.makeText(
                    this@OcrResultActivity,
                    "${result.providerLabel}已完成",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@OcrResultActivity,
                    TranslationErrorMessages.resolve(e),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun finishFlow() {
        if (restoreFloatingBall) {
            startService(FloatingBallService.createRestoreVisibilityIntent(this))
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

internal fun resolveOcrTranslationEngine(
    settings: TranslationSettings,
    localEngine: TranslationEngine,
    cloudEngine: TranslationEngine
): TranslationEngine {
    return if (settings.cloudProvider == CloudTranslationProvider.NONE) {
        localEngine
    } else {
        cloudEngine
    }
}

internal suspend fun translateOcrTextWithTarget(
    text: String,
    targetLanguageTag: String,
    engine: TranslationEngine
): TranslationResult {
    return engine.translate(text.trim(), targetLanguageTag)
}

@Composable
private fun OcrResultScreen(
    initialText: String,
    initialTargetLanguageTag: String,
    onCreateTextPin: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onTranslate: (String, String, (String) -> Unit) -> Unit,
    onClose: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var translatedText by remember { mutableStateOf("") }
    var targetLanguageTag by remember(initialTargetLanguageTag) {
        mutableStateOf(TranslationLanguageCatalog.findByAppTag(initialTargetLanguageTag).appTag)
    }
    val targetLanguage = TranslationLanguageCatalog.findByAppTag(targetLanguageTag)

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
            text = "可先修正识别文字，再生成贴图或执行翻译。",
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
        Button(
            onClick = { onTranslate(text.trim(), targetLanguageTag) { translatedText = it } },
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank()
        ) {
            Text("翻译")
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TranslationResultHeader(
                selectedLanguage = targetLanguage,
                onLanguageSelected = { option ->
                    targetLanguageTag = option.appTag
                }
            )
            OutlinedTextField(
                value = translatedText,
                onValueChange = { translatedText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                label = { Text("翻译结果") }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationResultHeader(
    selectedLanguage: TranslationLanguageOption,
    onLanguageSelected: (TranslationLanguageOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "翻译结果",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Box(modifier = Modifier.widthIn(min = 132.dp, max = 180.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedLanguage.displayName,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    singleLine = true,
                    label = { Text("目标语言") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    TranslationLanguageCatalog.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                expanded = false
                                onLanguageSelected(option)
                            }
                        )
                    }
                }
            }
        }
    }
}
