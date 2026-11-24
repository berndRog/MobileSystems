package de.rogallab.mobile.data.readmodels

import androidx.room3.Embedded
import de.rogallab.mobile.data.local.dtos.TDriveDto

// Query result of ITDriveDao.observeAllWithDriverAndCar(); not a table.
// driverFullName and carDisplayName are filled by the SQL aliases.
data class TDriveListRow(
   @Embedded val tDrive: TDriveDto,
   val driverFullName: String,
   val carDisplayName: String,
)
