package de.rogallab.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import de.rogallab.mobile.ui.theme.AppTheme

class MainActivity : BaseActivity(TAG) {
   override fun onCreate(savedInstanceState: Bundle?) {

      super.onCreate(savedInstanceState)

      Alog.set(
         useAndroidLog = true,
         isVerbose = true,
         isDebug = true,
         isInfo = true,
         isComp = true
      )

      enableEdgeToEdge()

      setContent {
         Alog.d(TAG,"setContent() Composition")

         AppTheme {
            Scaffold(
               contentColor = MaterialTheme.colorScheme.onBackground,
               modifier = Modifier
                  .padding(all = 16.dp)
                  .fillMaxSize(),
            ) { innerPadding ->
                  Alog.d(TAG, "before CountScreen() Composition")
//                  CountScreen1(
//                     initCount = 0,
//                     modifier = Modifier
//                        .padding(innerPadding)
//                        .fillMaxWidth()
//                  )
//                  CountScreen2(
//                     initCount = 0,
//                     modifier = Modifier
//                        .padding(innerPadding)
//                        .fillMaxWidth()
//                  )

//                  Stateholder(
//                     initCount = 0,
//                     modifier = Modifier
//                        .padding(innerPadding)
//                        .fillMaxWidth()
//                  )

                  CountAdapter(
                     modifier = Modifier
                        .padding(innerPadding)  // StateFlow
                        .fillMaxWidth()
                  )
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
   AppTheme {
      Scaffold(
         contentColor = MaterialTheme.colorScheme.onBackground,
         contentWindowInsets = WindowInsets.safeDrawing,
         modifier = Modifier.fillMaxSize()
      ) { innerPadding ->
         CountScreen1(
            initCount = 0,
            modifier = Modifier
               .padding(innerPadding)
               .padding(top = 8.dp)
               .padding(horizontal = 16.dp)
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
         contentColor = MaterialTheme.colorScheme.onBackground,
         contentWindowInsets = WindowInsets.safeDrawing,
         modifier = Modifier.fillMaxSize()
      ) { innerPadding ->
         CountScreen1(
            initCount = 0,
            modifier = Modifier
               .padding(innerPadding)
               .padding(top = 8.dp)
               .padding(horizontal = 16.dp)
               .fillMaxWidth()
         )
      }
   }
}
@Preview(showBackground = true)
@Composable
fun CountScreen2Preview() {
   AppTheme {
      Scaffold(
         contentColor = MaterialTheme.colorScheme.onBackground,
         contentWindowInsets = WindowInsets.safeDrawing,
         modifier = Modifier.fillMaxSize()
      ) { innerPadding ->
         CountScreen2(
            initCount = 0,
            modifier = Modifier
               .padding(innerPadding)
               .padding(top = 8.dp)
               .padding(horizontal = 16.dp)
               .fillMaxWidth()
         )
      }
   }
}

@Preview(showBackground = true)
@Composable
fun CountScreen2DarkPreview() {
   AppTheme(darkTheme = true) {
      Scaffold(
         contentColor = MaterialTheme.colorScheme.onBackground,
         contentWindowInsets = WindowInsets.safeDrawing,
         modifier = Modifier.fillMaxSize()
      ) { innerPadding ->
         CountScreen2(
            initCount = 0,
            modifier = Modifier
               .padding(innerPadding)
               .padding(top = 8.dp)
               .padding(horizontal = 16.dp)
               .fillMaxWidth()
         )
      }
   }
}
