package de.rogallab.mobile.testing

import de.rogallab.mobile.data.remote.ICarWebservice
import de.rogallab.mobile.data.remote.dtos.CarDto
import okhttp3.MultipartBody
import retrofit2.Response

class FakeCarWebservice : ICarWebservice {
   val calls = mutableListOf<String>()
   var current = carDto(imageUrls = listOf(IMAGE_URL))
   var createdDto: CarDto? = null
   var updatedDto: CarDto? = null
   var uploadFailure: Throwable? = null

   override suspend fun getAll(): List<CarDto> = listOf(current)
   override suspend fun countAll(): Int = 1
   override suspend fun getById(id: String): CarDto = current.copy(id = id)
   override suspend fun getByPersonId(id: String): List<CarDto> =
      listOf(current).filter { it.personId == id }

   override suspend fun create(car: CarDto): CarDto {
      calls += "create"
      createdDto = car
      current = car
      return current
   }

   override suspend fun update(id: String, car: CarDto): CarDto {
      calls += "update:$id"
      updatedDto = car
      current = car.copy(id = id, imageUrls = current.imageUrls)
      return current
   }

   override suspend fun uploadImage(id: String, file: MultipartBody.Part): CarDto {
      calls += "uploadImage:$id"
      uploadFailure?.let { throwable -> throw throwable }
      current = current.copy(imageUrls = current.imageUrls + UPLOADED_IMAGE_URL)
      return current
   }

   override suspend fun deleteImage(id: String, fileName: String): CarDto {
      calls += "deleteImage:$id:$fileName"
      current = current.copy(imageUrls = current.imageUrls.filterNot { it.endsWith(fileName) })
      return current
   }

   override suspend fun delete(id: String): Response<Unit> {
      calls += "delete:$id"
      return Response.success(Unit)
   }

   companion object {
      const val IMAGE_URL = "http://127.0.0.1:5082/usedcarsapi/v1/images/original.jpg"
      const val UPLOADED_IMAGE_URL = "http://127.0.0.1:5082/usedcarsapi/v1/images/uploaded.jpg"

      fun carDto(imageUrls: List<String> = emptyList()) = CarDto(
         id = "c1",
         manufacturer = "Fiat",
         model = "500",
         price = 18_900,
         imageUrls = imageUrls,
         personId = "p1",
      )
   }
}
