package de.rogallab.mobile.ui.people.create_detail

sealed interface PersonIntent {
   data class  FirstNameChange(val firstName: String) : PersonIntent
   data class  LastNameChange(val lastName: String) : PersonIntent

   data object Save : PersonIntent
   data object Cancel : PersonIntent
}