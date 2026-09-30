package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
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
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubheader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.example.data.Bookmark

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
 * 深度採用 Wear OS Material 3 設計語言之手錶主頁
 */
@Composable
fun HomeScreen(
    isRound: Boolean,
    shortcuts: List<QuickShortcut> = DEFAULT_SHORTCUTS,
    savedBookmarks: List<Bookmark>,
    onOpenSearchInput: () -> Unit,
    onVoiceSearch: () -> Unit,
    onNavigateUrl: (String) -> Unit,
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
            autoCentering = AutoCenteringParams(itemIndex = 1),
            contentPadding = PaddingValues(
                top = if (isRound) 44.dp else 18.dp,
                bottom = if (isRound) 68.dp else 26.dp,
                start = if (isRound) 16.dp else 8.dp,
                end = if (isRound) 16.dp else 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // App Title Header (採用 Material 3 品牌排版與副標)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(if (isRound) 0.82f else 0.94f)
                        .padding(bottom = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pixel Browser",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "GeckoView · 32-bit ARM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Material 3 搜尋膠囊卡片 (結合 FilledTonalIconButton 與安全內縮)
            item {
                Card(
                    onClick = onOpenSearchInput,
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜尋",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "搜尋或輸入網址",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onVoiceSearch,
                            modifier = Modifier.size(30.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "語音輸入",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Material 3 ListHeader: 快速捷徑
            item {
                ListSubheader(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                ) {
                    Text(text = "常用網站", color = MaterialTheme.colorScheme.secondary)
                }
            }

            // 捷徑卡片網格 Row 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
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

            // 捷徑卡片網格 Row 2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
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

            // Material 3 ListHeader: 我的書籤
            if (savedBookmarks.isNotEmpty()) {
                item {
                    ListSubheader(
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                    ) {
                        Text(text = "我的書籤", color = MaterialTheme.colorScheme.secondary)
                    }
                }

                items(savedBookmarks.take(3)) { bookmark ->
                    TitleCard(
                        onClick = { onNavigateUrl(bookmark.url) },
                        title = {
                            Text(
                                text = bookmark.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        subtitle = {
                            Text(
                                text = bookmark.url,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        time = {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                }
            }

            // Material 3 ListHeader: 導覽與管理
            item {
                ListSubheader(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                ) {
                    Text(text = "瀏覽管理", color = MaterialTheme.colorScheme.secondary)
                }
            }

            // 採用 Wear OS Material 3 FilledTonalButton 帶圖示與標籤
            item {
                FilledTonalButton(
                    onClick = onOpenBookmarks,
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.80f else 0.92f),
                    label = { Text("書籤管理") },
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
                    onClick = onOpenHistory,
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.80f else 0.92f),
                    label = { Text("歷史紀錄") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            item {
                FilledTonalButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.80f else 0.92f),
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

        // Material 3 ScrollIndicator 替換舊版 PositionIndicator
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
