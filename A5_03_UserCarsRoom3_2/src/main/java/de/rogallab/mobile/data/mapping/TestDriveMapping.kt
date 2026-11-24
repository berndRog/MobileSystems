package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.data.readmodels.TDriveListRow
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.readmodels.TDriveListItem

// Write side: persistence model <-> domain entity.
// No requireNotNull anymore: the domain entity only allows valid test drives.
fun TDriveDto.toTestDrive(): TDrive = TDrive(
   id = id,
   personId = personId,
   carId = carId,
   start = start,
   isCompleted = isCompleted,
)

fun TDrive.toTestDriveDto(): TDriveDto = TDriveDto(
   id = id,
   personId = personId,
   carId = carId,
   start = start,
   isCompleted = isCompleted,
)

// Read side: query result -> projection.
fun TDriveListRow.toTDriveListItem(): TDriveListItem = TDriveListItem(
   id = tDrive.id,
   driverFullName = driverFullName,
   carDisplayName = carDisplayName,
   start = tDrive.start,
   isCompleted = tDrive.isCompleted,
)
