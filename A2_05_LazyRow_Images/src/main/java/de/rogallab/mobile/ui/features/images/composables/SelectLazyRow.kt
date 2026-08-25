package de.rogallab.mobile.ui.features.images.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.domain.entities.DogImage
import de.rogallab.mobile.domain.utilities.AppLogger

@Composable
fun SelectLazyRow(
   modifier: Modifier = Modifier,
   dogs: List<DogImage>,            // State ↓
   onDogSelect: (DogImage) -> Unit  // Event ↑
) {
   val tag = "<-SelectLazyRow"

   LazyRow(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
   ) {
      items( items = dogs ) { dog ->
         ImageItem(
            dog = dog,
            onClick = {
               AppLogger.debug(tag, "Click ${it.name}")
               onDogSelect( it )
            },
            modifier = Modifier
               .size(width = 80.dp, height = 110.dp)
         )
      }
   }
}