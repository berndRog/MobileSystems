package de.rogallab.mobile.ui.cars.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.Globals
import de.rogallab.mobile.Globals.delay
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.data.network.userMessageOr
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

class CarsViewModel(
   private val _repository: ICarRepository,
   private val _personRepository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<CarsEffect>,
) : ViewModel(), IEffectSource<CarsEffect> by _effectDelegate {

   private val _stateFlow = MutableStateFlow(CarsUiState())
   val stateFlow: StateFlow<CarsUiState> = _stateFlow.asStateFlow()
   private var _observeJob: Job? = null
   private var _peopleJob: Job? = null

   init {
      observeCars()
      observePeople()
   }

   fun onIntent(intent: CarsIntent) {
      Alog.d(TAG, "intent: $intent")
      when (intent) {
         CarsIntent.RetryLoad -> { observeCars(); observePeople() }
         CarsIntent.Create -> navigateToCar(null)
         is CarsIntent.Detail -> navigateToCar(intent.carId)
         is CarsIntent.RequestRemove -> requestRemove(intent.carId)
         is CarsIntent.ConfirmRemove -> confirmRemove(intent.carId)
      }
   }

   private fun navigateToCar(carId: String?) {
      viewModelScope.launch { _effectDelegate.emit(CarsEffect.NavigateTo(carId)) }
   }

   private fun requestRemove(carId: String) {
      val car = _stateFlow.value.cars.find { it.id == carId }
      if (car == null) {
         emitError(R.string.error_car_not_found)
         return
      }
      viewModelScope.launch {
         val message = _stringProvider.getString(
            R.string.message_car_remove_confirm,
            car.manufacturer,
            car.model,
         )
         val actionLabel = _stringProvider.getString(R.string.action_confirm)
         _effectDelegate.emit(CarsEffect.ConfirmRemove(message, actionLabel, car.id))
      }
   }

   private fun confirmRemove(carId: String) {
      val car = _stateFlow.value.cars.find { it.id == carId }
      if (car == null) {
         emitError(R.string.error_car_not_found)
         return
      }
      viewModelScope.launch {
         _repository.remove(car)
            .onFailure { throwable ->
               emitErrorNow(R.string.error_car_delete, throwable)
            }
      }
   }

   private fun observeCars() {
      _observeJob?.cancel()
      _stateFlow.update { state: CarsUiState -> state.copy(isLoading = true, carsLoadError = null) }
      _observeJob = viewModelScope.launch {
         delay(Globals.delay)
         _repository.observeAll().collect { result: Result<List<Car>> ->
            result.onSuccess { cars ->
               _stateFlow.update { state: CarsUiState ->
                  state.copy(cars = cars, isLoading = false, carsLoadError = null)
               }
            }.onFailure { throwable ->
               val fallback = _stringProvider.getString(R.string.error_cars_load)
               val error = throwable.userMessageOr(fallback)
               Alog.e("<-CarsViewModel", error, throwable)
               _stateFlow.update { state: CarsUiState ->
                  state.copy(isLoading = false, carsLoadError = error)
               }
            }
         }
      }
   }

   private fun observePeople() {
      _peopleJob?.cancel()
      _stateFlow.update { state: CarsUiState -> state.copy(peopleLoadError = null) }
      _peopleJob = viewModelScope.launch {
         delay(Globals.delay)
         _personRepository.observeAll().collect { result ->
            result.onSuccess { items ->
               _stateFlow.update { state: CarsUiState -> state.copy(people = items, peopleLoadError = null) }
            }.onFailure { throwable ->
               val fallback = _stringProvider.getString(R.string.error_people_load)
               val error = throwable.userMessageOr(fallback)
               Alog.e("<-CarsViewModel", error, throwable)
               _stateFlow.update { state: CarsUiState -> state.copy(peopleLoadError = error) }
            }
         }
      }
   }

   private fun emitError(resourceId: Int) {
      viewModelScope.launch { emitErrorNow(resourceId) }
   }

   private suspend fun emitErrorNow(
      resourceId: Int,
      throwable: Throwable? = null,
   ) {
      val fallback = _stringProvider.getString(resourceId)
      val message = throwable?.userMessageOr(fallback) ?: fallback
      _effectDelegate.emit(CarsEffect.ShowError(message))
   }

   companion object { private const val TAG = "<-CarsViewModel" }
}

/*
 * Didaktik und Lernziele
 *
 * - CarsViewModel verwendet denselben UDF-Ablauf wie PeopleViewModel.
 * - Swipe-to-Delete entfernt kein Listenelement vorzeitig. Erst die bestätigte
 *   Snackbar-Aktion führt über ConfirmRemove zur Repository-Operation.
 * - Die Verkäuferliste wird separat beobachtet, damit die UI sellerId auf einen
 *   lesbaren Personennamen abbilden kann.
 */
