package de.rogallab.mobile.ui

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.info

open class BaseActivity(
   private val _tag: String
) : ComponentActivity() {


   // Activity is first created
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      if (savedInstanceState == null)
         AppLogger.info(_tag, "onCreate() Bundle == null")
      else
         AppLogger.info(_tag, "onCreate() Bundle != null")
   }

   // Activity is restarted
   override fun onRestart() {
      super.onRestart()
      AppLogger.info(_tag, "onRestart()")
   }

   // Activity is visible
   override fun onStart() {
      super.onStart()
      AppLogger.info(_tag, "onStart()")
   }

   // Activity interacts with the user
   override fun onResume() {
      super.onResume()
      AppLogger.info(_tag, "onResume()")
   }

   // User is leaving activity
   override fun onPause() {
      AppLogger.info(_tag, "onPause()")
      super.onPause()
   }

   // Activity is no longer visible
   override fun onStop() {
      AppLogger.info(_tag, "onStop()")
      super.onStop()
   }

   // Called before the activity is destroyed.
   override fun onDestroy() {
      AppLogger.info(_tag, "onDestroy()")
      super.onDestroy()
   }

   // Save instance state: invoked when the activity may be temporarily destroyed,
   override fun onSaveInstanceState(outState: Bundle) {
      super.onSaveInstanceState(outState)
      AppLogger.info(_tag, "onSaveInstanceState()")
   }

   override fun onRestoreInstanceState(savedInstanceState: Bundle) {
      super.onRestoreInstanceState(savedInstanceState)
      AppLogger.info(_tag, "onRestoreInstanceState()")
   }

   override fun onConfigurationChanged(newConfig: Configuration) {
      super.onConfigurationChanged(newConfig)

      // Checks the orientation of the screen
      if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
         AppLogger.info(_tag, "landscape")
      } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
         AppLogger.info(_tag, "portrait")
      }
   }

   override fun onWindowFocusChanged(hasFocus: Boolean) {
      AppLogger.info(_tag, "onWindowFocusChanged() $hasFocus")
      super.onWindowFocusChanged(hasFocus)
   }
}