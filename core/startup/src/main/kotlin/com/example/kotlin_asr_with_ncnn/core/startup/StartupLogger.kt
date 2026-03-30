package com.example.kotlin_asr_with_ncnn.core.startup

import android.util.Log

object StartupLogger {
    private const val TAG = "StartupPipeline"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(TAG, message, throwable)
        else Log.e(TAG, message)
    }
}
