package com.cornelius.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private val PREFS_NAME = "CorneliusPrefs"
    private val KEY_SERVER_URL = "server_url"
    private val DEFAULT_SERVER_URL = "http://192.168.1.5:8080/mobile"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup dynamic layout programmatically to avoid complex XML inflation issues
        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(0xFF09090B.toInt())

        swipeRefresh = SwipeRefreshLayout(this)
        webView = WebView(this)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.max = 100
        progressBar.visibility = View.GONE

        val progressParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            8
        )
        progressBar.layoutParams = progressParams

        swipeRefresh.addView(webView)
        rootLayout.addView(swipeRefresh)
        rootLayout.addView(progressBar)

        setContentView(rootLayout)

        setupWebView()
        setupSwipeRefresh()
        setupBackNavigation()

        loadServerUrl()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.userAgentString = settings.userAgentString + " CorneliusAndroidApp/1.0"

        webView.setBackgroundColor(0xFF09090B.toInt())

        // Hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Javascript Bridge for Toast & Vibration
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Only show dialog if error is for the main webpage frame, not background assets/requests
                if (request?.isForMainFrame == true) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    showConnectionErrorDialog()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }
        }

        // Long press anywhere to configure PC IP
        webView.setOnLongClickListener {
            showChangeIpDialog()
            true
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(0xFF38BDF8.toInt(), 0xFF22C55E.toInt())
        swipeRefresh.setProgressBackgroundColorSchemeColor(0xFF18181C.toInt())
        // Disable swipe pull-to-refresh to prevent accidental page reloads while scrolling chat
        swipeRefresh.isEnabled = false
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadServerUrl() {
        // Load fast local interface directly from APK assets to guarantee instant UI rendering
        webView.loadUrl("file:///android_asset/mobile.html")
    }

    private fun showConnectionErrorDialog() {
        vibratePhone()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentUrl = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL

        AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
            .setTitle("Falha ao Conectar")
            .setMessage("Não foi possível acessar o Cornelius em:\n$currentUrl\n\nCertifique-se de que:\n1. O computador está ligado com o Cornelius rodando.\n2. O celular está no mesmo Wi-Fi que o PC.\n3. O IP do computador está correto.")
            .setPositiveButton("Tentar Novamente") { _, _ ->
                webView.reload()
            }
            .setNeutralButton("Alterar IP do PC") { _, _ ->
                showChangeIpDialog()
            }
            .setCancelable(true)
            .show()
    }

    private fun showChangeIpDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentUrl = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL

        val input = EditText(this)
        input.setText(currentUrl)
        input.setSelection(currentUrl.length)

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(48, 24, 48, 24)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
            .setTitle("Configurar IP do Computador")
            .setMessage("Informe o IP e porta do Cornelius:")
            .setView(container)
            .setPositiveButton("Salvar e Conectar") { _, _ ->
                var newUrl = input.text.toString().trim()
                if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                    newUrl = "http://$newUrl"
                }
                prefs.edit().putString(KEY_SERVER_URL, newUrl).apply()
                Toast.makeText(this, "Conectando a $newUrl", Toast.LENGTH_SHORT).show()
                webView.loadUrl(newUrl)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    }

    class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun showToast(toast: String) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun vibrate(milliseconds: Long) {
            val vibrator = context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(milliseconds)
            }
        }
    }
}

