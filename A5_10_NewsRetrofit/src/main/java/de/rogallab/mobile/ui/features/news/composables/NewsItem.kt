package de.rogallab.mobile.ui.features.news.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import de.rogallab.mobile.data.dtos.Article
import de.rogallab.mobile.domain.utilities.toDateString
import de.rogallab.mobile.domain.utilities.toTimeString

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun NewsItem(
   article: Article,
   onClick: () -> Unit,
   imageLoader: ImageLoader
) {

   Column(modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
   ) {

      var text = article.source?.name ?: ""
      article.publishedAt.let { isoString: String ->
         val ldt: LocalDateTime = Instant.parse(isoString).toLocalDateTime(TimeZone.currentSystemDefault())
         val date = ldt.toDateString()
         val time = ldt.toTimeString()
         text = "${article.source?.name}, $date, $time"
      }
      Text(
         text = text,
         style = MaterialTheme.typography.bodySmall
      )

      Text(
         text = article.title,
         style = MaterialTheme.typography.bodyLarge
      )

      article.urlToImage?.let { path: String ->
         AsyncImage(
            model = path,
            imageLoader = imageLoader,
            contentDescription = "Bild",
            modifier = Modifier
               .height(130.dp)
               .clip(RoundedCornerShape(percent = 5)),
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop
         )
      }

      Text(
         text = article.description ?: "",
         style = MaterialTheme.typography.bodyMedium
      )
      HorizontalDivider(modifier = Modifier
         .padding(vertical = 8.dp)
         .fillMaxWidth(),
         thickness = 4.dp
      )


   }
}