package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.rogallab.mobile.shared.ui.BaseActivity
import de.rogallab.mobile.ui.navigation.comp.AppNavigation
import de.rogallab.mobile.ui.theme.AppTheme

class MainActivity : BaseActivity(TAG) {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      enableEdgeToEdge()

      setContent {
         AppTheme {
            AppNavigation()
         }
      }
   }

   companion object {
      private const val TAG = "<-MainActivity"
   }
}
