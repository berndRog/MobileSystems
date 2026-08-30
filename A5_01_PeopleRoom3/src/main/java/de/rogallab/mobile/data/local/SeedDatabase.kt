package de.rogallab.mobile.data.local

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlin.coroutines.cancellation.CancellationException

class SeedDatabase(
   private val _personDao: IPersonDao,
   private val _database: AppDatabase,
   private val _seed: Seed,
) {

   suspend fun seedPerson(): Boolean {
      try {
         if (_personDao.count() > 0) {
            Alog.d(TAG, "seed: database already seeded")
            return false
         }

         _database.clearAllTables()
         _seed.createPeopleList()
         _personDao.insert(
            _seed.people.map { person -> person.toPersonDto() }
         )
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
      private const val TAG = "<-SeedDatabase"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - SeedDatabase verwendet unmittelbar das lokale IPersonDao von A5_01.
 * - Die Beispieldaten werden nur in eine leere Datenbank geschrieben.
 */
