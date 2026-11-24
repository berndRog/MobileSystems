package de.rogallab.mobile.domain.usecases






import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.repositories.ICarRepository
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.shared.domain.utilities.resultOf

// Saves a car and completes its image edit session.
// The IImageEdit instance must be the same one the CarViewModel uses.
class CarUcCreate(
   private val _repository: ICarRepository,
   private val _imageEdit: IImageEdit,
) {
   suspend operator fun invoke(car: Car): Result<Unit> =
      resultOf(
         block = {
            _repository.create(car).getOrThrow()

            // Commit the image edit session only after successful persistence.
            // On failure all images remain, so the user can retry or cancel.
            _imageEdit.commit()
         }
      )
}
