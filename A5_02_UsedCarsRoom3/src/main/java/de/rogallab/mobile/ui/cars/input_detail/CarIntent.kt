package de.rogallab.mobile.ui.cars.input_detail

import android.net.Uri

sealed interface CarIntent {
   data class ManufacturerChanged(val value: String) : CarIntent
   data class ModelChanged(val value: String) : CarIntent
   data class RegistrationYearChanged(val value: String) : CarIntent
   data class MileageChanged(val value: String) : CarIntent
   data class PriceChanged(val value: String) : CarIntent
   data class SellerChanged(val personId: String?) : CarIntent

   // GalleryPickerHandler returns content URIs. The ViewModel copies them
   // into private app storage through the shared IImageFileStorage service.
   data class GalleryImagesSelected(val sourceUris: List<Uri>) : CarIntent

   // CameraPickerHandler already returns a confirmed private image path.
   data class CameraImageTaken(val imagePath: String) : CarIntent

   data class ImageRemoved(val imagePath: String) : CarIntent
   data class ImageFailed(val message: String) : CarIntent

   data object Save : CarIntent
   data object Cancel : CarIntent
}
