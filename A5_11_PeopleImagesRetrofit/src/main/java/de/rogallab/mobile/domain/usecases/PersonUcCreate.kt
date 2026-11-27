package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.resultOf
import kotlin.coroutines.cancellation.CancellationException
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

class PersonUcCreate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
   private val _imageFileStorage: IImageFileStorage,
) {

   suspend operator fun invoke(person: Person): Result<Unit> =
      resultOf(
         block = {
            _repository.create(person).getOrThrow()

            // Remove local transport state only after all server requests succeed.
            deleteLocalImageQuietly(person.imagePath)
            _imageEdit.commit()
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
