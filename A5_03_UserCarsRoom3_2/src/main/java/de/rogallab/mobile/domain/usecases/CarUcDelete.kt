package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.repositories.ICarRepository
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.resultOf

class CarUcDelete(
   private val _repository: ICarRepository,
   private val _imageFileStorage: IImageFileStorage,
) {
   // Loads the aggregate, deletes the database entry, then removes its image files.
   suspend operator fun invoke(carId: String): Result<Unit> =
      resultOf(
         block = {
            val car = _repository.findById(carId).getOrThrow()
               ?: throw NoSuchElementException("Car $carId not found")

            // ForeignKey.RESTRICT fails here if test drives still reference the car.
            // The exception becomes Result.failure and all images are kept.
            _repository.remove(car).getOrThrow()

            // Cleanup must not turn a successful database deletion into a failure.
            // deleteImageFromAppStorage already returns a Result and never throws.
            car.imagePaths.forEach { imagePath ->
               _imageFileStorage.deleteImageFromAppStorage(imagePath)
                  .onFailure { throwable ->
                     Alog.e(TAG, "delete car image failed: ${throwable.message}")
                  }
            }
         }
      )

   companion object {
      private const val TAG = "<-CarUcDelete"
   }
}