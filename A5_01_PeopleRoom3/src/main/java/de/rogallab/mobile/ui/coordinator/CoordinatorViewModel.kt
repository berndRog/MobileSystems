package de.rogallab.mobile.ui.coordinator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.io.deleteImageFromAppStorage
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.common.UiMessage
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.common.uiText
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Activity-scoped coordinator for workflows that span multiple screens.
//
// Swipe-to-Delete uses exactly one pending removal. While the Undo Snackbar is
// visible, the person is removed only from the UI state. The final Room DELETE
// is performed only after the Snackbar ends without Undo.
class PeopleCoordinatorViewModel(
   private val _repository: IPersonRepository,
) : ViewModel() {

   private data class PendingRemoval(
      val person: Person,
      val originalIndex: Int,
      val messageId: Long,
   )

   private val _state = MutableStateFlow(PeopleCoordinatorState())
   val state: StateFlow<PeopleCoordinatorState> = _state.asStateFlow()

   private val _events = Channel<PeopleCoordinatorEvent>(Channel.BUFFERED)
   val events = _events.receiveAsFlow()

   private val _waitingMessages = ArrayDeque<UiMessage>()
   private val _messageIds = AtomicLong(0)

   // A newer swipe replaces the single Undo slot. The older pending removal is
   // committed before the new item becomes undoable.
   private var _pendingRemoval: PendingRemoval? = null

   // The only public action entry point of this ViewModel.
   fun onIntent(intent: PeopleCoordinatorIntent) {
      when (intent) {
         is PeopleCoordinatorIntent.ShowMessage -> enqueueMessage(intent.text)
         is PeopleCoordinatorIntent.SavePerson -> savePerson(intent.person, intent.isNew)
         is PeopleCoordinatorIntent.RemovePerson -> requestRemoval(intent.person, intent.originalIndex)
         is PeopleCoordinatorIntent.UndoRemove -> undoRemoval(intent.messageId)
         is PeopleCoordinatorIntent.ConfirmRemove -> confirmRemoval(intent.messageId)
         is PeopleCoordinatorIntent.MessageConsumed -> consumeMessage(intent.id)
      }
   }

   // Performs the Room INSERT or UPDATE and cleans up any replaced image file.
   // The UI state is updated optimistically. The Snackbar message is published
   private fun savePerson(
      person: Person,
      isNew: Boolean,
   ) {
      viewModelScope.launch {
         _state.update { currentState ->
            currentState.copy(isWriting = true)
         }

         val previousPerson =
            if (isNew) null
            else _repository.findById(person.id).getOrNull()

         val result =
            if (isNew) _repository.create(person)
            else _repository.update(person)

         result
            .onSuccess {
               val previousImagePath = previousPerson?.imagePath
               if (previousImagePath != person.imagePath) {
                  deleteImageFromAppStorage(previousImagePath)
               }

               enqueueMessage(
                  uiText(R.string.message_person_saved, person.displayName))
            }
            .onFailure {
               val previousImagePath = previousPerson?.imagePath
               if (person.imagePath != previousImagePath) {
                  deleteImageFromAppStorage(person.imagePath)
               }

               enqueueMessage(
                  uiText(R.string.error_person_save))
            }

         _state.update { currentState ->
            currentState.copy(isWriting = false)
         }
      }
   }

   // Starts the Undo window. No repository operation is performed here.
   // PeopleViewModel has already removed the item from its visual state.
   private fun requestRemoval(
      person: Person,
      originalIndex: Int,
   ) {
      // The buffer intentionally has one slot. A second swipe commits the older
      // pending removal immediately and gives the new item the Undo opportunity.
      _pendingRemoval?.let { previousRemoval ->
         _pendingRemoval = null
         discardMessage(previousRemoval.messageId)
         persistRemoval(previousRemoval)
      }

      val message = newMessage(
         text = uiText(
            R.string.message_person_removed,
            person.displayName,
         ),
         actionLabel = uiText(R.string.action_undo),
      )

      _pendingRemoval = PendingRemoval(
         person = person,
         originalIndex = originalIndex,
         messageId = message.id,
      )

      publishMessage(message)
   }

   // Restores the UI only. Room still contains the unchanged person row.
   private fun undoRemoval(messageId: Long) {
      val pendingRemoval = _pendingRemoval
         ?.takeIf { currentRemoval ->
            currentRemoval.messageId == messageId
         }
         ?: return

      _pendingRemoval = null
      emitEvent(
         PeopleCoordinatorEvent.RestorePerson(
            person = pendingRemoval.person,
            originalIndex = pendingRemoval.originalIndex,
         )
      )
   }

   // Called only when the Undo Snackbar ended without its action being used.
   private fun confirmRemoval(messageId: Long) {
      val pendingRemoval = _pendingRemoval
         ?.takeIf { currentRemoval ->
            currentRemoval.messageId == messageId
         }
         ?: return

      _pendingRemoval = null
      persistRemoval(pendingRemoval)
   }

   // Performs the final Room DELETE and rolls the visual change back on error.
   private fun persistRemoval(pendingRemoval: PendingRemoval) {
      viewModelScope.launch {
         _state.update { currentState ->
            currentState.copy(isWriting = true)
         }

         val persistedPerson = _repository
            .findById(pendingRemoval.person.id)
            .getOrNull()

         _repository.remove(pendingRemoval.person)
            .onSuccess {
               deleteImageFromAppStorage(persistedPerson?.imagePath)

               if (
                  pendingRemoval.person.imagePath != persistedPerson?.imagePath
               ) {
                  deleteImageFromAppStorage(pendingRemoval.person.imagePath)
               }
            }
            .onFailure {
               if (
                  pendingRemoval.person.imagePath != persistedPerson?.imagePath
               ) {
                  deleteImageFromAppStorage(pendingRemoval.person.imagePath)
               }

               emitEvent(
                  PeopleCoordinatorEvent.RestorePerson(
                     person = pendingRemoval.person,
                     originalIndex = pendingRemoval.originalIndex,
                  )
               )

               enqueueMessage(
                  text = uiText(R.string.error_person_delete)
               )
            }

         _state.update { currentState ->
            currentState.copy(isWriting = false)
         }
      }
   }

   private fun emitEvent(event: PeopleCoordinatorEvent) {
      viewModelScope.launch {
         _events.send(event)
      }
   }

   private fun newMessage(
      text: UiText,
      actionLabel: UiText? = null,
   ): UiMessage = UiMessage(
      id = _messageIds.incrementAndGet(),
      text = text,
      actionLabel = actionLabel,
   )

   private fun enqueueMessage(
      text: UiText,
      actionLabel: UiText? = null,
   ) {
      publishMessage(
         newMessage(
            text = text,
            actionLabel = actionLabel,
         )
      )
   }

   private fun publishMessage(message: UiMessage) {
      if (_state.value.message == null) {
         _state.update { currentState ->
            currentState.copy(message = message)
         }
      }
      else {
         _waitingMessages.addLast(message)
      }
   }

   private fun discardMessage(id: Long) {
      if (_state.value.message?.id == id) {
         val nextMessage = if (_waitingMessages.isEmpty()) {
            null
         }
         else {
            _waitingMessages.removeFirst()
         }

         _state.update { currentState ->
            currentState.copy(message = nextMessage)
         }
      }
      else {
         _waitingMessages.removeIf { message -> message.id == id }
      }
   }

   private fun consumeMessage(id: Long) {
      if (_state.value.message?.id != id) return

      val nextMessage = if (_waitingMessages.isEmpty()) {
         null
      }
      else {
         _waitingMessages.removeFirst()
      }

      _state.update { currentState ->
         currentState.copy(message = nextMessage)
      }
   }
}
