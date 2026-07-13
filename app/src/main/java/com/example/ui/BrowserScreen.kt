package com.example.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.Surface
import android.app.RemoteInput
import androidx.wear.input.RemoteInputIntentHelper
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.interaction.MutableInteractionSource
import coil.request.ImageRequest
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.wear.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
import androidx.compose.ui.draw.clip
import com.example.data.*
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.net.URLEncoder

enum class ScreenState {
    MENU, HOME, BROWSER, HISTORY
}

@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val haptic = LocalHapticFeedback.current
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val isDeepMode by viewModel.isDeepMode.collectAsStateWithLifecycle()
    val isPowerSavingMode by viewModel.isPowerSavingMode.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val localBookmarks by viewModel.localBookmarks.collectAsStateWithLifecycle()
    val downloadedFiles by viewModel.downloadedFiles.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val textZoom by viewModel.textZoom.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isCloudRendering by viewModel.isCloudRendering.collectAsStateWithLifecycle()
    val isTextOnly by viewModel.isTextOnly.collectAsStateWithLifecycle()
    val isAggressiveCaching by viewModel.isAggressiveCaching.collectAsStateWithLifecycle()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsStateWithLifecycle()
    val isCpuThrottleEnabled by viewModel.isCpuThrottleEnabled.collectAsStateWithLifecycle()
    val isSmartRamCleanerEnabled by viewModel.isSmartRamCleanerEnabled.collectAsStateWithLifecycle()
    val isCircularSafeMode by viewModel.isCircularSafeMode.collectAsStateWithLifecycle()
    val imageCacheSize by viewModel.imageCacheSize.collectAsStateWithLifecycle()
    val voiceCacheSize by viewModel.voiceCacheSize.collectAsStateWithLifecycle()
    
    var showMenu by remember { mutableStateOf(false) }
    var isIncognito by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableStateOf(0) }
    var pageTitle by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
 
    // Intercept swipe-to-dismiss gesture to navigate WebView history backwards gracefully, or return to home
    BackHandler(enabled = !showMenu && currentUrl != "pixelbrowser://home") {
        if (canGoBack) {
            webViewRef?.goBack()
        } else {
            viewModel.navigateTo("pixelbrowser://home")
        }
    }

    if (showMenu) {
        BackHandler { showMenu = false }
    }

    val webViewFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Focus WebView for Physical Rotating Crown Scrolling whenever main menu closes
    LaunchedEffect(showMenu, currentUrl) {
        if (!showMenu && currentUrl != "pixelbrowser://home") {
            try {
                webViewFocusRequester.requestFocus()
            } catch (e: Exception) {
                // FocusRequester might not be attached to an active node yet
            }
        }
    }

    // WebView RAM Optimizer and Lifecycle Pause/Resume based on screen visibility
    LaunchedEffect(currentUrl) {
        if (currentUrl == "pixelbrowser://home") {
            if (isSmartRamCleanerEnabled) {
                try {
                    webViewRef?.onPause()
                    System.gc() // Actively trigger GC to clean up Java objects
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            try {
                webViewRef?.onResume()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val screenState = when {
        showMenu -> ScreenState.MENU
        currentUrl == "pixelbrowser://home" -> ScreenState.HOME
        currentUrl == "pixelbrowser://history" -> ScreenState.HISTORY
        else -> ScreenState.BROWSER
    }

    val finalHistory = if (isIncognito) emptyList() else history
    val finalSearchHistory = if (isIncognito) emptyList() else searchHistory

    AppScaffold {
        ConstraintLayout(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            val (contentRef, bottomTriggerRef, bottomIndicatorRef, pageIndicatorRef) = createRefs()
            
            AnimatedContent(
                targetState = screenState,
                transitionSpec = {
                    val slideIn = slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 300)
                    )
                    val slideOut = slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 300)
                    )
                    val popIn = slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(durationMillis = 300)
                    )
                    val popOut = slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(durationMillis = 300)
                    )
                    
                    if (initialState.ordinal < targetState.ordinal) {
                        slideIn togetherWith slideOut
                    } else {
                        popIn togetherWith popOut
                    }
                },
                label = "ScreenTransition",
                modifier = Modifier.fillMaxSize().constrainAs(contentRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            ) { state ->
                when (state) {
                    ScreenState.MENU -> {
                    BookmarkMenu(
                        bookmarks = bookmarks,
                        downloadedFiles = downloadedFiles,
                        history = finalHistory,
                        searchHistory = finalSearchHistory,
                        onAddSearchHistory = { query -> viewModel.addSearchHistory(query) },
                        onDeleteSearchHistory = { id -> viewModel.removeSearchHistory(id) },
                        onClearSearchHistory = { viewModel.clearSearchHistory() },
                        currentUrl = currentUrl,
                        pageTitle = pageTitle,
                        onAddBookmark = { url, title -> viewModel.addBookmark(url, title) },
                        textZoom = textZoom,
                        onSetTextZoom = { zoom -> viewModel.setTextZoom(zoom) },
                        isSpeaking = isSpeaking,
                        onToggleSpeak = {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                webViewRef?.evaluateJavascript(
                                    "(function() { return document.body.innerText; })();"
                                ) { text ->
                                    val cleanText = text?.removePrefix("\"")?.removeSuffix("\"")
                                        ?.replace("\\n", " ")
                                        ?.replace("\\\"", "\"")
                                        ?.replace("\\u003C", "<")
                                    if (!cleanText.isNullOrBlank()) {
                                        viewModel.speakText(cleanText)
                                    } else {
                                        Toast.makeText(viewModel.getApplication(), "No readable text on this page", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onTranslate = { lang ->
                            viewModel.translatePage(lang)
                            showMenu = false
                        },
                        onDeleteDownloadedFile = { id, path ->
                            viewModel.deleteDownloadedFile(id, path)
                        },
                        onDeleteBookmark = { url ->
                            viewModel.removeBookmark(url)
                        },
                        onDeleteHistoryEntry = { id ->
                            viewModel.deleteHistoryEntry(id)
                        },
                        onClearHistory = {
                            viewModel.clearHistory()
                        },
                        onNavigate = { url ->
                            viewModel.navigateTo(url)
                            showMenu = false
                        },
                        onClose = { showMenu = false },
                        onToggleDeepMode = { viewModel.toggleDeepMode() },
                        isDeepMode = isDeepMode,
                        isCloudRendering = isCloudRendering,
                        onToggleCloudRendering = { viewModel.toggleCloudRendering() },
                        isTextOnly = isTextOnly,
                        onToggleTextOnly = { viewModel.toggleTextOnly() },
                        isAggressiveCaching = isAggressiveCaching,
                        onToggleAggressiveCaching = { viewModel.toggleAggressiveCaching() },
                        isAdBlockEnabled = isAdBlockEnabled,
                        onToggleAdBlock = { viewModel.toggleAdBlock() },
                        isCpuThrottleEnabled = isCpuThrottleEnabled,
                        onToggleCpuThrottle = { viewModel.toggleCpuThrottle() },
                        isSmartRamCleanerEnabled = isSmartRamCleanerEnabled,
                        onToggleSmartRamCleaner = { viewModel.toggleSmartRamCleaner() }
                    )
                }
                ScreenState.HOME -> {
                    HomeScreen(
                        bookmarks = bookmarks,
                        localBookmarks = localBookmarks,
                        onAddLocalBookmark = { url, title -> viewModel.addLocalBookmark(url, title) },
                        onDeleteLocalBookmark = { url -> viewModel.removeLocalBookmark(url) },
                        downloadedFiles = downloadedFiles,
                        onDeleteDownloadedFile = { id, path -> viewModel.deleteDownloadedFile(id, path) },
                        history = finalHistory,
                        searchHistory = finalSearchHistory,
                        onAddSearchHistory = { query -> viewModel.addSearchHistory(query) },
                        onDeleteSearchHistory = { id -> viewModel.removeSearchHistory(id) },
                        onClearSearchHistory = { viewModel.clearSearchHistory() },
                        onDeleteBookmark = { url -> viewModel.removeBookmark(url) },
                        onNavigate = { url ->
                            viewModel.navigateTo(url)
                        },
                        textZoom = textZoom,
                        onSetTextZoom = { viewModel.setTextZoom(it) },
                        isDeepMode = isDeepMode,
                        onToggleDeepMode = { viewModel.toggleDeepMode() },
                        onClearHistory = { viewModel.clearHistory() },
                        onUpdateBookmark = { viewModel.updateBookmark(it) },
                        isCloudRendering = isCloudRendering,
                        onToggleCloudRendering = { viewModel.toggleCloudRendering() },
                        isTextOnly = isTextOnly,
                        onToggleTextOnly = { viewModel.toggleTextOnly() },
                        isAggressiveCaching = isAggressiveCaching,
                        onToggleAggressiveCaching = { viewModel.toggleAggressiveCaching() },
                        isAdBlockEnabled = isAdBlockEnabled,
                        onToggleAdBlock = { viewModel.toggleAdBlock() },
                        isCpuThrottleEnabled = isCpuThrottleEnabled,
                        onToggleCpuThrottle = { viewModel.toggleCpuThrottle() },
                        isSmartRamCleanerEnabled = isSmartRamCleanerEnabled,
                        onToggleSmartRamCleaner = { viewModel.toggleSmartRamCleaner() },
                        isCircularSafeMode = isCircularSafeMode,
                        onToggleCircularSafeMode = { viewModel.toggleCircularSafeMode() },
                        isIncognito = isIncognito,
                        onToggleIncognito = { isIncognito = !isIncognito },
                        imageCacheSize = imageCacheSize,
                        voiceCacheSize = voiceCacheSize,
                        onClearCache = { viewModel.clearVoiceAndImageCache() },
                        onRefreshCache = { viewModel.refreshCacheSizes() }
                    )
                }
                ScreenState.HISTORY -> {
                    HistoryScreen(
                        history = history,
                        searchHistory = searchHistory,
                        onNavigate = { url -> viewModel.navigateTo(url) },
                        onDeleteHistoryEntry = { id -> viewModel.deleteHistoryEntry(id) },
                        onDeleteSearchHistory = { id -> viewModel.removeSearchHistory(id) },
                        onClearHistory = { viewModel.clearHistory() },
                        onAddSearchHistory = { query -> viewModel.addSearchHistory(query) },
                        onBack = { viewModel.navigateTo("pixelbrowser://home") }
                    )
                }
                ScreenState.BROWSER -> {
                    val context = LocalContext.current
                    val isOffline = remember(currentUrl) {
                        if (currentUrl != "pixelbrowser://home" && !currentUrl.startsWith("file://") && !currentUrl.startsWith("about:")) {
                            !isNetworkAvailable(context)
                        } else {
                            false
                        }
                    }

                    if (isOffline) {
                        OfflineScreen(
                            onDismiss = {
                                viewModel.navigateTo("pixelbrowser://home")
                            }
                        )
                    } else {
                        var leftDragOffset by remember { mutableStateOf(0f) }
                        var rightDragOffset by remember { mutableStateOf(0f) }
                        val dragThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 50.dp.toPx() }

                        val isScreenRound = androidx.compose.ui.platform.LocalConfiguration.current.isScreenRound
                        val safeTopPadding = if (isScreenRound && isCircularSafeMode) 24.dp else 0.dp
                        val safeBottomPadding = if (isScreenRound && isCircularSafeMode) 38.dp else 0.dp
                        val safeSidePadding = if (isScreenRound && isCircularSafeMode) 22.dp else 0.dp

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isDeepMode) Color.Black else MaterialTheme.colorScheme.background)
                                .focusRequester(webViewFocusRequester)
                                .focusable()
                                .onRotaryScrollEvent {
                                    // Support high-precision mechanical crown scrolling for the web page itself!
                                    webViewRef?.scrollBy(0, (it.verticalScrollPixels * 1.5f).toInt())
                                    true
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = leftDragOffset - rightDragOffset
                                    }
                                    .padding(
                                        top = safeTopPadding,
                                        bottom = safeBottomPadding,
                                        start = safeSidePadding,
                                        end = safeSidePadding
                                    )
                                    .then(
                                        if (isScreenRound && isCircularSafeMode) {
                                            Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isDeepMode) Color.Black else Color.White)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isDeepMode) Color.DarkGray.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                WebViewComponent(
                                    url = currentUrl,
                                    isPowerSaving = isPowerSavingMode,
                                    textZoom = textZoom,
                                    isCloudRendering = isCloudRendering,
                                    isTextOnly = isTextOnly,
                                    isAggressiveCaching = isAggressiveCaching,
                                    isAdBlockEnabled = isAdBlockEnabled,
                                    isCpuThrottleEnabled = isCpuThrottleEnabled,
                                    onPageStarted = {
                                        isLoading = true
                                        loadProgress = 0
                                    },
                                    onPageFinished = { title, loadedUrl ->
                                        isLoading = false
                                        loadProgress = 100
                                        pageTitle = title ?: ""
                                        if (loadedUrl != null && loadedUrl != "about:blank") {
                                            viewModel.updateUrlFromWebView(loadedUrl)
                                            viewModel.addToHistory(loadedUrl, pageTitle)
                                        } else {
                                            viewModel.addToHistory(currentUrl, pageTitle)
                                        }
                                        canGoBack = webViewRef?.canGoBack() == true
                                        canGoForward = webViewRef?.canGoForward() == true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    onProgressChanged = { progress ->
                                        loadProgress = progress
                                        if (progress >= 100) {
                                            isLoading = false
                                        } else {
                                            isLoading = true
                                        }
                                    },
                                    onWebViewCreated = { webViewRef = it },
                                    onDownloadRequested = { downloadUrl, contentDisposition, mimeType ->
                                        viewModel.downloadFile(downloadUrl, contentDisposition, mimeType)
                                    }
                                )
                            }

                            // Overlay Controls
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = if (isScreenRound) (if (isCircularSafeMode) 6.dp else 18.dp) else 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val menuInt = remember { MutableInteractionSource() }
                                val downloadInt = remember { MutableInteractionSource() }

                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isDeepMode) Color.Black.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isDeepMode) Color.DarkGray.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (currentUrl != "pixelbrowser://home") {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    showMenu = true
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .expressiveScale(menuInt),
                                                interactionSource = menuInt
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Menu,
                                                    contentDescription = "Menu",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        
                                        val isBookmarked = bookmarks.any { it.url == currentUrl }
                                        val bookmarkInt = remember { MutableInteractionSource() }
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (isBookmarked) {
                                                    viewModel.removeBookmark(currentUrl)
                                                } else {
                                                    viewModel.addBookmark(currentUrl, pageTitle)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .expressiveScale(bookmarkInt),
                                            interactionSource = bookmarkInt
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Bookmark,
                                                contentDescription = "Bookmark",
                                                modifier = Modifier.size(18.dp),
                                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.downloadFile(currentUrl)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .expressiveScale(downloadInt),
                                            interactionSource = downloadInt
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            
                            DeferredLoadingProgress(
                                isLoadingProvider = { isLoading },
                                progressProvider = { loadProgress }
                            )

                            // Left Edge (Swipe Right to Go Back / Go Home)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxHeight()
                                    .width(28.dp)
                                    .pointerInput(currentUrl, canGoBack) {
                                        if (currentUrl == "pixelbrowser://home") return@pointerInput
                                        detectHorizontalDragGestures(
                                            onDragStart = { leftDragOffset = 0f },
                                            onDragEnd = {
                                                if (leftDragOffset > dragThresholdPx) {
                                                    if (canGoBack) {
                                                        webViewRef?.goBack()
                                                    } else {
                                                        viewModel.navigateTo("pixelbrowser://home")
                                                    }
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                leftDragOffset = 0f
                                            },
                                            onDragCancel = { leftDragOffset = 0f },
                                            onHorizontalDrag = { _, dragAmount ->
                                                leftDragOffset = (leftDragOffset + dragAmount).coerceAtLeast(0f)
                                            }
                                        )
                                    }
                            )

                            // Right Edge (Swipe Left to Go Forward)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(28.dp)
                                    .pointerInput(canGoForward) {
                                        if (!canGoForward) return@pointerInput
                                        detectHorizontalDragGestures(
                                            onDragStart = { rightDragOffset = 0f },
                                            onDragEnd = {
                                                if (rightDragOffset < -dragThresholdPx) {
                                                    webViewRef?.goForward()
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                rightDragOffset = 0f
                                            },
                                            onDragCancel = { rightDragOffset = 0f },
                                            onHorizontalDrag = { _, dragAmount ->
                                                rightDragOffset = (rightDragOffset + dragAmount).coerceAtMost(0f)
                                            }
                                        )
                                    }
                            )

                            // Visual back gesture indicator
                            if (leftDragOffset > 0f) {
                                val progress = (leftDragOffset / dragThresholdPx).coerceIn(0f, 1.2f)
                                val isTriggered = leftDragOffset >= dragThresholdPx
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .graphicsLayer {
                                            translationX = (-40.dp.toPx() + (leftDragOffset * 0.6f)).coerceAtMost(16.dp.toPx())
                                            alpha = progress.coerceIn(0f, 1f)
                                            scaleX = 0.8f + (progress * 0.2f).coerceAtMost(0.4f)
                                            scaleY = 0.8f + (progress * 0.2f).coerceAtMost(0.4f)
                                        }
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .size(40.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回",
                                        tint = if (isTriggered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Visual forward gesture indicator
                            if (rightDragOffset < 0f) {
                                val rightDragAbs = -rightDragOffset
                                val progress = (rightDragAbs / dragThresholdPx).coerceIn(0f, 1.2f)
                                val isTriggered = rightDragAbs >= dragThresholdPx
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .graphicsLayer {
                                            translationX = (40.dp.toPx() + (rightDragOffset * 0.6f)).coerceAtLeast(-16.dp.toPx())
                                            alpha = progress.coerceIn(0f, 1f)
                                            scaleX = 0.8f + (progress * 0.2f).coerceAtMost(0.4f)
                                            scaleY = 0.8f + (progress * 0.2f).coerceAtMost(0.4f)
                                        }
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .size(40.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "前進",
                                        tint = if (isTriggered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                        }
                    }
                }
            }
        }

        // Swipe up from the very bottom of the screen to open the full-screen menu
            val bottomDensity = androidx.compose.ui.platform.LocalDensity.current
            val bottomThresholdPx = with(bottomDensity) { 45.dp.toPx() }
            val bottomAnim = remember { Animatable(0f) }
            val bottomProgress = (abs(bottomAnim.value) / bottomThresholdPx).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .constrainAs(bottomTriggerRef) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .pointerInput(showMenu, currentUrl) {
                        if (showMenu || currentUrl == "pixelbrowser://home") return@pointerInput
                        detectVerticalDragGestures(
                            onDragStart = {
                                coroutineScope.launch {
                                    bottomAnim.snapTo(0f)
                                }
                            },
                            onDragEnd = {
                                if (bottomAnim.value < -bottomThresholdPx) {
                                    showMenu = true
                                }
                                coroutineScope.launch {
                                    bottomAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    bottomAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                coroutineScope.launch {
                                    bottomAnim.snapTo((bottomAnim.value + dragAmount).coerceAtMost(0f))
                                }
                            }
                        )
                    }
            )

            // Bottom slide-up visual indicator
            if (bottomProgress > 0f) {
                Box(
                    modifier = Modifier
                        .constrainAs(bottomIndicatorRef) {
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .offset(y = (40 - (bottomProgress * 40)).dp)
                        .size(width = 72.dp, height = 48.dp)
                        .alpha(bottomProgress)
                        .scale(0.8f + bottomProgress * 0.2f)
                        .background(
                            color = if (bottomProgress >= 1f)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Open Menu",
                        tint = if (bottomProgress >= 1f)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(y = (-4).dp)
                    )
                }
            }

            if (screenState == ScreenState.HOME || screenState == ScreenState.HISTORY) {
                val activePage = if (screenState == ScreenState.HOME) 0 else 1
                PageIndicator(
                    activePage = activePage,
                    pageCount = 2,
                    onPageSelected = { index ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (index == 0) {
                            viewModel.navigateTo("pixelbrowser://home")
                        } else if (index == 1) {
                            viewModel.navigateTo("pixelbrowser://history")
                        }
                    },
                    modifier = Modifier
                        .constrainAs(pageIndicatorRef) {
                            bottom.linkTo(parent.bottom, margin = 12.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                )
            }
        }


        SearchDialog(
            show = showSearchDialog,
            onDismiss = { showSearchDialog = false },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            searchHistory = searchHistory,
            onAddSearchHistory = { viewModel.addSearchHistory(it) },
            onDeleteSearchHistory = { viewModel.removeSearchHistory(it) },
            onNavigate = { viewModel.navigateTo(it) }
        )

        QrDialog(
            show = showQrDialog,
            onDismiss = { showQrDialog = false },
            currentUrl = currentUrl
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun areUrlsSame(url1: String?, url2: String?): Boolean {
    if (url1 == url2) return true
    if (url1 == null || url2 == null) return false
    
    val u1 = url1.trim().removeSuffix("/").lowercase()
    val u2 = url2.trim().removeSuffix("/").lowercase()
    if (u1 == u2) return true
    
    val clean1 = u1.replace("https://", "").replace("http://", "").replace("www.", "")
    val clean2 = u2.replace("https://", "").replace("http://", "").replace("www.", "")
    if (clean1 == clean2) return true
    
    return false
}

@Composable
fun WebViewComponent(
    url: String,
    isPowerSaving: Boolean,
    textZoom: Int,
    isCloudRendering: Boolean,
    isTextOnly: Boolean,
    isAggressiveCaching: Boolean,
    isAdBlockEnabled: Boolean,
    isCpuThrottleEnabled: Boolean,
    onPageStarted: () -> Unit,
    onPageFinished: (String?, String?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onDownloadRequested: (String, String?, String?) -> Unit
) {
    var lastLoadedUrl by remember { mutableStateOf("") }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                onWebViewCreated(this)
                
                // Allow cookies and third-party cookies
                try {
                    val cookieManager = android.webkit.CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                    onDownloadRequested(downloadUrl, contentDisposition, mimetype)
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onPageStarted()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageFinished(view?.title, url)

                        // Apply Dynamic On-device Reader mode / Cloud-rendering simplification scripts
                        if (isTextOnly) {
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    var style = document.createElement('style');
                                    style.innerHTML = `
                                        * { 
                                            animation: none !important; 
                                            transition: none !important; 
                                            background-image: none !important;
                                        }
                                        img, video, iframe, svg, canvas, .ads, .ad, .advertisement { 
                                            display: none !important; 
                                        }
                                        body { 
                                            background-color: black !important; 
                                            color: #e0e0e0 !important; 
                                            font-family: sans-serif !important; 
                                            line-height: 1.5 !important; 
                                            padding: 8px !important;
                                        }
                                        h1, h2, h3, h4, h5, h6 { 
                                            color: #00ffcc !important; 
                                            cursor: pointer !important;
                                            border-bottom: 1px solid #333 !important;
                                            padding-bottom: 4px !important;
                                        }
                                    `;
                                    document.head.appendChild(style);
                                    
                                    var headers = document.querySelectorAll('h1, h2, h3, h4, h5, h6');
                                    headers.forEach(function(header) {
                                        var next = header.nextElementSibling;
                                        if (next) {
                                            next.style.display = 'none';
                                            header.addEventListener('click', function() {
                                                if (next.style.display === 'none') {
                                                    next.style.display = 'block';
                                                } else {
                                                    next.style.display = 'none';
                                                }
                                            });
                                        }
                                    });
                                })();
                                """.trimIndent(), null
                            )
                        } else if (isCloudRendering) {
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    var style = document.createElement('style');
                                    style.innerHTML = `
                                        * { 
                                            animation: none !important; 
                                            transition: none !important; 
                                            background-image: none !important;
                                        }
                                        img { 
                                            max-width: 100% !important; 
                                            height: auto !important; 
                                            filter: grayscale(100%) !important; 
                                            opacity: 0.8 !important; 
                                        }
                                        video, iframe, canvas, object, embed { 
                                            display: none !important; 
                                        }
                                        body, div, section, article { 
                                            float: none !important; 
                                            position: static !important; 
                                            width: auto !important; 
                                            max-width: 100% !important; 
                                            margin: 0 !important; 
                                            padding: 4px !important; 
                                            background: black !important;
                                            color: white !important;
                                        }
                                    `;
                                    document.head.appendChild(style);
                                })();
                                """.trimIndent(), null
                            )
                        }

                        // Apply CPU Timer Throttle to preserve processor cycles
                        if (isCpuThrottleEnabled) {
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    var originalSetInterval = window.setInterval;
                                    window.setInterval = function(fn, delay) {
                                        if (delay < 1000) delay = 1000;
                                        return originalSetInterval(fn, delay);
                                    };
                                    var originalSetTimeout = window.setTimeout;
                                    window.setTimeout = function(fn, delay) {
                                        if (delay < 500) delay = 500;
                                        return originalSetTimeout(fn, delay);
                                    };
                                })();
                                """.trimIndent(), null
                            )
                        }

                        // Apply Smart Ad Blocker DOM elements cleansing script
                        if (isAdBlockEnabled) {
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    // 1. Inject Style to hide common ad layout frameworks
                                    var style = document.createElement('style');
                                    style.innerHTML = `
                                        .ads, .ad, .advertisement, [id*="google_ads"], [class*="google_ads"],
                                        [id*="ad-container"], [class*="ad-container"], [class*="adsbygoogle"],
                                        [id*="banner"], [class*="banner-ads"], [class*="sponsor"],
                                        .ad-box, .ad-banner, .ad-wrapper, .ad-slot, .ad-placement,
                                        iframe[src*="doubleclick"], iframe[src*="googleads"],
                                        div[id^="div-gpt-ad"], div[class^="ad-"], div[id^="ad-"] {
                                            display: none !important;
                                            visibility: hidden !important;
                                            opacity: 0 !important;
                                            height: 0 !important;
                                            width: 0 !important;
                                            pointer-events: none !important;
                                        }
                                    `;
                                    document.head.appendChild(style);

                                    // 2. Proactively remove iframe or div ad elements periodically or immediately
                                    function removeAds() {
                                        var selectors = [
                                            'iframe[src*="doubleclick"]',
                                            'iframe[src*="googleads"]',
                                            '.adsbygoogle',
                                            '[id*="google_ads"]',
                                            '[class*="google_ads"]',
                                            '.ad-box',
                                            '.ad-banner',
                                            '.ad-wrapper',
                                            '.ad-slot',
                                            '.ad-placement',
                                            'div[id^="div-gpt-ad"]',
                                            '.advertisement',
                                            '.ads'
                                        ];
                                        selectors.forEach(function(selector) {
                                            var els = document.querySelectorAll(selector);
                                            els.forEach(function(el) {
                                                if (el && el.parentNode) {
                                                    el.parentNode.removeChild(el);
                                                }
                                            });
                                        });
                                    }
                                    
                                    removeAds();
                                    // Run again with minor delays to catch delayed/dynamic ad script renders
                                    setTimeout(removeAds, 500);
                                    setTimeout(removeAds, 1500);
                                    setTimeout(removeAds, 3500);
                                })();
                                """.trimIndent(), null
                            )
                        }
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): android.webkit.WebResourceResponse? {
                        if (request != null) {
                            val urlString = request.url.toString().lowercase()
                            
                            // Ad-blocking and tracking blocker
                            if (isAdBlockEnabled) {
                                val adKeywords = listOf(
                                    "doubleclick", "google-analytics", "googlesyndication", "adservice", "adsense",
                                    "adsystem", "adbraim", "adcolony", "adnxs", "advertising", "/ads/", "analytics.",
                                    "pagead", "tracker", "telemetry", "facebook.com/tr", "scorecardresearch", "quantserve",
                                    "popads", "popunder", "amazon-adsystem", "criteo", "pubmatic", "rubiconproject",
                                    "outbrain", "taboola", "chartbeat", "optimizely", "mixpanel", "hotjar", "adsafeprotect",
                                    "adform", "smartadserver", "adtech", "revcontent", "adroll", "trafficjunky", "popcash",
                                    "mgid", "propellerads", "exoclick", "disqus", "yandex", "tracking", "pixel", "beacon",
                                    "analytics-", "clickguard", "statcounter", "loggly", "bugsnag", "segment.io"
                                )
                                val isAdOrTracker = adKeywords.any { urlString.contains(it) }
                                
                                if (isAdOrTracker) {
                                    return android.webkit.WebResourceResponse(
                                        "text/plain", 
                                        "UTF-8", 
                                        java.io.ByteArrayInputStream("".toByteArray())
                                    )
                                }
                            }
                            
                            // Web font blocking for text-only mode to speed up text display & save bandwidth
                            if (isTextOnly) {
                                val path = request.url.path?.lowercase() ?: ""
                                if (path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf") || path.endsWith(".otf") || urlString.contains("fonts.googleapis.com")) {
                                    return android.webkit.WebResourceResponse(
                                        "text/plain", 
                                        "UTF-8", 
                                        java.io.ByteArrayInputStream("".toByteArray())
                                    )
                                }
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: android.webkit.SslErrorHandler?,
                        error: android.net.http.SslError?
                    ) {
                        // Allow loading sites with certificate issues, very helpful on local networks and emulators
                        handler?.proceed()
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }
                }
                
                // Enable hardware acceleration for CSS animations and list scrolling
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    allowFileAccess = true // Enable local file access to load downloaded pages
                    loadsImagesAutomatically = !isTextOnly
                    
                    // Support zooming for better readability on tiny smartwatch screens
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false // Hide default zoom buttons to keep screen clean
                    
                    // Text auto-scaling to optimize paragraphs for tiny watch viewports
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                    
                    // Allow mixed content (HTTP content inside HTTPS site)
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    
                    // Pre-rasterize offscreen content for much smoother mechanical crown scrolling performance
                    offscreenPreRaster = true

                    // Standard Android Mobile UserAgent to avoid getting blank or "unsupported browser" pages on some websites
                    userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

                    cacheMode = if (isAggressiveCaching || isPowerSaving) {
                        WebSettings.LOAD_CACHE_ELSE_NETWORK
                    } else {
                        WebSettings.LOAD_DEFAULT
                    }
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val method = android.view.View::class.java.getMethod(
                            "setFrameRate",
                            Float::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType
                        )
                        method.invoke(this, if (isPowerSaving) 30f else 0f, 0)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        },
        update = { webView ->
            val webViewUrl = webView.url
            val areSame = areUrlsSame(webViewUrl, url)
            
            if (url != "pixelbrowser://home" && !url.startsWith("pixelbrowser://")) {
                if (!areSame && url != lastLoadedUrl) {
                    lastLoadedUrl = url
                    webView.loadUrl(url)
                }
            }
            
            webView.settings.textZoom = textZoom
            webView.settings.javaScriptEnabled = true
            webView.settings.loadsImagesAutomatically = !isTextOnly
            webView.settings.cacheMode = if (isAggressiveCaching || isPowerSaving) {
                WebSettings.LOAD_CACHE_ELSE_NETWORK
            } else {
                WebSettings.LOAD_DEFAULT
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val method = android.view.View::class.java.getMethod(
                        "setFrameRate",
                        Float::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                    method.invoke(webView, if (isPowerSaving) 30f else 0f, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
fun CommonSiteIcon(url: String, name: String, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor, iconText) = when (name) {
        "Google" -> Triple(androidx.compose.ui.graphics.Color(0xFFF2F2F2), androidx.compose.ui.graphics.Color(0xFF4285F4), "G")
        "Wikipedia" -> Triple(androidx.compose.ui.graphics.Color(0xFFE6E6E6), androidx.compose.ui.graphics.Color(0xFF000000), "W")
        "Yahoo" -> Triple(androidx.compose.ui.graphics.Color(0xFF6001D2), androidx.compose.ui.graphics.Color(0xFFFFFFFF), "Y")
        "YouTube" -> Triple(androidx.compose.ui.graphics.Color(0xFFFF0000), androidx.compose.ui.graphics.Color(0xFFFFFFFF), "YT")
        "GitHub" -> Triple(androidx.compose.ui.graphics.Color(0xFF24292E), androidx.compose.ui.graphics.Color(0xFFFFFFFF), "GH")
        "Reddit" -> Triple(androidx.compose.ui.graphics.Color(0xFFFF4500), androidx.compose.ui.graphics.Color(0xFFFFFFFF), "R")
        else -> Triple(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface, name.take(1))
    }

    var isIconLoaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(backgroundColor, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://www.google.com/s2/favicons?sz=64&domain_url=$url")
                .crossfade(true)
                .build(),
            contentDescription = name,
            onSuccess = { isIconLoaded = true },
            onError = { isIconLoaded = false },
            modifier = Modifier.fillMaxSize().padding(if (isIconLoaded) 6.dp else 0.dp)
        )

        if (!isIconLoaded) {
            Text(
                text = iconText,
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                maxLines = 1
            )
        }
    }
}

@Composable
fun Modifier.expressiveScale(interactionSource: MutableInteractionSource): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ExpressiveScale"
    )
    return this.scale(scale)
}

@Composable
fun PulsingRingButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "Scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing border ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
                .background(
                    color = ringColor.copy(alpha = pulseAlpha),
                    shape = CircleShape
                )
        )

        // Main button background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerColor, shape = CircleShape)
                .border(1.5.dp, ringColor.copy(alpha = 0.3f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
        }
    }
}

@Composable
fun ExpressiveVoiceWave(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceWave")
    val animValue1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Wave1"
    )
    val animValue2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Wave2"
    )
    val animValue3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Wave3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(animValue1, animValue2, animValue3, animValue2 * 0.9f).forEach { heightPercent ->
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = (18.dp * heightPercent))
                    .background(
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}

@Composable
fun ExpressiveDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        var animateIn by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            animateIn = true
        }
        val scale by animateFloatAsState(
            targetValue = if (animateIn) 1.0f else 0.75f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "DialogScale"
        )
        val dialogAlpha by animateFloatAsState(
            targetValue = if (animateIn) 1.0f else 0.0f,
            animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
            label = "DialogAlpha"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .scale(scale)
                .alpha(dialogAlpha),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@OptIn(androidx.wear.compose.material3.ExperimentalWearMaterial3Api::class)
@Composable
fun BookmarkMenu(
    bookmarks: List<com.example.data.Bookmark>,
    downloadedFiles: List<com.example.data.DownloadedFile>,
    history: List<com.example.data.HistoryEntry>,
    searchHistory: List<com.example.data.SearchHistory>,
    onAddSearchHistory: (String) -> Unit,
    onDeleteSearchHistory: (Long) -> Unit,
    onClearSearchHistory: () -> Unit,
    currentUrl: String,
    pageTitle: String,
    onAddBookmark: (String, String) -> Unit,
    textZoom: Int,
    onSetTextZoom: (Int) -> Unit,
    isSpeaking: Boolean,
    onToggleSpeak: () -> Unit,
    onTranslate: (String) -> Unit,
    onDeleteDownloadedFile: (Long, String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onDeleteHistoryEntry: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    onToggleDeepMode: () -> Unit,
    isDeepMode: Boolean,
    isCloudRendering: Boolean,
    onToggleCloudRendering: () -> Unit,
    isTextOnly: Boolean,
    onToggleTextOnly: () -> Unit,
    isAggressiveCaching: Boolean,
    onToggleAggressiveCaching: () -> Unit,
    isAdBlockEnabled: Boolean,
    onToggleAdBlock: () -> Unit,
    isCpuThrottleEnabled: Boolean,
    onToggleCpuThrottle: () -> Unit,
    isSmartRamCleanerEnabled: Boolean,
    onToggleSmartRamCleaner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showSearchMethodDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showTranslateMenu by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                onNavigate(if (spokenText.contains(".") && !spokenText.contains(" ")) {
                    if (spokenText.startsWith("http")) spokenText else "https://$spokenText"
                } else {
                    val encoded = java.net.URLEncoder.encode(spokenText, "UTF-8")
                    "https://www.google.com/search?q=$encoded"
                })
                onClose()
            }
        }
    }

    fun startVoiceSearch() {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "請說出搜尋內容")
        }
        voiceLauncher.launch(intent)
    }

    // Check if current URL is a translatable web page (http/https and not local/blank files)
    val isTranslatable = currentUrl.startsWith("http://") || currentUrl.startsWith("https://")

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus request failure
        }
    }

    ScreenScaffold(
        scrollState = listState,
        bottomButton = {
            EdgeButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClose()
                }
            ) {
                Icon(Icons.Default.Close, contentDescription = "關閉", modifier = Modifier.size(24.dp))
                Text("關閉")
            }
        }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onRotaryScrollEvent {
                        coroutineScope.launch {
                            listState.scrollBy(it.verticalScrollPixels)
                        }
                        true
                    }
            ) {
                item {
                    ListHeader {
                        Text("功能導航", style = MaterialTheme.typography.titleMedium)
                    }
                }

                item {
                    val addBookmarkInteraction = remember { MutableInteractionSource() }
                    val isAlreadyBookmarked = bookmarks.any { it.url == currentUrl }
                    
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isAlreadyBookmarked) {
                                onDeleteBookmark(currentUrl)
                            } else {
                                onAddBookmark(currentUrl, pageTitle)
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAlreadyBookmarked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(addBookmarkInteraction),
                        interactionSource = addBookmarkInteraction
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isAlreadyBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isAlreadyBookmarked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                if (isAlreadyBookmarked) "已加入書籤" else "新增至書籤",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isAlreadyBookmarked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                item {
                    val homeInteraction = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigate("pixelbrowser://home")
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(homeInteraction),
                        interactionSource = homeInteraction
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "首頁圖標",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text("首頁", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }

                // Share to Phone
                item {
                    val shareInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, currentUrl)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "分享至手機"))
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(shareInt),
                        interactionSource = shareInt
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "分享圖標",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text("分享至手機", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }



                item {
                    ListHeader {
                        Text("瀏覽器設定", style = MaterialTheme.typography.titleMedium)
                    }
                }
                
                item {
                    val deepModeInteraction = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleDeepMode()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(deepModeInteraction),
                        interactionSource = deepModeInteraction,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OLED 純黑模式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isDeepMode) "極致省電純黑背景" else "標準深色主題", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (isDeepMode) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "開關狀態",
                                tint = if (isDeepMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // --- Web Performance Optimizations Category ---
                item {
                    ListHeader {
                        Text("網頁效能優化", style = MaterialTheme.typography.titleSmall)
                    }
                }

                // Cloud Rendering
                item {
                    val cloudInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCloudRendering()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(cloudInt),
                        interactionSource = cloudInt,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("雲端簡化渲染", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isCloudRendering) "已啟用：剔除動畫/降載CPU" else "標準渲染模式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (isCloudRendering) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "開關狀態",
                                tint = if (isCloudRendering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Text Only / Text Folding
                item {
                    val textOnlyInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleTextOnly()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(textOnlyInt),
                        interactionSource = textOnlyInt,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("純文字與折疊", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isTextOnly) "已啟用：阻擋圖片/標題折疊" else "顯示完整媒體", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (isTextOnly) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "開關狀態",
                                tint = if (isTextOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Aggressive Caching
                item {
                    val cacheInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleAggressiveCaching()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(cacheInt),
                        interactionSource = cacheInt,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("積極快取載入", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isAggressiveCaching) "優先載入快取，省流極速" else "每次皆重新下載", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (isAggressiveCaching) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "開關狀態",
                                tint = if (isAggressiveCaching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Smart Ad-Blocker
                item {
                    val adBlockInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleAdBlock()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(adBlockInt),
                        interactionSource = adBlockInt,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("智慧廣告阻擋", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isAdBlockEnabled) "已啟用：自動剔除廣告/追蹤" else "未啟用智慧阻擋", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (isAdBlockEnabled) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "開關狀態",
                                tint = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // CPU Throttle
                item {
                    val cpuThrottleInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCpuThrottle()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(cpuThrottleInt),
                        interactionSource = cpuThrottleInt,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("智慧 CPU 負載抑制", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isCpuThrottleEnabled) "已啟用：延緩背景計時器/省電" else "標準背景執行", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (isCpuThrottleEnabled) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "開關狀態",
                                tint = if (isCpuThrottleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Smart RAM Cleaner
                item {
                    val ramCleanerInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSmartRamCleaner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(ramCleanerInt),
                        interactionSource = ramCleanerInt,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("智慧記憶體清理", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isSmartRamCleanerEnabled) "已啟用：返回主頁即釋放暫存" else "未啟用記憶體自動清理", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (isSmartRamCleanerEnabled) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "開關狀態",
                                tint = if (isSmartRamCleanerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Real-Time TTS Voice Reader (Read Aloud)
                item {
                    val speakInteraction = remember { MutableInteractionSource() }
                    Card(
                        onClick = onToggleSpeak,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(speakInteraction),
                        interactionSource = speakInteraction,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSpeaking) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("語音朗讀網頁", style = MaterialTheme.typography.labelMedium, color = if (isSpeaking) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(if (isSpeaking) "正為您朗讀中..." else "智慧 TTS 語音朗讀器", style = MaterialTheme.typography.bodySmall, color = if (isSpeaking) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                            if (isSpeaking) {
                                ExpressiveVoiceWave(modifier = Modifier.padding(end = 4.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "朗讀圖標",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Web Page Text Zoom Control
                item {
                    Card(
                        onClick = {},
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Text Zoom: $textZoom%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { if (textZoom > 60) onSetTextZoom(textZoom - 20) },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease text size",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onSetTextZoom(100) },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset text size",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { if (textZoom < 240) onSetTextZoom(textZoom + 20) },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase text size",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isTranslatable) {
                    item {
                        ListHeader {
                            Text("網頁翻譯", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    val languages = listOf(
                        "zh-TW" to "繁體中文",
                        "zh-CN" to "简体中文",
                        "en" to "English",
                        "ja" to "日本語",
                        "es" to "Español",
                        "fr" to "Français",
                        "de" to "Deutsch",
                        "ko" to "한국어"
                    )

                    if (!showTranslateMenu) {
                        item {
                            Card(
                                onClick = { showTranslateMenu = true },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Translate,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("翻譯", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                onClick = { showTranslateMenu = false },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 8.dp)
                            ) {
                                Text("返回", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(8.dp))
                            }
                        }
                        languages.forEach { (code, name) ->
                            item {
                                Card(
                                    onClick = {
                                        onTranslate(code)
                                        showTranslateMenu = false
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Translate,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }



                item {
                    ListHeader {
                        Text("下載項目", style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (downloadedFiles.isEmpty()) {
                    item {
                        Text("尚無下載檔案", style = MaterialTheme.typography.bodySmall)
                    }
                }

                items(items = downloadedFiles, key = { it.id }) { file ->
                    Card(
                        onClick = { onNavigate("file://${file.localPath}") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.fileName, style = MaterialTheme.typography.labelMedium, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                Text(formatFileSize(file.fileSize), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick = { onDeleteDownloadedFile(file.id, file.localPath) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "刪除檔案",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

            }

            PositionIndicator(
                scalingLazyListState = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            SearchMethodDialog(
                show = showSearchMethodDialog,
                onDismiss = { showSearchMethodDialog = false },
                onVoiceSearch = { startVoiceSearch() },
                onKeyboardSearch = { showSearchDialog = true }
            )

            SearchDialog(
                show = showSearchDialog,
                onDismiss = { showSearchDialog = false },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchHistory = searchHistory,
                onAddSearchHistory = onAddSearchHistory,
                onDeleteSearchHistory = onDeleteSearchHistory,
                onNavigate = { url ->
                    onNavigate(url)
                    onClose()
                }
            )
        }
}

@OptIn(androidx.wear.compose.material3.ExperimentalWearMaterial3Api::class)
@Composable
fun HomeScreen(
    bookmarks: List<com.example.data.Bookmark>,
    localBookmarks: List<com.example.data.LocalBookmark>,
    onAddLocalBookmark: (String, String) -> Unit,
    onDeleteLocalBookmark: (String) -> Unit,
    downloadedFiles: List<com.example.data.DownloadedFile>,
    onDeleteDownloadedFile: (Long, String) -> Unit,
    history: List<com.example.data.HistoryEntry>,
    searchHistory: List<com.example.data.SearchHistory>,
    onAddSearchHistory: (String) -> Unit,
    onDeleteSearchHistory: (Long) -> Unit,
    onClearSearchHistory: () -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onNavigate: (String) -> Unit,
    textZoom: Int,
    onSetTextZoom: (Int) -> Unit,
    isDeepMode: Boolean,
    onToggleDeepMode: () -> Unit,
    onClearHistory: () -> Unit,
    onUpdateBookmark: (com.example.data.Bookmark) -> Unit,
    isCloudRendering: Boolean,
    onToggleCloudRendering: () -> Unit,
    isTextOnly: Boolean,
    onToggleTextOnly: () -> Unit,
    isAggressiveCaching: Boolean,
    onToggleAggressiveCaching: () -> Unit,
    isAdBlockEnabled: Boolean,
    onToggleAdBlock: () -> Unit,
    isCpuThrottleEnabled: Boolean,
    onToggleCpuThrottle: () -> Unit,
    isSmartRamCleanerEnabled: Boolean,
    onToggleSmartRamCleaner: () -> Unit,
    isCircularSafeMode: Boolean,
    onToggleCircularSafeMode: () -> Unit,
    isIncognito: Boolean,
    onToggleIncognito: () -> Unit,
    imageCacheSize: String,
    voiceCacheSize: String,
    onClearCache: () -> Unit,
    onRefreshCache: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var showSearchMethodDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }

    val voiceLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                onNavigate(if (spokenText.contains(".") && !spokenText.contains(" ")) {
                    if (spokenText.startsWith("http")) spokenText else "https://$spokenText"
                } else {
                    val encoded = java.net.URLEncoder.encode(spokenText, "UTF-8")
                    "https://www.google.com/search?q=$encoded"
                })
            }
        }
    }

    fun startVoiceSearch() {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "請說出搜尋內容")
        }
        voiceLauncher.launch(intent)
    }

    var searchQuery by remember { mutableStateOf("") }
    var showAllBookmarksDialog by remember { mutableStateOf(false) }
    var showAddLocalBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkToEdit by remember { mutableStateOf<com.example.data.Bookmark?>(null) }
    var bookmarkToDelete by remember { mutableStateOf<com.example.data.Bookmark?>(null) }
    var swipeOffset by remember { mutableStateOf(0f) }
    val dragThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 50.dp.toPx() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus request failure
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeOffset = 0f },
                    onDragEnd = {
                        if (swipeOffset < -dragThresholdPx) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigate("pixelbrowser://history")
                        }
                        swipeOffset = 0f
                    },
                    onDragCancel = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset = (swipeOffset + dragAmount).coerceAtMost(0f)
                    }
                )
            }
    ) {
        ScreenScaffold(
            scrollState = listState,
            bottomButton = {
                EdgeButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showSettingsDialog = true
                    }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "設定", modifier = Modifier.size(24.dp))
                    Text("設定")
                }
            }
        ) {
            ScalingLazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onRotaryScrollEvent {
                        coroutineScope.launch {
                            listState.scrollBy(it.verticalScrollPixels)
                        }
                        true
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ListHeader {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Pixel Browser", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                item {
                    val searchInteraction = remember { MutableInteractionSource() }
                    val incognitoInteraction = remember { MutableInteractionSource() }
                    
                    val searchBarBgColor by animateColorAsState(
                        targetValue = if (isIncognito) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        animationSpec = tween(durationMillis = 300),
                        label = "SearchBarBg"
                    )
                    val searchBarContentColor by animateColorAsState(
                        targetValue = if (isIncognito) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        animationSpec = tween(durationMillis = 300),
                        label = "SearchBarContent"
                    )
                    val incognitoBgColor by animateColorAsState(
                        targetValue = if (isIncognito) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        animationSpec = tween(durationMillis = 300),
                        label = "IncognitoBg"
                    )
                    val incognitoIconColor by animateColorAsState(
                        targetValue = if (isIncognito) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        animationSpec = tween(durationMillis = 300),
                        label = "IncognitoIcon"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showSearchMethodDialog = true
                            },
                            colors = CardDefaults.cardColors(containerColor = searchBarBgColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .expressiveScale(searchInteraction),
                            contentPadding = PaddingValues(0.dp),
                            interactionSource = searchInteraction
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "搜尋圖標",
                                    modifier = Modifier.size(20.dp),
                                    tint = searchBarContentColor
                                )
                                Text(
                                    "搜尋或輸入網址",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = searchBarContentColor,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showSearchMethodDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "語音搜尋",
                                        tint = searchBarContentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleIncognito()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .expressiveScale(incognitoInteraction)
                                .then(
                                    if (isIncognito) {
                                        Modifier.border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = CircleShape
                                        )
                                    } else Modifier
                                )
                                .background(
                                    color = incognitoBgColor,
                                    shape = CircleShape
                                ),
                            interactionSource = incognitoInteraction
                        ) {
                            Icon(
                                if (isIncognito) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "無痕模式",
                                modifier = Modifier.size(20.dp),
                                tint = incognitoIconColor
                            )
                        }
                    }
                }

                item {
                    val downloadsInt = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showDownloadsDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .expressiveScale(downloadsInt),
                        interactionSource = downloadsInt
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "下載",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item {
                    ListHeader {
                        Text("常用網站", style = MaterialTheme.typography.titleMedium)
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val links1 = listOf(
                            "https://www.google.com" to "Google",
                            "https://en.wikipedia.org" to "Wikipedia",
                            "https://tw.yahoo.com" to "Yahoo"
                        )
                        links1.forEach { (url, name) ->
                            val interactionSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = { onNavigate(url) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .expressiveScale(interactionSource),
                                interactionSource = interactionSource
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(8.dp))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CommonSiteIcon(
                                        url = url,
                                        name = name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val links2 = listOf(
                            "https://m.youtube.com" to "YouTube",
                            "https://github.com" to "GitHub",
                            "https://www.reddit.com" to "Reddit"
                        )
                        links2.forEach { (url, name) ->
                            val interactionSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = { onNavigate(url) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .expressiveScale(interactionSource),
                                interactionSource = interactionSource
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(8.dp))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CommonSiteIcon(
                                        url = url,
                                        name = name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                    }
                }

                if (localBookmarks.isNotEmpty()) {
                    item {
                        ListHeader {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("常用網址", style = MaterialTheme.typography.titleMedium)
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showAddLocalBookmarkDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "新增網址",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    items(items = localBookmarks, key = { "local_${it.url}" }) { bookmark ->
                        val cardInt = remember { MutableInteractionSource() }
                        CompactListRow(
                            onClick = { onNavigate(bookmark.url) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteLocalBookmark(bookmark.url)
                            },
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDeleteLocalBookmark(bookmark.url)
                            },
                            title = bookmark.title,
                            url = bookmark.url,
                            modifier = Modifier.expressiveScale(cardInt),
                            interactionSource = cardInt
                        )
                    }
                } else {
                    item {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showAddLocalBookmarkDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("建立常用網址", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                if (bookmarks.isNotEmpty()) {
                    item {
                        ListHeader(
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showAllBookmarksDialog = true
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("書籤", style = MaterialTheme.typography.titleMedium)
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "管理書籤",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    items(items = bookmarks.take(3), key = { it.url }) { bookmark ->
                        val cardInt = remember { MutableInteractionSource() }
                        CompactListRow(
                            onClick = { onNavigate(bookmark.url) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteBookmark(bookmark.url)
                            },
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDeleteBookmark(bookmark.url)
                            },
                            title = bookmark.title,
                            url = bookmark.url,
                            modifier = Modifier.expressiveScale(cardInt),
                            interactionSource = cardInt
                        )
                    }

                    if (bookmarks.size > 3) {
                        item {
                            val moreInt = remember { MutableInteractionSource() }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showAllBookmarksDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .expressiveScale(moreInt),
                                interactionSource = moreInt
                            ) {
                                Text("顯示所有書籤 (${bookmarks.size})", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }


            }

            PositionIndicator(
                scalingLazyListState = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Visual history swipe gesture indicator (Right Side)
        if (swipeOffset < 0f) {
            val swipeAbs = -swipeOffset
            val progress = (swipeAbs / dragThresholdPx).coerceIn(0f, 1.2f)
            val isTriggered = swipeAbs >= dragThresholdPx
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        translationX = (40.dp.toPx() - (swipeAbs * 0.6f)).coerceAtLeast(-16.dp.toPx())
                        alpha = progress.coerceIn(0f, 1f)
                        scaleX = 0.8f + (progress * 0.2f).coerceAtMost(0.4f)
                        scaleY = 0.8f + (progress * 0.2f).coerceAtMost(0.4f)
                    }
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .size(40.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "瀏覽紀錄",
                    tint = if (isTriggered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    SearchMethodDialog(
        show = showSearchMethodDialog,
        onDismiss = { showSearchMethodDialog = false },
        onVoiceSearch = { startVoiceSearch() },
        onKeyboardSearch = { showSearchDialog = true }
    )

    SearchDialog(
        show = showSearchDialog,
        onDismiss = { showSearchDialog = false },
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        searchHistory = searchHistory,
        onAddSearchHistory = onAddSearchHistory,
        onDeleteSearchHistory = onDeleteSearchHistory,
        onNavigate = onNavigate
    )

    SettingsDialog(
        show = showSettingsDialog,
        onDismiss = { showSettingsDialog = false },
        textZoom = textZoom,
        onSetTextZoom = onSetTextZoom,
        isDeepMode = isDeepMode,
        onToggleDeepMode = onToggleDeepMode,
        isAdBlockEnabled = isAdBlockEnabled,
        onToggleAdBlock = onToggleAdBlock,
        isTextOnly = isTextOnly,
        onToggleTextOnly = onToggleTextOnly,
        isCircularSafeMode = isCircularSafeMode,
        onToggleCircularSafeMode = onToggleCircularSafeMode,
        onClearHistory = onClearHistory,
        imageCacheSize = imageCacheSize,
        voiceCacheSize = voiceCacheSize,
        onClearCache = onClearCache,
        onRefreshCache = onRefreshCache
    )

    BookmarkEditDialog(
        bookmark = bookmarkToEdit,
        onDismiss = { bookmarkToEdit = null },
        onUpdateBookmark = onUpdateBookmark
    )

    BookmarkDeleteDialog(
        bookmark = bookmarkToDelete,
        onDismiss = { bookmarkToDelete = null },
        onDeleteBookmark = onDeleteBookmark
    )

    AddLocalBookmarkDialog(
        show = showAddLocalBookmarkDialog,
        onDismiss = { showAddLocalBookmarkDialog = false },
        onAdd = { title, url ->
            onAddLocalBookmark(url, title)
            showAddLocalBookmarkDialog = false
        }
    )

    AllBookmarksDialog(
        show = showAllBookmarksDialog,
        onDismiss = { showAllBookmarksDialog = false },
        bookmarks = bookmarks,
        onNavigate = onNavigate,
        onEditBookmark = { bookmarkToEdit = it },
        onDeleteBookmark = { bookmarkToDelete = it }
    )

    DownloadsDialog(
        show = showDownloadsDialog,
        onDismiss = { showDownloadsDialog = false },
        downloadedFiles = downloadedFiles,
        onNavigate = onNavigate,
        onDeleteDownloadedFile = onDeleteDownloadedFile
    )
}

@Composable
fun HistoryScreen(
    history: List<com.example.data.HistoryEntry>,
    searchHistory: List<com.example.data.SearchHistory>,
    onNavigate: (String) -> Unit,
    onDeleteHistoryEntry: (Long) -> Unit,
    onDeleteSearchHistory: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onAddSearchHistory: (String) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var swipeOffset by remember { mutableStateOf(0f) }
    val dragThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 50.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeOffset = 0f },
                    onDragEnd = {
                        if (swipeOffset > dragThresholdPx) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBack()
                        }
                        swipeOffset = 0f
                    },
                    onDragCancel = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset = (swipeOffset + dragAmount).coerceAtLeast(0f)
                    }
                )
            }
    ) {
        ScalingLazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.scrollBy(it.verticalScrollPixels)
                    }
                    true
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ListHeader {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("瀏覽與搜尋紀錄", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            if (history.isEmpty() && searchHistory.isEmpty()) {
                item {
                    Text(
                        "尚無紀錄",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            if (searchHistory.isNotEmpty()) {
                item {
                    ListHeader {
                        Text("搜尋紀錄", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                items(items = searchHistory, key = { it.id }) { entry ->
                    val cardInt = remember { MutableInteractionSource() }
                    CompactListRow(
                        onClick = {
                            onAddSearchHistory(entry.query)
                            val dest = if (entry.query.contains(".") && !entry.query.contains(" ")) {
                                if (entry.query.startsWith("http")) entry.query else "https://${entry.query}"
                            } else {
                                val encoded = java.net.URLEncoder.encode(entry.query, "UTF-8")
                                "https://www.google.com/search?q=$encoded"
                            }
                            onNavigate(dest)
                        },
                        onDelete = { onDeleteSearchHistory(entry.id) },
                        title = entry.query,
                        url = "搜尋",
                        modifier = Modifier.expressiveScale(cardInt),
                        interactionSource = cardInt
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            if (history.isNotEmpty() || searchHistory.isNotEmpty()) {
                item {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClearHistory()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Text("清除所有紀錄", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        PositionIndicator(
            scalingLazyListState = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // Visual swipe-to-back indicator (Left Side, swiping right to return home)
        if (swipeOffset > 0f) {
            val progress = (swipeOffset / dragThresholdPx).coerceIn(0f, 1.2f)
            val isTriggered = swipeOffset >= dragThresholdPx
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        translationX = (-40.dp.toPx() + (swipeOffset * 0.6f)).coerceAtMost(16.dp.toPx())
                        alpha = progress.coerceIn(0f, 1f)
                        scaleX = 0.8f + (progress * 0.2f).coerceAtMost(0.4f)
                        scaleY = 0.8f + (progress * 0.2f).coerceAtMost(0.4f)
                    }
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .size(40.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "返回首頁",
                    tint = if (isTriggered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

fun isNetworkAvailable(context: android.content.Context): Boolean {
    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
    if (connectivityManager != null) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }
    return false
}

@Composable
fun OfflineScreen(
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "No internet connection",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "你的手錶未連上網際網路。請重新連線，然後再試一次。",
                color = Color.White,
                style = TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(56.dp))
        }

        EdgeButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDismiss()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .width(130.dp)
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("關閉", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactListRow(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    title: String,
    url: String,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(28.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (onLongClick != null) {
                        onLongClick.invoke()
                    } else if (onDelete != null) {
                        onDelete.invoke()
                    }
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Icon
        var isIconLoaded by remember { mutableStateOf(false) }
        val isWebUrl = url.startsWith("http://") || url.startsWith("https://")
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.45f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isWebUrl) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://www.google.com/s2/favicons?sz=64&domain_url=$url")
                        .crossfade(true)
                        .build(),
                    contentDescription = "網頁圖標",
                    onSuccess = { isIconLoaded = true },
                    onError = { isIconLoaded = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isIconLoaded) 8.dp else 0.dp)
                        .clip(CircleShape)
                )
            }
            if (!isIconLoaded) {
                Text(
                    text = title.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Text Content inside a nested capsule container
        val isSearch = url == "搜尋" || url == "搜尋詞"
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (url.isNotEmpty() && !isSearch) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SearchDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchHistory: List<SearchHistory>,
    onAddSearchHistory: (String) -> Unit,
    onDeleteSearchHistory: (Long) -> Unit,
    onNavigate: (String) -> Unit
) {
    if (!show) return

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    ExpressiveDialog(onDismissRequest = onDismiss) {
        val searchListState = rememberScalingLazyListState()
        val searchFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            try {
                searchFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failure
            }
        }

        ScreenScaffold(
            scrollState = searchListState,
            bottomButton = {
                EdgeButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "關閉",
                        modifier = Modifier.size(24.dp)
                    )
                    Text("關閉")
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                ScalingLazyColumn(
                    state = searchListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(searchFocusRequester)
                        .focusable()
                        .onRotaryScrollEvent {
                            coroutineScope.launch {
                                searchListState.scrollBy(it.verticalScrollPixels)
                            }
                            true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                item {
                    ListHeader {
                        Text("搜尋與網址", style = MaterialTheme.typography.titleMedium)
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        onAddSearchHistory(searchQuery)
                                        val dest = if (searchQuery.contains(".") && !searchQuery.contains(" ")) {
                                            if (searchQuery.startsWith("http")) searchQuery else "https://$searchQuery"
                                        } else {
                                            val encoded = URLEncoder.encode(searchQuery, "UTF-8")
                                            "https://www.google.com/search?q=$encoded"
                                        }
                                        onNavigate(dest)
                                        onDismiss()
                                    }
                                }
                            )
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                onAddSearchHistory(searchQuery)
                                val dest = if (searchQuery.contains(".") && !searchQuery.contains(" ")) {
                                    if (searchQuery.startsWith("http")) searchQuery else "https://$searchQuery"
                                } else {
                                    val encoded = URLEncoder.encode(searchQuery, "UTF-8")
                                    "https://www.google.com/search?q=$encoded"
                                }
                                onNavigate(dest)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("前往", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                if (searchHistory.isNotEmpty()) {
                    item {
                        ListHeader {
                            Text("最近搜尋", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    items(items = searchHistory.take(5), key = { it.id }) { entry ->
                        CompactListRow(
                            onClick = {
                                onSearchQueryChange(entry.query)
                            },
                            onDelete = { onDeleteSearchHistory(entry.id) },
                            title = entry.query,
                            url = entry.timestamp.toString() // Placeholder or actual time
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }

            PositionIndicator(
                scalingLazyListState = searchListState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
}

@Composable
fun SearchMethodDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onVoiceSearch: () -> Unit,
    onKeyboardSearch: () -> Unit
) {
    if (!show) return

    val haptic = LocalHapticFeedback.current

    ExpressiveDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "使用Google搜尋",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 語音搜尋按鈕
                    val voiceInteraction = remember { MutableInteractionSource() }
                    PulsingRingButton(
                        onClick = {
                            onVoiceSearch()
                            onDismiss()
                        },
                        icon = Icons.Default.Mic,
                        contentDescription = "語音搜尋",
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .padding(horizontal = 12.dp),
                        ringColor = androidx.compose.ui.graphics.Color(0xFF00E5FF),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        interactionSource = voiceInteraction
                    )

                    // 鍵盤搜尋按鈕
                    val keyboardInteraction = remember { MutableInteractionSource() }
                    PulsingRingButton(
                        onClick = {
                            onKeyboardSearch()
                            onDismiss()
                        },
                        icon = Icons.Default.Keyboard,
                        contentDescription = "鍵盤搜尋",
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .padding(horizontal = 12.dp),
                        ringColor = androidx.compose.ui.graphics.Color(0xFFD500F9),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        interactionSource = keyboardInteraction
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    textZoom: Int,
    onSetTextZoom: (Int) -> Unit,
    isDeepMode: Boolean,
    onToggleDeepMode: () -> Unit,
    isAdBlockEnabled: Boolean,
    onToggleAdBlock: () -> Unit,
    isTextOnly: Boolean,
    onToggleTextOnly: () -> Unit,
    isCircularSafeMode: Boolean,
    onToggleCircularSafeMode: () -> Unit,
    onClearHistory: () -> Unit,
    imageCacheSize: String,
    voiceCacheSize: String,
    onClearCache: () -> Unit,
    onRefreshCache: () -> Unit
) {
    if (!show) return

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    ExpressiveDialog(onDismissRequest = onDismiss) {
        val settingsListState = rememberScalingLazyListState()
        val settingsFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            onRefreshCache()
            try {
                settingsFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore
            }
        }

        ScreenScaffold(
            scrollState = settingsListState,
            bottomButton = {
                EdgeButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "完成",
                        modifier = Modifier.size(24.dp)
                    )
                    Text("完成")
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                ScalingLazyColumn(
                state = settingsListState,
                contentPadding = PaddingValues(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 56.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(settingsFocusRequester)
                    .focusable()
                    .onRotaryScrollEvent {
                        coroutineScope.launch {
                            settingsListState.scrollBy(it.verticalScrollPixels)
                        }
                        true
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ListHeader {
                        Text("快速設定", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // 1. 字型大小 (Text Zoom)
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("網頁字型大小", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            val zooms = listOf(75, 100, 125, 150, 175, 200)
                            val currentIndex = zooms.indexOf(textZoom).takeIf { it >= 0 } ?: 1
                            Button(
                                onClick = { if (currentIndex > 0) onSetTextZoom(zooms[currentIndex - 1]) },
                                modifier = Modifier.size(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "減少字型大小", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                "$textZoom%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = { if (currentIndex < zooms.size - 1) onSetTextZoom(zooms[currentIndex + 1]) },
                                modifier = Modifier.size(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "增加字型大小", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // 2. 極致純黑模式 (Deep Mode)
                item {
                    val oledInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = { onToggleDeepMode() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDeepMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .expressiveScale(oledInt),
                        interactionSource = oledInt
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "OLED 純黑省電",
                                    color = if (isDeepMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    if (isDeepMode) "已啟用" else "未啟用",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDeepMode) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (isDeepMode) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "切換狀態",
                                tint = if (isDeepMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // --- 圓形螢幕優化 ---
                item {
                    ListHeader {
                        Text("圓形螢幕優化", style = MaterialTheme.typography.titleSmall)
                    }
                }

                item {
                    val circInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = { onToggleCircularSafeMode() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCircularSafeMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .expressiveScale(circInt),
                        interactionSource = circInt
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "防遮擋安全視區",
                                    color = if (isCircularSafeMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    if (isCircularSafeMode) "已啟用：完整顯示不裁剪" else "未啟用：網頁可能被裁剪",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCircularSafeMode) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isCircularSafeMode) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "切換狀態",
                                tint = if (isCircularSafeMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // --- 智慧網頁優化 ---
                item {
                    ListHeader {
                        Text("智慧網頁優化", style = MaterialTheme.typography.titleSmall)
                    }
                }

                // 智慧廣告阻擋
                item {
                    val adBlockInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = { onToggleAdBlock() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAdBlockEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .expressiveScale(adBlockInt),
                        interactionSource = adBlockInt
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "智慧廣告阻擋",
                                    color = if (isAdBlockEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    if (isAdBlockEnabled) "已啟用：自動剔除廣告/追蹤" else "未啟用智慧阻擋",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAdBlockEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isAdBlockEnabled) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "切換狀態",
                                tint = if (isAdBlockEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 純文字與折疊
                item {
                    val textOnlyInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = { onToggleTextOnly() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTextOnly) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .expressiveScale(textOnlyInt),
                        interactionSource = textOnlyInt
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "純文字與折疊",
                                    color = if (isTextOnly) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    if (isTextOnly) "已啟用：阻擋圖片/標題折疊" else "顯示完整媒體",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isTextOnly) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isTextOnly) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "切換狀態",
                                tint = if (isTextOnly) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 快取資源統計與清除 (Cache Usage)
                item {
                    val cacheInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClearCache()
                            Toast.makeText(context, "已清除語音與圖像快取", Toast.LENGTH_SHORT).show()
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .expressiveScale(cacheInt),
                        interactionSource = cacheInt
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                "快取資源統計 (點擊清除)",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = "圖像",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "圖像資源",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    imageCacheSize,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "語音",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "語音資源",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    voiceCacheSize,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 4. 清除瀏覽歷史 (Clear History)
                item {
                    val clearInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            onClearHistory()
                            Toast.makeText(context, "已清除瀏覽歷史", Toast.LENGTH_SHORT).show()
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .expressiveScale(clearInt),
                        interactionSource = clearInt
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "清除",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text(
                                "清除瀏覽歷史",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            PositionIndicator(
                scalingLazyListState = settingsListState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
}

@Composable
fun BookmarkEditDialog(
    bookmark: Bookmark?,
    onDismiss: () -> Unit,
    onUpdateBookmark: (Bookmark) -> Unit
) {
    if (bookmark == null) return

    ExpressiveDialog(onDismissRequest = onDismiss) {
        var newTitle by remember(bookmark) { mutableStateOf(bookmark.title) }
        var newUrl by remember(bookmark) { mutableStateOf(bookmark.url) }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("編輯書籤", style = MaterialTheme.typography.titleSmall)
            androidx.compose.material3.TextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("名稱") })
            androidx.compose.material3.TextField(value = newUrl, onValueChange = { newUrl = it }, label = { Text("網址") })
            Button(onClick = {
                onUpdateBookmark(bookmark.copy(title = newTitle, url = newUrl))
                onDismiss()
            }) {
                Text("儲存")
            }
        }
    }
}

@Composable
fun AddLocalBookmarkDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    if (!show) return

    ExpressiveDialog(onDismissRequest = onDismiss) {
        var newTitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("") }

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failure
            }
        }

        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("新增本機書籤", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            }
            item {
                androidx.compose.material3.TextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("名稱 (例如 Google)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
            item {
                androidx.compose.material3.TextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    label = { Text("網址 (例如 google.com)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Text("取消", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = {
                            if (newUrl.isNotBlank()) {
                                var formattedUrl = newUrl.trim()
                                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                    formattedUrl = "https://$formattedUrl"
                                }
                                onAdd(newTitle.trim().ifBlank { newUrl }, formattedUrl)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("新增", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkDeleteDialog(
    bookmark: Bookmark?,
    onDismiss: () -> Unit,
    onDeleteBookmark: (String) -> Unit
) {
    if (bookmark == null) return

    ExpressiveDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("確定刪除此書籤?", style = MaterialTheme.typography.titleSmall)
            Row {
                Button(onClick = {
                    onDeleteBookmark(bookmark.url)
                    onDismiss()
                }) {
                    Text("刪除")
                }
                Button(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    }
}

@Composable
fun AllBookmarksDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    bookmarks: List<Bookmark>,
    onNavigate: (String) -> Unit,
    onEditBookmark: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit
) {
    if (!show) return

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    ExpressiveDialog(onDismissRequest = onDismiss) {
        val bookmarkListState = rememberScalingLazyListState()
        val bookmarkFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            try {
                bookmarkFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failure
            }
        }

        ScreenScaffold(
            scrollState = bookmarkListState,
            bottomButton = {
                EdgeButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "關閉",
                        modifier = Modifier.size(24.dp)
                    )
                    Text("關閉")
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                ScalingLazyColumn(
                    state = bookmarkListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(bookmarkFocusRequester)
                        .focusable()
                        .onRotaryScrollEvent {
                            coroutineScope.launch {
                                bookmarkListState.scrollBy(it.verticalScrollPixels)
                            }
                            true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                item {
                    ListHeader {
                        Text("我的書籤", style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (bookmarks.isEmpty()) {
                    item {
                        Text("尚無書籤", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    items(items = bookmarks, key = { it.url }) { bookmark ->
                        CompactListRow(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigate(bookmark.url)
                                onDismiss()
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEditBookmark(bookmark)
                            },
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDeleteBookmark(bookmark)
                            },
                            title = bookmark.title,
                            url = bookmark.url
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }

            PositionIndicator(
                scalingLazyListState = bookmarkListState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
}

@Composable
fun DownloadsDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    downloadedFiles: List<com.example.data.DownloadedFile>,
    onNavigate: (String) -> Unit,
    onDeleteDownloadedFile: (Long, String) -> Unit
) {
    if (!show) return

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    ExpressiveDialog(onDismissRequest = onDismiss) {
        val listState = rememberScalingLazyListState()
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failure
            }
        }

        ScreenScaffold(
            scrollState = listState,
            bottomButton = {
                EdgeButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "關閉",
                        modifier = Modifier.size(24.dp)
                    )
                    Text("關閉")
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onRotaryScrollEvent {
                            coroutineScope.launch {
                                listState.scrollBy(it.verticalScrollPixels)
                            }
                            true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                item {
                    ListHeader {
                        Text("下載項目", style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (downloadedFiles.isEmpty()) {
                    item {
                        Text("尚無下載檔案", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    items(items = downloadedFiles, key = { it.id }) { file ->
                        val cardInt = remember { MutableInteractionSource() }
                        Card(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigate("file://${file.localPath}")
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .expressiveScale(cardInt),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            interactionSource = cardInt
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        file.fileName,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        formatFileSize(file.fileSize),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDeleteDownloadedFile(file.id, file.localPath)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "刪除檔案",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }

            PositionIndicator(
                scalingLazyListState = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
}

@Composable
fun QrDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    currentUrl: String
) {
    if (!show) return

    val coroutineScope = rememberCoroutineScope()

    ExpressiveDialog(onDismissRequest = onDismiss) {
        val qrListState = rememberScalingLazyListState()
        val qrFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            try {
                qrFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failure
            }
        }

        ScreenScaffold(
            scrollState = qrListState,
            bottomButton = {
                EdgeButton(
                    onClick = {
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "關閉 QR 碼",
                        modifier = Modifier.size(24.dp)
                    )
                    Text("關閉")
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                ScalingLazyColumn(
                    state = qrListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(qrFocusRequester)
                        .focusable()
                        .onRotaryScrollEvent {
                            coroutineScope.launch {
                                qrListState.scrollBy(it.verticalScrollPixels)
                            }
                            true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                item {
                    ListHeader {
                        Text("Open on Phone", style = MaterialTheme.typography.titleMedium)
                    }
                }
                
                item {
                    Text(
                        "Scan QR with your phone to open current page",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                item {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.medium)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val encodedUrl = java.net.URLEncoder.encode(currentUrl, "UTF-8")
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=$encodedUrl")
                                .size(180, 180)
                                .crossfade(true)
                                .build(),
                            contentDescription = "QR Code to open current page on phone",
                            modifier = Modifier.size(94.dp)
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                item {
                    Text(
                        currentUrl,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }

            PositionIndicator(
                scalingLazyListState = qrListState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
}

@Composable
fun LoadingOverlay(progress: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        // Full screen progress indicator
        CircularProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 4.dp
        )

        Text(
            text = "$progress%",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DeferredLoadingProgress(
    isLoadingProvider: () -> Boolean,
    progressProvider: () -> Int
) {
    if (isLoadingProvider()) {
        LoadingOverlay(progress = progressProvider())
    }
}

@Composable
fun PageIndicator(
    activePage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
    onPageSelected: ((Int) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until pageCount) {
                val isActive = i == activePage
                
                // Material 3 sliding/expanding dot style with smooth spring animations
                val dotWidth by animateDpAsState(
                    targetValue = if (isActive) 12.dp else 6.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "DotWidth"
                )
                
                val dotColor by animateColorAsState(
                    targetValue = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    },
                    animationSpec = tween(durationMillis = 200),
                    label = "DotColor"
                )

                Box(
                    modifier = Modifier
                        .size(width = dotWidth, height = 6.dp)
                        .background(
                            color = dotColor,
                            shape = CircleShape
                        )
                        .then(
                            if (onPageSelected != null) {
                                Modifier.clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onPageSelected(i)
                                }
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}
