package de.rogallab.mobile.ui.people.create_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.normalized
import de.rogallab.mobile.ui.people.toNullableInput
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonViewModel(
   val personId: String?,
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _validator: PersonValidator,
   private val _effectDelegate: EffectDelegate<PersonEffect>,
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   // Prepare messages through the same delegate exposed by IEffectSource.
   private val _effects = EffectDispatcher<PersonEffect>(
      _scope = viewModelScope,
      _effectDelegate = _effectDelegate,
      _stringProvider = _stringProvider,
      _errorEffect = { PersonEffect.ShowError(it) },
      _messageEffect = { PersonEffect.ShowMessage(it) },
      _tag = TAG,
   )

   // A null or blank id means that a new person is created.
   private val _personId = personId?.toNullableInput()

   // The initial state depends on whether a person is created or edited.
   private val _initialState =
      if (_personId == null) PersonUiState(isNew = true,  isLoading = false)
      else                   PersonUiState(isNew = false, isLoading = true)

   // StateFlow PersonUiState
   private val _stateFlow = MutableStateFlow(_initialState)
   val stateFlow = _stateFlow.asStateFlow()

   // For a detail screen load the existing person.
   init {
      if (!_stateFlow.value.isNew) loadPerson(_personId!!)
   }

   // Load existing person in detail mode.
   private fun loadPerson(id: String) {

      // Mark the attempt before launching so repeated retries cannot overlap it.
      _stateFlow.update { state ->
         state.copy(isLoading = true, loadFailure = null)
      }

      viewModelScope.launch {

         // Keep the existing delay used to demonstrate loading.
         delay(1000)

         // load person by id from repository
         _repository.findById(id) // Result<Person?> is returned

            // Handle successful repository result
            .onSuccess { person ->
               currentCoroutineContext().ensureActive()
               // Store the loaded person.
               if (person != null) {
                  Alog.d(TAG, "_repository.findById.onSuccess: $person")
                  _stateFlow.update { state ->
                     state.copy(person = person, loadFailure = null)
                  }
               }
               // A successful Result may still contain null if no person exists.
               else {
                  val error = _stringProvider.getString(R.string.error_person_not_found)
                  Alog.d(TAG, "_repository.findById.onSuccess (error): $error")
                  _stateFlow.update { state ->
                     state.copy(loadFailure = PersonLoadFailure.NotFound(error))
                  }
               }
            }

            // Handle repository failure returned as Result.failure
            .onFailure { throwable ->
               if (throwable is CancellationException) throw throwable
               currentCoroutineContext().ensureActive()
               val error = _stringProvider.getString(R.string.error_person_load)
               Alog.e(TAG, error, throwable)
               _stateFlow.update { state ->
                  state.copy(loadFailure = PersonLoadFailure.Failed(error))
               }
            }

         // Reset the loading flag regardless of success or failure.
         _stateFlow.update { state -> state.copy(isLoading = false) }
      } // launch
   }

   // Dispatcher: transform intent into an action
   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         is PersonIntent.FirstNameChange -> changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         is PersonIntent.EmailChange -> changeEmail(intent.email)
         is PersonIntent.PhoneChange -> changePhone(intent.phone)
         PersonIntent.Save -> save()
         PersonIntent.Cancel -> cancel()
         PersonIntent.RetryLoad ->
            // A3_02 has no Back action, so NotFound can also be retried.
            if (!_stateFlow.value.isNew && !_stateFlow.value.isLoading &&
               _stateFlow.value.loadFailure != null
            ) {
               loadPerson(_personId!!)
            }
      }
   }

   private fun changeFirstName(firstName: String) =
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(firstName = firstName))
      }

   private fun changeLastName(lastName: String) =
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(lastName = lastName))
      }

   private fun changeEmail(email: String) {
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(email = email.toNullableInput()))
      }
   }

   private fun changePhone(phone: String) {
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(phone = phone.toNullableInput()))
      }
   }

   // Validates and persists the current person.
   private fun save() {

      // Prevent multiple concurrent save operations.
      if (_stateFlow.value.isSaving || _stateFlow.value.isLoading ||
         _stateFlow.value.loadFailure != null) return

      // Publish the normalized snapshot even when validation fails.
      val person = _stateFlow.value.person.normalized()
      _stateFlow.update { state -> state.copy(person = person) }

      // Validate the complete person before accessing the repository.
      val error = _validator.validatePerson(person)
      if (error != null) {
         // Validation runs outside a coroutine; start only the message send.
         _effects.launchShowError(error)
         return
      }

      // Expose the active save to both the UI and the duplicate-save guard.
      _stateFlow.update { state -> state.copy(isSaving = true) }

      val job = viewModelScope.launch {
         currentCoroutineContext().ensureActive()
         // Create a new person or update the existing person
         val result =
            if (_stateFlow.value.isNew) _repository.create(person)
            else                        _repository.update(person)

         result
            // Handle the repository result and emit a one-time effect for success
            .onSuccess {
               // Reuse this coroutine so sending stays part of the save operation.
               _effects.showMessage(R.string.message_person_saved, person.fullName)
            }
            // Handle repository errors and emit a one-time effect for failure.
            .onFailure { throwable ->
               _effects.showError(R.string.error_person_save, throwable)
            }

      }
      // Release the guard on completion, including cancellation before dispatch.
      // Do not launch another coroutine: the ViewModel scope may be cancelled.
      job.invokeOnCompletion {
         _stateFlow.update { state -> state.copy(isSaving = false) }
      }
   }

   // Cancels editing
   private fun cancel() {
      if (_stateFlow.value.isSaving) return

      _stateFlow.update { state ->
         state.copy(
            person = state.person.copy(firstName = "", lastName = "",
               email = null, phone = null, imagePath = null)
         )
      }
   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}

/*
 * Didaktik und Lernziele
 *
 * Dieses ViewModel verwaltet zwei unterschiedliche Informationsarten:
 * dauerhaften UI-State und einmalige UI-Effects.
 *
 * - PersonUiState enthält den dauerhaft sichtbaren Zustand des Screens.
 *   Änderungen erfolgen konsequent in der Form:
 *
 *      _stateFlow.update { state: PersonUiState ->
 *         state.copy(...)
 *      }
 *
 *   Dadurch ist bereits am Lambda-Bezeichner erkennbar, welcher State
 *   verändert wird.
 *
 * - Ladefehler bleiben im UI-State sichtbar. Save-Erfolg, Save-Fehler und
 *   Validierungsfehler werden als einmalige PersonEffects ausgegeben.
 *   NotFound bietet in A3_02 ebenfalls Retry, weil ein Backstack fehlt.
 *   isSaving sperrt weitere Save- und Cancel-Aktionen bis zum Ergebnis.
 *
 * - EffectDispatcher löst Meldungsressourcen über IStringProvider auf und
 *   erzeugt die typisierten Effects. Vorhandene Validierungstexte werden
 *   unverändert übergeben. Der Dispatcher verwendet denselben EffectDelegate
 *   wie die nach außen delegierte IEffectSource-Schnittstelle.
 *
 * - launchShowError startet den Versand außerhalb einer Coroutine.
 *   showError und showMessage bleiben innerhalb der laufenden Coroutine.
 *   Technische Fehler werden mit Throwable einmal zentral protokolliert.
 *
 * - Ladefehler werden weiterhin hier lokalisiert und im UiState gespeichert.
 *   Sie lösen keinen zusätzlichen Meldungs-Effect aus.
 *
 * - Cancellation ist kein Benutzerfehler. Der Completion-Handler setzt nur
 *   isSaving zurück und führt weder Meldungsversand noch fachliche Aktionen aus.
 *
 * - IEffectSource<PersonEffect> wird mit "by _effectDelegate" delegiert.
 *   Channel und Flow müssen deshalb nicht in jedem ViewModel erneut
 *   implementiert werden.
 *
 * - A3_02 besitzt noch keinen Backstack. Speichern zeigt eine Meldung,
 *   Cancel setzt das Formular zurück.
 *
 * Lernziele:
 *
 * - State und einmalige Effects unterscheiden.
 * - Ressourcenmeldungen und bereits aufgelöste Validierungstexte unterscheiden.
 * - EffectDispatcher für Aufbereitung und EffectDelegate für Transport einsetzen.
 * - launch-Aufrufe von suspendierendem Versand unterscheiden.
 * - Implementierungsdelegation mit "by" verstehen.
 * - Fehlerbehandlung von konkreter UI-Darstellung entkoppeln.
 * - Ladefehler als State und Fehler einzelner Aktionen als Effects behandeln.
 */
