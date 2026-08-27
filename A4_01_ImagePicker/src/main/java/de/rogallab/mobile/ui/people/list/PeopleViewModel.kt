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

   // Holds the persistent UI state of the people screen.
   private val _stateFlow: MutableStateFlow<PeopleUiState> =
      MutableStateFlow(PeopleUiState())

   // Exposes the state as a read-only StateFlow to the UI.
   val stateFlow: StateFlow<PeopleUiState> =
      _stateFlow.asStateFlow()

   // Job used to observe changes from the repository.
   private var _observeJob: Job? = null

   init {
      observePeople()
   }

   // Dispatches incoming UI intents to the corresponding action.
   fun onIntent(intent: PeopleIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         PeopleIntent.Create -> navigateToPerson(null)
         is PeopleIntent.Detail -> navigateToPerson(intent.personId)
         is PeopleIntent.Remove -> remove(intent.person)
      }
   }

   // Emits the prepared navigation effect.
   private fun navigateToPerson(personId: String?) {
      viewModelScope.launch {
         _effectDelegate.emit(
            PeopleEffect.NavigateTo(personId)
         )
      }
   }

   // Observes the repository and updates the list state.
   private fun observePeople() {
      _observeJob?.cancel()

      _observeJob = viewModelScope.launch {

         // Show the loading indicator until the first result arrives.
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
               .onFailure { throwable ->
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(isLoading = false)
                  }
                  var error = _stringProvider.getString(R.string.error_people_observe)
                  _effectDelegate.emit(PeopleEffect.ShowError(error))
               }
         }
      }
   }

   // Removes a person from the repository.
   private fun remove(person: Person) {
      viewModelScope.launch {
         _repository.remove(person)
            .onSuccess {

            }
            .onFailure { throwable ->
               var error = _stringProvider.getString(R.string.error_person_remove)
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
 * - PeopleUiState beschreibt den dauerhaften Zustand der Personenliste.
 *   Änderungen verwenden konsequent state: PeopleUiState als Lambda-Parameter.
 *
 * - Repository-Fehler werden als einmalige ShowError-Effects ausgegeben.
 *
 * - ShowUndo ist bereits vorbereitet und enthält Meldung, Action-Text sowie die id der
 *   gelöschten Person. Die eigentliche Wiederherstellung wird erst beim
 *   späteren Gestures-/Undo-Schritt implementiert.
 *
 * - Create und Detail erzeugen bereits NavigateTo. Der Adapter übersetzt diesen
 *   Effect jetzt in das Hinzufügen eines PersonKey zum Navigation-3-Back-Stack.
 *
 * - String-Ressourcen werden im ViewModel über IStringProvider aufgelöst.
 *   Die Effects transportieren anschließend nur noch fertige Strings.
 *
 * Lernziele:
 *
 * - Gemeinsame Effect-Infrastruktur in mehreren ViewModels einsetzen.
 * - Fehler aus Repository-Operationen als einmalige Effects behandeln.
 * - Navigation und Undo als spätere Erweiterungen vorbereiten.
 */
