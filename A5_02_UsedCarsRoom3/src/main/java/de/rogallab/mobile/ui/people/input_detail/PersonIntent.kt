package de.rogallab.mobile.ui.people.input_detail

import de.rogallab.mobile.ui.common.UiText

sealed interface PersonIntent {
   data class FirstNameChanged(val value: String) : PersonIntent
   data class LastNameChanged(val value: String) : PersonIntent
   data class EmailChanged(val value: String) : PersonIntent
   data class PhoneChanged(val value: String) : PersonIntent
   data class ImageChanged(val value: String?) : PersonIntent
   data class ImageStorageFailed(val message: UiText) : PersonIntent
   data object Save : PersonIntent
   data object Cancel : PersonIntent
}
