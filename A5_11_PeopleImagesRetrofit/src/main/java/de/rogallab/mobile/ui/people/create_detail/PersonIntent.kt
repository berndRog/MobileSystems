package de.rogallab.mobile.ui.people.create_detail

import android.net.Uri

sealed interface PersonIntent {
   data class FirstNameChange(val firstName: String) : PersonIntent
   data class LastNameChange(val lastName: String) : PersonIntent
   data class EmailChange(val email: String) : PersonIntent
   data class PhoneChange(val phone: String) : PersonIntent

   data class GalleryImageSelected(val sourceUri: Uri) : PersonIntent
   data class CameraImageTaken(val imagePath: String?) : PersonIntent
   data class RemoveImage(val imagePath: String?) : PersonIntent

   data class ImageFailed(val message: String) : PersonIntent

   data object Save : PersonIntent
   data object Cancel : PersonIntent
}
