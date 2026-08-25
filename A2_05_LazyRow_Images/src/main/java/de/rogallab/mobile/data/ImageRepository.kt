package de.rogallab.mobile.data

import de.rogallab.mobile.domain.IImageRepository
import de.rogallab.mobile.domain.entities.DogImage

class ImageRepository(
   private val _seed: Seed
): IImageRepository {

   override fun getAll(): Result<List<DogImage>> {
      return try {
         Result.success(_seed.dogs)
      } catch (e: Exception) {
         Result.failure(e)
      }
   }
}