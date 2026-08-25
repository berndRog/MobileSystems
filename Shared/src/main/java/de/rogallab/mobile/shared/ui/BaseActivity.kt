package de.rogallab.mobile.shared.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import de.rogallab.mobile.shared.domain.utilities.Alog

open class BaseActivity(
   private val _tag: String
) : ComponentActivity() {

   // Activity is first created
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      if (savedInstanceState == null)
         Alog.i(_tag, "onCreate() Bundle == null")
      else
         Alog.i(_tag, "onCreate() Bundle != null")
   }

   // Activity is restarted
   override fun onRestart() {
      super.onRestart()
      Alog.i(_tag, "onRestart()")
   }

   // Activity is visible
   override fun onStart() {
      super.onStart()
      Alog.i(_tag, "onStart()")
   }

   // Activity interacts with the user
   override fun onResume() {
      super.onResume()
      Alog.i(_tag, "onResume()")
   }

   // User is leaving activity
   override fun onPause() {
      Alog.i(_tag, "onPause()")
      super.onPause()
   }

   // Activity is no longer visible
   override fun onStop() {
      Alog.i(_tag, "onStop()")
      super.onStop()
   }

   // Called before the activity is destroyed.
   override fun onDestroy() {
      Alog.i(_tag, "onDestroy()")
      super.onDestroy()
   }

   // Save instance state: invoked when the activity may be temporarily destroyed,
   override fun onSaveInstanceState(outState: Bundle) {
      super.onSaveInstanceState(outState)
      Alog.i(_tag, "onSaveInstanceState()")
   }

   override fun onRestoreInstanceState(savedInstanceState: Bundle) {
      super.onRestoreInstanceState(savedInstanceState)
      Alog.i(_tag, "onRestoreInstanceState()")
   }

   override fun onWindowFocusChanged(hasFocus: Boolean) {
      Alog.i(_tag, "onWindowFocusChanged() $hasFocus")
      super.onWindowFocusChanged(hasFocus)
   }
}