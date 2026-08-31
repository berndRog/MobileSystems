package de.rogallab.mobile

import androidx.compose.material3.SnackbarDuration

object Globals {

   const val APP_NAME = "A5_01_RoomPeople"

   const val databaseName = "A5_01_RoomPeople.db"
   const val databaseVersion  = 1

   const val fileName = databaseName
   const val directoryName = "android"

   val mediaStoreGroupname = "Photos CameraPermissions"

   val animationDuration = 1000
   val snackbarDuration = SnackbarDuration.Indefinite

   var isDebug = true
   var isInfo = true
   var isVerbose = true
   var isComp = false
}