package com.example.wear

import android.app.RemoteInput
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.util.Log
import androidx.wear.input.RemoteInputIntentHelper

/**
 * Compatibility layer for Wear OS APIs to resolve application crashes on Android 17 (API 37).
 */
object WearOsCompatLayer {
    private const val TAG = "WearOsCompatLayer"

    /**
     * Initializes the compatibility layer for Wear OS APIs.
     * Safely checks API levels to avoid crashes on newer Android versions.
     */
    fun initialize() {
        if (Build.VERSION.SDK_INT >= 37) { // API 37 corresponds to Android 17
            Log.i(TAG, "Initializing Wear OS API compatibility layer for Android 17+")
        } else {
            Log.i(TAG, "Standard Wear OS APIs are used on this Android version")
        }
    }

    /**
     * Returns a safe Intent for launching Voice Search or Keyboard Input, handling compatibility
     * requirements that may cause crashes on newer Android versions.
     */
    fun getSafeVoiceSearchIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= 37) {
            // On Android 17+, ACTION_RECOGNIZE_SPEECH might cause crashes or not be available directly.
            // Use the recommended RemoteInput API for Wear OS.
            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
            val remoteInputs = listOf(
                RemoteInput.Builder("input_result")
                    .setLabel("請說出搜尋內容")
                    .build()
            )
            RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
            intent
        } else {
            // Fallback for older versions
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "請說出搜尋內容")
            }
        }
    }
}
