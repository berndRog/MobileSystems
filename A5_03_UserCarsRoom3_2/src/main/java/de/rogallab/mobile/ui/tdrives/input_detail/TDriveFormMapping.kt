package de.rogallab.mobile.ui.tdrives.input_detail

import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.ui.common.DateTimeText
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

// Domain -> form: the start instant becomes local text for the TextField.
fun TDrive.toTDriveForm(timeZone: TimeZone): TDriveForm = TDriveForm(
   id = id,
   personId = personId,
   carId = carId,
   startInput = DateTimeText.format(start.toLocalDateTime(timeZone)),
   isCompleted = isCompleted,
)

// Normalizes user input before validation. Stays a TDriveForm,
// so the UI can show the cleaned values.
fun TDriveForm.normalized(): TDriveForm = copy(
   startInput = startInput.trim(),
)

// Form -> domain: call only after successful validation.
// requireNotNull documents the precondition that the validator guarantees.
fun TDriveForm.toTDrive(timeZone: TimeZone): TDrive = TDrive(
   id = id,
   personId = requireNotNull(personId) { "A test drive requires a driver." },
   carId = requireNotNull(carId) { "A test drive requires a car." },
   start = DateTimeText.parse(startInput).toInstant(timeZone),
   isCompleted = isCompleted,
)
