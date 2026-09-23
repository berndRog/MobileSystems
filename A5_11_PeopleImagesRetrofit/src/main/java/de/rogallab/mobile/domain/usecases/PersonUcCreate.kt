package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

class PersonUcCreate(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {

      // The repository completes JSON creation and a possible image upload first.
      val result = _repository.create(person)

      if (result.isSuccess) {
         // A local picker file is transport state and can now be removed.
         deleteLocalImageQuietly(person.imagePath)
      }
      // A failed upload keeps the local image available for another attempt.

      return result
   }

   private suspend fun deleteLocalImageQuietly(imagePath: String?) {
      if (!imagePath.isLocalImagePath()) return

      _imageFileStorage.deleteImageFromAppStorage(imagePath)
         .onFailure { throwable ->
            Alog.e(TAG, "delete temporary image failed: ${throwable.message}")
         }
   }

   companion object {
      private const val TAG = "<-PersonUcCreate"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Use Case umfasst das Anlegen der Person, einen möglichen Multipart-
 *   Upload im Repository und das anschließende Aufräumen der lokalen Datei.
 *
 * - Erst nach dem vollständigen Erfolg besitzt PeopleImagesApi eine dauerhafte
 *   Bildkopie. Bei einem Fehler bleibt die lokale Datei für einen Retry erhalten.
 *
 * Lernziele:
 *
 * - Eine Anwendungsoperation über Server- und Dateigrenzen hinweg koordinieren.
 * - Persistente Serverbilder von temporärem lokalem Transportzustand trennen.
 * - Fehlerpfade gezielt und unabhängig vom ViewModel testen.
 */
