package de.rogallab.mobile

object Globals {
   // adb reverse tcp:5082 tcp:5082 forwards the emulator port to UsedCarsApi.
   const val baseUrl = "http://127.0.0.1:5082/"

   // Picker and seed images remain local only until the upload has succeeded.
   const val imageDirectoryName = "usedcars513"
   const val animationDuration = 1000
   const val delay = 300L
}
