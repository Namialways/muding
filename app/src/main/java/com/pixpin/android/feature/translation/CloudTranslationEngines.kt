package com.pixpin.android.feature.translation

import com.pixpin.android.data.settings.AppSettingsRepository
import com.pixpin.android.data.settings.CloudTranslationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

class BaiduCloudTranslationEngine(
    private val settingsRepository: AppSettingsRepository
) : TranslationEngine {

    override suspend fun translate(text: String, targetLanguageTag: String): TranslationResult {
        val settings = settingsRepository.getTranslationSettings()
        require(settings.baiduAppId.isNotBlank() && settings.baiduSecretKey.isNotBlank()) {
            "请先配置百度翻译 AppId 和密钥"
        }
        val targetLanguage = TranslationLanguageCatalog.findByAppTag(targetLanguageTag)
        val query = text.trim()
        require(query.isNotBlank()) { "No text to translate" }
        val salt = UUID.randomUUID().toString().replace("-", "")
        val sign = md5("${settings.baiduAppId}$query$salt${settings.baiduSecretKey}")
        val requestUrl = buildString {
            append("https://fanyi-api.baidu.com/api/trans/vip/translate")
            append("?q=").append(urlEncode(query))
            append("&from=auto")
            append("&to=").append(urlEncode(targetLanguage.baiduCode))
            append("&appid=").append(urlEncode(settings.baiduAppId))
            append("&salt=").append(urlEncode(salt))
            append("&sign=").append(urlEncode(sign))
        }

        return withContext(Dispatchers.IO) {
            val response = httpGet(requestUrl)
            val json = JSONObject(response)
            if (json.has("error_code")) {
                throw IllegalStateException(json.optString("error_msg", "百度翻译失败"))
            }
            val resultArray = json.optJSONArray("trans_result") ?: JSONArray()
            val translated = buildString {
                for (index in 0 until resultArray.length()) {
                    val item = resultArray.optJSONObject(index) ?: continue
                    if (isNotEmpty()) append('\n')
                    append(item.optString("dst"))
                }
            }.trim()
            require(translated.isNotBlank()) { "百度翻译返回空结果" }
            TranslationResult(
                translatedText = translated,
                providerLabel = "百度翻译"
            )
        }
    }
}

class YoudaoCloudTranslationEngine(
    private val settingsRepository: AppSettingsRepository
) : TranslationEngine {

    override suspend fun translate(text: String, targetLanguageTag: String): TranslationResult {
        val settings = settingsRepository.getTranslationSettings()
        require(settings.youdaoAppKey.isNotBlank() && settings.youdaoAppSecret.isNotBlank()) {
            "请先配置有道 AppKey 和密钥"
        }
        val targetLanguage = TranslationLanguageCatalog.findByAppTag(targetLanguageTag)
        val query = text.trim()
        require(query.isNotBlank()) { "No text to translate" }
        val salt = UUID.randomUUID().toString()
        val curtime = (System.currentTimeMillis() / 1000L).toString()
        val input = buildYoudaoInput(query)
        val sign = sha256("${settings.youdaoAppKey}$input$salt$curtime${settings.youdaoAppSecret}")
        val body = buildString {
            append("q=").append(urlEncode(query))
            append("&from=auto")
            append("&to=").append(urlEncode(targetLanguage.youdaoCode))
            append("&appKey=").append(urlEncode(settings.youdaoAppKey))
            append("&salt=").append(urlEncode(salt))
            append("&sign=").append(urlEncode(sign))
            append("&signType=v3")
            append("&curtime=").append(urlEncode(curtime))
        }
        return withContext(Dispatchers.IO) {
            val response = httpPostForm("https://openapi.youdao.com/api", body)
            val json = JSONObject(response)
            val errorCode = json.optString("errorCode", "0")
            if (errorCode != "0") {
                throw IllegalStateException(json.optString("msg", "有道翻译失败，错误码: $errorCode"))
            }
            val translationArray = json.optJSONArray("translation") ?: JSONArray()
            val translated = buildString {
                for (index in 0 until translationArray.length()) {
                    val item = translationArray.optString(index)
                    if (item.isBlank()) continue
                    if (isNotEmpty()) append('\n')
                    append(item)
                }
            }.trim()
            require(translated.isNotBlank()) { "有道翻译返回空结果" }
            TranslationResult(
                translatedText = translated,
                providerLabel = "有道智云"
            )
        }
    }
}

class CloudTranslationEngineRouter(
    private val settingsRepository: AppSettingsRepository,
    private val baiduEngine: BaiduCloudTranslationEngine,
    private val youdaoEngine: YoudaoCloudTranslationEngine
) : TranslationEngine {

    override suspend fun translate(text: String, targetLanguageTag: String): TranslationResult {
        return when (settingsRepository.getTranslationSettings().cloudProvider) {
            CloudTranslationProvider.BAIDU -> baiduEngine.translate(text, targetLanguageTag)
            CloudTranslationProvider.YOUDAO -> youdaoEngine.translate(text, targetLanguageTag)
            CloudTranslationProvider.NONE -> throw IllegalStateException("请先在设置中选择云翻译服务商")
        }
    }
}

private fun buildYoudaoInput(query: String): String {
    return if (query.length <= 20) {
        query
    } else {
        query.take(10) + query.length + query.takeLast(10)
    }
}

private fun httpGet(urlString: String): String {
    val connection = URL(urlString).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 12000
    connection.readTimeout = 12000
    return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}

private fun httpPostForm(urlString: String, body: String): String {
    val connection = URL(urlString).openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.connectTimeout = 12000
    connection.readTimeout = 12000
    connection.doOutput = true
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
        writer.write(body)
    }
    return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}

private fun urlEncode(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
}

private fun md5(value: String): String {
    return digest("MD5", value)
}

private fun sha256(value: String): String {
    return digest("SHA-256", value)
}

private fun digest(algorithm: String, value: String): String {
    val bytes = MessageDigest.getInstance(algorithm).digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}
