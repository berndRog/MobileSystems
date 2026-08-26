package de.rogallab.mobile.shared.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.shared.data.local.database.AppDatabasePerson
import de.rogallab.mobile.shared.data.local.dtos.PersonDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonDaoInstrumentedTest {

   private lateinit var database: AppDatabasePerson
   private lateinit var dao: IPersonDao

   @Before
   fun setUp() {
      database = Room
         .inMemoryDatabaseBuilder<AppDatabasePerson>()
         .setDriver(BundledSQLiteDriver())
         .setQueryCoroutineContext(Dispatchers.IO)
         .build()

      dao = database.createPersonDao()
   }

   @After
   fun tearDown() {
      database.close()
   }

   @Test
   fun insert_countAndFindById_returnStoredPerson() = runTest {
      val person = PersonDto(
         firstName = "Ada",
         lastName = "Lovelace",
         id = "p1",
      )

      dao.insert(person)

      assertEquals(1, dao.count())
      assertEquals(person, dao.findById("p1"))
   }

   @Test
   fun findById_unknownId_returnsNull() = runTest {
      assertNull(dao.findById("missing"))
   }

   @Test
   fun observeAll_ordersPeopleByFirstName() = runTest {
      dao.insert(PersonDto(firstName = "Grace", lastName = "Hopper", id = "p2"))
      dao.insert(PersonDto(firstName = "Ada", lastName = "Lovelace", id = "p1"))

      val people = dao.observeAll().first()

      assertEquals(
         listOf("Ada", "Grace"),
         people.map(PersonDto::firstName)
      )
   }

   @Test
   fun update_replacesStoredValuesForSamePrimaryKey() = runTest {
      val original = PersonDto(
         firstName = "Ada",
         lastName = "Lovelace",
         id = "p1",
      )
      dao.insert(original)

      val changed = original.copy(
         email = "ada@example.org",
         phone = "+49 123",
      )
      dao.update(changed)

      assertEquals(changed, dao.findById("p1"))
      assertEquals(1, dao.count())
   }

   @Test
   fun delete_removesStoredPerson() = runTest {
      val person = PersonDto(
         firstName = "Ada",
         lastName = "Lovelace",
         id = "p1",
      )
      dao.insert(person)

      dao.delete(person)

      assertEquals(0, dao.count())
      assertNull(dao.findById("p1"))
   }

   @Test
   fun insert_duplicatePrimaryKey_aborts() = runTest {
      dao.insert(
         PersonDto(firstName = "Ada", lastName = "Lovelace", id = "p1")
      )

      try {
         dao.insert(
            PersonDto(firstName = "Grace", lastName = "Hopper", id = "p1")
         )
         fail("Expected duplicate primary key to abort the insert")
      }
      catch (_: Exception) {
         // Expected: OnConflictStrategy.ABORT rejects the second row.
      }

      assertEquals(1, dao.count())
      assertEquals("Ada", dao.findById("p1")?.firstName)
   }
}
