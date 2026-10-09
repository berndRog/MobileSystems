package de.rogallab.mobile.ui.people.list

sealed interface PeopleEffect {

   // Shows a short informational message in the UI.
   data class ShowMessage(val message: String) : PeopleEffect

   // Requests an error message that disappears automatically after a long duration.
   data class ShowError(val message: String) : PeopleEffect

   // Navigates back to the previous screen
   data object NavigateBack : PeopleEffect

   // Opens the person destination. A null id represents create mode.
   data class NavigateTo(val personId: String? = null) : PeopleEffect
}
