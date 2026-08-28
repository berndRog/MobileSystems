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

   // Holds the observable UI state of the people screen.
   private val _stateFlow: MutableStateFlow<PeopleUiState> =
      MutableStateFlow(PeopleUiState())

   // Exposes the state as a read-only StateFlow to the UI.
   val stateFlow: StateFlow<PeopleUiState> =
      _stateFlow.asStateFlow()

   // Job used to observe changes from the repository.
   private var _observeJob: Job? = null

   init {
      Alog.i(TAG, "init: observePeople()")
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

   // Emits the navigation effect. A null id opens the create destination.
   private fun navigateToPerson(personId: String?) {
      viewModelScope.launch {
         _effectDelegate.emit(
            PeopleEffect.NavigateTo(personId)
         )
      }
   }

   // Observes the repository and publishes its current list as UI state.
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

                  val error = _stringProvider.getString(R.string.error_people_observe)
                  _effectDelegate.emit(PeopleEffect.ShowError(error))
               }
         }
      }
   }

   // Deletes the person immediately
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
 * - A3_04 ergänzt die Navigation aus A3_03 um Swipe-Gesten. Die Geste selbst
 *   bleibt in der Compose-UI; das ViewModel erhält nur die daraus entstandenen
 *   Intents.
 *
 * - Swipe-to-Detail wird wie ein normaler Detail-Aufruf behandelt und erzeugt
 *   NavigateTo. Swipe-to-Delete erzeugt PeopleIntent.Remove.
 *
 * - Remove ruft in diesem Schritt _repository.remove(...) unmittelbar auf.
 *   Nach erfolgreichem Löschen liefert observeAll() die geänderte persistierte
 *   Liste und PeopleUiState wird aktualisiert.
 *
 * - Schlägt das Löschen fehl, bleibt der Repository-State unverändert und das
 *   ViewModel erzeugt ShowError als einmaligen Effect.
 *
 * - Einen zusätzlichen temporären Löschzustand gibt es bewusst noch nicht.
 *   Visuelles Entfernen, Undo und verzögertes Persistieren werden erst in
 *   A3_05_SwipeDeleteUndo ergänzt.
 *
 * Lernziele:
 *
 * - UI-Gesten in fachliche Intents übersetzen.
 * - Swipe-to-Detail über den bestehenden Navigation-Effect weiterleiten.
 * - Swipe-to-Delete zunächst als direkte Repository-Operation implementieren.
 * - State und einmalige Effects weiterhin klar voneinander trennen.
 */
