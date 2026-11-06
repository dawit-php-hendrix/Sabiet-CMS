package com.example

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var activeWebView: WebView? = null
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        networkMonitor = NetworkMonitor(applicationContext)

        setContent {
            MyApplicationTheme {
                val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(
                    initialValue = networkMonitor.isCurrentlyConnected()
                )

                // Primary website URL: Sabiet Materials Control System (MCS)
                val targetWebsiteUrl = "https://sabiet.com/coy"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        SabietWebViewContainer(
                            targetUrl = targetWebsiteUrl,
                            isOnline = isOnline,
                            onWebViewCreated = { webView ->
                                activeWebView = webView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Ensure session cookies are written to persistent storage
        WebSessionManager.persistCookies()
    }

    override fun onStop() {
        super.onStop()
        WebSessionManager.persistCookies()
    }

    override fun onDestroy() {
        // Clear cache on exit as requested by the user, while keeping login cookies/storage safe
        if (isFinishing) {
            WebSessionManager.clearCacheOnExit(applicationContext, activeWebView)
            activeWebView?.destroy()
            activeWebView = null
        }
        super.onDestroy()
    }
}
