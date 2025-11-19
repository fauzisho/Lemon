package com.uzi.lemonbenchmark

import android.app.Application
import android.util.Log
import com.facebook.soloader.SoLoader

class LemonBenchmarkApplication : Application() {
    private val TAG = "LemonBenchmarkApp"

    override fun onCreate() {
        super.onCreate()
        
        // Initialize SoLoader for loading native libraries
        SoLoader.init(this, false)
        Log.d(TAG, "SoLoader initialized")
        
        try {
            // Load ExecuTorch core library
            System.loadLibrary("executorch")
            Log.d(TAG, "✅ ExecuTorch library loaded (v1.0.0 stable)")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "❌ Failed to load ExecuTorch core library", e)
        }
    }
}
