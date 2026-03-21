package com.pixpin.android.feature.translation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class TranslationErrorMessagesTest {

    @Test
    fun `resolve missing credentials message`() {
        val message = TranslationErrorMessages.resolve(
            TranslationException(
                type = TranslationFailureType.MISSING_CREDENTIALS,
                providerLabel = "百度翻译"
            )
        )

        assertEquals("请先在翻译设置中配置百度翻译密钥", message)
    }

    @Test
    fun `resolve local model missing message`() {
        val message = TranslationErrorMessages.resolve(
            TranslationException(
                type = TranslationFailureType.LOCAL_MODEL_MISSING,
                providerLabel = "本地翻译"
            )
        )

        assertEquals("请先在翻译设置中下载对应语言模型", message)
    }

    @Test
    fun `resolve timeout message`() {
        val message = TranslationErrorMessages.resolve(
            SocketTimeoutException("timeout")
        )

        assertEquals("翻译服务连接超时，请稍后重试", message)
    }

    @Test
    fun `resolve network unavailable message`() {
        val message = TranslationErrorMessages.resolve(
            UnknownHostException("offline")
        )

        assertEquals("网络不可用，请检查网络后重试", message)
    }
}
