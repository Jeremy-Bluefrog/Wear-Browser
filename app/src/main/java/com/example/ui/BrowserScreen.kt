package com.example.ui

import android.app.RemoteInput
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.example.wear.WearGestureDetector
import com.example.wear.WearOsCompatLayer

enum class ScreenState {
    HOME,
    BROWSER,
    BOOKMARKS,
    HISTORY,
    SETTINGS
}

/**
 * 瀏覽器主畫面導航與狀態整合控制器（深度採用 Wear OS Material 3 架構與 TimeText）
 */
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isRound = LocalConfiguration.current.isScreenRound

    var currentScreen by remember { mutableStateOf(ScreenState.HOME) }
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val pageTitle by viewModel.pageTitle.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()

    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val offlineArticles by viewModel.offlineArticles.collectAsStateWithLifecycle()
    val searchEngine by viewModel.searchEngine.collectAsStateWithLifecycle()
    val textZoom by viewModel.textZoom.collectAsStateWithLifecycle()
    val isPureBlackMode by viewModel.isPureBlackMode.collectAsStateWithLifecycle()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsStateWithLifecycle()
    val isWristGesturesEnabled by viewModel.isWristGesturesEnabled.collectAsStateWithLifecycle()
    val rotarySpeed by viewModel.rotarySpeed.collectAsStateWithLifecycle()
    val isIncognitoMode by viewModel.isIncognitoMode.collectAsStateWithLifecycle()

    val isReaderMode by viewModel.isReaderMode.collectAsStateWithLifecycle()
    val readerTitle by viewModel.readerTitle.collectAsStateWithLifecycle()
    val readerContent by viewModel.readerContent.collectAsStateWithLifecycle()

    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()

    var showUrlInputDialog by remember { mutableStateOf(false) }
    var showZoomDialog by remember { mutableStateOf(false) }
    var showTabsDialog by remember { mutableStateOf(false) }

    // 依 URL 狀態切換導覽畫面
    LaunchedEffect(currentUrl) {
        if (currentUrl == "pixelbrowser://home") {
            currentScreen = ScreenState.HOME
        } else {
            currentScreen = ScreenState.BROWSER
        }
    }

    // Wear OS 語音輸入啟動器
    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = RemoteInput.getResultsFromIntent(result.data)
        val query = results?.getCharSequence("input_result")?.toString()
            ?: result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!query.isNullOrBlank()) {
            viewModel.performSearch(query)
            currentScreen = ScreenState.BROWSER
        }
    }

    fun launchVoiceSearch() {
        try {
            val intent = WearOsCompatLayer.getSafeVoiceSearchIntent()
            voiceInputLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "無法啟動語音輸入", Toast.LENGTH_SHORT).show()
        }
    }

    // 整合 Wear OS 手腕手勢控制
    DisposableEffect(isWristGesturesEnabled, currentScreen, isReaderMode) {
        if (!isWristGesturesEnabled) return@DisposableEffect onDispose {}

        val gestureDetector = WearGestureDetector(context, object : WearGestureDetector.GestureListener {
            override fun onPrimaryAction() {
                // 輕叩兩次：首頁啟動語音，瀏覽頁重新整理
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (currentScreen == ScreenState.BROWSER) {
                    viewModel.geckoState.reload()
                } else if (currentScreen == ScreenState.HOME) {
                    launchVoiceSearch()
                }
            }

            override fun onDismissAction() {
                // 轉腕甩動：退出閱讀模式或返回上一頁
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (isReaderMode) {
                    viewModel.exitReaderMode()
                } else if (showUrlInputDialog) {
                    showUrlInputDialog = false
                } else if (showZoomDialog) {
                    showZoomDialog = false
                } else if (showTabsDialog) {
                    showTabsDialog = false
                } else if (currentScreen == ScreenState.BROWSER) {
                    if (canGoBack) {
                        viewModel.geckoState.goBack()
                    } else {
                        viewModel.goHome()
                        currentScreen = ScreenState.HOME
                    }
                } else if (currentScreen != ScreenState.HOME) {
                    currentScreen = ScreenState.HOME
                }
            }
        })

        gestureDetector.start()

        onDispose {
            gestureDetector.stop()
        }
    }

    // 系統手勢 / 返回鍵處理
    BackHandler(enabled = true) {
        if (isReaderMode) {
            viewModel.exitReaderMode()
            return@BackHandler
        }
        if (showUrlInputDialog) {
            showUrlInputDialog = false
            return@BackHandler
        }
        if (showZoomDialog) {
            showZoomDialog = false
            return@BackHandler
        }
        if (showTabsDialog) {
            showTabsDialog = false
            return@BackHandler
        }
        when (currentScreen) {
            ScreenState.BROWSER -> {
                if (canGoBack) {
                    viewModel.geckoState.goBack()
                } else {
                    viewModel.goHome()
                    currentScreen = ScreenState.HOME
                }
            }
            ScreenState.HOME -> {
                val activity = context as? android.app.Activity
                activity?.finish()
            }
            else -> {
                currentScreen = ScreenState.HOME
            }
        }
    }

    // Wear OS Material 3 核心 AppScaffold 與 ScreenScaffold 架構
    AppScaffold {
        ScreenScaffold(
            timeText = {
                // 瀏覽網頁時自動隱藏 TimeText，讓網頁全螢幕沉浸展示；主頁與設定頁頂部完美曲面呈現當前時間
                if (currentScreen != ScreenState.BROWSER) {
                    TimeText {
                        time()
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isPureBlackMode) Color.Black else MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
                    ScreenState.HOME -> {
                        HomeScreen(
                            isRound = isRound,
                            savedBookmarks = bookmarks,
                            savedOfflineArticles = offlineArticles,
                            recentHistory = history,
                            isIncognitoMode = isIncognitoMode,
                            onOpenSearchInput = { showUrlInputDialog = true },
                            onVoiceSearch = { launchVoiceSearch() },
                            onNavigateUrl = { url ->
                                viewModel.loadUrl(url)
                                currentScreen = ScreenState.BROWSER
                            },
                            onOpenOfflineArticle = { article ->
                                viewModel.openOfflineArticle(article)
                                currentScreen = ScreenState.BROWSER
                            },
                            onOpenDownloads = { currentScreen = ScreenState.BOOKMARKS },
                            onToggleIncognito = {
                                viewModel.toggleIncognitoMode()
                                val msg = if (!isIncognitoMode) "無痕隱私瀏覽已開啟 🕶️" else "已關閉無痕瀏覽"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onOpenBookmarks = { currentScreen = ScreenState.BOOKMARKS },
                            onOpenHistory = { currentScreen = ScreenState.HISTORY },
                            onOpenSettings = { currentScreen = ScreenState.SETTINGS }
                        )
                    }
                    ScreenState.BROWSER -> {
                        GeckoBrowserView(
                            state = viewModel.geckoState,
                            pageTitle = pageTitle,
                            isLoading = isLoading,
                            progress = progress,
                            canGoBack = canGoBack,
                            canGoForward = canGoForward,
                            isPureBlackMode = isPureBlackMode,
                            isRound = isRound,
                            isBookmarked = bookmarks.any { it.url == currentUrl },
                            isReaderMode = isReaderMode,
                            readerTitle = readerTitle,
                            readerContent = readerContent,
                            rotarySpeed = rotarySpeed,
                            tabCount = tabs.size,
                            isIncognitoMode = isIncognitoMode,
                            onPageStarted = { _ ->
                                viewModel.updateLoadingState(true, 15)
                            },
                            onPageFinished = { u, title ->
                                viewModel.updateLoadingState(false, 100)
                                viewModel.updatePageInfo(title, u)
                            },
                            onProgressChanged = { p ->
                                viewModel.updateLoadingState(p < 100, p)
                            },
                            onNavigationStateChanged = { back, fwd ->
                                viewModel.updateNavigationState(back, fwd)
                            },
                            onReaderDataExtracted = { title, content ->
                                viewModel.setReaderData(title, content)
                            },
                            onToggleReaderMode = {
                                viewModel.toggleReaderMode()
                            },
                            onExitReaderMode = {
                                viewModel.exitReaderMode()
                            },
                            onOpenUrlInput = {
                                showUrlInputDialog = true
                            },
                            onOpenTabs = {
                                showTabsDialog = true
                            },
                            onSaveOfflineArticle = {
                                if (readerContent.isNotBlank()) {
                                    viewModel.saveOfflineArticle(currentUrl, pageTitle, readerContent)
                                } else {
                                    // 若尚未提取閱讀內容，以標題與網址建立書籤備份
                                    viewModel.addBookmark(currentUrl, pageTitle)
                                }
                                Toast.makeText(context, "已收藏至離線庫", Toast.LENGTH_SHORT).show()
                            },
                            onToggleIncognito = {
                                viewModel.toggleIncognitoMode()
                                val msg = if (!isIncognitoMode) "無痕隱私瀏覽已開啟 🕶️" else "已關閉無痕瀏覽"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onBack = { viewModel.geckoState.goBack() },
                            onForward = { viewModel.geckoState.goForward() },
                            onRefresh = { viewModel.geckoState.reload() },
                            onToggleBookmark = {
                                viewModel.toggleBookmark(currentUrl, pageTitle)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val isNowBookmarked = bookmarks.none { it.url == currentUrl }
                                val msg = if (isNowBookmarked) "已加入書籤" else "已移除書籤"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onOpenZoom = { showZoomDialog = true },
                            onGoHome = {
                                viewModel.goHome()
                                currentScreen = ScreenState.HOME
                            }
                        )
                    }
                    ScreenState.BOOKMARKS -> {
                        BookmarksScreen(
                            isRound = isRound,
                            bookmarks = bookmarks,
                            offlineArticles = offlineArticles,
                            onNavigateUrl = { url ->
                                viewModel.loadUrl(url)
                                currentScreen = ScreenState.BROWSER
                            },
                            onOpenOfflineArticle = { article ->
                                viewModel.openOfflineArticle(article)
                                currentScreen = ScreenState.BROWSER
                            },
                            onDeleteBookmark = { url ->
                                viewModel.removeBookmark(url)
                                Toast.makeText(context, "已刪除書籤", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteOfflineArticle = { url ->
                                viewModel.removeOfflineArticle(url)
                                Toast.makeText(context, "已刪除離線文章", Toast.LENGTH_SHORT).show()
                            },
                            onAddBookmark = { url, title ->
                                viewModel.addBookmark(url, title)
                            },
                            onBack = { currentScreen = ScreenState.HOME }
                        )
                    }
                    ScreenState.HISTORY -> {
                        HistoryScreen(
                            isRound = isRound,
                            history = history,
                            onNavigateUrl = { url ->
                                viewModel.loadUrl(url)
                                currentScreen = ScreenState.BROWSER
                            },
                            onClearHistory = {
                                viewModel.clearHistory()
                                Toast.makeText(context, "歷史紀錄已清除", Toast.LENGTH_SHORT).show()
                            },
                            onBack = { currentScreen = ScreenState.HOME }
                        )
                    }
                    ScreenState.SETTINGS -> {
                        SettingsScreen(
                            isRound = isRound,
                            searchEngine = searchEngine,
                            textZoom = textZoom,
                            isPureBlackMode = isPureBlackMode,
                            isAdBlockEnabled = isAdBlockEnabled,
                            isWristGesturesEnabled = isWristGesturesEnabled,
                            rotarySpeed = rotarySpeed,
                            isIncognitoMode = isIncognitoMode,
                            onSearchEngineChange = { viewModel.setSearchEngine(it) },
                            onTextZoomChange = { viewModel.setTextZoom(it) },
                            onTogglePureBlack = { viewModel.togglePureBlackMode() },
                            onToggleAdBlock = { viewModel.toggleAdBlock() },
                            onToggleWristGestures = { viewModel.toggleWristGestures() },
                            onRotarySpeedChange = { viewModel.setRotarySpeed(it) },
                            onToggleIncognito = {
                                viewModel.toggleIncognitoMode()
                                val msg = if (!isIncognitoMode) "無痕隱私瀏覽已開啟 🕶️" else "已關閉無痕瀏覽"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onClearData = {
                                try {
                                    viewModel.clearHistory()
                                    viewModel.clearOfflineArticles()
                                    Toast.makeText(context, "歷史紀錄與離線快取已清除", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "清除完成", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBack = { currentScreen = ScreenState.HOME }
                        )
                    }
                }

                // 搜尋與網址輸入對話框
                if (showUrlInputDialog) {
                    UrlInputDialog(
                        isRound = isRound,
                        onDismiss = { showUrlInputDialog = false },
                        onSubmit = { input ->
                            showUrlInputDialog = false
                            viewModel.loadUrl(input)
                            currentScreen = ScreenState.BROWSER
                        },
                        onVoiceClick = {
                            showUrlInputDialog = false
                            launchVoiceSearch()
                        }
                    )
                }

                // 字體縮放對話框
                if (showZoomDialog) {
                    ZoomDialog(
                        isRound = isRound,
                        currentZoom = textZoom,
                        onZoomChange = { newZoom ->
                            viewModel.setTextZoom(newZoom)
                        },
                        onDismiss = { showZoomDialog = false }
                    )
                }

                // 多標籤頁管理對話框
                if (showTabsDialog) {
                    TabsManagerDialog(
                        isRound = isRound,
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = { id ->
                            viewModel.switchTab(id)
                            currentScreen = ScreenState.BROWSER
                        },
                        onCloseTab = { id ->
                            viewModel.closeTab(id)
                        },
                        onNewTab = {
                            viewModel.openNewTab()
                            currentScreen = ScreenState.HOME
                        },
                        onDismiss = { showTabsDialog = false }
                    )
                }
            }
        }
    }
}
