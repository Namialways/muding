package com.pixpin.android.presentation.translation

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixpin.android.app.AppGraph
import com.pixpin.android.data.settings.AppSettingsRepository
import com.pixpin.android.data.settings.CloudTranslationProvider
import com.pixpin.android.data.settings.TranslationSettings
import com.pixpin.android.feature.translation.LocalTranslationModelManager
import com.pixpin.android.feature.translation.TranslationLanguageCatalog
import com.pixpin.android.feature.translation.displayName
import com.pixpin.android.presentation.theme.PixPinTheme
import kotlinx.coroutines.launch

class TranslationSettingsActivity : ComponentActivity() {

    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var modelManager: LocalTranslationModelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = AppGraph.appSettingsRepository(this)
        modelManager = AppGraph.localTranslationModelManager()

        setContent {
            PixPinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TranslationSettingsScreen(
                        initialSettings = settingsRepository.getTranslationSettings(),
                        modelManager = modelManager,
                        onSave = { settings ->
                            persistSettings(settings)
                            Toast.makeText(this, "翻译设置已保存", Toast.LENGTH_SHORT).show()
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }

    private fun persistSettings(settings: TranslationSettings) {
        settingsRepository.setLocalTranslationTargetLanguageTag(settings.localTargetLanguageTag)
        settingsRepository.setLocalTranslationDownloadOnWifiOnly(settings.localDownloadOnWifiOnly)
        settingsRepository.setCloudTranslationProvider(settings.cloudProvider)
        settingsRepository.setBaiduTranslationCredentials(
            appId = settings.baiduAppId,
            secretKey = settings.baiduSecretKey
        )
        settingsRepository.setYoudaoTranslationCredentials(
            appKey = settings.youdaoAppKey,
            appSecret = settings.youdaoAppSecret
        )
    }
}

@Composable
private fun TranslationSettingsScreen(
    initialSettings: TranslationSettings,
    modelManager: LocalTranslationModelManager,
    onSave: (TranslationSettings) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var targetLanguageTag by remember { mutableStateOf(initialSettings.localTargetLanguageTag) }
    var wifiOnly by remember { mutableStateOf(initialSettings.localDownloadOnWifiOnly) }
    var cloudProvider by remember { mutableStateOf(initialSettings.cloudProvider) }
    var baiduAppId by remember { mutableStateOf(initialSettings.baiduAppId) }
    var baiduSecretKey by remember { mutableStateOf(initialSettings.baiduSecretKey) }
    var youdaoAppKey by remember { mutableStateOf(initialSettings.youdaoAppKey) }
    var youdaoAppSecret by remember { mutableStateOf(initialSettings.youdaoAppSecret) }
    var downloadedModels by remember { mutableStateOf(emptySet<String>()) }
    var localBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun currentSettings(): TranslationSettings {
        return TranslationSettings(
            localTargetLanguageTag = targetLanguageTag,
            localDownloadOnWifiOnly = wifiOnly,
            cloudProvider = cloudProvider,
            baiduAppId = baiduAppId,
            baiduSecretKey = baiduSecretKey,
            youdaoAppKey = youdaoAppKey,
            youdaoAppSecret = youdaoAppSecret
        )
    }

    suspend fun refreshDownloaded() {
        downloadedModels = modelManager.getDownloadedLanguageTags()
    }

    LaunchedEffect(Unit) {
        refreshDownloaded()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("翻译设置", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("本地翻译模型", style = MaterialTheme.typography.titleMedium)
                TranslationLanguageCatalog.options.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = targetLanguageTag == option.appTag,
                            onClick = { targetLanguageTag = option.appTag }
                        )
                        Text(option.displayName)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("仅在 Wi‑Fi 下下载", modifier = Modifier.weight(1f))
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = { wifiOnly = it }
                    )
                }
                Text(
                    text = "已下载模型：${
                        if (downloadedModels.isEmpty()) {
                            "无"
                        } else {
                            downloadedModels.joinToString {
                                TranslationLanguageCatalog.findByAppTag(it).displayName
                            }
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            localBusy = true
                            scope.launch {
                                runCatching {
                                    modelManager.download(targetLanguageTag, wifiOnly)
                                    refreshDownloaded()
                                    message = "本地翻译模型下载完成"
                                }.onFailure {
                                    message = "下载失败：${it.message}"
                                }
                                localBusy = false
                            }
                        },
                        enabled = !localBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("下载当前模型")
                    }
                    OutlinedButton(
                        onClick = {
                            localBusy = true
                            scope.launch {
                                runCatching {
                                    modelManager.delete(targetLanguageTag)
                                    refreshDownloaded()
                                    message = "本地翻译模型已删除"
                                }.onFailure {
                                    message = "删除失败：${it.message}"
                                }
                                localBusy = false
                            }
                        },
                        enabled = !localBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("删除当前模型")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("云翻译服务", style = MaterialTheme.typography.titleMedium)
                CloudTranslationProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = cloudProvider == provider,
                            onClick = { cloudProvider = provider }
                        )
                        Text(provider.displayName())
                    }
                }
                OutlinedTextField(
                    value = baiduAppId,
                    onValueChange = { baiduAppId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("百度 AppId") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = baiduSecretKey,
                    onValueChange = { baiduSecretKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("百度 SecretKey") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = youdaoAppKey,
                    onValueChange = { youdaoAppKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("有道 AppKey") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = youdaoAppSecret,
                    onValueChange = { youdaoAppSecret = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("有道 AppSecret") },
                    singleLine = true
                )
            }
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onSave(currentSettings()) },
                modifier = Modifier.weight(1f)
            ) {
                Text("保存")
            }
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("关闭")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
