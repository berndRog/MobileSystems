package de.rogallab.mobile.ui.people.input_detail.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ImageRenderer(
   modifier: Modifier = Modifier,
   imageVector: ImageVector,
   imagePath: String?,
   contentDescription: String? = null,
) {

   Surface(
      modifier = modifier,
      shape = RoundedCornerShape(16.dp)
   ) {
      if (imagePath.isNullOrBlank()) {
         Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            Icon(
               imageVector = imageVector,
               contentDescription = contentDescription,
               modifier = Modifier.size(120.dp),
            )
         }
      } else {
         AsyncImage(
            model = imagePath,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(),
         )
      }
   }
}