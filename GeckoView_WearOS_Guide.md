# Wear OS 6/7 專用 GeckoView 瀏覽器整合指南

這份指南專為 **Wear OS (Android Wear)** 環境設計。由於手錶硬體資源高度受限（通常僅 1-2GB RAM）、螢幕為圓形，且許多 Wear OS 系統未內建或啟用了完整的系統 WebView，因此將 **Mozilla GeckoView** 網頁渲染引擎直接打包進 App 是實現穩定、獨立瀏覽網頁的最佳架構方案（如同 Samsung Internet 瀏覽器的做法）。

---

## 1. 專案級組態設定 (Gradle Setup)

由於 GeckoView 託管於 Mozilla 的官方 Maven 倉庫，且其體積較大，必須正確設定倉庫源與架構過濾（ABI Filter）。

### A. 調整 `settings.gradle.kts`
在 `dependencyResolutionManagement` 區段中加入 Mozilla Maven 倉庫：

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 加入 Mozilla 官方 Maven 倉庫
        maven {
            url = uri("https://maven.mozilla.org/maven2/")
        }
    }
}
```

### B. 調整 `app/build.gradle.kts`
GeckoView 支援多種 CPU 架構。手錶晶片通常為 `armeabi-v7a` 或 `arm64-v8a`（如高通 W5+ Gen 1 支援 64 位元，部分舊款為 32 位元）。
為了節省 Wear OS 極為珍貴的儲存空間與下載體積，**強烈建議在 `ndk` 中過濾架構**，避免將不必要的 `x86`、`x86_64` 驅動打包進 APK。

```kotlin
android {
    ...
    defaultConfig {
        ...
        // Wear OS 架構過濾，避免生成過大的 APK
        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a"))
        }
    }
}

dependencies {
    // 引入 GeckoView 穩定版 (可依據需求調整版本)
    // 註：geckoview-stable 將包含適合您所選架構的編譯檔案
    implementation("org.mozilla.geckoview:geckoview-stable:120.0.20231016142144")
}
```

---

## 2. 最基礎但完整的 GeckoView 實作

### 方案 A：Jetpack Compose 實作（現代 Wear OS 建議首選）
在 Jetpack Compose 中，我們使用 `AndroidView` 來橋接並渲染 `GeckoView`。

```kotlin
package com.example.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class GeckoBrowserActivity : ComponentActivity() {

    // GeckoRuntime 負責管理 Gecko 引擎生命週期，必須保持單一實例以節省資源
    private var geckoRuntime: GeckoRuntime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 在應用程式啟動時初始化單一 GeckoRuntime 實例
        geckoRuntime = GeckoRuntime.create(this)

        setContent {
            // 呼叫 Gecko 瀏覽器畫面，並直接加載 Google
            GeckoBrowserScreen(
                runtime = geckoRuntime!!,
                initialUrl = "https://google.com"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 釋放執行期資源
        geckoRuntime = null
    }
}

@Composable
fun GeckoBrowserScreen(runtime: GeckoRuntime, initialUrl: String) {
    // 2. 建立並記住 GeckoSession 實例（管理頁面狀態、瀏覽歷史等）
    val session = remember { GeckoSession() }

    // 3. 使用 DisposableEffect 管理 Session 生命週期與網頁加載
    DisposableEffect(session) {
        // 綁定 Runtime 到 Session
        session.open(runtime)
        // 載入指定的網址
        session.loadUri(initialUrl)

        onDispose {
            // 當 Composable 銷毀時關閉 Session
            session.close()
        }
    }

    // 4. 將 GeckoView 橋接至 Jetpack Compose 畫面中
    AndroidView(
        factory = { context ->
            GeckoView(context).apply {
                // 將 GeckoSession 綁定到該視圖（GeckoView）進行渲染
                setSession(session)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

---

### 方案 B：傳統 XML 佈局實作 (Traditional View XML)
如果您更傾向使用傳統 XML View，可以按照以下方式實作：

#### 佈局檔案 `res/layout/activity_gecko.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/black">

    <!-- 宣告 GeckoView 視圖 -->
    <org.mozilla.geckoview.GeckoView
        android:id="@+id/geckoview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</FrameLayout>
```

#### Activity 類別檔案 `GeckoXmlActivity.kt`
```kotlin
package com.example.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.R
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class GeckoXmlActivity : AppCompatActivity() {

    private lateinit var geckoView: GeckoView
    private lateinit var geckoSession: GeckoSession
    private var geckoRuntime: GeckoRuntime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gecko)

        // 1. 初始化視圖
        geckoView = findViewById(R.id.geckoview)

        // 2. 建立一個瀏覽工作階段
        geckoSession = GeckoSession()

        // 3. 取得或建立單一運作時實例 (GeckoRuntime)
        geckoRuntime = GeckoRuntime.create(this)

        // 4. 開啟工作階段並載入 Google 網頁
        geckoSession.open(geckoRuntime!!)
        geckoView.setSession(geckoSession)
        
        geckoSession.loadUri("https://google.com")
    }

    override fun onDestroy() {
        super.onDestroy()
        // 5. 確保銷毀時安全關閉工作階段並釋放 runtime 資源
        geckoSession.close()
        geckoRuntime = null
    }
}
```

---

## 3. Wear OS 6/7 特殊最佳化秘笈（資深架構師建議）

手錶平台和手機有本質上的差異，以下是整合 GeckoView 時的頂級架構建議：

### 📌 記憶體嚴格控管 (1-2GB RAM Constraints)
1. **單一 Runtime 實例 (Singleton Pattern)**：`GeckoRuntime` 非常龐大，**絕對不能在多個 Activity 內重複建立**。建議使用 `Application` 類別或單例模式來持有全域唯一的 `GeckoRuntime` 實例，在整個 App 生命週期中重複利用。
2. **手動垃圾回收與快取清理**：在 `onTrimMemory` 被觸發時，呼叫 `geckoRuntime.clearCache()` 或是調降 Gecko 的內部執行優先級，防止因記憶體不足 (OOM) 被 Android 系統強行中止。
3. **分頁限制 (Session Limit)**：在 Wear OS 上建議強行限制分頁數量為 1 至 2 個，不要開啟多 Tab 模式。

### 📌 圓形螢幕適配 (Round Screen Insets)
* **安全區域 (Circular Safe Margin)**：圓形螢幕會裁切掉四個角落。載入網頁時，建議在 GeckoView 的外部佈局設定適當的邊距，或者注入自訂 CSS（利用 `session.runtime.settings` 或 `GeckoSession.NavigationDelegate` 執行 JavaScript）來增加 `padding-left` 與 `padding-right`，確保 Google 的搜尋欄、導覽列不會被手錶外圈裁切。

### 📌 物理旋轉錶冠支援 (Rotary Input Support)
* 許多 Wear OS 手錶（例如 Galaxy Watch、Pixel Watch）擁有實體或虛擬旋轉錶冠。為了提供完美的滾動體驗，您需要捕捉手錶的旋轉事件（Rotary Events）並將其轉換成網頁的 `scrollBy` 操作。

```kotlin
// 在 AndroidView 中捕捉旋轉事件並轉換為 GeckoSession 滾動
geckoView.setOnGenericMotionListener { _, event ->
    if (event.action == android.view.MotionEvent.ACTION_SCROLL && 
        event.isFromSource(android.view.InputDevice.SOURCE_ROTARY_ENCODER)) {
        
        // 獲取旋轉的增量
        val axisValue = event.getAxisValue(android.view.MotionEvent.AXIS_SCROLL)
        // 將滾動事件傳遞至 GeckoSession
        val scrollAmount = (axisValue * 150).toInt() // 依手感微調係數
        geckoSession.purgeHistory() // 視需要優化歷史快取
        // 透過 JavaScript 或 Scroll API 進行頁面捲動
        true
    } else {
        false
    }
}
```

---

### 📌 無系統 WebView 的系統支援
使用 GeckoView 的最大優勢，就是不論手錶底層使用的是不是純淨版 AOSP（如許多廉價或特製智慧手錶未包含 Google Mobile Services 與 Android System WebView），您的瀏覽器都擁有完全掌控的、穩定的 **Gecko (Firefox)** 渲染核心。
這能確保您的網頁在所有手錶上的相容性與渲染效果完全一致！
