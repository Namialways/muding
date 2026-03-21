package com.pixpin.android.feature.translation

import com.pixpin.android.data.settings.AppSettingsRepository
import com.pixpin.android.data.settings.CloudTranslationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.UUID

class BaiduCloudTranslationEngine(
    private val settingsRepository: AppSettingsRepository
) : TranslationEngine {

    override suspend fun translate(text: String, targetLanguageTag: String): TranslationResult {
        val settings = settingsRepository.getTranslationSettings()
        require(settings.baiduAppId.isNotBlank() && settings.baiduSecretKey.isNotBlank()) {
            "请先配置百度翻译 AppId 和 SecretKey"
        }
        val targetLanguage = TranslationLanguageCatalog.findByAppTag(targetLanguageTag)
        val query = text.trim()
        require(query.isNotBlank()) { "没有可翻译的文本" }
        val salt = UUID.randomUUID().toString().replace("-", "")
        val sign = TranslationSigning.buildBaiduSign(
            appId = settings.baiduAppId,
            query = query,
            salt = salt,
            secretKey = settings.baiduSecretKey
        )
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
            require(translated.isNotBlank()) { "百度翻译返回了空结果" }
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
            "请先配置有道 AppKey 和 AppSecret"
        }
        val targetLanguage = TranslationLanguageCatalog.findByAppTag(targetLanguageTag)
        val query = text.trim()
        require(query.isNotBlank()) { "没有可翻译的文本" }
        val salt = UUID.randomUUID().toString()
        val curtime = (System.currentTimeMillis() / 1000L).toString()
        val sign = TranslationSigning.buildYoudaoSign(
            appKey = settings.youdaoAppKey,
            query = query,
            salt = salt,
            curtime = curtime,
            appSecret = settings.youdaoAppSecret
        )
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
                throw IllegalStateException(
                    json.optString("msg", "有道翻译失败，错误码: $errorCode")
                )
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
            require(translated.isNotBlank()) { "有道翻译返回了空结果" }
            TranslationResult(
                translatedText = translated,
                providerLabel = "有道智云"
            )
        }
    }
}

class CloudTranslationEngineRouter(
    private val settingsRepository: AppSettingsRepository,
    private val baiduEngine: TranslationEngine,
    private val youdaoEngine: TranslationEngine
) : TranslationEngine {

    override suspend fun translate(text: String, targetLanguageTag: String): TranslationResult {
        return when (settingsRepository.getTranslationSettings().cloudProvider) {
            CloudTranslationProvider.BAIDU -> baiduEngine.translate(text, targetLanguageTag)
            CloudTranslationProvider.YOUDAO -> youdaoEngine.translate(text, targetLanguageTag)
            CloudTranslationProvider.NONE -> throw IllegalStateException("请先在设置中选择云翻译服务商")
        }
    }
}

private fun httpGet(urlString: String): String {
    val connection = URL(urlString).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 12000
    connection.readTimeout = 12000
    return connection.readResponseBody()
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
    return connection.readResponseBody()
}

private fun HttpURLConnection.readResponseBody(): String {
    val stream = if (responseCode in 200..299) {
        inputStream
    } else {
        errorStream ?: inputStream
    }
    return stream.useUtf8Text()
}

private fun InputStream.useUtf8Text(): String {
    return bufferedReader(Charsets.UTF_8).use { it.readText() }
}

private fun urlEncode(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
}
