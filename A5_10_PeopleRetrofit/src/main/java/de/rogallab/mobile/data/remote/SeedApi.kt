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
      try {
         if (_personWeb.countAll() > 0) {
            Alog.d(TAG, "seed: api already seeded")
            return false
         }

         _seed.createPeopleList()

         for (person: Person in _seed.people) {
            _personWeb.create(person.toPersonDto())
         }

         Alog.i(TAG, "seed: ${_seed.people.size} people created")
         return true
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Alog.e(TAG, "seed: ${throwable.message}")
         return false
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
 * - Ist die API leer, erzeugt Seed die Beispieldaten und SeedApi sendet jede Person
 *   über denselben JSON-Vertrag wie normale Create-Operationen.
 * - Die erzeugten Bilddateien bleiben im privaten Android-App-Verzeichnis. Nur
 *   ihr lokaler Pfad wird als imageUrl-String von der PeopleApi gespeichert.
 *   Die API empfängt und verarbeitet keine Bilddaten.
 * - CancellationException wird nicht verschluckt, damit Coroutine-Cancellation
 *   weiterhin korrekt funktioniert.
 */
