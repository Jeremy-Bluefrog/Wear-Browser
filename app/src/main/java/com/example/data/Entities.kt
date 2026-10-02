package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["timestamp"])]
)
data class Bookmark(
    @PrimaryKey val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "history_entries",
    indices = [Index(value = ["timestamp"])]
)
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["timestamp"])]
)
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "offline_articles",
    indices = [Index(value = ["timestamp"])]
)
data class OfflineArticle(
    @PrimaryKey val url: String,
    val title: String,
    val content: String,
    val readingTimeMinutes: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
