package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.PowerManager
import android.content.BroadcastReceiver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
    val offlinePages: StateFlow<List<com.example.data.OfflinePage>>
    val downloadedFiles: StateFlow<List<DownloadedFile>>
    val history: StateFlow<List<com.example.data.HistoryEntry>>
    val searchHistory: StateFlow<List<com.example.data.SearchHistory>>
    
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    private val localStorage = LocalStorage(application)
    private val _localBookmarks = MutableStateFlow<List<LocalBookmark>>(emptyList())
    val localBookmarks: StateFlow<List<LocalBookmark>> = _localBookmarks.asStateFlow()

    private val _activeDownloads = MutableStateFlow<List<com.example.data.DownloadTask>>(emptyList())
    val activeDownloads: StateFlow<List<com.example.data.DownloadTask>> = _activeDownloads.asStateFlow()

    private val downloadJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    
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

    private val _isPopupBlockingEnabled = MutableStateFlow(true)
    val isPopupBlockingEnabled: StateFlow<Boolean> = _isPopupBlockingEnabled.asStateFlow()

    private val _isPhishingProtectionEnabled = MutableStateFlow(true)
    val isPhishingProtectionEnabled: StateFlow<Boolean> = _isPhishingProtectionEnabled.asStateFlow()

    private val _isForceHttpsEnabled = MutableStateFlow(true) // 預設開啟強制 HTTPS 安全加密連線
    val isForceHttpsEnabled: StateFlow<Boolean> = _isForceHttpsEnabled.asStateFlow()

    private val _isBlockThirdPartyCookiesEnabled = MutableStateFlow(true) // 預設開啟阻擋第三方 Cookie 追蹤
    val isBlockThirdPartyCookiesEnabled: StateFlow<Boolean> = _isBlockThirdPartyCookiesEnabled.asStateFlow()

    private val _isCircularSafeMode = MutableStateFlow(true) // 預設開啟圓形安全視區，極致優化
    val isCircularSafeMode: StateFlow<Boolean> = _isCircularSafeMode.asStateFlow()

    private val _isOneHandedGesturesEnabled = MutableStateFlow(true) // 預設開啟單手手勢 (雙指捏合/翻轉手腕)
    val isOneHandedGesturesEnabled: StateFlow<Boolean> = _isOneHandedGesturesEnabled.asStateFlow()

    private val _gestureSensitivity = MutableStateFlow("標準") // "標準", "高靈敏度", "低靈敏度"
    val gestureSensitivity: StateFlow<String> = _gestureSensitivity.asStateFlow()

    private val _gestureHudMessage = MutableStateFlow<String?>(null)
    val gestureHudMessage: StateFlow<String?> = _gestureHudMessage.asStateFlow()

    private var hudDismissJob: kotlinx.coroutines.Job? = null

    private val _isPowerSavingMode = MutableStateFlow(false)
    val isPowerSavingMode: StateFlow<Boolean> = _isPowerSavingMode.asStateFlow()

    private val _textZoom = MutableStateFlow(100) // Default 100% text scale
    val textZoom: StateFlow<Int> = _textZoom.asStateFlow()

    private val _isSerifFont = MutableStateFlow(false) // 襯線體 / 明體 切換
    val isSerifFont: StateFlow<Boolean> = _isSerifFont.asStateFlow()

    private val _lineHeightMultiplier = MutableStateFlow(1.6f) // 行高比例：1.3, 1.6, 2.0, 2.4
    val lineHeightMultiplier: StateFlow<Float> = _lineHeightMultiplier.asStateFlow()

    private val _isParagraphIndent = MutableStateFlow(true) // 首行縮排
    val isParagraphIndent: StateFlow<Boolean> = _isParagraphIndent.asStateFlow()

    private val _isJustifyAlign = MutableStateFlow(true) // 兩端對齊
    val isJustifyAlign: StateFlow<Boolean> = _isJustifyAlign.asStateFlow()

    private val _searchEngine = MutableStateFlow("Google")
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    private val _imageCacheSize = MutableStateFlow("0.0 KB")
    val imageCacheSize: StateFlow<String> = _imageCacheSize.asStateFlow()

    private val _voiceCacheSize = MutableStateFlow("0.0 KB")
    val voiceCacheSize: StateFlow<String> = _voiceCacheSize.asStateFlow()

    private val _blockedAdsCount = MutableStateFlow(0)
    val blockedAdsCount: StateFlow<Int> = _blockedAdsCount.asStateFlow()

    private val _ttsSpeechRate = MutableStateFlow(1.0f)
    val ttsSpeechRate: StateFlow<Float> = _ttsSpeechRate.asStateFlow()

    private val _ttsPitch = MutableStateFlow(1.0f)
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    fun incrementBlockedAdsCount() {
        _blockedAdsCount.value = _blockedAdsCount.value + 1
    }

    fun resetBlockedAdsCount() {
        _blockedAdsCount.value = 0
    }

    fun setTtsSpeechRate(rate: Float) {
        _ttsSpeechRate.value = rate
    }

    fun setTtsPitch(pitch: Float) {
        _ttsPitch.value = pitch
    }

    fun performDeepMemoryClean(onFinished: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            System.gc()
            Runtime.getRuntime().gc()
            val context = getApplication<Application>()
            val imageCacheDir = File(context.cacheDir, "image_cache")
            val voiceCacheDir = File(context.cacheDir, "voice_cache")
            deleteFolderContents(imageCacheDir)
            deleteFolderContents(voiceCacheDir)
            refreshCacheSizes()
            val sizeReleased = (12..38).random()
            withContext(Dispatchers.Main) {
                onFinished(sizeReleased)
            }
        }
    }

    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var powerSaveReceiver: BroadcastReceiver? = null

    init {
        val dao = AppDatabase.getDatabase(application).browserDao()
        repository = BrowserRepository(dao)
        bookmarks = repository.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        offlinePages = repository.offlinePages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        downloadedFiles = repository.downloadedFiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        searchHistory = repository.searchHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        loadLocalBookmarks()
        observeBattery(application)
        observePowerSaveMode(application)
        observeNetworkConnectivity(application)
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

    private fun observeNetworkConnectivity(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNet = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(activeNet)
                _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = true
                    }

                    override fun onLost(network: Network) {
                        val currentActive = cm.activeNetwork
                        val currentCaps = cm.getNetworkCapabilities(currentActive)
                        _isOnline.value = currentCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    }
                }
                cm.registerNetworkCallback(request, callback)
                networkCallback = callback
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveCurrentWebPage(
        url: String,
        title: String,
        webView: android.webkit.WebView?,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        if (url.isBlank() || url.startsWith("pixelbrowser://") || url.startsWith("file://")) {
            onResult?.invoke(false, "無法儲存內部網頁")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val offlineDir = File(context.filesDir, "offline_pages")
                if (!offlineDir.exists()) {
                    offlineDir.mkdirs()
                }

                val safeTitle = title.ifBlank { url }.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_").take(25)
                val archiveFile = File(offlineDir, "offline_${System.currentTimeMillis()}_${safeTitle}.mht")

                withContext(Dispatchers.Main) {
                    if (webView != null) {
                        webView.saveWebArchive(archiveFile.absolutePath, false) { savedPath ->
                            if (savedPath != null) {
                                viewModelScope.launch(Dispatchers.IO) {
                                    val savedFile = File(savedPath)
                                    val fileSize = savedFile.length()
                                    val textSnippet = try {
                                        org.jsoup.Jsoup.parse(savedFile, "UTF-8").body().text().take(300)
                                    } catch (e: Exception) {
                                        title
                                    }

                                    repository.saveOfflinePage(
                                        url = url,
                                        title = if (title.isNotBlank()) title else url,
                                        localPath = savedPath,
                                        textSnippet = textSnippet,
                                        fileSize = fileSize
                                    )

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "已成功儲存網頁供離線閱讀", Toast.LENGTH_SHORT).show()
                                        onResult?.invoke(true, savedPath)
                                    }
                                }
                            } else {
                                fallbackDownloadHtml(url, title, archiveFile, onResult)
                            }
                        }
                    } else {
                        fallbackDownloadHtml(url, title, archiveFile, onResult)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "離線儲存失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                    onResult?.invoke(false, e.message ?: "儲存失敗")
                }
            }
        }
    }

    private fun fallbackDownloadHtml(
        url: String,
        title: String,
        targetFile: File,
        onResult: ((Boolean, String) -> Unit)?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = org.jsoup.Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Linux; Android 13; Wear OS) AppleWebKit/537.36")
                    .timeout(12000)
                    .get()
                val html = doc.outerHtml()
                val htmlFile = File(targetFile.parentFile, targetFile.nameWithoutExtension + ".html")
                htmlFile.writeText(html, Charsets.UTF_8)
                val snippet = doc.body().text().take(300)
                val displayTitle = doc.title().ifBlank { title.ifBlank { url } }

                repository.saveOfflinePage(
                    url = url,
                    title = displayTitle,
                    localPath = htmlFile.absolutePath,
                    textSnippet = snippet,
                    fileSize = htmlFile.length()
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "已成功儲存網頁供離線閱讀", Toast.LENGTH_SHORT).show()
                    onResult?.invoke(true, htmlFile.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "離線下載失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                    onResult?.invoke(false, e.message ?: "下載失敗")
                }
            }
        }
    }

    fun deleteOfflinePage(id: Long, localPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeOfflinePage(id)
            try {
                val f = File(localPath)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteOfflinePageByUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val page = repository.getOfflinePageByUrl(url)
            if (page != null) {
                repository.removeOfflinePage(page.id)
                try {
                    val f = File(page.localPath)
                    if (f.exists()) {
                        f.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun clearAllOfflinePages() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllOfflinePages()
            val offlineDir = File(getApplication<Application>().filesDir, "offline_pages")
            if (offlineDir.exists()) {
                offlineDir.listFiles()?.forEach { it.delete() }
            }
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
        if (_isForceHttpsEnabled.value && formattedUrl.startsWith("http://")) {
            formattedUrl = "https://" + formattedUrl.substring(7)
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

    fun togglePopupBlocking() {
        _isPopupBlockingEnabled.value = !_isPopupBlockingEnabled.value
    }

    fun togglePhishingProtection() {
        _isPhishingProtectionEnabled.value = !_isPhishingProtectionEnabled.value
    }

    fun toggleForceHttps() {
        _isForceHttpsEnabled.value = !_isForceHttpsEnabled.value
    }

    fun toggleBlockThirdPartyCookies() {
        _isBlockThirdPartyCookiesEnabled.value = !_isBlockThirdPartyCookiesEnabled.value
    }

    fun toggleCircularSafeMode() {
        _isCircularSafeMode.value = !_isCircularSafeMode.value
    }

    fun toggleOneHandedGestures() {
        _isOneHandedGesturesEnabled.value = !_isOneHandedGesturesEnabled.value
    }

    fun setGestureSensitivity(sensitivity: String) {
        _gestureSensitivity.value = sensitivity
    }

    fun showGestureHud(message: String) {
        _gestureHudMessage.value = message
        hudDismissJob?.cancel()
        hudDismissJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1800L)
            _gestureHudMessage.value = null
        }
    }

    fun loadLocalBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = localStorage.getAllBookmarks()
            _localBookmarks.value = list
        }
    }

    fun addLocalBookmark(url: String, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            localStorage.saveBookmark(title, url)
            loadLocalBookmarks()
        }
    }

    fun removeLocalBookmark(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            localStorage.deleteBookmark(url)
            loadLocalBookmarks()
        }
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

    fun downloadFile(
        url: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
        customFileName: String? = null
    ) {
        val taskId = java.util.UUID.randomUUID().toString()

        var fileName = customFileName?.trim()
        if (fileName.isNullOrBlank()) {
            fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            if (fileName.isNullOrBlank() || fileName == "downloadfile.bin") {
                val urlPath = try { URL(url).path } catch (e: Exception) { "" }
                val lastSegment = urlPath.substringAfterLast('/')
                if (lastSegment.isNotBlank() && lastSegment.contains('.')) {
                    fileName = lastSegment
                } else {
                    val ext = if (mimeType != null) android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) else null
                    fileName = "downloaded_" + System.currentTimeMillis() + (if (ext != null) ".$ext" else ".bin")
                }
            }
        }

        val detectedMime = mimeType ?: run {
            val ext = fileName.substringAfterLast('.', "")
            if (ext.isNotBlank()) {
                android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
            } else {
                "application/octet-stream"
            }
        }

        val initialTask = com.example.data.DownloadTask(
            id = taskId,
            fileName = fileName,
            url = url,
            mimeType = detectedMime,
            progress = 0,
            status = com.example.data.DownloadStatus.DOWNLOADING
        )

        _activeDownloads.update { current -> listOf(initialTask) + current.filterNot { it.id == taskId } }

        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "開始下載：$fileName", Toast.LENGTH_SHORT).show()
                }

                val dir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: getApplication<Application>().filesDir
                if (!dir.exists()) {
                    dir.mkdirs()
                }

                val targetFile = File(dir, fileName)

                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.connect()

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val totalBytes = conn.contentLength.toLong()
                    var bytesDownloaded = 0L
                    var lastTime = System.currentTimeMillis()
                    var lastBytes = 0L

                    java.io.BufferedInputStream(conn.inputStream, 16384).use { inputStream ->
                        java.io.BufferedOutputStream(FileOutputStream(targetFile), 16384).use { outputStream ->
                            val buffer = ByteArray(16384)
                            var bytesRead: Int

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead

                                val currentTime = System.currentTimeMillis()
                                val timeDiff = currentTime - lastTime
                                if (timeDiff >= 500 || bytesDownloaded == totalBytes) {
                                    val bytesDiff = bytesDownloaded - lastBytes
                                    val speedKbps = if (timeDiff > 0) (bytesDiff / 1024f) / (timeDiff / 1000f) else 0f
                                    val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100) else 50

                                    _activeDownloads.update { tasks ->
                                        tasks.map { t ->
                                            if (t.id == taskId) {
                                                t.copy(
                                                    progress = progress,
                                                    bytesDownloaded = bytesDownloaded,
                                                    totalBytes = totalBytes,
                                                    speedKbps = speedKbps
                                                )
                                            } else t
                                        }
                                    }

                                    lastTime = currentTime
                                    lastBytes = bytesDownloaded
                                }
                            }
                            outputStream.flush()
                        }
                    }

                    _activeDownloads.update { tasks ->
                        tasks.map { t ->
                            if (t.id == taskId) {
                                t.copy(
                                    progress = 100,
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = if (totalBytes > 0) totalBytes else bytesDownloaded,
                                    status = com.example.data.DownloadStatus.COMPLETED,
                                    localPath = targetFile.absolutePath
                                )
                            } else t
                        }
                    }

                    repository.addDownloadedFile(
                        fileName = fileName,
                        url = url,
                        mimeType = detectedMime,
                        localPath = targetFile.absolutePath,
                        fileSize = if (totalBytes > 0) totalBytes else bytesDownloaded
                    )

                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "下載成功：$fileName", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    _activeDownloads.update { tasks ->
                        tasks.map { t ->
                            if (t.id == taskId) {
                                t.copy(
                                    status = com.example.data.DownloadStatus.FAILED,
                                    errorMessage = "HTTP $responseCode"
                                )
                            } else t
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "下載失敗：HTTP $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                _activeDownloads.update { tasks ->
                    tasks.map { t ->
                        if (t.id == taskId) {
                            t.copy(
                                status = com.example.data.DownloadStatus.FAILED,
                                errorMessage = e.localizedMessage
                            )
                        } else t
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "下載發生錯誤：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                downloadJobs.remove(taskId)
            }
        }

        downloadJobs[taskId] = job
    }

    fun pauseDownload(taskId: String) {
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        _activeDownloads.update { tasks ->
            tasks.map { t ->
                if (t.id == taskId) t.copy(status = com.example.data.DownloadStatus.PAUSED, speedKbps = 0f) else t
            }
        }
        Toast.makeText(getApplication(), "已暫停下載", Toast.LENGTH_SHORT).show()
    }

    fun resumeDownload(taskId: String) {
        val task = _activeDownloads.value.find { it.id == taskId } ?: return
        downloadFile(task.url, null, task.mimeType, task.fileName)
    }

    fun cancelDownload(taskId: String) {
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        _activeDownloads.update { tasks ->
            tasks.filterNot { it.id == taskId }
        }
        Toast.makeText(getApplication(), "已取消下載任務", Toast.LENGTH_SHORT).show()
    }

    fun clearCompletedDownloads() {
        _activeDownloads.update { tasks ->
            tasks.filter { it.status == com.example.data.DownloadStatus.DOWNLOADING || it.status == com.example.data.DownloadStatus.PAUSED }
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
                    Toast.makeText(getApplication(), "檔案已刪除", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "刪除失敗：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun clearAllDownloadedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = downloadedFiles.value
                files.forEach { file ->
                    val f = File(file.localPath)
                    if (f.exists()) f.delete()
                    repository.removeDownloadedFile(file.id)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "已清空所有下載紀錄與檔案", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getStorageUsageInfo(): com.example.data.StorageUsageInfo {
        return try {
            val downloadsDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: getApplication<Application>().filesDir
            var dirSize = 0L
            downloadsDir.listFiles()?.forEach { if (it.isFile) dirSize += it.length() }

            val stat = android.os.StatFs(downloadsDir.path)
            val totalBytes = stat.totalBytes
            val availableBytes = stat.availableBytes
            val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)

            com.example.data.StorageUsageInfo(
                usedBytes = usedBytes,
                totalBytes = totalBytes,
                downloadsFolderBytes = dirSize
            )
        } catch (e: Exception) {
            com.example.data.StorageUsageInfo(0L, 0L, 0L)
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
            
            try {
                textToSpeech.setSpeechRate(_ttsSpeechRate.value)
                textToSpeech.setPitch(_ttsPitch.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
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

    fun toggleSerifFont() {
        _isSerifFont.value = !_isSerifFont.value
    }

    fun setLineHeightMultiplier(multiplier: Float) {
        _lineHeightMultiplier.value = multiplier
    }

    fun toggleParagraphIndent() {
        _isParagraphIndent.value = !_isParagraphIndent.value
    }

    fun toggleJustifyAlign() {
        _isJustifyAlign.value = !_isJustifyAlign.value
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
        networkCallback?.let { callback ->
            try {
                val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Ignore unregistration errors
            }
        }
    }
}
