package de.rogallab.mobile.ui.people.list

sealed interface PeopleIntent {
   data object Create : PeopleIntent
   data class Detail(val personId: String) : PeopleIntent

   // Requests deletion of an existing person. The repository is not changed yet.
   data class RequestRemove(val personId: String) : PeopleIntent

   // Confirms a previously requested deletion and starts the repository operation.
   data class ConfirmRemove(val personId: String) : PeopleIntent
}
