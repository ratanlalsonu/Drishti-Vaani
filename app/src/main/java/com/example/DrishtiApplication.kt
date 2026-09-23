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
            val accelerationDir = File(filesDir, "com.google.mlkit.acceleration")
            if (!accelerationDir.exists()) {
                accelerationDir.mkdirs()
            }
            // Do not keep a 0-byte dummy file which corrupts native protobuf parsing
            val dummyFile = File(accelerationDir, "com.google.perception.acceleration_analytics_storage_v2.")
            if (dummyFile.exists() && dummyFile.length() == 0L) {
                dummyFile.delete()
            }
        } catch (e: Exception) {
            Log.w("DrishtiApplication", "Could not ensure ML Kit acceleration directory: ${e.message}")
        }
    }
}
