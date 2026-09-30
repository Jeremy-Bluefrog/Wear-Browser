package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val dao: BrowserDao) {
    val bookmarks: Flow<List<Bookmark>> = dao.getAllBookmarks()
    val history: Flow<List<HistoryEntry>> = dao.getAllHistory()
    val searchHistory: Flow<List<SearchHistory>> = dao.getAllSearchHistory()

    suspend fun addBookmark(url: String, title: String) {
        dao.insertBookmark(Bookmark(url = url, title = title.ifBlank { url }))
    }

    suspend fun updateBookmark(bookmark: Bookmark) {
        dao.updateBookmark(bookmark)
    }

    suspend fun removeBookmark(url: String) {
        dao.deleteBookmark(url)
    }

    suspend fun addHistoryEntry(url: String, title: String) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            dao.insertHistoryEntry(HistoryEntry(url = url, title = title.ifBlank { url }))
        }
    }

    suspend fun removeHistoryEntry(id: Long) {
        dao.deleteHistoryEntry(id)
    }

    suspend fun clearHistory() {
        dao.clearAllHistory()
    }

    suspend fun addSearchHistoryEntry(query: String) {
        if (query.isNotBlank()) {
            dao.insertSearchHistory(SearchHistory(query = query.trim()))
        }
    }

    suspend fun removeSearchHistoryEntry(id: Long) {
        dao.deleteSearchHistoryEntry(id)
    }

    suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }
}
