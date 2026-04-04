package com.muding.android.presentation.translation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.data.settings.CloudTranslationProvider
import com.muding.android.data.settings.TranslationSettings
import com.muding.android.feature.translation.LocalTranslationModelManager
import com.muding.android.feature.translation.TranslationLanguageCatalog
import com.muding.android.feature.translation.TranslationResult
import com.muding.android.feature.translation.displayName

interface LocalTranslationModelGateway {
    suspend fun getDownloadedLanguageTags(): Set<String>
    suspend fun download(languageTag: String, wifiOnly: Boolean)
    suspend fun delete(languageTag: String)
}

class DefaultLocalTranslationModelGateway(
    private val modelManager: LocalTranslationModelManager
) : LocalTranslationModelGateway {
    override suspend fun getDownloadedLanguageTags(): Set<String> {
        return modelManager.getDownloadedLanguageTags()
    }

    override suspend fun download(languageTag: String, wifiOnly: Boolean) {
        modelManager.download(languageTag, wifiOnly)
    }

    override suspend fun delete(languageTag: String) {
        modelManager.delete(languageTag)
    }
}

interface CloudTranslationVerifier {
    suspend fun verify(targetLanguageTag: String): TranslationResult
}

data class TranslationSettingsUiState(
    val targetLanguageTag: String,
    val localDownloadOnWifiOnly: Boolean,
    val downloadedLanguageTags: Set<String>,
    val cloudProvider: CloudTranslationProvider,
    val baiduAppIdDraft: String,
    val baiduSecretKeyDraft: String,
    val youdaoAppKeyDraft: String,
    val youdaoAppSecretDraft: String,
    val localBusy: Boolean = false,
    val cloudBusy: Boolean = false,
    val localMessage: String? = null,
    val cloudMessage: String? = null
) {
    val targetLanguageDisplayName: String
        get() = TranslationLanguageCatalog.findByAppTag(targetLanguageTag).displayName

    val isBuiltInLocalModel: Boolean
        get() = targetLanguageTag == "en"

    val isCurrentLocalModelDownloaded: Boolean
        get() = isBuiltInLocalModel || downloadedLanguageTags.contains(targetLanguageTag)

    val localModelStatusLabel: String
        get() = when {
            isBuiltInLocalModel -> "内置可用"
            isCurrentLocalModelDownloaded -> "已下载"
            else -> "未下载"
        }

    val localModelActionLabel: String?
        get() = when {
            isBuiltInLocalModel -> null
            isCurrentLocalModelDownloaded -> "删除当前模型"
            else -> "下载当前模型"
        }

    val selectedCloudProviderLabel: String
        get() = cloudProvider.displayName()

    val showCloudCredentials: Boolean
        get() = cloudProvider != CloudTranslationProvider.NONE

    val showYoudaoCredentials: Boolean
        get() = cloudProvider == CloudTranslationProvider.YOUDAO

    val showBaiduCredentials: Boolean
        get() = cloudProvider == CloudTranslationProvider.BAIDU

    val showCloudVerificationAction: Boolean
        get() = showCloudCredentials

    val isCloudCredentialDraftComplete: Boolean
        get() = when (cloudProvider) {
            CloudTranslationProvider.NONE -> false
            CloudTranslationProvider.BAIDU -> baiduAppIdDraft.isNotBlank() && baiduSecretKeyDraft.isNotBlank()
            CloudTranslationProvider.YOUDAO -> youdaoAppKeyDraft.isNotBlank() && youdaoAppSecretDraft.isNotBlank()
        }
}

class TranslationSettingsViewModel(
    private val settingsRepository: AppSettingsRepository,
    private val localModelGateway: LocalTranslationModelGateway,
    private val cloudVerifier: CloudTranslationVerifier
) : ViewModel() {

    var uiState by mutableStateOf(settingsRepository.getTranslationSettings().toUiState())
        private set

    suspend fun refreshDownloadedModels() {
        uiState = uiState.copy(
            downloadedLanguageTags = localModelGateway.getDownloadedLanguageTags()
        )
    }

    fun selectTargetLanguage(languageTag: String) {
        settingsRepository.setLocalTranslationTargetLanguageTag(languageTag)
        uiState = uiState.copy(
            targetLanguageTag = languageTag,
            localMessage = null,
            cloudMessage = null
        )
    }

    fun setLocalDownloadOnWifiOnly(enabled: Boolean) {
        settingsRepository.setLocalTranslationDownloadOnWifiOnly(enabled)
        uiState = uiState.copy(
            localDownloadOnWifiOnly = enabled,
            localMessage = null
        )
    }

    fun selectCloudProvider(provider: CloudTranslationProvider) {
        settingsRepository.setCloudTranslationProvider(provider)
        uiState = uiState.copy(
            cloudProvider = provider,
            cloudMessage = null
        )
    }

    fun updateBaiduAppId(value: String) {
        uiState = uiState.copy(baiduAppIdDraft = value, cloudMessage = null)
    }

    fun updateBaiduSecretKey(value: String) {
        uiState = uiState.copy(baiduSecretKeyDraft = value, cloudMessage = null)
    }

    fun updateYoudaoAppKey(value: String) {
        uiState = uiState.copy(youdaoAppKeyDraft = value, cloudMessage = null)
    }

    fun updateYoudaoAppSecret(value: String) {
        uiState = uiState.copy(youdaoAppSecretDraft = value, cloudMessage = null)
    }

    suspend fun performCurrentLocalModelAction() {
        val actionLabel = uiState.localModelActionLabel ?: return
        uiState = uiState.copy(localBusy = true, localMessage = null)
        val languageTag = uiState.targetLanguageTag
        val wifiOnly = uiState.localDownloadOnWifiOnly
        runCatching {
            if (actionLabel == "删除当前模型") {
                localModelGateway.delete(languageTag)
            } else {
                localModelGateway.download(languageTag, wifiOnly)
            }
            localModelGateway.getDownloadedLanguageTags()
        }.onSuccess { downloadedTags ->
            uiState = uiState.copy(
                downloadedLanguageTags = downloadedTags,
                localBusy = false,
                localMessage = if (actionLabel == "删除当前模型") {
                    "当前模型已删除"
                } else {
                    "当前模型已下载"
                }
            )
        }.onFailure { throwable ->
            uiState = uiState.copy(
                localBusy = false,
                localMessage = throwable.message ?: "模型操作失败"
            )
        }
    }

    suspend fun saveAndVerifyCurrentProvider() {
        if (!uiState.showCloudVerificationAction) {
            return
        }
        uiState = uiState.copy(cloudBusy = true, cloudMessage = null)
        persistCredentialDrafts()
        runCatching {
            cloudVerifier.verify(uiState.targetLanguageTag)
        }.onSuccess { result ->
            uiState = uiState.copy(
                cloudBusy = false,
                cloudMessage = "${result.providerLabel}已保存并验证"
            )
        }.onFailure { throwable ->
            uiState = uiState.copy(
                cloudBusy = false,
                cloudMessage = throwable.message ?: "验证失败"
            )
        }
    }

    private fun persistCredentialDrafts() {
        when (uiState.cloudProvider) {
            CloudTranslationProvider.BAIDU -> settingsRepository.setBaiduTranslationCredentials(
                appId = uiState.baiduAppIdDraft,
                secretKey = uiState.baiduSecretKeyDraft
            )

            CloudTranslationProvider.YOUDAO -> settingsRepository.setYoudaoTranslationCredentials(
                appKey = uiState.youdaoAppKeyDraft,
                appSecret = uiState.youdaoAppSecretDraft
            )

            CloudTranslationProvider.NONE -> Unit
        }
    }
}

private fun TranslationSettings.toUiState(): TranslationSettingsUiState {
    return TranslationSettingsUiState(
        targetLanguageTag = localTargetLanguageTag,
        localDownloadOnWifiOnly = localDownloadOnWifiOnly,
        downloadedLanguageTags = emptySet(),
        cloudProvider = cloudProvider,
        baiduAppIdDraft = baiduAppId,
        baiduSecretKeyDraft = baiduSecretKey,
        youdaoAppKeyDraft = youdaoAppKey,
        youdaoAppSecretDraft = youdaoAppSecret
    )
}
