package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

class PersonUcUpdate(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {

      // The repository completes JSON and image requests before cleanup starts.
      val result = _repository.update(person)

      if (result.isSuccess) {
         // Delete only a local replacement; remote URLs belong to the server.
         deleteLocalImageQuietly(person.imagePath)
      }
      // A failed update keeps a local replacement available for retry.

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
      private const val TAG = "<-PersonUcUpdate"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Use Case kapselt die vollständige Aktualisierung einschließlich eines
 *   möglichen Bild-Uploads oder der serverseitigen Bildlöschung.
 *
 * - Eine lokale Ersatzdatei wird erst nach Erfolg entfernt. Eine HTTP(S)-URL
 *   wird nie an den lokalen Dateispeicher übergeben, da sie dem Server gehört.
 *
 * Lernziele:
 *
 * - Lokale Dateipfade und entfernte Ressourcen sicher unterscheiden.
 * - Mehrschrittige REST-Operationen hinter einem Use Case verbergen.
 * - Aufräumlogik erst nach erfolgreicher Persistenz ausführen.
 */
