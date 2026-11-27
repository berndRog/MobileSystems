package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.mapping.toPerson
import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.remote.dtos.PersonDto
import de.rogallab.mobile.data.remote.isRemoteImageUrl
import de.rogallab.mobile.data.remote.toImageRequestPart
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

class PersonRepository(
   private val _webservice: IPersonWebservice,
   private val _networkExceptionMapper: NetworkExceptionMapper,
) : IPersonRepository {

   // Retrofit GET requests return one response instead of an observable database Flow.
   // This StateFlow keeps the current in-memory observation state for observeAll().
   private val _peopleStateFlow: MutableStateFlow<Result<List<Person>>> =
      MutableStateFlow(Result.success(emptyList()))

   fun observeAll(): Flow<Result<List<Person>>> = flow {
      // Load a fresh server snapshot before forwarding subsequent state changes.
      refresh()
      Alog.d(TAG, "observeAll: emit observation state")
      emitAll(_peopleStateFlow)
   }

   override suspend fun findById(id: String): Result<Person?> =
      try {
         // GET one person from the server and synchronize the observation state.
         val person = _webservice.getById(id).toPerson()
         upsertPerson(person)
         Alog.d(TAG, "findById: $person")
         Result.success(person)
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (exception: HttpException) {
         // HTTP 404 represents the valid result "person does not exist".
         if (exception.code() == 404) Result.success(null)
         else Result.failure(_networkExceptionMapper.map(exception))
      }
      catch (throwable: Throwable) {
         Result.failure(_networkExceptionMapper.map(throwable))
      }

   override suspend fun create(person: Person): Result<Unit> =
      try {
         // Validate and build a possible file part before creating the resource.
         val imagePart = person.imagePath
            ?.takeUnless(String::isBlank)
            ?.takeUnless(String::isRemoteImageUrl)
            ?.toImageRequestPart()

         // ImageUrl is server-managed in PeopleImagesApi and is therefore not
         // part of the JSON create operation.
         val created = _webservice
            .create(person.copy(imagePath = null).toPersonDto())
            .toPerson()

         val persisted = try {
            if (imagePart == null) created
            else _webservice.uploadImage(created.id, imagePart).toPerson()
         }
         catch (exception: CancellationException) {
            rollbackCreate(created.id)
            throw exception
         }
         catch (throwable: Throwable) {
            // Keep Create retryable when the second request fails.
            rollbackCreate(created.id)
            throw throwable
         }

         upsertPerson(persisted)
         Alog.d(TAG, "create: $persisted")
         Result.success(Unit)
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Result.failure(_networkExceptionMapper.map(throwable))
      }

   override suspend fun update(person: Person): Result<Unit> =
      try {
         // The JSON update changes only Person properties. PeopleImagesApi keeps
         // its current ImageUrl until one of the separate image endpoints runs.
         val jsonUpdated = _webservice.update(
            id = person.id,
            person = person.copy(imagePath = null).toPersonDto(),
         ).toPerson()

         val imagePath = person.imagePath?.takeUnless(String::isBlank)
         val updated = when {
            imagePath == null ->
               _webservice.deleteImage(person.id).toPerson()

            imagePath.isRemoteImageUrl() ->
               jsonUpdated

            else ->
               _webservice
                  .uploadImage(person.id, imagePath.toImageRequestPart())
                  .toPerson()
         }

         // Replace the local observation state with the authoritative server result.
         upsertPerson(updated)
         Alog.d(TAG, "update: $updated")
         Result.success(Unit)
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Result.failure(_networkExceptionMapper.map(throwable))
      }

   // POST Person and POST Image are two independent HTTP operations. If the
   // second request fails, remove the newly created Person so Save can be retried.
   private suspend fun rollbackCreate(id: String) {
      try {
         val response = _webservice.delete(id)
         if (!response.isSuccessful)
            Alog.e(TAG, "rollback create failed: HTTP ${response.code()}")
      }
      catch (throwable: Throwable) {
         Alog.e(TAG, "rollback create failed: ${throwable.message}")
      }
   }

   override suspend fun remove(person: Person): Result<Unit> =
      try {
         // DELETE returns no Person body, therefore the observation state is changed explicitly.
         val response = _webservice.delete(person.id)
         if (!response.isSuccessful)
            throw HttpException(response)

         removePersonFromState(person.id)
         Alog.d(TAG, "remove: $person")
         Result.success(Unit)
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Result.failure(_networkExceptionMapper.map(throwable))
      }

   private suspend fun refresh() {
      // Replace the complete observation state with the current server snapshot.
      _peopleStateFlow.value = try {
         val people = _webservice
            .getAll()
            .map(PersonDto::toPerson)
         Alog.d(TAG, "refresh: get webApi: ${people.count()} people")
         Result.success(sorted(people))
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Result.failure(_networkExceptionMapper.map(throwable))
      }
   }

   // Insert or replace one Person after a successful GET, POST or PUT request.
   private fun upsertPerson(person: Person) {
      val people = _peopleStateFlow.value.getOrDefault(emptyList())
      val updated = people.filterNot { it.id == person.id } + person
      _peopleStateFlow.value = Result.success(sorted(updated))
   }

   // Remove one Person from the observation state after a successful DELETE request.
   private fun removePersonFromState(id: String) {
      val people = _peopleStateFlow.value.getOrDefault(emptyList())
      _peopleStateFlow.value = Result.success(
         people.filterNot { person: Person -> person.id == id }
      )
   }

   // Keep the same visible order as the server and the Room example.
   private fun sorted(people: List<Person>): List<Person> =
      people.sortedWith(
         compareBy<Person> { person -> person.lastName.lowercase() }
            .thenBy { person -> person.firstName.lowercase() }
      )

   companion object {
      private const val TAG = "<-PersonRepository"
   }
}
