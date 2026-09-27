package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val dao: BrowserDao) {
    val bookmarks: Flow<List<Bookmark>> = dao.getAllBookmarks()
    val offlinePages: Flow<List<OfflinePage>> = dao.getAllOfflinePages()
    val downloadedFiles: Flow<List<DownloadedFile>> = dao.getAllDownloadedFiles()
    val history: Flow<List<HistoryEntry>> = dao.getAllHistory()
    val searchHistory: Flow<List<SearchHistory>> = dao.getAllSearchHistory()

    suspend fun getOfflinePageByUrl(url: String): OfflinePage? {
        return dao.getOfflinePageByUrl(url)
    }

    suspend fun saveOfflinePage(url: String, title: String, localPath: String, textSnippet: String, fileSize: Long) {
        dao.insertOfflinePage(
            OfflinePage(
                url = url,
                title = title.ifBlank { url },
                localPath = localPath,
                textSnippet = textSnippet,
                fileSize = fileSize
            )
        )
    }

    suspend fun removeOfflinePage(id: Long) {
        dao.deleteOfflinePage(id)
    }

    suspend fun removeOfflinePageByUrl(url: String) {
        dao.deleteOfflinePageByUrl(url)
    }

    suspend fun clearAllOfflinePages() {
        dao.clearAllOfflinePages()
    }

    suspend fun addBookmark(url: String, title: String) {
        dao.insertBookmark(Bookmark(url, title))
    }

    suspend fun updateBookmark(bookmark: Bookmark) {
        dao.updateBookmark(bookmark)
    }

    suspend fun removeBookmark(url: String) {
        dao.deleteBookmark(url)
    }

    suspend fun addDownloadedFile(fileName: String, url: String, mimeType: String, localPath: String, fileSize: Long) {
        dao.insertDownloadedFile(
            DownloadedFile(
                fileName = fileName,
                url = url,
                mimeType = mimeType,
                localPath = localPath,
                fileSize = fileSize
            )
        )
    }

    suspend fun removeDownloadedFile(id: Long) {
        dao.deleteDownloadedFile(id)
    }

    suspend fun addHistoryEntry(url: String, title: String) {
        dao.insertHistoryEntry(HistoryEntry(url = url, title = title))
    }

    suspend fun removeHistoryEntry(id: Long) {
        dao.deleteHistoryEntry(id)
    }

    suspend fun clearHistory() {
        dao.clearAllHistory()
    }

    suspend fun addSearchHistoryEntry(query: String) {
        dao.insertSearchHistory(SearchHistory(query = query))
    }

    suspend fun removeSearchHistoryEntry(id: Long) {
        dao.deleteSearchHistoryEntry(id)
    }

    suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }
}
