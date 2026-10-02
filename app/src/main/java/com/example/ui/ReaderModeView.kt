package com.example.ui

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import java.util.Locale

/**
 * 深度採用 Wear OS Material 3 規範之極簡閱讀模式 (Reader Mode)
 * 整合語音朗讀 (Text-to-Speech) 與離線儲存功能。
 */
@Composable
fun ReaderModeView(
    isRound: Boolean,
    title: String,
    content: String,
    onSaveOffline: (() -> Unit)? = null,
    onExitReaderMode: () -> Unit
) {
    val context = LocalContext.current
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

    // TextToSpeech 語音朗讀管理
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ttsInstance = tts
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
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
                bottom = if (isRound) 76.dp else 32.dp,
                start = if (isRound) 18.dp else 10.dp,
                end = if (isRound) 18.dp else 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Material 3 標題與預估閱讀時間
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
                        .fillMaxWidth(if (isRound) 0.84f else 0.94f)
                        .padding(bottom = 2.dp)
                )
            }

            // 快捷功能按鈕列 (TTS 朗讀 + 儲存離線)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TTS 語音朗讀切換
                    FilledTonalButton(
                        onClick = {
                            val tts = ttsInstance
                            if (tts == null) {
                                Toast.makeText(context, "語音引擎準備中...", Toast.LENGTH_SHORT).show()
                                return@FilledTonalButton
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isSpeaking) {
                                tts.stop()
                                isSpeaking = false
                            } else {
                                val fullText = "$title. " + paragraphs.joinToString(". ")
                                tts.speak(fullText, TextToSpeech.QUEUE_FLUSH, null, "reader_mode_tts")
                                isSpeaking = true
                                Toast.makeText(context, "開始語音朗讀", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = if (isSpeaking) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        label = {
                            Text(
                                text = if (isSpeaking) "暫停朗讀" else "語音朗讀",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    )

                    // 儲存離線按鈕
                    if (onSaveOffline != null) {
                        FilledTonalIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSaveOffline()
                            },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "儲存離線文章",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
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
                    onClick = {
                        ttsInstance?.stop()
                        isSpeaking = false
                        onExitReaderMode()
                    },
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
