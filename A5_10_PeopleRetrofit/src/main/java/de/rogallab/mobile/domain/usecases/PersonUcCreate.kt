package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.images.IImageEdit

class PersonUcCreate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {

      // Creates the person before committing the image edit session.
      val result = _repository.create(person)

      if (result.isSuccess) {
         // Commit the image edit session only after a successful repository write.
         _imageEdit.commit()
      }
      // A failed repository write keeps the selected image available for retry.

      return result
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Use Case beschreibt die vollständige Anwendungsoperation
 *   "Person mit Bild anlegen" und koordiniert dafür Repository und ImageEdit.
 *
 * - Die Bild-Session wird erst nach einem erfolgreichen Repository-Zugriff
 *   übernommen. Bei einem Fehler kann der Benutzer den Speichervorgang erneut
 *   versuchen oder die Bearbeitung abbrechen.
 *
 * Lernziele:
 *
 * - Zusammengesetzte Anwendungslogik aus dem ViewModel auslagern.
 * - Technische Abhängigkeiten über Constructor Injection bereitstellen.
 * - Die Reihenfolge von Datenbankzugriff und Bildverwaltung begründen.
 */
