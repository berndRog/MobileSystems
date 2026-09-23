package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

class PersonUcDelete(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {

      // The server deletes the person and its associated server-side image.
      val result = _repository.remove(person)

      if (result.isSuccess && person.imagePath.isLocalImagePath()) {
         // Local cleanup is best effort; a remote URL must never be deleted here.
         _imageFileStorage.deleteImageFromAppStorage(person.imagePath)
            .onFailure { throwable ->
               Alog.e(TAG, "delete local person image failed: ${throwable.message}")
            }
      }

      return result
   }

   companion object {
      private const val TAG = "<-PersonUcDelete"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - PeopleImagesApi besitzt persistierte Bilder und entfernt sie zusammen mit
 *   der Person. Android darf eine zurückgegebene HTTP(S)-URL nicht lokal löschen.
 *
 * - Nur falls eine Person ausnahmsweise noch auf eine lokale temporäre Datei
 *   verweist, wird diese nach erfolgreicher Serverlöschung bestmöglich bereinigt.
 *
 * Lernziele:
 *
 * - Verantwortungsgrenzen zwischen Client und Server erkennen.
 * - Ressourcen nur über den jeweils zuständigen Speichermechanismus löschen.
 * - Serverfehler von nachgelagertem lokalem Aufräumen unterscheiden.
 */
