package de.rogallab.mobile.ui.cars.input_detail

import android.content.Context
import de.rogallab.mobile.R
import de.rogallab.mobile.ui.cars.input_detail.comp.CarForm

class CarValidator(
   private val context: Context,
) {
   fun validateManufacturer(value: String): String? =
      if (value.isBlank()) context.getString(R.string.error_car_manufacturer_required)
      else null

   fun validateModel(value: String): String? =
      if (value.isBlank()) context.getString(R.string.error_car_model_required)
      else null

   fun validatePrice(value: String): String? {
      if (value.isBlank()) return null
      val price = value.toIntOrNull()
      return if (price == null || price < 0) {
         context.getString(R.string.error_car_price)
      }
      else null
   }

   // Validates the complete form in the order used by the save action.
   // The first error is returned; null means that the form is valid.
   fun validateCar(form: CarForm): String? =
      validateManufacturer(form.manufacturer)
         ?: validateModel(form.model)
         ?: validatePrice(form.priceInput)
         ?: if (form.personId == null) {
            context.getString(R.string.error_car_seller_required)
         }
         else if (form.imagePaths.isEmpty()) {
            context.getString(R.string.error_car_image_required)
         }
         else null
}
