package com.muding.android.feature.translation

import java.security.MessageDigest

object TranslationSigning {

    fun buildBaiduSign(appId: String, query: String, salt: String, secretKey: String): String {
        return md5("$appId$query$salt$secretKey")
    }

    fun buildYoudaoInput(query: String): String {
        return if (query.length <= 20) {
            query
        } else {
            query.take(10) + query.length + query.takeLast(10)
        }
    }

    fun buildYoudaoSign(
        appKey: String,
        query: String,
        salt: String,
        curtime: String,
        appSecret: String
    ): String {
        return sha256("$appKey${buildYoudaoInput(query)}$salt$curtime$appSecret")
    }

    fun md5(value: String): String = digest("MD5", value)

    fun sha256(value: String): String = digest("SHA-256", value)

    private fun digest(algorithm: String, value: String): String {
        val bytes = MessageDigest.getInstance(algorithm).digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
