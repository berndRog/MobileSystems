package de.rogallab.mobile.ui.people.list

sealed interface PeopleEffect {

   data class ShowMessage(val message: String) : PeopleEffect
   data class ShowError(val message: String) : PeopleEffect

   // Requests a confirmation Snackbar before deleting an existing person.
   data class ConfirmRemove(
      val message: String,
      val actionLabel: String,
      val personId: String,
   ) : PeopleEffect

   data class NavigateTo(val personId: String? = null) : PeopleEffect
}
