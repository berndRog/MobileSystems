package de.rogallab.mobile.ui.people.create_detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.R as SharedR
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput
import de.rogallab.mobile.shared.domain.utilities.sanitizePhoneInput
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import de.rogallab.mobile.shared.ui.images.IImageEdit
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.normalized
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonViewModel(
   val personId: String?,
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _validator: PersonValidator,
   private val _imageFileStorage: IImageFileStorage,
   private val _imageEdit: IImageEdit,
   private val _effectDelegate: EffectDelegate<PersonEffect>,
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   private val _personId = personId?.takeUnless(String::isBlank)
   private val _isNew = _personId == null
   private var _isSaving = false

   private val _initialState =
      if (_isNew)
         PersonUiState(isNew = true, isLoading = false)
      else
         PersonUiState(isNew = false, isLoading = true)

   private val _stateFlow: MutableStateFlow<PersonUiState> =
      MutableStateFlow(_initialState)

   val stateFlow: StateFlow<PersonUiState> =
      _stateFlow.asStateFlow()

   init {
      if (!_isNew) loadPerson(_personId!!)
   }

   private fun loadPerson(id: String) {
      viewModelScope.launch {
         _stateFlow.update { state: PersonUiState ->
            state.copy(isLoading = true)
         }

         delay(1000)

         _repository.findById(id)
            .onSuccess { person ->
               if (person == null) {
                  val error = _stringProvider.getString(R.string.error_person_not_found)
                  _effectDelegate.emit(PersonEffect.ShowError(error))
                  _stateFlow.update { state: PersonUiState ->
                     state.copy(isLoading = false)
                  }
                  return@onSuccess
               }

               _imageEdit.start(listOfNotNull(person.imagePath))
               _stateFlow.update { state: PersonUiState ->
                  state.copy(person = person, isLoading = false)
               }
            }
            .onFailure {
               val error = _stringProvider.getString(R.string.error_person_load)
               _effectDelegate.emit(PersonEffect.ShowError(error))
               _stateFlow.update { state: PersonUiState ->
                  state.copy(isLoading = false)
               }
            }
      }
   }

   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         is PersonIntent.FirstNameChange -> changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         is PersonIntent.EmailChange -> changeEmail(intent.email)
         is PersonIntent.PhoneChange -> changePhone(intent.phone)
         is PersonIntent.GalleryImageSelected -> storeGalleryImage(intent.sourceUri)
         is PersonIntent.ImagePathChange -> changeImage(intent.imagePath)
         is PersonIntent.ImageStorageFailed -> showError(intent.message)
         PersonIntent.Save -> save()
         PersonIntent.Cancel -> cancel()
      }
   }

   private fun changeFirstName(firstName: String) =
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(firstName = firstName.trim()))
      }

   private fun changeLastName(lastName: String) =
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(lastName = lastName.trim()))
      }

   private fun changeEmail(email: String) {
      var emailNullable: String? = null
      if (email.trim().isNotEmpty()) emailNullable = email.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(email = emailNullable))
      }
   }

   private fun changePhone(phone: String) {
      var phoneNullable: String? = null
      if (phone.trim().isNotEmpty()) phoneNullable = phone.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(phone = phoneNullable))
      }
   }

   private fun storeGalleryImage(sourceUri: Uri) {
      viewModelScope.launch {
         val imagePath = _imageFileStorage
            .copyImageToAppStorage(sourceUri)
            .getOrElse {
               showError(_stringProvider.getString(SharedR.string.error_image_save))
               return@launch
            }

         replaceImage(imagePath)
      }
   }

   private fun changeImage(imagePath: String?) {
      viewModelScope.launch {
         replaceImage(imagePath)
      }
   }

   private suspend fun replaceImage(imagePath: String?) {
      val images = _imageEdit.replace(listOfNotNull(imagePath))
      _stateFlow.update { state: PersonUiState ->
         state.copy(
            person = state.person.copy(imagePath = images.firstOrNull())
         )
      }
   }

   private fun showError(message: String) {
      viewModelScope.launch {
         _effectDelegate.emit(PersonEffect.ShowError(message))
      }
   }

   private fun save() {
      if (_isSaving) return

      var person = _stateFlow.value.person.normalized()

      if (person.email != null) {
         val email = sanitizeEmailInput(person.email)
         if (email != person.email) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(email = email))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      if (person.phone != null) {
         val phone = sanitizePhoneInput(person.phone)
         if (phone != person.phone) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(phone = phone))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      val error = _validator.validatePerson(person)
      if (error != null) {
         showError(error)
         return
      }

      _stateFlow.update { state: PersonUiState ->
         state.copy(person = person)
      }
      _isSaving = true

      viewModelScope.launch {
         val result =
            if (_isNew) _repository.create(person)
            else _repository.update(person)

         result
            .onSuccess {
               _imageEdit.commit()

               val message = _stringProvider.getString(
                  R.string.message_person_saved,
                  person.fullName,
               )
               _effectDelegate.emit(PersonEffect.ShowMessage(message))
               _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Save))
            }
            .onFailure {
               val errorMessage = _stringProvider.getString(R.string.error_person_save)
               _effectDelegate.emit(PersonEffect.ShowError(errorMessage))
            }

         _isSaving = false
      }
   }

   private fun cancel() {
      viewModelScope.launch {
         _imageEdit.discard()
         _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Cancel))
      }
   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}
