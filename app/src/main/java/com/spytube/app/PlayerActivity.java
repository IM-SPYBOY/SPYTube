package com.spytube.app;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import com.spytube.app.models.MediaItem;

public class PlayerActivity extends AppCompatActivity {

    private TextView textCenterInfo;
    private boolean isLongPressing = false;
    private SharedPreferences prefs;

    private WebView webView;
    private ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private View layoutBrightness, layoutVolume;
    private View viewBrightnessProgress, viewVolumeProgress;
    private Runnable longPressRunnable;
    private Runnable hideOverlayRunnable;
    private android.media.AudioManager audioManager;
    private int maxVolume;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        
        // Modern Immersive Fullscreen (API 30+ for Android 15-16 compatibility)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress);
        
        // Overlays
        layoutBrightness = findViewById(R.id.layout_brightness);
        layoutVolume = findViewById(R.id.layout_volume);
        viewBrightnessProgress = findViewById(R.id.view_brightness_progress);
        viewVolumeProgress = findViewById(R.id.view_volume_progress);
        textCenterInfo = findViewById(R.id.text_center_info);

        // Audio & Prefs
        audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
        prefs = getSharedPreferences("player_prefs", MODE_PRIVATE);

        // Restore Brightness
        float savedBrightness = prefs.getFloat("brightness", -1f);
        if (savedBrightness != -1f) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = savedBrightness;
            getWindow().setAttributes(lp);
        }

        // Long Press Logic
        longPressRunnable = () -> {
            isLongPressing = true;
            webView.evaluateJavascript("if(document.getElementsByTagName('video')[0]) document.getElementsByTagName('video')[0].playbackRate = 2.0;", null);
            textCenterInfo.setText("2x Speed >>");
            textCenterInfo.setVisibility(View.VISIBLE);
        };

        // Get Data
        MediaItem media;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            media = getIntent().getSerializableExtra("media", MediaItem.class);
        } else {
            media = (MediaItem) getIntent().getSerializableExtra("media");
        }
        int season = getIntent().getIntExtra("season", 1);
        int episode = getIntent().getIntExtra("episode", 1);
        int serverIndex = getIntent().getIntExtra("server", 0);

        if (media == null) {
            finish();
            return;
        }

        setupWebView();
        loadVideo(media, season, episode, serverIndex);

        // History
        com.spytube.app.models.WatchHistoryManager.addToHistory(this, media);
    }

    private float startX, startY, lastY;
    private boolean isDragging = false;
    private final int TOUCH_SLOP = 50;
    // Only trigger swipe in the narrow edge strips (15% from each side)
    // This prevents our gesture from eating the player's own controls (play, seek bar, settings)
    private boolean isInSwipeZone = false;

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        float edgeZone = screenWidth * 0.15f; // 15% from each edge

        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            startX = event.getX();
            startY = event.getY();
            lastY = event.getY();
            isDragging = false;
            // Only allow swipe if touch starts in the narrow edge strips
            isInSwipeZone = (startX < edgeZone || startX > screenWidth - edgeZone);
            handler.postDelayed(longPressRunnable, 1000); // 1 second for 2x speed
            super.dispatchTouchEvent(event);
            return true;
        }

        if (event.getAction() == android.view.MotionEvent.ACTION_MOVE) {
            float deltaY = event.getY() - startY;
            float deltaX = event.getX() - startX;

            // Cancel long press on ANY movement (press + swipe should NOT trigger 2x)
            if (Math.abs(deltaY) > 10 || Math.abs(deltaX) > 10) {
                handler.removeCallbacks(longPressRunnable);
            }

            if (!isDragging) {
                // Only hijack swipe if we started in the LEFT edge zone (brightness only)
                if (isInSwipeZone && startX < edgeZone && Math.abs(deltaY) > TOUCH_SLOP && Math.abs(deltaY) > Math.abs(deltaX)) {
                    isDragging = true;

                    android.view.MotionEvent cancel = android.view.MotionEvent.obtain(event);
                    cancel.setAction(android.view.MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    
                    lastY = event.getY();
                    return true;
                }
            } else {
                 float delta = lastY - event.getY();
                 lastY = event.getY();
                 changeBrightness(delta);
                 return true;
            }
        }

        if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
            handler.removeCallbacks(longPressRunnable);
            if (isLongPressing) {
                isLongPressing = false;
                webView.evaluateJavascript("if(document.getElementsByTagName('video')[0]) document.getElementsByTagName('video')[0].playbackRate = 1.0;", null);
                textCenterInfo.setVisibility(View.GONE);
                return true;
            }
            if (isDragging) {
                isDragging = false;
                hideOverlays();
                return true;
            }
        }

        return super.dispatchTouchEvent(event); // Pass other events to WebView
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save Brightness
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (lp.screenBrightness != -1f) {
            prefs.edit().putFloat("brightness", lp.screenBrightness).apply();
        }
        
        // Sync Cookies (Web Settings)
        android.webkit.CookieManager.getInstance().flush();
    }

    private void changeBrightness(float deltaY) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float current = lp.screenBrightness;
        if (current == -1) current = 0.5f;

        // Smoother, finer control
        // Screen height ~1080px. 
        // We want full swipe to be maybe 80% change?
        float sensitivity = 1.5f; 
        float change = (deltaY / getResources().getDisplayMetrics().heightPixels) * sensitivity;
        
        current = Math.max(0.01f, Math.min(1.0f, current + change));

        lp.screenBrightness = current;
        getWindow().setAttributes(lp);

        showOverlay(layoutBrightness, viewBrightnessProgress, current);
    }

    private void changeVolume(float deltaY) {
        int currentConfig = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
        int max = maxVolume;
        
        // For volume, we can't do fractional updates to the system stream directly easily without custom UI showing "virtual" volume.
        // But we can improve the step logic.
        // Accumulate delta? Or just use a threshold that resets.
        
        // Let's use a "virtual" float volume for the progress bar, and snap system volume.
        
        // Simpler approach for now:
        // Use a threshold relative to screen height to trigger a 'step'.
        // 15 steps usually. 
        // So 1 step per ~6% of screen height drag.
        
        float stepThreshold = getResources().getDisplayMetrics().heightPixels / 30f; // More sensitive
        
        if (Math.abs(deltaY) > stepThreshold) {
             if (deltaY > 0) {
                 audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, 0);
             } else {
                 audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, 0);
             }
        }
        
        currentConfig = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
        float percent = (float) currentConfig / max;
        showOverlay(layoutVolume, viewVolumeProgress, percent);
    }

    private void showOverlay(View layout, View progressView, float percent) {
        layout.setVisibility(View.VISIBLE);
        layout.setAlpha(1f);
        
        layout.post(() -> {
            android.view.ViewGroup.LayoutParams params = progressView.getLayoutParams();
            params.height = (int) (layout.getHeight() * percent);
            progressView.setLayoutParams(params);
        });
        
        // Cancel only the hide-overlay runnable, not ALL callbacks (which would nuke longPressRunnable)
        if (hideOverlayRunnable != null) {
            handler.removeCallbacks(hideOverlayRunnable);
        }
    }

    private void hideOverlays() {
        hideOverlayRunnable = () -> {
            layoutBrightness.animate().alpha(0f).setDuration(300).withEndAction(() -> layoutBrightness.setVisibility(View.GONE));
            layoutVolume.animate().alpha(0f).setDuration(300).withEndAction(() -> layoutVolume.setVisibility(View.GONE));
        };
        handler.postDelayed(hideOverlayRunnable, 1000);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        // Enable cookies globally BEFORE configuring WebView
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true); // Needed for embedded player iframes

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true); 
        settings.setDatabaseEnabled(true);
        // Set explicit database path for localStorage/IndexedDB persistence
        settings.setDatabasePath(getApplicationContext().getDir("webview_db", MODE_PRIVATE).getPath());
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Allow file access for cached content
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        settings.setOffscreenPreRaster(true);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString().toLowerCase();
                // Allow navigation within whitelisted streaming domains
                if (url.contains("zxcstream.xyz") ||
                    url.contains("vidrock.net") ||
                    url.contains("vidlink.pro") ||
                    url.contains("rabbitstream") ||
                    url.contains("megacloud") ||
                    url.contains("rapid-cloud") ||
                    url.contains("dokicloud") ||
                    url.contains("mcloud.bz") ||
                    url.contains("e4.onhiflix")) {
                    return false;
                }
                // Block everything else (ad redirects)
                return true;
            }

            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString().toLowerCase();
                // Block known ad/tracker domains at the network level
                String[] adDomains = {
                    "doubleclick.net", "googlesyndication", "googleadservices",
                    "adservice.google", "pagead2.googlesyndication",
                    "popads.net", "popcash.net", "propellerads",
                    "adsterra", "exoclick", "juicyads", "trafficjunky",
                    "betterjtv", "adbrite", "adcolony",
                    "ads.yahoo", "ad.doubleclick", "adnxs.com",
                    "outbrain.com", "taboola.com", "mgid.com",
                    "revcontent.com", "content.ad",
                    "pushwoosh", "onesignal", "pushengage",
                    "counter.yadro", "mc.yandex",
                    "facebook.net/signals", "connect.facebook",
                    "analytics.google", "stats.g.doubleclick",
                    "disquscdn", "disqus.com",
                    "cdn.stickyadstv", "securepubads",
                    "imasdk.googleapis", "tpc.googlesyndication"
                };
                for (String ad : adDomains) {
                    if (url.contains(ad)) {
                        return new android.webkit.WebResourceResponse("text/plain", "UTF-8", 
                            new java.io.ByteArrayInputStream("".getBytes()));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                handler.postDelayed(() -> progressBar.setVisibility(View.GONE), 1000);

                // Heavy-duty ad removal: CSS + JS + Repeating Killer
                String adBlockScript = "javascript:(function(){" +
                    // 1. Aggressive CSS to hide all ad-related elements
                    "var s=document.createElement('style');" +
                    "s.textContent='" +
                    "iframe:not([src*=\"vidrock\"]):not([src*=\"zxcstream\"]):not([src*=\"vidlink\"]):not([src*=\"rabbitstream\"]):not([src*=\"megacloud\"]):not([src*=\"rapid-cloud\"]):not([src*=\"dokicloud\"]):not([src*=\"mcloud\"]):not([src*=\"onhiflix\"])," +
                    "div[class*=\"ad-\"],div[class*=\"ads-\"],div[class*=\"ad_\"],div[id*=\"ad-\"],div[id*=\"ad_\"]," +
                    "div[class*=\"popup\"],div[class*=\"modal\"]:not([class*=\"video\"]):not([class*=\"player\"])," +
                    "div[class*=\"overlay\"]:not([class*=\"video\"]):not([class*=\"player\"]):not([class*=\"control\"])," +
                    "div[class*=\"banner\"],div[class*=\"sponsor\"]," +
                    ".ad-container,.ads,.ad-banner,.ad-wrapper,.adsbygoogle," +
                    "a[href*=\"click\"][target=\"_blank\"],a[onclick*=\"window.open\"]," +
                    "div[style*=\"z-index: 999\"],div[style*=\"z-index:999\"]," +
                    "div[style*=\"z-index: 9999\"],div[style*=\"z-index:9999\"]," +
                    "div[style*=\"z-index: 99999\"],div[style*=\"z-index:99999\"]," +
                    "div[style*=\"position: fixed\"]:not([class*=\"video\"]):not([class*=\"player\"]):not([class*=\"control\"])" +
                    "{display:none!important;pointer-events:none!important;}';" +
                    "document.head.appendChild(s);" +
                    "setInterval(function(){" +
                    "document.querySelectorAll('iframe').forEach(function(f){" +
                    "var src=f.src||'';" +
                    "if(src&&!src.match(/vidrock|zxcstream|vidlink|rabbitstream|megacloud|rapid-cloud|dokicloud|mcloud|onhiflix|blob:/i)){" +
                    "f.remove();" +
                    "}" +
                    "});" +
                    // Remove high z-index overlays that aren't part of the video player
                    "document.querySelectorAll('div').forEach(function(d){" +
                    "var z=parseInt(window.getComputedStyle(d).zIndex)||0;" +
                    "if(z>9000&&!d.querySelector('video')&&!d.className.match(/player|video|control|progress/i)){" +
                    "d.remove();" +
                    "}" +
                    "});" +
                    "},2000);" +
                    "})()";
                view.evaluateJavascript(adBlockScript, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                return false; // Block actual popup window creation
            }

            @Override
            public android.graphics.Bitmap getDefaultVideoPoster() {
                // Return a 1x1 transparent bitmap instead of the ugly default grey play button
                return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress > 80) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void loadVideo(MediaItem media, int season, int episode, int serverIndex) {
        String url = buildApiUrl(media, season, episode, serverIndex);
        webView.loadUrl(url);
    }

    private String buildApiUrl(MediaItem media, int season, int episode, int serverIndex) {
        if (serverIndex == 0) {
            // Server 1: ZXCStream (Primary Default)
            String base = "https://zxcstream.xyz/player";
            String lang = "en";
            if (media.isTv()) {
                return base + "/tv/" + media.id + "/" + season + "/" + episode + "/" + lang + "?autoplay=true&back=true&server=1";
            } else {
                return base + "/movie/" + media.id + "/" + lang + "?autoplay=true&back=true&server=1";
            }
        } else if (serverIndex == 2) {
            // Server 3: VidLink (API player, same as others)
            String base = "https://vidlink.pro";
            if (media.isTv()) {
                return base + "/tv/" + media.id + "/" + season + "/" + episode + "?autoplay=true";
            } else {
                return base + "/movie/" + media.id + "?autoplay=true";
            }
        } else {
            // Server 2: VidRock
            String base = "https://vidrock.net";
            if (media.isTv()) {
                return base + "/tv/" + media.id + "/" + season + "/" + episode + "?autoplay=true&download=false";
            } else {
                return base + "/movie/" + media.id + "?autoplay=true&download=false";
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Prevent WebView memory leak
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            ((android.view.ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
