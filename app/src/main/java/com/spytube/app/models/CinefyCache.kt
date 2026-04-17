package com.spytube.app.models

import android.content.Context
import android.content.SharedPreferences


object CinefyCache {
    private const val PREFS_NAME = "CinefyCache"
    private const val PREFS_RESUME = "CinefyResume"
    
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private fun resumePrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_RESUME, Context.MODE_PRIVATE)
    
    
    @JvmStatic
    fun save(context: Context, title: String, provider: String, mediaId: String, isTv: Boolean, season: Int = 0, episode: Int = 0) {
        val key = title.lowercase().trim()
        prefs(context).edit().putString(key, "$provider|$mediaId|$isTv|$season|$episode").apply()
    }
    
    
    @JvmStatic
    fun lookup(context: Context, title: String): Map<String, Any>? {
        val key = title.lowercase().trim()
        val value = prefs(context).getString(key, null) ?: return null
        val parts = value.split("|")
        if (parts.size < 3) return null
        return mapOf(
            "provider" to parts[0],
            "mediaId" to parts[1],
            "isTv" to parts[2].toBoolean(),
            "season" to (parts.getOrNull(3)?.toIntOrNull() ?: 0),
            "episode" to (parts.getOrNull(4)?.toIntOrNull() ?: 0)
        )
    }
    
    
    @JvmStatic
    fun savePosition(context: Context, title: String, position: Double) {
        val key = title.lowercase().trim()
        resumePrefs(context).edit().putFloat(key, position.toFloat()).apply()
    }
    
    
    @JvmStatic
    fun getPosition(context: Context, title: String): Float {
        val key = title.lowercase().trim()
        return resumePrefs(context).getFloat(key, 0f)
    }
    
    
    @JvmStatic
    fun savePoster(context: Context, title: String, posterUrl: String) {
        val key = "poster_" + title.lowercase().trim()
        prefs(context).edit().putString(key, posterUrl).apply()
    }
    
    
    @JvmStatic
    fun getPoster(context: Context, title: String): String? {
        val key = "poster_" + title.lowercase().trim()
        return prefs(context).getString(key, null)
    }
}
