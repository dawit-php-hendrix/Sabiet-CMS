package com.example

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import java.io.File

object WebSessionManager {
    private const val TAG = "WebSessionManager"

    /**
     * Configures the CookieManager so that user login sessions and authentication cookies
     * are persisted locally on the device across app launches.
     */
    fun setupSessionPersistence(webView: WebView) {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            cookieManager.flush()
            Log.d(TAG, "Session and cookie persistence enabled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring session persistence", e)
        }
    }

    /**
     * Flushes active cookies to storage to ensure user authentication state is safely saved.
     */
    fun persistCookies() {
        try {
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing cookies", e)
        }
    }

    /**
     * Clears temporary web cache and disk cache on app exit as requested,
     * while deliberately preserving user cookies and DOM storage so login sessions remain intact.
     */
    fun clearCacheOnExit(context: Context, webView: WebView?) {
        try {
            // Flush cookies before closing so login state is secure
            CookieManager.getInstance().flush()

            // Clear temporary HTTP and render cache from WebView
            webView?.clearCache(true)

            // Clear app internal cache directory (temporary response caches, images, etc.)
            deleteDir(context.cacheDir)
            deleteDir(File(context.applicationInfo.dataDir, "app_webview/Default/HTTP Cache"))
            deleteDir(File(context.applicationInfo.dataDir, "app_webview/Cache"))

            Log.d(TAG, "WebView cache cleared successfully on exit")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache on exit", e)
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list() ?: return false
            for (child in children) {
                val success = deleteDir(File(dir, child))
                if (!success) return false
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }
}
