package de.rogallab.mobile.data.remote

import de.rogallab.mobile.data.mapping.toPersonDto
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

         _seed.people.map { person ->
            //person.toPersonDto()
            _personWeb.create(
               firstName = person.firstName,
               lastName = person.lastName,

            )
         }
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
 * - SeedDatabase verwendet unmittelbar das lokale IPersonDao von A5_01.
 * - Die Beispieldaten werden nur in eine leere Datenbank geschrieben.
 */
