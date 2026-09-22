package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.images.IImageEdit

class UpdatePersonUseCase(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {

   // Updates the person before committing the replacement image.
   // Persisted original images remain untouched when the update fails.
   suspend operator fun invoke(person: Person): Result<Unit> {
      val result = _repository.update(person)

      if (result.isSuccess) {
         _imageEdit.commit()
      }

      return result
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
