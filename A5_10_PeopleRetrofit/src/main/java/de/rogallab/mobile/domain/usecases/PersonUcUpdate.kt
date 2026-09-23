package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.images.IImageEdit

class PersonUcUpdate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {

      // Updates the remote person before committing the replacement image.
      val result = _repository.update(person)

      if (result.isSuccess) {
         // Commit the image edit session only after a successful server write.
         _imageEdit.commit()
      }
      // Persisted original images remain untouched when the update fails.

      return result
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Use Case bündelt das Aktualisieren der entfernten Person mit dem
 *   Abschluss der lokalen Bildbearbeitung zu einer Anwendungsoperation.
 *
 * - Ein bisher gespeichertes Originalbild darf erst entfernt werden, nachdem
 *   der neue imagePath erfolgreich über das Repository übertragen wurde.
 *
 * Lernziele:
 *
 * - Mehrschrittige Anwendungslogik in einem Use Case kapseln.
 * - Lokale Ressourcen bei fehlgeschlagenen Netzwerkzugriffen schützen.
 * - Use Cases ohne ViewModel und Android-Lebenszyklus testen.
 */
