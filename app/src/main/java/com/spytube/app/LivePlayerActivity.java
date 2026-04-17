package com.spytube.app;

import android.animation.ObjectAnimator;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.spytube.app.models.IptvChannel;

import java.util.HashMap;
import java.util.Map;

public class LivePlayerActivity extends AppCompatActivity {

    private static final String TAG = "LivePlayer";
    private ExoPlayer player;
    private PlayerView playerView;
    private LinearLayout errorLayout, topBar, centerControls;
    private TextView errorText, errorDetail, resizeLabel;
    private ImageButton btnPlayPause;
    private View scrim, liveDot;
    private IptvChannel channel;

    private static final int[] RESIZE_MODES = {
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
    };
    private static final String[] RESIZE_LABELS = {"Fit", "Zoom", "Stretch", "Fixed Width"};
    private int currentResizeIndex = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean controlsVisible = false;
    private final Runnable hideControlsRunnable = this::hideControls;
    private ObjectAnimator pulseAnimator;

    // Auto-retry on source errors
    private static final int MAX_RETRIES = 5;
    private int retryCount = 0;
    private boolean isRetrying = false;
    private DefaultHttpDataSource.Factory dataSourceFactory;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_live_player);
        } catch (Exception e) {
            Log.e(TAG, "Layout inflate failed", e);
            finish();
            return;
        }

        setupImmersiveMode();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        // Views
        playerView = findViewById(R.id.player_view);
        scrim = findViewById(R.id.scrim);
        topBar = findViewById(R.id.top_bar);
        centerControls = findViewById(R.id.center_controls);
        TextView channelName = findViewById(R.id.channel_name);
        errorLayout = findViewById(R.id.error_layout);
        errorText = findViewById(R.id.error_text);
        errorDetail = findViewById(R.id.error_detail);
        resizeLabel = findViewById(R.id.resize_label);
        liveDot = findViewById(R.id.live_dot);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnResize = findViewById(R.id.btn_resize);
        btnPlayPause = findViewById(R.id.btn_play_pause);

        // Live dot circular + pulse
        GradientDrawable dotShape = new GradientDrawable();
        dotShape.setShape(GradientDrawable.OVAL);
        dotShape.setColor(0xFFFF0000);
        liveDot.setBackground(dotShape);
        pulseAnimator = ObjectAnimator.ofFloat(liveDot, "alpha", 1f, 0.2f, 1f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.start();

        // Get channel
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                channel = (IptvChannel) getIntent().getSerializableExtra("channel", IptvChannel.class);
            } else {
                channel = (IptvChannel) getIntent().getSerializableExtra("channel");
            }
        } catch (Exception e) {
            Log.e(TAG, "Intent parse error", e);
        }

        if (channel == null) {
            Toast.makeText(this, "Channel data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        channelName.setText(channel.getName());

        // Button handlers
        btnBack.setOnClickListener(v -> finish());
        btnResize.setOnClickListener(v -> toggleResize());

        btnPlayPause.setOnClickListener(v -> {
            if (player != null) {
                if (player.isPlaying()) {
                    player.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    player.play();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
            scheduleHideControls();
        });


        // Tap to show/hide
        playerView.setOnClickListener(v -> toggleControls());

        // Build data source
        dataSourceFactory = buildDataSourceFactory();

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(5000, 30000, 1000, 2000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .build();

        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Error: " + error.errorCode + " | " + error.getMessage(), error);

                // On first try for parsing errors, attempt HLS fallback
                if (retryCount == 0 &&
                    (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                     error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED)) {
                    retryAsHls(dataSourceFactory);
                    retryCount++;
                    return;
                }

                // Auto-retry with exponential backoff
                if (retryCount < MAX_RETRIES) {
                    scheduleRetry();
                } else {
                    isRetrying = false;
                    showError("Stream Error",
                            (error.getMessage() != null ? error.getMessage() : "Stream may be offline")
                            + "\n\nAll retry attempts failed.");
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    retryCount = 0;
                    isRetrying = false;
                    errorLayout.setVisibility(View.GONE);
                    btnPlayPause.setImageResource(
                            player.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                } else if (state == Player.STATE_BUFFERING && isRetrying) {
                    showError("Retrying...",
                            "Attempt " + retryCount + " of " + MAX_RETRIES + "  •  Reconnecting...");
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setImageResource(
                        isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            }
        });

        try {
            player.setMediaItem(new MediaItem.Builder().setUri(channel.getStreamUrl()).build());
            player.prepare();
            player.setPlayWhenReady(true);
        } catch (Exception e) {
            showError("Stream Error", "Cannot load: " + e.getMessage());
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void toggleResize() {
        currentResizeIndex = (currentResizeIndex + 1) % RESIZE_MODES.length;
        playerView.setResizeMode(RESIZE_MODES[currentResizeIndex]);
        resizeLabel.setText(RESIZE_LABELS[currentResizeIndex]);
        resizeLabel.setVisibility(View.VISIBLE);
        resizeLabel.setAlpha(1f);
        handler.postDelayed(() ->
            resizeLabel.animate().alpha(0f).setDuration(500)
                .withEndAction(() -> resizeLabel.setVisibility(View.GONE)).start(), 1200);
        scheduleHideControls();
    }

    private void toggleControls() {
        if (controlsVisible) hideControls();
        else showControls();
    }

    private void showControls() {
        scrim.setVisibility(View.VISIBLE);
        topBar.setVisibility(View.VISIBLE);
        centerControls.setVisibility(View.VISIBLE);
        scrim.animate().alpha(1f).setDuration(200).start();
        topBar.animate().alpha(1f).setDuration(200).start();
        centerControls.animate().alpha(1f).setDuration(200).start();
        controlsVisible = true;
        scheduleHideControls();
    }

    private void hideControls() {
        scrim.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> scrim.setVisibility(View.GONE)).start();
        topBar.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> topBar.setVisibility(View.GONE)).start();
        centerControls.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> centerControls.setVisibility(View.GONE)).start();
        controlsVisible = false;
        handler.removeCallbacks(hideControlsRunnable);
    }

    private void scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable);
        handler.postDelayed(hideControlsRunnable, 4000);
    }

    @OptIn(markerClass = UnstableApi.class)
    private DefaultHttpDataSource.Factory buildDataSourceFactory() {
        DefaultHttpDataSource.Factory f = new DefaultHttpDataSource.Factory();
        f.setConnectTimeoutMs(30_000);
        f.setReadTimeoutMs(30_000);
        f.setAllowCrossProtocolRedirects(true);
        String ua = (channel.getUserAgent() != null && !channel.getUserAgent().isEmpty())
                ? channel.getUserAgent()
                : "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36";
        f.setUserAgent(ua);
        Map<String, String> headers = new HashMap<>();
        if (channel.getReferrer() != null && !channel.getReferrer().isEmpty()) {
            headers.put("Referer", channel.getReferrer());
            try {
                java.net.URI uri = new java.net.URI(channel.getReferrer());
                headers.put("Origin", uri.getScheme() + "://" + uri.getHost());
            } catch (Exception ignored) {}
        }
        if (!headers.isEmpty()) f.setDefaultRequestProperties(headers);
        return f;
    }

    @OptIn(markerClass = UnstableApi.class)
    private void retryAsHls(DefaultHttpDataSource.Factory dsFactory) {
        if (player == null || channel == null) return;
        try {
            HlsMediaSource src = new HlsMediaSource.Factory(dsFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(MediaItem.fromUri(channel.getStreamUrl()));
            player.setMediaSource(src);
            player.prepare();
            player.setPlayWhenReady(true);
        } catch (Exception e) {
            showError("Stream Error", "Unable to play this stream");
        }
    }

    
    private void scheduleRetry() {
        retryCount++;
        isRetrying = true;
        long delayMs = (long) (2000 * Math.pow(2, retryCount - 1));
        Log.d(TAG, "Scheduling retry " + retryCount + "/" + MAX_RETRIES + " in " + delayMs + "ms");

        showError("Retrying...",
                "Attempt " + retryCount + " of " + MAX_RETRIES + "  •  Waiting " + (delayMs / 1000) + "s...");

        handler.postDelayed(() -> {
            if (player == null || isFinishing() || isDestroyed()) return;
            try {
                Log.d(TAG, "Executing retry " + retryCount);
                player.stop();
                player.setMediaItem(new MediaItem.Builder().setUri(channel.getStreamUrl()).build());
                player.prepare();
                player.setPlayWhenReady(true);
            } catch (Exception e) {
                Log.e(TAG, "Retry failed", e);
                if (retryCount < MAX_RETRIES) {
                    scheduleRetry();
                } else {
                    isRetrying = false;
                    showError("Stream Error", "Unable to play this stream after " + MAX_RETRIES + " attempts.");
                }
            }
        }, delayMs);
    }

    private void showError(String title, String detail) {
        runOnUiThread(() -> {
            if (errorLayout != null) {
                errorLayout.setVisibility(View.VISIBLE);
                if (errorText != null) errorText.setText(title);
                if (errorDetail != null && detail != null) errorDetail.setText(detail);
            }
        });
    }

    private void setupImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override protected void onPause() { super.onPause(); if (player != null) player.pause(); }
    @Override protected void onResume() { super.onResume(); if (player != null) player.play(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (pulseAnimator != null) pulseAnimator.cancel();
        if (player != null) { player.release(); player = null; }
    }
}
