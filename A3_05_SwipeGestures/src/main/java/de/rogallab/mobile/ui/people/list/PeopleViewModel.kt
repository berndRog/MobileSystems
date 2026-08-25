package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeopleViewModel(
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<PeopleEffect>,
) : ViewModel(), IEffectSource<PeopleEffect> by _effectDelegate {

   private val _stateFlow: MutableStateFlow<PeopleUiState> =
      MutableStateFlow(PeopleUiState())

   val stateFlow: StateFlow<PeopleUiState> =
      _stateFlow.asStateFlow()

   private var _observeJob: Job? = null

   // Keeps the unfiltered repository result. Visual removal is intentionally
   // separated from persistence during the Undo window.
   private var _repositoryPeople: List<Person> = emptyList()

   // IDs hidden only in the UI. The repository still contains these people
   // until CommitRemove is received.
   private val _visuallyRemovedIds = mutableSetOf<String>()

   // Keeps the domain object required for a possible later repository delete.
   private val _pendingRemovals = mutableMapOf<String, Person>()

   init {
      Alog.i(TAG, "init: observePeople()")
      observePeople()
   }

   fun onIntent(intent: PeopleIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         PeopleIntent.Create -> navigateToPerson(null)
         is PeopleIntent.Detail -> navigateToPerson(intent.personId)
         is PeopleIntent.Remove -> removeVisually(intent.person)
         is PeopleIntent.UndoRemove -> undoRemove(intent.personId)
         is PeopleIntent.CommitRemove -> commitRemove(intent.personId)
      }
   }

   private fun navigateToPerson(personId: String?) {
      viewModelScope.launch {
         _effectDelegate.emit(
            PeopleEffect.NavigateTo(personId)
         )
      }
   }

   private fun observePeople() {
      _observeJob?.cancel()

      _observeJob = viewModelScope.launch {
         _stateFlow.update { state: PeopleUiState ->
            state.copy(isLoading = true)
         }

         delay(2500)

         _repository.observeAll().collect { result: Result<List<Person>> ->
            result
               .onSuccess { people ->
                  _repositoryPeople = people

                  // A committed removal remains hidden until the repository
                  // confirms that the row no longer exists.
                  val repositoryIds = people.mapTo(mutableSetOf()) { it.id }
                  _visuallyRemovedIds.removeAll { personId ->
                     personId !in _pendingRemovals && personId !in repositoryIds
                  }

                  publishVisiblePeople(isLoading = false)
                  Alog.d(TAG, "observePeople: people=${people.size}")
               }
               .onFailure { throwable ->
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(isLoading = false)
                  }

                  Alog.e(TAG, "observePeople failed: ${throwable.message}")
                  _effectDelegate.emit(
                     PeopleEffect.ShowError(
                        _stringProvider.getString(R.string.error_people_observe)
                     )
                  )
               }
         }
      }
   }

   // Removes a person only from the visible state. The repository is not
   // touched while the user can still select Undo.
   private fun removeVisually(person: Person) {
      if (_pendingRemovals.containsKey(person.id)) return

      _pendingRemovals[person.id] = person
      _visuallyRemovedIds += person.id
      publishVisiblePeople()

      viewModelScope.launch {
         _effectDelegate.emit(
            PeopleEffect.ShowUndo(
               message = _stringProvider.getString(R.string.message_person_removed, person.fullName),
               actionLabel = _stringProvider.getString(R.string.action_undo),
               personId = person.id,
            )
         )
      }
   }

   // Cancels a pending deletion. The person becomes visible again because the
   // repository was not changed yet.
   private fun undoRemove(personId: String) {
      if (_pendingRemovals.remove(personId) == null) return

      _visuallyRemovedIds.remove(personId)
      publishVisiblePeople()
      Alog.d(TAG, "undoRemove: personId=$personId")
   }

   // Deletes from the repository only after the Action Snackbar was dismissed
   // without selecting Undo.
   private fun commitRemove(personId: String) {
      val person = _pendingRemovals[personId] ?: return

      viewModelScope.launch {
         _repository.remove(person)
            .onSuccess {
               // Keep the id visually hidden until observeAll() confirms that
               // the repository no longer contains it.
               _pendingRemovals.remove(personId)
               Alog.d(TAG, "commitRemove: personId=$personId")
            }
            .onFailure { throwable ->
               // Persistence failed: restore the visual item and report error.
               _pendingRemovals.remove(personId)
               _visuallyRemovedIds.remove(personId)
               publishVisiblePeople()

               Alog.e(TAG, "commitRemove failed: ${throwable.message}")
               _effectDelegate.emit(
                  PeopleEffect.ShowError(
                     _stringProvider.getString(R.string.error_person_remove)
                  )
               )
            }
      }
   }

   private fun publishVisiblePeople(
      isLoading: Boolean = _stateFlow.value.isLoading,
   ) {
      val visiblePeople = _repositoryPeople.filterNot { person ->
         person.id in _visuallyRemovedIds
      }

      _stateFlow.update { state: PeopleUiState ->
         state.copy(
            people = visiblePeople,
            isLoading = isLoading,
         )
      }
   }

   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Swipe-to-Delete löscht in diesem Schritt bewusst noch nicht sofort aus dem
 *   Repository. Zuerst wird die Person nur aus PeopleUiState ausgeblendet.
 *
 * - Während die Action-Snackbar sichtbar ist, liegen drei Informationen vor:
 *
 *      _repositoryPeople      -> unveränderter Repository-Stand
 *      _visuallyRemovedIds    -> nur in der UI ausgeblendete Personen
 *      _pendingRemovals       -> mögliche spätere Repository-Löschungen
 *
 * - Wird "Rückgängig" gewählt, entfernt UndoRemove die id aus dem visuellen
 *   Filter. Die Person erscheint wieder, ohne dass das Repository angefasst
 *   werden musste.
 *
 * - Erst wenn die Action-Snackbar ohne Undo endet, führt CommitRemove die
 *   eigentliche Repository-Operation aus.
 *
 * - Schlägt dieses endgültige Löschen fehl, wird die Person wieder sichtbar
 *   gemacht und ein ShowError-Effect erzeugt.
 *
 * Lernziele:
 *
 * - Visuellen UI-State von persistiertem Repository-State unterscheiden.
 * - Undo vor einer destruktiven Persistenzoperation ermöglichen.
 * - Swipe-Geste, Animation, Effect und Repository-Operation zeitlich trennen.
 * - Mehrere gleichzeitig wartende Undo-Vorgänge über IDs auseinanderhalten.
 */
