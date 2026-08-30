package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.composables.ImageRenderer

private const val TAG = "<-PersonCard"

@Composable
fun PersonCard(
   person: Person,
   modifier: Modifier = Modifier,
) {
   var cCount by remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount++}") }

   Card(
      modifier = modifier
         .fillMaxWidth()
         .height(88.dp),
      shape = RoundedCornerShape(10.dp),
   ) {
      Row(
         modifier = Modifier.fillMaxSize(),
         verticalAlignment = Alignment.CenterVertically,
      ) {
         val imagePath = person.validImagePath

         ImageRenderer(
            modifier = Modifier
               .weight(1f)
               .padding(4.dp),
            imageVector = Icons.Default.AccountCircle,
            imagePath = person.imagePath,
            contentDescription = person.displayName
         )
//         Surface(
//            modifier = Modifier
//               .weight(0.25f)
//               .padding(4.dp)
//               .fillMaxHeight(),
//            shape = RoundedCornerShape(10.dp)
//         ) {
//            if (imagePath.isNullOrBlank()) {
//               Icon(
//                  imageVector = Icons.Default.AccountCircle,
//                  contentDescription = stringResource(R.string.person_image_list),
//               )
//            } else {
//               AsyncImage(
//                  modifier = Modifier
//                     .fillMaxHeight(),
//                  model = imagePath.toImageModel(),
//                  contentDescription = stringResource(R.string.person_image_list),
//                  contentScale = ContentScale.Crop,
//               )
//            }
//         }

         Column(
            modifier = Modifier
               .weight(3.0f)
               .padding(horizontal = 4.dp),
         ) {
            Text(
               text = person.displayName,
               style = MaterialTheme.typography.titleMedium
            )
            person.email?.let { email ->
               Text(
                  text = email,
                  style = MaterialTheme.typography.bodyMedium
               )
            }
            person.phone?.let { phone ->
               Text(
                  text = phone,
                  style = MaterialTheme.typography.bodyMedium
               )
            }
         }
      }
   }
}