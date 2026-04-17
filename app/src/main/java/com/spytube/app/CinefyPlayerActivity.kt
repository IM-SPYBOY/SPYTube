package com.spytube.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceResponse
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.spytube.app.api.CinefyClient
import okhttp3.HttpUrl.Companion.toHttpUrl


class CinefyPlayerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var textCenterInfo: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    private var layoutBrightness: View? = null
    private var viewBrightnessProgress: View? = null
    private var layoutVolume: View? = null
    private var viewVolumeProgress: View? = null
    private var isLongPressing = false
    private var longPressRunnable: Runnable? = null
    private var hideOverlayRunnable: Runnable? = null

    private var startX = 0f
    private var startY = 0f
    private var lastY = 0f
    private var isDragging = false
    private var isInSwipeZone = false
    private var swipeSide = 0 // 0=none, 1=left(brightness), 2=right(volume)
    private val TOUCH_SLOP = 50
    private var volumeSmooth = 0f // smooth accumulator for volume
    @Volatile private var jsMenuOpen = false
    private var playTitle: String = "" // track title for saving resume position

    private var prefs: android.content.SharedPreferences? = null
    private var audioManager: android.media.AudioManager? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // Fullscreen immersive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        webView = findViewById(R.id.webview)
        progressBar = findViewById(R.id.progress)
        layoutBrightness = findViewById(R.id.layout_brightness)
        viewBrightnessProgress = findViewById(R.id.view_brightness_progress)
        layoutVolume = findViewById(R.id.layout_volume)
        viewVolumeProgress = findViewById(R.id.view_volume_progress)
        textCenterInfo = findViewById(R.id.text_center_info)
        audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager

        prefs = getSharedPreferences("player_prefs", MODE_PRIVATE)

        // Restore brightness
        prefs?.getFloat("brightness", -1f)?.let { saved ->
            if (saved != -1f) {
                val lp = window.attributes
                lp.screenBrightness = saved
                window.attributes = lp
            }
        }

        // Long press → 2x speed (no overlay text)
        longPressRunnable = Runnable {
            isLongPressing = true
            webView.evaluateJavascript(
                "if(V){V.playbackRate=2.0}; document.getElementById('si').textContent='2x ▶▶'; document.getElementById('si').classList.add('sh');",
                null
            )
        }

        val searchTitle = intent.getStringExtra("searchTitle")
        val provider = intent.getStringExtra("provider") ?: ""
        val mediaId = intent.getStringExtra("mediaId") ?: ""
        val title = intent.getStringExtra("title") ?: searchTitle ?: ""
        val showId = intent.getStringExtra("showId") ?: ""
        val seasonId = intent.getStringExtra("seasonId") ?: ""
        val season = intent.getIntExtra("season", 0)
        val episode = intent.getIntExtra("episode", 0)
        val isTv = intent.getBooleanExtra("isTv", false)

        val localUri = intent.getStringExtra("localUri") ?: ""

        android.util.Log.d("CinefyPlayer", "INTENT EXTRAS: searchTitle='$searchTitle' mediaId='$mediaId' title='$title' isTv=$isTv season=$season episode=$episode localUri='$localUri'")

        // If no mediaId and no searchTitle and no localUri, nothing to play
        if (mediaId.isEmpty() && searchTitle.isNullOrEmpty() && localUri.isEmpty()) {
            android.util.Log.e("CinefyPlayer", "EARLY EXIT: no mediaId, no searchTitle, no localUri — finishing!")
            finish(); return
        }

        setupWebView()

        // JS bridge
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun close() {
                runOnUiThread { finish() }
            }
            @android.webkit.JavascriptInterface
            fun setMenuOpen(open: Boolean) {
                jsMenuOpen = open
            }
            @android.webkit.JavascriptInterface
            fun fallbackPlayer() {
                runOnUiThread {
                    // Fallback to vidlink player
                    val title = intent.getStringExtra("searchTitle") ?: intent.getStringExtra("title") ?: ""
                    val isTv = intent.getBooleanExtra("isTv", false)
                    val season = intent.getIntExtra("season", 0)
                    val episode = intent.getIntExtra("episode", 0)
                    try {
                        val pi = Intent(this@CinefyPlayerActivity, PlayerActivity::class.java)
                        val media = intent.getSerializableExtra("media") as? com.spytube.app.models.MediaItem
                        if (media != null) {
                            pi.putExtra("media", media)
                            pi.putExtra("season", if (season < 1) 1 else season)
                            pi.putExtra("episode", if (episode < 1) 1 else episode)
                            pi.putExtra("server", 2) // vidlink
                            startActivity(pi)
                        }
                    } catch (_: Exception) {}
                    finish()
                }
            }
            @android.webkit.JavascriptInterface
            fun cacheResult(title: String, provider: String, mediaId: String, isTv: Boolean, season: Int, episode: Int) {
                com.spytube.app.models.CinefyCache.save(
                    this@CinefyPlayerActivity, title, provider, mediaId, isTv, season, episode
                )
            }
            @android.webkit.JavascriptInterface
            fun saveProgress(title: String, position: Double) {
                com.spytube.app.models.CinefyCache.savePosition(
                    this@CinefyPlayerActivity, title, position
                )
            }
        }, "AndroidBridge")

        // Store params for injection after page loads
        webView.tag = mapOf(
            "mode" to if (searchTitle != null) "search" else "direct",
            "provider" to provider, "mediaId" to mediaId, "title" to title,
            "searchTitle" to (searchTitle ?: ""),
            "showId" to showId, "seasonId" to seasonId,
            "season" to season, "episode" to episode, "isTv" to isTv,
            "resumePosition" to intent.getFloatExtra("resumePosition", 0f),
            "localUri" to (intent.getStringExtra("localUri") ?: "")
        )
        playTitle = title

        webView.loadUrl("file:///android_asset/cinefy_player.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        android.webkit.CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            databaseEnabled = true
            databasePath = applicationContext.getDir("webview_db", MODE_PRIVATE).path
            @Suppress("DEPRECATION")
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = false
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
        }
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.settings.setOffscreenPreRaster(true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }

            // Route ALL external requests through DohTunnel (shared DoH + connection pool)
            // Skip: local files, video segments (.ts) for native speed
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                // Skip local assets
                if (url.startsWith("file://") || url.startsWith("data:")) return super.shouldInterceptRequest(view, request)
                if (url.endsWith(".ts") || url.contains(".ts?")) return super.shouldInterceptRequest(view, request)
                try {
                    val okReq = okhttp3.Request.Builder()
                        .url(url)
                        .apply { request.requestHeaders?.forEach { (k, v) -> addHeader(k, v) } }
                        .build()
                    val resp = com.spytube.app.api.DohTunnel.client.newCall(okReq).execute()
                    val ct = resp.header("Content-Type") ?: "application/octet-stream"
                    val mime = ct.split(";").first().trim()
                    val enc = if (ct.contains("charset=")) ct.substringAfter("charset=").trim() else "UTF-8"
                    return WebResourceResponse(
                        mime, enc, resp.code, resp.message.ifEmpty { "OK" },
                        resp.headers.toMultimap().mapValues { it.value.joinToString(", ") },
                        resp.body?.byteStream()
                    )
                } catch (e: Exception) {
                    android.util.Log.w("CinefyPlayer", "DoH tunnel miss: ${e.message}")
                    return super.shouldInterceptRequest(view, request)
                }
            }


            override fun onPageFinished(view: WebView, url: String) {
                android.util.Log.d("CinefyPlayer", "onPageFinished URL=$url tag=${view.tag != null}")
                if (!url.contains("cinefy_player")) return
                val tag = view.tag as? Map<*, *> ?: return
                val mode = tag["mode"] as? String ?: "direct"
                val prov = (tag["provider"] as? String ?: "").replace("'", "\\'")
                val mid = (tag["mediaId"] as? String ?: "").replace("'", "\\'")
                val ttl = (tag["title"] as? String ?: "").replace("\\", "\\\\").replace("'", "\\'")
                val st = (tag["searchTitle"] as? String ?: "").replace("\\", "\\\\").replace("'", "\\'")
                val sid = (tag["showId"] as? String ?: "").replace("'", "\\'")
                val ssid = (tag["seasonId"] as? String ?: "").replace("'", "\\'")
                val sn = tag["season"] as? Int ?: 0
                val ep = tag["episode"] as? Int ?: 0
                val tv = tag["isTv"] as? Boolean ?: false

                view.tag = null

                val resumePos = tag["resumePosition"] as? Float ?: 0f
                val localUri = tag["localUri"] as? String ?: ""

                if (localUri.isNotEmpty()) {
                    view.evaluateJavascript("""
                        _t = '$ttl';
                        _resumeTime = $resumePos;
                        document.getElementById('tt').textContent = _t;
                        document.getElementById('spinner').classList.add('hide');
                        V.src = '$localUri';
                        V.play();
                    """.trimIndent(), null)
                } else if (mode == "search") {
                    view.evaluateJavascript("""
                        _t = '$ttl';
                        _tv = $tv;
                        _sn = $sn;
                        _ep = $ep;
                        _resumeTime = $resumePos;
                        searchAndPlay('$st', $tv, $sn, $ep);
                    """.trimIndent(), null)
                } else {
                    view.evaluateJavascript("""
                        _p = '$prov';
                        _m = '$mid';
                        _t = '$ttl';
                        _si = '$sid';
                        _ss = '$ssid';
                        _sn = $sn;
                        _ep = $ep;
                        _tv = $tv;
                        _resumeTime = $resumePos;
                        startPlayer();
                    """.trimIndent(), null)
                }

                handler.postDelayed({ progressBar.visibility = View.GONE }, 3000)
            }

            override fun onReceivedSslError(view: WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
                handler.proceed()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean = false

            override fun getDefaultVideoPoster(): android.graphics.Bitmap? =
                android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress > 80) progressBar.visibility = View.GONE
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val edgeZone = screenWidth * 0.15f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                lastY = event.y
                isDragging = false
                swipeSide = 0
                volumeSmooth = audioManager?.let { it.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() } ?: 0f
                isInSwipeZone = !jsMenuOpen && (startX < edgeZone || startX > screenWidth - edgeZone)
                if (!jsMenuOpen) longPressRunnable?.let { handler.postDelayed(it, 600) }
                super.dispatchTouchEvent(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - startY
                val deltaX = event.x - startX
                if (Math.abs(deltaY) > 10 || Math.abs(deltaX) > 10) {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                }
                if (!isDragging) {
                    if (isInSwipeZone && Math.abs(deltaY) > TOUCH_SLOP && Math.abs(deltaY) > Math.abs(deltaX)) {
                        isDragging = true
                        swipeSide = if (startX < edgeZone) 1 else 2
                        val cancel = MotionEvent.obtain(event)
                        cancel.action = MotionEvent.ACTION_CANCEL
                        super.dispatchTouchEvent(cancel)
                        cancel.recycle()
                        lastY = event.y
                        return true
                    }
                } else {
                    val delta = lastY - event.y
                    lastY = event.y
                    if (swipeSide == 1) changeBrightness(delta) else changeVolume(delta)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }
                if (isLongPressing) {
                    isLongPressing = false
                    webView.evaluateJavascript(
                        "if(V){V.playbackRate=1.0}; document.getElementById('si').classList.remove('sh');",
                        null
                    )
                    return true
                }
                if (isDragging) {
                    isDragging = false
                    swipeSide = 0
                    hideOverlays()
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun changeBrightness(deltaY: Float) {
        val lp = window.attributes
        var current = lp.screenBrightness
        if (current == -1f) current = 0.5f
        val sensitivity = 1.5f
        val change = (deltaY / resources.displayMetrics.heightPixels) * sensitivity
        current = (current + change).coerceIn(0.01f, 1.0f)
        lp.screenBrightness = current
        window.attributes = lp

        layoutBrightness?.let { layout ->
            viewBrightnessProgress?.let { progress ->
                showOverlay(layout, progress, current)
            }
        }
    }

    private fun showOverlay(layout: View, progressView: View, percent: Float) {
        layout.visibility = View.VISIBLE
        layout.alpha = 1f
        layout.post {
            val params = progressView.layoutParams
            params.height = (layout.height * percent).toInt()
            progressView.layoutParams = params
        }
        hideOverlayRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun changeVolume(deltaY: Float) {
        val am = audioManager ?: return
        val maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
        val sensitivity = 1.5f
        val change = (deltaY / resources.displayMetrics.heightPixels) * sensitivity * maxVol
        volumeSmooth = (volumeSmooth + change).coerceIn(0f, maxVol)
        val newVol = volumeSmooth.toInt().coerceIn(0, maxVol.toInt())
        val curVol = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        if (newVol != curVol) {
            am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
        }
        // Show smooth visual
        val percent = volumeSmooth / maxVol
        layoutVolume?.let { layout ->
            viewVolumeProgress?.let { progress ->
                showOverlay(layout, progress, percent)
            }
        }
    }

    private fun hideOverlays() {
        hideOverlayRunnable = Runnable {
            layoutBrightness?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
                layoutBrightness?.visibility = View.GONE
            }
            layoutVolume?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
                layoutVolume?.visibility = View.GONE
            }
        }
        handler.postDelayed(hideOverlayRunnable!!, 1000)
    }

    override fun onPause() {
        super.onPause()
        window.attributes.let { lp ->
            if (lp.screenBrightness != -1f) {
                prefs?.edit()?.putFloat("brightness", lp.screenBrightness)?.apply()
            }
        }
        // Save resume position
        if (playTitle.isNotEmpty()) {
            try {
                webView.evaluateJavascript("V.currentTime||0") { result ->
                    val time = result?.toDoubleOrNull() ?: 0.0
                    if (time > 5.0) {
                        com.spytube.app.models.CinefyCache.savePosition(this, playTitle, time)
                    }
                }
            } catch (_: Exception) {}
        }
        android.webkit.CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.clearHistory()
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.destroy()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
