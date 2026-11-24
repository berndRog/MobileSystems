package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.repositories.ITDriveRepository
import de.rogallab.mobile.shared.domain.utilities.resultOf

// Deletes a test drive.
// Receives only the id (the list holds projections) and loads the aggregate,
// like Person and Car, so all list screens delete the same way.
class TDriveUcDelete(
   private val _repository: ITDriveRepository,
) {
   suspend operator fun invoke(tDriveId: String): Result<Unit> =
      resultOf(
         block = {
            val tDrive = _repository.findById(tDriveId).getOrThrow()
               ?: throw NoSuchElementException("Test drive $tDriveId not found")
            _repository.remove(tDrive).getOrThrow()
         }
      )
}
