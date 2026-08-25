package de.rogallab.mobile.ui.colorpicker

import androidx.compose.ui.graphics.Color

fun colorToHex(color: Color): String {
   val red = (color.red * 255).toInt()
   val green = (color.green * 255).toInt()
   val blue = (color.blue * 255).toInt()
   return String.format("#%02X%02X%02X", red, green, blue)
}
