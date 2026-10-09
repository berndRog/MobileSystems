package de.rogallab.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.BaseActivity
import de.rogallab.mobile.ui.count.composables.CountAdapter
import de.rogallab.mobile.ui.count.composables.CountScreen1
import de.rogallab.mobile.ui.count.composables.CountScreen2
import de.rogallab.mobile.ui.count.composables.Stateholder
import de.rogallab.mobile.ui.theme.AppTheme

class MainActivity : BaseActivity(TAG) {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      // Log-Settings
      Alog.set(
         useAndroidLog = true,
         isVerbose = true,
         isDebug = true,
         isInfo = true,
         isComp = true
      )

      // Enable edge-to-edge mode (status bar and navigation bar are transparent)
      enableEdgeToEdge()

      // Set the content of the activity to a composable function
      setContent {
         Alog.d(TAG,"setContent() Composition")

         // Apply the app's theme
         AppTheme {

            // Scaffold provides a basic layout structure with slots for top bar, bottom bar,
            // floating action button, etc.
            Scaffold(
               modifier = Modifier.fillMaxSize()
            ) { innerPadding ->

                  Alog.d(TAG, "before CountScreen() Composition")
//                  CountScreen1(
//                     initCount = 0,
//                     modifier = Modifier
//                        .padding(innerPadding)
//                        .consumeWindowInsets(innerPadding)
//                        .padding(horizontal = 8.dp)
//                        .fillMaxWidth()
//                  )

//                  CountScreen2(
//                     initCount = 0,
//                     modifier = Modifier
//                        .padding(innerPadding)
//                        .consumeWindowInsets(innerPadding)
//                        .padding(horizontal = 8.dp)
//                        .fillMaxWidth()
//                  )

                  Stateholder(
                     initCount = 0,
                     modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                  )

//                  CountAdapter(
//                     modifier = Modifier
//                        .padding(innerPadding)  // StateFlow
//                        .fillMaxWidth()
//                  )
            }
         }
      }
   }

   companion object {
      private const val TAG = "<-MainActivity"
   }
}

@Preview(showBackground = true)
@Composable
fun CountScreen1Preview() {
   AppTheme(darkTheme = false, dynamicColor = true) {
      Scaffold(
         modifier = Modifier.fillMaxSize()
      ) { innerPadding ->
         CountScreen1(
            initCount = 0,
            modifier = Modifier
               .padding(innerPadding)
               .consumeWindowInsets(innerPadding)
               .padding(horizontal = 8.dp)
               .fillMaxWidth()
         )
      }
   }
}

@Preview(showBackground = true)
@Composable
fun CountScreen1DarkPreview() {
   AppTheme(darkTheme = true, dynamicColor = true) {
      Scaffold(
         modifier = Modifier.fillMaxSize()
      ) { innerPadding ->
         CountScreen1(
            initCount = 0,
            modifier = Modifier
               .padding(innerPadding)
               .consumeWindowInsets(innerPadding)
               .padding(horizontal = 8.dp)
               .fillMaxWidth()
         )
      }
   }
}
