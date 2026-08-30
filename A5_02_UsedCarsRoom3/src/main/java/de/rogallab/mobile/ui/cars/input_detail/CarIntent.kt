package de.rogallab.mobile.ui.cars.input_detail

sealed interface CarIntent {
   data class ManufacturerChanged(val value: String) : CarIntent
   data class ModelChanged(val value: String) : CarIntent
   data class RegistrationYearChanged(val value: String) : CarIntent
   data class MileageChanged(val value: String) : CarIntent
   data class PriceChanged(val value: String) : CarIntent
   data class SellerChanged(val personId: String?) : CarIntent
   data class ImagesAdded(val imagePaths: List<String>) : CarIntent
   data class ImageRemoved(val imagePath: String) : CarIntent
   data class ImageStorageFailed(val message: String) : CarIntent
   data object Save : CarIntent
   data object Cancel : CarIntent
}
