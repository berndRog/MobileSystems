package de.rogallab.mobile.ui.tdrives.input_detail

import android.content.Context
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.ui.common.DateTimeText
import kotlinx.datetime.LocalDateTime

class TDriveValidator(
   private val context: Context,
) {
   fun parseStart(value: String): LocalDateTime? =
      DateTimeText.parseOrNull(value)

   fun validateStart(value: String): String? =
      if (DateTimeText.parseOrNull(value) == null) {
         context.getString(R.string.error_date_time_format)
      }
      else null

   fun validateTestDrive(
      tDrive: TDrive,
      startInput: String,
   ): String? =
      if (tDrive.personId == null) {
         context.getString(R.string.error_test_drive_person_required)
      }
      else if (tDrive.carId == null) {
         context.getString(R.string.error_test_drive_car_required)
      }
      else {
         validateStart(startInput)
      }
}
