package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.resultOf
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlin.coroutines.cancellation.CancellationException

class PersonUcDelete(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   // Deletes the database entry before removing its image file.
   // This order prevents a failed repository call from leaving a broken image reference.
   suspend operator fun invoke(person: Person): Result<Unit> =
      resultOf(
         block = {
            _repository.remove(person).getOrThrow()

            // Cleanup must not turn a successful repository deletion into a failure.
            try {
               _imageFileStorage.deleteImageFromAppStorage(person.imagePath)
                  .onFailure { throwable ->
                     if (throwable is CancellationException) throw throwable
                     Alog.e(TAG, "delete person image failed: ${throwable.message}")
                  }
            } catch (e: CancellationException) {
               throw e
            } catch (e: Throwable) {
               Alog.e(TAG, "delete person image failed: ${e.message}")
            }
            Unit
         },
         onFinally = {}
      )

   companion object {
      private const val TAG = "<-PersonUcDelete"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - "Person löschen" umfasst auch mit Room zwei Datenquellen: den Datensatz
 *   in der Datenbank und die zugehörige Datei im privaten App-Speicher.
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
