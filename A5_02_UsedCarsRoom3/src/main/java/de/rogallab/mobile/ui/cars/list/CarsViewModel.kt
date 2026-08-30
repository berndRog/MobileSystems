package de.rogallab.mobile.ui.cars.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import kotlinx.coroutines.Job
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

   init {
      observeCars()
      observePeople()
   }

   fun onIntent(intent: CarsIntent) {
      Alog.d(TAG, "intent: $intent")
      when (intent) {
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
            .onFailure { emitErrorNow(R.string.error_car_delete) }
      }
   }

   private fun observeCars() {
      _observeJob?.cancel()
      _observeJob = viewModelScope.launch {
         _stateFlow.update { state: CarsUiState -> state.copy(isLoading = true) }
         _repository.observeAll().collect { result: Result<List<Car>> ->
            result.onSuccess { cars ->
               _stateFlow.update { state: CarsUiState ->
                  state.copy(cars = cars, isLoading = false)
               }
            }.onFailure {
               _stateFlow.update { state: CarsUiState -> state.copy(isLoading = false) }
               emitErrorNow(R.string.error_cars_load)
            }
         }
      }
   }

   private fun observePeople() {
      viewModelScope.launch {
         _personRepository.observeAll().collect { result ->
            result.onSuccess { people ->
               _stateFlow.update { state: CarsUiState -> state.copy(people = people) }
            }.onFailure {
               emitErrorNow(R.string.error_people_load)
            }
         }
      }
   }

   private fun emitError(resourceId: Int) {
      viewModelScope.launch { emitErrorNow(resourceId) }
   }

   private suspend fun emitErrorNow(resourceId: Int) {
      _effectDelegate.emit(CarsEffect.ShowError(_stringProvider.getString(resourceId)))
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
