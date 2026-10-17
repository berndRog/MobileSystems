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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeopleViewModel(
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<PeopleEffect>,
) : ViewModel(), IEffectSource<PeopleEffect> by _effectDelegate {

   // StateFlow PeopleUiState
   private val _stateFlow = MutableStateFlow(PeopleUiState())
   val stateFlow = _stateFlow.asStateFlow()

   // Start the reactive observation pipeline
   init {
      Alog.i(TAG, "init: observePeople()")
      observePeople()
   }

   // Job used to observe changes from the repository.
   private var _observeJob: Job? = null

   // Observes the repository and updates the list state.
   private fun observePeople() {
      // Cancel any existing observation job before starting a new one.
      _observeJob?.cancel()

      // Mark the attempt before launching so repeated retries cannot overlap it.
      _stateFlow.update { state ->
         state.copy(isLoading = true, loadFailure = null)
      }

      _observeJob = viewModelScope.launch {

         delay(1000)

         // Successful Room emissions keep the list current.
         // Result.failure ends this collector
         // Only RetryLoad starts a new observation.
         _repository.observeAll().collect { result ->
            result
               // Update the UI state with the list of people when the repository operation succeeded.
               .onSuccess { people ->
                  _stateFlow.update { state ->
                     state.copy(people = people, isLoading = false, loadFailure = null)
                  }
               }

               // Update the UI state with an error message when the repository operation failed.
               .onFailure { throwable ->
                  val error = _stringProvider.getString(
                     R.string.error_people_observe
                  )
                  Alog.e(TAG, error, throwable)
                  _stateFlow.update { state ->
                     state.copy(isLoading = false, loadFailure = error)
                  }
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
         PeopleIntent.RetryLoad ->
            if (!_stateFlow.value.isLoading && _stateFlow.value.loadFailure != null) {
               observePeople()
            }
      }
   }

   // Emits navigation as a one-time effect; AppNavigation updates the back stack.
   private fun navigateToPerson(personId: String?) {
      viewModelScope.launch {
         _effectDelegate.emit(PeopleEffect.NavigateTo(personId))
      }
   }

   // A swipe asks for confirmation before changing the repository.
   private fun requestRemove(personId: String) {
      val person = _stateFlow.value.people.find { it.id == personId }
      if (person == null) {
         viewModelScope.launch {
            _effectDelegate.emit(PeopleEffect.ShowError(
               _stringProvider.getString(R.string.error_person_not_found)
            ))
         }
         return
      }

      viewModelScope.launch {
         _effectDelegate.emit(PeopleEffect.ConfirmRemove(
            message = _stringProvider.getString(
               R.string.message_person_remove_confirm, person.firstName, person.lastName
            ),
            actionLabel = _stringProvider.getString(R.string.action_confirm),
            personId = person.id,
         ))
      }
   }

   // The confirmation action performs the repository operation.
   private fun confirmRemove(personId: String) {
      val person = _stateFlow.value.people.find { it.id == personId }
      if (person == null) {
         viewModelScope.launch {
            _effectDelegate.emit(PeopleEffect.ShowError(
               _stringProvider.getString(R.string.error_person_not_found)
            ))
         }
         return
      }
      remove(person)
   }

   // Removes a person from the repository.
   private fun remove(person: Person) {
      viewModelScope.launch {
         _repository.remove(person)
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
 * - Ein Listen-Ladefehler bleibt im State sichtbar. Erst RetryLoad startet
 *   einen neuen Collector; erfolgreiche Room-Emissionen aktualisieren die Liste.
 * - Fehler einzelner Benutzeraktionen bleiben einmalige ShowError-Effects.
 *
 * - Create und Detail erzeugen NavigateTo für den Navigation-3-Backstack.
 * - Swipe-to-Delete erzeugt ConfirmRemove; erst die Snackbar-Aktion löst
 *   ConfirmRemove als Intent und damit das Löschen im Repository aus.
 *
 * - String-Ressourcen werden im ViewModel über IStringProvider aufgelöst.
 *   Die Effects transportieren anschließend nur noch fertige Strings.
 *
 * Lernziele:
 *
 * - Gemeinsame Effect-Infrastruktur in mehreren ViewModels einsetzen.
 * - Dauerhafte Ladefehler von einmaligen Aktionsfehlern unterscheiden.
 * - Navigation als einmaligen Effect an den Backstack weitergeben.
 */
