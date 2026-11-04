package de.rogallab.mobile.testing

import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.remote.dtos.PersonDto
import retrofit2.Response

class FakePersonWebservice : IPersonWebservice {
   val people = listOf(
      PersonDto(id = "p1", firstName = "Ada", lastName = "Lovelace"),
      PersonDto(id = "p2", firstName = "Grace", lastName = "Hopper"),
   )
   var deleteResponse: Response<Unit> = Response.success(Unit)
   var deletedId: String? = null

   override suspend fun getAll() = people
   override suspend fun countAll() = people.size
   override suspend fun getById(id: String) = people.first { it.id == id }
   override suspend fun create(person: PersonDto): PersonDto = error("unused")
   override suspend fun update(id: String, person: PersonDto): PersonDto = error("unused")
   override suspend fun delete(id: String): Response<Unit> {
      deletedId = id
      return deleteResponse
   }
}
