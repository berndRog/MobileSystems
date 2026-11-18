package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.shared.domain.utilities.resultOf

import kotlinx.coroutines.CancellationException

class PersonUcUpdate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {

   suspend operator fun invoke(person: Person): Result<Unit> =
      resultOf(
         block = {
            // First update the person in the repository.
            _repository.update(person).getOrThrow()

            // Commit the replacement image only after successful persistence.
            _imageEdit.commit()
         },
         onFinally = {}
      )
}

/*
class PersonUcUpdate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {
   suspend operator fun invoke(person: Person): Result<Unit> {
      try {
         // First update the person in the repository.
         val result = _repository.update(person)

         // Commit the replacement image only after successful persistence.
         if (result.isSuccess) {
            _imageEdit.commit()
         }

         return result

      } catch (e: CancellationException) {
         // Coroutine cancellation must be propagated.
         throw e

      } catch (e: Exception) {
         // Convert unexpected exceptions to Result.failure.
         return Result.failure(e)

      } finally {
         // Always executed, even on return or exception.
         // Do not discard the image edit session here:
         // it may still be needed for a retry.
      }
   }
}
*/

/*
class PersonUcUpdate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {

      // Updates the person before committing the replacement image.
      val result = _repository.update(person)

      if (result.isSuccess) {
         // Commit the image edit session only after a successful repository write.
         _imageEdit.commit()
      }
      // Persisted original images remain untouched when the update fails.

      return result
   }
}
*/

/*
 * Didaktik und Lernziele
 *
 * - Der Use Case bündelt das Aktualisieren der Person mit dem Abschluss der
 *   laufenden Bildbearbeitung zu einer fachlich zusammengehörigen Operation.
 *
 * - Ein bisher gespeichertes Originalbild darf erst entfernt werden, nachdem
 *   der neue imagePath erfolgreich im Repository gespeichert wurde.
 *
 * Lernziele:
 *
 * - Mehrschrittige Anwendungslogik in einem Use Case kapseln.
 * - Persistierte Originaldaten bei fehlgeschlagenen Updates schützen.
 * - Use Cases unabhängig vom Android-UI-Lebenszyklus testen.
 */
