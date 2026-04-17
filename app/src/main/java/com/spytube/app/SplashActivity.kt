package com.spytube.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.spytube.app.models.ContentPreloader


class SplashActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var videoFinished = false
    private var dataReady = false
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        )
        super.onCreate(savedInstanceState)

        // Simple Fullscreen flags
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        setContentView(R.layout.activity_splash)

        // Start preloading
        ContentPreloader.preload(this) {
            runOnUiThread {
                dataReady = true
                tryLaunch()
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setupVideoView()
        } else {
            setupExoPlayer()
        }
    }
    private fun setupVideoView() {
        val videoView = findViewById<android.widget.VideoView>(R.id.splash_video_view)
        videoView.visibility = View.VISIBLE
        findViewById<View>(R.id.splash_player_view).visibility = View.GONE

        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.splash_video}")
        videoView.setVideoURI(videoUri)

        videoView.setOnCompletionListener {
            videoFinished = true
            tryLaunch()
        }

        videoView.setOnErrorListener { _, _, _ ->
            videoFinished = true
            tryLaunch()
            true
        }

        videoView.start()

        // Safety timeout (2.5s)
        videoView.postDelayed({
            if (!isFinishing) {
                videoFinished = true
                dataReady = true
                tryLaunch()
            }
        }, 2500)
    }

    private fun setupExoPlayer() {
        val playerView = findViewById<PlayerView>(R.id.splash_player_view)
        playerView.visibility = View.VISIBLE
        findViewById<View>(R.id.splash_video_view).visibility = View.GONE

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.splash_video}")
        val mediaItem = MediaItem.fromUri(videoUri)

        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        videoFinished = true
                        tryLaunch()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    videoFinished = true
                    tryLaunch()
                }
            })
        }

        // Safety timeout (2.5s)
        playerView.postDelayed({
            if (!isFinishing) {
                videoFinished = true
                dataReady = true // Force ready on timeout
                tryLaunch()
            }
        }, 2500)
    }

    private fun tryLaunch() {
        if (launched) return
        if (!videoFinished && !dataReady) return // Wait for at least one signal? No, both
        // Logic: Wait for video OR timeout. And wait for Preload OR timeout.

        // Actually, we want to wait for BOTH video and data, unless timeout forced them.
        if (!videoFinished || !dataReady) return

        launched = true

        player?.release()
        player = null

        startActivity(Intent(this, MainActivity::class.java))
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
