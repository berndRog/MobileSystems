package de.rogallab.mobile.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExampleRow(
   modifier: Modifier = Modifier
) {
   Column(modifier = modifier) {
      Text(
         modifier = Modifier
            .fillMaxWidth(1f)
            .padding(vertical = 16.dp),
         text = "Rows with 1 - 3 boxes",
         fontSize = 24.sp,
         fontWeight = FontWeight.Bold,
      )

      Row {
         // one box fills the remaining width
         Box(modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.Red)
         )
      }

      Row(modifier = Modifier.padding(top = 16.dp)) {
         Box(modifier = Modifier
            .size(80.dp, 120.dp)
            .background(Color.Red)
         )
         Box(modifier = Modifier
            .size(width = 80.dp, height = 160.dp)
            .background(Color.Green)
         )
         Box(modifier = Modifier
            .width(120.dp)
            .fillMaxHeight(0.25f)
            .background(Color.Blue)
         )
      }

      Row(modifier = Modifier.padding(top = 16.dp)) {
         Box(modifier = Modifier
            .size(80.dp, 120.dp)
            .background(Color.Red)
         )
         Box(modifier = Modifier
            .height( 160.dp)
            .weight(1f)       //
            .background(Color.Green)
         )
         Box(modifier = Modifier
            .width(120.dp)
            .fillMaxHeight(0.25f)
            .background(Color.Blue)
         )
      }

      Row(modifier = Modifier.padding(top = 16.dp)) {
         Box(modifier = Modifier
            .height( 120.dp)
            .weight(1f)
            .background(Color.Red)
         )
         Box(modifier = Modifier
            .height( 160.dp)
            .weight(1f) // does not fill
            .background(Color.Green)
         )
         Box(modifier = Modifier
            .fillMaxHeight(0.25f)
            .weight(1f)
            .background(Color.Blue)
         )
      }
   }
}