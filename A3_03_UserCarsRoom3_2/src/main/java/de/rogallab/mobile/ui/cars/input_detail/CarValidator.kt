package de.rogallab.mobile.ui.cars.input_detail

import android.content.Context
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car

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

   fun validateCar(
      car: Car,
      priceInput: String,
   ): String? =
      validateManufacturer(car.manufacturer)
         ?: validateModel(car.model)
         ?: validatePrice(priceInput)
         ?: if (car.personId == null) {
            context.getString(R.string.error_car_seller_required)
         }
         else if (car.imagePaths.isEmpty()) {
            context.getString(R.string.error_car_image_required)
         }
         else null
}
