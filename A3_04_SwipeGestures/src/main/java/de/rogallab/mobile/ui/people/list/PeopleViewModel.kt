package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import de.rogallab.mobile.AppTiming
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import de.rogallab.mobile.shared.domain.IEffectSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeopleViewModel(
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<PeopleEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<PeopleEffect> by _effectDelegate {

   // Reuse the injected transport for messages and feature-specific effects.
   private val _effects = EffectDispatcher<PeopleEffect>(
      _scope = viewModelScope,
      _effectDelegate = _effectDelegate,
      _stringProvider = _stringProvider,
      _errorEffect = { PeopleEffect.ShowError(it) },
      _messageEffect = { PeopleEffect.ShowMessage(it) },
      _tag = TAG,
   )

   // StateFlow PeopleUiState
   private val _stateFlow = MutableStateFlow(PeopleUiState())
   val stateFlow = _stateFlow.asStateFlow()

   // Job used to observe changes from the repository.
   private var _observeJob: Job? = null

   // Start the reactive observation pipeline
   init {
      Alog.i(TAG, "init: observePeople()")
      observePeople()
   }

   // Observes the repository and updates the list state.
   private fun observePeople() {
      // Cancel any existing observation job before starting a new one.
      _observeJob?.cancel()

      // Mark the attempt before launching so repeated retries cannot overlap it.
      _stateFlow.update { state ->
         state.copy(isLoading = true, loadFailure = null)
      }

      _observeJob = viewModelScope.launch {

         // Successful Room emissions keep the list current.
         // Result.failure ends this collector. Only RetryLoad starts a new observation.
         delay(_timing.delayRead)

         _repository.observeAll().collect { result ->
            result
               // Update the UI state with the list of people when the repository operation succeeded.
               .onSuccess { people ->
                  currentCoroutineContext().ensureActive()
                  _stateFlow.update { state ->
                     state.copy(people = people, isLoading = false, loadFailure = null)
                  }
               }

               // Update the UI state with an error message when the repository operation failed.
               .onFailure { throwable ->
                  if (throwable is CancellationException) throw throwable
                  currentCoroutineContext().ensureActive()
                  val error = _stringProvider.getString(R.string.error_people_observe)
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
         PeopleIntent.Create -> _effects.launchEmit(PeopleEffect.NavigateTo(null))
         is PeopleIntent.Detail -> _effects.launchEmit(PeopleEffect.NavigateTo(intent.personId))
         is PeopleIntent.RequestRemove -> requestRemove(intent.personId)
         is PeopleIntent.ConfirmRemove -> confirmRemove(intent.personId)
         PeopleIntent.RetryLoad ->
            if (!_stateFlow.value.isLoading && _stateFlow.value.loadFailure != null) {
               observePeople()
            }
      }
   }

   // A swipe asks for confirmation before changing the repository.
   private fun requestRemove(personId: String) {
      val person = _stateFlow.value.people.find { it.id == personId }
      if (person == null) {
         _effects.launchShowError(R.string.error_person_not_found)
         return
      }

      viewModelScope.launch {
         _effects.emit(PeopleEffect.ConfirmRemove(
            message = _stringProvider.getString(R.string.message_person_remove_confirm,
               person.firstName, person.lastName),
            actionLabel = _stringProvider.getString(R.string.action_confirm),
            personId = person.id,
         ))
      }
   }

   // The confirmation action performs the repository operation.
   private fun confirmRemove(personId: String) {
      val person = _stateFlow.value.people.find { it.id == personId }
      if (person == null) {
         _effects.launchShowError(R.string.error_person_not_found)
         return
      }
      remove(person)
   }

   // Removes a person from the repository.
   private fun remove(person: Person) {
      viewModelScope.launch {
         delay(_timing.delayWrite)

         _repository.remove(person)
            .onSuccess {
               _effects.showMessage(R.string.message_person_removed, person.fullName)
            }
            .onFailure { throwable ->
               _effects.showError(R.string.error_person_remove, throwable)
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
 * - EffectDispatcher übernimmt Aktionsmeldungen und deren Fehlerlogging wie
 *   in A3_02. Ladefehler bleiben lokal aufgelöster UiState.
 *   Die Effects transportieren weiterhin fertige Strings.
 *
 * - launchEmit startet die Navigation aus der Intent-Verarbeitung. Innerhalb
 *   laufender Operationen werden Effects suspendierend und ohne zusätzliche
 *   Versand-Coroutine gesendet. Der Delegate und die UI-Consumer bleiben gleich.
 *
 * Lernziele:
 *
 * - Gemeinsame Effect-Infrastruktur in mehreren ViewModels einsetzen.
 * - Dauerhafte Ladefehler von einmaligen Aktionsfehlern unterscheiden.
 * - Navigation als einmaligen Effect an den Backstack weitergeben.
 * - Fachliche Bestätigungs-/Undo-Effects von allgemeinen Meldungen unterscheiden.
 * - Bestehende Coroutine-Lebensdauern beim vereinheitlichten Versand erhalten.
 */
