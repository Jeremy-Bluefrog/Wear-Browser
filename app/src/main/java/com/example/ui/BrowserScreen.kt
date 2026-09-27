package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.OutlinedTextField
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.CardDefaults
import android.graphics.Bitmap
import android.os.Build
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.wear.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.PositionIndicator
import androidx.compose.ui.draw.clip
import com.example.data.*
import com.example.wear.WearGestureDetector
import coil.compose.AsyncImage
import coil.request.CachePolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.net.URLEncoder

enum class ScreenState {
    MENU, HOME, BROWSER, HISTORY, OFFLINE_PAGES
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
    val offlinePages by viewModel.offlinePages.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val localBookmarks by viewModel.localBookmarks.collectAsStateWithLifecycle()
    val downloadedFiles by viewModel.downloadedFiles.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
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
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf("") }

    val voiceCacheSize by viewModel.voiceCacheSize.collectAsStateWithLifecycle()
    val blockedAdsCount by viewModel.blockedAdsCount.collectAsStateWithLifecycle()
    val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsStateWithLifecycle()
    val ttsPitch by viewModel.ttsPitch.collectAsStateWithLifecycle()
    val isSerifFont by viewModel.isSerifFont.collectAsStateWithLifecycle()
    val lineHeightMultiplier by viewModel.lineHeightMultiplier.collectAsStateWithLifecycle()
    val isParagraphIndent by viewModel.isParagraphIndent.collectAsStateWithLifecycle()
    val isJustifyAlign by viewModel.isJustifyAlign.collectAsStateWithLifecycle()

    val isOneHandedGesturesEnabled by viewModel.isOneHandedGesturesEnabled.collectAsStateWithLifecycle()
    val gestureSensitivity by viewModel.gestureSensitivity.collectAsStateWithLifecycle()
    val gestureHudMessage by viewModel.gestureHudMessage.collectAsStateWithLifecycle()
    
    var showMenu by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showHomeSettingsDialog by remember { mutableStateOf(false) }
    var showHomeDownloadsDialog by remember { mutableStateOf(false) }
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

    val gestureVoiceLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            var spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (spokenText == null) {
                val results = android.app.RemoteInput.getResultsFromIntent(result.data)
                spokenText = results?.getCharSequence("input_result")?.toString()
            }
            if (!spokenText.isNullOrBlank()) {
                val isWeb = spokenText.startsWith("http://") || spokenText.startsWith("https://") || (spokenText.contains(".") && !spokenText.contains(" "))
                viewModel.navigateTo(if (isWeb) {
                    if (spokenText.startsWith("http://") || spokenText.startsWith("https://")) spokenText else "https://$spokenText"
                } else {
                    val encoded = java.net.URLEncoder.encode(spokenText, "UTF-8")
                    "https://www.google.com/search?q=$encoded"
                })
            }
        }
    }

    val gestureDetector = remember(context) {
        WearGestureDetector(context, object : WearGestureDetector.GestureListener {
            override fun onPrimaryAction() {
                if (currentUrl == "pixelbrowser://home") {
                    viewModel.showGestureHud("🖐️ 雙指捏合：語音搜尋")
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    try {
                        val intent = com.example.wear.WearOsCompatLayer.getSafeVoiceSearchIntent()
                        gestureVoiceLauncher.launch(intent)
                    } catch (e: Exception) {
                        showSearchDialog = true
                    }
                } else {
                    viewModel.showGestureHud("🖐️ 雙指捏合：網頁朗讀")
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSpeaking) {
                        viewModel.stopSpeaking()
                    } else {
                        webViewRef?.evaluateJavascript(
                            "(function() { return document.body.innerText; })();"
                        ) { text ->
                            val cleanText = text?.removeSurrounding("\"")
                                ?.replace("\\n", "\n")
                                ?.replace("\\\"", "\"")
                                ?.replace("\\\\", "\\")
                            if (!cleanText.isNullOrBlank()) {
                                viewModel.speakText(cleanText)
                            }
                        }
                    }
                }
            }

            override fun onDismissAction() {
                if (showSearchDialog || showMenu || showSecurityWarning || showQrDialog || showDownloadDialog) {
                    showSearchDialog = false
                    showMenu = false
                    showSecurityWarning = false
                    showQrDialog = false
                    showDownloadDialog = false
                    viewModel.showGestureHud("↩️ 翻轉手腕：關閉面板")
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else if (currentUrl != "pixelbrowser://home") {
                    if (canGoBack) {
                        webViewRef?.goBack()
                    } else {
                        viewModel.navigateTo("pixelbrowser://home")
                    }
                    viewModel.showGestureHud("↩️ 翻轉手腕：返回")
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else {
                    viewModel.showGestureHud("↩️ 翻轉手腕：已在首頁")
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        })
    }

    LaunchedEffect(isOneHandedGesturesEnabled, gestureSensitivity) {
        gestureDetector.isEnabled = isOneHandedGesturesEnabled
        gestureDetector.sensitivity = when (gestureSensitivity) {
            "高靈敏度" -> WearGestureDetector.Sensitivity.HIGH
            "低靈敏度" -> WearGestureDetector.Sensitivity.LOW
            else -> WearGestureDetector.Sensitivity.STANDARD
        }
        if (isOneHandedGesturesEnabled) {
            gestureDetector.start()
        } else {
            gestureDetector.stop()
        }
    }
 
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
    LaunchedEffect(currentUrl, webViewRef) {
        if (currentUrl == "pixelbrowser://home") {
            try {
                webViewRef?.onPause()
                webViewRef?.pauseTimers()
                webViewRef?.visibility = android.view.View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                webViewRef?.visibility = android.view.View.VISIBLE
                webViewRef?.onResume()
                webViewRef?.resumeTimers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Lifecycle-aware WebView and Gesture detector optimization
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewRef, gestureDetector, isOneHandedGesturesEnabled) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            try {
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                        webViewRef?.onPause()
                        webViewRef?.pauseTimers()
                        gestureDetector.stop()
                    }
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                        if (currentUrl != "pixelbrowser://home") {
                            webViewRef?.onResume()
                            webViewRef?.resumeTimers()
                        }
                        if (isOneHandedGesturesEnabled) {
                            gestureDetector.start()
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
            gestureDetector.stop()
        }
    }

    val screenState = when {
        showMenu -> ScreenState.MENU
        currentUrl == "pixelbrowser://home" -> ScreenState.HOME
        currentUrl == "pixelbrowser://history" -> ScreenState.HISTORY
        currentUrl == "pixelbrowser://offline" -> ScreenState.OFFLINE_PAGES
        else -> ScreenState.BROWSER
    }

    val finalHistory = history
    val finalSearchHistory = searchHistory

    AppScaffold {
        ConstraintLayout(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            val (contentRef, bottomTriggerRef, bottomIndicatorRef, pageIndicatorRef, gestureHudRef) = createRefs()
            
            Box(
                modifier = Modifier.fillMaxSize().constrainAs(contentRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            ) {
                // ALWAYS render the browser in the background to preserve state and avoid lag
                Box(
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer { alpha = if (screenState == ScreenState.BROWSER) 1f else 0f }
                ) {
                    val context = LocalContext.current
                    
                    val leftDragOffset = 0f
                    val rightDragOffset = 0f
                    val dragThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 50.dp.toPx() }

                        val isScreenRound = androidx.compose.ui.platform.LocalConfiguration.current.isScreenRound
                        val targetTopPadding = if (isScreenRound && isCircularSafeMode) 22.dp else 0.dp
                        val targetBottomPadding = if (isScreenRound && isCircularSafeMode) 40.dp else 0.dp
                        val targetSidePadding = if (isScreenRound && isCircularSafeMode) 20.dp else 0.dp

                        val safeTopPadding by animateDpAsState(
                            targetValue = targetTopPadding,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "safeTopPadding"
                        )
                        val safeBottomPadding by animateDpAsState(
                            targetValue = targetBottomPadding,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "safeBottomPadding"
                        )
                        val safeSidePadding by animateDpAsState(
                            targetValue = targetSidePadding,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "safeSidePadding"
                        )

                        var rotaryAccumulator by remember { mutableStateOf(0f) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isDeepMode) Color.Black else MaterialTheme.colorScheme.background)
                                .focusRequester(webViewFocusRequester)
                                .focusable()
                                .onRotaryScrollEvent {
                                    val delta = it.verticalScrollPixels
                                    webViewRef?.scrollBy(0, (delta * 1.5f).toInt())
                                    rotaryAccumulator += kotlin.math.abs(delta)
                                    if (rotaryAccumulator > 30f) {
                                        rotaryAccumulator = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
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
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(if (isDeepMode) Color.Black else Color.White)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isDeepMode) Color.DarkGray.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                            } else {
                                                Modifier.background(if (isDeepMode) Color.Black else Color.White)
                                            }
                                        )
                                ) {
                                    var webViewFailed by remember { mutableStateOf(false) }
                                    if (!webViewFailed) {
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
                                        onWebViewCreated = { webView ->
                                            webViewRef = webView
                                            if (isCpuThrottleEnabled) {
                                                webView.settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.LOW)
                                            }
                                            canGoBack = webView.canGoBack()
                                            canGoForward = webView.canGoForward()
                                        },
                                        onDownloadRequested = { url, contentDisposition, mimetype ->
                                            showDownloadDialog = true
                                            downloadUrl = url
                                        },
                                        onWebViewFailed = { webViewFailed = true }
                                    )
                                    } else {
                                        NativeTextReader(
                                            url = currentUrl,
                                            isDeepMode = isDeepMode,
                                            isSerifFont = isSerifFont
                                        )
                                    }
                                    
                                    if (isLoading && loadProgress < 100) {
                                        if (!isScreenRound) {
                                            androidx.compose.material3.LinearProgressIndicator(
                                                progress = { loadProgress / 100f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .align(Alignment.TopCenter),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }

                                // 圓形螢幕頂部專屬弧形載入進度條
                                if (isScreenRound && isLoading && loadProgress < 100) {
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    val trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    val progressFraction = (loadProgress / 100f).coerceIn(0f, 1f)

                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(3.dp)
                                    ) {
                                        val strokeWidth = 4.dp.toPx()
                                        val startAngle = 220f
                                        val sweepTotal = 100f
                                        // 繪製背景弧線
                                        drawArc(
                                            color = trackColor,
                                            startAngle = startAngle,
                                            sweepAngle = sweepTotal,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = strokeWidth,
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        )
                                        // 繪製前進進度弧線
                                        drawArc(
                                            color = primaryColor,
                                            startAngle = startAngle,
                                            sweepAngle = sweepTotal * progressFraction,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = strokeWidth,
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        )
                                    }
                                }

                                // 圓形螢幕底部安全懸浮迷你導覽按鈕列 (避開圓弧底緣)
                                if (screenState == ScreenState.BROWSER && currentUrl != "pixelbrowser://home") {
                                    val bottomPillOffset = if (isScreenRound) 6.dp else 4.dp
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = bottomPillOffset)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 返回/首頁按鈕
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (canGoBack) {
                                                    webViewRef?.goBack()
                                                } else {
                                                    viewModel.navigateTo("pixelbrowser://home")
                                                }
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Home,
                                                contentDescription = if (canGoBack) "上一頁" else "主頁",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // 重新整理按鈕
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                webViewRef?.reload()
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "重新整理",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // 圓形安全視區快速切換
                                        if (isScreenRound) {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.toggleCircularSafeMode()
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isCircularSafeMode) Icons.Default.FitScreen else Icons.Default.Fullscreen,
                                                    contentDescription = "圓形安全視區開關",
                                                    tint = if (isCircularSafeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // 選單按鈕
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showMenu = true
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "選單",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0xFF8B0000).copy(alpha = 0.85f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val listState = rememberScalingLazyListState()
                                    ScalingLazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                                        }
                                        item {
                                            val title = when (threatTypeDetected) {
                                                ThreatType.PHISHING -> "可疑的釣魚網站"
                                                ThreatType.MALWARE -> "惡意軟體警告"
                                                else -> "不安全的網站"
                                            }
                                            val desc = when (threatTypeDetected) {
                                                ThreatType.PHISHING -> "此網站可能會誘騙您透露密碼或信用卡等個人資訊。"
                                                ThreatType.MALWARE -> "此網站可能會嘗試在您的裝置上安裝危險軟體。"
                                                else -> "瀏覽此網站可能存在安全風險。"
                                            }
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
                                                text = "此網站可能會誘騙您透露密碼或信用卡等個人資訊。",
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
                        }
                }
                
                // Overlay other screens
                if (screenState != ScreenState.BROWSER) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        when (screenState) {
                            ScreenState.MENU -> {
                    BookmarkMenu(
                        bookmarks = bookmarks,
                        offlinePages = offlinePages,
                        downloadedFiles = downloadedFiles,
                        history = finalHistory,
                        searchHistory = finalSearchHistory,
                        onAddSearchHistory = { query -> viewModel.addSearchHistory(query) },
                        onDeleteSearchHistory = { id -> viewModel.removeSearchHistory(id) },
                        onClearSearchHistory = { viewModel.clearSearchHistory() },
                        currentUrl = currentUrl,
                        pageTitle = pageTitle,
                        onAddBookmark = { url, title -> viewModel.addBookmark(url, title) },
                        onSaveOfflinePage = {
                            viewModel.saveCurrentWebPage(
                                url = currentUrl,
                                title = pageTitle,
                                webView = webViewRef
                            )
                        },
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
                        offlinePages = offlinePages,
                        isOnline = isOnline,
                        onAddLocalBookmark = remember(viewModel) { { url, title -> viewModel.addLocalBookmark(url, title) } },
                        onDeleteLocalBookmark = remember(viewModel) { { url -> viewModel.removeLocalBookmark(url) } },
                        searchHistory = finalSearchHistory,
                        onAddSearchHistory = remember(viewModel) { { query -> viewModel.addSearchHistory(query) } },
                        onDeleteSearchHistory = remember(viewModel) { { id -> viewModel.removeSearchHistory(id) } },
                        onClearSearchHistory = remember(viewModel) { { viewModel.clearSearchHistory() } },
                        onDeleteBookmark = remember(viewModel) { { url -> viewModel.removeBookmark(url) } },
                        onUpdateBookmark = remember(viewModel) { { viewModel.updateBookmark(it) } },
                        onNavigate = remember(viewModel) { { url -> viewModel.navigateTo(url) } },
                        onOpenSettings = remember { { showHomeSettingsDialog = true } },
                        onOpenDownloads = remember { { showHomeDownloadsDialog = true } }
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

                ScreenState.OFFLINE_PAGES -> {
                    OfflinePagesScreen(
                        offlinePages = offlinePages,
                        isOnline = isOnline,
                        onOpenPage = { page ->
                            viewModel.navigateTo("file://${page.localPath}")
                        },
                        onOpenInReader = { page ->
                            viewModel.navigateTo("file://${page.localPath}")
                        },
                        onDeletePage = { page ->
                            viewModel.deleteOfflinePage(page.id, page.localPath)
                        },
                        onClearAll = {
                            viewModel.clearAllOfflinePages()
                        },
                        onBack = {
                            viewModel.navigateTo("pixelbrowser://home")
                        }
                    )
                }
                else -> {}
            }
        }
    }
} // close Box for contentRef
            
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

            if (screenState == ScreenState.HISTORY) {
                val activePage = 1
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

            // Wear OS One-Handed Gesture HUD Notification overlay
            AnimatedVisibility(
                visible = gestureHudMessage != null,
                enter = fadeIn(animationSpec = tween(150)) + slideInVertically(animationSpec = tween(150)) { -it },
                exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200)) { -it },
                modifier = Modifier
                    .constrainAs(gestureHudRef) {
                        top.linkTo(parent.top, margin = 12.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gestureHudMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
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

        if (showHomeSettingsDialog) {
            SettingsDialog(
                show = true,
                onDismiss = { showHomeSettingsDialog = false },
                textZoom = textZoom,
                onSetTextZoom = { viewModel.setTextZoom(it) },
                isDeepMode = isDeepMode,
                onToggleDeepMode = { viewModel.toggleDeepMode() },
                isAdBlockEnabled = isAdBlockEnabled,
                onToggleAdBlock = { viewModel.toggleAdBlock() },
                isTextOnly = isTextOnly,
                onToggleTextOnly = { viewModel.toggleTextOnly() },
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
                onClearHistory = { viewModel.clearHistory() },
                imageCacheSize = imageCacheSize,
                voiceCacheSize = voiceCacheSize,
                onClearCache = { viewModel.clearVoiceAndImageCache() },
                onRefreshCache = { viewModel.refreshCacheSizes() },
                isOneHandedGesturesEnabled = isOneHandedGesturesEnabled,
                onToggleOneHandedGestures = { viewModel.toggleOneHandedGestures() },
                gestureSensitivity = gestureSensitivity,
                onSetGestureSensitivity = { viewModel.setGestureSensitivity(it) }
            )
        }

        if (showHomeDownloadsDialog) {
            DownloadsDialog(
                show = true,
                onDismiss = { showHomeDownloadsDialog = false },
                downloadedFiles = downloadedFiles,
                activeDownloads = activeDownloads,
                onNavigate = { viewModel.navigateTo(it) },
                onDeleteDownloadedFile = { id, path -> viewModel.deleteDownloadedFile(id, path) },
                onPauseDownload = { taskId -> viewModel.pauseDownload(taskId) },
                onResumeDownload = { taskId -> viewModel.resumeDownload(taskId) },
                onCancelDownload = { taskId -> viewModel.cancelDownload(taskId) },
                onInitiateDownload = { url, name -> viewModel.downloadFile(url = url, customFileName = name) },
                onClearCompletedDownloads = { viewModel.clearCompletedDownloads() },
                onClearAllDownloadedFiles = { viewModel.clearAllDownloadedFiles() },
                getStorageUsageInfo = { viewModel.getStorageUsageInfo() }
            )
        }
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
    onAdBlocked: () -> Unit = {},
    onWebViewFailed: () -> Unit = {}
) {
    val lastLoadedUrl = remember { arrayOf("") }

    AndroidView(
        factory = { context ->
            val container = android.widget.FrameLayout(context)
            try {
                val webView = WebView(context).apply {
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
                                            color: #e5e7eb !important; 
                                            font-family: $fontStyleValue, system-ui, -apple-system, sans-serif !important; 
                                            line-height: $lineHeightMultiplier !important; 
                                            text-align: $textAlignValue !important;
                                            text-justify: inter-ideograph !important;
                                            padding: 14% 10% 28% 10% !important; /* Circular WearOS screen optimized safe zone */
                                            font-size: ${textZoom}% !important;
                                            letter-spacing: 0.05em !important;
                                            word-break: break-word !important;
                                            text-rendering: optimizeLegibility !important;
                                            -webkit-font-smoothing: antialiased !important;
                                        }
                                        p {
                                            $pStyle
                                            margin-top: 0 !important;
                                            margin-bottom: 1.5em !important;
                                            text-align: $textAlignValue !important;
                                        }
                                        h1, h2, h3, h4, h5, h6 { 
                                            color: #30f2c0 !important; /* Premium neon teal header */
                                            font-weight: bold !important;
                                            cursor: pointer !important;
                                            border-left: 3px solid #30f2c0 !important;
                                            border-bottom: 1px dashed rgba(255, 255, 255, 0.15) !important;
                                            padding-left: 8px !important;
                                            padding-bottom: 6px !important;
                                            margin-top: 1.8em !important;
                                            margin-bottom: 0.8em !important;
                                            line-height: 1.3 !important;
                                        }
                                        a {
                                            color: #81d4fa !important;
                                            text-decoration: underline !important;
                                            cursor: pointer !important;
                                        }
                                        blockquote {
                                            border-left: 3px solid #ffb74d !important;
                                            padding-left: 10px !important;
                                            margin: 1.2em 0 !important;
                                            color: #ffb74d !important;
                                            font-style: italic !important;
                                        }
                                        pre, code {
                                            background-color: #1a1a1a !important;
                                            color: #ffd54f !important;
                                            font-family: monospace !important;
                                            padding: 2px 4px !important;
                                            border-radius: 4px !important;
                                            font-size: 0.9em !important;
                                            word-break: break-all !important;
                                        }
                                        ul, ol {
                                            padding-left: 1.2em !important;
                                            margin-bottom: 1.2em !important;
                                        }
                                        li {
                                            margin-bottom: 0.6em !important;
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
                                            padding: 12% 10% 24% 10% !important; /* Circular WearOS screen optimized safe zone */
                                            background: black !important;
                                            color: #e5e7eb !important;
                                            font-family: system-ui, -apple-system, sans-serif !important;
                                            line-height: 1.5 !important;
                                            letter-spacing: 0.04em !important;
                                            font-size: ${textZoom}% !important;
                                            word-break: break-word !important;
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
                container.addView(webView)
            } catch (e: Throwable) {
                val errorView = android.widget.TextView(context).apply {
                    text = "無法載入瀏覽器引擎。請確定您的手錶已安裝並啟用「Android System WebView」。\n\n錯誤: ${e.message}"
                    setTextColor(android.graphics.Color.WHITE)
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 32, 32, 32)
                }
                container.addView(errorView)
                onWebViewFailed()
            }
            container
        },
        update = { container ->
            val webView = container.getChildAt(0) as? WebView ?: return@AndroidView
            
            val webViewUrl = webView.url
            val areSame = areUrlsSame(webViewUrl, url)
            
            if (url != "pixelbrowser://home" && !url.startsWith("pixelbrowser://")) {
                if (!areSame && url != lastLoadedUrl[0]) {
                    lastLoadedUrl[0] = url
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
        onRelease = { container ->
            val webView = container.getChildAt(0) as? WebView
            webView?.stopLoading()
            webView?.clearHistory()
            webView?.removeAllViews()
            webView?.destroy()
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
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
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
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
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
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
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
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = dialogAlpha
                },
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
    offlinePages: List<com.example.data.OfflinePage> = emptyList(),
    downloadedFiles: List<com.example.data.DownloadedFile>,
    history: List<com.example.data.HistoryEntry>,
    searchHistory: List<com.example.data.SearchHistory>,
    onAddSearchHistory: (String) -> Unit,
    onDeleteSearchHistory: (Long) -> Unit,
    onClearSearchHistory: () -> Unit,
    currentUrl: String,
    pageTitle: String,
    onAddBookmark: (String, String) -> Unit,
    onSaveOfflinePage: () -> Unit = {},
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
                modifier = Modifier.height(60.dp),
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

                // 儲存離線網頁
                if (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")) {
                    item {
                        val isOfflineSaved = offlinePages.any { it.url == currentUrl }
                        val offlineSaveInteraction = remember { MutableInteractionSource() }
                        Card(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSaveOfflinePage()
                                onClose()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOfflineSaved) Color(0xFF1B5E20) else MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .expressiveScale(offlineSaveInteraction),
                            interactionSource = offlineSaveInteraction
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOfflineSaved) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isOfflineSaved) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Column {
                                    Text(
                                        if (isOfflineSaved) "重新下載離線網頁" else "下載網頁供離線閱讀",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isOfflineSaved) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        "完整儲存 HTML 與本文供無網時檢視",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = if (isOfflineSaved) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 離線網頁庫入口
                item {
                    val offlineLibInteraction = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigate("pixelbrowser://offline")
                            onClose()
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(offlineLibInteraction),
                        interactionSource = offlineLibInteraction
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    "離線網頁庫",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${offlinePages.size} 篇已下載網頁",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
                    val geckoInt = remember { MutableInteractionSource() }
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = android.content.Intent(context, com.example.GeckoBrowserActivity::class.java)
                            context.startActivity(intent)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .expressiveScale(geckoInt),
                        interactionSource = geckoInt
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "GeckoView 下拉重新整理",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text("GeckoView (含下拉重新整理)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
    offlinePages: List<com.example.data.OfflinePage> = emptyList(),
    isOnline: Boolean = true,
    onAddLocalBookmark: (String, String) -> Unit,
    onDeleteLocalBookmark: (String) -> Unit,
    searchHistory: List<com.example.data.SearchHistory>,
    onAddSearchHistory: (String) -> Unit,
    onDeleteSearchHistory: (Long) -> Unit,
    onClearSearchHistory: () -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onUpdateBookmark: (com.example.data.Bookmark) -> Unit,
    onNavigate: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    var showSearchDialog by remember { mutableStateOf(false) }

    // 檢查剪貼簿是否有複製網址（非同步延遲載入，避免阻塞首頁首幀渲染）
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var clipboardText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
        try {
            val clip = clipboardManager.getText()?.text?.trim()
            if (!clip.isNullOrBlank() && (clip.startsWith("http://") || clip.startsWith("https://") || (clip.contains(".") && !clip.contains(" ") && clip.length > 3))) {
                clipboardText = clip
            }
        } catch (_: Exception) {}
    }

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

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus request failure
        }
    }

    // 常用網站快速啟動資料列表 (固定常數 remember，避免重複分配)
    val quickShortcuts = remember {
        listOf(
            Triple("Google", "https://www.google.com", Icons.Default.Search),
            Triple("YouTube", "https://m.youtube.com", Icons.Default.PlayArrow),
            Triple("維基百科", "https://zh.m.wikipedia.org", Icons.Default.MenuBook),
            Triple("新聞", "https://news.google.com", Icons.Default.Public)
        )
    }

    val scalingParams = remember {
        ScalingLazyColumnDefaults.scalingParams(
            edgeScale = 0.85f,
            edgeAlpha = 0.7f,
            minTransitionArea = 0.2f,
            maxTransitionArea = 0.8f
        )
    }
    val flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)

    val isScreenRound = androidx.compose.ui.platform.LocalConfiguration.current.isScreenRound

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        ScreenScaffold(
            scrollState = listState,
            bottomButton = {
                EdgeButton(
                    modifier = Modifier.height(60.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenSettings()
                    }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "設定", modifier = Modifier.size(24.dp))
                    Text("設定")
                }
            }
        ) {
            ScalingLazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = if (isScreenRound) 36.dp else 24.dp,
                    start = if (isScreenRound) 14.dp else 8.dp,
                    end = if (isScreenRound) 14.dp else 8.dp,
                    bottom = if (isScreenRound) 76.dp else 60.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                scalingParams = scalingParams,
                flingBehavior = flingBehavior,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .focusRequester(focusRequester)
                    .focusable()
                    .rotaryScrollable(
                        behavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                        focusRequester = focusRequester
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item(key = "header") {
                    ListHeader {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Pixel Browser",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 離線模式橫幅通知
                if (!isOnline) {
                    item(key = "offline_banner") {
                        Card(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigate("pixelbrowser://offline")
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE65100).copy(alpha = 0.9f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "目前處於離線狀態",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "點此開啟已下載的 ${offlinePages.size} 篇離線網頁",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 搜尋主卡片
                item(key = "search_card") {
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSearchDialog = true
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "搜尋",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "搜尋或輸入網址",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Google • 語音 • 書籤",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    startVoiceSearch()
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "語音搜尋",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 剪貼簿快速貼上推薦（若有複製網址）
                if (clipboardText != null) {
                    val clip = clipboardText!!
                    item(key = "clipboard_bar") {
                        Card(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigate(
                                    if (clip.startsWith("http://") || clip.startsWith("https://")) {
                                        clip
                                    } else {
                                        "https://$clip"
                                    }
                                )
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "前往剪貼簿: ${clip.take(18)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 快捷撥號 (Speed Dial Shortcuts)
                item(key = "speed_dial_grid") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickShortcuts.forEach { (name, url, iconVector) ->
                            Card(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNavigate(url)
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = name,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 快捷功能按鈕組：瀏覽紀錄、下載管理、離線網頁
                item(key = "quick_actions_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 瀏覽紀錄
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigate("pixelbrowser://history")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "紀錄",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 下載管理
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenDownloads()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "下載",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 離線網頁
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigate("pixelbrowser://offline")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (offlinePages.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (offlinePages.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "離線(${offlinePages.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (offlinePages.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 書籤區域
                if (localBookmarks.isNotEmpty()) {
                    item(key = "local_bookmarks_header") {
                        ListHeader {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "我的書籤",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "+ 新增",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
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
                            url = bookmark.url
                        )
                    }
                } else {
                    item(key = "no_bookmarks_placeholder") {
                        Card(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showAddLocalBookmarkDialog = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "新增常用書籤",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "一鍵快速造訪喜愛網站",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "新增",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (bookmarks.isNotEmpty()) {
                    item(key = "global_bookmarks_header") {
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
                                Text("所有書籤", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "管理書籤",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    items(items = bookmarks.take(3), key = { "global_${it.url}" }) { bookmark ->
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
                            url = bookmark.url
                        )
                    }

                    if (bookmarks.size > 3) {
                        item(key = "show_all_bookmarks_btn") {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showAllBookmarksDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text("顯示所有書籤 (${bookmarks.size})", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSearchDialog) {
        SearchDialog(
            show = true,
            onDismiss = { showSearchDialog = false },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            searchHistory = searchHistory,
            onAddSearchHistory = onAddSearchHistory,
            onDeleteSearchHistory = onDeleteSearchHistory,
            onNavigate = onNavigate
        )
    }

    if (bookmarkToEdit != null) {
        BookmarkEditDialog(
            bookmark = bookmarkToEdit,
            onDismiss = { bookmarkToEdit = null },
            onUpdateBookmark = onUpdateBookmark
        )
    }

    if (bookmarkToDelete != null) {
        BookmarkDeleteDialog(
            bookmark = bookmarkToDelete,
            onDismiss = { bookmarkToDelete = null },
            onDeleteBookmark = onDeleteBookmark
        )
    }

    if (showAddLocalBookmarkDialog) {
        AddLocalBookmarkDialog(
            show = true,
            onDismiss = { showAddLocalBookmarkDialog = false },
            onAdd = { title, url ->
                onAddLocalBookmark(url, title)
                showAddLocalBookmarkDialog = false
            }
        )
    }

    if (showAllBookmarksDialog) {
        AllBookmarksDialog(
            show = true,
            onDismiss = { showAllBookmarksDialog = false },
            bookmarks = bookmarks,
            onNavigate = onNavigate,
            onEditBookmark = { bookmarkToEdit = it },
            onDeleteBookmark = { bookmarkToDelete = it }
        )
    }
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
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
    onDismiss: () -> Unit,
    onOpenOfflinePages: (() -> Unit)? = null
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "No internet connection",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "手錶未連上網際網路",
                color = Color.White,
                style = TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 15.sp
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (onOpenOfflinePages != null) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenOfflinePages()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("瀏覽離線網頁", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        EdgeButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDismiss()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
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
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        val isWebUrl = url.startsWith("http://") || url.startsWith("https://")
        val fallbackLetter = remember(title) { title.take(1).uppercase() }
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.Black.copy(alpha = 0.45f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackLetter,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isWebUrl) {
                val context = LocalContext.current
                val imageRequest = remember(url, context) {
                    ImageRequest.Builder(context)
                        .data("https://www.google.com/s2/favicons?sz=64&domain_url=$url")
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "網頁圖標",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
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
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val executeSearch = {
        val trimmed = searchQuery.trim()
        if (trimmed.isNotBlank()) {
            onAddSearchHistory(trimmed)
            val dest = if (trimmed.contains(".") && !trimmed.contains(" ")) {
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
            } else {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                "https://www.google.com/search?q=$encoded"
            }
            onNavigate(dest)
            onDismiss()
        }
    }

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
                    modifier = Modifier.height(60.dp),
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
                    item(key = "search_header") {
                        ListHeader {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("搜尋與網址列", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item(key = "input_box") {
                        Card(
                            onClick = {},
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "輸入網址或關鍵字...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = { executeSearch() }
                                        )
                                    )
                                }

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSearchQueryChange("")
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "清空文字",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "quick_actions") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Paste from clipboard button
                            CompactChip(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        onSearchQueryChange(clipText)
                                    }
                                },
                                label = { Text("📋 貼上剪貼簿", style = MaterialTheme.typography.labelSmall) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )

                            if (searchQuery.isNotEmpty()) {
                                CompactChip(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSearchQueryChange("")
                                    },
                                    label = { Text("✕ 清空", style = MaterialTheme.typography.labelSmall) },
                                    colors = ChipDefaults.chipColors(
                                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    item(key = "go_button") {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                executeSearch()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("搜尋並前往", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (searchHistory.isNotEmpty()) {
                        item(key = "history_header") {
                            ListHeader {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("最近搜尋記錄", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                        items(items = searchHistory.take(5), key = { "search_hist_${it.id}" }) { entry ->
                            CompactListRow(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSearchQueryChange(entry.query)
                                },
                                onDelete = { onDeleteSearchHistory(entry.id) },
                                title = entry.query,
                                url = "搜尋記錄"
                            )
                        }
                    }

                    item(key = "bottom_spacer") {
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
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "選擇搜尋方式",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 語音搜尋卡片
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onVoiceSearch()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "語音搜尋",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "語音搜尋",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // 鍵盤搜尋卡片
                    Card(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onKeyboardSearch()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "鍵盤搜尋",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "鍵盤輸入",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
    onRefreshCache: () -> Unit,
    isOneHandedGesturesEnabled: Boolean = true,
    onToggleOneHandedGestures: () -> Unit = {},
    gestureSensitivity: String = "標準",
    onSetGestureSensitivity: (String) -> Unit = {}
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
                    modifier = Modifier.height(60.dp),
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

                // --- 單手體感手勢控制 ---
                item {
                    ListHeader {
                        Text("單手體感手勢", style = MaterialTheme.typography.titleSmall)
                    }
                }

                item {
                    SwitchButton(
                        checked = isOneHandedGesturesEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleOneHandedGestures() 
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PanTool,
                                contentDescription = null,
                                tint = if (isOneHandedGesturesEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("單手體感手勢操作", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isOneHandedGesturesEnabled) "雙指捏合 / 翻轉手腕" else "手勢操作已關閉", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (isOneHandedGesturesEnabled) {
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
                            Text("手勢感應靈敏度", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val sensitivities = listOf("低靈敏度", "標準", "高靈敏度")
                                sensitivities.forEach { sens ->
                                    val isSelected = gestureSensitivity == sens
                                    Button(
                                        onClick = { 
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSetGestureSensitivity(sens) 
                                        },
                                        modifier = Modifier.height(28.dp).weight(1f).padding(horizontal = 2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(sens, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            onClick = {},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "💡 手勢操作指南",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "• 雙指捏合 (Double Pinch)：在首頁開啟語音搜尋；在網頁觸發朗讀\n• 翻轉手腕 (Wrist Turn)：快速關閉彈出面板或返回上一頁",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
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
                    modifier = Modifier.height(60.dp),
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
                    modifier = Modifier.height(60.dp),
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
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDeleteBookmark(bookmark)
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .background(MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(28.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "刪除",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            },
                            content = {
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
    activeDownloads: List<com.example.data.DownloadTask> = emptyList(),
    onNavigate: (String) -> Unit,
    onDeleteDownloadedFile: (Long, String) -> Unit,
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onInitiateDownload: (String, String?) -> Unit = { _, _ -> },
    onClearCompletedDownloads: () -> Unit = {},
    onClearAllDownloadedFiles: () -> Unit = {},
    getStorageUsageInfo: () -> com.example.data.StorageUsageInfo = { com.example.data.StorageUsageInfo(0L, 0L, 0L) }
) {
    if (!show) return

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableIntStateOf(0) } // 0: 全部, 1: 媒體, 2: 文件, 3: 壓縮/軟體
    var showManualDownloadDialog by remember { mutableStateOf(false) }
    var selectedFileForDetail by remember { mutableStateOf<com.example.data.DownloadedFile?>(null) }
    var storageInfo by remember { mutableStateOf(getStorageUsageInfo()) }

    LaunchedEffect(show, downloadedFiles.size, activeDownloads.size) {
        storageInfo = getStorageUsageInfo()
    }

    // 分類篩選邏輯
    val filteredCompletedFiles = remember(downloadedFiles, selectedCategory) {
        when (selectedCategory) {
            1 -> downloadedFiles.filter {
                val mime = it.mimeType.lowercase()
                val ext = it.fileName.substringAfterLast('.', "").lowercase()
                mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/") ||
                        ext in listOf("jpg", "jpeg", "png", "gif", "webp", "mp4", "mkv", "mp3", "wav")
            }
            2 -> downloadedFiles.filter {
                val mime = it.mimeType.lowercase()
                val ext = it.fileName.substringAfterLast('.', "").lowercase()
                mime.contains("pdf") || mime.contains("document") || mime.contains("text") ||
                        ext in listOf("pdf", "doc", "docx", "txt", "html", "json")
            }
            3 -> downloadedFiles.filter {
                val mime = it.mimeType.lowercase()
                val ext = it.fileName.substringAfterLast('.', "").lowercase()
                mime.contains("zip") || mime.contains("compressed") ||
                        ext in listOf("zip", "rar", "7z", "apk", "bin", "tar", "gz")
            }
            else -> downloadedFiles
        }
    }

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
                    modifier = Modifier.height(60.dp),
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
                    .padding(6.dp),
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("進階下載管理", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }

                    // 1. 手錶儲存空間使用量儀表 (Watch Storage Usage Bar)
                    item {
                        Card(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("手錶儲存空間", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "${formatFileSize(storageInfo.usedBytes)} / ${formatFileSize(storageInfo.totalBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                val usedRatio = if (storageInfo.totalBytes > 0) (storageInfo.usedBytes.toFloat() / storageInfo.totalBytes).coerceIn(0f, 1f) else 0.3f
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { usedRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                                Text(
                                    "下載夾佔用：${formatFileSize(storageInfo.downloadsFolderBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. 新增手動下載網址按鈕 (Initiate Manual Download)
                    item {
                        val manualInt = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showManualDownloadDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .expressiveScale(manualInt),
                            interactionSource = manualInt
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("➕ 新增手動下載網址", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // 3. 進行中的下載任務 (Active Downloads)
                    if (activeDownloads.isNotEmpty()) {
                        item {
                            ListHeader {
                                Text("進行中的下載 (${activeDownloads.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        items(items = activeDownloads, key = { "active_${it.id}" }) { task ->
                            Card(
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (task.status) {
                                        com.example.data.DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        com.example.data.DownloadStatus.PAUSED -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        com.example.data.DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            task.fileName,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            when (task.status) {
                                                com.example.data.DownloadStatus.DOWNLOADING -> "${task.progress}%"
                                                com.example.data.DownloadStatus.PAUSED -> "已暫停"
                                                com.example.data.DownloadStatus.COMPLETED -> "已完成"
                                                com.example.data.DownloadStatus.FAILED -> "失敗"
                                                com.example.data.DownloadStatus.CANCELLED -> "已取消"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (task.status == com.example.data.DownloadStatus.DOWNLOADING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (task.status == com.example.data.DownloadStatus.DOWNLOADING || task.status == com.example.data.DownloadStatus.PAUSED) {
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = { (task.progress.toFloat() / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(3.5.dp)),
                                            color = if (task.status == com.example.data.DownloadStatus.DOWNLOADING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (task.status == com.example.data.DownloadStatus.DOWNLOADING) {
                                                "${formatFileSize(task.bytesDownloaded)} / ${if (task.totalBytes > 0) formatFileSize(task.totalBytes) else "未知"} (${String.format("%.1f", task.speedKbps)} KB/s)"
                                            } else {
                                                formatFileSize(task.bytesDownloaded)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (task.status == com.example.data.DownloadStatus.DOWNLOADING) {
                                                IconButton(
                                                    onClick = { onPauseDownload(task.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Pause, contentDescription = "暫停", modifier = Modifier.size(16.dp))
                                                }
                                            } else if (task.status == com.example.data.DownloadStatus.PAUSED) {
                                                IconButton(
                                                    onClick = { onResumeDownload(task.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = "繼續", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            IconButton(
                                                onClick = { onCancelDownload(task.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "取消", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. 分類標籤篩選列 (Category Pills)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val categories = listOf("全部", "媒體", "文件", "軟體/壓縮")
                            categories.forEachIndexed { index, title ->
                                val isSelected = selectedCategory == index
                                CompactChip(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedCategory = index
                                    },
                                    label = { Text(title, fontSize = 10.sp) },
                                    colors = if (isSelected) {
                                        ChipDefaults.chipColors(backgroundColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        ChipDefaults.chipColors(backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                    }
                                )
                            }
                        }
                    }

                    // 5. 已完成的檔案列表 (Completed Downloaded Files)
                    item {
                        ListHeader {
                            Text("已完成的檔案 (${filteredCompletedFiles.size})", style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    if (filteredCompletedFiles.isEmpty()) {
                        item {
                            Text("目前沒有此類別的下載檔案", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(items = filteredCompletedFiles, key = { it.id }) { file ->
                            val cardInt = remember { MutableInteractionSource() }
                            val (typeIcon, iconColor) = getFileTypeIcon(file.mimeType, file.fileName)

                            Card(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedFileForDetail = file
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
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
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

                    // 6. 清理與維護功能 (Clear Completed Tasks & All Files)
                    if (activeDownloads.any { it.status == com.example.data.DownloadStatus.COMPLETED } || downloadedFiles.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (activeDownloads.any { it.status == com.example.data.DownloadStatus.COMPLETED }) {
                                    CompactChip(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onClearCompletedDownloads()
                                        },
                                        label = { Text("清除已完成的任務標籤") },
                                        colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                    )
                                }

                                if (downloadedFiles.isNotEmpty()) {
                                    CompactChip(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onClearAllDownloadedFiles()
                                        },
                                        label = { Text("清空所有下載紀錄與檔案", color = MaterialTheme.colorScheme.error) },
                                        colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                    )
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

    // 7. 手動輸入下載網址對話框
    if (showManualDownloadDialog) {
        InitiateDownloadDialog(
            show = showManualDownloadDialog,
            onDismiss = { showManualDownloadDialog = false },
            onConfirmDownload = { url, customName ->
                onInitiateDownload(url, customName)
                showManualDownloadDialog = false
            }
        )
    }

    // 8. 檔案詳情與操作選單對話框 (File Action Sheet)
    selectedFileForDetail?.let { file ->
        FileDetailActionDialog(
            file = file,
            onDismiss = { selectedFileForDetail = null },
            onOpenFile = {
                onNavigate("file://${file.localPath}")
                selectedFileForDetail = null
                onDismiss()
            },
            onReDownload = {
                onInitiateDownload(file.url, file.fileName)
                selectedFileForDetail = null
            },
            onDelete = {
                onDeleteDownloadedFile(file.id, file.localPath)
                selectedFileForDetail = null
            }
        )
    }
}

/**
 * 檔案類型對應圖示與色彩
 */
private fun getFileTypeIcon(mimeType: String, fileName: String): Pair<ImageVector, Color> {
    val lowerMime = mimeType.lowercase()
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when {
        lowerMime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "svg") ->
            Icons.Default.Image to Color(0xFF38BDF8)
        lowerMime.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "avi") ->
            Icons.Default.Movie to Color(0xFFF43F5E)
        lowerMime.startsWith("audio/") || ext in listOf("mp3", "wav", "ogg", "flac") ->
            Icons.Default.MusicNote to Color(0xFFA855F7)
        lowerMime.contains("pdf") || lowerMime.contains("document") || ext in listOf("pdf", "doc", "docx", "txt", "html", "json") ->
            Icons.Default.Description to Color(0xFFF59E0B)
        lowerMime.contains("zip") || lowerMime.contains("compressed") || ext in listOf("zip", "rar", "7z", "apk", "tar", "gz", "bin") ->
            Icons.Default.FolderZip to Color(0xFF10B981)
        else ->
            Icons.Default.InsertDriveFile to Color(0xFF94A3B8)
    }
}

/**
 * 手動發起下載對話框
 */
@Composable
fun InitiateDownloadDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirmDownload: (url: String, fileName: String?) -> Unit
) {
    if (!show) return

    var inputUrl by remember { mutableStateOf("https://") }
    var customFileName by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    ExpressiveDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("新增手動下載", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                label = { Text("下載檔案 URL", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = customFileName,
                onValueChange = { customFileName = it },
                label = { Text("自訂檔名 (可選)", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactChip(
                    onClick = { onDismiss() },
                    label = { Text("取消") },
                    colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                )

                CompactChip(
                    onClick = {
                        if (inputUrl.isNotBlank() && inputUrl != "https://") {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onConfirmDownload(inputUrl.trim(), customFileName.ifBlank { null })
                        }
                    },
                    label = { Text("開始下載") },
                    colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
    }
}

/**
 * 已下載檔案詳細操作選單對話框 (File Details Action Sheet)
 */
@Composable
fun FileDetailActionDialog(
    file: com.example.data.DownloadedFile,
    onDismiss: () -> Unit,
    onOpenFile: () -> Unit,
    onReDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val (typeIcon, iconColor) = getFileTypeIcon(file.mimeType, file.fileName)

    ExpressiveDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(typeIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Text(file.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Text(
                "大小：${formatFileSize(file.fileSize)} | 格式：${file.mimeType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenFile()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("📂 開啟 / 預覽檔案", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Download URL", file.url)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "已複製下載連結", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Text("📋 複製下載連結", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onReDownload()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Text("🔄 重新下載檔案", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text("🗑️ 刪除檔案與紀錄", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }

            CompactChip(
                onClick = onDismiss,
                label = { Text("返回") },
                colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            )
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
                    modifier = Modifier.height(60.dp),
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
            strokeWidth = 8.dp
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
                    modifier = Modifier.height(60.dp),
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
