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

enum class ThreatType {
    PHISHING,
    MALWARE
}

fun checkUnsafeWebsite(url: String): ThreatType? {
    val lowercaseUrl = url.lowercase()
    
    // Check known test domains first
    val knownPhishing = listOf(
        "phishing-test.com", "malicious-site.org", "fake-bank-login.net", 
        "secure-paypal-verify.com", "verification-login-update.com", 
        "claim-free-prize.xyz", "g00g1e.com", "micros0ft.com"
    )
    if (knownPhishing.any { lowercaseUrl.contains(it) }) {
        return ThreatType.PHISHING
    }
    
    // Check suspicious keywords
    val phishingKeywords = listOf(
        "phish", "credential-update", "bank-security-alert", 
        "verify-billing-info", "free-gift-card", "secure-login-verify",
        "update-account-now", "claim-reward"
    )
    if (phishingKeywords.any { lowercaseUrl.contains(it) }) {
        return ThreatType.PHISHING
    }
    
    val malwareKeywords = listOf(
        "malware", "trojan-download", "spyware-install", "virus-download",
        "hacked-site", "malicious-script"
    )
    if (malwareKeywords.any { lowercaseUrl.contains(it) }) {
        return ThreatType.MALWARE
    }
    
    return null
}

@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val context = LocalContext.current
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
    val isPopupBlockingEnabled by viewModel.isPopupBlockingEnabled.collectAsStateWithLifecycle()
    val isPhishingProtectionEnabled by viewModel.isPhishingProtectionEnabled.collectAsStateWithLifecycle()
    val isForceHttpsEnabled by viewModel.isForceHttpsEnabled.collectAsStateWithLifecycle()
    val isBlockThirdPartyCookiesEnabled by viewModel.isBlockThirdPartyCookiesEnabled.collectAsStateWithLifecycle()
    val imageCacheSize by viewModel.imageCacheSize.collectAsStateWithLifecycle()
    val voiceCacheSize by viewModel.voiceCacheSize.collectAsStateWithLifecycle()
    val blockedAdsCount by viewModel.blockedAdsCount.collectAsStateWithLifecycle()
    val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsStateWithLifecycle()
    val ttsPitch by viewModel.ttsPitch.collectAsStateWithLifecycle()
    val isSerifFont by viewModel.isSerifFont.collectAsStateWithLifecycle()
    val lineHeightMultiplier by viewModel.lineHeightMultiplier.collectAsStateWithLifecycle()
    val isParagraphIndent by viewModel.isParagraphIndent.collectAsStateWithLifecycle()
    val isJustifyAlign by viewModel.isJustifyAlign.collectAsStateWithLifecycle()
    
    var showMenu by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showSecurityWarning by remember { mutableStateOf(false) }
    var unsafeUrlToLoad by remember { mutableStateOf("") }
    var threatTypeDetected by remember { mutableStateOf<ThreatType?>(null) }
    var bypassedHosts by remember { mutableStateOf(setOf<String>()) }
    
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableStateOf(0) }
    var pageTitle by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
 
    BackHandler(enabled = showSecurityWarning) {
        showSecurityWarning = false
        viewModel.navigateTo("pixelbrowser://home")
    }

    // Intercept swipe-to-dismiss gesture to navigate WebView history backwards gracefully, or return to home
    BackHandler(enabled = !showMenu && !showSecurityWarning && currentUrl != "pixelbrowser://home") {
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

    // Lifecycle-aware WebView optimization: pause JavaScript execution and timers when the app goes to the background
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewRef) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            try {
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                        webViewRef?.onPause()
                        webViewRef?.pauseTimers()
                    }
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                        if (currentUrl != "pixelbrowser://home") {
                            webViewRef?.onResume()
                            webViewRef?.resumeTimers()
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val screenState = when {
        showMenu -> ScreenState.MENU
        currentUrl == "pixelbrowser://home" -> ScreenState.HOME
        currentUrl == "pixelbrowser://history" -> ScreenState.HISTORY
        else -> ScreenState.BROWSER
    }

    val finalHistory = history
    val finalSearchHistory = searchHistory

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
                        onToggleSmartRamCleaner = { viewModel.toggleSmartRamCleaner() },
                        isPopupBlockingEnabled = isPopupBlockingEnabled,
                        onTogglePopupBlocking = { viewModel.togglePopupBlocking() },
                        isPhishingProtectionEnabled = isPhishingProtectionEnabled,
                        onTogglePhishingProtection = { viewModel.togglePhishingProtection() },
                        isForceHttpsEnabled = isForceHttpsEnabled,
                        onToggleForceHttps = { viewModel.toggleForceHttps() },
                        isBlockThirdPartyCookiesEnabled = isBlockThirdPartyCookiesEnabled,
                        onToggleBlockThirdPartyCookies = { viewModel.toggleBlockThirdPartyCookies() },
                        blockedCount = blockedAdsCount,
                        ttsSpeechRate = ttsSpeechRate,
                        ttsPitch = ttsPitch,
                        onSetTtsSpeechRate = { rate -> viewModel.setTtsSpeechRate(rate) },
                        onSetTtsPitch = { pitch -> viewModel.setTtsPitch(pitch) },
                        onPerformDeepClean = {
                            viewModel.performDeepMemoryClean { releasedMb ->
                                Toast.makeText(context, "深度記憶體清理已完成\n已釋放約 ${releasedMb}MB 空間", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onResetBlockedCount = { viewModel.resetBlockedAdsCount() }
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
                        isSerifFont = isSerifFont,
                        onToggleSerifFont = { viewModel.toggleSerifFont() },
                        lineHeightMultiplier = lineHeightMultiplier,
                        onSetLineHeightMultiplier = { viewModel.setLineHeightMultiplier(it) },
                        isParagraphIndent = isParagraphIndent,
                        onToggleParagraphIndent = { viewModel.toggleParagraphIndent() },
                        isJustifyAlign = isJustifyAlign,
                        onToggleJustifyAlign = { viewModel.toggleJustifyAlign() },
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
                            if (!showSecurityWarning) {
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
                                        isPopupBlockingEnabled = isPopupBlockingEnabled,
                                        isPhishingProtectionEnabled = isPhishingProtectionEnabled,
                                        isBlockThirdPartyCookiesEnabled = isBlockThirdPartyCookiesEnabled,
                                        isSerifFont = isSerifFont,
                                        lineHeightMultiplier = lineHeightMultiplier,
                                        isParagraphIndent = isParagraphIndent,
                                        isJustifyAlign = isJustifyAlign,
                                        bypassedHosts = bypassedHosts,
                                        onUnsafeSiteDetected = { url, threat ->
                                            unsafeUrlToLoad = url
                                            threatTypeDetected = threat
                                            showSecurityWarning = true
                                        },
                                        onAdBlocked = { viewModel.incrementBlockedAdsCount() },
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
                            } else {
                                val currentThreat = threatTypeDetected ?: ThreatType.PHISHING
                                val title = if (currentThreat == ThreatType.PHISHING) "偵測到疑似釣魚網站！" else "偵測到惡意網站與威脅！"
                                val desc = if (currentThreat == ThreatType.PHISHING) {
                                    "此網頁可能偽裝成其他合法網站，旨在騙取您的個人資訊或敏感密碼。"
                                } else {
                                    "此網站可能含有惡意軟體、木馬程式，造訪它可能會損害您的裝置。"
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF8B0000))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ScalingLazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "警示",
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        item {
                                            Text(
                                                text = title,
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                        item {
                                            Text(
                                                text = "網址: $unsafeUrlToLoad",
                                                color = Color.White.copy(alpha = 0.7f),
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                        item {
                                            Text(
                                                text = desc,
                                                color = Color.White.copy(alpha = 0.9f),
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                        item {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        item {
                                            Button(
                                                onClick = {
                                                    showSecurityWarning = false
                                                    viewModel.navigateTo("pixelbrowser://home")
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.White,
                                                    contentColor = Color(0xFF8B0000)
                                                ),
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                            ) {
                                                Text("返回安全主頁", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        item {
                                            TextButton(
                                                onClick = {
                                                    val host = android.net.Uri.parse(unsafeUrlToLoad).host?.lowercase() ?: ""
                                                    if (host.isNotEmpty()) {
                                                        bypassedHosts = bypassedHosts + host
                                                    }
                                                    showSecurityWarning = false
                                                    webViewRef?.loadUrl(unsafeUrlToLoad)
                                                }
                                            ) {
                                                Text("仍要造訪 (不安全)", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }

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
    isPopupBlockingEnabled: Boolean,
    isPhishingProtectionEnabled: Boolean,
    isBlockThirdPartyCookiesEnabled: Boolean,
    isSerifFont: Boolean,
    lineHeightMultiplier: Float,
    isParagraphIndent: Boolean,
    isJustifyAlign: Boolean,
    bypassedHosts: Set<String>,
    onUnsafeSiteDetected: (String, ThreatType) -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: (String?, String?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onDownloadRequested: (String, String?, String?) -> Unit,
    onAdBlocked: () -> Unit = {}
) {
    var lastLoadedUrl by remember { mutableStateOf("") }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                onWebViewCreated(this)
                
                // Allow cookies and block third-party cookies if configured
                try {
                    val cookieManager = android.webkit.CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, !isBlockThirdPartyCookiesEnabled)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                    onDownloadRequested(downloadUrl, contentDisposition, mimetype)
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onPageStarted()
                        if (url != null && isPhishingProtectionEnabled) {
                            val host = android.net.Uri.parse(url).host?.lowercase() ?: ""
                            if (!bypassedHosts.contains(host)) {
                                val threat = checkUnsafeWebsite(url)
                                if (threat != null) {
                                    view?.stopLoading()
                                    onUnsafeSiteDetected(url, threat)
                                }
                            }
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageFinished(view?.title, url)

                        // Apply Dynamic On-device Reader mode / Cloud-rendering simplification scripts
                        if (isTextOnly) {
                            val fontStyleValue = if (isSerifFont) "serif" else "sans-serif"
                            val textAlignValue = if (isJustifyAlign) "justify" else "left"
                            val pStyle = if (isParagraphIndent) "text-indent: 2em !important;" else ""
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
                                            font-family: $fontStyleValue !important; 
                                            line-height: $lineHeightMultiplier !important; 
                                            text-align: $textAlignValue !important;
                                            padding: 8px !important;
                                        }
                                        p {
                                            $pStyle
                                            margin-bottom: 12px !important;
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
                                    onAdBlocked()
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
                        val requestUrl = request?.url?.toString()
                        if (requestUrl != null && isPhishingProtectionEnabled) {
                            val host = request.url.host?.lowercase() ?: ""
                            if (!bypassedHosts.contains(host)) {
                                val threat = checkUnsafeWebsite(requestUrl)
                                if (threat != null) {
                                    onUnsafeSiteDetected(requestUrl, threat)
                                    return true // Intercept navigation
                                }
                            }
                        }
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

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            view?.loadDataWithBaseURL(null, "<html><body style='background-color:black;color:white;text-align:center;padding-top:20px;'><p>網頁載入失敗</p><p>請檢查網路連線</p></body></html>", "text/html", "UTF-8", null)
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: android.webkit.WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: android.webkit.RenderProcessGoneDetail?
                    ): Boolean {
                        // Return true to indicate that the crash was handled, preventing the app from crashing entirely
                        return true
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }

                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean {
                        if (isPopupBlockingEnabled) {
                            Toast.makeText(view?.context, "已自動阻擋彈出視窗", Toast.LENGTH_SHORT).show()
                            return false
                        }
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        if (transport != null) {
                            transport.webView = view
                            resultMsg.sendToTarget()
                            return true
                        }
                        return false
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
                    
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                    
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
            
            try {
                val cookieManager = android.webkit.CookieManager.getInstance()
                cookieManager.setAcceptThirdPartyCookies(webView, !isBlockThirdPartyCookiesEnabled)
            } catch (e: Exception) {
                e.printStackTrace()
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
        onRelease = { webView ->
            webView.stopLoading()
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
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
        val context = LocalContext.current
        val imageRequest = remember(url, context) {
            ImageRequest.Builder(context)
                .data("https://www.google.com/s2/favicons?sz=64&domain_url=$url")
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = imageRequest,
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
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "Scale"
    )
    val hapticEffect = LocalHapticFeedback.current
    val scaleModifier = if (interactionSource != null) {
        Modifier.scale(scale)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(scaleModifier)
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
    isPopupBlockingEnabled: Boolean,
    onTogglePopupBlocking: () -> Unit,
    isPhishingProtectionEnabled: Boolean,
    onTogglePhishingProtection: () -> Unit,
    isForceHttpsEnabled: Boolean,
    onToggleForceHttps: () -> Unit,
    isBlockThirdPartyCookiesEnabled: Boolean,
    onToggleBlockThirdPartyCookies: () -> Unit,
    blockedCount: Int,
    ttsSpeechRate: Float,
    ttsPitch: Float,
    onSetTtsSpeechRate: (Float) -> Unit,
    onSetTtsPitch: (Float) -> Unit,
    onPerformDeepClean: () -> Unit,
    onResetBlockedCount: () -> Unit,
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
    var showSecurityDiagDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showTranslateMenu by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            var spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (spokenText == null) {
                val results = android.app.RemoteInput.getResultsFromIntent(result.data)
                spokenText = results?.getCharSequence("input_result")?.toString()
            }
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
        try {
            val intent = com.example.wear.WearOsCompatLayer.getSafeVoiceSearchIntent()
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                        listState.dispatchRawDelta(it.verticalScrollPixels)
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
                    SwitchButton(
                        checked = isDeepMode,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleDeepMode() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness2,
                                contentDescription = null,
                                tint = if (isDeepMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OLED 純黑模式", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isDeepMode) "極致省電純黑背景" else "標準深色主題", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                    SwitchButton(
                        checked = isCloudRendering,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCloudRendering() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = if (isCloudRendering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("雲端簡化渲染", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isCloudRendering) "已啟用：剔除動畫/降載CPU" else "標準渲染模式", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Text Only / Text Folding
                item {
                    SwitchButton(
                        checked = isTextOnly,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleTextOnly() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Article,
                                contentDescription = null,
                                tint = if (isTextOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("純文字與折疊", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isTextOnly) "已啟用：阻擋圖片/標題折疊" else "顯示完整媒體", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Aggressive Caching
                item {
                    SwitchButton(
                        checked = isAggressiveCaching,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleAggressiveCaching() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cached,
                                contentDescription = null,
                                tint = if (isAggressiveCaching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("積極快取載入", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isAggressiveCaching) "優先載入快取，省流極速" else "每次皆重新下載", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Smart Ad-Blocker
                item {
                    SwitchButton(
                        checked = isAdBlockEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleAdBlock() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("智慧廣告阻擋", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isAdBlockEnabled) "已啟用：自動剔除廣告/追蹤" else "未啟用智慧阻擋", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // CPU Throttle
                item {
                    SwitchButton(
                        checked = isCpuThrottleEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCpuThrottle() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isCpuThrottleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("智慧 CPU 負載抑制", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isCpuThrottleEnabled) "已啟用：延緩背景計時器/省電" else "標準背景執行", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Smart RAM Cleaner
                item {
                    SwitchButton(
                        checked = isSmartRamCleanerEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSmartRamCleaner() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = if (isSmartRamCleanerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("智慧記憶體清理", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isSmartRamCleanerEnabled) "已啟用：返回主頁即釋放暫存" else "未啟用記憶體自動清理", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 深度清理記憶體按鈕 (Deep Memory Clean)
                item {
                    val cleanInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPerformDeepClean()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(cleanInt),
                        interactionSource = cleanInt,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = "清理",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "深度清理系統記憶體",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Popup Blocker
                item {
                    SwitchButton(
                        checked = isPopupBlockingEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePopupBlocking() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = if (isPopupBlockingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("阻擋彈出式視窗", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isPopupBlockingEnabled) "已啟用：自動攔截彈出廣告" else "未啟用彈出式視窗阻擋", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Phishing & Malware Protection
                item {
                    SwitchButton(
                        checked = isPhishingProtectionEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePhishingProtection() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isPhishingProtectionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("防範惡意與釣魚網站", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isPhishingProtectionEnabled) "已啟用：即時威脅偵測與警告" else "未啟用安全性防護", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

                // TTS Speech Rate & Pitch Controls
                item {
                    Card(
                        onClick = {},
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TTS 朗讀設定", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            // Speed Rate Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("速度: ${String.format("%.2fx", ttsSpeechRate)}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0.8f, 1.0f, 1.25f, 1.5f).forEach { rate ->
                                        val isSelected = Math.abs(ttsSpeechRate - rate) < 0.05f
                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSetTtsSpeechRate(rate)
                                            },
                                            modifier = Modifier.size(width = 30.dp, height = 24.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                                            ),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = if (rate == 1.0f) "1x" else String.format("%.1f", rate),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Pitch Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("音調: ${String.format("%.1fx", ttsPitch)}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0.8f, 1.0f, 1.2f).forEach { pitch ->
                                        val isSelected = Math.abs(ttsPitch - pitch) < 0.05f
                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSetTtsPitch(pitch)
                                            },
                                            modifier = Modifier.size(width = 32.dp, height = 24.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                                            ),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = if (pitch == 1.0f) "1x" else String.format("%.1f", pitch),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 安全診斷卡片
                item {
                    val securityInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSecurityDiagDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(securityInt),
                        interactionSource = securityInt,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("安全性診斷與統計", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("已阻擋廣告: $blockedCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "開啟診斷",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
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

            SecurityDialog(
                show = showSecurityDiagDialog,
                onDismiss = { showSecurityDiagDialog = false },
                currentUrl = currentUrl,
                blockedCount = blockedCount,
                isAdBlockEnabled = isAdBlockEnabled,
                onToggleAdBlock = onToggleAdBlock,
                isPopupBlockingEnabled = isPopupBlockingEnabled,
                onTogglePopupBlocking = onTogglePopupBlocking,
                isPhishingProtectionEnabled = isPhishingProtectionEnabled,
                onTogglePhishingProtection = onTogglePhishingProtection,
                isForceHttpsEnabled = isForceHttpsEnabled,
                onToggleForceHttps = onToggleForceHttps,
                isBlockThirdPartyCookiesEnabled = isBlockThirdPartyCookiesEnabled,
                onToggleBlockThirdPartyCookies = onToggleBlockThirdPartyCookies,
                isCpuThrottleEnabled = isCpuThrottleEnabled,
                onToggleCpuThrottle = onToggleCpuThrottle,
                onResetBlockedCount = onResetBlockedCount
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
    isSerifFont: Boolean,
    onToggleSerifFont: () -> Unit,
    lineHeightMultiplier: Float,
    onSetLineHeightMultiplier: (Float) -> Unit,
    isParagraphIndent: Boolean,
    onToggleParagraphIndent: () -> Unit,
    isJustifyAlign: Boolean,
    onToggleJustifyAlign: () -> Unit,
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
            var spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (spokenText == null) {
                val results = android.app.RemoteInput.getResultsFromIntent(result.data)
                spokenText = results?.getCharSequence("input_result")?.toString()
            }
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
        try {
            val intent = com.example.wear.WearOsCompatLayer.getSafeVoiceSearchIntent()
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                        listState.dispatchRawDelta(it.verticalScrollPixels)
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
                    
                    val searchBarBgColor = MaterialTheme.colorScheme.primaryContainer
                    val searchBarContentColor = MaterialTheme.colorScheme.onPrimaryContainer

                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSearchMethodDialog = true
                        },
                        colors = CardDefaults.cardColors(containerColor = searchBarBgColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
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

                if (localBookmarks.isNotEmpty()) {
                    item {
                        ListHeader {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("書籤", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "新增",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showAddLocalBookmarkDialog = true
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            
                            Text(
                                text = "收藏你的最愛",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "將常造訪的網頁儲存至此，即可隨時一鍵快速開啟！",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showAddLocalBookmarkDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("新增第一個書籤", style = MaterialTheme.typography.labelMedium)
                                }
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
        isSerifFont = isSerifFont,
        onToggleSerifFont = onToggleSerifFont,
        lineHeightMultiplier = lineHeightMultiplier,
        onSetLineHeightMultiplier = onSetLineHeightMultiplier,
        isParagraphIndent = isParagraphIndent,
        onToggleParagraphIndent = onToggleParagraphIndent,
        isJustifyAlign = isJustifyAlign,
        onToggleJustifyAlign = onToggleJustifyAlign,
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
                    listState.dispatchRawDelta(it.verticalScrollPixels)
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
                val context = LocalContext.current
                val imageRequest = remember(url, context) {
                    ImageRequest.Builder(context)
                        .data("https://www.google.com/s2/favicons?sz=64&domain_url=$url")
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
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

        // Text Content directly in the Row with wide spacing
        val isSearch = url == "搜尋" || url == "搜尋詞"
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.Center
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
                            searchListState.dispatchRawDelta(it.verticalScrollPixels)
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
                    IconButton(
                        onClick = {
                            onVoiceSearch()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "語音搜尋",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 鍵盤搜尋按鈕
                    IconButton(
                        onClick = {
                            onKeyboardSearch()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "鍵盤搜尋",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
    isSerifFont: Boolean,
    onToggleSerifFont: () -> Unit,
    lineHeightMultiplier: Float,
    onSetLineHeightMultiplier: (Float) -> Unit,
    isParagraphIndent: Boolean,
    onToggleParagraphIndent: () -> Unit,
    isJustifyAlign: Boolean,
    onToggleJustifyAlign: () -> Unit,
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
    var showAboutDialog by remember { mutableStateOf(false) }

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
                        settingsListState.dispatchRawDelta(it.verticalScrollPixels)
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
                    SwitchButton(
                        checked = isDeepMode,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleDeepMode() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness2,
                                contentDescription = null,
                                tint = if (isDeepMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OLED 純黑省電", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isDeepMode) "極致省電純黑背景" else "標準深色主題", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                    SwitchButton(
                        checked = isCircularSafeMode,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCircularSafeMode() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = null,
                                tint = if (isCircularSafeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("防遮擋安全視區", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isCircularSafeMode) "完整顯示不裁剪" else "網頁可能被裁剪", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                    SwitchButton(
                        checked = isAdBlockEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleAdBlock() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("智慧廣告阻擋", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isAdBlockEnabled) "自動剔除廣告與追蹤" else "未啟用廣告阻擋", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 純文字與折疊
                item {
                    SwitchButton(
                        checked = isTextOnly,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleTextOnly() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Article,
                                contentDescription = null,
                                tint = if (isTextOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("純文字與折疊", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isTextOnly) "阻擋圖片與標題折疊" else "顯示完整網頁媒體", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // --- 進階文字排版 ---
                item {
                    ListHeader {
                        Text("進階文字排版", style = MaterialTheme.typography.titleSmall)
                    }
                }

                // 1. 字體樣式 (Font Style)
                item {
                    SwitchButton(
                        checked = isSerifFont,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSerifFont() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FontDownload,
                                contentDescription = null,
                                tint = if (isSerifFont) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("閱讀器字體樣式", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isSerifFont) "襯線明體 (適合長文)" else "無襯線黑體 (預設)", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. 閱讀行高比例 (Line Height Ratio Selector)
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text("段落行高比例", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val lineHeights = listOf(1.3f, 1.6f, 2.0f, 2.4f)
                            lineHeights.forEach { height ->
                                val isSelected = lineHeightMultiplier == height
                                Button(
                                    onClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSetLineHeightMultiplier(height) 
                                    },
                                    modifier = Modifier.height(28.dp).weight(1f).padding(horizontal = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        "${height}x", 
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 首行縮排 (Paragraph Indent)
                item {
                    SwitchButton(
                        checked = isParagraphIndent,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleParagraphIndent() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatIndentIncrease,
                                contentDescription = null,
                                tint = if (isParagraphIndent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("中文段落首行縮排", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isParagraphIndent) "開啟：段首自動縮排" else "關閉：齊頭段落", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 4. 兩端對齊 (Justified Alignment)
                item {
                    SwitchButton(
                        checked = isJustifyAlign,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleJustifyAlign() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatAlignJustify,
                                contentDescription = null,
                                tint = if (isJustifyAlign) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("圓形螢幕兩端對齊", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isJustifyAlign) "文字均勻貼合螢幕邊緣" else "標準靠左對齊", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

                // --- 關於瀏覽器 ---
                item {
                    ListHeader {
                        Text("關於", style = MaterialTheme.typography.titleSmall)
                    }
                }

                item {
                    val aboutInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAboutDialog = true
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .expressiveScale(aboutInt),
                        interactionSource = aboutInt
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
                                    "關於本瀏覽器",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    "版本 1.2.0 (安全加強版)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "關於",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
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
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}
}

@OptIn(androidx.wear.compose.material3.ExperimentalWearMaterial3Api::class)
@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    ExpressiveDialog(onDismissRequest = onDismiss) {
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
                        imageVector = Icons.Default.Check,
                        contentDescription = "確認",
                        modifier = Modifier.size(24.dp)
                    )
                    Text("確定")
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
                    contentPadding = PaddingValues(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 56.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onRotaryScrollEvent {
                            listState.dispatchRawDelta(it.verticalScrollPixels)
                            true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        ListHeader {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("關於本瀏覽器", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }

                    // 應用程式基本資訊卡片
                    item {
                        Card(
                            onClick = {},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "瀏覽器圖標",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Pixel 智慧安全瀏覽器",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "版本 1.2.0 (安全加強版)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 核心安全功能標題
                    item {
                        ListHeader {
                            Text("五重核心安全防護", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // 安全防護機制
                    item {
                        Card(
                            onClick = {},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("即時惡意威脅阻擋", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Text("阻擋釣魚、惡意與欺詐網址。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // 智慧廣告阻擋
                    item {
                        Card(
                            onClick = {},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("智慧廣告與追蹤阻擋", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Text("剔除垃圾橫幅廣告與背景追蹤。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // 強制 HTTPS
                    item {
                        Card(
                            onClick = {},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("強制 HTTPS 安全加密", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Text("所有 http 連線皆自動升級加密。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // 阻擋第三方 Cookie
                    item {
                        Card(
                            onClick = {},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("阻擋第三方 Cookie", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Text("防止跨站追蹤與隱私外洩。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // 圓形安全視區與清理
                    item {
                        Card(
                            onClick = {},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF9C27B0),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("極致效能與專利視區", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Text("圓形安全視區與智慧快取清理引擎。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // 感謝資訊
                    item {
                        Text(
                            text = "專為 Wear OS 與圓形智慧手錶所設計，安全、高效、省電。精雕細琢，只為提供給您最安心的瀏覽體驗。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    item {
                        Text(
                            text = "© 2026 AI Studio Team",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
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
                Text("新增書籤", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
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
                            bookmarkListState.dispatchRawDelta(it.verticalScrollPixels)
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "尚無書籤",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
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
                            listState.dispatchRawDelta(it.verticalScrollPixels)
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
                            qrListState.dispatchRawDelta(it.verticalScrollPixels)
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
                        val context = LocalContext.current
                        val encodedUrl = remember(currentUrl) { java.net.URLEncoder.encode(currentUrl, "UTF-8") }
                        val qrRequest = remember(encodedUrl, context) {
                            ImageRequest.Builder(context)
                                .data("https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=$encodedUrl")
                                .size(180, 180)
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = qrRequest,
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

@OptIn(androidx.wear.compose.material3.ExperimentalWearMaterial3Api::class)
@Composable
fun SecurityDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    currentUrl: String,
    blockedCount: Int,
    isAdBlockEnabled: Boolean,
    onToggleAdBlock: () -> Unit,
    isPopupBlockingEnabled: Boolean,
    onTogglePopupBlocking: () -> Unit,
    isPhishingProtectionEnabled: Boolean,
    onTogglePhishingProtection: () -> Unit,
    isForceHttpsEnabled: Boolean,
    onToggleForceHttps: () -> Unit,
    isBlockThirdPartyCookiesEnabled: Boolean,
    onToggleBlockThirdPartyCookies: () -> Unit,
    isCpuThrottleEnabled: Boolean,
    onToggleCpuThrottle: () -> Unit,
    onResetBlockedCount: () -> Unit
) {
    if (!show) return

    val haptic = LocalHapticFeedback.current
    val isSecure = currentUrl.startsWith("https://")
    val isLocal = currentUrl.startsWith("file://") || currentUrl.startsWith("pixelbrowser://")
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var score = 0
    if (isAdBlockEnabled) score++
    if (isPopupBlockingEnabled) score++
    if (isPhishingProtectionEnabled) score++
    if (isForceHttpsEnabled) score++
    if (isBlockThirdPartyCookiesEnabled) score++

    val securityLevelText = when (score) {
        5 -> "極高 (最安全)"
        4 -> "高 (防護良好)"
        3 -> "中 (基礎防護)"
        else -> "低 (建議開啟)"
    }
    val securityLevelColor = when (score) {
        5 -> Color(0xFF4CAF50)
        4 -> Color(0xFF8BC34A)
        3 -> Color(0xFFFFC107)
        else -> Color(0xFFFF5722)
    }
    val securityLevelDesc = when (score) {
        5 -> "防護已達最高安全標準！"
        4 -> "上網隱私已獲優質保護。"
        3 -> "建議開啟更多防禦機制。"
        else -> "安全防護較低，請加強。"
    }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    ExpressiveDialog(onDismissRequest = onDismiss) {
        ScreenScaffold(
            scrollState = listState,
            bottomButton = {
                EdgeButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "完成")
                    Text("完成")
                }
            }
        ) {
            ScalingLazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 16.dp, start = 8.dp, end = 8.dp, bottom = 56.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onRotaryScrollEvent {
                        listState.dispatchRawDelta(it.verticalScrollPixels)
                        true
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    ListHeader {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("安全與隱私診斷", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // 安全防護等級評分卡片
                item {
                    Card(
                        onClick = {},
                        colors = CardDefaults.cardColors(
                            containerColor = securityLevelColor.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = securityLevelColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "防護等級：$securityLevelText",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = securityLevelColor,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = securityLevelDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Card(
                        onClick = {},
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSecure) Color(0xFF1B5E20).copy(alpha = 0.2f) else if (isLocal) MaterialTheme.colorScheme.surfaceContainer else Color(0xFFB71C1C).copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isSecure) Icons.Default.Lock else if (isLocal) Icons.Default.Info else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isSecure) Color(0xFF4CAF50) else if (isLocal) MaterialTheme.colorScheme.onSurface else Color(0xFFF44336),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSecure) "加密安全連線 (HTTPS)" else if (isLocal) "系統內部網頁" else "未加密連線 (HTTP)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSecure) Color(0xFF4CAF50) else if (isLocal) MaterialTheme.colorScheme.onSurface else Color(0xFFF44336),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Card(
                        onClick = {
                            if (blockedCount > 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onResetBlockedCount()
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("已阻擋廣告與追蹤器", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(
                                text = "$blockedCount",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            if (blockedCount > 0) {
                                Text("點擊重設為 0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                item {
                    ListHeader {
                        Text("安全引擎設定", style = MaterialTheme.typography.titleSmall)
                    }
                }

                // AdBlock
                item {
                    SwitchButton(
                        checked = isAdBlockEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleAdBlock() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("智慧廣告阻擋", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isAdBlockEnabled) "已阻擋網頁多餘廣告" else "未啟用廣告阻擋", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Popup blocker
                item {
                    SwitchButton(
                        checked = isPopupBlockingEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePopupBlocking() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = if (isPopupBlockingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("彈出視窗阻擋", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isPopupBlockingEnabled) "防止惡意彈出式分頁" else "未阻擋彈出視窗", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Phishing protection
                item {
                    SwitchButton(
                        checked = isPhishingProtectionEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePhishingProtection() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isPhishingProtectionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("惡意與防釣魚警告", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isPhishingProtectionEnabled) "安全偵測可疑惡意網站" else "未啟用防釣魚警告", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Force HTTPS
                item {
                    SwitchButton(
                        checked = isForceHttpsEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleForceHttps() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isForceHttpsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("強制 HTTPS 安全連線", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isForceHttpsEnabled) "自動升級為加密網頁" else "未加密：安全防護較低", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Block Third Party Cookies
                item {
                    SwitchButton(
                        checked = isBlockThirdPartyCookiesEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleBlockThirdPartyCookies() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isBlockThirdPartyCookiesEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("阻擋第三方 Cookie 追蹤", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isBlockThirdPartyCookiesEnabled) "防止跨站定位與廣告" else "未啟用：允許寫入追蹤", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // CPU Timer Throttle
                item {
                    SwitchButton(
                        checked = isCpuThrottleEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCpuThrottle() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isCpuThrottleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("背景效能優化", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isCpuThrottleEnabled) "限制閒置分頁以節省電量" else "未啟用效能優化", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
