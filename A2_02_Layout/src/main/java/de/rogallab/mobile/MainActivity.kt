package de.rogallab.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.BaseActivity
import de.rogallab.mobile.ui.images.ImagesScreen
import de.rogallab.mobile.ui.layout.ExampleBox
import de.rogallab.mobile.ui.layout.ExampleColumn
import de.rogallab.mobile.ui.layout.ExampleFlowRow
import de.rogallab.mobile.ui.layout.ExampleRow
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

         // Display an image using Coil's AsyncImage composable edge-to-edge
//         AsyncImage(
//            model = R.drawable.parrot4,
//            contentDescription = "Papagei",
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//         )

         // Apply the app's theme
         AppTheme {

            // Scaffold provides a basic layout structure with slots for top bar, bottom bar,
            // floating action button, etc.
            Scaffold(
               modifier = Modifier.fillMaxSize(),
               //containerColor = Color.Transparent
            ) { innerPadding ->

//               TextField(
//                  modifier = Modifier
//                     .padding(innerPadding)
//                     .padding(horizontal = 8.dp)
//                     .fillMaxWidth(),
//                  value = "",
//                  onValueChange = {},
//                  label = { Text("Eingabe") }
//               )

               ExampleColumn(
                  modifier = Modifier
                     .padding(innerPadding)
                     .padding(horizontal = 8.dp)
                     .fillMaxHeight()
               )

//               ExampleRow(
//                  modifier = Modifier
//                     .padding(innerPadding)
//                     .padding(horizontal = 8.dp)
//                     .fillMaxWidth()
//               )

//               ExampleBox(
//                  modifier = Modifier
//                     .padding(innerPadding)
//               )

//               ImagesScreen(
//                  modifier = Modifier
//                     .padding(innerPadding)
//
//               )
//
//               ExampleFlowRow(
//                  modifier = Modifier
//                     .padding(innerPadding)
//                     .padding(horizontal = 20.dp)
//                     .fillMaxWidth()
//               )

            }
         }

//            Box(modifier = Modifier.fillMaxSize()) {
//               Image(
//                  painter = painterResource(id = R.drawable.gorilla),
//                  contentDescription = "Lion Background",
//                  modifier = Modifier
//                     .padding(top = 40.dp)
//                     .fillMaxSize()
//                     .alpha(0.75f),
//                  contentScale = ContentScale.Crop
//               )
//
//
//               Scaffold(
//                  //contentColor = MaterialTheme.colorScheme.onBackground,
//                  containerColor = Color.Transparent,
//                  contentWindowInsets = WindowInsets.safeContent,
//                  modifier = Modifier.fillMaxSize()
//               ) { innerPadding ->
//                  Alog.debug(TAG, "before CountScreen() Composition")
//                  Screen(
//                     modifier = Modifier
//                        .padding(innerPadding)  // StateFlow
//                        .fillMaxSize()
//                  )
//               }
//            }
//         }
      }
   }

   companion object {
      private const val TAG = "<-MainActivity"
   }
}

@Composable
fun Screen(
   modifier: Modifier
) {
   Column(
      modifier = modifier
         .verticalScroll(rememberScrollState())
   ) {

      ImagesScreen(
         modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
      )

   }
}