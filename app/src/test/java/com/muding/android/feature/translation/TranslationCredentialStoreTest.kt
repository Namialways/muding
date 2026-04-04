package com.muding.android.feature.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationCredentialStoreTest {

    @Test
    fun `save credentials trims values and removes blanks`() {
        val secretStore = InMemorySecretStore()
        val store = TranslationCredentialStore(secretStore)

        store.saveBaiduCredentials("  app-id  ", "  secret-key  ")
        store.saveYoudaoCredentials("  ", "  ")

        assertEquals(
            TranslationCredentialBundle(
                baiduAppId = "app-id",
                baiduSecretKey = "secret-key",
                youdaoAppKey = "",
                youdaoAppSecret = ""
            ),
            store.getCredentials()
        )
        assertFalse(secretStore.contains(TranslationCredentialStore.KEY_YOUDAO_APP_KEY))
        assertFalse(secretStore.contains(TranslationCredentialStore.KEY_YOUDAO_APP_SECRET))
    }

    @Test
    fun `migrateFromLegacy copies secrets and clears legacy storage`() {
        val secretStore = InMemorySecretStore()
        val store = TranslationCredentialStore(secretStore)
        val legacySource = FakeLegacyTranslationSecretSource(
            baiduAppId = "legacy-baidu-id",
            baiduSecretKey = "legacy-baidu-secret",
            youdaoAppKey = "legacy-youdao-key",
            youdaoAppSecret = "legacy-youdao-secret"
        )

        store.migrateFromLegacy(legacySource)

        assertEquals(
            TranslationCredentialBundle(
                baiduAppId = "legacy-baidu-id",
                baiduSecretKey = "legacy-baidu-secret",
                youdaoAppKey = "legacy-youdao-key",
                youdaoAppSecret = "legacy-youdao-secret"
            ),
            store.getCredentials()
        )
        assertTrue(legacySource.cleared)
    }

    @Test
    fun `migrateFromLegacy keeps existing secure secrets`() {
        val secretStore = InMemorySecretStore().apply {
            put(TranslationCredentialStore.KEY_BAIDU_APP_ID, "secure-id")
            put(TranslationCredentialStore.KEY_BAIDU_SECRET_KEY, "secure-secret")
        }
        val store = TranslationCredentialStore(secretStore)
        val legacySource = FakeLegacyTranslationSecretSource(
            baiduAppId = "legacy-id",
            baiduSecretKey = "legacy-secret",
            youdaoAppKey = "",
            youdaoAppSecret = ""
        )

        store.migrateFromLegacy(legacySource)

        assertEquals("secure-id", store.getCredentials().baiduAppId)
        assertEquals("secure-secret", store.getCredentials().baiduSecretKey)
        assertTrue(legacySource.cleared)
    }

    @Test
    fun `clearAllCredentials removes every stored provider secret`() {
        val secretStore = InMemorySecretStore().apply {
            put(TranslationCredentialStore.KEY_BAIDU_APP_ID, "id")
            put(TranslationCredentialStore.KEY_BAIDU_SECRET_KEY, "secret")
            put(TranslationCredentialStore.KEY_YOUDAO_APP_KEY, "key")
            put(TranslationCredentialStore.KEY_YOUDAO_APP_SECRET, "secret")
        }
        val store = TranslationCredentialStore(secretStore)

        store.clearAllCredentials()

        assertEquals(
            TranslationCredentialBundle(
                baiduAppId = "",
                baiduSecretKey = "",
                youdaoAppKey = "",
                youdaoAppSecret = ""
            ),
            store.getCredentials()
        )
    }

    private class InMemorySecretStore : SecretStore {
        private val values = linkedMapOf<String, String>()

        override fun get(key: String): String? = values[key]

        override fun put(key: String, value: String) {
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }

        fun contains(key: String): Boolean = values.containsKey(key)
    }

    private class FakeLegacyTranslationSecretSource(
        private val baiduAppId: String,
        private val baiduSecretKey: String,
        private val youdaoAppKey: String,
        private val youdaoAppSecret: String
    ) : LegacyTranslationSecretSource {

        var cleared: Boolean = false
            private set

        override fun readBaiduAppId(): String = baiduAppId

        override fun readBaiduSecretKey(): String = baiduSecretKey

        override fun readYoudaoAppKey(): String = youdaoAppKey

        override fun readYoudaoAppSecret(): String = youdaoAppSecret

        override fun clearLegacySecrets() {
            cleared = true
        }
    }
}
