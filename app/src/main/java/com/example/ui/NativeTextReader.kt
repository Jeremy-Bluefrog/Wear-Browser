package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@OptIn(androidx.wear.compose.material3.ExperimentalWearMaterial3Api::class)
@Composable
fun NativeTextReader(url: String, isDeepMode: Boolean, isSerifFont: Boolean) {
    var textParagraphs by remember(url) { mutableStateOf<List<String>>(emptyList()) }
    var title by remember(url) { mutableStateOf<String?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var errorMessage by remember(url) { mutableStateOf<String?>(null) }

    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val isRound = LocalConfiguration.current.isScreenRound

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(url) {
        if (url.isBlank() || url.startsWith("pixelbrowser://")) {
            isLoading = false
            return@LaunchedEffect
        }
        
        isLoading = true
        errorMessage = null
        try {
            withContext(Dispatchers.IO) {
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Linux; Android Wear) AppleWebKit/537.36")
                    .timeout(10000)
                    .get()
                
                title = doc.title()
                
                val article = doc.select("article, main, .content, .post, .article, p")
                val paragraphs = mutableListOf<String>()
                
                if (article.isNotEmpty()) {
                    for (element in article) {
                        if (element.tagName() == "p") {
                            val pText = element.text().trim()
                            if (pText.isNotBlank()) paragraphs.add(pText)
                        } else {
                            val pList = element.select("p")
                            for (p in pList) {
                                val pText = p.text().trim()
                                if (pText.isNotBlank()) paragraphs.add(pText)
                            }
                        }
                    }
                }
                
                if (paragraphs.isEmpty()) {
                    val bodyText = doc.body().text().trim()
                    if (bodyText.isNotBlank()) {
                        paragraphs.addAll(bodyText.split("\n\n").filter { it.isNotBlank() })
                    }
                }
                
                textParagraphs = paragraphs
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "載入失敗"
        } finally {
            isLoading = false
        }
    }

    val scalingParams = remember {
        ScalingLazyColumnDefaults.scalingParams(
            edgeScale = 0.82f,
            edgeAlpha = 0.65f,
            minTransitionArea = 0.25f,
            maxTransitionArea = 0.75f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDeepMode) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "正在排版最佳閱讀內容...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (errorMessage != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = if (isRound) 24.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "錯誤",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "無法載入此頁面",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            ScreenScaffold(
                scrollState = listState
            ) {
                ScalingLazyColumn(
                    state = listState,
                    scalingParams = scalingParams,
                    flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState),
                    contentPadding = PaddingValues(
                        top = if (isRound) 32.dp else 16.dp,
                        bottom = if (isRound) 44.dp else 24.dp,
                        start = if (isRound) 16.dp else 12.dp,
                        end = if (isRound) 16.dp else 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .rotaryScrollable(
                            behavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                            focusRequester = focusRequester
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!title.isNullOrBlank()) {
                        item {
                            ListHeader {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "原生閱讀模式",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        item {
                            Text(
                                text = title!!,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDeepMode) Color.White else MaterialTheme.colorScheme.onBackground,
                                textAlign = if (isRound) TextAlign.Center else TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }

                    if (textParagraphs.isEmpty()) {
                        item {
                            Text(
                                text = "此網頁無足夠的內文可供提取。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(textParagraphs) { p ->
                            Text(
                                text = p,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                fontFamily = if (isSerifFont) FontFamily.Serif else FontFamily.Default,
                                color = if (isDeepMode) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onBackground,
                                textAlign = if (isRound) TextAlign.Start else TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
