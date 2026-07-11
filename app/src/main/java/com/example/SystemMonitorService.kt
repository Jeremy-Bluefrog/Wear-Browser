package com.example

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.app.ActivityManager
import android.content.Context
import android.util.Log

class SystemMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val monitorRunnable = object : Runnable {
        override fun run() {
            logSystemResources()
            handler.postDelayed(this, 10000) // Monitor every 10 seconds
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("SystemMonitor", "Service Created")
        handler.post(monitorRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun logSystemResources() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val availableMegs = memoryInfo.availMem / 1048576L
        val totalMegs = memoryInfo.totalMem / 1048576L
        val percentAvail = memoryInfo.availMem.toDouble() / memoryInfo.totalMem.toDouble() * 100

        Log.i("SystemMonitor", "Memory: ${availableMegs}MB available out of ${totalMegs}MB (${percentAvail.toInt()}%). Low memory: ${memoryInfo.lowMemory}")

        if (memoryInfo.lowMemory || percentAvail < 15) {
            Log.w("SystemMonitor", "PERFORMANCE BOTTLENECK: System memory is critically low.")
        }
        
        // Note: CPU usage per process is no longer easily accessible via top/stat on modern Android without root,
        // but we can log the memory state which is crucial for Wear OS.
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitorRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
