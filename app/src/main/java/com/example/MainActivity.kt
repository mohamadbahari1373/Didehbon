package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.CyberFloatingControls
import com.example.ui.components.DidehbonErrorView
import com.example.ui.components.DidehbonSplashScreen
import com.example.ui.components.ExitConfirmDialog
import com.example.ui.components.TopWebProgressBar
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.DidehbonTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        const val DEFAULT_TARGET_URL = "http://didehbon.ir"
    }

    private var webViewInstance: WebView? = null
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    // ActivityResult launcher for web file uploads (<input type="file">)
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val clipData = data?.clipData
            if (clipData != null && clipData.itemCount > 0) {
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            } else if (data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
        } else {
            null
        }
        fileUploadCallback?.onReceiveValue(uris)
        fileUploadCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialUrl = intent?.data?.toString() ?: DEFAULT_TARGET_URL

        setContent {
            DidehbonTheme {
                DidehbonApp(
                    initialUrl = initialUrl,
                    onExitApp = { finish() },
                    onOpenFileChooser = { callback, params ->
                        fileUploadCallback?.onReceiveValue(null)
                        fileUploadCallback = callback
                        try {
                            val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            fileChooserLauncher.launch(intent)
                            true
                        } catch (e: Exception) {
                            fileUploadCallback?.onReceiveValue(null)
                            fileUploadCallback = null
                            false
                        }
                    },
                    onRegisterWebView = { webView ->
                        webViewInstance = webView
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()?.let { newUrl ->
            webViewInstance?.loadUrl(newUrl)
        }
    }

    override fun onResume() {
        super.onResume()
        webViewInstance?.onResume()
    }

    override fun onPause() {
        webViewInstance?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webViewInstance?.apply {
            stopLoading()
            clearHistory()
            destroy()
        }
        webViewInstance = null
        super.onDestroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DidehbonApp(
    initialUrl: String,
    onExitApp: () -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onRegisterWebView: (WebView) -> Unit
) {
    var webProgress by remember { mutableIntStateOf(0) }
    var isInitialLoading by remember { mutableStateOf(true) }
    var isPageLoading by remember { mutableStateOf(true) }
    var hasNetworkError by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Auto-dismiss initial splash screen after timeout so UI never gets stuck
    LaunchedEffect(Unit) {
        delay(3000)
        isInitialLoading = false
    }

    // Intercept hardware Back Button
    BackHandler {
        val webView = webViewRef
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            showExitDialog = true
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberNavyDark),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberNavyDark)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Main Native-feel 100% Full-Screen WebView
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("didehbon_webview"),
                factory = { context ->
                    WebView(context).apply {
                        webViewRef = this
                        onRegisterWebView(this)

                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Visual & Full Smooth Scrolling Configuration
                        setBackgroundColor(0xFF0B0E14.toInt())
                        isVerticalScrollBarEnabled = true
                        isHorizontalScrollBarEnabled = false
                        isScrollbarFadingEnabled = true
                        isNestedScrollingEnabled = true
                        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

                        // WebSettings Configuration
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = false
                            }

                            // File & content access
                            allowFileAccess = true
                            allowContentAccess = true

                            // Responsive sizing & viewports
                            useWideViewPort = true
                            loadWithOverviewMode = true

                            // Zoom controls
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false

                            // Media & Mixed HTTP/HTTPS Content
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                            // Text encoding
                            defaultTextEncodingName = "UTF-8"
                        }

                        // Cookies configuration
                        val currentWebView = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(currentWebView, true)
                        }

                        // Download Manager listener
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            try {
                                val request = DownloadManager.Request(Uri.parse(url)).apply {
                                    setMimeType(mimetype)
                                    addRequestHeader("User-Agent", userAgent)
                                    val cookie = CookieManager.getInstance().getCookie(url)
                                    if (cookie != null) {
                                        addRequestHeader("Cookie", cookie)
                                    }
                                    val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                    setTitle(filename)
                                    setDescription("دانلود از دیده‌بان...")
                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                                }
                                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                                dm?.enqueue(request)
                                Toast.makeText(context, "در حال دانلود فایل...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                try {
                                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(fallbackIntent)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "خطا در دریافت فایل", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        // WebChromeClient for progress, title, and file uploads
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                webProgress = newProgress
                                if (newProgress >= 80) {
                                    isInitialLoading = false
                                }
                                isPageLoading = newProgress < 100
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                return onOpenFileChooser(filePathCallback, fileChooserParams)
                            }
                        }

                        // WebViewClient for navigation, errors, and external intents
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isPageLoading = true
                                hasNetworkError = false
                                if (url != null) {
                                    currentUrl = url
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isPageLoading = false
                                isInitialLoading = false
                                if (url != null) {
                                    currentUrl = url
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    hasNetworkError = true
                                    isInitialLoading = false
                                    isPageLoading = false
                                }
                            }

                            @SuppressLint("WebViewClientOnReceivedSslError")
                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                // Proceed safely to support both HTTP and HTTPS configurations
                                handler?.proceed()
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                // Gracefully recover from GPU/MESA render process exit without crashing
                                try {
                                    view?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                    view?.loadUrl(currentUrl)
                                } catch (_: Exception) {
                                    hasNetworkError = true
                                }
                                return true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                return handleExternalOrInternalUrl(context, view, url)
                            }
                        }

                        // Load initial target website
                        loadUrl(initialUrl)
                    }
                }
            )

            // Top Progress Bar for in-app navigation
            TopWebProgressBar(
                progress = webProgress,
                isLoading = isPageLoading
            )

            // Discreet Floating Quick Controls (Home, Reload, Browser)
            CyberFloatingControls(
                onHomeClick = {
                    webViewRef?.loadUrl(MainActivity.DEFAULT_TARGET_URL)
                },
                onRefreshClick = {
                    webViewRef?.reload()
                },
                onOpenExternalClick = {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        webViewRef?.context?.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(webViewRef?.context, "مرورگری یافت نشد", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Custom Radar Splash Screen on cold start
            DidehbonSplashScreen(
                progress = webProgress,
                isVisible = isInitialLoading
            )

            // Cyber Network Error Screen when connection fails
            if (hasNetworkError) {
                DidehbonErrorView(
                    targetUrl = currentUrl,
                    onRetry = {
                        hasNetworkError = false
                        isPageLoading = true
                        webViewRef?.loadUrl(currentUrl)
                    }
                )
            }

            // Exit Confirmation Dialog
            if (showExitDialog) {
                ExitConfirmDialog(
                    onDismiss = { showExitDialog = false },
                    onConfirmExit = {
                        showExitDialog = false
                        onExitApp()
                    }
                )
            }
        }
    }
}

/**
 * Handles deep links, protocols (tel, mailto, tg, whatsapp), and internal site navigation
 */
private fun handleExternalOrInternalUrl(context: Context, webView: WebView?, url: String): Boolean {
    val uri = Uri.parse(url)
    val scheme = uri.scheme?.lowercase() ?: ""
    val host = uri.host?.lowercase() ?: ""

    // Handle standard communication protocols
    if (scheme in listOf("tel", "mailto", "sms", "whatsapp", "tg", "intent")) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Toast.makeText(context, "برنامه مربوطه بر روی دستگاه یافت نشد", Toast.LENGTH_SHORT).show()
            return true
        }
    }

    // Keep didehbon.ir and related subdomains inside WebView
    if (host.contains("didehbon.ir") || host.contains("wallex.net") || host.isEmpty()) {
        return false // let WebView handle it
    }

    // For third-party external links, open in external browser
    return try {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}
