package de.rogallab.mobile.data.remote

import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlin.coroutines.cancellation.CancellationException

class SeedApi(
   private val _personWeb: IPersonWebservice,
   private val _seed: Seed,
) {

   suspend fun seedPerson(): Boolean {
      val createdIds = mutableListOf<String>()

      try {
         if (_personWeb.countAll() > 0) {
            Alog.d(TAG, "seed: api already seeded")
            return false
         }

         _seed.createPeopleList()

         for (person: Person in _seed.people) {
            val created = _personWeb.create(
               person.copy(imagePath = null).toPersonDto()
            )
            createdIds += created.id

            person.imagePath
               ?.takeUnless(String::isBlank)
               ?.let { imagePath ->
                  _personWeb.uploadImage(
                     id = created.id,
                     file = imagePath.toImageRequestPart(),
                  )
               }
         }

         // The server now owns the uploaded images.
         _seed.deleteLocalImages()

         Alog.i(TAG, "seed: ${_seed.people.size} people created")
         return true
      }
      catch (exception: CancellationException) {
         rollback(createdIds)
         throw exception
      }
      catch (throwable: Throwable) {
         rollback(createdIds)
         Alog.e(TAG, "seed: ${throwable.message}")
         return false
      }
   }

   private suspend fun rollback(createdIds: List<String>) {
      createdIds.asReversed().forEach { id: String ->
         try {
            _personWeb.delete(id)
         }
         catch (throwable: Throwable) {
            Alog.e(TAG, "seed rollback $id: ${throwable.message}")
         }
      }
   }

   companion object {
      private const val TAG = "<-SeedApi"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Vor dem Seeding fragt der Client nur GET /people/count ab. Die vollständige
 *   Personenliste muss für die Leerprüfung nicht übertragen werden.
 * - Ist die API leer, sendet SeedApi jede Person zuerst über den normalen JSON-
 *   Endpunkt. Das zugehörige Bild folgt danach über POST /people/{id}/image.
 * - Erst wenn alle Personen und Bilder erfolgreich gespeichert sind, werden die
 *   temporären Seed-Dateien auf Android gelöscht.
 * - Schlägt ein Request fehl, entfernt rollback() die in diesem Lauf bereits
 *   angelegten Personen. Der komplette Seed-Vorgang bleibt dadurch wiederholbar.
 * - CancellationException wird nicht verschluckt, damit Coroutine-Cancellation
 *   weiterhin korrekt funktioniert.
 */
