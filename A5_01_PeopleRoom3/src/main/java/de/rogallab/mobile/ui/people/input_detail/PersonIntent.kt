package de.rogallab.mobile.ui.people.input_detail

import de.rogallab.mobile.ui.common.UiText

// Shared intents for the common create/edit screen.
//
// Both workflows use the same PersonContent and therefore dispatch the same
// field-change and image-change intents. The shared PersonViewModel interprets
// Save as create or update according to PersonViewModelArguments.personId.
sealed interface PersonIntent {
   data class FirstNameChanged(val value: String) : PersonIntent
   data class LastNameChanged(val value: String) : PersonIntent
   data class EmailChanged(val value: String) : PersonIntent
   data class PhoneChanged(val value: String) : PersonIntent
   data class ImageChanged(val value: String?) : PersonIntent

   // Reports an image-copy or camera-file error from the Compose UI.
   data class ImageStorageFailed(
      val message: UiText,
   ) : PersonIntent

   data object Save : PersonIntent
   data object Cancel : PersonIntent
}
