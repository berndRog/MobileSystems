package de.rogallab.mobile.ui.tdrives.list

import de.rogallab.mobile.domain.entities.TDrive

sealed interface TDrivesIntent {
   data object Create : TDrivesIntent
   data class Open(val testDriveId: String) : TDrivesIntent
   data class Remove(val tDrive: TDrive, val originalIndex: Int) : TDrivesIntent
   data class Restore(val tDrive: TDrive, val originalIndex: Int) : TDrivesIntent
   data object Restored : TDrivesIntent
}
