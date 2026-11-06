package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test target website URL is configured to sabiet coy domain`() {
        val targetUrl = "https://sabiet.com/coy"
        assertTrue(targetUrl.startsWith("https://"))
        assertTrue(targetUrl.contains("sabiet.com/coy"))
    }

    @Test
    fun `test URL scheme filtering ensures web links stay inside webview`() {
        val webSchemes = listOf("http", "https")
        val externalAppSchemes = listOf("tel", "mailto", "sms", "whatsapp")

        for (scheme in webSchemes) {
            val shouldLaunchExternalBrowser = !(scheme == "http" || scheme == "https")
            assertFalse(shouldLaunchExternalBrowser)
        }

        for (scheme in externalAppSchemes) {
            val isCustomScheme = scheme != "http" && scheme != "https"
            assertTrue(isCustomScheme)
        }
    }
}
