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
import de.rogallab.mobile.ui.people.create_detail.PersonUiState
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
   private var _removeJob: Job? = null
   private var _actionGeneration = 0

   init {
      Alog.i(TAG, "init: observePeople()")
      observePeople()
   }

   // Observes the repository and updates the list state.
   private fun observePeople() {

      // Cancel any existing observation job before starting a new one.
      _observeJob?.cancel()

      // Clear the previous failure as soon as the retry starts.
      _stateFlow.update { state: PeopleUiState ->
         state.copy(isLoading = true, loadError = null)
      }
      _observeJob = viewModelScope.launch {

         // Simulate a longer loading operation.
         delay(1000)

         _repository.observeAll().collect { result: Result<List<Person>> ->
            result
               .onSuccess { people ->
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(people = people, loadError = null)
                  }
               }
               .onFailure { throwable ->
                  val error = _stringProvider.getString(R.string.error_people_observe)
                  Alog.e(TAG, error, throwable)
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(loadError = error)
                  }
               }

            // set isLoading = false after loading is complete
            _stateFlow.update { state: PeopleUiState ->
               state.copy(isLoading = false)
            }
         }
      }
   }


   // Dispatches incoming UI intents to the corresponding action.
   fun onIntent(intent: PeopleIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         PeopleIntent.Create -> navigateToPerson(null)
         PeopleIntent.RetryLoad -> observePeople()
         is PeopleIntent.Detail -> navigateToPerson(intent.personId)
         is PeopleIntent.Remove -> remove(intent.person)
      }
   }

   // List actions belong to this destination, even while its ViewModel remains alive below detail.
   fun onScreenLeft() {
      _actionGeneration++
      _removeJob?.cancel()
   }

   // Emits the prepared navigation effect.
   private fun navigateToPerson(personId: String?) {
      viewModelScope.launch {
         _effectDelegate.emit(PeopleEffect.NavigateTo(personId))
      }
   }


   // Removes a person from the repository.
   private fun remove(person: Person) {
      val generation = _actionGeneration
      _removeJob = viewModelScope.launch {
         val result = _repository.remove(person)
         if (generation != _actionGeneration) {
            result.exceptionOrNull()?.let { Alog.e(TAG, "Ignored remove failure after leaving", it) }
            return@launch
         }
         result
            .onSuccess {
               val message = _stringProvider.getString(
                  R.string.message_person_removed, person.fullName)
               _effectDelegate.emit(PeopleEffect.ShowMessage(message))
            }
            .onFailure { throwable ->
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
 * - PeopleUiState beschreibt den dauerhaften Zustand der Personenliste.
 *   Änderungen verwenden konsequent state: PeopleUiState als Lambda-Parameter.
 *
 * - Ein Fehler beim Beobachten der Liste bleibt als loadError im UI-State
 *   sichtbar. Fehler einzelner Aktionen werden als ShowError-Effects ausgegeben.
 *   Eine laufende Listenaktion wird beim Wechsel zur Detailansicht entwertet;
 *   ihr späteres Ergebnis erzeugt dort keine Meldung.
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
