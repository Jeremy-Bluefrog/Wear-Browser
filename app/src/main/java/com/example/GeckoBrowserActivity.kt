package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import kotlin.math.roundToInt

/**
 * 專為 Wear OS 圓形手錶設計的 GeckoView 下拉重新整理 (Pull-to-Refresh) 瀏覽器。
 *
 * 功能特色：
 * 1. 頂部網頁滾動邊界偵測 (scrollY <= 0)。
 * 2. 圓形螢幕專屬的視覺下拉重新整理動畫指示器 (Pull-to-Refresh Indicator)。
 * 3. 動態弧形進度條 (Curved Progress Arc) 與旋轉刷新圖示。
 * 4. 完整的 GeckoSession 載入狀態與手勢阻尼懸停動畫。
 */
class GeckoBrowserActivity : ComponentActivity() {

    private lateinit var geckoRuntime: GeckoRuntime
    private lateinit var geckoSession: GeckoSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        geckoRuntime = GeckoRuntime.getDefault(this)
        geckoSession = GeckoSession()
        geckoSession.open(geckoRuntime)
        geckoSession.loadUri("https://google.com")

        setContent {
            MaterialTheme {
                GeckoBrowserScreen(session = geckoSession)
            }
        }
    }

    override fun onDestroy() {
        geckoSession.close()
        super.onDestroy()
    }
}

/**
 * 主瀏覽器畫面：整合 GeckoView、下拉手勢偵測、圓形螢幕指示器與頂部導覽列。
 */
@Composable
fun GeckoBrowserScreen(session: GeckoSession) {
    var isAtTop by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf("https://google.com") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(0f) }
    val refreshThresholdPx = 160f // 下拉觸發重新整理的臨界像素值
    val indicatorMaxOffsetY = 120f // 指示器最大下拉偏移量

    // 綁定 GeckoSession 監聽器
    DisposableEffect(session) {
        val scrollDelegate = object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                // 當網頁滾動至最頂端 (scrollY <= 0) 時允許觸發下拉手勢
                isAtTop = scrollY <= 0
            }
        }

        val progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                isRefreshing = true
                loadingProgress = 10
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                isRefreshing = false
                loadingProgress = 100
                coroutineScope.launch {
                    pullOffset.animateTo(0f, animationSpec = spring(stiffness = 300f))
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                loadingProgress = progress
                if (progress >= 100) {
                    isRefreshing = false
                    coroutineScope.launch {
                        pullOffset.animateTo(0f, animationSpec = spring(stiffness = 300f))
                    }
                }
            }
        }

        val navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>
            ) {
                url?.let { currentUrl = it }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBackValue: Boolean) {
                canGoBack = canGoBackValue
            }

            override fun onCanGoForward(session: GeckoSession, canGoForwardValue: Boolean) {
                canGoForward = canGoForwardValue
            }
        }

        session.scrollDelegate = scrollDelegate
        session.progressDelegate = progressDelegate
        session.navigationDelegate = navigationDelegate

        onDispose {
            session.scrollDelegate = null
            session.progressDelegate = null
            session.navigationDelegate = null
        }
    }

    // 計算下拉進度比率 (0.0f ~ 1.0f)
    val pullFraction = (pullOffset.value / refreshThresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isAtTop, isRefreshing) {
                if (isRefreshing) return@pointerInput

                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        // 僅在網頁位於最頂端且往下拖曳時處理下拉手勢
                        if (isAtTop && (dragAmount > 0 || pullOffset.value > 0)) {
                            change.consume()
                            coroutineScope.launch {
                                // 加入帶有彈力阻尼感的滑動比率
                                val dampening = (1f - (pullOffset.value / (indicatorMaxOffsetY * 2f))).coerceIn(0.2f, 1f)
                                val newOffset = (pullOffset.value + dragAmount * dampening).coerceIn(0f, indicatorMaxOffsetY * 1.5f)
                                pullOffset.snapTo(newOffset)
                            }
                        }
                    },
                    onDragEnd = {
                        if (pullOffset.value >= refreshThresholdPx) {
                            // 觸發重新整理
                            isRefreshing = true
                            session.reload()
                            coroutineScope.launch {
                                pullOffset.animateTo(indicatorMaxOffsetY, animationSpec = spring(stiffness = 400f))
                            }
                        } else {
                            // 未達臨界值，復原回頂部
                            coroutineScope.launch {
                                pullOffset.animateTo(0f, animationSpec = spring(stiffness = 500f))
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            pullOffset.animateTo(0f, animationSpec = spring(stiffness = 500f))
                        }
                    }
                )
            }
    ) {
        // 1. 底層 GeckoView 網頁渲染元件 (隨下拉滑動輕微位移)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (pullOffset.value * 0.4f).roundToInt()) }
        ) {
            GeckoViewContainer(session = session)
        }

        // 2. 圓形頂部邊緣的弧形進度條 (Curved Top Edge Refresh Arc)
        TopEdgeArcIndicator(
            pullFraction = pullFraction,
            isRefreshing = isRefreshing,
            progress = loadingProgress
        )

        // 3. 下拉重新整理視覺指示器懸浮卡片 (Pull-to-Refresh Visual Indicator)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, pullOffset.value.roundToInt() - 30) }
        ) {
            PullToRefreshBadge(
                pullFraction = pullFraction,
                isRefreshing = isRefreshing,
                progress = loadingProgress,
                onManualRefresh = {
                    isRefreshing = true
                    session.reload()
                    coroutineScope.launch {
                        pullOffset.animateTo(indicatorMaxOffsetY, animationSpec = spring())
                    }
                }
            )
        }

        // 4. Wear OS 簡潔懸浮快速導覽工具列 (浮動在底部圓形邊緣)
        MiniNavigationBar(
            currentUrl = currentUrl,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            isRefreshing = isRefreshing,
            onBack = { session.goBack() },
            onForward = { session.goForward() },
            onRefresh = {
                isRefreshing = true
                session.reload()
                coroutineScope.launch {
                    pullOffset.animateTo(indicatorMaxOffsetY, animationSpec = spring())
                }
            },
            onHome = { session.loadUri("https://google.com") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

/**
 * 圓形手錶頂部邊緣弧形進度條 (Wear OS Ambient Curved Progress Arc)
 */
@Composable
fun TopEdgeArcIndicator(
    pullFraction: Float,
    isRefreshing: Boolean,
    progress: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arcRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arcRotationAngle"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
    ) {
        val strokeWidth = 8.dp.toPx()
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

        if (isRefreshing) {
            // 重新整理中：繪製旋轉發光弧線
            val sweepAngle = (progress.toFloat() / 100f * 280f).coerceAtLeast(40f)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF38BDF8),
                        Color(0xFF818CF8),
                        Color(0xFFC084FC),
                        Color(0xFF38BDF8)
                    )
                ),
                startAngle = rotationAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        } else if (pullFraction > 0f) {
            // 下拉過程中：根據下拉比例繪製頂部收斂弧線 (-135度至 -45度)
            val maxSweep = 100f
            val sweep = pullFraction * maxSweep
            val startAngle = -90f - (sweep / 2f)

            val arcColor = if (pullFraction >= 1f) Color(0xFF4ADE80) else Color(0xFF38BDF8)

            drawArc(
                color = arcColor,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * 下拉重新整理視覺指示卡片 (Visual Pull-to-Refresh Badge)
 */
@Composable
fun PullToRefreshBadge(
    pullFraction: Float,
    isRefreshing: Boolean,
    progress: Int,
    onManualRefresh: () -> Unit
) {
    val isReadyToRelease = pullFraction >= 1.0f

    val arrowRotation by animateFloatAsState(
        targetValue = if (isReadyToRelease) 180f else pullFraction * 180f,
        animationSpec = spring(stiffness = 400f),
        label = "arrowRotation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "spinAnimation")
    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "spinRotation"
    )

    AnimatedVisibility(
        visible = pullFraction > 0.05f || isRefreshing,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (isRefreshing) {
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            } else if (isReadyToRelease) {
                                listOf(Color(0xFF065F46), Color(0xFF047857))
                            } else {
                                listOf(Color(0xFF1E293B), Color(0xFF334155))
                            }
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // 圖示：重新整理中顯示轉圈，下拉中顯示動態箭頭
                if (isRefreshing) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "載入中",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(spinRotation)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "更新中 ${progress}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = if (isReadyToRelease) Icons.Default.Refresh else Icons.Default.ArrowDownward,
                        contentDescription = "下拉指示",
                        tint = if (isReadyToRelease) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isReadyToRelease) 0f else arrowRotation)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isReadyToRelease) "放開立即重新整理" else "下拉重新整理",
                        color = if (isReadyToRelease) Color(0xFF4ADE80) else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 底部懸浮導覽按鈕列 (適合 Wear OS 手錶觸控)
 */
@Composable
fun MiniNavigationBar(
    currentUrl: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isRefreshing: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xCC0F172A))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onBack,
            enabled = canGoBack,
            modifier = Modifier.size(28.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (canGoBack) Color.White else Color(0xFF64748B)
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "上一頁",
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onHome,
            modifier = Modifier.size(28.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color(0xFF38BDF8)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "首頁",
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(28.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isRefreshing) Color(0xFF4ADE80) else Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "重新整理",
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onForward,
            enabled = canGoForward,
            modifier = Modifier.size(28.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (canGoForward) Color.White else Color(0xFF64748B)
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "下一頁",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 宿主 GeckoView 的 Compose 容器。
 */
@Composable
fun GeckoViewContainer(session: GeckoSession, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val geckoView = remember {
        GeckoView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setSession(session)
        }
    }

    AndroidView(
        factory = { geckoView },
        modifier = modifier.fillMaxSize()
    )
}
