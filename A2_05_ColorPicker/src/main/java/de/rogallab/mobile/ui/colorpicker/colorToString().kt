package de.rogallab.mobile.ui.colorpicker

import androidx.compose.ui.graphics.Color

fun colorToString(color: Color): String {
   val redStr = "%.2f".format(color.red)
   val greenStr = "%.2f".format(color.green)
   val blueStr = "%.2f".format(color.blue)
   return "R: $redStr, G: $greenStr, B: $blueStr"
}