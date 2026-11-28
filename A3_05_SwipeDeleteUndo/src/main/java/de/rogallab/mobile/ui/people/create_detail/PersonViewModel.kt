package de.rogallab.mobile.ui.people.create_detail

import androidx.lifecycle.ViewModel
import de.rogallab.mobile.AppTiming
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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
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
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   // Use the same delegate for message preparation and the exposed effect flow.
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

   private val _initialState =
      if (_personId == null) PersonUiState(isNew = true, isLoading = false)
      else PersonUiState(isNew = false, isLoading = true)

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

         // load person by id from repository
         delay(_timing.delayRead)

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
      }
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
            if (!_stateFlow.value.isNew && !_stateFlow.value.isLoading &&
               _stateFlow.value.loadFailure is PersonLoadFailure.Failed
            ) {
               loadPerson(_personId!!)
            }
      }
   }

   private fun changeFirstName(firstName: String) =
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(firstName = firstName.trim()))
      }

   private fun changeLastName(lastName: String) =
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(lastName = lastName.trim()))
      }

   private fun changeEmail(email: String) {
      var emailNullable: String? = email.toNullableInput()
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(email = emailNullable))
      }
   }

   private fun changePhone(phone: String) {
      var phoneNullable: String? = phone.toNullableInput()
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(phone = phoneNullable))
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
         _effects.launchShowError(error)
         return
      }

      // Expose the active save to both the UI and the duplicate-save guard.
      _stateFlow.update { state -> state.copy(isSaving = true) }

      val job = viewModelScope.launch {
         currentCoroutineContext().ensureActive()

         // Create a new person or update the existing person
         delay(_timing.delayWrite)

         val result =
            if (_stateFlow.value.isNew) _repository.create(person)
            else                        _repository.update(person)

         result
            // Handle the repository result and emit a one-time effect for success
            .onSuccess {
               // Keep confirmation and navigation sequential in this save coroutine.
               _effects.showMessage(R.string.message_person_saved, person.fullName)
               _effects.emit(PersonEffect.NavigateBack(BackReason.Save))
            }
            // Handle repository errors and emit a one-time effect for failure.
            .onFailure { throwable ->
               _effects.showError(R.string.error_person_save, throwable)
            }

      }
      // Only reset the guard here, including cancellation during either send.
      // The existing save coroutine still owns the message and navigation.
      job.invokeOnCompletion {
         _stateFlow.update { state -> state.copy(isSaving = false) }
      }
   }

   // Cancels editing
   private fun cancel() {
      if (_stateFlow.value.isSaving) return

      _stateFlow.update { state ->
         state.copy(
            person = state.person.copy(firstName = "", lastName = "")
         )
      }
      _effects.launchEmit(PersonEffect.NavigateBack(BackReason.Cancel))
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
 *      _stateFlow.update { state ->
 *         state.copy(...)
 *      }
 *
 *   Dadurch ist bereits am Lambda-Bezeichner erkennbar, welcher State
 *   verändert wird.
 *
 * - Ladefehler bleiben im UI-State sichtbar. Save-Erfolg, Save-Fehler und
 *   Validierungsfehler werden als einmalige PersonEffects ausgegeben.
 *   NotFound führt in A3_03 zurück; Repository-Fehler bieten Retry.
 *   isSaving sperrt weitere Save- und Cancel-Aktionen bis zum Ergebnis.
 *
 * - EffectDispatcher bereitet Meldungen auf und sendet über denselben Delegate,
 *   der die IEffectSource-Schnittstelle bereitstellt. Ressourcen für Meldungen
 *   und deren Fehlerlogging sind damit wie in A3_02 zentralisiert.
 *
 * - launchShowError sendet Validierungstexte außerhalb einer Coroutine.
 *   showMessage und emit senden Save-Meldung und Rücknavigation nacheinander
 *   innerhalb derselben Coroutine. Cancel verwendet launchEmit.
 *
 * - Ladefehler bleiben hier lokalisierter UiState. Cancellation wird nicht als
 *   Benutzerfehler behandelt. Der Completion-Handler setzt nur isSaving zurück;
 *   er erzeugt weder Effects noch eine zusätzliche Coroutine.
 *
 * - IEffectSource<PersonEffect> wird mit "by _effectDelegate" delegiert.
 *   Channel und Flow müssen deshalb nicht in jedem ViewModel erneut
 *   implementiert werden.
 *
 * - Save und Cancel erzeugen unterschiedliche NavigateBack-Effects für den
 *   Navigation-3-Backstack und dessen Übergangsanimationen.
 *
 * Lernziele:
 *
 * - State und einmalige Effects unterscheiden.
 * - Meldungsaufbereitung im Dispatcher und Transport im Delegate unterscheiden.
 * - launch-Aufrufe und suspendierenden Versand passend zum Aufrufer verwenden.
 * - Die Reihenfolge von Speicherbestätigung und Navigation erhalten.
 * - Implementierungsdelegation mit "by" verstehen.
 * - Fehlerbehandlung von konkreter UI-Darstellung entkoppeln.
 * - Ladefehler als State und Fehler einzelner Aktionen als Effects behandeln.
 */
