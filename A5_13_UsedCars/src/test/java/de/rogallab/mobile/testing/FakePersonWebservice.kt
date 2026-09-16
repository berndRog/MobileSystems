package de.rogallab.mobile.testing

import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.remote.dtos.PersonDto
import okhttp3.MultipartBody
import retrofit2.Response

class FakePersonWebservice : IPersonWebservice {
   val calls = mutableListOf<String>()
   var uploadFailure: Throwable? = null
   var createdDto: PersonDto? = null
   var updatedDto: PersonDto? = null
   var uploadedPart: MultipartBody.Part? = null

   override suspend fun getAll(): List<PersonDto> = emptyList()
   override suspend fun countAll(): Int = 0
   override suspend fun getById(id: String): PersonDto = personDto(id)

   override suspend fun create(person: PersonDto): PersonDto {
      calls += "create"
      createdDto = person
      return person
   }

   override suspend fun update(id: String, person: PersonDto): PersonDto {
      calls += "update:$id"
      updatedDto = person
      return person.copy(id = id, imageUrl = IMAGE_URL)
   }

   override suspend fun uploadImage(id: String, file: MultipartBody.Part): PersonDto {
      calls += "uploadImage:$id"
      uploadedPart = file
      uploadFailure?.let { throw it }
      return personDto(id, IMAGE_URL)
   }

   override suspend fun deleteImage(id: String): PersonDto {
      calls += "deleteImage:$id"
      return personDto(id, null)
   }

   override suspend fun delete(id: String): Response<Unit> {
      calls += "delete:$id"
      return Response.success(Unit)
   }

   companion object {
      const val IMAGE_URL = "http://127.0.0.1:5082/usedcarsapi/v1/images/person.jpg"

      fun personDto(id: String = "p1", imageUrl: String? = null) = PersonDto(
         id = id,
         firstName = "Ada",
         lastName = "Lovelace",
         email = null,
         phone = null,
         imageUrl = imageUrl,
      )
   }
}
