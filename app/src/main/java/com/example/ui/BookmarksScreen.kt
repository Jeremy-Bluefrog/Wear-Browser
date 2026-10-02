package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 深度採用 Wear OS Material 3 規範之書籤與離線文章管理介面
 * 全新支援：從右向左滑動即可刪除書籤／離線內容（Swipe Right-to-Left to Delete）
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
                top = if (isRound) 42.dp else 16.dp,
                bottom = if (isRound) 68.dp else 26.dp,
                start = if (isRound) 14.dp else 8.dp,
                end = if (isRound) 14.dp else 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 頂部導航列：返回 + 標題 + 新增
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f),
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

            // 滑動手勢提示
            if (bookmarks.isNotEmpty() || offlineArticles.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.84f else 0.94f)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "向左滑動項目即可刪除",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 離線文章區塊
            if (offlineArticles.isNotEmpty()) {
                item {
                    ListSubheader(
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f)
                    ) {
                        Text(
                            text = "離線文章 (${offlineArticles.size})",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                items(
                    items = offlineArticles,
                    key = { it.url }
                ) { article ->
                    SwipeToDeleteItem(
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f),
                        onDelete = { onDeleteOfflineArticle(article.url) },
                        onClick = { onOpenOfflineArticle(article) }
                    ) {
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
                                    text = "約 ${article.readingTimeMinutes} 分鐘 · 點擊閱讀",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    }
                }
            }

            // 我的書籤區塊
            item {
                ListSubheader(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f)
                ) {
                    Text(
                        text = "我的書籤 (${bookmarks.size})",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (bookmarks.isEmpty() && offlineArticles.isEmpty()) {
                item {
                    Text(
                        text = "目前尚無書籤或離線文章\n瀏覽網頁時點擊選單即可收藏",
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
                        text = "尚無自訂網頁書籤\n點擊上方 + 號即可新增",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.80f else 0.92f)
                            .padding(vertical = 8.dp)
                    )
                }
            } else {
                items(
                    items = bookmarks,
                    key = { it.url }
                ) { bookmark ->
                    SwipeToDeleteItem(
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.84f else 0.94f),
                        onDelete = { onDeleteBookmark(bookmark.url) },
                        onClick = { onNavigateUrl(bookmark.url) }
                    ) {
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
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    }
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

/**
 * 專為 Wear OS 設計的「從右向左滑動刪除」組件 (Swipe Right-to-Left to Delete)
 * - 向左滑動時露出紅色刪除按鈕
 * - 超過臨界值時觸發觸覺震動，釋放時直接執行動畫並完成刪除
 * - 未過臨界值時彈簧平滑復原
 * - 點擊露出的紅色區域亦可直接刪除
 * - 正常點擊未滑動的卡片則執行原有點擊操作 (onClick)
 */
@Composable
fun SwipeToDeleteItem(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val offsetX = remember { Animatable(0f) }
    var itemWidthPx by remember { mutableFloatStateOf(300f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    // 觸發刪除的滑動門檻：約 64dp 或卡片寬度的 35%
    val thresholdPx = with(density) { 64.dp.toPx() }.coerceAtMost(itemWidthPx * 0.35f)
    val isPastThreshold = offsetX.value <= -thresholdPx

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .onSizeChanged {
                if (it.width > 0) {
                    itemWidthPx = it.width.toFloat()
                }
            }
    ) {
        // 背景層：向左滑出時露出的紅色刪除區域
        val bgColor = if (isPastThreshold) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
        val contentColor = if (isPastThreshold) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor)
                .clickable {
                    // 點擊露出的紅色背景亦可直接刪除
                    coroutineScope.launch {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        offsetX.animateTo(
                            targetValue = -itemWidthPx * 1.2f,
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                        )
                        onDelete()
                    }
                }
                .padding(end = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "刪除",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "刪除",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 前景層：滑動卡片本體
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hasTriggeredHaptic = false
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value <= -thresholdPx) {
                                    // 超過刪除門檻，播放移出動畫並觸發刪除
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    offsetX.animateTo(
                                        targetValue = -itemWidthPx * 1.2f,
                                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                    )
                                    onDelete()
                                } else {
                                    // 未達到門檻，平滑彈回原位
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            // 限制只能向左滑動 (負值)
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-itemWidthPx, 0f)
                            if (newOffset != offsetX.value) {
                                change.consume()
                                coroutineScope.launch {
                                    offsetX.snapTo(newOffset)
                                }
                                val past = newOffset <= -thresholdPx
                                if (past && !hasTriggeredHaptic) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    hasTriggeredHaptic = true
                                } else if (!past && hasTriggeredHaptic) {
                                    hasTriggeredHaptic = false
                                }
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
