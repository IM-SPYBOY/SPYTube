package com.spytube.app.models

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


object FavoritesManager {
    private const val PREFS_NAME = "SPYTubeFavorites"
    private const val KEY_FAVORITES = "favorites_list"
    private val gson = Gson()

    // In-memory cache to avoid re-parsing JSON on every isFavorite() check
    private var cachedList: List<MediaItem>? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getList(context: Context): List<MediaItem> {
        cachedList?.let { return it }
        val json = getPrefs(context).getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MediaItem>>() {}.type
            val list: List<MediaItem> = gson.fromJson(json, type) ?: emptyList()
            cachedList = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addToList(context: Context, item: MediaItem) {
        val list = getList(context).toMutableList()
        // Don't add duplicates
        if (list.none { it.id == item.id }) {
            list.add(0, item) // Add to front
            saveList(context, list)
        }
    }

    fun removeFromList(context: Context, id: Int) {
        val list = getList(context).toMutableList()
        list.removeAll { it.id == id }
        saveList(context, list)
    }

    fun isFavorite(context: Context, id: Int): Boolean {
        return getList(context).any { it.id == id }
    }

    fun toggleFavorite(context: Context, item: MediaItem): Boolean {
        return if (isFavorite(context, item.id)) {
            removeFromList(context, item.id)
            false
        } else {
            addToList(context, item)
            true
        }
    }

    private fun saveList(context: Context, list: List<MediaItem>) {
        cachedList = list
        val json = gson.toJson(list)
        getPrefs(context).edit().putString(KEY_FAVORITES, json).apply()
    }
}
