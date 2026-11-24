package de.rogallab.mobile.ui.cars.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.readmodels.CarListItem
import de.rogallab.mobile.domain.readmodels.ICarReadModel
import de.rogallab.mobile.domain.usecases.CarUcDelete
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

class CarsViewModel(
   private val _readModel: ICarReadModel,
   private val _carUcDelete: CarUcDelete,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<CarsEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<CarsEffect> by _effectDelegate {

   private val _stateFlow = MutableStateFlow(CarsUiState())
   val stateFlow: StateFlow<CarsUiState> = _stateFlow.asStateFlow()
   private var _observeJob: Job? = null

   init {
      observeCars()
   }

   fun onIntent(intent: CarsIntent) {
      Alog.d(TAG, "intent: $intent")
      when (intent) {
         CarsIntent.RetryLoad ->
            if (!_stateFlow.value.isLoading && _stateFlow.value.loadFailure != null) {
               observeCars()
            }
         CarsIntent.Create -> navigateToCar(null)
         is CarsIntent.Detail -> navigateToCar(intent.carId)
         is CarsIntent.RequestRemove -> requestRemove(intent.carId)
         is CarsIntent.ConfirmRemove -> confirmRemove(intent.carId)
      }
   }

   // The read model flow emits again whenever Car or Person changes,
   // so seller names stay current without observing people separately.
   private fun observeCars() {
      val previousJob = _observeJob
      _stateFlow.update { it.copy(isLoading = true, loadFailure = null) }
      _observeJob = viewModelScope.launch {
         previousJob?.cancelAndJoin()
         try {
            delay(_timing.delayRead)
            _readModel.observeListItems().collect { result: Result<List<CarListItem>> ->
               currentCoroutineContext().ensureActive()
               result
                  .onSuccess { items ->
                     _stateFlow.update {
                        it.copy(cars = items, isLoading = false, loadFailure = null)
                     }
                  }
                  .onFailure { throwable ->
                     if (throwable is CancellationException) throw throwable
                     _stateFlow.update {
                        it.copy(isLoading = false,
                           loadFailure = _stringProvider.getString(R.string.error_cars_load))
                     }
                     currentCoroutineContext().cancel()
                     currentCoroutineContext().ensureActive()
                  }
            }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            _stateFlow.update {
               it.copy(isLoading = false,
                  loadFailure = _stringProvider.getString(R.string.error_cars_load))
            }
         }
      }
   }

   private fun navigateToCar(carId: String?) {
      viewModelScope.launch { _effectDelegate.emit(CarsEffect.NavigateTo(carId)) }
   }

   // A swipe asks for confirmation before anything is deleted.
   private fun requestRemove(carId: String) {
      val item = findItem(carId) ?: return
      viewModelScope.launch {
         val message = _stringProvider.getString(
            R.string.message_car_remove_confirm,
            item.displayName,
         )
         val actionLabel = _stringProvider.getString(R.string.action_confirm)
         _effectDelegate.emit(CarsEffect.ConfirmRemove(message, actionLabel, item.id))
      }
   }

   // The confirmed action passes only the id; the use case loads the aggregate.
   private fun confirmRemove(carId: String) {
      val item = findItem(carId) ?: return
      viewModelScope.launch {
         delay(_timing.delayWrite)

         _carUcDelete(item.id)
            .onFailure { throwable ->
               Alog.e(TAG, "remove failed", throwable)
               emitErrorNow(R.string.error_car_delete)
            }
         // On success the read model flow emits the list without this car.
      }
   }

   private fun findItem(carId: String): CarListItem? {
      val item = _stateFlow.value.cars.find { it.id == carId }
      if (item == null) emitError(R.string.error_car_not_found)
      return item
   }

   private fun emitError(resourceId: Int) {
      viewModelScope.launch { emitErrorNow(resourceId) }
   }

   private suspend fun emitErrorNow(resourceId: Int) {
      _effectDelegate.emit(CarsEffect.ShowError(_stringProvider.getString(resourceId)))
   }

   companion object { private const val TAG = "<-CarsViewModel" }
}
