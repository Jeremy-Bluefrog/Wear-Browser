package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LocalBookmark(
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

class LocalStorage(context: Context) {
    private val prefs = context.getSharedPreferences("pixel_browser_local_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BOOKMARKS = "local_bookmarks"
    }

    @Synchronized
    fun saveBookmark(title: String, url: String): Boolean {
        if (url.isBlank()) return false
        val current = getAllBookmarks().toMutableList()
        // Prevent duplicate URLs
        current.removeAll { it.url == url }
        current.add(0, LocalBookmark(title.ifBlank { url }, url))
        
        return saveList(current)
    }

    @Synchronized
    fun deleteBookmark(url: String): Boolean {
        val current = getAllBookmarks().toMutableList()
        val removed = current.removeAll { it.url == url }
        if (removed) {
            return saveList(current)
        }
        return false
    }

    @Synchronized
    fun getAllBookmarks(): List<LocalBookmark> {
        val jsonString = prefs.getString(KEY_BOOKMARKS, null) ?: return getDefaultBookmarks()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<LocalBookmark>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LocalBookmark(
                        title = obj.optString("title", ""),
                        url = obj.optString("url", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultBookmarks()
        }
    }

    private fun saveList(list: List<LocalBookmark>): Boolean {
        return try {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("title", item.title)
                    put("url", item.url)
                    put("timestamp", item.timestamp)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_BOOKMARKS, jsonArray.toString()).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getDefaultBookmarks(): List<LocalBookmark> {
        // Pre-populate some useful default local bookmarks if empty
        return listOf(
            LocalBookmark("Google 搜尋", "https://www.google.com"),
            LocalBookmark("維基百科", "https://zh.wikipedia.org"),
            LocalBookmark("GitHub", "https://github.com")
        )
    }
}
