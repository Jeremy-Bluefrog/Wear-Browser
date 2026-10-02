package com.example.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListSubheader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.example.data.Bookmark
import com.example.data.OfflineArticle

/**
 * 深度採用 Wear OS Material 3 規範之書籤與離線文章管理介面
 */
@Composable
fun BookmarksScreen(
    isRound: Boolean,
    bookmarks: List<Bookmark>,
    offlineArticles: List<OfflineArticle> = emptyList(),
    onNavigateUrl: (String) -> Unit,
    onOpenOfflineArticle: (OfflineArticle) -> Unit = {},
    onDeleteBookmark: (String) -> Unit,
    onDeleteOfflineArticle: (String) -> Unit = {},
    onAddBookmark: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.size(30.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "書籤與離線庫",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FilledTonalIconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.size(30.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增書籤",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 離線文章區塊
            if (offlineArticles.isNotEmpty()) {
                item {
                    ListSubheader(
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                    ) {
                        Text(text = "離線文章 (${offlineArticles.size})", color = Color(0xFF34D399))
                    }
                }

                items(offlineArticles) { article ->
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
                                text = "約 ${article.readingTimeMinutes} 分鐘 · 點擊直接閱讀",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        time = {
                            FilledTonalIconButton(
                                onClick = { onDeleteOfflineArticle(article.url) },
                                modifier = Modifier.size(24.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "刪除離線文章",
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
            }

            // 我的書籤區塊
            item {
                ListSubheader(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                ) {
                    Text(text = "我的書籤 (${bookmarks.size})", color = MaterialTheme.colorScheme.secondary)
                }
            }

            if (bookmarks.isEmpty() && offlineArticles.isEmpty()) {
                item {
                    Text(
                        text = "目前尚無書籤或離線文章\n瀏覽網頁時點擊星號或儲存離線即可收藏",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.80f else 0.92f)
                            .padding(vertical = 18.dp)
                    )
                }
            } else if (bookmarks.isEmpty()) {
                item {
                    Text(
                        text = "尚無自訂網頁書籤",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.80f else 0.92f)
                            .padding(vertical = 8.dp)
                    )
                }
            } else {
                items(bookmarks) { bookmark ->
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
                            FilledTonalIconButton(
                                onClick = { onDeleteBookmark(bookmark.url) },
                                modifier = Modifier.size(24.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "刪除",
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
            }
        }

        ScrollIndicator(
            state = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        if (showAddDialog) {
            AddBookmarkDialog(
                isRound = isRound,
                onDismiss = { showAddDialog = false },
                onAdd = { url, title ->
                    showAddDialog = false
                    onAddBookmark(url, title)
                }
            )
        }
    }
}
