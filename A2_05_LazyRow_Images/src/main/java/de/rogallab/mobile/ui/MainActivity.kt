package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.utilities.AppLogger.debug
import de.rogallab.mobile.ui.features.images.composables.ImagesListScreen
import de.rogallab.mobile.ui.theme.AppTheme

class MainActivity : BaseActivity(TAG) {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      enableEdgeToEdge()
      setContent {
            AppTheme {
               Scaffold(
                  contentColor = MaterialTheme.colorScheme.onBackground,
                  contentWindowInsets = WindowInsets.safeDrawing,
                  modifier = Modifier.padding(horizontal = 20.dp).fillMaxSize(),
               ) { innerPadding ->
                  ImagesListScreen(
                     modifier = Modifier.padding(innerPadding)
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
fun Preview() {

   AppTheme {

      val text = stringResource(id = R.string.dog_01)

      Column(
         horizontalAlignment = Alignment.CenterHorizontally,
         modifier = Modifier
            .fillMaxWidth()
            .padding(all = 8.dp)
      ) {
         Image(
            modifier = Modifier.size(250.dp,300.dp)
               .border(1.dp, color = MaterialTheme.colorScheme.tertiary),
            painter = painterResource(id = R.drawable.dog_01),
            contentDescription = text,
            contentScale = ContentScale.Crop
         )
         Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
         )
      }
   }
}