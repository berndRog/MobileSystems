package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.readmodels.PersonListItem
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class PeopleViewModel(
   private val _readModel: IPersonReadModel,
   private val _personUcDelete: PersonUcDelete,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<PeopleEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<PeopleEffect> by _effectDelegate {

   private val _stateFlow = MutableStateFlow(PeopleUiState())
   val stateFlow: StateFlow<PeopleUiState> = _stateFlow.asStateFlow()
   private var _observeJob: Job? = null

   init {
      observePeople()
   }

   fun onIntent(intent: PeopleIntent) {
      Alog.d(TAG, "intent: $intent")
      when (intent) {
         PeopleIntent.RetryLoad ->
            if (!_stateFlow.value.isLoading && _stateFlow.value.loadFailure != null) {
               observePeople()
            }
         PeopleIntent.Create -> navigateToPerson(null)
         is PeopleIntent.Detail -> navigateToPerson(intent.personId)
         is PeopleIntent.RequestRemove -> requestRemove(intent.personId)
         is PeopleIntent.ConfirmRemove -> confirmRemove(intent.personId)
      }
   }

   // Publish failures before cleanup; Retry waits for the previous collector.
   private fun observePeople() {
      val previousJob = _observeJob

      // Mark the attempt before another Retry intent can arrive.
      _stateFlow.update { it.copy(isLoading = true, loadFailure = null) }

      _observeJob = viewModelScope.launch {

         // Wait until the previous collector has finished its cleanup
         previousJob?.cancelAndJoin()
         currentCoroutineContext().ensureActive()

         try {

            delay(_timing.delayRead)

            _readModel.observeListItems().collect { result: Result<List<PersonListItem>> ->
               // Ignore emissions received after this collector was cancelled.
               currentCoroutineContext().ensureActive()

               result
                  .onSuccess { items: List<PersonListItem> ->
                     _stateFlow.update {
                        it.copy(listItems = items, isLoading = false, loadFailure = null)
                     }
                  }
                  .onFailure { throwable ->
                     if (throwable is CancellationException) throw throwable
                     _stateFlow.update {
                        it.copy(isLoading = false,
                           loadFailure = _stringProvider.getString(R.string.error_people_observe))
                     }
                     // Cancel the currently running observation coroutine.
                     currentCoroutineContext().cancel()
                     currentCoroutineContext().ensureActive()
                  }
            }
         }
         catch (e: CancellationException) { throw e }

         catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            _stateFlow.update {
               it.copy(isLoading = false,
                  loadFailure = _stringProvider.getString(R.string.error_people_observe))
            }
         }
      }
   }

   private fun navigateToPerson(personId: String?) {
      viewModelScope.launch {
         _effectDelegate.emit(PeopleEffect.NavigateTo(personId))
      }
   }

   // A swipe asks for confirmation before anything is deleted.
   private fun requestRemove(personId: String) {
      val item = findPerson(personId) ?: return
      viewModelScope.launch {
         val message = _stringProvider.getString(
            R.string.message_person_remove_confirm,
            item.fullName,
         )
         val actionLabel = _stringProvider.getString(R.string.action_confirm)
         _effectDelegate.emit(PeopleEffect.ConfirmRemove(message, actionLabel, item.id))
      }
   }

   // The confirmed action passes only the id; the use case reloads the person.
   private fun confirmRemove(personId: String) {
      val item = findPerson(personId) ?: return
      viewModelScope.launch {

         try {
            delay(_timing.delayWrite)

            _personUcDelete(item.id).getOrThrow()

            // Ensure that the coroutine is still active.
            currentCoroutineContext().ensureActive()

            // Emit a success message.
            _effectDelegate.emit(PeopleEffect.ShowMessage(
               _stringProvider.getString(R.string.message_person_removed, item.fullName)
            ))
         }

         // Preserve cooperative coroutine cancellation.
         catch(e: CancellationException) { throw e }

         // Handle Result failures and directly thrown exceptions.
         catch(e: Exception) {
            if (e is CancellationException) throw e
            currentCoroutineContext().ensureActive()

            Alog.e(TAG, "remove failed", e)
            _effectDelegate.emit(PeopleEffect.ShowError(
               _stringProvider.getString(R.string.error_person_remove)
            ))
         }
      }
   }

   private fun findPerson(personId: String): PersonListItem? {
      val item = _stateFlow.value.listItems.find { it.id == personId }
      if (item == null) viewModelScope.launch{
         _effectDelegate.emit(PeopleEffect.ShowMessage(
            _stringProvider.getString(R.string.error_person_not_found)
         ))
      }
      return item
   }



   companion object { private const val TAG = "<-PeopleViewModel" }
}