package com.example.feature.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isStarted = false

    init {
        // Ensure acceleration analytics directory exists to prevent native proto_data_store log warnings
        try {
            val accelDir = File(context.filesDir, "com.google.mlkit.acceleration")
            if (!accelDir.exists()) {
                accelDir.mkdirs()
            }
        } catch (e: Exception) {
            Log.w("CameraManager", "Could not create mlkit acceleration dir: ${e.message}")
        }
    }

    fun startCamera(
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
        onCameraReady: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (isStarted) return
        isStarted = true

        // Configure PreviewView to use COMPATIBLE texture view mode to avoid BufferQueue/SurfaceView abandon collisions
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        if (cameraExecutor == null || cameraExecutor?.isShutdown == true) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also { analysis ->
                        cameraExecutor?.let { exec ->
                            analysis.setAnalyzer(exec, analyzer)
                        }
                    }

                // Default to rear camera for real-world environmental awareness
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                onCameraReady()
            } catch (e: Exception) {
                Log.e("CameraManager", "Use case binding failed", e)
                onError("Camera initialization failed: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera(previewView: PreviewView? = null) {
        isStarted = false
        try {
            previewView?.apply {
                // Safely clear surface provider
            }
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e("CameraManager", "Error unbinding camera", e)
        }
    }

    fun shutdown(previewView: PreviewView? = null) {
        stopCamera(previewView)
        try {
            cameraExecutor?.shutdown()
            cameraExecutor = null
        } catch (e: Exception) {
            Log.e("CameraManager", "Error shutting down executor", e)
        }
    }
}
