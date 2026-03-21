package com.pixpin.android.feature.translation

data class TranslationCredentialBundle(
    val baiduAppId: String,
    val baiduSecretKey: String,
    val youdaoAppKey: String,
    val youdaoAppSecret: String
)

interface SecretStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

interface LegacyTranslationSecretSource {
    fun readBaiduAppId(): String
    fun readBaiduSecretKey(): String
    fun readYoudaoAppKey(): String
    fun readYoudaoAppSecret(): String
    fun clearLegacySecrets()
}

class TranslationCredentialStore(
    private val secretStore: SecretStore
) {

    fun getCredentials(): TranslationCredentialBundle {
        return TranslationCredentialBundle(
            baiduAppId = secretStore.get(KEY_BAIDU_APP_ID).orEmpty(),
            baiduSecretKey = secretStore.get(KEY_BAIDU_SECRET_KEY).orEmpty(),
            youdaoAppKey = secretStore.get(KEY_YOUDAO_APP_KEY).orEmpty(),
            youdaoAppSecret = secretStore.get(KEY_YOUDAO_APP_SECRET).orEmpty()
        )
    }

    fun saveBaiduCredentials(appId: String, secretKey: String) {
        putOrRemove(KEY_BAIDU_APP_ID, appId)
        putOrRemove(KEY_BAIDU_SECRET_KEY, secretKey)
    }

    fun saveYoudaoCredentials(appKey: String, appSecret: String) {
        putOrRemove(KEY_YOUDAO_APP_KEY, appKey)
        putOrRemove(KEY_YOUDAO_APP_SECRET, appSecret)
    }

    fun migrateFromLegacy(source: LegacyTranslationSecretSource) {
        val existing = getCredentials()
        if (
            existing.baiduAppId.isNotBlank() ||
            existing.baiduSecretKey.isNotBlank() ||
            existing.youdaoAppKey.isNotBlank() ||
            existing.youdaoAppSecret.isNotBlank()
        ) {
            source.clearLegacySecrets()
            return
        }

        val legacyBaiduAppId = source.readBaiduAppId()
        val legacyBaiduSecretKey = source.readBaiduSecretKey()
        val legacyYoudaoAppKey = source.readYoudaoAppKey()
        val legacyYoudaoAppSecret = source.readYoudaoAppSecret()
        if (
            legacyBaiduAppId.isBlank() &&
            legacyBaiduSecretKey.isBlank() &&
            legacyYoudaoAppKey.isBlank() &&
            legacyYoudaoAppSecret.isBlank()
        ) {
            return
        }

        saveBaiduCredentials(legacyBaiduAppId, legacyBaiduSecretKey)
        saveYoudaoCredentials(legacyYoudaoAppKey, legacyYoudaoAppSecret)
        source.clearLegacySecrets()
    }

    private fun putOrRemove(key: String, value: String) {
        val normalized = value.trim()
        if (normalized.isBlank()) {
            secretStore.remove(key)
        } else {
            secretStore.put(key, normalized)
        }
    }

    companion object {
        const val KEY_BAIDU_APP_ID = "translation.baidu.app_id"
        const val KEY_BAIDU_SECRET_KEY = "translation.baidu.secret_key"
        const val KEY_YOUDAO_APP_KEY = "translation.youdao.app_key"
        const val KEY_YOUDAO_APP_SECRET = "translation.youdao.app_secret"
    }
}
