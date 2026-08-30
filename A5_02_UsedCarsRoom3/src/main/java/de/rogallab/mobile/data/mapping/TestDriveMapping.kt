package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.domain.entities.TDrive
import java.time.LocalDateTime

fun TDriveDto.toTestDrive(): TDrive = TDrive(
   id = id,
   personId = personId,
   carId = carId,
   start = LocalDateTime.parse(start),
   notes = notes,
   isCompleted = isCompleted,
)

fun TDrive.toTestDriveDto(): TDriveDto = TDriveDto(
   id = id,
   personId = requireNotNull(personId) { "A test drive requires a person." },
   carId = requireNotNull(carId) { "A test drive requires a car." },
   start = start.toString(),
   notes = notes,
   isCompleted = isCompleted,
)
