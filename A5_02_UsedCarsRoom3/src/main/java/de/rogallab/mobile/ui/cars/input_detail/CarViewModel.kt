package de.rogallab.mobile.ui.cars.input_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.io.deleteImageFromAppStorage
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.normalizedImagePaths
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.common.uiText
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CarViewModel(
   arguments: CarVmArgs,
   private val _carRepository: ICarRepository,
   private val _personRepository: IPersonRepository,
   private val _validator: CarValidator,
) : ViewModel() {
   private val _carId = arguments.carId?.takeUnless { carId ->
      carId.isBlank()
   }
   private val _isNew = _carId == null
   private val _state = MutableStateFlow(
      if (_isNew) {
         CarUiState(
            car = Car(id = UUID.randomUUID().toString()),
            isNew = true,
         )
      }
      else {
         CarUiState(isNew = false, isLoading = true)
      }
   )
   val state: StateFlow<CarUiState> = _state.asStateFlow()

   private val _events = Channel<CarEvent>(Channel.BUFFERED)
   val events = _events.receiveAsFlow()

   private var _originalImagePaths: Set<String> = emptySet()
   private var _imageOwnershipTransferred = false

   init {
      AppLogger.debug(TAG, "init: isNew=$_isNew, carId=$_carId")
      observePeople()
      if (!_isNew) loadCar()
   }

   fun onIntent(intent: CarIntent) {
      when (intent) {
         is CarIntent.ManufacturerChanged -> updateCar { car ->
            car.copy(manufacturer = intent.value)
         }
         is CarIntent.ModelChanged -> updateCar { car ->
            car.copy(model = intent.value)
         }
         is CarIntent.RegistrationYearChanged -> _state.update { currentState ->
            currentState.copy(registrationYearInput = intent.value)
         }
         is CarIntent.MileageChanged -> _state.update { currentState ->
            currentState.copy(mileageInput = intent.value)
         }
         is CarIntent.PriceChanged -> _state.update { currentState ->
            currentState.copy(priceInput = intent.value)
         }
         is CarIntent.SellerChanged -> updateCar { car ->
            car.copy(sellerId = intent.personId)
         }
         is CarIntent.ImagesAdded -> addImages(intent.imagePaths)
         is CarIntent.ImageRemoved -> removeImage(intent.imagePath)
         is CarIntent.ImageStorageFailed -> showSnackbar(intent.message)
         CarIntent.Save -> validateAndRequestSave()
         CarIntent.Cancel -> cancelEditing()
      }
   }

   private fun observePeople() {
      viewModelScope.launch {
         _personRepository.observeAll().collect { result ->
            result
               .onSuccess { people ->
                  _state.update { currentState ->
                     currentState.copy(people = people)
                  }
               }
               .onFailure {
                  showSnackbar(uiText(R.string.error_people_load))
               }
         }
      }
   }

   private fun loadCar() {
      val carId = _carId ?: return
      deleteUnsavedImages()

      viewModelScope.launch {
         _carRepository.findById(carId)
            .onSuccess { car ->
               if (car == null) {
                  _state.update { currentState ->
                     currentState.copy(isLoading = false)
                  }
                  showSnackbarAndNavigateBack(uiText(R.string.error_car_not_found))
               }
               else {
                  _originalImagePaths = car.imagePaths.toSet()
                  _imageOwnershipTransferred = false
                  _state.update { currentState ->
                     currentState.copy(
                        car = car,
                        registrationYearInput = car.registrationYear?.toString().orEmpty(),
                        mileageInput = car.mileage?.toString().orEmpty(),
                        priceInput = car.priceInEuro?.toString().orEmpty(),
                        isLoading = false,
                     )
                  }
               }
            }
            .onFailure {
               _state.update { currentState ->
                  currentState.copy(isLoading = false)
               }
               showSnackbarAndNavigateBack(uiText(R.string.error_car_load))
            }
      }
   }

   private fun updateCar(transform: (Car) -> Car) {
      _state.update { currentState ->
         val car = currentState.car ?: return@update currentState
         currentState.copy(car = transform(car))
      }
   }

   private fun addImages(imagePaths: List<String>) {
      if (imagePaths.isEmpty()) return
      updateCar { car ->
         val updatedImagePaths = (car.imagePaths + imagePaths)
            .normalizedImagePaths()
            .take(MAX_CAR_IMAGE_COUNT)

         val unusedImagePaths = imagePaths - updatedImagePaths.toSet()
         unusedImagePaths.forEach { unusedImagePath ->
            deleteImageFromAppStorage(unusedImagePath)
         }

         car.copy(imagePaths = updatedImagePaths)
      }
   }

   private fun removeImage(imagePath: String) {
      if (imagePath !in _originalImagePaths) {
         deleteImageFromAppStorage(imagePath)
      }
      updateCar { car ->
         car.copy(imagePaths = car.imagePaths - imagePath)
      }
   }

   private fun validateAndRequestSave() {
      val currentState = _state.value
      val car = currentState.car ?: return
      val normalizedCar = car.copy(
         manufacturer = car.manufacturer.trim(),
         model = car.model.trim(),
         registrationYear = currentState.registrationYearInput.trim().toIntOrNull(),
         mileage = currentState.mileageInput.trim().toIntOrNull(),
         priceInEuro = currentState.priceInput.trim().toIntOrNull(),
         imagePaths = car.imagePaths.normalizedImagePaths(),
      )
      val errorMessage = _validator.validateCar(
         normalizedCar,
         currentState.registrationYearInput,
         currentState.mileageInput,
         currentState.priceInput,
      )
      if (errorMessage != null) {
         showSnackbar(UiText.Resolved(errorMessage))
         return
      }

      _state.update { state -> state.copy(car = normalizedCar) }
      _imageOwnershipTransferred = true
      emitEvent(CarEvent.RequestSave(normalizedCar, _isNew))
   }

   private fun cancelEditing() {
      deleteUnsavedImages()
      emitEvent(CarEvent.NavigateBack)
   }

   private fun deleteUnsavedImages() {
      if (_imageOwnershipTransferred) return
      val currentImagePaths = _state.value.car?.imagePaths.orEmpty()
      currentImagePaths
         .filterNot { imagePath -> _originalImagePaths.contains(imagePath) }
         .forEach { imagePath ->
            deleteImageFromAppStorage(imagePath)
         }
   }

   private fun showSnackbar(message: UiText) {
      emitEvent(CarEvent.ShowSnackbar(message))
   }

   private fun showSnackbarAndNavigateBack(message: UiText) {
      viewModelScope.launch {
         _events.send(CarEvent.ShowSnackbar(message))
         _events.send(CarEvent.NavigateBack)
      }
   }

   private fun emitEvent(event: CarEvent) {
      viewModelScope.launch { _events.send(event) }
   }

   override fun onCleared() {
      deleteUnsavedImages()
      super.onCleared()
   }

   companion object {
      private const val TAG = "<-CarViewModel"
   }
}
