package de.rogallab.mobile.ui.colorpicker

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/**
 * Convert an RGB triplet (0f–1f) to HSV.
 *
 * @return FloatArray(H, S, V) where
 *         H ∈ [0f, 360f], S ∈ [0f, 100f], V ∈ [0f, 100f]
 */
fun convertRGBtoHSV(
   color: Color
): FloatArray {  // H, S, V

   val r = color.red
   val g = color.green
   val b = color.blue

   val cMax = max(r, max(g, b))
   val cMin = min(r, min(g, b))
   val delta = cMax - cMin

   // Hue
   val h = when {
      abs(delta) <= 0.0001 -> 0f
      cMax == r  -> (60 * (((g - b) / delta) % 6) + 360) % 360
      cMax == g  -> 60 * (((b - r) / delta) + 2)
      else       -> 60 * (((r - g) / delta) + 4)
   }

   // Saturation
   val s = if (cMax == 0f) 0f else (delta / cMax) * 100f

   // Value (brightness)
   val v = cMax * 100f

   return floatArrayOf(h, s, v)

}


/**
 * Convert HSV to RGB.
 *
 * @param h Hue in [0, 360)
 * @param s Saturation in [0, 100]
 * @param v Value (brightness) in [0, 100]
 * @return FloatArray(R, G, B) where R, G, B ∈ [0f, 1f]
 */
fun convertHSVtoRGB(
   h: Float,
   s: Float,
   v: Float
): FloatArray {  // H, S, V

   require(h >= 0f && h < 360f) { "Hue must be in [0,360)" }
   require(s >= 0f && s <= 100f && v >= 0f && v <= 100f) { "S and V must be 0-100" }

   val sf = s / 100f
   val vf = v / 100f

   val c = vf * sf
   val x = c * (1 - abs((h / 60f) % 2 - 1))
   val m = vf - c

   val (r1, g1, b1) = when (h) {
      in 0f   ..< 60f  -> Triple(c, x, 0f)
      in 60f  ..< 120f -> Triple(x, c, 0f)
      in 120f ..< 180f -> Triple(0f, c, x)
      in 180f ..< 240f -> Triple(0f, x, c)
      in 240f ..< 300f -> Triple(x, 0f, c)
      else             -> Triple(c, 0f, x)    // 300°-360°
   }

   val r = r1 + m
   val g = g1 + m
   val b = b1 + m

   return floatArrayOf(r, g, b)

}