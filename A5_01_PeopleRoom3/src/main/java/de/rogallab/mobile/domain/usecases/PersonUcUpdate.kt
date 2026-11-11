package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.images.IImageEdit
import kotlin.coroutines.cancellation.CancellationException

class PersonUcUpdate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {

      // Updates the person before committing the replacement image.
      val result = _repository.update(person)

      if (result.isSuccess) {
         try {
            // Commit the image edit session only after a successful repository write.
            _imageEdit.commit()
         } catch (e: CancellationException) {
            throw e
         } catch (e: Throwable) {
            // Cleanup cannot turn an already persisted person into a failed Save.
            Alog.e(TAG, "commit person image failed: ${e.message}")
         }
      }
      // Persisted original images remain untouched when the update fails.

      return result
   }

   companion object {
      private const val TAG = "<-PersonUcUpdate"
   }
}

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
