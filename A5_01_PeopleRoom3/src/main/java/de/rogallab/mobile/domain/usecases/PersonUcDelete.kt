package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.shared.domain.utilities.resultOf
import kotlin.coroutines.cancellation.CancellationException
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

class PersonUcDelete(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   // Deletes the database entry before removing its image file.
   // This order prevents a failed repository call from leaving a broken image reference.
   suspend operator fun invoke(personId: String): Result<Unit> =
      resultOf(
         block = {
            val person = _repository.findById(personId).getOrThrow()
               ?: throw NoSuchElementException("Person $personId not found")

            _repository.remove(person).getOrThrow()

            // Cleanup must not turn a successful repository deletion into a failure.
            try {
               _imageFileStorage.deleteImageFromAppStorage(person.imagePath)
                  .onFailure { throwable ->
                     if (throwable is CancellationException) throw throwable
                     Alog.e(TAG, "delete person image failed: ${throwable.message}")
                  }
            } catch (e: CancellationException) {
               throw e
            } catch (e: Exception) {
               Alog.e(TAG, "delete person image failed: ${e.message}")
            }
            Unit
         },
         onFinally = {}
      )

   companion object {
      private const val TAG = "<-PersonUcDelete"
   }
}
