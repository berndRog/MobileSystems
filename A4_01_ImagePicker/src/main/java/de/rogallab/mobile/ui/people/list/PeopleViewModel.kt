package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import de.rogallab.mobile.AppTiming
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.domain.IEffectSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeopleViewModel(
   private val _repository: IPersonRepository,
   private val _personUcDelete: PersonUcDelete,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<PeopleEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<PeopleEffect> by _effectDelegate {

   // StateFlow holds the persistent people list and load failure.
   private val _stateFlow = MutableStateFlow(PeopleUiState())
   val stateFlow: StateFlow<PeopleUiState> = _stateFlow.asStateFlow()

   // Job used to observe changes from the repository.
   private var _observeJob: Job? = null

   // Start the reactive observation pipeline
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
         is PeopleIntent.RequestRemove -> requestRemove(intent.personId)
         is PeopleIntent.ConfirmRemove -> confirmRemove(intent.personId)
         PeopleIntent.RetryLoad ->
            if (!_stateFlow.value.isLoading && _stateFlow.value.loadFailure != null) {
               observePeople()
            }
      }
   }

   // Observes the repository and updates the list state.
   private fun observePeople() {
      val previousJob = _observeJob

      // Mark the attempt before another Retry intent can arrive.
      _stateFlow.update { it.copy(isLoading = true, loadFailure = null) }

      _observeJob = viewModelScope.launch {
         // Wait until the previous collector has finished its cleanup.
         previousJob?.cancelAndJoin()
         currentCoroutineContext().ensureActive()

         delay(_timing.delayRead)

         _repository.observeAll().collect { result ->
            // Ignore emissions received after this collector was cancelled.
            currentCoroutineContext().ensureActive()

            result
               .onSuccess { people ->
                  _stateFlow.update {
                     it.copy(people = people, isLoading = false, loadFailure = null)
                  }
               }
               .onFailure { error ->
                  // Cancellation is control flow, not a user-visible error.
                  if (error is CancellationException) throw error
                  val message = _stringProvider.getString(R.string.error_people_observe)
                  Alog.e(TAG, message, error)
                  _stateFlow.update {
                     it.copy(isLoading = false, loadFailure = message)
                  }

                  // Stop this collector, not a newer job stored in the field.
                  currentCoroutineContext().cancel()
                  currentCoroutineContext().ensureActive()
               }
         }
      }
   }

   // Emits navigation as a one-time effect; AppNavigation updates the back stack.
   private fun navigateToPerson(personId: String?) {
      viewModelScope.launch {
         _effectDelegate.emit(PeopleEffect.NavigateTo(personId))
      }
   }

   // A swipe asks for confirmation before anything is deleted.
   private fun requestRemove(personId: String) {
      val person = findPerson(personId) ?: return
      viewModelScope.launch {
         _effectDelegate.emit(PeopleEffect.ConfirmRemove(
            message = _stringProvider.getString(
               R.string.message_person_remove_confirm, person.fullName
            ),
            actionLabel = _stringProvider.getString(R.string.action_confirm),
            personId = person.id,
         ))
      }
   }

   // The confirmed action passes only the id; the use case reloads the person.
   private fun confirmRemove(personId: String) {
      val person = findPerson(personId) ?: return
      viewModelScope.launch {
         delay(_timing.delayWrite)
         _personUcDelete(person.id)
            .onSuccess {
               currentCoroutineContext().ensureActive()
               _effectDelegate.emit(PeopleEffect.ShowMessage(
                  _stringProvider.getString(R.string.message_person_removed, person.fullName)
               ))
            }
            .onFailure { error ->
               if (error is CancellationException) throw error
               currentCoroutineContext().ensureActive()
               emitErrorNow(R.string.error_person_remove)
            }
      }
   }

   private fun findPerson(personId: String): Person? {
      val person = _stateFlow.value.people.find { it.id == personId }
      if (person == null) emitError(R.string.error_person_not_found)
      return person
   }

   private fun emitError(resourceId: Int) {
      viewModelScope.launch { emitErrorNow(resourceId) }
   }

   private suspend fun emitErrorNow(resourceId: Int) {
      _effectDelegate.emit(PeopleEffect.ShowError(_stringProvider.getString(resourceId)))
   }

   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A4_01_ImagePicker behält Swipe-to-Detail und Swipe-to-Delete bei, verzichtet
 *   aber bewusst auf den zusätzlichen Undo-Zustand. Dadurch bleibt der allgemeine
 *   ImagePicker-Schritt unabhängig von der aufwändigeren Undo-Mechanik.
 *
 * - Swipe-to-Delete erzeugt zunächst PeopleIntent.RequestRemove. Das Repository
 *   wird dabei noch nicht verändert. Stattdessen erzeugt das ViewModel
 *   PeopleEffect.ConfirmRemove mit Meldung, Action-Label und Person-ID.
 *
 * - Erst wenn die Action der Snackbar gewählt wurde, sendet die UI
 *   PeopleIntent.ConfirmRemove. Danach wird PersonUcDelete ausgeführt.
 *   Wird die Snackbar verworfen oder läuft sie ab, bleibt die Person unverändert.
 *
 * - Nach der Bestätigung delegiert PeopleViewModel die zusammengesetzte
 *   Operation an PersonUcDelete. Dieser entfernt zuerst den Datensatz und
 *   anschließend die zugehörige Bilddatei aus dem privaten App-Speicher.
 *
 * - Ein VisualRemovalDelegate, pending Removals und ein Restore-State sind in
 *   A4_01 deshalb nicht erforderlich. Diese Erweiterungen werden bewusst in
 *   A4_02_ImagePickerUndo eingeführt.
 *
 * Lernziele:
 *
 * - State, Intent und Effect weiterhin konsequent voneinander unterscheiden.
 * - Eine destruktive Aktion vor der Repository-Änderung bestätigen.
 * - Bestätigungs-Snackbar und Undo als unterschiedliche Konzepte verstehen.
 * - Den ImagePicker ohne zusätzliche Undo-Komplexität weiterverwenden.
 * - UI-Interaktion im ViewModel und Anwendungslogik im Use Case trennen.
 */
