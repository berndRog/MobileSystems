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

   fun validateRegistrationYear(value: String): String? {
      if (value.isBlank()) return null
      val year = value.toIntOrNull()
      return if (year == null || year !in 1900..2100) {
         context.getString(R.string.error_car_registration_year)
      }
      else null
   }

   fun validateMileage(value: String): String? {
      if (value.isBlank()) return null
      val mileage = value.toIntOrNull()
      return if (mileage == null || mileage < 0) {
         context.getString(R.string.error_car_mileage)
      }
      else null
   }

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
      registrationYearInput: String,
      mileageInput: String,
      priceInput: String,
   ): String? =
      validateManufacturer(car.manufacturer)
         ?: validateModel(car.model)
         ?: validateRegistrationYear(registrationYearInput)
         ?: validateMileage(mileageInput)
         ?: validatePrice(priceInput)
         ?: if (car.sellerId == null) {
            context.getString(R.string.error_car_seller_required)
         }
         else if (car.imagePaths.isEmpty()) {
            context.getString(R.string.error_car_image_required)
         }
         else null
}
