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

   // Holds the observable PeopleUIState
   private val _stateFlow: MutableStateFlow<PeopleUiState> =
      MutableStateFlow(PeopleUiState())
   // Exposes the PeopleUiState as a read-only StateFlow to the UI.
   val stateFlow: StateFlow<PeopleUiState> =
      _stateFlow.asStateFlow()

   // Job used to observe changes from the repository.
   private var _observeJob: Job? = null

   init {
      observePeople()
   }

   // Observes the repository and publishes its current list as UI state.
   private fun observePeople() {
      _observeJob?.cancel()

      _observeJob = viewModelScope.launch {
         _stateFlow.update { state: PeopleUiState ->
            state.copy(isLoading = true)
         }

         // Simulate a longer loading operation.
         delay(1000)

         _repository.observeAll().collect { result: Result<List<Person>> ->
            result
               .onSuccess { people ->
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(people = people, isLoading = false)
                  }
               }
               .onFailure {
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(isLoading = false)
                  }

                  val error = _stringProvider.getString(R.string.error_people_observe)
                  _effectDelegate.emit(PeopleEffect.ShowError(error))
               }
         }
      }
   }

   // Dispatches incoming UI intents to the corresponding action.
   fun onIntent(intent: PeopleIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         PeopleIntent.Create -> navigateToPerson(null)
         is PeopleIntent.Detail -> navigateToPerson(intent.personId)
         is PeopleIntent.RequestRemove -> requestRemove(intent.personId)
         is PeopleIntent.ConfirmRemove -> confirmRemove(intent.personId)
      }
   }

   // Emits the navigation effect
   private fun navigateToPerson(personId: String?) {
      viewModelScope.launch {
         _effectDelegate.emit(PeopleEffect.NavigateTo(personId))
      }
   }

   // Requests confirmation before the repository is changed.
   private fun requestRemove(personId: String) {
      val person = _stateFlow.value.people.find { person: Person ->
         person.id == personId
      }

      if (person == null) {
         viewModelScope.launch {
            val error = _stringProvider.getString(R.string.error_person_not_found)
            _effectDelegate.emit(PeopleEffect.ShowError(error))
         }
         return
      }

      viewModelScope.launch {
         val message = _stringProvider.getString(
            R.string.message_person_remove_confirm,
            person.firstName,
            person.lastName,
         )
         val actionLabel = _stringProvider.getString(R.string.action_confirm)

         _effectDelegate.emit(
            PeopleEffect.ConfirmRemove(
               message = message,
               actionLabel = actionLabel,
               personId = person.id,
            )
         )
      }
   }

   // Deletes the person only after the confirmation action was selected.
   private fun confirmRemove(personId: String) {
      val person = _stateFlow.value.people.find { person: Person ->
         person.id == personId
      }

      if (person == null) {
         viewModelScope.launch {
            val error = _stringProvider.getString(R.string.error_person_not_found)
            _effectDelegate.emit(PeopleEffect.ShowError(error))
         }
         return
      }

      remove(person)
   }


   // Deletes the confirmed person from the repository.
   private fun remove(person: Person) {
      viewModelScope.launch {
         _repository.remove(person)
            .onSuccess {
               val message = _stringProvider.getString(
                  R.string.message_person_removed, person.fullName)
               _effectDelegate.emit(PeopleEffect.ShowMessage(message))
            }
            .onFailure {
               val error = _stringProvider.getString(R.string.error_person_remove)
               _effectDelegate.emit(PeopleEffect.ShowError(error))
            }
      }
   }

   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A3_04 ergänzt die Navigation aus A3_03 um Swipe-Gesten. Die Geste selbst
 *   bleibt in der Compose-UI; das ViewModel erhält nur die daraus entstandenen
 *   Intents.
 *
 * - Swipe-to-Detail wird wie ein normaler Detail-Aufruf behandelt und erzeugt
 *   NavigateTo. Swipe-to-Delete erzeugt zunächst RequestRemove.
 *
 * - RequestRemove verändert das Repository noch nicht. Das ViewModel erzeugt
 *   stattdessen ConfirmRemove als einmaligen Effect mit fertigem Meldungstext,
 *   Action-Label und Person-ID.
 *
 * - Erst wenn die Action der Snackbar gewählt wurde, sendet die UI
 *   PeopleIntent.ConfirmRemove. Danach wird _repository.remove(...) ausgeführt.
 *   Wird die Snackbar verworfen oder läuft sie ab, bleibt die Person unverändert.
 *
 * - A3_04 benötigt dafür keinen zusätzlichen temporären UI-State. Visuelles
 *   Entfernen, Undo und verzögertes Persistieren werden erst in
 *   A3_05_SwipeDeleteUndo ergänzt.
 *
 * Lernziele:
 *
 * - UI-Gesten in fachliche Intents übersetzen.
 * - Eine destruktive Aktion vor der Repository-Änderung bestätigen.
 * - State und einmalige Effects weiterhin klar voneinander trennen.
 * - Delete-Bestätigung und Undo als unterschiedliche Konzepte verstehen.
 */
