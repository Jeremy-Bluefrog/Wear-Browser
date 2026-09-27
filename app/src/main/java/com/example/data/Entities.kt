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
    tableName = "offline_pages",
    indices = [Index(value = ["url"], unique = true), Index(value = ["timestamp"])]
)
data class OfflinePage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val localPath: String,
    val textSnippet: String = "",
    val fileSize: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "downloaded_files",
    indices = [Index(value = ["timestamp"])]
)
data class DownloadedFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val localPath: String,
    val fileSize: Long,
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

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    val mimeType: String = "application/octet-stream",
    val progress: Int = 0, // 0..100
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedKbps: Float = 0f,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val timestamp: Long = System.currentTimeMillis(),
    val localPath: String? = null,
    val errorMessage: String? = null
)

data class StorageUsageInfo(
    val usedBytes: Long,
    val totalBytes: Long,
    val downloadsFolderBytes: Long
)


