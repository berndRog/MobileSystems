package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import de.rogallab.mobile.AppTiming
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.domain.IEffectSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class PeopleViewModel(
   private val _repository: IPersonRepository,
   private val _personUcDelete: PersonUcDelete,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<PeopleEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<PeopleEffect> by _effectDelegate {

   // StateFlow holds the persistent people list and load failure.
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
         try {
            // A failed Result or a thrown Flow exception ends this observation.
            delay(_timing.delayRead)

            _repository.observeAll().collect { result ->
               val people = result.getOrThrow()
               _stateFlow.update { state ->
                  state.copy(people = people, isLoading = false, loadFailure = null)
               }
            }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Throwable) {
            // Keep the failure visible until RetryLoad starts a new collector.
            val error = _stringProvider.getString(R.string.error_people_observe)
            Alog.e(TAG, error, e)
            _stateFlow.update { state -> state.copy(loadFailure = error) }
         } finally {
            // Release the spinner even if the Flow fails or is cancelled.
            _stateFlow.update { state -> state.copy(isLoading = false) }
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

   // Removes a person through the image-aware use case.
   private fun remove(person: Person) {
      viewModelScope.launch {
         try {
            delay(_timing.delayWrite)

            _personUcDelete(person).getOrThrow()
            val message = _stringProvider.getString(
               R.string.message_person_removed, person.fullName)
            _effectDelegate.emit(PeopleEffect.ShowMessage(message))
         } catch (e: CancellationException) {
            throw e
         } catch (e: Throwable) {
            // Convert unexpected delete failures into the existing error effect.
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
 * - A5_01_PeopleRoom3 übernimmt den UDF-Ablauf der vorherigen Beispiele
 *   unverändert: Die UI sendet Intents, PeopleViewModel aktualisiert den State
 *   beziehungsweise erzeugt einmalige Effects.
 *
 * - Swipe-to-Delete verwendet weiterhin die einfache Bestätigung aus A4_01.
 *   RequestRemove verändert die Datenbank noch nicht. Das ViewModel erzeugt
 *   zunächst ConfirmRemove mit Meldung, Action-Label und Person-ID.
 *
 * - Erst wenn die Action der Snackbar gewählt wurde, sendet die UI
 *   PeopleIntent.ConfirmRemove. Danach wird PersonUcDelete ausgeführt.
 *   Wird die Snackbar verworfen oder läuft sie ab, bleibt die Person erhalten.
 *
 * - PersonUcDelete entfernt zuerst den Room-Datensatz und anschließend die
 *   zugehörige Bilddatei. Das ViewModel koordiniert diese Datenquellen nicht
 *   mehr selbst.
 *
 * - Neu ist nicht das ViewModel, sondern die Implementierung hinter
 *   IPersonRepository: PersonRepository greift in A5_01 auf das lokale
 *   Room-3-DAO zu. Dadurch bleibt die UI unabhängig von der Persistenztechnik.
 *
 * - A5_01 verwendet bewusst keinen VisualRemovalDelegate und kein Undo. Die
 *   komplexere Undo-Variante bleibt als eigener Lernschritt in A4_02 sichtbar.
 *
 * Lernziele:
 *
 * - Eine bestehende UI gegen eine neue Persistenzimplementierung weiterverwenden.
 * - Repository als Grenze zwischen ViewModel und Room verstehen.
 * - Delete-Bestätigung und persistente Löschung klar voneinander trennen.
 * - UI-Interaktion im ViewModel und Anwendungslogik im Use Case trennen.
 */
