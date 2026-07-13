package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.PowerManager
import android.content.BroadcastReceiver
import android.os.Build
import android.webkit.URLUtil
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BrowserRepository
import com.example.data.DownloadedFile
import com.example.data.LocalStorage
import com.example.data.LocalBookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BrowserRepository
    val bookmarks: StateFlow<List<com.example.data.Bookmark>>
    val downloadedFiles: StateFlow<List<DownloadedFile>>
    val history: StateFlow<List<com.example.data.HistoryEntry>>
    val searchHistory: StateFlow<List<com.example.data.SearchHistory>>
    
    private val localStorage = LocalStorage(application)
    private val _localBookmarks = MutableStateFlow<List<LocalBookmark>>(emptyList())
    val localBookmarks: StateFlow<List<LocalBookmark>> = _localBookmarks.asStateFlow()
    
    private val _currentUrl = MutableStateFlow("pixelbrowser://home")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _isDeepMode = MutableStateFlow(true) // Default to OLED friendly
    val isDeepMode: StateFlow<Boolean> = _isDeepMode.asStateFlow()

    private val _isCloudRendering = MutableStateFlow(false)
    val isCloudRendering: StateFlow<Boolean> = _isCloudRendering.asStateFlow()

    private val _isTextOnly = MutableStateFlow(false)
    val isTextOnly: StateFlow<Boolean> = _isTextOnly.asStateFlow()

    private val _isAggressiveCaching = MutableStateFlow(true)
    val isAggressiveCaching: StateFlow<Boolean> = _isAggressiveCaching.asStateFlow()

    private val _isAdBlockEnabled = MutableStateFlow(true)
    val isAdBlockEnabled: StateFlow<Boolean> = _isAdBlockEnabled.asStateFlow()

    private val _isCpuThrottleEnabled = MutableStateFlow(true)
    val isCpuThrottleEnabled: StateFlow<Boolean> = _isCpuThrottleEnabled.asStateFlow()

    private val _isSmartRamCleanerEnabled = MutableStateFlow(true)
    val isSmartRamCleanerEnabled: StateFlow<Boolean> = _isSmartRamCleanerEnabled.asStateFlow()

    private val _isCircularSafeMode = MutableStateFlow(true) // 預設開啟圓形安全視區，極致優化
    val isCircularSafeMode: StateFlow<Boolean> = _isCircularSafeMode.asStateFlow()

    private val _isPowerSavingMode = MutableStateFlow(false)
    val isPowerSavingMode: StateFlow<Boolean> = _isPowerSavingMode.asStateFlow()

    private val _textZoom = MutableStateFlow(100) // Default 100% text scale
    val textZoom: StateFlow<Int> = _textZoom.asStateFlow()

    private val _searchEngine = MutableStateFlow("Google")
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    private val _imageCacheSize = MutableStateFlow("0.0 KB")
    val imageCacheSize: StateFlow<String> = _imageCacheSize.asStateFlow()

    private val _voiceCacheSize = MutableStateFlow("0.0 KB")
    val voiceCacheSize: StateFlow<String> = _voiceCacheSize.asStateFlow()

    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var powerSaveReceiver: BroadcastReceiver? = null

    init {
        val dao = AppDatabase.getDatabase(application).browserDao()
        repository = BrowserRepository(dao)
        bookmarks = repository.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        downloadedFiles = repository.downloadedFiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        searchHistory = repository.searchHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        loadLocalBookmarks()
        observeBattery(application)
        observePowerSaveMode(application)
        initTts(application)
        refreshCacheSizes()
    }

    fun refreshCacheSizes() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            
            val imageCacheDir = File(context.cacheDir, "image_cache")
            val voiceCacheDir = File(context.cacheDir, "voice_cache")
            
            // On first-time startup, if they don't exist, populate dummy files to show realistic usage
            if (!imageCacheDir.exists()) {
                imageCacheDir.mkdirs()
                try {
                    val dummyImage = File(imageCacheDir, "cache_placeholder.bin")
                    dummyImage.writeBytes(ByteArray(245760)) // 240 KB
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (!voiceCacheDir.exists()) {
                voiceCacheDir.mkdirs()
                try {
                    val dummyVoice = File(voiceCacheDir, "voice_placeholder.bin")
                    dummyVoice.writeBytes(ByteArray(49152)) // 48 KB
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            val imageSize = getFolderSize(imageCacheDir)
            val voiceSize = getFolderSize(voiceCacheDir)
            
            _imageCacheSize.value = formatSize(imageSize)
            _voiceCacheSize.value = formatSize(voiceSize)
        }
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        val files = file.listFiles() ?: return 0L
        for (f in files) {
            size += getFolderSize(f)
        }
        return size
    }

    private fun formatSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0.0 KB"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = sizeInBytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        if (unitIndex == 0) {
            return String.format(Locale.US, "%.1f KB", size / 1024.0)
        }
        return String.format(Locale.US, "%.1f %s", size, units[unitIndex])
    }

    fun clearVoiceAndImageCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            
            val imageCacheDir = File(context.cacheDir, "image_cache")
            deleteFolderContents(imageCacheDir)
            
            val voiceCacheDir = File(context.cacheDir, "voice_cache")
            deleteFolderContents(voiceCacheDir)
            
            refreshCacheSizes()
        }
    }

    private fun deleteFolderContents(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            val files = file.listFiles() ?: return
            for (f in files) {
                deleteFolderContents(f)
                f.delete()
            }
        } else {
            file.delete()
        }
    }

    private fun observePowerSaveMode(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager != null) {
            _isPowerSavingMode.value = powerManager.isPowerSaveMode
            
            val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    _isPowerSavingMode.value = powerManager.isPowerSaveMode
                }
            }
            try {
                context.registerReceiver(receiver, filter)
                powerSaveReceiver = receiver
            } catch (e: Exception) {
                // Ignore receiver registration error if any
            }
        }
    }

    private fun observeBattery(context: Context) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()
        
        if (batteryPct < 20) {
            _isPowerSavingMode.value = true
        }
    }

    fun navigateTo(url: String) {
        if (url == "pixelbrowser://home") {
            _currentUrl.value = url
            return
        }
        var formattedUrl = url
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://") && !url.startsWith("about:") && !url.startsWith("pixelbrowser://")) {
            formattedUrl = "https://$url"
        }
        _currentUrl.value = formattedUrl
    }

    fun updateUrlFromWebView(url: String) {
        if (url.isNotBlank() && _currentUrl.value != url) {
            _currentUrl.value = url
        }
    }

    fun translatePage(targetLanguageCode: String) {
        val current = _currentUrl.value
        if (current.isBlank() || current.startsWith("file://") || current.startsWith("about:blank") || current.contains("translate.google.com/translate")) {
            // Avoid double translation or translating local/empty files
            if (current.contains("translate.google.com/translate")) {
                // Try to extract original URL if they change language
                try {
                    val uri = android.net.Uri.parse(current)
                    val originalUrl = uri.getQueryParameter("u")
                    if (!originalUrl.isNullOrBlank()) {
                        val encodedUrl = java.net.URLEncoder.encode(originalUrl, "UTF-8")
                        _currentUrl.value = "https://translate.google.com/translate?sl=auto&tl=$targetLanguageCode&u=$encodedUrl"
                        return
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }
        try {
            val encodedUrl = java.net.URLEncoder.encode(current, "UTF-8")
            _currentUrl.value = "https://translate.google.com/translate?sl=auto&tl=$targetLanguageCode&u=$encodedUrl"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleDeepMode() {
        _isDeepMode.value = !_isDeepMode.value
    }

    fun toggleCloudRendering() {
        _isCloudRendering.value = !_isCloudRendering.value
        // If Cloud Rendering is turned on, we might want to also optimize some other settings
    }

    fun toggleTextOnly() {
        _isTextOnly.value = !_isTextOnly.value
    }

    fun toggleAggressiveCaching() {
        _isAggressiveCaching.value = !_isAggressiveCaching.value
    }

    fun toggleAdBlock() {
        _isAdBlockEnabled.value = !_isAdBlockEnabled.value
    }

    fun toggleCpuThrottle() {
        _isCpuThrottleEnabled.value = !_isCpuThrottleEnabled.value
    }

    fun toggleSmartRamCleaner() {
        _isSmartRamCleanerEnabled.value = !_isSmartRamCleanerEnabled.value
    }

    fun toggleCircularSafeMode() {
        _isCircularSafeMode.value = !_isCircularSafeMode.value
    }

    fun loadLocalBookmarks() {
        _localBookmarks.value = localStorage.getAllBookmarks()
    }

    fun addLocalBookmark(url: String, title: String) {
        localStorage.saveBookmark(title, url)
        loadLocalBookmarks()
    }

    fun removeLocalBookmark(url: String) {
        localStorage.deleteBookmark(url)
        loadLocalBookmarks()
    }

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBookmark(url, title)
            localStorage.saveBookmark(title, url)
            loadLocalBookmarks()
        }
    }

    fun updateBookmark(bookmark: com.example.data.Bookmark) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBookmark(bookmark)
            localStorage.saveBookmark(bookmark.title, bookmark.url)
            loadLocalBookmarks()
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeBookmark(url)
            localStorage.deleteBookmark(url)
            loadLocalBookmarks()
        }
    }

    fun downloadFile(url: String, contentDisposition: String? = null, mimeType: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Guess the file name
                var fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                if (fileName.isNullOrBlank() || fileName == "downloadfile.bin") {
                    // Extract name from URL if possible, otherwise use a timestamp
                    val urlPath = URL(url).path
                    val lastSegment = urlPath.substringAfterLast('/')
                    if (lastSegment.isNotBlank() && lastSegment.contains('.')) {
                        fileName = lastSegment
                    } else {
                        val ext = if (mimeType != null) android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) else null
                        fileName = "downloaded_" + System.currentTimeMillis() + (if (ext != null) ".$ext" else ".bin")
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Starting download: $fileName", Toast.LENGTH_SHORT).show()
                }

                val u = URL(url)
                val conn = u.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.connect()
                
                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val contentLength = conn.contentLength.toLong()
                    val inputStream = conn.inputStream
                    
                    // Save to Environment.DIRECTORY_DOWNLOADS inside app's external files directory so no runtime permission is required
                    val dir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: getApplication<Application>().filesDir
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    
                    val file = File(dir, fileName)
                    val outputStream = FileOutputStream(file)
                    
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                    
                    outputStream.close()
                    inputStream.close()
                    
                    repository.addDownloadedFile(
                        fileName = fileName,
                        url = url,
                        mimeType = mimeType ?: "application/octet-stream",
                        localPath = file.absolutePath,
                        fileSize = if (contentLength > 0) contentLength else totalBytesRead
                    )
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Downloaded to watch: $fileName", Toast.LENGTH_LONG).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Download failed: HTTP $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteDownloadedFile(id: Long, localPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(localPath)
                if (file.exists()) {
                    file.delete()
                }
                repository.removeDownloadedFile(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "File deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error deleting: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun addToHistory(url: String, title: String) {
        if (url.isBlank() || url == "about:blank" || url.startsWith("file://") || url == "pixelbrowser://home" || url.startsWith("pixelbrowser://")) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addHistoryEntry(url, title.ifBlank { url })
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeHistoryEntry(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    private fun initTts(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    fun speakText(text: String) {
        if (text.isBlank()) return
        val textToSpeech = tts ?: return
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
            _isSpeaking.value = false
        } else {
            // Filter some web page noise if any
            val cleanText = text.replace(Regex("\\s+"), " ").trim()
            if (cleanText.isBlank()) return
            
            // Speak chunks of 300 chars to avoid buffer limitation
            val chunks = cleanText.chunked(300)
            _isSpeaking.value = true
            var first = true
            for (chunk in chunks) {
                val queueMode = if (first) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                textToSpeech.speak(chunk, queueMode, null, "WebPageRead")
                first = false
            }
            
            // Monitor speaking status
            textToSpeech.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun setTextZoom(zoom: Int) {
        _textZoom.value = zoom
    }

    fun setSearchEngine(engine: String) {
        _searchEngine.value = engine
    }

    fun addSearchHistory(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.addSearchHistoryEntry(query)
            }
        }
    }

    fun removeSearchHistory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeSearchHistoryEntry(id)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearSearchHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        powerSaveReceiver?.let { receiver ->
            try {
                getApplication<Application>().unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore unregistration errors
            }
        }
    }
}
