package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Bookmark
import com.example.data.BrowserRepository
import com.example.data.HistoryEntry
import com.example.data.OfflineArticle
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
import java.util.UUID

/**
 * 輕量級手錶標籤頁模型（專為手錶 RAM 限制最佳化）
 */
data class TabInfo(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "pixelbrowser://home",
    val title: String = "首頁",
    val isSecure: Boolean = true
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BrowserRepository
    val bookmarks: StateFlow<List<Bookmark>>
    val history: StateFlow<List<HistoryEntry>>
    val searchHistory: StateFlow<List<SearchHistory>>
    val offlineArticles: StateFlow<List<OfflineArticle>>

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

    // 手腕手勢控制
    private val _isWristGesturesEnabled = MutableStateFlow(prefs.getBoolean("wrist_gestures_enabled", true))
    val isWristGesturesEnabled: StateFlow<Boolean> = _isWristGesturesEnabled.asStateFlow()

    // 錶冠滾動靈敏度乘數 (1.2f, 2.0f, 3.2f)
    private val _rotarySpeed = MutableStateFlow(prefs.getFloat("rotary_speed", 2.0f))
    val rotarySpeed: StateFlow<Float> = _rotarySpeed.asStateFlow()

    // 無痕隱私瀏覽模式 (Incognito Mode)
    private val _isIncognitoMode = MutableStateFlow(false)
    val isIncognitoMode: StateFlow<Boolean> = _isIncognitoMode.asStateFlow()

    // 極簡文章閱讀模式 (Reader Mode)
    private val _isReaderMode = MutableStateFlow(false)
    val isReaderMode: StateFlow<Boolean> = _isReaderMode.asStateFlow()

    private val _readerTitle = MutableStateFlow("")
    val readerTitle: StateFlow<String> = _readerTitle.asStateFlow()

    private val _readerContent = MutableStateFlow("")
    val readerContent: StateFlow<String> = _readerContent.asStateFlow()

    // 多標籤頁管理 (上限 3 頁以維護手錶輕量資源)
    private val defaultInitialTab = TabInfo(url = "pixelbrowser://home", title = "首頁")
    private val _tabs = MutableStateFlow(listOf(defaultInitialTab))
    val tabs: StateFlow<List<TabInfo>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(defaultInitialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BrowserRepository(database.browserDao())
        bookmarks = repository.bookmarks.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        history = repository.history.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        searchHistory = repository.searchHistory.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        offlineArticles = repository.offlineArticles.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun loadUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed == "pixelbrowser://home") {
            _currentUrl.value = "pixelbrowser://home"
            _pageTitle.value = "Pixel Browser"
            _isReaderMode.value = false
            updateActiveTabInfo("pixelbrowser://home", "首頁")
            return
        }

        val targetUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> buildSearchUrl(trimmed, _searchEngine.value)
        }

        _isReaderMode.value = false
        _currentUrl.value = targetUrl
        updateActiveTabInfo(targetUrl, targetUrl)
        geckoSession.loadUri(targetUrl)
    }

    fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        // 若非無痕模式，記錄搜尋歷史
        if (!_isIncognitoMode.value) {
            viewModelScope.launch {
                repository.addSearchHistoryEntry(trimmed)
            }
        }

        val url = if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || (trimmed.contains(".") && !trimmed.contains(" "))) {
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
        } else {
            buildSearchUrl(trimmed, _searchEngine.value)
        }
        _isReaderMode.value = false
        _currentUrl.value = url
        updateActiveTabInfo(url, trimmed)
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
            updateActiveTabInfo(url, title.ifBlank { url })

            // 若非無痕模式，記錄瀏覽歷史
            if (!_isIncognitoMode.value) {
                viewModelScope.launch {
                    repository.addHistoryEntry(url, title.ifBlank { url })
                }
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
        updateActiveTabInfo("pixelbrowser://home", "首頁")
    }

    // 多標籤管理
    fun openNewTab(url: String = "pixelbrowser://home") {
        val currentTabs = _tabs.value
        if (currentTabs.size >= 3) {
            // 已達上限，切換至最後一頁並導航
            val last = currentTabs.last()
            switchTab(last.id)
            if (url != "pixelbrowser://home") {
                loadUrl(url)
            }
            return
        }
        val newTab = TabInfo(url = url, title = if (url == "pixelbrowser://home") "新分頁" else url)
        _tabs.value = currentTabs + newTab
        _activeTabId.value = newTab.id
        loadUrl(url)
    }

    fun switchTab(tabId: String) {
        val target = _tabs.value.find { it.id == tabId } ?: return
        _activeTabId.value = tabId
        _isReaderMode.value = false
        _currentUrl.value = target.url
        _pageTitle.value = target.title
        if (target.url != "pixelbrowser://home") {
            geckoSession.loadUri(target.url)
        }
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        if (currentTabs.size <= 1) {
            // 僅存一個標籤時重設為首頁
            val resetTab = TabInfo(url = "pixelbrowser://home", title = "首頁")
            _tabs.value = listOf(resetTab)
            _activeTabId.value = resetTab.id
            goHome()
            return
        }
        val remaining = currentTabs.filter { it.id != tabId }
        _tabs.value = remaining
        if (_activeTabId.value == tabId) {
            val next = remaining.last()
            switchTab(next.id)
        }
    }

    private fun updateActiveTabInfo(url: String, title: String) {
        val activeId = _activeTabId.value
        _tabs.value = _tabs.value.map {
            if (it.id == activeId) it.copy(url = url, title = title) else it
        }
    }

    /**
     * 無痕隱私瀏覽模式切換
     */
    fun toggleIncognitoMode() {
        _isIncognitoMode.value = !_isIncognitoMode.value
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

    /**
     * 儲存離線文章
     */
    fun saveOfflineArticle(url: String, title: String, content: String) {
        if (url.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            repository.saveOfflineArticle(url, title, content)
        }
    }

    fun removeOfflineArticle(url: String) {
        viewModelScope.launch {
            repository.removeOfflineArticle(url)
        }
    }

    fun clearOfflineArticles() {
        viewModelScope.launch {
            repository.clearOfflineArticles()
        }
    }

    fun openOfflineArticle(article: OfflineArticle) {
        _readerTitle.value = article.title
        _readerContent.value = article.content
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
