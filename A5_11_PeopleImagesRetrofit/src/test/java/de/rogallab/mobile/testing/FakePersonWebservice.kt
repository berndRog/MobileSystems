package de.rogallab.mobile.testing

import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.remote.dtos.PersonDto
import okhttp3.MultipartBody
import retrofit2.Response

class FakePersonWebservice : IPersonWebservice {
   val calls = mutableListOf<String>()

   var people = mutableListOf<PersonDto>()
   var createResult = personDto()
   var updateResult = personDto()
   var uploadResult = personDto(imageUrl = IMAGE_URL)
   var deleteImageResult = personDto(imageUrl = null)
   var uploadFailure: Throwable? = null

   var createdDto: PersonDto? = null
   var updatedDto: PersonDto? = null
   var uploadedPart: MultipartBody.Part? = null

   override suspend fun getAll(): List<PersonDto> {
      calls += "getAll"
      return people
   }

   override suspend fun countAll(): Int {
      calls += "countAll"
      return people.size
   }

   override suspend fun getById(id: String): PersonDto {
      calls += "getById:$id"
      return people.first { person -> person.id == id }
   }

   override suspend fun create(person: PersonDto): PersonDto {
      calls += "create"
      createdDto = person
      return createResult.copy(id = person.id)
   }

   override suspend fun update(id: String, person: PersonDto): PersonDto {
      calls += "update:$id"
      updatedDto = person
      return updateResult.copy(id = id)
   }

   override suspend fun uploadImage(
      id: String,
      file: MultipartBody.Part,
   ): PersonDto {
      calls += "uploadImage:$id"
      uploadedPart = file
      uploadFailure?.let { throwable -> throw throwable }
      return uploadResult.copy(id = id)
   }

   override suspend fun deleteImage(id: String): PersonDto {
      calls += "deleteImage:$id"
      return deleteImageResult.copy(id = id)
   }

   override suspend fun delete(id: String): Response<Unit> {
      calls += "delete:$id"
      return Response.success(Unit)
   }

   companion object {
      const val IMAGE_URL =
         "http://127.0.0.1:5081/peopleapi/v1/images/person.jpg"

      fun personDto(
         id: String = "p1",
         imageUrl: String? = null,
      ) = PersonDto(
         id = id,
         firstName = "Ada",
         lastName = "Lovelace",
         email = null,
         phone = null,
         imageUrl = imageUrl,
      )
   }
}
