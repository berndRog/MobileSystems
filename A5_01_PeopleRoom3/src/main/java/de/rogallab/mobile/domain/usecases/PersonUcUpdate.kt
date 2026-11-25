package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.resultOf
import de.rogallab.mobile.shared.domain.images.IImageEdit

class PersonUcUpdate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {

   suspend operator fun invoke(person: Person): Result<Unit> =
      resultOf(
         block = {
            _repository.update(person).getOrThrow()

            // Commit the image edit session only after successful persistence.
            _imageEdit.commit()
         },
         onFinally = {}
      )
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
