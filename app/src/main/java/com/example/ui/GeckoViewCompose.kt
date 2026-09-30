package com.example.ui

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

/**
 * GeckoView 運行狀態管理物件 (GeckoState)
 * 封裝 GeckoSession 與 GeckoRuntime 的狀態與操作，提供 Compose 響應式狀態支援。
 */
@Stable
class GeckoState(
    val runtime: GeckoRuntime,
    val session: GeckoSession,
    initialUrl: String = ""
) {
    var currentUrl by mutableStateOf(initialUrl)
        internal set

    var pageTitle by mutableStateOf("")
        internal set

    var isLoading by mutableStateOf(false)
        internal set

    var progress by mutableIntStateOf(0)
        internal set

    var canGoBack by mutableStateOf(false)
        internal set

    var canGoForward by mutableStateOf(false)
        internal set

    var isSecure by mutableStateOf(false)
        internal set

    private var geckoViewRef: GeckoView? = null

    internal fun bindView(view: GeckoView) {
        geckoViewRef = view
    }

    internal fun unbindView() {
        geckoViewRef = null
    }

    /**
     * 載入指定網址
     */
    fun loadUrl(url: String) {
        if (url.isNotBlank() && url != currentUrl) {
            currentUrl = url
            session.loadUri(url)
        }
    }

    /**
     * 重新整理目前網頁
     */
    fun reload() {
        session.reload()
    }

    /**
     * 返回上一頁
     */
    fun goBack() {
        if (canGoBack) {
            session.goBack()
        }
    }

    /**
     * 前往下一頁
     */
    fun goForward() {
        if (canGoForward) {
            session.goForward()
        }
    }

    /**
     * 執行輕量前端腳本 (DOM 操作與樣式注入)
     */
    fun evaluateJavascript(script: String) {
        try {
            session.loadUri("javascript:(function(){ try { $script } catch(e){} })()")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 即時應用 OLED 純黑深色樣式
     */
    fun applyDarkMode(enable: Boolean) {
        if (enable) {
            val darkCss = """
                var s = document.getElementById('gecko-pure-black-style');
                if (!s) {
                    s = document.createElement('style');
                    s.id = 'gecko-pure-black-style';
                    s.innerHTML = 'html, body { background-color: #000000 !important; color: #E4E4E7 !important; } * { border-color: #27272A !important; } p, span, h1, h2, h3, h4, h5, h6, li, a { color: #E4E4E7 !important; } a { color: #60A5FA !important; } img, video { filter: brightness(0.85); }';
                    document.head.appendChild(s);
                }
            """.trimIndent()
            evaluateJavascript(darkCss)
        } else {
            val removeCss = """
                var s = document.getElementById('gecko-pure-black-style');
                if (s) { s.remove(); }
            """.trimIndent()
            evaluateJavascript(removeCss)
        }
    }

    /**
     * 即時應用網頁縮放 (字體與排版比例)
     */
    fun applyTextZoom(zoom: Int) {
        val scale = (zoom / 100f).coerceIn(0.5f, 2.5f)
        evaluateJavascript("document.body.style.zoom = '$scale';")
    }

    /**
     * 即時應用廣告過濾隱藏樣式
     */
    fun applyAdBlock(enable: Boolean) {
        if (enable) {
            val adCss = """
                var a = document.getElementById('gecko-adblock-style');
                if (!a) {
                    a = document.createElement('style');
                    a.id = 'gecko-adblock-style';
                    a.innerHTML = '[class*="ad-"], [id*="ad-"], [class*="adsbygoogle"], iframe[src*="doubleclick"], .adsbox { display: none !important; }';
                    document.head.appendChild(a);
                }
            """.trimIndent()
            evaluateJavascript(adCss)
        }
    }

    /**
     * 提取當前頁面核心文章文字供閱讀模式使用
     */
    fun extractReaderContent() {
        val extractorJs = """
            var t = document.title || '';
            var el = document.querySelector('article') || document.querySelector('main') || document.body;
            var text = '';
            if (el) {
                var paras = Array.from(el.querySelectorAll('p, h1, h2, h3, blockquote'));
                if (paras.length > 0) {
                    text = paras.map(p => p.innerText.trim()).filter(s => s.length > 0).join('\n\n');
                } else {
                    text = el.innerText || '';
                }
            }
            text = text.substring(0, 12000);
            location.href = 'pixelreader://data?title=' + encodeURIComponent(t) + '&body=' + encodeURIComponent(text);
        """.trimIndent()
        evaluateJavascript(extractorJs)
    }

    /**
     * 滾動視圖 (適用於 Wear OS 錶冠或自訂手勢)
     */
    fun scrollBy(deltaX: Float, deltaY: Float) {
        geckoViewRef?.let { view ->
            val now = SystemClock.uptimeMillis()
            val event = MotionEvent.obtain(
                now, now, MotionEvent.ACTION_SCROLL,
                deltaX, deltaY, 0
            )
            view.dispatchGenericMotionEvent(event)
            event.recycle()
        }
    }

    /**
     * 釋放 Session 資源
     */
    fun destroy() {
        try {
            session.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 在 Compose 中記憶並初始化 GeckoRuntime 與 GeckoSession 的狀態工廠函式
 */
@Composable
fun rememberGeckoState(
    initialUrl: String = "https://www.google.com",
    useTrackingProtection: Boolean = true
): GeckoState {
    val context = LocalContext.current.applicationContext

    val state = remember {
        val runtime = GeckoRuntime.getDefault(context)
        val sessionSettings = GeckoSessionSettings.Builder()
            .useTrackingProtection(useTrackingProtection)
            .build()
        val session = GeckoSession(sessionSettings)
        session.open(runtime)

        if (initialUrl.isNotBlank()) {
            session.loadUri(initialUrl)
        }

        GeckoState(runtime, session, initialUrl)
    }

    DisposableEffect(state) {
        onDispose {
            state.destroy()
        }
    }

    return state
}

/**
 * 專為 Jetpack Compose 設計的 GeckoView AndroidView Wrapper 元件。
 */
@Composable
fun GeckoViewWrapper(
    state: GeckoState,
    modifier: Modifier = Modifier,
    onPageStarted: ((url: String) -> Unit)? = null,
    onPageFinished: ((url: String, title: String) -> Unit)? = null,
    onProgressChanged: ((progress: Int) -> Unit)? = null,
    onNavigationStateChanged: ((canGoBack: Boolean, canGoForward: Boolean) -> Unit)? = null,
    onTitleChanged: ((title: String) -> Unit)? = null,
    onReaderDataExtracted: ((title: String, body: String) -> Unit)? = null,
    onCrash: (() -> Unit)? = null,
    onViewCreated: ((GeckoView) -> Unit)? = null
) {
    val session = state.session

    val currentOnPageStarted by rememberUpdatedState(onPageStarted)
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    val currentOnProgressChanged by rememberUpdatedState(onProgressChanged)
    val currentOnNavigationStateChanged by rememberUpdatedState(onNavigationStateChanged)
    val currentOnTitleChanged by rememberUpdatedState(onTitleChanged)
    val currentOnReaderDataExtracted by rememberUpdatedState(onReaderDataExtracted)
    val currentOnCrash by rememberUpdatedState(onCrash)
    val currentOnViewCreated by rememberUpdatedState(onViewCreated)

    // 綁定 GeckoSession 各項委派監聽器
    DisposableEffect(session) {
        val progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                if (url.startsWith("pixelreader://")) return
                state.isLoading = true
                state.progress = 10
                state.currentUrl = url
                currentOnPageStarted?.invoke(url)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                state.isLoading = false
                state.progress = 100
                currentOnPageFinished?.invoke(state.currentUrl, state.pageTitle)
            }

            override fun onProgressChange(session: GeckoSession, progressVal: Int) {
                state.progress = progressVal
                state.isLoading = progressVal < 100
                currentOnProgressChanged?.invoke(progressVal)
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {
                state.isSecure = securityInfo.isSecure
            }
        }

        val navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>
            ) {
                url?.let { targetUrl ->
                    if (targetUrl.startsWith("pixelreader://data")) {
                        try {
                            val parsed = Uri.parse(targetUrl)
                            val title = parsed.getQueryParameter("title") ?: state.pageTitle
                            val body = parsed.getQueryParameter("body") ?: ""
                            currentOnReaderDataExtracted?.invoke(title, body)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return
                    }
                    state.currentUrl = targetUrl
                    currentOnPageFinished?.invoke(targetUrl, state.pageTitle)
                }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                state.canGoBack = canGoBack
                currentOnNavigationStateChanged?.invoke(canGoBack, state.canGoForward)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                state.canGoForward = canGoForward
                currentOnNavigationStateChanged?.invoke(state.canGoBack, canGoForward)
            }
        }

        val contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                title?.let {
                    state.pageTitle = it
                    currentOnTitleChanged?.invoke(it)
                }
            }

            override fun onCrash(session: GeckoSession) {
                currentOnCrash?.invoke()
            }
        }

        session.progressDelegate = progressDelegate
        session.navigationDelegate = navigationDelegate
        session.contentDelegate = contentDelegate

        onDispose {
            session.progressDelegate = null
            session.navigationDelegate = null
            session.contentDelegate = null
        }
    }

    // AndroidView 封裝原生 GeckoView，妥善處理生命週期與釋放
    AndroidView(
        factory = { context ->
            GeckoView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setSession(session)
                state.bindView(this)
                currentOnViewCreated?.invoke(this)
            }
        },
        update = { view ->
            state.bindView(view)
        },
        onRelease = { view ->
            state.unbindView()
            try {
                view.releaseSession()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        modifier = modifier
    )
}
