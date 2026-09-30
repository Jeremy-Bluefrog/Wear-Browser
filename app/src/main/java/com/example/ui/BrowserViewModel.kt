package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Bookmark
import com.example.data.BrowserRepository
import com.example.data.HistoryEntry
import com.example.data.SearchHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import java.net.URLEncoder

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BrowserRepository
    val bookmarks: StateFlow<List<Bookmark>>
    val history: StateFlow<List<HistoryEntry>>
    val searchHistory: StateFlow<List<SearchHistory>>

    private val prefs = application.getSharedPreferences("pixel_browser_prefs", Context.MODE_PRIVATE)

    // GeckoView Runtime & Session
    val geckoRuntime: GeckoRuntime by lazy { GeckoRuntime.getDefault(application) }
    val geckoSession: GeckoSession by lazy {
        val settings = GeckoSessionSettings.Builder()
            .useTrackingProtection(prefs.getBoolean("ad_block_enabled", true))
            .build()
        GeckoSession(settings).apply {
            open(geckoRuntime)
        }
    }
    val geckoState: GeckoState by lazy {
        GeckoState(geckoRuntime, geckoSession, "pixelbrowser://home")
    }

    // 當前網址與標題
    private val _currentUrl = MutableStateFlow("pixelbrowser://home")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("Pixel Browser")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    // 瀏覽器偏好設定
    private val _searchEngine = MutableStateFlow(prefs.getString("search_engine", "Google") ?: "Google")
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    private val _textZoom = MutableStateFlow(prefs.getInt("text_zoom", 100))
    val textZoom: StateFlow<Int> = _textZoom.asStateFlow()

    private val _isPureBlackMode = MutableStateFlow(prefs.getBoolean("pure_black_mode", true))
    val isPureBlackMode: StateFlow<Boolean> = _isPureBlackMode.asStateFlow()

    private val _isAdBlockEnabled = MutableStateFlow(prefs.getBoolean("ad_block_enabled", true))
    val isAdBlockEnabled: StateFlow<Boolean> = _isAdBlockEnabled.asStateFlow()

    // 新增優化功能：手腕手勢控制
    private val _isWristGesturesEnabled = MutableStateFlow(prefs.getBoolean("wrist_gestures_enabled", true))
    val isWristGesturesEnabled: StateFlow<Boolean> = _isWristGesturesEnabled.asStateFlow()

    // 新增優化功能：錶冠滾動靈敏度乘數 (1.2f, 2.0f, 3.2f)
    private val _rotarySpeed = MutableStateFlow(prefs.getFloat("rotary_speed", 2.0f))
    val rotarySpeed: StateFlow<Float> = _rotarySpeed.asStateFlow()

    // 新增優化功能：極簡文章閱讀模式 (Reader Mode)
    private val _isReaderMode = MutableStateFlow(false)
    val isReaderMode: StateFlow<Boolean> = _isReaderMode.asStateFlow()

    private val _readerTitle = MutableStateFlow("")
    val readerTitle: StateFlow<String> = _readerTitle.asStateFlow()

    private val _readerContent = MutableStateFlow("")
    val readerContent: StateFlow<String> = _readerContent.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BrowserRepository(database.browserDao())
        bookmarks = repository.bookmarks.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        history = repository.history.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        searchHistory = repository.searchHistory.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun loadUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed == "pixelbrowser://home") {
            _currentUrl.value = "pixelbrowser://home"
            _pageTitle.value = "Pixel Browser"
            _isReaderMode.value = false
            return
        }

        val targetUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> buildSearchUrl(trimmed, _searchEngine.value)
        }

        _isReaderMode.value = false
        _currentUrl.value = targetUrl
        geckoSession.loadUri(targetUrl)
    }

    fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            repository.addSearchHistoryEntry(trimmed)
        }

        val url = if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || (trimmed.contains(".") && !trimmed.contains(" "))) {
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
        } else {
            buildSearchUrl(trimmed, _searchEngine.value)
        }
        _isReaderMode.value = false
        _currentUrl.value = url
        geckoSession.loadUri(url)
    }

    private fun buildSearchUrl(query: String, engine: String): String {
        val encoded = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query
        }
        return when (engine) {
            "Bing" -> "https://www.bing.com/search?q=$encoded"
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$encoded"
            "Baidu" -> "https://www.baidu.com/s?wd=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }

    fun updatePageInfo(title: String, url: String) {
        if (url != "pixelbrowser://home" && (url.startsWith("http://") || url.startsWith("https://"))) {
            _currentUrl.value = url
            _pageTitle.value = title.ifBlank { url }
            viewModelScope.launch {
                repository.addHistoryEntry(url, title.ifBlank { url })
            }
            // 頁面載入完成後自動注入樣式客製化
            applyPageCustomizations()
        }
    }

    fun updateLoadingState(loading: Boolean, prog: Int) {
        _isLoading.value = loading
        _progress.value = prog
    }

    fun updateNavigationState(canBack: Boolean, canForward: Boolean) {
        _canGoBack.value = canBack
        _canGoForward.value = canForward
    }

    fun goHome() {
        _isReaderMode.value = false
        _currentUrl.value = "pixelbrowser://home"
        _pageTitle.value = "Pixel Browser"
    }

    /**
     * 應用 OLED 深色模式、字體縮放與廣告過濾規則
     */
    fun applyPageCustomizations() {
        geckoState.applyDarkMode(_isPureBlackMode.value)
        geckoState.applyTextZoom(_textZoom.value)
        geckoState.applyAdBlock(_isAdBlockEnabled.value)
    }

    /**
     * 切換閱讀模式
     */
    fun toggleReaderMode() {
        if (_isReaderMode.value) {
            _isReaderMode.value = false
        } else {
            // 觸發前端腳本提取文章內容
            geckoState.extractReaderContent()
        }
    }

    fun exitReaderMode() {
        _isReaderMode.value = false
    }

    fun setReaderData(title: String, content: String) {
        _readerTitle.value = title.ifBlank { _pageTitle.value }
        _readerContent.value = content
        _isReaderMode.value = true
    }

    fun toggleBookmark(url: String, title: String) {
        if (url == "pixelbrowser://home" || url.isBlank()) return
        viewModelScope.launch {
            val isAlreadyBookmarked = bookmarks.value.any { it.url == url }
            if (isAlreadyBookmarked) {
                repository.removeBookmark(url)
            } else {
                repository.addBookmark(url, title.ifBlank { url })
            }
        }
    }

    fun addBookmark(url: String, title: String) {
        if (url.isNotBlank()) {
            viewModelScope.launch {
                repository.addBookmark(url, title.ifBlank { url })
            }
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch {
            repository.removeBookmark(url)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun setSearchEngine(engine: String) {
        _searchEngine.value = engine
        prefs.edit().putString("search_engine", engine).apply()
    }

    fun setTextZoom(zoom: Int) {
        _textZoom.value = zoom
        prefs.edit().putInt("text_zoom", zoom).apply()
        geckoState.applyTextZoom(zoom)
    }

    fun togglePureBlackMode() {
        val newVal = !_isPureBlackMode.value
        _isPureBlackMode.value = newVal
        prefs.edit().putBoolean("pure_black_mode", newVal).apply()
        geckoState.applyDarkMode(newVal)
    }

    fun toggleAdBlock() {
        val newVal = !_isAdBlockEnabled.value
        _isAdBlockEnabled.value = newVal
        prefs.edit().putBoolean("ad_block_enabled", newVal).apply()
        geckoState.applyAdBlock(newVal)
    }

    fun toggleWristGestures() {
        val newVal = !_isWristGesturesEnabled.value
        _isWristGesturesEnabled.value = newVal
        prefs.edit().putBoolean("wrist_gestures_enabled", newVal).apply()
    }

    fun setRotarySpeed(speed: Float) {
        _rotarySpeed.value = speed
        prefs.edit().putFloat("rotary_speed", speed).apply()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            geckoSession.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
