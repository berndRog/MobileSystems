package de.rogallab.mobile.shared.ui

import android.app.Application
import de.rogallab.mobile.shared.domain.utilities.Alog
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
)
class BaseActivityTest {

   @After
   fun tearDown() {
      Alog.reset()
   }

   @Test
   fun lifecycleCallbacks_areForwardedAndLogged() {
      Alog.set(
         useAndroidLog = false,
         isInfo = true,
      )

      val output = captureStandardOutput {
         Robolectric
            .buildActivity(TestBaseActivity::class.java)
            .create()
            .start()
            .resume()
            .pause()
            .stop()
            .destroy()
      }

      assertTrue(output.contains("onCreate() Bundle == null"))
      assertTrue(output.contains("onStart()"))
      assertTrue(output.contains("onResume()"))
      assertTrue(output.contains("onPause()"))
      assertTrue(output.contains("onStop()"))
      assertTrue(output.contains("onDestroy()"))
   }

   private fun captureStandardOutput(block: () -> Unit): String {
      val originalOut = System.out
      val outputStream = ByteArrayOutputStream()

      return try {
         System.setOut(PrintStream(outputStream))
         block()
         outputStream.toString()
      }
      finally {
         System.setOut(originalOut)
      }
   }
}

class TestBaseActivity : BaseActivity("TestBaseActivity")
