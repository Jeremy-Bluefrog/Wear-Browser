package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Update
    suspend fun updateBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmark(url: String)

    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC LIMIT 300")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: HistoryEntry)

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteHistoryEntry(id: Long)

    @Query("DELETE FROM history_entries")
    suspend fun clearAllHistory()

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllSearchHistory(): Flow<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(entry: SearchHistory)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchHistoryEntry(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    @Query("SELECT * FROM offline_articles ORDER BY timestamp DESC")
    fun getAllOfflineArticles(): Flow<List<OfflineArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineArticle(article: OfflineArticle)

    @Query("DELETE FROM offline_articles WHERE url = :url")
    suspend fun deleteOfflineArticle(url: String)

    @Query("DELETE FROM offline_articles")
    suspend fun clearAllOfflineArticles()

    @Query("SELECT * FROM offline_articles WHERE url = :url LIMIT 1")
    suspend fun getOfflineArticle(url: String): OfflineArticle?
}
