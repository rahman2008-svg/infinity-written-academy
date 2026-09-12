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
    assertEquals("Infinity Written Academy", appName)
  }

  @Test
  fun `verify developer details`() {
    val developerName = "Prince AR Abdur Rahman"
    val companyName = "NexVora Lab's Ofc"
    assertEquals("Prince AR Abdur Rahman", developerName)
    assertEquals("NexVora Lab's Ofc", companyName)
  }
}
