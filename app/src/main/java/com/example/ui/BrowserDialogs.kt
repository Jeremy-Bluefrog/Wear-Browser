package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text

/**
 * 網址與搜尋輸入彈窗（深度採用 Wear OS Material 3，無貼邊）
 */
@Composable
fun UrlInputDialog(
    isRound: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    onVoiceClick: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberScalingLazyListState()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0000000))
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                autoCentering = AutoCenteringParams(itemIndex = 1),
                contentPadding = PaddingValues(
                    top = if (isRound) 44.dp else 18.dp,
                    bottom = if (isRound) 64.dp else 24.dp,
                    start = if (isRound) 16.dp else 8.dp,
                    end = if (isRound) 16.dp else 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "搜尋與網址列",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                    )
                }

                // 輸入框
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.82f else 0.94f)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (text.isNotBlank()) onSubmit(text)
                            }),
                            decorationBox = { innerTextField ->
                                if (text.isEmpty()) {
                                    Text(
                                        text = "輸入網址或關鍵字...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // 貼上剪貼簿按鈕
                item {
                    FilledTonalButton(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                text = clipText
                            }
                        },
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.74f else 0.86f),
                        label = { Text("貼上剪貼簿", style = MaterialTheme.typography.labelSmall) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }

                // 操作按鈕列 (語音 + 前往)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.80f else 0.92f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = onVoiceClick,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "語音",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (text.isNotBlank()) onSubmit(text)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = "前往", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // 取消按鈕
                item {
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.50f else 0.65f)
                            .height(28.dp)
                    ) {
                        Text(text = "取消", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            ScrollIndicator(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * 新增自訂書籤彈窗（深度採用 Wear OS Material 3，無貼邊）
 */
@Composable
fun AddBookmarkDialog(
    isRound: Boolean,
    onDismiss: () -> Unit,
    onAdd: (url: String, title: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val listState = rememberScalingLazyListState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0000000))
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                autoCentering = AutoCenteringParams(itemIndex = 1),
                contentPadding = PaddingValues(
                    top = if (isRound) 44.dp else 18.dp,
                    bottom = if (isRound) 64.dp else 24.dp,
                    start = if (isRound) 16.dp else 8.dp,
                    end = if (isRound) 16.dp else 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        text = "新增自訂書籤",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                    )
                }

                // 名稱欄位
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.82f else 0.94f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(
                                        text = "名稱 (例如: 我的網站)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // 網址欄位
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.82f else 0.94f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        BasicTextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (url.isEmpty()) {
                                    Text(
                                        text = "網址 (例如: example.com)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // 儲存與取消
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.80f else 0.92f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Text(text = "取消", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                if (url.isNotBlank()) {
                                    val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                                    onAdd(finalUrl, title.ifBlank { finalUrl })
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = "儲存", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            ScrollIndicator(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * 網頁縮放調整彈窗（深度採用 Wear OS Material 3，無貼邊）
 */
@Composable
fun ZoomDialog(
    isRound: Boolean,
    currentZoom: Int,
    onZoomChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0000000))
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                autoCentering = AutoCenteringParams(itemIndex = 1),
                contentPadding = PaddingValues(
                    top = if (isRound) 44.dp else 18.dp,
                    bottom = if (isRound) 64.dp else 24.dp,
                    start = if (isRound) 16.dp else 8.dp,
                    end = if (isRound) 16.dp else 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "網頁縮放調整",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(if (isRound) 0.80f else 0.92f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val newZoom = (currentZoom - 20).coerceAtLeast(60)
                                onZoomChange(newZoom)
                            },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "縮小",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        FilledTonalButton(
                            onClick = { onZoomChange(100) },
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "$currentZoom%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                val newZoom = (currentZoom + 20).coerceAtMost(200)
                                onZoomChange(newZoom)
                            },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "放大",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth(if (isRound) 0.55f else 0.70f)
                            .height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = "完成", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            ScrollIndicator(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
