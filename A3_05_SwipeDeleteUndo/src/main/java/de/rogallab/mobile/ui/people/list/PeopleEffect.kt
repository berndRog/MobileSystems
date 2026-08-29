package de.rogallab.mobile.ui.people.list

sealed interface PeopleEffect {

   data class ShowMessage(val message: String) : PeopleEffect
   data class ShowError(val message: String) : PeopleEffect

   // Requests an Action Snackbar for a person that is currently hidden only
   // from the visible UI state. The result decides between Undo and CommitRemove.
   data class ShowUndo(
      val message: String,
      val actionLabel: String,
      val personId: String,
   ) : PeopleEffect

   data object NavigateBack : PeopleEffect
   data class NavigateTo(val personId: String? = null) : PeopleEffect
}
