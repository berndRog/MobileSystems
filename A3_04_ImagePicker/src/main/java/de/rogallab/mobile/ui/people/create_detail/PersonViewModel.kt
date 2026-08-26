package de.rogallab.mobile.ui.people.create_detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
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
import de.rogallab.mobile.shared.R as SharedR

class PersonViewModel(
   val personId: String?,
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _validator: PersonValidator,
   private val _imageFileStorage: IImageFileStorage,
   private val _imageEdit: IImageEdit,
   private val _effectDelegate: EffectDelegate<PersonEffect>,
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   // Normalize the optional navigation parameter.
   // A null or blank id means that a new person is being created.
   private val _personId =
      personId?.takeUnless(String::isBlank)

   private val _isNew =
      _personId == null

   // Prevent duplicate repository writes while a Save operation is running.
   // This is internal processing state and therefore not part of PersonUiState.
   private var _isSaving = false

   // A new person can be edited immediately.
   // An existing person has to be loaded from the repository first.
   private val _initialState =
      if (_isNew)
         PersonUiState(
            isNew = true,
            isLoading = false
         )
      else
         PersonUiState(
            isNew = false,
            isLoading = true
         )

   // Mutable state is kept private inside the ViewModel.
   private val _stateFlow: MutableStateFlow<PersonUiState> =
      MutableStateFlow(_initialState)

   // The UI observes only the read-only StateFlow.
   val stateFlow: StateFlow<PersonUiState> =
      _stateFlow.asStateFlow()

   init {
      // Existing persons are loaded automatically when the ViewModel is created.
      if (!_isNew) {
         loadPerson(_personId!!)
      }
   }

   // Loads an existing person and initializes the image edit session
   // with the image currently stored by the entity.
   private fun loadPerson(id: String) {
      viewModelScope.launch {

         // Keep the current state and only switch the loading flag.
         _stateFlow.update { state: PersonUiState ->
            state.copy(isLoading = true)
         }

         // Simulate a visible loading operation for the teaching example.
         delay(1000)

         _repository.findById(id)
            .onSuccess { person ->

               // A successful repository call may still return no matching person.
               if (person == null) {
                  val error =
                     _stringProvider.getString(
                        R.string.error_person_not_found
                     )

                  // Errors are one-time UI effects and therefore do not belong
                  // to the persistent PersonUiState.
                  _effectDelegate.emit(
                     PersonEffect.ShowError(error)
                  )

                  _stateFlow.update { state: PersonUiState ->
                     state.copy(isLoading = false)
                  }
                  return@onSuccess
               }

               // Start the image edit session with the persisted image.
               // The delegate remembers this image as the original selection.
               _imageEdit.start(
                  listOfNotNull(person.imagePath)
               )

               // Publish the loaded person as the new persistent UI state.
               _stateFlow.update { state: PersonUiState ->
                  state.copy(
                     person = person,
                     isLoading = false,
                  )
               }
            }
            .onFailure {

               // Repository failures are converted into a localized UI effect.
               val error =
                  _stringProvider.getString(
                     R.string.error_person_load
                  )

               _effectDelegate.emit(
                  PersonEffect.ShowError(error)
               )

               _stateFlow.update { state: PersonUiState ->
                  state.copy(isLoading = false)
               }
            }
      }
   }

   // Single public entry point for all events coming from the UI layer.
   // The dispatcher forwards every intent to a private ViewModel function.
   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         is PersonIntent.FirstNameChange ->changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         is PersonIntent.EmailChange -> changeEmail(intent.email)
         is PersonIntent.PhoneChange -> changePhone(intent.phone)

         // Gallery selection still contains an external Content-Uri.
         // The image must first be copied into private app storage.
         is PersonIntent.GalleryImageSelected -> storeGalleryImage(intent.sourceUri)
         // Camera images already arrive as confirmed internal file paths.
         // The same intent is also used when an image is removed with null.
         is PersonIntent.ImagePathChange -> changeImage(intent.imagePath)
         // Technical image errors are converted into the common error effect.
         is PersonIntent.ImageStorageFailed -> showError(intent.message)

         PersonIntent.Save -> save()
         PersonIntent.Cancel -> cancel()
      }
   }

   // Update only the first name while keeping all other state values.
   private fun changeFirstName(firstName: String) =
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(firstName = firstName.trim()))
      }

   // Update only the last name while keeping all other state values.
   private fun changeLastName(lastName: String) =
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(lastName = lastName.trim()))
      }

   // Convert an empty input field back to the nullable domain representation.
   private fun changeEmail(email: String) {
      var emailNullable: String? = null
      if (email.trim().isNotEmpty())  emailNullable = email.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(email = emailNullable))
      }
   }

   // Convert an empty input field back to the nullable domain representation.
   private fun changePhone(phone: String) {
      var phoneNullable: String? = null
      if (phone.trim().isNotEmpty()) phoneNullable = phone.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(phone = phoneNullable))
      }
   }

   // A gallery image initially belongs to the external Photo Picker.
   // Copy it into private app storage before using it in the edit session.
   private fun storeGalleryImage(sourceUri: Uri) {
      viewModelScope.launch {
         // IImageFileStorage performs the technical Uri-to-file operation.
         val imagePath = _imageFileStorage
            .copyImageToAppStorage(sourceUri)
            .getOrElse {
               // The gallery image could not be stored, therefore the current
               // edit-session image remains unchanged.
               showError(_stringProvider.getString(SharedR.string.error_image_save))
               return@launch
            }
         // From this point on the feature works only with an internal path.
         replaceImage(imagePath)
      }
   }

   // Camera images already arrive as internal paths.
   // Passing null removes the image from the current edit-session selection.
   private fun changeImage(imagePath: String?) {
      viewModelScope.launch {
         replaceImage(imagePath)
      }
   }

   // Delegate image lifecycle management to IImageEdit.
   //
   // The ViewModel only provides the desired selection. The delegate decides
   // which temporary images may already be deleted and which persisted originals
   // must remain until Save has completed successfully.
   private suspend fun replaceImage(imagePath: String?) {

      // Person currently supports one image, while IImageEdit deliberately
      // uses List<String> so that it can also support multi-image entities.
      val images = _imageEdit.replace(listOfNotNull(imagePath))

      // Reflect the resulting edit-session selection in PersonUiState.
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(imagePath = images.firstOrNull()))
      }
   }

   // Send an error as a one-time effect instead of storing it in PersonUiState.
   private fun showError(message: String) {
      viewModelScope.launch {
         _effectDelegate.emit(PersonEffect.ShowError(message))
      }
   }

   // Validate and persist the current person.
   private fun save() {

      // Ignore another Save request while a repository write is still running.
      if (_isSaving) return

      // Normalize all form values before validation and persistence.
      var person = _stateFlow.value.person.normalized()

      // Sanitize the email before validation.
      if (person.email != null) {
         val email = sanitizeEmailInput(person.email)
         if (email != person.email) {
            // Show the sanitized value in the UI as well.
            _stateFlow.update { state: PersonUiState ->
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

      // Publish the normalized and validated person before saving it.
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = person)
      }
      _isSaving = true

      viewModelScope.launch {

         // New entities are inserted, existing entities are updated.
         val result =
            if (_isNew)
               _repository.create(person)
            else
               _repository.update(person)

         result
            .onSuccess {
               // The repository now owns the new image selection.
               // Only at this point may obsolete persisted originals be deleted.
               _imageEdit.commit()

               // First show the success message...
               val message = _stringProvider.getString(R.string.message_person_saved, person.fullName,)
               _effectDelegate.emit(PersonEffect.ShowMessage(message))

               // ...and then request reverse navigation with Save semantics.
               _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Save))
            }
            .onFailure {
               // A failed repository write must not commit the image session.
               // The original and replacement images therefore remain available
               // so that the user can retry or cancel the edit operation.
               val error = _stringProvider.getString(R.string.error_person_save)
               _effectDelegate.emit(PersonEffect.ShowError(error))
            }

         _isSaving = false
      }
   }

   // Discard the current edit session and navigate back without saving.
   private fun cancel() {
      viewModelScope.launch {

         // Remove only images created during the current edit session.
         // Persisted original images remain untouched.
         _imageEdit.discard()

         // Navigation is emitted only after image cleanup has completed.
         _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Cancel))
      }
   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - PersonViewModel bleibt der zentrale Zustands- und Intent-Verarbeiter für
 *   die Personenbearbeitung. Der Screen beobachtet weiterhin genau einen
 *   StateFlow<PersonUiState>, während einmalige Meldungen und Navigationen
 *   getrennt über PersonEffect ausgegeben werden.
 *
 * - Alle UI-Ereignisse werden über die öffentliche Methode onIntent(...)
 *   verarbeitet. Die eigentliche Logik bleibt in privaten ViewModel-Funktionen.
 *   Dadurch besitzt das ViewModel weiterhin genau einen klaren Einstiegspunkt
 *   für Events aus der UI.
 *
 * - Die Bildbearbeitung ergänzt den bisherigen Personen-Workflow, ohne einen
 *   zweiten UI-State einzuführen. Das aktuelle Bild bleibt Bestandteil von
 *   PersonUiState und damit des bestehenden unidirektionalen Datenflusses.
 *
 * - Galerie und Kamera liefern dem ViewModel unterschiedliche Ausgangsdaten:
 *
 *      Galerie
 *          -> Content-Uri
 *
 *      Kamera
 *          -> bereits bestätigter interner Dateipfad
 *
 * - Eine Content-Uri aus dem Android Photo Picker darf nicht direkt als
 *   dauerhafte Bildreferenz der Person verwendet werden. Deshalb kopiert
 *   PersonViewModel ein Galerie-Bild zunächst über IImageFileStorage in den
 *   privaten App-Speicher:
 *
 *      GalleryPickerHandler
 *          -> Uri
 *          -> PersonIntent.GalleryImageSelected
 *          -> PersonViewModel
 *          -> IImageFileStorage.copyImageToAppStorage(...)
 *          -> interner imagePath
 *
 * - Ein Kamera-Bild wurde dagegen bereits durch CameraPickerHandler vorbereitet
 *   und nach erfolgreicher Aufnahme bestätigt. PersonViewModel erhält deshalb
 *   direkt den internen Dateipfad über PersonIntent.ImagePathChange.
 *
 * - Nach diesem technischen Unterschied werden Galerie- und Kamera-Bilder
 *   wieder gleich behandelt. In beiden Fällen wird der interne Dateipfad an
 *   IImageEdit weitergegeben.
 *
 * - IImageFileStorage und IImageEdit besitzen bewusst unterschiedliche
 *   Verantwortungen:
 *
 *      IImageFileStorage
 *          technische Dateioperationen
 *          Uri in App-Speicher kopieren
 *          Dateien anlegen, bestätigen und löschen
 *
 *      IImageEdit
 *          Lebensdauer der Bilder während einer Edit-Session verwalten
 *          Originalbild merken
 *          Ersatzbild übernehmen
 *          Save und Cancel absichern
 *
 * - PersonViewModel kennt dadurch nicht die einzelnen Löschregeln für alte und
 *   neue Bilder. Diese Logik wird an ImageEditDelegate delegiert.
 *
 * - Beim Laden einer bestehenden Person startet das ViewModel die Edit-Session
 *   mit dem bereits gespeicherten Bild:
 *
 *      Repository
 *          -> Person
 *          -> imagePath
 *          -> IImageEdit.start(...)
 *
 *   Dieses Bild gilt anschließend als Original der laufenden Bearbeitung.
 *
 * - replaceImage(...) bildet die gemeinsame Schnittstelle für Änderungen der
 *   aktuellen Bildauswahl. Für Person wird nur ein Bild verwendet, IImageEdit
 *   arbeitet jedoch bereits mit List<String>. Dadurch kann dieselbe
 *   Infrastruktur später auch für Entitäten mit mehreren Bildern verwendet
 *   werden.
 *
 * - Beim Entfernen eines Bildes wird nicht direkt eine Datei gelöscht.
 *   Das ViewModel übergibt lediglich eine leere Auswahl an IImageEdit.
 *   Der Delegate entscheidet anschließend abhängig von der Edit-Session,
 *   ob eine Datei sofort entfernt werden darf oder bis zum erfolgreichen
 *   Speichern erhalten bleiben muss.
 *
 * - Beim erfolgreichen Speichern gilt folgende Reihenfolge:
 *
 *      Person validieren
 *          -> Repository.create/update(...)
 *          -> erfolgreich
 *          -> IImageEdit.commit()
 *          -> ShowMessage
 *          -> NavigateBack
 *
 * - commit() wird bewusst erst nach erfolgreichem Repository-Zugriff aufgerufen.
 *   Erst dann ist sichergestellt, dass die neue Bildreferenz dauerhaft in der
 *   Datenbank gespeichert wurde. Jetzt dürfen nicht mehr verwendete Original-
 *   bilder gelöscht werden.
 *
 * - Schlägt das Speichern fehl, wird commit() nicht ausgeführt. Dadurch bleiben
 *   sowohl das bisher gespeicherte Originalbild als auch das aktuell gewählte
 *   Ersatzbild erhalten. Der Benutzer kann den Speichervorgang erneut versuchen
 *   oder die Bearbeitung abbrechen.
 *
 * - Beim Abbrechen gilt:
 *
 *      Cancel
 *          -> IImageEdit.cancel()
 *          -> neu erzeugte, nicht gespeicherte Bilder löschen
 *          -> ursprüngliche Bilder erhalten
 *          -> NavigateBack
 *
 * - Die Navigation wird auch hier erst nach dem Aufräumen der Edit-Session
 *   ausgelöst. Dadurch bleibt die Bildverwaltung vollständig abgeschlossen,
 *   bevor der PersonScreen verlassen wird.
 *
 * - _isSaving ist bewusst kein Bestandteil von PersonUiState. Die Variable
 *   verhindert lediglich parallele Repository-Schreibvorgänge und wird von
 *   der UI nicht dargestellt. Technischer interner Zustand muss daher nicht
 *   automatisch Teil des beobachtbaren UI-States sein.
 *
 * - Fehlermeldungen werden ebenfalls nicht dauerhaft im PersonUiState
 *   gespeichert. Repository-, Validierungs- und Bildfehler werden als
 *   PersonEffect.ShowError ausgegeben und von der UI einmalig verarbeitet.
 *
 * - Damit bleibt der bekannte Datenfluss erhalten:
 *
 *      PersonScreen
 *          -> Callback
 *
 *      PersonAdapter
 *          -> PersonIntent
 *
 *      PersonViewModel
 *          -> StateFlow<PersonUiState>
 *          -> PersonEffect
 *
 *      PersonAdapter
 *          -> PersonScreen / Snackbar / Navigation
 *
 * Lernziele:
 *
 * - Einen einzigen Intent-Einstiegspunkt im ViewModel verwenden.
 * - Persistent State und einmalige Effects voneinander unterscheiden.
 * - Content-Uri und internen Dateipfad unterscheiden.
 * - Galerie-Bilder vor der weiteren Verarbeitung in den App-Speicher kopieren.
 * - Technische Dateiverwaltung über IImageFileStorage kapseln.
 * - Bild-Lebenszyklen einer Bearbeitung über IImageEdit delegieren.
 * - Original- und Ersatzbilder bei Save und Cancel sicher behandeln.
 * - commit() erst nach erfolgreichem Repository-Zugriff ausführen.
 * - cancel() zum Aufräumen einer nicht gespeicherten Edit-Session verwenden.
 * - Bestehenden UDF-/MVI-Datenfluss auch bei komplexerer Bildlogik beibehalten.
 */