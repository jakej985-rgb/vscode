package com.codepocket.local

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.codepocket.local.databinding.ActivityWebviewBinding

/**
 * WebView activity that hosts the VS Code UI.
 * 
 * Key features:
 * - Loads configurable URL (localhost server in production)
 * - Handles file chooser for uploads
 * - Handles file downloads
 * - Preserves state across rotation
 * - Hardware accelerated rendering
 */
class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebviewBinding
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    companion object {
        private const val TAG = "WebViewActivity"
        private const val WEBVIEW_STATE_KEY = "webview_state"
        const val EXTRA_WORKSPACE_ID = "workspace_id"
    }



    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    // Generic file picker for when MIME type isn't specified
    private val genericFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    // Notification permission launcher (for downloads on Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        requestNotificationPermissionIfNeeded()

        // Restore WebView state or load URL
        if (savedInstanceState != null) {
            savedInstanceState.getBundle(WEBVIEW_STATE_KEY)?.let { bundle ->
                binding.webView.restoreState(bundle)
            } ?: loadConfiguredUrl()
        } else {
            loadConfiguredUrl()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            // Enable hardware acceleration
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            settings.apply {
                // JavaScript is required for VS Code
                javaScriptEnabled = true
                
                // DOM storage for VS Code state
                domStorageEnabled = true
                
                // Database support
                databaseEnabled = true
                
                // File access for local files
                allowFileAccess = true
                allowContentAccess = true
                
                // Zoom controls
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                
                // Viewport settings
                useWideViewPort = true
                loadWithOverviewMode = true
                
                // Cache
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Mixed content - allow for localhost
                // 127.0.0.1 is a secure origin, but we may load http resources
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                
                // Modern web features
                mediaPlaybackRequiresUserGesture = false
                
                // Debugging (can be disabled in production)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }
            }

            // WebViewClient for navigation handling
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                    Log.d(TAG, "Page started: $url")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    Log.d(TAG, "Page finished: $url")
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        Log.e(TAG, "Error loading page: ${error?.description}")
                        showErrorUI(error?.description?.toString() ?: "Unknown error")
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    
                    // Keep localhost URLs in WebView
                    if (url.startsWith("http://127.0.0.1") || 
                        url.startsWith("http://localhost")) {
                        return false
                    }
                    
                    // Open external URLs in browser
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to open external URL: $url", e)
                    }
                    return true
                }
            }

            // WebChromeClient for file chooser and other Chrome features
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    binding.progressBar.progress = newProgress
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    // Cancel any existing callback
                    this@WebViewActivity.filePathCallback?.onReceiveValue(null)
                    this@WebViewActivity.filePathCallback = filePathCallback

                    val acceptTypes = fileChooserParams?.acceptTypes
                    val mimeType = if (!acceptTypes.isNullOrEmpty() && acceptTypes[0].isNotBlank()) {
                        acceptTypes[0]
                    } else {
                        "*/*"
                    }

                    Log.d(TAG, "File chooser requested, MIME type: $mimeType")

                    try {
                        if (mimeType == "*/*") {
                            genericFilePickerLauncher.launch(arrayOf("*/*"))
                        } else {
                            filePickerLauncher.launch(mimeType)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to launch file picker", e)
                        filePathCallback?.onReceiveValue(null)
                        this@WebViewActivity.filePathCallback = null
                        return false
                    }

                    return true
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        Log.d(TAG, "[Console] ${it.messageLevel()}: ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                    }
                    return true
                }
            }

            // Download listener
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                Log.d(TAG, "Download requested: $url, type: $mimetype, size: $contentLength")
                handleDownload(url, userAgent, contentDisposition, mimetype)
            }
        }
    }

    private fun loadConfiguredUrl() {
        val prefs = getSharedPreferences(PrefsConstants.PREF_NAME, MODE_PRIVATE)
        var url = prefs.getString(PrefsConstants.KEY_SERVER_URL, PrefsConstants.DEFAULT_URL)
            ?: PrefsConstants.DEFAULT_URL
        
        // Add workspace ID if present
        if (intent.hasExtra(EXTRA_WORKSPACE_ID)) {
             val workspaceId = intent.getStringExtra(EXTRA_WORKSPACE_ID)
             if (!workspaceId.isNullOrEmpty()) {
                 // Check if URL already has query params
                 url += if (url.contains("?")) "&ws=$workspaceId" else "?ws=$workspaceId"
             }
        }

        // Add Auth Token
        val token = getAuthToken()
        if (token != null) {
            url += if (url.contains("?")) "&token=$token" else "?token=$token"
        }
        
        Log.d(TAG, "Loading URL: $url")
        binding.webView.loadUrl(url)
    }

    private fun getAuthToken(): String? {
        val tokenFile = java.io.File(filesDir, "nodejs-project/.auth_token")
        return if (tokenFile.exists()) {
            try {
                tokenFile.readText().trim()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read auth token", e)
                null
            }
        } else {
            null
        }
    }

    private fun showErrorUI(message: String) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
        binding.btnRetry.setOnClickListener {
            binding.errorLayout.visibility = View.GONE
            loadConfiguredUrl()
        }
    }

    private fun handleDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimetype: String
    ) {
        try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimetype)
                addRequestHeader("User-Agent", userAgent)
                setTitle(fileName)
                setDescription("Downloading file...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Downloading: $fileName", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Download enqueued: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webViewState = Bundle()
        binding.webView.saveState(webViewState)
        outState.putBundle(WEBVIEW_STATE_KEY, webViewState)
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onPause() {
        binding.webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
