package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.mapping.toPerson
import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.remote.dtos.PersonDto
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class PersonRepository(
   private val _webservice: IPersonWebservice,
) : IPersonRepository {

   private val _peopleFlow: MutableStateFlow<Result<List<Person>>> =
      MutableStateFlow(Result.success(emptyList()))

   override fun observeAll(): Flow<Result<List<Person>>> = flow {
      refresh()
      emitAll(_peopleFlow)
   }

   override suspend fun findById(id: String): Result<Person?> =
      try {
         val person = _webservice.getById(id).toPerson()
         upsertCached(person)
         Result.success(person)
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (exception: HttpException) {
         if (exception.code() == 404) Result.success(null)
         else Result.failure(exception)
      }
      catch (throwable: Throwable) {
         Result.failure(throwable)
      }

   override suspend fun create(person: Person): Result<Unit> =
      resultOf {
         val created = _webservice.create(
            firstName = person.firstName.toTextPart(),
            lastName = person.lastName.toTextPart(),
            email = person.email?.toTextPart(),
            phone = person.phone?.toTextPart(),
            id = person.id.toTextPart(),
            image = person.imagePath.toImagePartOrNull(),
         ).toPerson()

         upsertCached(created)
         Alog.d(TAG, "create: $created")
      }

   override suspend fun update(person: Person): Result<Unit> =
      resultOf {
         val current = cachedPerson(person.id)
         val imagePart = person.imagePath.toImagePartOrNull()

         val removeImage =
            imagePart == null &&
               person.imagePath == null &&
               current?.imagePath != null

         val updated = _webservice.update(
            id = person.id,
            firstName = person.firstName.toTextPart(),
            lastName = person.lastName.toTextPart(),
            email = person.email?.toTextPart(),
            phone = person.phone?.toTextPart(),
            removeImage = removeImage.toString().toTextPart(),
            image = imagePart,
         ).toPerson()

         upsertCached(updated)
         Alog.d(TAG, "update: $updated")
      }

   override suspend fun remove(person: Person): Result<Unit> =
      resultOf {
         val response = _webservice.delete(person.id)
         if (!response.isSuccessful)
            throw HttpException(response)

         removeCached(person.id)
         Alog.d(TAG, "remove: $person")
      }

   private suspend fun refresh() {
      _peopleFlow.value = resultOf {
         val people = _webservice
            .getAll()
            .map(PersonDto::toPerson)

         sorted(people)
      }
   }

   private fun cachedPerson(id: String): Person? =
      _peopleFlow.value
         .getOrNull()
         ?.firstOrNull { person: Person -> person.id == id }

   private fun upsertCached(person: Person) {
      val people = _peopleFlow.value.getOrDefault(emptyList())
      val updated = people.filterNot { it.id == person.id } + person
      _peopleFlow.value = Result.success(sorted(updated))
   }

   private fun removeCached(id: String) {
      val people = _peopleFlow.value.getOrDefault(emptyList())
      _peopleFlow.value = Result.success(
         people.filterNot { person: Person -> person.id == id }
      )
   }

   private fun sorted(people: List<Person>): List<Person> =
      people.sortedWith(
         compareBy<Person> { person -> person.lastName.lowercase() }
            .thenBy { person -> person.firstName.lowercase() }
      )

   private fun String.toTextPart(): RequestBody =
      toRequestBody(TEXT_MEDIA_TYPE)

   private fun String?.toImagePartOrNull(): MultipartBody.Part? {
      val imagePath = this?.takeUnless(String::isBlank) ?: return null
      if (imagePath.startsWith("http://") || imagePath.startsWith("https://"))
         return null

      val file = File(imagePath)
      require(file.isFile) {
         "The selected image file does not exist: $imagePath"
      }

      val contentType = when (file.extension.lowercase()) {
         "jpg", "jpeg" -> "image/jpeg"
         "png" -> "image/png"
         "webp" -> "image/webp"
         else -> "application/octet-stream"
      }.toMediaType()

      return MultipartBody.Part.createFormData(
         "Image",
         file.name,
         file.asRequestBody(contentType),
      )
   }

   private suspend fun <T> resultOf(
      block: suspend () -> T,
   ): Result<T> =
      try {
         Result.success(block())
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Result.failure(throwable)
      }

   companion object {
      private const val TAG = "<-PersonRepository"
      private val TEXT_MEDIA_TYPE = "text/plain".toMediaType()
   }
}

/*
 * Didaktik und Lernziele
 *
 * - IPersonRepository bleibt gegenüber A5_01 unverändert. Aus Sicht der
 *   ViewModels wurde nur der Adapter hinter diesem Port ausgetauscht:
 *
 *      A5_01: ViewModel -> IPersonRepository -> Room/DAO -> SQLite
 *      A5_11: ViewModel -> IPersonRepository -> Retrofit -> PeopleApi
 *
 * - Eine REST-API liefert keinen beobachtbaren Room-Flow. PersonRepository hält
 *   deshalb die zuletzt geladene Serverliste in einem MutableStateFlow. Nach
 *   erfolgreichem POST, PUT oder DELETE wird derselbe Cache aktualisiert und die
 *   bereits bestehende PeopleViewModel-Beobachtung erhält automatisch neue Daten.
 *
 * - Create überträgt Personendaten und ein optionales lokales Bild gemeinsam als
 *   multipart/form-data. Die vom Server zurückgegebene ImageUrl ersetzt danach
 *   den temporären lokalen Pfad im Repository-Cache.
 *
 * - Update unterscheidet drei Bildzustände:
 *
 *      persistierte http(s)-URL -> vorhandenes Serverbild beibehalten
 *      lokaler Dateipfad        -> Bild als Multipart-Part ersetzen
 *      null                     -> RemoveImage=true, falls zuvor ein Bild existierte
 *
 * - Die eigentliche Reihenfolge von Bild speichern, Person ändern und altes Bild
 *   löschen liegt bewusst in PeopleApi. Der Android-Client beschreibt nur die
 *   gewünschte Änderung und orchestriert keine separaten /images-Aufrufe.
 *
 * - CancellationException wird weiterhin nicht in Result.failure umgewandelt,
 *   damit strukturierte Coroutine-Cancellation erhalten bleibt.
 */
