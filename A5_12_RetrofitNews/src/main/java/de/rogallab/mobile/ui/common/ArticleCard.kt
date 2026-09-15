package de.rogallab.mobile.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.rogallab.mobile.domain.entities.Article

@Composable
fun ArticleCard(
   article: Article,
   onClick: () -> Unit,
   modifier: Modifier = Modifier,
) {
   Card(
      modifier = modifier
         .fillMaxWidth()
         .clickable(onClick = onClick),
      shape = RoundedCornerShape(12.dp),
   ) {
      if (article.imageUrl.isNullOrBlank()) {
         Column(
            modifier = Modifier
               .fillMaxWidth()
               .height(160.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            Icon(
               imageVector = Icons.AutoMirrored.Default.Article,
               contentDescription = null,
            )
         }
      }
      else {
         AsyncImage(
            model = article.imageUrl,
            contentDescription = article.title,
            modifier = Modifier
               .fillMaxWidth()
               .height(180.dp),
            contentScale = ContentScale.Crop,
         )
      }

      Column(
         modifier = Modifier.padding(16.dp),
         verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
         if (article.sourceName.isNotBlank()) {
            Text(
               text = article.sourceName,
               style = MaterialTheme.typography.labelMedium,
            )
         }
         Text(
            text = article.title,
            style = MaterialTheme.typography.titleMedium,
         )
         article.description?.let { description ->
            Text(
               text = description,
               style = MaterialTheme.typography.bodyMedium,
               maxLines = 3,
            )
         }
         if (article.publishedAt.isNotBlank()) {
            Text(
               text = article.publishedAt.replace('T', ' ').removeSuffix("Z"),
               style = MaterialTheme.typography.bodySmall,
            )
         }
      }
   }
}
