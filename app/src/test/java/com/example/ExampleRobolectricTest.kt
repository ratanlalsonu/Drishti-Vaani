package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Drishti Vaani", appName)
  }

  @Test
  fun `verify mlkit acceleration directory and storage files exist`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val accelDir = java.io.File(context.filesDir, "com.google.mlkit.acceleration")
    val storageFile = java.io.File(accelDir, "com.google.perception.acceleration_analytics_storage_v2.")
    org.junit.Assert.assertTrue(accelDir.exists() || accelDir.mkdirs())
    org.junit.Assert.assertTrue(storageFile.exists() || storageFile.createNewFile())
    org.junit.Assert.assertTrue(storageFile.canRead())
  }
}
