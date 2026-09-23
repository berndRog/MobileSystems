package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.images.IImageEdit

class PersonUcCreate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {

      // Creates the remote person before committing the local image edit session.
      val result = _repository.create(person)

      if (result.isSuccess) {
         // Commit the image edit session only after a successful server write.
         _imageEdit.commit()
      }
      // A failed server write keeps the selected image available for retry.

      return result
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Use Case beschreibt die vollständige Anwendungsoperation
 *   "Person über REST anlegen" und koordiniert Repository und ImageEdit.
 *
 * - Die Bild-Session wird erst nach einem erfolgreichen Serverzugriff
 *   übernommen. Bei einem Netzwerkfehler bleibt das lokale Bild erhalten,
 *   sodass der Benutzer den Speichervorgang erneut versuchen kann.
 *
 * Lernziele:
 *
 * - Zusammengesetzte Anwendungslogik aus dem ViewModel auslagern.
 * - Use Cases unabhängig von Retrofit und Android UI formulieren.
 * - Die Reihenfolge von Serverzugriff und lokaler Bildverwaltung begründen.
 */
