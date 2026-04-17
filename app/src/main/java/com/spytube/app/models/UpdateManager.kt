package com.spytube.app.models

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.io.File


object UpdateManager {
    private const val TAG = "UpdateManager"

    private const val KEY_LATEST_VERSION = "latest_version_code"
    private const val KEY_LATEST_NAME = "latest_version_name"
    private const val KEY_UPDATE_URL = "update_url"
    private const val KEY_FORCE_MSG = "force_update_message"
    private const val KEY_IS_FORCE = "is_force_update" // New boolean toggle

    private const val DEFAULT_URL = "https://github.com/IM-SPYBOY/SPYTube/releases/latest/download/SPYTube.apk"
    private const val WEBSITE_URL = "https://spytube.in"

    enum class UpdateState { NO_UPDATE, OPTIONAL_UPDATE, FORCE_UPDATE }

    data class UpdateInfo(
        val state: UpdateState,
        val latestVersionName: String = "",
        val updateUrl: String = DEFAULT_URL,
        val forceMessage: String = ""
    )

    fun checkForUpdate(context: Context, onResult: (UpdateInfo) -> Unit) {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // Instant fetch for testing
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set defaults
        remoteConfig.setDefaultsAsync(mapOf(
            KEY_LATEST_VERSION to 1L,
            KEY_LATEST_NAME to "1.0",
            KEY_UPDATE_URL to DEFAULT_URL,
            KEY_FORCE_MSG to "This version is no longer supported. Please update to continue using SPYTube.",
            KEY_IS_FORCE to false
        ))

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get version as String (e.g. "1.3" or "2.0")
                val latestVersionReq = remoteConfig.getString(KEY_LATEST_VERSION)
                val latestName = remoteConfig.getString(KEY_LATEST_NAME)
                val isForce = remoteConfig.getBoolean(KEY_IS_FORCE)
                var updateUrl = remoteConfig.getString(KEY_UPDATE_URL)
                
                if (updateUrl.isEmpty() || !updateUrl.startsWith("http")) {
                    updateUrl = DEFAULT_URL
                }
                val forceMsg = remoteConfig.getString(KEY_FORCE_MSG)

                val currentVersionName = getAppVersionName(context)
                Log.d(TAG, "Current: $currentVersionName, Latest: $latestVersionReq, Force: $isForce")

                val updateAvailable = isUpdateAvailable(currentVersionName, latestVersionReq)

                val state = when {
                    !updateAvailable -> UpdateState.NO_UPDATE
                    isForce -> UpdateState.FORCE_UPDATE
                    else -> UpdateState.OPTIONAL_UPDATE
                }

                onResult(UpdateInfo(state, latestName, updateUrl, forceMsg))
            } else {
                Log.e(TAG, "Remote config fetch failed", task.exception)
                onResult(UpdateInfo(UpdateState.NO_UPDATE))
            }
        }
    }

    private fun isUpdateAvailable(current: String, latest: String): Boolean {
        if (latest.isBlank()) return false
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until length) {
            val c = if (i < currentParts.size) currentParts[i] else 0
            val l = if (i < latestParts.size) latestParts[i] else 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun downloadAndInstall(context: Context, url: String, onDownloadFinished: (Boolean) -> Unit) {
        // Validate URL before passing to DownloadManager
        val safeUrl = if (url.startsWith("http")) url else DEFAULT_URL

        try {
            android.widget.Toast.makeText(context, "Downloading update...", android.widget.Toast.LENGTH_SHORT).show()

            val fileName = "SPYTube-update.apk"
            val subPath = "updates/$fileName"
            
            // Clean up old file
            val file = File(context.getExternalFilesDir(null), subPath)
            if (file.exists()) file.delete()

            val request = DownloadManager.Request(Uri.parse(safeUrl))
                .setTitle("SPYTube Update")
                .setDescription("Downloading latest version...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, null, subPath)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = manager.enqueue(request)

            // Listen for download completion
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        ctx.unregisterReceiver(this)
                        
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = manager.query(query)
                        
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = cursor.getInt(statusIndex)
                            
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                // Verify file exists
                                val downloadFile = File(context.getExternalFilesDir(null), subPath)
                                if (downloadFile.exists()) {
                                    installApk(ctx, downloadFile)
                                    onDownloadFinished(true)
                                } else {
                                    android.widget.Toast.makeText(ctx, "Error: File missing after success", android.widget.Toast.LENGTH_LONG).show()
                                    onDownloadFinished(false)
                                }
                            } else {
                                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                val reason = cursor.getInt(reasonIndex)
                                Log.e(TAG, "Download failed. Status: $status, Reason: $reason")
                                android.widget.Toast.makeText(ctx, "Download failed. Error Code: $reason", android.widget.Toast.LENGTH_LONG).show()
                                onDownloadFinished(false)
                            }
                        } else {
                             android.widget.Toast.makeText(ctx, "Download status unknown", android.widget.Toast.LENGTH_SHORT).show()
                             onDownloadFinished(false)
                        }
                        cursor.close()
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            android.widget.Toast.makeText(context, "Download failed.", android.widget.Toast.LENGTH_LONG).show()
            onDownloadFinished(false)
            // Fallback removed as per user request
        }
    }

    fun openWebsite(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE_URL))
        context.startActivity(intent)
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun getAppVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version code", e)
            1
        }
    }

    private fun getAppVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version name", e)
            "1.0"
        }
    }
}
