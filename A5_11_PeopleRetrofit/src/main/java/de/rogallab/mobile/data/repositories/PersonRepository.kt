package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.mapping.toPerson
import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.remote.dtos.PersonDto
import de.rogallab.mobile.data.remote.toImagePartOrNull
import de.rogallab.mobile.data.remote.toTextPart
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

class PersonRepository(
   private val _webservice: IPersonWebservice,
) : IPersonRepository {

   // Retrofit GET requests return one response, not an observable database Flow.
   // Keep the last known server list in memory so observeAll() can remain reactive.
   private val _peopleFlow: MutableStateFlow<Result<List<Person>>> =
      MutableStateFlow(Result.success(emptyList()))

   override fun observeAll(): Flow<Result<List<Person>>> = flow {
      // Load one fresh snapshot from the server before exposing the local StateFlow.
      refresh()
      Alog.d(TAG, "observeAll: emit cached list")
      emitAll(_peopleFlow)
   }

   override suspend fun findById(id: String): Result<Person?> =
      try {
         // GET one person from the server and keep the observation state consistent.
         val person = _webservice.getById(id).toPerson()
         upsertCached(person)
         Result.success(person)
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (exception: HttpException) {
         // HTTP 404 represents the valid result "person does not exist".
         if (exception.code() == 404) Result.success(null)
         else Result.failure(exception)
      }
      catch (throwable: Throwable) {
         Result.failure(throwable)
      }

   override suspend fun create(person: Person): Result<Unit> =
      resultOf {
         // Send person data and an optional local image in one multipart request.
         val created = _webservice.create(
            firstName = person.firstName.toTextPart(),
            lastName = person.lastName.toTextPart(),
            email = person.email?.toTextPart(),
            phone = person.phone?.toTextPart(),
            id = person.id.toTextPart(),
            image = person.imagePath.toImagePartOrNull(),
         ).toPerson()

         // Use the server response because it may contain the generated image URL.
         upsertCached(created)
         Alog.d(TAG, "create: $created")
      }

   override suspend fun update(person: Person): Result<Unit> =
      resultOf {
         // Read the last known state to distinguish keeping from removing an image.
         val current = cachedPerson(person.id)
         val imagePart = person.imagePath.toImagePartOrNull()

         // null after an existing server image means: remove the persisted image.
         val removeImage =
            imagePart == null &&
               person.imagePath == null &&
               current?.imagePath != null

         // A local file is uploaded; an existing http(s) URL is not uploaded again.
         val updated = _webservice.update(
            id = person.id,
            firstName = person.firstName.toTextPart(),
            lastName = person.lastName.toTextPart(),
            email = person.email?.toTextPart(),
            phone = person.phone?.toTextPart(),
            removeImage = removeImage.toString().toTextPart(),
            image = imagePart,
         ).toPerson()

         // Replace the local observation state with the authoritative server result.
         upsertCached(updated)
         Alog.d(TAG, "update: $updated")
      }

   override suspend fun remove(person: Person): Result<Unit> =
      resultOf {
         // DELETE returns no Person body, therefore the local list is changed explicitly.
         val response = _webservice.delete(person.id)
         if (!response.isSuccessful)
            throw HttpException(response)

         removeCached(person.id)
         Alog.d(TAG, "remove: $person")
      }

   private suspend fun refresh() {
      // Replace the complete local observation state with the current server snapshot.
      _peopleFlow.value = resultOf {
         val people = _webservice
            .getAll()
            .map(PersonDto::toPerson)
         Alog.d(TAG, "observeAll: get webApi: ${people.count()} people")
         sorted(people)
      }
   }

   // Return a Person from the last known server snapshot held for observeAll().
   // This local state is not a Room database and not a persistent/offline cache.
   private fun cachedPerson(id: String): Person? =
      _peopleFlow.value
         .getOrNull()
         ?.firstOrNull { person: Person -> person.id == id }

   // Insert or replace one Person in the local observation state after GET/POST/PUT.
   private fun upsertCached(person: Person) {
      val people = _peopleFlow.value.getOrDefault(emptyList())
      val updated = people.filterNot { it.id == person.id } + person
      _peopleFlow.value = Result.success(sorted(updated))
   }

   // Remove one Person from the local observation state after a successful DELETE.
   private fun removeCached(id: String) {
      val people = _peopleFlow.value.getOrDefault(emptyList())
      _peopleFlow.value = Result.success(
         people.filterNot { person: Person -> person.id == id }
      )
   }

   // Keep the same visible order as the server/Room example: last name, first name.
   private fun sorted(people: List<Person>): List<Person> =
      people.sortedWith(
         compareBy<Person> { person -> person.lastName.lowercase() }
            .thenBy { person -> person.firstName.lowercase() }
      )

   // Convert technical failures into Result while preserving coroutine cancellation.
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
   }
}

/*
 * Didaktik und Lernziele
 *
 * A5_01 und A5_11 implementieren weiterhin dieselbe Schnittstelle
 * IPersonRepository. Dadurch können PeopleViewModel und PersonViewModel beim
 * Wechsel von Room zu Retrofit unverändert mit observeAll(), findById(), create(),
 * update() und remove() arbeiten.
 *
 * Der wesentliche Unterschied liegt bei observeAll():
 *
 * A5_01 / Room:
 *    IPersonDao.observeAll() liefert selbst einen Flow<List<PersonDto>>.
 *    Room beobachtet die beteiligte Tabelle. Nach INSERT, UPDATE oder DELETE wird
 *    die SELECT-Abfrage erneut ausgeführt und der Flow liefert automatisch die
 *    neue Liste. Das Repository benötigt deshalb keine eigene Personenliste.
 *
 * A5_11 / Retrofit:
 *    GET /people liefert nur eine einzelne Antwort, also einen momentanen Snapshot
 *    des Serverzustands. HTTP stellt keinen dauerhaften Flow bereit. Damit die
 *    bestehende Repository-Schnittstelle observeAll() trotzdem erhalten bleibt,
 *    hält PersonRepository die zuletzt bekannte Serverliste in _peopleFlow.
 *
 * _peopleFlow ist damit kein Ersatz für Room und auch kein persistenter Offline-
 * Cache. Er ist lediglich ein clientseitiger Beobachtungszustand für die laufende
 * App. refresh() ersetzt ihn durch einen neuen GET-Snapshot. Nach erfolgreichem
 * POST oder PUT wird die vom Server zurückgegebene Person mit upsertCached()
 * eingetragen; nach DELETE entfernt removeCached() die Person. Dadurch erhält ein
 * bereits laufender Collector von observeAll() sofort den neuen Zustand.
 *
 * cachedPerson() liest ebenfalls nur aus diesem letzten bekannten Zustand. Die
 * Methode wird beim Update benötigt, um die drei Bildfälle unterscheiden zu können:
 *
 *    bestehende http(s)-URL  -> Serverbild beibehalten
 *    neuer lokaler Dateipfad -> neues Bild per Multipart hochladen
 *    imagePath == null       -> bestehendes Serverbild entfernen
 *
 * Create und Update senden Personendaten und ein optionales lokales Bild als
 * multipart/form-data. Die PeopleApi übernimmt das Speichern, Ersetzen bzw.
 * Löschen der Bilddatei und liefert die persistierte ImageUrl zurück.
 *
 * CancellationException wird nicht in Result.failure umgewandelt, damit die
 * strukturierte Coroutine-Cancellation erhalten bleibt.
 */
