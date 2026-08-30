package de.rogallab.mobile.ui.cars.input_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import de.rogallab.mobile.shared.ui.images.IImageEdit
import de.rogallab.mobile.ui.people.create_detail.BackReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CarViewModel(
   val carId: String?,
   private val _carRepository: ICarRepository,
   private val _personRepository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _validator: CarValidator,
   private val _imageEdit: IImageEdit,
   private val _effectDelegate: EffectDelegate<CarEffect>,
) : ViewModel(), IEffectSource<CarEffect> by _effectDelegate {

   private val _carId = carId?.takeUnless(String::isBlank)
   private val _isNew = _carId == null
   private var _isSaving = false
   private val _stateFlow = MutableStateFlow(
      if (_isNew) CarUiState(car = Car(id = java.util.UUID.randomUUID().toString()), isNew = true)
      else CarUiState(isNew = false, isLoading = true)
   )
   val stateFlow: StateFlow<CarUiState> = _stateFlow.asStateFlow()

   init {
      observePeople()
      if (_isNew) _imageEdit.start(emptyList()) else loadCar(_carId!!)
   }

   fun onIntent(intent: CarIntent) {
      when (intent) {
         is CarIntent.ManufacturerChanged -> updateCar { it.copy(manufacturer = intent.value) }
         is CarIntent.ModelChanged -> updateCar { it.copy(model = intent.value) }
         is CarIntent.RegistrationYearChanged -> _stateFlow.update { state: CarUiState -> state.copy(registrationYearInput = intent.value) }
         is CarIntent.MileageChanged -> _stateFlow.update { state: CarUiState -> state.copy(mileageInput = intent.value) }
         is CarIntent.PriceChanged -> _stateFlow.update { state: CarUiState -> state.copy(priceInput = intent.value) }
         is CarIntent.SellerChanged -> updateCar { it.copy(sellerId = intent.personId) }
         is CarIntent.ImagesAdded -> addImages(intent.imagePaths)
         is CarIntent.ImageRemoved -> removeImage(intent.imagePath)
         is CarIntent.ImageStorageFailed -> showError(intent.message)
         CarIntent.Save -> save()
         CarIntent.Cancel -> cancel()
      }
   }

   private fun observePeople() {
      viewModelScope.launch {
         _personRepository.observeAll().collect { result ->
            result.onSuccess { people ->
               _stateFlow.update { state: CarUiState -> state.copy(people = people) }
            }.onFailure { showErrorNow(_stringProvider.getString(R.string.error_people_load)) }
         }
      }
   }

   private fun loadCar(id: String) {
      viewModelScope.launch {
         _carRepository.findById(id).onSuccess { car ->
            if (car == null) {
               _stateFlow.update { state: CarUiState -> state.copy(isLoading = false) }
               showErrorNow(_stringProvider.getString(R.string.error_car_not_found))
            } else {
               _imageEdit.start(car.imagePaths)
               _stateFlow.update { state: CarUiState ->
                  state.copy(
                     car = car,
                     registrationYearInput = car.registrationYear?.toString().orEmpty(),
                     mileageInput = car.mileage?.toString().orEmpty(),
                     priceInput = car.priceInEuro?.toString().orEmpty(),
                     isLoading = false,
                  )
               }
            }
         }.onFailure {
            _stateFlow.update { state: CarUiState -> state.copy(isLoading = false) }
            showErrorNow(_stringProvider.getString(R.string.error_car_load))
         }
      }
   }

   private fun updateCar(transform: (Car) -> Car) {
      _stateFlow.update { state: CarUiState ->
         state.car?.let { state.copy(car = transform(it)) } ?: state
      }
   }

   private fun addImages(imagePaths: List<String>) {
      viewModelScope.launch {
         var images = _imageEdit.add(imagePaths)
         if (images.size > MAX_CAR_IMAGE_COUNT) {
            images = _imageEdit.replace(images.take(MAX_CAR_IMAGE_COUNT))
         }
         updateCar { it.copy(imagePaths = images) }
      }
   }

   private fun removeImage(imagePath: String) {
      viewModelScope.launch {
         val images = _imageEdit.remove(imagePath)
         updateCar { it.copy(imagePaths = images) }
      }
   }

   private fun save() {
      if (_isSaving) return
      val state = _stateFlow.value
      val car = state.car ?: return
      val normalized = car.copy(
         manufacturer = car.manufacturer.trim(),
         model = car.model.trim(),
         registrationYear = state.registrationYearInput.trim().toIntOrNull(),
         mileage = state.mileageInput.trim().toIntOrNull(),
         priceInEuro = state.priceInput.trim().toIntOrNull(),
      )
      val error = _validator.validateCar(normalized, state.registrationYearInput, state.mileageInput, state.priceInput)
      if (error != null) { showError(error); return }
      _stateFlow.update { current: CarUiState -> current.copy(car = normalized) }
      _isSaving = true
      viewModelScope.launch {
         val result = if (_isNew) _carRepository.create(normalized) else _carRepository.update(normalized)
         result.onSuccess {
            _imageEdit.commit()
            _effectDelegate.emit(CarEffect.ShowMessage(_stringProvider.getString(R.string.message_car_saved, normalized.displayName)))
            _effectDelegate.emit(CarEffect.NavigateBack(BackReason.Save))
         }.onFailure {
            _effectDelegate.emit(CarEffect.ShowError(_stringProvider.getString(R.string.error_car_save)))
         }
         _isSaving = false
      }
   }

   private fun cancel() {
      viewModelScope.launch {
         _imageEdit.discard()
         _effectDelegate.emit(CarEffect.NavigateBack(BackReason.Cancel))
      }
   }
   private fun showError(message: String) { viewModelScope.launch { showErrorNow(message) } }
   private suspend fun showErrorNow(message: String) { _effectDelegate.emit(CarEffect.ShowError(message)) }
}
