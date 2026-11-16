package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import de.rogallab.mobile.shared.ui.BaseActivity
import de.rogallab.mobile.shared.ui.effects.rememberSnackbarController
import de.rogallab.mobile.ui.camerax.comp.CameraAdapter
import de.rogallab.mobile.ui.navigation.AppNavigation
import de.rogallab.mobile.ui.theme.AppTheme

class MainActivity : BaseActivity(TAG) {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      enableEdgeToEdge()

      setContent {

         AppTheme {

            val snackbarHostState = remember { SnackbarHostState() }
            val snackbarController = rememberSnackbarController(snackbarHostState)

            Scaffold(
               snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
               Box(Modifier.padding(innerPadding)) {
                  AppNavigation(snackbarController)
               }
            }
         }
      }
   }
   companion object {
      private const val TAG = "<-MainActivity"
   }
}

