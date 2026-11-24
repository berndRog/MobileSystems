package de.rogallab.mobile.ui.cars.input_detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonSelectItem
import de.rogallab.mobile.domain.repositories.ICarRepository
import de.rogallab.mobile.domain.usecases.CarUcCreate
import de.rogallab.mobile.domain.usecases.CarUcUpdate
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.cars.input_detail.comp.CarForm
import de.rogallab.mobile.ui.people.create_detail.BackReason
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
import de.rogallab.mobile.shared.R as SharedR

class CarViewModel(
   val carId: String?,
   private val _carRepository: ICarRepository,
   private val _personReadModel: IPersonReadModel,
   private val _stringProvider: IStringProvider,
   private val _validator: CarValidator,
   private val _imageFileStorage: IImageFileStorage,
   private val _imageEdit: IImageEdit,
   private val _carUcCreate: CarUcCreate,
   private val _carUcUpdate: CarUcUpdate,
   private val _effectDelegate: EffectDelegate<CarEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<CarEffect> by _effectDelegate {

   private val _carId = carId?.takeUnless(String::isBlank)
   private val _isNew = _carId == null
   private var _isSaving = false
   private var _loadJob: Job? = null
   private var _peopleJob: Job? = null

   // A new car starts with an empty form; an existing car is loaded first.
   private val _initialState =
      if (_isNew) CarUiState(form = CarForm(), isNew = true)
      else CarUiState(isNew = false, isLoading = true)

   private val _stateFlow = MutableStateFlow(_initialState)
   val stateFlow: StateFlow<CarUiState> = _stateFlow.asStateFlow()

   init {
      observePeople()
      if (_isNew) _imageEdit.start(emptyList())
      else loadCar(_carId!!)
   }

   // Loads the seller selection through the person read model.
   // Only IPerson... types access the Person table.
   private fun observePeople() {
      val previousJob = _peopleJob
      previousJob?.cancel()
      // Set loading before launch: repeated Retry intents cannot start another attempt.
      _stateFlow.update { it.copy(isPeopleLoading = true, peopleLoadFailure = null) }
      _peopleJob = viewModelScope.launch {
         previousJob?.cancelAndJoin()
         try {
            delay(_timing.delayRead)
            _personReadModel.observeSelectItems().collect { result: Result<List<PersonSelectItem>> ->
               currentCoroutineContext().ensureActive()
               result
                  .onSuccess { items ->
                     _stateFlow.update {
                        it.copy(people = items, isPeopleLoading = false, peopleLoadFailure = null)
                     }
                  }
                  .onFailure { throwable ->
                     currentCoroutineContext().ensureActive()
                     if (throwable is CancellationException) throw throwable
                     _stateFlow.update {
                        it.copy(isPeopleLoading = false,
                           peopleLoadFailure = _stringProvider.getString(R.string.error_people_load))
                     }
                     // Stop this collector, not a possibly newer job stored in the field.
                     currentCoroutineContext().cancel()
                     currentCoroutineContext().ensureActive()
                  }
            }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            // Also handle a Flow that throws instead of emitting Result.failure.
            _stateFlow.update {
               it.copy(isPeopleLoading = false,
                  peopleLoadFailure = _stringProvider.getString(R.string.error_people_load))
            }
         }
      }
   }

   private fun loadCar(id: String) {
      val previousJob = _loadJob
      previousJob?.cancel()
      _stateFlow.update { it.copy(isLoading = true, loadFailure = null) }
      _loadJob = viewModelScope.launch {
         previousJob?.cancelAndJoin()
         try {
            delay(_timing.delayRead)
            _carRepository.findById(id)
               .onSuccess { car ->
                  currentCoroutineContext().ensureActive()
                  if (car == null) {
                     _stateFlow.update {
                        it.copy(isLoading = false, loadFailure = CarLoadFailure.NotFound(
                           _stringProvider.getString(R.string.error_car_not_found)))
                     }
                  } else {
                     _imageEdit.start(car.imagePaths)
                     _stateFlow.update {
                        it.copy(form = car.toCarForm(),
                           isLoading = false, loadFailure = null)
                     }
                  }
               }
               .onFailure { throwable ->
                  currentCoroutineContext().ensureActive()
                  if (throwable is CancellationException) throw throwable
                  _stateFlow.update {
                     it.copy(isLoading = false, loadFailure = CarLoadFailure.Failed(
                        _stringProvider.getString(R.string.error_car_load)))
                  }
               }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            _stateFlow.update {
               it.copy(isLoading = false, loadFailure = CarLoadFailure.Failed(
                  _stringProvider.getString(R.string.error_car_load)))
            }
         }
      }
   }

   fun onIntent(intent: CarIntent) {
      when (intent) {
         is CarIntent.ManufacturerChanged ->
            updateForm { form -> form.copy(manufacturer = intent.value) }

         is CarIntent.ModelChanged ->
            updateForm { form -> form.copy(model = intent.value) }

         is CarIntent.PriceChanged ->
            updateForm { form -> form.copy(priceInput = intent.value) }

         is CarIntent.SellerChanged ->
            updateForm { form -> form.copy(personId = intent.personId) }

         is CarIntent.GalleryImagesSelected ->
            storeGalleryImages(intent.sourceUris)

         is CarIntent.CameraImageTaken ->
            storeCameraImage(intent.imagePath)

         is CarIntent.ImageRemoved ->
            removeImage(intent.imagePath)

         is CarIntent.ImageFailed ->
            showError(intent.message)

         CarIntent.RetryLoad ->
            if (!_isNew && !_stateFlow.value.isLoading &&
               _stateFlow.value.loadFailure is CarLoadFailure.Failed) {
               loadCar(_carId!!)
            }
         CarIntent.RetryPeople ->
            if (!_stateFlow.value.isPeopleLoading && _stateFlow.value.peopleLoadFailure != null) {
               observePeople()
            }
         CarIntent.Save -> save()
         CarIntent.Cancel -> cancel()
      }
   }



   // Raw input is stored unchanged; normalization happens on Save.
   private fun updateForm(transform: (CarForm) -> CarForm) {
      _stateFlow.update { state: CarUiState -> state.copy(form = transform(state.form)) }
   }

   // Copies Photo Picker Uris with the shared storage service. The UI never
   // turns external Uris into persistent entity data itself.
   private fun storeGalleryImages(sourceUris: List<Uri>) {
      if (sourceUris.isEmpty()) return

      viewModelScope.launch {
         val imagePaths = mutableListOf<String>()
         var copyFailed = false

         sourceUris.forEach { sourceUri ->
            _imageFileStorage
               .copyImageToAppStorage(sourceUri)
               .onSuccess { imagePath -> imagePaths += imagePath }
               .onFailure { copyFailed = true }
         }

         if (imagePaths.isNotEmpty()) {
            addImages(imagePaths)
         }

         if (copyFailed) {
            showErrorNow(
               _stringProvider.getString(SharedR.string.error_image_save)
            )
         }
      }
   }

   // CameraPickerHandler has already created and confirmed the private file.
   private fun storeCameraImage(imagePath: String) {
      viewModelScope.launch {
         addImages(listOf(imagePath))
      }
   }

   // IImageEdit owns the technical edit-session rules. If more images are
   // returned than the feature allows, replace() also cleans transient overflow.
   private suspend fun addImages(imagePaths: List<String>) {
      var images = _imageEdit.add(imagePaths)
      if (images.size > MAX_CAR_IMAGE_COUNT) {
         images = _imageEdit.replace(images.take(MAX_CAR_IMAGE_COUNT))
      }

      updateForm { form -> form.copy(imagePaths = images) }
   }

   private fun removeImage(imagePath: String) {
      viewModelScope.launch {
         val images = _imageEdit.remove(imagePath)
         updateForm { form -> form.copy(imagePaths = images) }
      }
   }

   private fun save() {
      if (_isSaving || _stateFlow.value.isLoading || _stateFlow.value.loadFailure != null) return

      // Normalize all form values and show them in the UI as well.
      val form: CarForm = _stateFlow.value.form.normalized()
      _stateFlow.update { state: CarUiState -> state.copy(form = form) }

      // Validate the form before creating a domain object.
      val error = _validator.validateCar(form)
      if (error != null) {
         showError(error)
         return
      }

      // Only a valid form becomes a domain car.
      val car: Car = form.toCar()
      _isSaving = true

      viewModelScope.launch {
         try {
            delay(_timing.delayWrite)

            // The use cases save the car and commit the image edit session.
            if (_isNew) _carUcCreate(car).getOrThrow()
            else _carUcUpdate(car).getOrThrow()

            _effectDelegate.emit(
               CarEffect.ShowMessage(
                  _stringProvider.getString(R.string.message_car_saved, car.displayName)
               )
            )
            _effectDelegate.emit(CarEffect.NavigateBack(BackReason.Save))
         }
         catch (e: CancellationException) {
            // Coroutine cancellation must be propagated.
            throw e
         }
         catch (e: Exception) {
            // A failed write does not commit the image session,
            // so the user can retry or cancel.
            showErrorNow(_stringProvider.getString(R.string.error_car_save))
         }
         finally {
            // Reset in every case, including cancellation.
            _isSaving = false
         }
      }
   }

   private fun cancel() {
      if (_isSaving) return
      viewModelScope.launch {
         _imageEdit.discard()
         _effectDelegate.emit(
            CarEffect.NavigateBack(BackReason.Cancel)
         )
      }
   }

   private fun showError(message: String) {
      viewModelScope.launch {
         showErrorNow(message)
      }
   }

   private suspend fun showErrorNow(message: String) {
      _effectDelegate.emit(CarEffect.ShowError(message))
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A5_02 verwendet für Fahrzeugbilder dieselbe Shared-Infrastruktur wie die
 *   vorherigen ImagePicker-Beispiele. Das Feature implementiert keine eigene
 *   Datei- oder Picker-Schicht.
 *
 * - GalleryPickerHandlerMultiple liefert Content-Uris. Erst das ViewModel kopiert diese
 *   über IImageFileStorage in den privaten App-Speicher.
 *
 * - CameraPickerHandler liefert dagegen bereits einen bestätigten internen
 *   Dateipfad. Beide Wege enden anschließend in derselben IImageEdit-Session.
 *
 * - IImageEdit verwaltet Originalbilder, neue Bilder sowie Save/Cancel. Damit
 *   bleibt die technische Lebensdauer der Dateien aus CarViewModel ausgelagert.
 */
