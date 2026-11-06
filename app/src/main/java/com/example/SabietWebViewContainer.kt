package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.SabietAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabietWebViewContainer(
    targetUrl: String,
    isOnline: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var errorDescription by remember { mutableStateOf<String?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var isRetrying by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    // Handle back button natively: go back in WebView history without showing URLs
    BackHandler(enabled = canGoBack && !isError) {
        webViewInstance?.let { wv ->
            if (wv.canGoBack()) {
                wv.goBack()
            }
        }
    }

    // When connection is restored and we are in error state, automatically or smoothly allow retry
    LaunchedEffect(isOnline) {
        if (isOnline && isError) {
            // Auto reload when connection is back
            isRetrying = true
            isError = false
            webViewInstance?.reload()
            delay(1000)
            isRetrying = false
        }
    }

    Crossfade(
        targetState = isError,
        label = "ContentCrossfade",
        modifier = modifier.fillMaxSize()
    ) { errorActive ->
        if (errorActive) {
            OfflineScreen(
                isOnline = isOnline,
                isRetrying = isRetrying,
                errorMessage = errorDescription,
                onRetry = {
                    scope.launch {
                        isRetrying = true
                        isError = false
                        webViewInstance?.reload()
                        delay(1200)
                        isRetrying = false
                    }
                }
            )
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    webViewInstance?.reload()
                },
                state = pullRefreshState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("webview_pull_refresh_container")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            createConfiguredWebView(
                                context = ctx,
                                onProgressUpdate = { progress ->
                                    pageProgress = progress
                                    isLoading = progress < 100
                                    if (progress >= 100) {
                                        isRefreshing = false
                                    }
                                },
                                onPageStarted = {
                                    isLoading = true
                                },
                                onPageFinished = { wv ->
                                    isLoading = false
                                    isRefreshing = false
                                    canGoBack = wv.canGoBack()
                                    // Remember login session cookies locally
                                    WebSessionManager.persistCookies()
                                },
                                onErrorReceived = { description ->
                                    isError = true
                                    errorDescription = description
                                    isRefreshing = false
                                    isLoading = false
                                }
                            ).also { wv ->
                                webViewInstance = wv
                                onWebViewCreated(wv)
                                wv.loadUrl(targetUrl)
                            }
                        },
                        update = { wv ->
                            canGoBack = wv.canGoBack()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("main_webview")
                    )

                    // Native-like thin progress indicator at top edge - no address bar/URLs
                    AnimatedVisibility(
                        visible = isLoading && pageProgress in 1..99,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        LinearProgressIndicator(
                            progress = { pageProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = SabietAccent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    onProgressUpdate: (Int) -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: (WebView) -> Unit,
    onErrorReceived: (String) -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Native hardware acceleration & smooth scrolling
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

        // Enable Android Autofill & Google Password Manager integration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        }

        settings.apply {
            // JavaScript & DOM Storage for seamless session/login continuity
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            // Form data and password saving for Google Account autofill
            @Suppress("DEPRECATION")
            saveFormData = true
            @Suppress("DEPRECATION")
            savePassword = true

            // Performance optimizations
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Network and cache performance
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            // Media & fonts
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true

            // Native mobile user agent enhancement
            userAgentString = userAgentString.replace("; wv", "")
        }

        // Enable session & persistent cookies
        WebSessionManager.setupSessionPersistence(this)

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressUpdate(newProgress)
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onPageStarted()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view != null) {
                    // Assist Google Password Manager with credential field detection
                    view.evaluateJavascript(
                        """
                        (function() {
                            try {
                                var pwdInputs = document.querySelectorAll('input[type="password"]');
                                pwdInputs.forEach(function(pwd) {
                                    if (!pwd.getAttribute('autocomplete')) {
                                        pwd.setAttribute('autocomplete', 'current-password');
                                    }
                                    var form = pwd.closest('form');
                                    if (form) {
                                        var userInputs = form.querySelectorAll('input[type="text"], input[type="email"], input[name*="user"], input[name*="login"], input[id*="user"]');
                                        userInputs.forEach(function(u) {
                                            if (!u.getAttribute('autocomplete')) {
                                                u.setAttribute('autocomplete', 'username');
                                            }
                                        });
                                    }
                                });
                            } catch(e) {}
                        })();
                        """.trimIndent(),
                        null
                    )

                    // Trigger Android Autofill commit so Google Password Manager prompts to save credentials
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            val autofillManager = context.getSystemService(android.view.autofill.AutofillManager::class.java)
                            autofillManager?.commit()
                        } catch (_: Exception) {}
                    }

                    onPageFinished(view)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase()

                // All web links (http, https) MUST open within the WebView instead of external browser
                if (scheme == "http" || scheme == "https") {
                    return false
                }

                // Handle system intent schemes (e.g. tel:, mailto:, sms:, whatsapp:)
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    // Ignore unsupported custom schemes safely
                    true
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // Intercept main frame network errors to trigger custom offline page
                if (request?.isForMainFrame == true) {
                    val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        error?.description?.toString() ?: "Network error"
                    } else {
                        "Connection failed"
                    }
                    onErrorReceived(description)
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    val statusCode = errorResponse?.statusCode ?: 200
                    if (statusCode in 500..599) {
                        onErrorReceived("Server is temporarily unavailable (HTTP $statusCode)")
                    }
                }
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                // If the render process crashes, recover gracefully
                view?.destroy()
                onErrorReceived("Web renderer crashed. Reconnecting...")
                return true
            }
        }
    }
}
