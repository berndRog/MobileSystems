package de.rogallab.mobile.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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
   private lateinit var database: AppDatabase
   private lateinit var dao: IPersonDao

   @Before fun setup() {
      database = Room.inMemoryDatabaseBuilder(
         ApplicationProvider.getApplicationContext(), AppDatabase::class.java,
      ).setDriver(BundledSQLiteDriver()).allowMainThreadQueries().build()
      dao = database.createPersonDao()
   }
   @After fun tearDown() { database.close() }

   @Test fun insertAndObserveAll_returnsSortedPeople() = runTest {
      dao.insert(listOf(
         PersonDto("2", "Grace", "Hopper", "grace.hopper@example.org", null, null),
         PersonDto("1", "Ada", "Lovelace", "ada.lovelace@example.org", null, null),
      ))
      val people = dao.observeAll().first()
      assertEquals(listOf("Hopper", "Lovelace"), people.map(PersonDto::lastName))
   }
}
