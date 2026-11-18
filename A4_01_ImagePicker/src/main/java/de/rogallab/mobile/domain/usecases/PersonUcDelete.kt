package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.resultOf
import kotlinx.coroutines.CancellationException

class PersonUcDelete(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   suspend operator fun invoke(person: Person): Result<Unit> =
      resultOf(
         block = {
            // First delete the person from the repository.
            _repository.remove(person).getOrThrow()

            // Image cleanup is best effort because the person
            // has already been deleted from the repository.
            try {
               _imageFileStorage.deleteImageFromAppStorage(person.imagePath)
                  .onFailure { throwable ->
                     if (throwable is CancellationException) {
                        throw throwable
                     }
                     Alog.e(TAG, "delete person image failed: ${throwable.message}")
                  }

            } catch (e: CancellationException) {
               throw e

            } catch (e: Exception) {
               // Log unexpected errors without failing the deletion.
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
class PersonUcDelete(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {
      try {
         // First delete the person from the repository.
         val result = _repository.remove(person)

         if (result.isSuccess) {
            // Image cleanup is best effort.
            // Do not fail the use case if image deletion fails.
            try {
               _imageFileStorage.deleteImageFromAppStorage(person.imagePath)
                  .onFailure { throwable ->
                     Alog.e(TAG, "delete person image failed: ${throwable.message}")
                  }

            } catch (e: CancellationException) {
               throw e

            } catch (e: Exception) {
               // Log unexpected errors without failing the deletion.
               Alog.e(TAG, "delete person image failed: ${e.message}")
            }
         }

         return result

      } catch (e: CancellationException) {
         // Coroutine cancellation must be propagated.
         throw e

      } catch (e: Exception) {
         // Convert unexpected repository exceptions to Result.failure.
         return Result.failure(e)

      } finally {
         // Always executed, including return and exception.
         // No additional cleanup required here.
      }
   }

   companion object {
      private const val TAG = "<-PersonUcDelete"
   }
}
 */

/*class PersonUcDelete(
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
               // log the error but do not propagate it, because the main operation succeeded
               Alog.e(TAG, "delete person image failed: ${throwable.message}")
            }
      }

      return result
   }

   companion object {
      private const val TAG = "<-PersonUcDelete"
   }
}*/

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
