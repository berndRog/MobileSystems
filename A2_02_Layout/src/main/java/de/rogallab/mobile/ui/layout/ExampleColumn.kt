package de.rogallab.mobile.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExampleColumn(
   modifier: Modifier = Modifier
) {
   Column(modifier = modifier) {
      Text(
         modifier = Modifier
            .fillMaxWidth(fraction = 1f)
            .padding(vertical = 16.dp),
         text = "Column with three boxes",
         fontSize = 24.sp,
         fontWeight = FontWeight.Bold,
      )

      Column {
         Box(modifier = Modifier
            .size(80.dp, 160.dp)
            .background(Color.Red)
         )
         Box(modifier = Modifier
            .fillMaxWidth(fraction = 1f)
            .weight(1f)
            .background(Color.Green)
         )
         Box(modifier = Modifier
            .size(40.dp, 240.dp)
            .background(Color.Blue)
         )
      }
   }
}