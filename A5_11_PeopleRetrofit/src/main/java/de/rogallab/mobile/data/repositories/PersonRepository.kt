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

   // Retrofit GET requests return one response instead of an observable database Flow.
   // This StateFlow keeps the current in-memory observation state for observeAll().
   private val _peopleStateFlow: MutableStateFlow<Result<List<Person>>> =
      MutableStateFlow(Result.success(emptyList()))

   override fun observeAll(): Flow<Result<List<Person>>> = flow {
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
         else Result.failure(exception)
      }
      catch (throwable: Throwable) {
         Result.failure(throwable)
      }

   override suspend fun create(person: Person): Result<Unit> =
      try {
         val created = _webservice.create(
            firstName = person.firstName.toTextPart(),
            lastName = person.lastName.toTextPart(),
            email = person.email?.toTextPart(),
            phone = person.phone?.toTextPart(),
            id = person.id.toTextPart(),
            image = person.imagePath.toImagePartOrNull(),
         ).toPerson()
         upsertPerson(created)
         Alog.d(TAG, "create: $created")
         Result.success(Unit)
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Result.failure(throwable)
      }

   override suspend fun update(person: Person): Result<Unit> =

      try {
         // Read the current observation state to distinguish keeping from removing an image.
         val current = currentPerson(person.id)
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
         upsertPerson(updated)
         Alog.d(TAG, "update: $updated")
         Result.success(Unit)
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Result.failure(throwable)
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
         Result.failure(throwable)
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
         Result.failure(throwable)
      }
   }

   // Return one Person from the current in-memory observation state.
   private fun currentPerson(id: String): Person? =
      _peopleStateFlow.value
         .getOrNull()
         ?.firstOrNull { person: Person -> person.id == id }

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

/*
 * Didaktik und Lernziele
 *
 * A5_01 und A5_11 implementieren dieselbe Schnittstelle IPersonRepository.
 * PeopleViewModel und PersonViewModel können deshalb weiterhin observeAll(),
 * findById(), create(), update() und remove() verwenden, obwohl die Datenquelle
 * vollständig ausgetauscht wurde.
 *
 * In A5_01 liefert Room die Beobachtbarkeit bereits mit:
 *
 *    IPersonDao.observeAll() -> Flow<List<PersonDto>>
 *
 * Room beobachtet die Tabelle. Nach INSERT, UPDATE oder DELETE wird
 * SELECT automatisch erneut ausgeführt und der Flow liefert eine neue Liste.
 * Das A5_01-Repository benötigt deshalb weder eine eigene Personenliste noch
 * einen MutableStateFlow.
 *
 * Retrofit arbeitet anders. GET /people liefert genau einen HTTP-Response mit
 * einem Snapshot des aktuellen Serverzustands. Nach diesem Response ist der
 * Request beendet; es existiert kein dauerhafter Flow, der spätere Änderungen
 * automatisch meldet.
 *
 * Damit die aus A5_01 übernommene Repository-Schnittstelle observeAll() trotzdem
 * erhalten bleiben kann, hält A5_11 den aktuell bekannten Serverzustand in
 * _peopleStateFlow. Dieser StateFlow ist keine lokale Datenbank, kein Offline-
 * Speicher und keine zusätzliche Persistenzschicht. Er existiert nur im Speicher
 * der laufenden App und stellt den beobachtbaren Zustand für die UI bereit.
 *
 * refresh() lädt die komplette Liste erneut mit GET /people und ersetzt diesen
 * Zustand. Nach erfolgreichem POST oder PUT wird die vom Server zurückgegebene
 * Person mit upsertPerson() in den Zustand übernommen. DELETE liefert keinen
 * Person-Body; deshalb entfernt removePersonFromState() die gelöschte Person nach
 * erfolgreicher Serverantwort. Laufende Collector von observeAll() erhalten so
 * unmittelbar den neuen Zustand.
 *
 * currentPerson() liest ebenfalls nur aus diesem In-Memory-Zustand. Beim Update
 * wird damit festgestellt, ob zuvor ein Serverbild vorhanden war. Dadurch können
 * drei Fälle unterschieden werden:
 *
 *    bestehende http(s)-URL  -> Serverbild beibehalten
 *    neuer lokaler Dateipfad -> neues Bild per Multipart hochladen
 *    imagePath == null       -> bestehendes Serverbild entfernen
 *
 * Eine zusätzliche lokale Room-Datenbank wäre ein anderer Architekturansatz.
 * Dann müsste geklärt werden, ob Room die Source of Truth ist und wie REST- und
 * lokale Daten synchronisiert werden. Diese Offline-First-/Synchronisationslogik
 * gehört bewusst nicht zum Lernziel von A5_11_PeopleRetrofit.
 *
 * CancellationException wird nicht in Result.failure umgewandelt, damit die
 * strukturierte Coroutine-Cancellation erhalten bleibt.
 */
