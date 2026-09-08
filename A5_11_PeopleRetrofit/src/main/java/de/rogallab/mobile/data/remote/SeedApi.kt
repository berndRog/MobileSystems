package de.rogallab.mobile.data.remote

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
            _personWeb.create(
               firstName = person.firstName.toTextPart(),
               lastName = person.lastName.toTextPart(),
               email = person.email?.toTextPart(),
               phone = person.phone?.toTextPart(),
               id = person.id.toTextPart(),
               image = person.imagePath.toImagePartOrNull(),
            )
         }

         // The server now owns the uploaded images. Remove temporary seed files.
         _seed.deleteLocalImages()

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
 *   über denselben multipart/form-data-Vertrag wie normale Create-Operationen.
 * - Damit werden auch die Seed-Bilder vom People-UseCase der WebAPI gespeichert und
 *   die resultierende ImageUrl serverseitig der jeweiligen Person zugeordnet.
 * - Nach erfolgreichem Upload sind die lokalen Drawable-Kopien nur noch temporäre
 *   Dateien und werden wieder aus dem privaten App-Verzeichnis entfernt.
 * - CancellationException wird nicht verschluckt, damit Coroutine-Cancellation
 *   weiterhin korrekt funktioniert.
 */
