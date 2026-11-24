package de.rogallab.mobile.ui.tdrives.input_detail

import android.content.Context
import de.rogallab.mobile.R
import de.rogallab.mobile.ui.common.DateTimeText

class TDriveValidator(
   private val context: Context,
) {
   fun validateStart(value: String): String? =
      if (DateTimeText.parseOrNull(value) == null) {
         context.getString(R.string.error_date_time_format)
      }
      else null

   // Validates the complete form in the order used by the save action.
   // The first error is returned; null means that the form is valid.
   fun validateTDrive(form: TDriveForm): String? =
      if (form.personId == null) {
         context.getString(R.string.error_test_drive_person_required)
      }
      else if (form.carId == null) {
         context.getString(R.string.error_test_drive_car_required)
      }
      else {
         validateStart(form.startInput)
      }
}
