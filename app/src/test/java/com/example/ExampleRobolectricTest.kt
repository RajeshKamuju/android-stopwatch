package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("Stopwatch", appName)
  }

  @Test
  fun `test time formatting helper`() {
    assertEquals("00:00:00", MainActivity.formatTime(0))
    assertEquals("00:00:05", MainActivity.formatTime(5000))
    assertEquals("00:01:30", MainActivity.formatTime(90000))
    assertEquals("01:23:45", MainActivity.formatTime(5025000))
  }

  @Test
  fun `activity launches successfully`() {
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    scenario.onActivity { activity ->
      assertNotNull(activity)
    }
  }
}

