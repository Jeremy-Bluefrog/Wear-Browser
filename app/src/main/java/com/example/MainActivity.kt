package com.example

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BrowserScreen
import com.example.ui.BrowserViewModel
import com.example.ui.theme.WearAppTheme
import com.example.wear.WearOsCompatLayer

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Wear OS API compatibility layer
    WearOsCompatLayer.initialize()

    enableEdgeToEdge()
    setContent {
      WearAppTheme {
        val viewModel: BrowserViewModel = viewModel()
        BrowserScreen(viewModel = viewModel)
      }
    }
  }

  @Suppress("DEPRECATION")
  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
      System.gc()
    }
  }
}
