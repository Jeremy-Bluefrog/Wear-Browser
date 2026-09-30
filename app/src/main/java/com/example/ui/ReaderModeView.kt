package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text

/**
 * 深度採用 Wear OS Material 3 規範之極簡閱讀模式 (Reader Mode)
 */
@Composable
fun ReaderModeView(
    isRound: Boolean,
    title: String,
    content: String,
    onExitReaderMode: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    val paragraphs = remember(content) {
        content.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    val readingTimeMin = remember(content) {
        val wordCount = content.length
        (wordCount / 300).coerceAtLeast(1)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                bottom = if (isRound) 72.dp else 28.dp,
                start = if (isRound) 18.dp else 10.dp,
                end = if (isRound) 18.dp else 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Material 3 ListHeader 標頭
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "極簡閱讀 · 約 $readingTimeMin 分鐘",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 文章標題
            item {
                Text(
                    text = title.ifBlank { "文章閱讀" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(if (isRound) 0.82f else 0.94f)
                        .padding(bottom = 4.dp)
                )
            }

            // 文章段落內容
            if (paragraphs.isEmpty()) {
                item {
                    Text(
                        text = "未能提取正文或內容過短，請切換回一般網頁模式瀏覽。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.80f else 0.92f)
                    )
                }
            } else {
                items(paragraphs) { p ->
                    Text(
                        text = p,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 17.sp,
                        color = Color(0xFFE4E4E7),
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                    )
                }
            }

            // 底部結束按鈕
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                FilledTonalButton(
                    onClick = onExitReaderMode,
                    modifier = Modifier
                        .fillMaxWidth(if (isRound) 0.65f else 0.80f)
                        .height(34.dp),
                    label = { Text("退出閱讀模式", style = MaterialTheme.typography.labelSmall) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }

        ScrollIndicator(
            state = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}
