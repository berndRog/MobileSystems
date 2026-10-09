package de.rogallab.mobile.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Keeps a loading failure and its recovery action visible in the screen body. */
@Composable
fun LoadFailureContent(
   message: String,
   actionLabel: String,
   onAction: () -> Unit,
   modifier: Modifier = Modifier,
) {
   Column(
      modifier = modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
   ) {
      Text(text = message)
      Button(onClick = onAction) {
         Text(text = actionLabel)
      }
   }
}
