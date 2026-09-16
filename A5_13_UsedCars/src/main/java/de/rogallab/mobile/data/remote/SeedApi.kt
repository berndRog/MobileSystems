package de.rogallab.mobile.data.remote

import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlin.coroutines.cancellation.CancellationException

class SeedApi(
   private val _personWeb: IPersonWebservice,
   private val _carWeb: ICarWebservice,
   private val _tDriveWeb: ITDriveWebservice,
   private val _imageFileStorage: IImageFileStorage,
   private val _seed: Seed,
) {
   suspend fun seed(): Boolean {
      val personIds = mutableListOf<String>()
      val carIds = mutableListOf<String>()
      val tDriveIds = mutableListOf<String>()
      val localImages = mutableListOf<String>()

      try {
         if (_personWeb.countAll() > 0 || _carWeb.countAll() > 0 ||
            _tDriveWeb.countAll() > 0) return false

         _seed.createPersonDtos().forEach { dto ->
            val created = _personWeb.create(dto.copy(imageUrl = null))
            personIds += created.id
            dto.imageUrl?.let { imagePath ->
               _personWeb.uploadImage(created.id, imagePath.toImageRequestPart())
               localImages += imagePath
            }
         }

         _seed.createCarDtos().forEach { dto ->
            val created = _carWeb.create(dto.copy(imageUrls = emptyList()))
            carIds += created.id
            dto.imageUrls.forEach { imagePath ->
               _carWeb.uploadImage(created.id, imagePath.toImageRequestPart())
               localImages += imagePath
            }
         }

         _seed.createsTestDriveDtos().forEach { dto ->
            tDriveIds += _tDriveWeb.create(dto).id
         }

         localImages.forEach { imagePath ->
            _imageFileStorage.deleteImageFromAppStorage(imagePath)
         }
         return true
      }
      catch (exception: CancellationException) {
         rollback(tDriveIds, carIds, personIds)
         throw exception
      }
      catch (throwable: Throwable) {
         Alog.e(TAG, "seed failed: ${throwable.message}")
         rollback(tDriveIds, carIds, personIds)
         return false
      }
   }

   private suspend fun rollback(
      tDriveIds: List<String>,
      carIds: List<String>,
      personIds: List<String>,
   ) {
      tDriveIds.asReversed().forEach { runCatching { _tDriveWeb.delete(it) } }
      carIds.asReversed().forEach { runCatching { _carWeb.delete(it) } }
      personIds.asReversed().forEach { runCatching { _personWeb.delete(it) } }
   }

   private companion object {
      const val TAG = "<-SeedApi"
   }
}
