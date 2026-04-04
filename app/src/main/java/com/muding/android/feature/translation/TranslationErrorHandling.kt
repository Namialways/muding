package com.muding.android.feature.translation

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class TranslationFailureType {
    EMPTY_TEXT,
    UNSUPPORTED_TARGET_LANGUAGE,
    LOCAL_MODEL_MISSING,
    CLOUD_PROVIDER_NOT_SELECTED,
    MISSING_CREDENTIALS,
    NETWORK_UNAVAILABLE,
    NETWORK_TIMEOUT,
    SERVICE_REJECTED,
    EMPTY_RESULT,
    UNKNOWN
}

class TranslationException(
    val type: TranslationFailureType,
    val providerLabel: String? = null,
    override val message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

object TranslationErrorMessages {

    fun resolve(throwable: Throwable): String {
        val error = throwable as? TranslationException
            ?: mapGenericThrowable(throwable)
        return when (error.type) {
            TranslationFailureType.EMPTY_TEXT -> "没有可翻译的文本"
            TranslationFailureType.UNSUPPORTED_TARGET_LANGUAGE -> "当前目标语言暂不支持翻译"
            TranslationFailureType.LOCAL_MODEL_MISSING -> "请先在翻译设置中下载对应语言模型"
            TranslationFailureType.CLOUD_PROVIDER_NOT_SELECTED -> "请先在翻译设置中选择云翻译服务商"
            TranslationFailureType.MISSING_CREDENTIALS -> {
                "请先在翻译设置中配置${error.providerLabel ?: "云翻译"}密钥"
            }

            TranslationFailureType.NETWORK_UNAVAILABLE -> "网络不可用，请检查网络后重试"
            TranslationFailureType.NETWORK_TIMEOUT -> "${error.providerLabel ?: "翻译服务"}连接超时，请稍后重试"
            TranslationFailureType.SERVICE_REJECTED -> {
                error.message ?: "${error.providerLabel ?: "翻译服务"}暂时不可用，请稍后重试"
            }

            TranslationFailureType.EMPTY_RESULT -> "${error.providerLabel ?: "翻译服务"}没有返回有效结果"
            TranslationFailureType.UNKNOWN -> "翻译失败，请稍后重试"
        }
    }

    fun mapGenericThrowable(
        throwable: Throwable,
        providerLabel: String? = null
    ): TranslationException {
        return when (throwable) {
            is TranslationException -> throwable
            is SocketTimeoutException -> TranslationException(
                type = TranslationFailureType.NETWORK_TIMEOUT,
                providerLabel = providerLabel,
                cause = throwable
            )

            is UnknownHostException, is ConnectException, is IOException -> TranslationException(
                type = TranslationFailureType.NETWORK_UNAVAILABLE,
                providerLabel = providerLabel,
                cause = throwable
            )

            else -> TranslationException(
                type = TranslationFailureType.UNKNOWN,
                providerLabel = providerLabel,
                cause = throwable
            )
        }
    }
}
