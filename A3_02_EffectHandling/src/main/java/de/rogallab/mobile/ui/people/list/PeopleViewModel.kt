package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class PeopleViewModel(
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<PeopleEffect>,
) : ViewModel(), IEffectSource<PeopleEffect> by _effectDelegate {

   // Message preparation and transport share one feature-specific delegate.
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

         delay(1000)

         // Successful Room emissions keep the list current.
         // Result.failure ends this collector. Only RetryLoad starts a new observation.
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
         PeopleIntent.Create -> {}     //navigateToPerson(null)
         is PeopleIntent.Detail -> {}  //navigateToPerson(intent.personId)
         is PeopleIntent.Remove -> remove(intent.person)
         PeopleIntent.RetryLoad ->
            if (!_stateFlow.value.isLoading && _stateFlow.value.loadFailure != null) {
               observePeople()
            }
      }
   }

   // Removes a person from the repository.
   private fun remove(person: Person) {
      viewModelScope.launch {
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
 * - Create und Detail bleiben in A3_02 ohne Navigationsfunktion; A3_03
 *   ergänzt den Backstack.
 *
 * - EffectDispatcher löst Ressourcen für Aktionsmeldungen auf, erzeugt
 *   typisierte Effects und protokolliert übergebene Fehler einmal.
 *   Die Effects transportieren weiterhin fertige Strings.
 *
 * - Ladefehler werden weiterhin im ViewModel lokalisiert, protokolliert und
 *   im UiState gespeichert. Cancellation wird nicht als Ladefehler behandelt.
 *
 * - showError und showMessage verwenden die bereits laufende Lösch-Coroutine.
 *   Ein zusätzlicher launch-Aufruf würde den Versand vom Vorgang lösen.
 *   Dispatcher und IEffectSource verwenden denselben EffectDelegate.
 *
 * Lernziele:
 *
 * - Gemeinsame Effect-Infrastruktur in mehreren ViewModels einsetzen.
 * - Dauerhafte Ladefehler von einmaligen Aktionsfehlern unterscheiden.
 * - Effects ohne Navigation kennenlernen.
 * - Meldungsaufbereitung im Dispatcher vom Transport im Delegate unterscheiden.
 * - Suspendierend innerhalb einer bestehenden Coroutine senden.
 */
