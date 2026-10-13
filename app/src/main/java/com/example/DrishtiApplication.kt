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
            // Ensure acceleration storage files exist so native proto_data_store can open them without ENOENT error
            val storageFiles = listOf(
                "com.google.perception.acceleration_analytics_storage_v2.",
                "com.google.perception.acceleration_analytics_storage_v2"
            )
            for (filename in storageFiles) {
                val targetFile = File(accelerationDir, filename)
                if (!targetFile.exists()) {
                    targetFile.createNewFile()
                }
                targetFile.setReadable(true, false)
                targetFile.setWritable(true, false)
            }
        } catch (e: Exception) {
            Log.w("DrishtiApplication", "Could not ensure ML Kit acceleration directory: ${e.message}")
        }
    }
}
