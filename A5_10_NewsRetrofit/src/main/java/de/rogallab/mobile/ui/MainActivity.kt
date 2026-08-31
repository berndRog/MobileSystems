package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import de.rogallab.mobile.ui.base.BaseActivity
import de.rogallab.mobile.ui.navigation.composables.AppNavigation

import de.rogallab.mobile.ui.theme.AppTheme
import org.koin.compose.KoinContext

class MainActivity : BaseActivity(TAG) {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setContent {
            AppTheme {
               Surface(modifier = Modifier.fillMaxSize()) {
                  AppNavigation()
               }
            }

      }
   }

   companion object {
      private const val TAG = "<-MainActivity"
   }
}