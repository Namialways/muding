package com.muding.android.presentation.translation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muding.android.app.AppGraph
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.data.settings.CloudTranslationProvider
import com.muding.android.feature.translation.LocalTranslationModelManager
import com.muding.android.feature.translation.TranslationEngine
import com.muding.android.feature.translation.TranslationLanguageCatalog
import com.muding.android.feature.translation.TranslationResult
import com.muding.android.feature.translation.displayName
import com.muding.android.presentation.main.SectionHeader
import com.muding.android.presentation.main.SettingGroup
import com.muding.android.presentation.main.rememberMainUiTokens
import com.muding.android.presentation.theme.MudingTheme
import kotlinx.coroutines.launch

class TranslationSettingsActivity : ComponentActivity() {

    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var modelManager: LocalTranslationModelManager
    private lateinit var cloudTranslationEngine: TranslationEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = AppGraph.appSettingsRepository(this)
        modelManager = AppGraph.localTranslationModelManager()
        cloudTranslationEngine = AppGraph.cloudTranslationEngine(this)

        setContent {
            MudingTheme {
                val factory = remember {
                    TranslationSettingsViewModelFactory(
                        settingsRepository = settingsRepository,
                        localModelGateway = DefaultLocalTranslationModelGateway(modelManager),
                        cloudVerifier = SettingsBackedCloudTranslationVerifier(cloudTranslationEngine)
                    )
                }
                val viewModel: TranslationSettingsViewModel = viewModel(factory = factory)
                TranslationSettingsRoute(
                    viewModel = viewModel,
                    onClose = { finish() }
                )
            }
        }
    }
}

private class TranslationSettingsViewModelFactory(
    private val settingsRepository: AppSettingsRepository,
    private val localModelGateway: LocalTranslationModelGateway,
    private val cloudVerifier: CloudTranslationVerifier
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TranslationSettingsViewModel::class.java)) {
            return TranslationSettingsViewModel(
                settingsRepository = settingsRepository,
                localModelGateway = localModelGateway,
                cloudVerifier = cloudVerifier
            ) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}

private class SettingsBackedCloudTranslationVerifier(
    private val translationEngine: TranslationEngine
) : CloudTranslationVerifier {
    override suspend fun verify(targetLanguageTag: String): TranslationResult {
        return translationEngine.translate("hello", targetLanguageTag)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationSettingsRoute(
    viewModel: TranslationSettingsViewModel,
    onClose: () -> Unit
) {
    val uiState = viewModel.uiState
    val tokens = rememberMainUiTokens()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshDownloadedModels()
    }

    Scaffold(
        containerColor = tokens.palette.pageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("翻译设置") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = tokens.palette.pageBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = tokens.spacing.pageGutter),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                SectionHeader(
                    title = "翻译设置",
                    description = "设置目标语言、本地模型和云翻译服务。"
                )
            }

            item {
                LocalTranslationGroup(
                    uiState = uiState,
                    onSelectLanguage = viewModel::selectTargetLanguage,
                    onWifiOnlyChanged = viewModel::setLocalDownloadOnWifiOnly,
                    onRunCurrentModelAction = {
                        scope.launch { viewModel.performCurrentLocalModelAction() }
                    }
                )
            }

            item {
                CloudTranslationGroup(
                    uiState = uiState,
                    onSelectProvider = viewModel::selectCloudProvider,
                    onBaiduAppIdChange = viewModel::updateBaiduAppId,
                    onBaiduSecretKeyChange = viewModel::updateBaiduSecretKey,
                    onYoudaoAppKeyChange = viewModel::updateYoudaoAppKey,
                    onYoudaoAppSecretChange = viewModel::updateYoudaoAppSecret,
                    onSaveAndVerify = {
                        scope.launch { viewModel.saveAndVerifyCurrentProvider() }
                    }
                )
            }
        }
    }
}

@Composable
private fun LocalTranslationGroup(
    uiState: TranslationSettingsUiState,
    onSelectLanguage: (String) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onRunCurrentModelAction: () -> Unit
) {
    SettingGroup(title = "本地翻译") {
        DropdownSettingField(
            label = "目标语言",
            value = uiState.targetLanguageDisplayName,
            options = TranslationLanguageCatalog.options.map { it.displayName to it.appTag },
            onSelect = onSelectLanguage
        )
        SwitchSettingRow(
            title = "仅在 Wi-Fi 下下载",
            checked = uiState.localDownloadOnWifiOnly,
            onCheckedChange = onWifiOnlyChanged
        )
        StatusRow(
            label = "模型状态",
            value = uiState.localModelStatusLabel
        )
        uiState.localModelActionLabel?.let { actionLabel ->
            if (actionLabel == "删除当前模型") {
                OutlinedButton(
                    onClick = onRunCurrentModelAction,
                    enabled = !uiState.localBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.localBusy) "处理中..." else actionLabel)
                }
            } else {
                Button(
                    onClick = onRunCurrentModelAction,
                    enabled = !uiState.localBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.localBusy) "处理中..." else actionLabel)
                }
            }
        }
        uiState.localMessage?.let { message ->
            StatusMessage(message = message)
        }
    }
}

@Composable
private fun CloudTranslationGroup(
    uiState: TranslationSettingsUiState,
    onSelectProvider: (CloudTranslationProvider) -> Unit,
    onBaiduAppIdChange: (String) -> Unit,
    onBaiduSecretKeyChange: (String) -> Unit,
    onYoudaoAppKeyChange: (String) -> Unit,
    onYoudaoAppSecretChange: (String) -> Unit,
    onSaveAndVerify: () -> Unit
) {
    SettingGroup(title = "云翻译") {
        DropdownSettingField(
            label = "翻译服务",
            value = uiState.selectedCloudProviderLabel,
            options = CloudTranslationProvider.entries.map { it.displayName() to it },
            onSelect = onSelectProvider
        )

        if (uiState.showYoudaoCredentials) {
            OutlinedTextField(
                value = uiState.youdaoAppKeyDraft,
                onValueChange = onYoudaoAppKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("有道 App Key") },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.youdaoAppSecretDraft,
                onValueChange = onYoudaoAppSecretChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("有道 App Secret") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        }

        if (uiState.showBaiduCredentials) {
            OutlinedTextField(
                value = uiState.baiduAppIdDraft,
                onValueChange = onBaiduAppIdChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("百度 App ID") },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.baiduSecretKeyDraft,
                onValueChange = onBaiduSecretKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("百度 Secret Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        }

        if (uiState.showCloudVerificationAction) {
            Button(
                onClick = onSaveAndVerify,
                enabled = !uiState.cloudBusy && uiState.isCloudCredentialDraftComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.cloudBusy) "验证中..." else "保存并验证")
            }
        } else {
            StatusMessage(message = "当前仅使用本地翻译。")
        }

        uiState.cloudMessage?.let { message ->
            StatusMessage(message = message)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSettingField(
    label: String,
    value: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (optionLabel, optionValue) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        expanded = false
                        onSelect(optionValue)
                    }
                )
            }
        }
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = rememberMainUiTokens().palette.body
        )
    }
}

@Composable
private fun StatusMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = rememberMainUiTokens().palette.body
    )
}
