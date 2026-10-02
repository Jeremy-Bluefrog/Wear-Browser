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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tab
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
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text

/**
 * 深度採用 Wear OS Material 3 規範之 GeckoBrowserView：
 * 整合頂部安全網址膠囊、無痕隱私指示、弧形進度條圖形、多標籤入口與二級快捷選單抽屜。
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
    tabCount: Int = 1,
    isIncognitoMode: Boolean = false,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onReaderDataExtracted: (String, String) -> Unit,
    onToggleReaderMode: () -> Unit,
    onExitReaderMode: () -> Unit,
    onOpenUrlInput: () -> Unit,
    onOpenTabs: () -> Unit,
    onSaveOfflineArticle: () -> Unit,
    onToggleIncognito: () -> Unit,
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
    var showQuickMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 若處於閱讀模式，直接切換為專屬閱讀視圖
    if (isReaderMode) {
        ReaderModeView(
            isRound = isRound,
            title = readerTitle,
            content = readerContent,
            onSaveOffline = onSaveOfflineArticle,
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
            .background(if (isPureBlackMode) Color.Black else Color.DarkGray)
            .onRotaryScrollEvent {
                val scrollDelta = it.verticalScrollPixels * rotarySpeed
                state.scrollBy(0f, scrollDelta)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                true
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        // GeckoView 網頁渲染主體
        GeckoViewWrapper(
            state = state,
            onPageStarted = onPageStarted,
            onPageFinished = onPageFinished,
            onProgressChanged = onProgressChanged,
            onNavigationStateChanged = onNavigationStateChanged,
            onReaderDataExtracted = onReaderDataExtracted,
            modifier = Modifier.fillMaxSize()
        )

        // 頂部圓形曲度進度條 (載入時呈現漸層流動光弧)
        if (isLoading && progress < 100) {
            if (isRound) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
                    val sweepAngle = (progress / 100f) * 120f
                    val startAngle = 210f
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFC084FC))
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
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

        // 頂部網址膠囊（可點擊快速修改網址、顯示 HTTPS / 無痕安全狀態與閱讀模式切換）
        AnimatedVisibility(
            visible = showBars && !showQuickMenu,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isRound) 14.dp else 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xEC18181B))
                    .border(1.dp, if (isIncognitoMode) Color(0x66C084FC) else Color(0x33FFFFFF), CircleShape)
                    .clickable { onOpenUrlInput() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 安全防護或無痕指示圖示
                if (isIncognitoMode) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "無痕瀏覽",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(11.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (state.isSecure) Icons.Default.Lock else Icons.Default.Public,
                        contentDescription = if (state.isSecure) "安全連接" else "網站",
                        tint = if (state.isSecure) Color(0xFF34D399) else Color.Gray,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isIncognitoMode) "無痕 · $displayHost" else displayHost,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 95.dp)
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

        // 懸浮底部主控 Dock（4個舒適的 34dp 觸控目標，安全高度 bottom = 32.dp，圓手錶完美居中）
        AnimatedVisibility(
            visible = showBars && !showQuickMenu,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isRound) 30.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xF218181B))
                    .border(1.dp, Color(0x38FFFFFF), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 上一頁按鈕
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (canGoBack) Color.White else Color(0xFF52525B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "上一頁",
                        modifier = Modifier.size(15.dp)
                    )
                }

                // 多標籤頁管理入口
                FilledTonalIconButton(
                    onClick = onOpenTabs,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFF27272A),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tab,
                            contentDescription = "標籤頁",
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$tabCount",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 快捷操作選單展開按鈕
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showQuickMenu = true
                    },
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "快捷選單",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 重新整理按鈕
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重新整理",
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // 二級快捷操作抽屜選單（全面 Material 3 排版，無貼邊，極致清晰）
        if (showQuickMenu) {
            val menuListState = rememberScalingLazyListState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF509090B))
                    .clickable { showQuickMenu = false }
            ) {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = menuListState,
                    autoCentering = AutoCenteringParams(itemIndex = 1),
                    contentPadding = PaddingValues(
                        top = if (isRound) 42.dp else 16.dp,
                        bottom = if (isRound) 68.dp else 24.dp,
                        start = if (isRound) 16.dp else 8.dp,
                        end = if (isRound) 16.dp else 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        ListHeader(modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)) {
                            Text(
                                text = "網頁快捷功能",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 閱讀模式
                    item {
                        FilledTonalButton(
                            onClick = {
                                showQuickMenu = false
                                onToggleReaderMode()
                            },
                            modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                            label = { Text("文章閱讀模式", style = MaterialTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    // 書籤切換
                    item {
                        FilledTonalButton(
                            onClick = {
                                showQuickMenu = false
                                onToggleBookmark()
                            },
                            modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                            label = {
                                Text(
                                    if (isBookmarked) "已加書籤 (點擊移除)" else "加入書籤",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (isBookmarked) Color(0xFFF59E0B) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    // 儲存為離線文章
                    item {
                        FilledTonalButton(
                            onClick = {
                                showQuickMenu = false
                                onSaveOfflineArticle()
                            },
                            modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                            label = { Text("儲存離線文章", style = MaterialTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    // 頁面字級縮放
                    item {
                        FilledTonalButton(
                            onClick = {
                                showQuickMenu = false
                                onOpenZoom()
                            },
                            modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                            label = { Text("頁面字級縮放", style = MaterialTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    // 無痕隱私瀏覽模式切換
                    item {
                        FilledTonalButton(
                            onClick = {
                                showQuickMenu = false
                                onToggleIncognito()
                            },
                            modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                            label = {
                                Text(
                                    if (isIncognitoMode) "關閉無痕模式" else "開啟無痕模式",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isIncognitoMode) Color(0xFFC084FC) else Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    // 回到瀏覽器首頁
                    item {
                        FilledTonalButton(
                            onClick = {
                                showQuickMenu = false
                                onGoHome()
                            },
                            modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                            label = { Text("返回首頁", style = MaterialTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    // 關閉抽屜選單按鈕
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item {
                        Button(
                            onClick = { showQuickMenu = false },
                            modifier = Modifier
                                .fillMaxWidth(if (isRound) 0.55f else 0.70f)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Text(text = "收起選單", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                ScrollIndicator(
                    state = menuListState,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}
