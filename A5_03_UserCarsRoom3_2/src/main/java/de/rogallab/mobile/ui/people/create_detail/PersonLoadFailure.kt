package de.rogallab.mobile.ui.people.create_detail

sealed interface PersonLoadFailure {
   val message: String

   data class NotFound(override val message: String) : PersonLoadFailure
   data class Failed(override val message: String) : PersonLoadFailure
}