package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeopleViewModel(
   private val _repository: IPersonRepository
) : ViewModel() {

   private val _state = MutableStateFlow(PeopleUiState())
   val state: StateFlow<PeopleUiState> = _state.asStateFlow()

   private val _events = Channel<PeopleEvent>(Channel.BUFFERED)
   val events = _events.receiveAsFlow()

   private var _observeJob: Job? = null

   // IDs hidden only in the UI while Undo is still possible or while the final
   // Room DELETE has not yet appeared in the observed database snapshot.
   private val _visuallyRemovedIds = mutableSetOf<String>()

   init {
      AppLogger.info(TAG, "init: observePeople()")
      observePeople()
   }

   // The only public action entry point of this ViewModel.
   fun onIntent(intent: PeopleIntent) {
      AppLogger.debug(TAG, "onIntent: $intent")
      when (intent) {
         PeopleIntent.Create -> emitEvent(PeopleEvent.NavigateToCreate)

         is PeopleIntent.Open -> emitEvent(
            PeopleEvent.NavigateToDetails(
               personId = intent.personId
            )
         )

         is PeopleIntent.Remove -> removeVisually(
            person = intent.person,
            originalIndex = intent.originalIndex
         )

         is PeopleIntent.Restore -> restoreVisually(
            person = intent.person,
            originalIndex = intent.originalIndex
         )

         PeopleIntent.Restored -> acknowledgeRestoredItem()
      }
   }

   private fun observePeople() {
      _observeJob?.cancel()
      _observeJob = viewModelScope.launch {
         _state.update { personListState ->
            personListState.copy(
               isLoading = true
            )
         }

         _repository.observeAll().collect { result ->
            result
               .onSuccess { databasePeople ->
                  val databaseIds = databasePeople
                     .mapTo(mutableSetOf(), Person::id)

                  // Once Room no longer contains an ID, the deferred DELETE has
                  // been persisted and no visual overlay is needed anymore.
                  _visuallyRemovedIds.removeAll { id -> id !in databaseIds }

                  AppLogger.debug(TAG, "observePeople: databasePeople=${databasePeople.size}, visuallyRemovedIds=${_visuallyRemovedIds.size}")
                  _state.update { personListState ->
                     personListState.copy(
                        people = databasePeople.filterNot { person ->
                           person.id in _visuallyRemovedIds
                        },
                        isLoading = false
                     )
                  }
               }
               .onFailure {
                  _state.update { personListState ->
                     personListState.copy(isLoading = false)
                  }

                  emitEvent(
                     PeopleEvent.ShowSnackbar(
                        message = uiText(R.string.error_people_load),
                     )
                  )
               }
         }
      }
   }

   /** Removes the row from PersonListState only; Room remains unchanged. */
   private fun removeVisually(person: Person, originalIndex: Int) {
      val actualIndex = _state.value.people.indexOfFirst { currentPerson -> currentPerson.id == person.id }
      val stableIndex = when {
         originalIndex >= 0 -> originalIndex
         actualIndex >= 0 -> actualIndex
         else -> 0
      }

      _visuallyRemovedIds.add(person.id)
      _state.update { current ->
         current.copy(
            people = current.people.filterNot { currentPerson -> currentPerson.id == person.id },
            restoredPersonId = null
         )
      }

      emitEvent(
         PeopleEvent.RequestRemove(
            person = person,
            originalIndex = stableIndex
         )
      )
   }

   /** Restores the row at its former position; no INSERT is necessary. */
   private fun restoreVisually(person: Person, originalIndex: Int) {
      _visuallyRemovedIds.remove(person.id)

      _state.update { current ->
         val people = current.people
            .filterNot { currentPerson -> currentPerson.id == person.id }
            .toMutableList()

         people.add(
            index = originalIndex.coerceIn(0, people.size),
            element = person
         )

         current.copy(
            people = people,
            restoredPersonId = person.id
         )
      }
   }

   private fun acknowledgeRestoredItem() {
      _state.update { currentState -> currentState.copy(restoredPersonId = null) }
   }

   private fun emitEvent(event: PeopleEvent) {
      viewModelScope.launch {
         _events.send(event)
      }
   }

   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}
