package de.rogallab.mobile.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.dtos.PersonDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonDaoTest {
   private lateinit var _database: AppDatabase
   private lateinit var _dao: IPersonDao

   @Before
   fun setup() {
      _database = Room.inMemoryDatabaseBuilder(
         ApplicationProvider.getApplicationContext(),
         AppDatabase::class.java
      )
         .setDriver(AndroidSQLiteDriver())
         .allowMainThreadQueries()
         .build()

      _dao = _database.createPersonDao()
   }

   @After
   fun tearDown() {
      _database.close()
   }

   @Test
   fun insertAndSelectAll_returnsSortedPeople() = runTest {
      _dao.insert(
         listOf(
            PersonDto(
               id = "2",
               firstName = "Grace",
               lastName = "Hopper",
               email = "grace.hopper@example.org",
               phone = null,
               imagePath = null,
            ),
            PersonDto(
               id = "1",
               firstName = "Ada",
               lastName = "Lovelace",
               email = "ada.lovelace@example.org",
               phone = null,
               imagePath = null,
            )
         )
      )

      val people = _dao.selectAll().first()

      assertEquals(
         listOf("Hopper", "Lovelace"),
         people.map(PersonDto::lastName)
      )
   }
}
