package de.rogallab.mobile.shared.domain.utilities

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlogTest {

   @After
   fun tearDown() {
      Alog.reset()
   }

   @Test
   fun nonAndroidBackend_writesEnabledLevelsToStandardOutput() {
      Alog.set(
         useAndroidLog = false,
         isVerbose = true,
         isDebug = true,
         isInfo = true,
         isComp = true,
      )

      val output = captureStandardOutput {
         Alog.v("Test", "verbose")
         Alog.d("Test", "debug")
         Alog.i("Test", "info")
         Alog.c("Test", "compose")
      }

      assertTrue(output.contains("V/Test:"))
      assertTrue(output.contains("verbose"))
      assertTrue(output.contains("D/Test:"))
      assertTrue(output.contains("debug"))
      assertTrue(output.contains("I/Test:"))
      assertTrue(output.contains("info"))
      assertTrue(output.contains("C/Test: compose"))
   }

   @Test
   fun disable_suppressesVerboseDebugInfoAndComposeOutput() {
      Alog.set(useAndroidLog = false)
      Alog.disable()

      val output = captureStandardOutput {
         Alog.v("Test", "verbose")
         Alog.d("Test", "debug")
         Alog.i("Test", "info")
         Alog.c("Test", "compose")
      }

      assertFalse(output.contains("verbose"))
      assertFalse(output.contains("debug"))
      assertFalse(output.contains("info"))
      assertFalse(output.contains("compose"))
   }

   @Test
   fun error_isStillWrittenWhenOtherLevelsAreDisabled() {
      Alog.set(useAndroidLog = false)
      Alog.disable()

      val output = captureStandardOutput {
         Alog.e("Test", "failure")
      }

      assertTrue(output.contains("E/Test:"))
      assertTrue(output.contains("failure"))
   }

   @Test
   fun set_changesOnlySpecifiedFlags() {
      Alog.set(
         useAndroidLog = false,
         isVerbose = false,
         isDebug = true,
      )

      val output = captureStandardOutput {
         Alog.v("Test", "verbose")
         Alog.d("Test", "debug")
      }

      assertFalse(output.contains("verbose"))
      assertTrue(output.contains("debug"))
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
