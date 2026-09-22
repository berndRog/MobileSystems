package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

class DeletePersonUseCase(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   // Deletes the database entry before removing its image file.
   // This order prevents a failed repository call from leaving a broken image reference.
   suspend operator fun invoke(person: Person): Result<Unit> {
      val result = _repository.remove(person)

      if (result.isSuccess) {
         // Image cleanup is best effort because the person has already been deleted.
         _imageFileStorage.deleteImageFromAppStorage(person.imagePath)
            .onFailure { throwable ->
               Alog.e(TAG, "delete person image failed: ${throwable.message}")
            }
      }

      return result
   }

   companion object {
      private const val TAG = "<-DeletePersonUseCase"
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
