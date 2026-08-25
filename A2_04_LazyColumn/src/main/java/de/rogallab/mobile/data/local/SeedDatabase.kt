package de.rogallab.mobile.data.local

import androidx.room3.RoomDatabase
import de.rogallab.mobile.shared.data.IPersonDao
import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.shared.domain.utilities.Alog
import org.koin.core.component.KoinComponent

class SeedDatabase(
   private val _personDao: IPersonDao,
   private val _database: RoomDatabase,
   private val _seed: Seed
) : KoinComponent {

   suspend fun seedPerson(): Boolean {
      try {
         _personDao.count().let { count ->
            if (count > 0) {
               Alog.d("<-SeedDatabase", "seed: Database already seeded")
               return false
            }
         }
         _database.clearAllTables()

         _seed.createPeopleList()
         for (person in _seed.people) {
            _personDao.insert(person.toPersonDto())
         }
         return true
      } catch (e: Exception) {
         Alog.e("<-SeedDatabase", "seed: ${e.message}")
      }
      return false
   }
}