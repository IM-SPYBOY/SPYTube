package com.spytube.app.models

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


object WatchHistoryManager {
    private const val PREFS_NAME = "SPYTubeHistory"
    private const val KEY_HISTORY = "watch_history"
    private const val MAX_HISTORY = 50
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getHistory(context: Context): List<MediaItem> {
        val json = getPrefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MediaItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    
    @JvmStatic
    fun addToHistory(context: Context, item: MediaItem) {
        val list = getHistory(context).toMutableList()
        // Remove if already exists (will re-add to front)
        list.removeAll { it.id == item.id }
        list.add(0, item) // Most recent first
        // Cap history size
        val capped = if (list.size > MAX_HISTORY) list.take(MAX_HISTORY) else list
        saveHistory(context, capped)
    }

    @JvmStatic
    fun removeFromHistory(context: Context, id: Int) {
        val list = getHistory(context).toMutableList()
        list.removeAll { it.id == id }
        saveHistory(context, list)
    }

    @JvmStatic
    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(context: Context, list: List<MediaItem>) {
        val json = gson.toJson(list)
        getPrefs(context).edit().putString(KEY_HISTORY, json).apply()
    }
}
