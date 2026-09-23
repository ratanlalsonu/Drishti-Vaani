package com.example

import android.app.Application
import android.util.Log
import java.io.File

class DrishtiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureMlKitStorageDirectories()
    }

    private fun ensureMlKitStorageDirectories() {
        try {
            // ML Kit native perception/acceleration analytics expects this directory to exist.
            // Pre-creating the directory and storage file prevents native proto_data_store.cc:36
            // "No such file or directory" error on startup.
            val accelerationDir = File(filesDir, "com.google.mlkit.acceleration")
            if (!accelerationDir.exists()) {
                accelerationDir.mkdirs()
            }
            val analyticsStorage = File(accelerationDir, "com.google.perception.acceleration_analytics_storage_v2.")
            if (!analyticsStorage.exists()) {
                analyticsStorage.createNewFile()
            }
        } catch (e: Exception) {
            Log.w("DrishtiApplication", "Could not pre-initialize ML Kit acceleration storage: ${e.message}")
        }
    }
}
