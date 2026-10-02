package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListSubheader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.example.data.Bookmark
import com.example.data.HistoryEntry
import com.example.data.OfflineArticle

data class QuickShortcut(
    val title: String,
    val url: String,
    val badge: String,
    val badgeBgColor: Color
)

val DEFAULT_SHORTCUTS = listOf(
    QuickShortcut("Google", "https://www.google.com", "G", Color(0xFF4285F4)),
    QuickShortcut("維基百科", "https://zh.wikipedia.org", "W", Color(0xFF4B5563)),
    QuickShortcut("Yahoo", "https://tw.yahoo.com", "Y", Color(0xFF7C3AED)),
    QuickShortcut("GitHub", "https://github.com", "GH", Color(0xFF24292E)),
    QuickShortcut("YouTube", "https://m.youtube.com", "▶", Color(0xFFEF4444)),
    QuickShortcut("百度", "https://www.baidu.com", "B", Color(0xFF2563EB))
)

/**
 * 深度採用 Wear OS 4/5 Material 3 規範之首頁介面：
 * 完美模仿使用者提供之 Bento 佈局，包含：
 * - 左上角：搜尋（長形淺青色膠囊）
 * - 右上角：語音搜尋（深青色近圓形膠囊）
 * - 左下角：瀏覽紀錄（深青色半寬膠囊）
 * - 右下角：下載（深青色半寬膠囊）
 * - 下方：「近期紀錄」清單卡片
 */
@Composable
fun HomeScreen(
    isRound: Boolean,
    shortcuts: List<QuickShortcut> = DEFAULT_SHORTCUTS,
    savedBookmarks: List<Bookmark>,
    savedOfflineArticles: List<OfflineArticle> = emptyList(),
    recentHistory: List<HistoryEntry> = emptyList(),
    isIncognitoMode: Boolean = false,
    onOpenSearchInput: () -> Unit,
    onVoiceSearch: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    onOpenOfflineArticle: (OfflineArticle) -> Unit,
    onOpenDownloads: () -> Unit,
    onToggleIncognito: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                false
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            contentPadding = PaddingValues(
                top = if (isRound) 22.dp else 10.dp,
                bottom = if (isRound) 68.dp else 26.dp,
                start = if (isRound) 14.dp else 8.dp,
                end = if (isRound) 14.dp else 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 第一排 Bento 膠囊：左上【搜尋】(淺青) + 右上【語音搜尋】(深青)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(if (isRound) 0.90f else 0.96f)
                        .padding(top = if (isRound) 14.dp else 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左上角：搜尋 (動態色彩主要色調 primary，搭配 onPrimary 圖示)
                    Box(
                        modifier = Modifier
                            .weight(0.64f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenSearchInput()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜尋",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // 右上角：語音搜尋 (動態色彩次要容器色 secondaryContainer，搭配 onSecondaryContainer 圖示)
                    Box(
                        modifier = Modifier
                            .weight(0.36f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onVoiceSearch()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "語音搜尋",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // 第二排 Bento 膠囊：左下【瀏覽紀錄】+ 右下【下載】
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.90f else 0.96f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左下角：瀏覽紀錄 (動態色彩次要容器色)
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenHistory()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "瀏覽紀錄",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // 右下角：下載 (動態色彩次要容器色)
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenDownloads()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "下載與離線內容",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // 「近期紀錄」標題（完美仿照圖片字體排版，採用動態主題文字色）
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "近期紀錄",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            // 近期紀錄卡片清單（採用動態 surfaceContainer 色彩與圓角）
            if (recentHistory.isNotEmpty()) {
                items(recentHistory.take(4)) { entry ->
                    Card(
                        onClick = { onNavigateUrl(entry.url) },
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.88f else 0.94f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = entry.title.ifBlank { entry.url },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // 若尚無歷史紀錄，顯示預設最近推薦紀錄卡片
                item {
                    Card(
                        onClick = { onNavigateUrl("https://www.google.com") },
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.88f else 0.94f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Google 首頁",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                item {
                    Card(
                        onClick = { onNavigateUrl("https://zh.wikipedia.org") },
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.88f else 0.94f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "維基百科",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 離線下載文章區塊（若有離線內容）
            if (savedOfflineArticles.isNotEmpty()) {
                item {
                    ListSubheader(
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f)
                    ) {
                        Text(text = "離線下載內容 (${savedOfflineArticles.size})", color = Color(0xFF34D399))
                    }
                }

                items(savedOfflineArticles.take(3)) { article ->
                    TitleCard(
                        onClick = { onOpenOfflineArticle(article) },
                        title = {
                            Text(
                                text = article.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        subtitle = {
                            Text(
                                text = "約 ${article.readingTimeMinutes} 分鐘 · 離線文章",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        time = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
            }

            // 常用網站快速捷徑
            item {
                ListSubheader(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f)
                ) {
                    Text(text = "常用網站", color = MaterialTheme.colorScheme.secondary)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    shortcuts.take(3).forEach { item ->
                        ShortcutButton(
                            shortcut = item,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateUrl(item.url) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    shortcuts.drop(3).take(3).forEach { item ->
                        ShortcutButton(
                            shortcut = item,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateUrl(item.url) }
                        )
                    }
                }
            }

            // 系統工具與無痕模式
            item {
                ListSubheader(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f)
                ) {
                    Text(text = "快捷工具", color = MaterialTheme.colorScheme.secondary)
                }
            }

            item {
                FilledTonalButton(
                    onClick = onToggleIncognito,
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.92f),
                    colors = if (isIncognitoMode) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF4C1D95),
                            contentColor = Color(0xFFE9D5FF)
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    label = {
                        Text(if (isIncognitoMode) "無痕隱私模式已開啟 🕶️" else "無痕模式：已關閉")
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

            item {
                FilledTonalButton(
                    onClick = onOpenBookmarks,
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.92f),
                    label = { Text("書籤與離線庫") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            item {
                FilledTonalButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.92f),
                    label = { Text("瀏覽器設定") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        // Material 3 ScrollIndicator 弧形滾動指示器
        ScrollIndicator(
            state = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
fun ShortcutButton(
    shortcut: QuickShortcut,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(shortcut.badgeBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = shortcut.badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = shortcut.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
