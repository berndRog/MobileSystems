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
import de.rogallab.mobile.shared.domain.images.IImageEditDelegate
import de.rogallab.mobile.shared.domain.images.ImageChange
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.cars.input_detail.comp.CarForm
import de.rogallab.mobile.ui.people.create_detail.BackReason
import kotlinx.coroutines.CoroutineStart
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
   private val _images: IImageEditDelegate,
   private val _carUcCreate: CarUcCreate,
   private val _carUcUpdate: CarUcUpdate,
   private val _effectDelegate: EffectDelegate<CarEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<CarEffect> by _effectDelegate {

   private val _carId = carId?.takeUnless(String::isBlank)
   private val _isNew = _carId == null
   // Terminal handoff to navigation; this is not a second saving flag.
   private var _isClosing = false
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
      if (_isNew) _images.start(emptyList())
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
                     _images.start(car.imagePaths)
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
      if ((_stateFlow.value.isSaving || _isClosing) &&
         (intent is CarIntent.ManufacturerChanged ||
            intent is CarIntent.ModelChanged ||
            intent is CarIntent.PriceChanged ||
            intent is CarIntent.SellerChanged)) return
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

   // All image entry points run on Main, like the other UI intents.
   private fun beginImageChange(): Boolean {
      val state = _stateFlow.value
      if (state.isEditingImages || state.isSaving || state.isLoading ||
         state.loadFailure != null || _isClosing) return false
      _stateFlow.update { it.copy(isEditingImages = true) }
      return true
   }

   private fun changeImages(operation: suspend () -> Result<ImageChange>) {
      if (!beginImageChange()) return
      viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
         try {
            currentCoroutineContext().ensureActive()
            operation()
               .onSuccess { change ->
                  currentCoroutineContext().ensureActive()
                  change.paths?.let { paths -> updateForm { it.copy(imagePaths = paths) } }
                  if (change.failedImports > 0) {
                     showErrorNow(_stringProvider.getString(SharedR.string.error_image_save))
                  }
               }
               .onFailure { error ->
                  currentCoroutineContext().ensureActive()
                  if (error is CancellationException) throw error
                  showErrorNow(_stringProvider.getString(SharedR.string.error_image_save))
               }
         } finally {
            _stateFlow.update { it.copy(isEditingImages = false) }
         }
      }
   }

   private fun storeGalleryImages(uris: List<Uri>) {
      if (uris.isNotEmpty()) changeImages { _images.importGallery(uris) }
   }

   private fun storeCameraImage(path: String) {
      val state = _stateFlow.value
      if (state.isEditingImages || state.isSaving || state.isLoading ||
         state.loadFailure != null || _isClosing) {
         viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _images.rejectCamera(path).onFailure { error ->
               if (error is CancellationException) throw error
               if (!_isClosing) showErrorNow(_stringProvider.getString(SharedR.string.error_image_save))
            }
         }
      } else changeImages { _images.acceptCamera(path) }
   }

   private fun removeImage(path: String) = changeImages { _images.remove(path) }

   private fun save() {
      val state = _stateFlow.value
      if (state.isSaving || state.isEditingImages || _isClosing || state.isLoading || state.loadFailure != null) return

      // This immutable snapshot is the only input to the write operation.
      val form = state.form.normalized().copy(imagePaths = state.form.imagePaths.toList())
      _stateFlow.update { it.copy(form = form) }
      val error = _validator.validateCar(form)
      if (error != null) {
         showError(error)
         return
      }
      val car: Car = form.toCar()

      // Mark the attempt synchronously, before the coroutine can be scheduled.
      _stateFlow.update { it.copy(isSaving = true) }
      // Enter try/finally immediately, even if the scope is cancelled before dispatch.
      viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
         try {
            delay(_timing.delayWrite)
            val result =
               if (_isNew) _carUcCreate(car)
               else _carUcUpdate(car)

            result
               .onSuccess {
                  currentCoroutineContext().ensureActive()
                  _isClosing = true
                  _effectDelegate.emit(
                     CarEffect.ShowMessage(
                        _stringProvider.getString(R.string.message_car_saved, car.displayName)
                     )
                  )
                  _effectDelegate.emit(CarEffect.NavigateBack(BackReason.Save))
               }
               .onFailure { throwable ->
                  currentCoroutineContext().ensureActive()
                  if (throwable is CancellationException) throw throwable
                  showErrorNow(_stringProvider.getString(R.string.error_car_save))
               }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            showErrorNow(_stringProvider.getString(R.string.error_car_save))
         } finally {
            _stateFlow.update { it.copy(isSaving = false) }
         }
      }
   }

   private fun cancel() {
      if (_stateFlow.value.isSaving || _stateFlow.value.isEditingImages || _isClosing) return
      // Once Cancel is accepted, Save must not race the pending navigation.
      _isClosing = true
      viewModelScope.launch {
         try {
            _images.discard().getOrThrow()
            _effectDelegate.emit(CarEffect.NavigateBack(BackReason.Cancel))
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            _isClosing = false
            showErrorNow(_stringProvider.getString(SharedR.string.error_image_save))
         }
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
 * - The image delegate imports picker results and owns technical file cleanup.
 *
 * - CameraPickerHandler liefert dagegen bereits einen bestätigten internen
 *   Dateipfad. Beide Wege enden anschließend in derselben IImageEdit-Session.
 *
 * - The ViewModel owns operation guards, form updates and user-facing effects.
 */
