package de.rogallab.mobile.ui.coordinator

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.common.UiText

sealed interface PeopleCoordinatorIntent {
   // Publishes a one-shot Snackbar message from a child screen ViewModel.
   data class ShowMessage(
      val text: UiText,
   ) : PeopleCoordinatorIntent

   // Save
   data class SavePerson(
      val person: Person,
      val isNew: Boolean,
   ) : PeopleCoordinatorIntent

   // Registers a pending removal after the list has hidden the person visually.
   // Room is deliberately not changed at this point.
   data class RemovePerson(
      val person: Person,
      val originalIndex: Int,
   ) : PeopleCoordinatorIntent

   // Undo restores only the list UI because the Room row still exists.
   data class UndoRemove(
      val messageId: Long,
   ) : PeopleCoordinatorIntent

   // Commits the pending removal after the Snackbar ended without Undo.
   // This is the only intent in the Swipe/Undo flow that calls repository.remove().
   data class ConfirmRemove(
      val messageId: Long,
   ) : PeopleCoordinatorIntent

   // Advances the coordinator's message queue after a Snackbar has finished.
   data class MessageConsumed(
      val id: Long,
   ) : PeopleCoordinatorIntent
}
