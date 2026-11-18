package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.resultOf
import kotlin.coroutines.cancellation.CancellationException
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

class PersonUcDelete(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   suspend operator fun invoke(person: Person): Result<Unit> =
      resultOf(
         block = {
            _repository.remove(person).getOrThrow()

            // Remote URLs belong to the server; only clean up local files.
            if (person.imagePath.isLocalImagePath()) {
               // Cleanup must not turn a successful repository deletion into a failure.
               try {
                  _imageFileStorage.deleteImageFromAppStorage(person.imagePath)
                     .onFailure { throwable ->
                        if (throwable is CancellationException) throw throwable
                        Alog.e(TAG, "delete local person image failed: ${throwable.message}")
                     }
               } catch (e: CancellationException) {
                  throw e
               } catch (e: Exception) {
                  Alog.e(TAG, "delete local person image failed: ${e.message}")
               }
            }
            Unit
         },
         onFinally = {}
      )

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
