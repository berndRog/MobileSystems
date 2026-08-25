package de.rogallab.mobile.ui.images

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.rogallab.mobile.R

@Composable
fun ImagesScreen(
   imageNumber: Int = 1,
   modifier: Modifier = Modifier
) {

   Box(modifier = modifier.size(200.dp)) {

      AsyncImage(
         model = R.drawable.toucan,
         contentDescription = "Toucan",
         modifier = Modifier.fillMaxSize(),
         contentScale = ContentScale.Crop
      )
   }

//   Row(modifier = modifier.fillMaxWidth()) {
//
//      AsyncImage(
//         model = R.drawable.parrot4,
//         contentDescription = "Papagei",
//         modifier = Modifier.weight(1f),
//         contentScale = ContentScale.Crop
//      )
//
//      Spacer(modifier = Modifier.width(16.dp))
//
//      AsyncImage(
//         model = R.drawable.toucan,
//         contentDescription = "Toucan",
//         modifier = Modifier.weight(1f)
//      )
//   }
//
//   AsyncImage(
//      model = R.drawable.zebra,
//      contentDescription = "Zebra",
//      modifier = Modifier.fillMaxWidth(),
//      contentScale = ContentScale.Fit
//   )
//
//
//   AsyncImage(
//      model = R.drawable.butterfly,
//      contentDescription = "Schlange",
//      modifier = Modifier.fillMaxWidth(),
//      contentScale = ContentScale.Fit
//   )
//   AsyncImage(
//      model = R.drawable.snake,
//      contentDescription = "Schlange",
//      modifier = Modifier.fillMaxWidth(),
//      contentScale = ContentScale.Fit
//   )
//
//   AsyncImage(
//      model = R.drawable.cow,
//      contentDescription = "Kuh",
//      //modifier = Modifier.width(250.dp),
//      //contentScale = ContentScale.Crop,
//      //clipToBounds = true,
//   )

}