package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException

class PersonUcDelete(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   suspend operator fun invoke(personId: String): Result<Unit> {
      currentCoroutineContext().ensureActive()
      var person: Person? = null
      _repository.findById(personId)
         .onSuccess { person = it }
         .onFailure { error ->
            if (error is CancellationException) throw error
            currentCoroutineContext().ensureActive()
            return Result.failure(error)
         }
      currentCoroutineContext().ensureActive()
      val currentPerson = person
         ?: return Result.failure(NoSuchElementException("Person $personId not found"))

      return _repository.remove(currentPerson)
         .onSuccess {
            currentCoroutineContext().ensureActive()
            // Expected cleanup failures must not undo a successful deletion.
            _imageFileStorage.deleteImageFromAppStorage(currentPerson.imagePath)
               .onFailure { error ->
                  if (error is CancellationException) throw error
                  currentCoroutineContext().ensureActive()
                  Alog.e(TAG, "delete person image failed: ${error.message}")
               }
         }
         .onFailure { error ->
            if (error is CancellationException) throw error
            currentCoroutineContext().ensureActive()
         }
   }

   companion object {
      private const val TAG = "<-PersonUcDelete"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - "Person löschen" umfasst ab A4_01 zwei Datenquellen: den Datensatz im
 *   Repository und die zugehörige Datei im privaten App-Speicher.
 *
 * - Zuerst wird der Datensatz gelöscht. Würde das Bild zuerst entfernt und der
 *   Repository-Zugriff anschließend fehlschlagen, bliebe eine Person mit einem
 *   ungültigen imagePath zurück.
 *
 * - Schlägt nur das abschließende Aufräumen der Bilddatei fehl, bleibt die
 *   bereits erfolgreiche Personenlöschung das Ergebnis der Hauptoperation.
 *   Der Fehler wird protokolliert, damit die UI keinen falschen Zustand meldet.
 *
 * Lernziele:
 *
 * - Mehrere Datenquellen in einer Anwendungsoperation koordinieren.
 * - Die Reihenfolge nicht-atomarer Schritte anhand möglicher Fehler begründen.
 * - Kritische Operationen von nachgelagertem Best-Effort-Aufräumen trennen.
 */
