package de.rogallab.mobile.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun ExampleFlowRow(
   modifier: Modifier = Modifier
) {
   Column(modifier = modifier) {
      Text(
         modifier = Modifier
            .fillMaxWidth(1f)
            .padding(vertical = 16.dp),
         text = "Flow Row with weights",
         fontWeight = FontWeight.Bold,
      )

      FlowRow(
         modifier = Modifier
            .fillMaxWidth()
            .padding(top=16.dp)
            .wrapContentHeight(align = Alignment.Top),
         horizontalArrangement = Arrangement.spacedBy(10.dp),
         verticalArrangement = Arrangement.spacedBy(20.dp),
         maxItemsInEachRow = 8,
      ) {
         repeat(20) {
            Box(Modifier
               .align(Alignment.CenterVertically)
               //.width(50.dp)
               .height(50.dp)
               .weight(1f)
               .background(randomBrightColor())
            ) {
               Text(
                  text = (it+1).toString(),
                  fontSize = 24.sp,
                  modifier = Modifier.padding(8.dp)
               )
            }
         }
      }
   }
}

fun randomColor(): Color {
   return Color(
      red = Random.nextFloat(),
      green = Random.nextFloat(),
      blue = Random.nextFloat(),
      alpha = 1f
   )
}

fun randomBrightColor(): Color {
   val hue = Random.nextFloat() * 360f
   // HSV: max saturation and value for brightness
   return Color.hsv(hue, 1f, 1f, 1f, ColorSpaces.Srgb)
}