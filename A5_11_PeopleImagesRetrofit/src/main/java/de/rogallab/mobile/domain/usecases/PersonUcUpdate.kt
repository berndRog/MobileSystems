package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.resultOf
import kotlin.coroutines.cancellation.CancellationException
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

class PersonUcUpdate(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   suspend operator fun invoke(person: Person): Result<Unit> =
      resultOf(
         block = {
            _repository.update(person).getOrThrow()

            // Remove local transport state only after all server requests succeed.
            deleteLocalImageQuietly(person.imagePath)
         },
         onFinally = {}
      )

   private suspend fun deleteLocalImageQuietly(imagePath: String?) {
      if (!imagePath.isLocalImagePath()) return

      try {
         _imageFileStorage.deleteImageFromAppStorage(imagePath)
            .onFailure { throwable ->
               if (throwable is CancellationException) throw throwable
               Alog.e(TAG, "delete temporary image failed: ${throwable.message}")
            }
      } catch (e: CancellationException) {
         throw e
      } catch (e: Exception) {
         Alog.e(TAG, "delete temporary image failed: ${e.message}")
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
