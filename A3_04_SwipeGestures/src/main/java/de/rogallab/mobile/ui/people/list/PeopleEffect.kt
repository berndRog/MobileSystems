package de.rogallab.mobile.ui.people.list

sealed interface PeopleEffect {

   // Shows a short informational message in the UI.
   data class ShowMessage(val message: String) : PeopleEffect

   // Shows an error message that must be acknowledged by the user.
   data class ShowError(val message: String) : PeopleEffect

   // Navigates back to the previous screen. Handled by the Navigation 3 layer.
   data object NavigateBack : PeopleEffect

   // Opens the person destination. A null id represents create mode.
   data class NavigateTo(
      val personId: String? = null,
   ) : PeopleEffect
}
