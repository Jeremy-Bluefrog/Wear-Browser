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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubheader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text

/**
 * 深度採用 Wear OS Material 3 規範之瀏覽器設定畫面
 */
@Composable
fun SettingsScreen(
    isRound: Boolean,
    searchEngine: String,
    textZoom: Int,
    isPureBlackMode: Boolean,
    isAdBlockEnabled: Boolean,
    isWristGesturesEnabled: Boolean = true,
    rotarySpeed: Float = 2.0f,
    isIncognitoMode: Boolean = false,
    onSearchEngineChange: (String) -> Unit,
    onTextZoomChange: (Int) -> Unit,
    onTogglePureBlack: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onToggleWristGestures: () -> Unit,
    onRotarySpeedChange: (Float) -> Unit,
    onToggleIncognito: () -> Unit,
    onClearData: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    val enginesRow1 = listOf("Google", "Bing")
    val enginesRow2 = listOf("DuckDuckGo", "百度")
    val zoomPresets = listOf(80, 100, 120, 150)
    val rotaryPresets = listOf(1.2f to "1.2x 精細", 2.0f to "2.0x 標準", 3.2f to "3.2x 極速")

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
            // Material 3 標題列
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    verticalAlignment = Alignment.CenterVertically
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "偏好設定",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Material 3 ListSubheader: 預設搜尋引擎
            item {
                ListSubheader(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                ) {
                    Text(text = "預設搜尋引擎", color = MaterialTheme.colorScheme.secondary)
                }
            }

            // 搜尋引擎選擇 Row 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    enginesRow1.forEach { engine ->
                        val selected = engine == searchEngine
                        if (selected) {
                            Button(
                                onClick = { onSearchEngineChange(engine) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(text = engine, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { onSearchEngineChange(engine) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text(text = engine, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // 搜尋引擎選擇 Row 2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    enginesRow2.forEach { engine ->
                        val selected = engine == searchEngine
                        if (selected) {
                            Button(
                                onClick = { onSearchEngineChange(engine) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(text = engine, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { onSearchEngineChange(engine) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text(text = engine, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Material 3 ListSubheader: 網頁字體縮放
            item {
                ListSubheader(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                ) {
                    Text(text = "網頁字體縮放", color = MaterialTheme.colorScheme.secondary)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val newZoom = (textZoom - 20).coerceAtLeast(60)
                            onTextZoomChange(newZoom)
                        },
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "縮小",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = { onTextZoomChange(100) },
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "$textZoom%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            val newZoom = (textZoom + 20).coerceAtMost(200)
                            onTextZoomChange(newZoom)
                        },
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "放大",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    zoomPresets.forEach { z ->
                        val selected = z == textZoom
                        if (selected) {
                            Button(
                                onClick = { onTextZoomChange(z) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(text = "$z%", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { onTextZoomChange(z) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                            ) {
                                Text(text = "$z%", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Material 3 ListSubheader: 錶冠滾動靈敏度
            item {
                ListSubheader(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                ) {
                    Text(text = "實體錶冠滾動速度", color = MaterialTheme.colorScheme.secondary)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rotaryPresets.forEach { (speed, label) ->
                        val selected = kotlin.math.abs(rotarySpeed - speed) < 0.1f
                        if (selected) {
                            Button(
                                onClick = { onRotarySpeedChange(speed) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text(text = label, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { onRotarySpeedChange(speed) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                            ) {
                                Text(text = label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Material 3 ListSubheader: 隱私與手勢開關
            item {
                ListSubheader(
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f)
                ) {
                    Text(text = "隱私與系統開關", color = MaterialTheme.colorScheme.secondary)
                }
            }

            // 無痕隱私瀏覽模式
            item {
                SwitchButton(
                    checked = isIncognitoMode,
                    onCheckedChange = { onToggleIncognito() },
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    label = { Text("無痕隱私瀏覽") },
                    secondaryLabel = { Text("不記錄瀏覽與搜尋歷史") }
                )
            }

            // OLED 純黑深色模式
            item {
                SwitchButton(
                    checked = isPureBlackMode,
                    onCheckedChange = { onTogglePureBlack() },
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    label = { Text("OLED 純黑省電") },
                    secondaryLabel = { Text("網頁注入純黑背景延長續航") }
                )
            }

            // 廣告與追蹤攔截
            item {
                SwitchButton(
                    checked = isAdBlockEnabled,
                    onCheckedChange = { onToggleAdBlock() },
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    label = { Text("廣告與追蹤攔截") },
                    secondaryLabel = { Text("過濾浮動廣告加速載入") }
                )
            }

            // 手腕手勢控制
            item {
                SwitchButton(
                    checked = isWristGesturesEnabled,
                    onCheckedChange = { onToggleWristGestures() },
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.82f else 0.94f),
                    label = { Text("手腕手勢控制") },
                    secondaryLabel = { Text("轉腕返回 · 輕叩重新整理") }
                )
            }

            // 清除歷史與快取 (採用 Material 3 警示按鈕配色)
            item {
                Button(
                    onClick = onClearData,
                    modifier = Modifier.fillMaxWidth(if (isRound) 0.80f else 0.92f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "清除快取與紀錄", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // 底部版本說明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(if (isRound) 0.82f else 0.94f)
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pixel Browser (GeckoView)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "遵循 Wear OS Material 3 規範設計",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Material 3 ScrollIndicator
        ScrollIndicator(
            state = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}
