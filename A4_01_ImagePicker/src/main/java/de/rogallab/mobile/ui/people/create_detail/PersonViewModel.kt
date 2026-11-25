package de.rogallab.mobile.ui.people.create_detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.normalized
import de.rogallab.mobile.ui.people.toNullableInput
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput
import de.rogallab.mobile.shared.domain.utilities.sanitizePhoneInput
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.images.IImageEditDelegate
import de.rogallab.mobile.shared.domain.images.ImageChange
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import kotlinx.coroutines.CoroutineStart
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

class PersonViewModel(
   val personId: String?,
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _validator: PersonValidator,
   private val _images: IImageEditDelegate,
   private val _personUcCreate: PersonUcCreate,
   private val _personUcUpdate: PersonUcUpdate,
   private val _effectDelegate: EffectDelegate<PersonEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   private val _personId = personId?.toNullableInput()
   private val _isNew = _personId == null
   // Block further actions until navigation handles the closing effect.
   private var _isClosing = false

   // A new person starts with an empty form; an existing person is loaded first.
   private val _initialState =
      if (_isNew) PersonUiState(isNew = true)
      else PersonUiState(isNew = false, isLoading = true)

   private val _stateFlow = MutableStateFlow(_initialState)
   val stateFlow: StateFlow<PersonUiState> = _stateFlow.asStateFlow()

   init {
      if (_isNew) _images.start(emptyList())
      else loadPerson(_personId!!)
   }

   fun onIntent(intent: PersonIntent) {
      when (intent) {
         is PersonIntent.FirstNameChange -> updateForm { it.copy(firstName = intent.firstName.trim()) }
         is PersonIntent.LastNameChange -> updateForm { it.copy(lastName = intent.lastName.trim()) }
         is PersonIntent.EmailChange -> updateForm { it.copy(email = intent.email.toNullableInput()) }
         is PersonIntent.PhoneChange -> updateForm { it.copy(phone = intent.phone.toNullableInput()) }
         is PersonIntent.GalleryImageSelected -> storeGalleryImage(intent.sourceUri)
         is PersonIntent.CameraImageTaken -> storeCameraImage(intent.imagePath)
         is PersonIntent.RemoveImage -> removeImage(intent.imagePath)
         is PersonIntent.ImageFailed -> showError(intent.message)
         PersonIntent.RetryLoad ->
            if (!_isClosing && !_isNew && !_stateFlow.value.isLoading &&
               _stateFlow.value.loadFailure is PersonLoadFailure.Failed) {
               loadPerson(_personId!!)
            }
         PersonIntent.Save -> save()
         PersonIntent.Cancel -> cancel()
      }
   }

   // Retry is gated by isLoading; the ViewModel always edits the original id.
   private fun loadPerson(id: String) {
      _stateFlow.update { it.copy(isLoading = true, loadFailure = null) }
      viewModelScope.launch {
         delay(_timing.delayRead)

         // Expected failures arrive as Result; unexpected exceptions are covered in A5.
         _repository.findById(id)
            .onSuccess { person ->
               currentCoroutineContext().ensureActive()
               if (_isClosing) return@onSuccess
               if (person == null) {
                  _stateFlow.update {
                     it.copy(isLoading = false, loadFailure = PersonLoadFailure.NotFound(
                        _stringProvider.getString(R.string.error_person_not_found)))
                  }
               } else {
                  _images.start(listOfNotNull(person.imagePath))
                  _stateFlow.update {
                     it.copy(person = person, isLoading = false, loadFailure = null)
                  }
               }
            }
            .onFailure { error ->
               // Cancellation is control flow, not a user-visible error.
               if (error is CancellationException) throw error
               currentCoroutineContext().ensureActive()
               if (_isClosing) return@onFailure
               _stateFlow.update {
                  it.copy(isLoading = false, loadFailure = PersonLoadFailure.Failed(
                     _stringProvider.getString(R.string.error_person_load)))
               }
            }
      }
   }

   // Keep the familiar Person state; block edits while saving or closing.
   private fun updateForm(transform: (Person) -> Person) {
      if (_stateFlow.value.isSaving || _isClosing) return
      _stateFlow.update { it.copy(person = transform(it.person)) }
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
      val job = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
         currentCoroutineContext().ensureActive()
         operation()
            .onSuccess { change ->
               currentCoroutineContext().ensureActive()
               change.paths?.let { paths -> updateForm { it.copy(imagePath = paths.firstOrNull()) } }
               if (change.failedImports > 0) {
                  showErrorNow(_stringProvider.getString(SharedR.string.error_image_save))
               }
            }
            .onFailure { error ->
               if (error is CancellationException) throw error
               currentCoroutineContext().ensureActive()
               showErrorNow(_stringProvider.getString(SharedR.string.error_image_save))
            }
      }
      // Register outside the body, even when the job has already completed.
      // Only this atomic state update releases the guard; no file cleanup belongs here.
      job.invokeOnCompletion {
         _stateFlow.update { it.copy(isEditingImages = false) }
      }
   }

   private fun storeGalleryImage(uri: Uri) =
      changeImages { _images.importGallery(listOf(uri)) }

   private fun storeCameraImage(path: String) {
      val state = _stateFlow.value
      if (state.isEditingImages || state.isSaving || state.isLoading ||
         state.loadFailure != null || _isClosing) {
         viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _images.rejectCamera(path).onFailure { error ->
               if (error is CancellationException) throw error
               currentCoroutineContext().ensureActive()
               if (!_isClosing) showErrorNow(_stringProvider.getString(SharedR.string.error_image_save))
            }
         }
      } else changeImages { _images.acceptCamera(path) }
   }

   private fun removeImage(path: String?) {
      val imagePath = path ?: _stateFlow.value.person.imagePath ?: return
      changeImages { _images.remove(imagePath) }
   }

   private fun save() {
      val state = _stateFlow.value
      if (state.isSaving || state.isEditingImages || _isClosing || state.isLoading || state.loadFailure != null) return

      // Normalize all form values before validation and persistence.
      var person = _stateFlow.value.person.normalized()

      // Sanitize the email before validation.
      if (person.email != null) {
         val email = sanitizeEmailInput(person.email)
         if (email != person.email) {
            // Show the sanitized value in the UI as well.
            _stateFlow.update { state  ->
               state.copy(person = state.person.copy(email = email))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // Sanitize the phone number before validation.
      if (person.phone != null) {
         val phone = sanitizePhoneInput(person.phone)
         if (phone != person.phone) {
            // Show the sanitized value in the UI as well.
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(phone = phone))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // Validate the complete entity before accessing the repository.
      val error = _validator.validatePerson(person)
      if (error != null) {
         showError(error)
         return
      }

      val personToSave = person
      // Freeze the submitted values and block further write operations immediately.
      _stateFlow.update { it.copy(person = personToSave, isSaving = true) }

      val job = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
         currentCoroutineContext().ensureActive()
         delay(_timing.delayWrite)
         val result =
            if (_isNew) _personUcCreate(personToSave)
            else _personUcUpdate(personToSave)

         result
            .onSuccess {
               currentCoroutineContext().ensureActive()
               _isClosing = true
               _effectDelegate.emit(
                  PersonEffect.ShowMessage(
                     _stringProvider.getString(R.string.message_person_saved, personToSave.fullName)
                  )
               )
               _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Save))
            }
            .onFailure { error ->
               if (error is CancellationException) throw error
               currentCoroutineContext().ensureActive()
               showErrorNow(_stringProvider.getString(R.string.error_person_save))
            }
      }
      // Completion also releases the guard after cancellation.
      // Do not launch a coroutine here: the ViewModel scope may already be cancelled.
      job.invokeOnCompletion {
         _stateFlow.update { it.copy(isSaving = false) }
      }
   }

   private fun cancel() {
      if (_stateFlow.value.isSaving || _stateFlow.value.isEditingImages || _isClosing) return
      // Once Cancel is accepted, Save must not race the pending navigation.
      _isClosing = true
      viewModelScope.launch {
         _images.discard()
            .onSuccess {
               currentCoroutineContext().ensureActive()
               _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Cancel))
            }
            .onFailure { error ->
               if (error is CancellationException) throw error
               currentCoroutineContext().ensureActive()
               // An expected cleanup failure allows another Cancel attempt.
               _isClosing = false
               showErrorNow(_stringProvider.getString(SharedR.string.error_image_save))
            }
      }
   }

   private fun showError(message: String) {
      viewModelScope.launch { showErrorNow(message) }
   }

   private suspend fun showErrorNow(message: String) {
      _effectDelegate.emit(PersonEffect.ShowError(message))
   }
}
