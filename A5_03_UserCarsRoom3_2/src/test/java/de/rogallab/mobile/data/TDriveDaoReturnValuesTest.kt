package de.rogallab.mobile.data

import android.app.Application
import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.dtos.CarDto
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TDriveDaoReturnValuesTest {
   private lateinit var database: AppDatabase
   private lateinit var dao: ITDriveDao
   private val person = TDriveDto("t1", "seller", "c1", Instant.parse("2026-01-01T12:00:00Z"), false)

   @Before
   fun setUp() = runTest {
      database = Room.inMemoryDatabaseBuilder(
         ApplicationProvider.getApplicationContext<android.content.Context>(),
         AppDatabase::class.java,
      ).setDriver(AndroidSQLiteDriver()).build()
      dao = database.createTDriveDao()
      database.createPersonDao().insert(PersonDto("seller", "Ada", "Lovelace", null, null, null))
      database.createCarDao().insert(CarDto("c1", "Ford", "Focus", 10000, emptyList(), "seller"))
   }

   @After
   fun tearDown() {
      database.close()
   }

   @Test
   fun insert_returnsSQLiteRowIdAndPreservesBusinessId() = runTest {
      val rowId: Long = dao.insert(person)

      assertTrue(rowId > 0L)
      database.useReaderConnection { connection ->
         connection.usePrepared("SELECT rowid FROM TDrive WHERE id = ?") { statement ->
            statement.bindText(1, person.id)
            assertTrue(statement.step())
            assertEquals(rowId, statement.getLong(0))
         }
      }
      assertEquals(person, dao.findById(person.id))
   }

   @Test
   fun insert_existingId_abortsAndKeepsOriginal() = runTest {
      dao.insert(person)
      try {
         dao.insert(person.copy(isCompleted = true))
         fail("ABORT must reject a duplicate primary key")
      } catch (_: SQLiteException) {
         // Check Room's actual constraint handling, rather than a fake's behavior.
      }

      assertEquals(1, dao.count())
      assertEquals(person, dao.findById(person.id))
   }

   @Test
   fun insertList_returnsUnitAndStoresAllRows() = runTest {
      val other = person.copy(id = "t2", start = Instant.parse("2026-01-02T12:00:00Z"))
      val result: Unit = dao.insert(listOf(person, other))

      assertEquals(Unit, result)
      assertEquals(2, dao.count())
      assertEquals(person, dao.findById(person.id))
      assertEquals(other, dao.findById(other.id))
   }

   @Test
   fun update_existingRow_returnsOneAndPersistsChanges() = runTest {
      dao.insert(person)
      val changed = person.copy(isCompleted = true)

      assertEquals(1, dao.update(changed))
      assertEquals(changed, dao.findById(person.id))
   }

   @Test
   fun update_missingRow_returnsZero() = runTest {
      assertEquals(0, dao.update(person))
      assertEquals(0, dao.count())
   }

   @Test
   fun delete_existingRow_returnsOneAndRemovesRow() = runTest {
      dao.insert(person)

      assertEquals(1, dao.delete(person))
      assertNull(dao.findById(person.id))
   }

   @Test
   fun delete_missingRow_returnsZero() = runTest {
      assertEquals(0, dao.delete(person))
      assertEquals(0, dao.count())
   }
}
