package com.example.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/**
 * 升級版 GeckoBrowserView：
 * 整合頂部網址膠囊、極簡閱讀模式、弧形進度條圖形與安全懸浮工具列。
 */
@Composable
fun GeckoBrowserView(
    state: GeckoState,
    pageTitle: String,
    isLoading: Boolean,
    progress: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isPureBlackMode: Boolean,
    isRound: Boolean,
    isBookmarked: Boolean,
    isReaderMode: Boolean,
    readerTitle: String,
    readerContent: String,
    rotarySpeed: Float = 2.0f,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onReaderDataExtracted: (String, String) -> Unit,
    onToggleReaderMode: () -> Unit,
    onExitReaderMode: () -> Unit,
    onOpenUrlInput: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenZoom: () -> Unit,
    onGoHome: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    var showBars by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 若處於閱讀模式，直接切換為專屬閱讀視圖
    if (isReaderMode) {
        ReaderModeView(
            isRound = isRound,
            title = readerTitle,
            content = readerContent,
            onExitReaderMode = onExitReaderMode
        )
        return
    }

    val displayHost = remember(state.currentUrl) {
        try {
            val uri = Uri.parse(state.currentUrl)
            uri.host ?: state.currentUrl.removePrefix("https://").removePrefix("http://").take(18)
        } catch (e: Exception) {
            "網頁瀏覽"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isPureBlackMode) Color.Black else Color.White)
            .onRotaryScrollEvent { event ->
                val deltaY = event.verticalScrollPixels * rotarySpeed
                state.scrollBy(0f, deltaY)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                true
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        // GeckoView 核心視圖
        GeckoViewWrapper(
            state = state,
            modifier = Modifier.fillMaxSize(),
            onPageStarted = onPageStarted,
            onPageFinished = onPageFinished,
            onProgressChanged = onProgressChanged,
            onNavigationStateChanged = onNavigationStateChanged,
            onReaderDataExtracted = onReaderDataExtracted
        )

        // 頂部圓弧載入進度條（修復圖形：包含半透明底軌與漸層發光進度弧線）
        if (isLoading && progress < 100) {
            if (isRound) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                ) {
                    val strokeW = 3.5.dp.toPx()
                    val arcSize = Size(size.width - strokeW, size.height - strokeW)
                    val topLeft = Offset(strokeW / 2f, strokeW / 2f)
                    val startAngle = -150f
                    val totalSweep = 120f
                    val currentSweep = (progress / 100f).coerceIn(0.05f, 1f) * totalSweep

                    // 底軌弧線
                    drawArc(
                        color = Color(0x3338BDF8),
                        startAngle = startAngle,
                        sweepAngle = totalSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // 漸層進度弧線
                    drawArc(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                        ),
                        startAngle = startAngle,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0x33000000))
                        .align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress / 100f)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // 頂部網址膠囊（可點擊快速修改網址、顯示 HTTPS 安全狀態與閱讀模式切換）
        AnimatedVisibility(
            visible = showBars,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isRound) 14.dp else 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xDC18181B))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onOpenUrlInput() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (state.isSecure) Icons.Default.Lock else Icons.Default.Public,
                    contentDescription = if (state.isSecure) "安全連接" else "網站",
                    tint = if (state.isSecure) Color(0xFF34D399) else Color.Gray,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = displayHost,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 100.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // 快速閱讀模式小按鈕
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF27272A))
                        .clickable { onToggleReaderMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "閱讀模式",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // 懸浮底部工具列（安全高度 bottom = 32.dp，精確掌控寬度 ~150dp，徹底解決貼邊問題）
        AnimatedVisibility(
            visible = showBars,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isRound) 32.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xF018181B))
                    .border(1.dp, Color(0x40FFFFFF), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 上一頁按鈕
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier.size(26.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (canGoBack) Color.White else Color(0xFF52525B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "上一頁",
                        modifier = Modifier.size(13.dp)
                    )
                }

                // 重新整理按鈕
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(26.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重新整理",
                        modifier = Modifier.size(13.dp)
                    )
                }

                // 首頁按鈕
                IconButton(
                    onClick = onGoHome,
                    modifier = Modifier.size(26.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "首頁",
                        modifier = Modifier.size(13.dp)
                    )
                }

                // 書籤切換按鈕
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(26.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isBookmarked) Color(0xFFF59E0B) else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "書籤",
                        modifier = Modifier.size(13.dp)
                    )
                }

                // 閱讀模式按鈕
                IconButton(
                    onClick = onToggleReaderMode,
                    modifier = Modifier.size(26.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF34D399))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "閱讀模式",
                        modifier = Modifier.size(13.dp)
                    )
                }

                // 縮放調整按鈕
                IconButton(
                    onClick = onOpenZoom,
                    modifier = Modifier.size(26.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFA78BFA))
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "縮放",
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
