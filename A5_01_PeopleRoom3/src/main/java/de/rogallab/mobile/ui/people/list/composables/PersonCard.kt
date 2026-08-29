package de.rogallab.mobile.ui.people.list.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose

@Composable
fun PersonCard(
   firstName: String,
   lastName: String,
   email: String?,
   phone: String?,
   imagePath: String?,
   modifier: Modifier = Modifier
) {
   val tag = "<-PersonCard"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.intValue++}") }

   val cardHeight = 88.dp
   val fullName = stringResource(
      R.string.person_full_name,
      firstName,
      lastName,
   )

   Card(
      modifier = modifier
         .fillMaxWidth()
         .height(cardHeight),
      shape = RoundedCornerShape(percent = 10),
   ) {
      Row(
         modifier = Modifier.fillMaxSize(),
         verticalAlignment = Alignment.CenterVertically,
      ) {
         if (imagePath != null) {
            Box(
               modifier = Modifier
                  .weight(0.175f)
                  .fillMaxHeight()
                  .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                  .clip(RoundedCornerShape(percent = 10))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
               AsyncImage(
                  model = imagePath,
                  contentDescription = stringResource(R.string.person_image_list),
                  modifier = Modifier.fillMaxSize(),
                  alignment = Alignment.Center,
                  contentScale = ContentScale.Crop
               )
            }

            Column(
               modifier = Modifier
                  .weight(0.825f)
                  .padding(start = 8.dp, end= 4.dp, top = 0.dp, bottom = 4.dp)
            ) {
               Text(
                  text = fullName,
                  style = MaterialTheme.typography.bodyLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
               )
               email?.let {
                  Text(
                     text = it,
                     style = MaterialTheme.typography.bodyMedium,
                     maxLines = 1,
                     overflow = TextOverflow.Ellipsis
                  )
               }
               phone?.let {
                  Text(
                     text = it,
                     style = MaterialTheme.typography.bodyMedium,
                     maxLines = 1,
                     overflow = TextOverflow.Ellipsis
                  )
               }
            }
         } else {
            Column(
               modifier = Modifier
                  .weight(1f)
                  .padding(vertical = 0.dp, horizontal = 8.dp)
            ) {
               Text(
                  text = fullName,
                  style = MaterialTheme.typography.bodyLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
               )
               email?.let {
                  Text(
                     text = it,
                     style = MaterialTheme.typography.bodyMedium,
                     maxLines = 1,
                     overflow = TextOverflow.Ellipsis
                  )
               }
               phone?.let {
                  Text(
                     text = it,
                     style = MaterialTheme.typography.bodyMedium,
                     maxLines = 1,
                     overflow = TextOverflow.Ellipsis
                  )
               }
            }
         }
      }
   }
}