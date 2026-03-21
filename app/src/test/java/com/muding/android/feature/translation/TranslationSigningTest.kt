package com.muding.android.feature.translation

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationSigningTest {

    @Test
    fun `buildYoudaoInput keeps short query unchanged`() {
        assertEquals("hello", TranslationSigning.buildYoudaoInput("hello"))
    }

    @Test
    fun `buildYoudaoInput condenses long query`() {
        assertEquals(
            "abcdefghij26qrstuvwxyz",
            TranslationSigning.buildYoudaoInput("abcdefghijklmnopqrstuvwxyz")
        )
    }

    @Test
    fun `buildBaiduSign matches expected md5`() {
        val sign = TranslationSigning.buildBaiduSign(
            appId = "appid",
            query = "hello",
            salt = "123",
            secretKey = "secret"
        )

        assertEquals("c58cbb6d549dc3a83ba1fe72cd40ce54", sign)
    }

    @Test
    fun `buildYoudaoSign matches expected sha256`() {
        val sign = TranslationSigning.buildYoudaoSign(
            appKey = "testAppKey",
            query = "abcdefghijklmnopqrstuvwxyz",
            salt = "salt123",
            curtime = "1700000000",
            appSecret = "secret456"
        )

        assertEquals(
            "9eb5ae37679fb86ef2865a5ee3a9397f9c1e410073959b360c672e718cda7a17",
            sign
        )
    }
}
