package de.rogallab.mobile.data

import android.app.Application
import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.room3.useReaderConnection
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.SeedDatabase
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.testing.FakeImageFileStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// Real, file-backed Room database: reopening must not repeat onCreate.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SeedDatabaseTest {
   private val context = ApplicationProvider.getApplicationContext<Context>()
   private val name = "seed-${UUID.randomUUID()}.db"
   private val seed = Seed(FakeImageFileStorage())
   private lateinit var database: AppDatabase

   private fun open(
      seedData: Seed? = seed,
      beforeSeed: RoomDatabase.Callback? = null,
      afterSeed: RoomDatabase.Callback? = null,
   ) {
      val builder = Room.databaseBuilder<AppDatabase>(context, name)
         .setDriver(AndroidSQLiteDriver())
      if (beforeSeed != null) builder.addCallback(beforeSeed)
      if (seedData != null) builder.addCallback(SeedDatabase(seedData))
      if (afterSeed != null) builder.addCallback(afterSeed)
      database = builder.build()
   }

   private suspend fun requestOpen() {
      database.useReaderConnection { }
   }

   private suspend fun assertCounts(people: Int) {
      assertEquals(people, database.personDao().count())
   }

   private fun insertFailureCallback(shouldFail: () -> Boolean = { true }) =
      object : RoomDatabase.Callback() {
         override suspend fun onCreate(connection: SQLiteConnection) {
            if (shouldFail()) {
               // Fail after the first seed row, inside Room's creation transaction.
               connection.execSQL("""
                  CREATE TRIGGER invalid_seed BEFORE INSERT ON Person
                  WHEN NEW.id = '02000000-0000-0000-0000-000000000000'
                  BEGIN
                     SELECT RAISE(ABORT, 'seed insert failed');
                  END
               """.trimIndent())
            }
         }
      }

   private suspend fun expectCreationFailure() {
      try {
         requestOpen()
         fail("Seed insert must fail database creation")
      } catch (expected: SQLiteException) {
         assertTrue(expected.message.orEmpty().contains("seed insert failed"))
      }
   }

   private fun assertCreationRolledBack() {
      AndroidSQLiteDriver().open(context.getDatabasePath(name).absolutePath).use { connection ->
         connection.prepare("PRAGMA user_version").use {
            assertTrue(it.step())
            assertEquals(0L, it.getLong(0))
         }
         connection.prepare(
            "SELECT COUNT(*) FROM sqlite_master WHERE name = 'Person'"
         ).use {
            assertTrue(it.step())
            assertEquals(0L, it.getLong(0))
         }
      }
   }

   @After
   fun tearDown() {
      if (::database.isInitialized) database.close()
      context.deleteDatabase(name)
   }

   @Test
   fun firstCreation_persistsOriginalPersonSeed() = runTest {
      open()
      requestOpen()
      assertCounts(26)
      seed.createPersonDtos().forEach {
         assertEquals(it, database.personDao().findById(it.id))
      }
   }

   @Test
   fun repeatedOpenRequests_doNotInsertAdditionalData() = runTest {
      open()
      repeat(3) { requestOpen() }
      assertCounts(26)
   }

   @Test
   fun closeAndReopen_doesNotRepeatSeed() = runTest {
      open()
      requestOpen()
      database.close()
      open()
      requestOpen()
      assertCounts(26)
   }

   @Test
   fun allTablesCleared_stayEmptyOnRepeatedAccessAndReopen() = runTest {
      open()
      requestOpen()
      database.clearAllTables()
      requestOpen()
      assertCounts(0)
      database.close()
      open()
      requestOpen()
      assertCounts(0)
   }

   @Test
   fun deletedPerson_isNotRestored() = runTest {
      open()
      requestOpen()
      val person = seed.createPersonDtos().last()
      database.personDao().delete(person)
      database.close()
      open()
      requestOpen()
      assertCounts(25)
      assertNull(database.personDao().findById(person.id))
   }

   @Test
   fun insertFailure_rollsBackSchemaAndSeed_thenReopenCanRetry() = runTest {
      open(beforeSeed = insertFailureCallback())
      expectCreationFailure()
      database.close()
      assertCreationRolledBack()
      open()
      requestOpen()
      assertCounts(26)
   }

   @Test
   fun failedCreation_canBeRetriedOnSameRoomInstance() = runTest {
      var shouldFail = true
      open(beforeSeed = insertFailureCallback { shouldFail })
      expectCreationFailure()
      shouldFail = false
      requestOpen()
      assertCounts(26)
   }

   @Test
   fun cancellation_propagatesAndRollsBack_thenReopenCanRetry() = runTest {
      open(afterSeed = object : RoomDatabase.Callback() {
         override suspend fun onCreate(connection: SQLiteConnection) {
            throw CancellationException("cancel seed")
         }
      })
      try {
         requestOpen()
         fail("Cancellation must propagate")
      } catch (expected: CancellationException) {
         assertEquals("cancel seed", expected.message)
      }
      database.close()
      assertCreationRolledBack()
      open()
      requestOpen()
      assertCounts(26)
   }

   @Test
   fun existingDatabaseWithoutSeedCallback_keepsItsDataUnchanged() = runTest {
      open(seedData = null)
      val original = PersonDto("existing", "Existing", "Person", null, null, null)
      database.personDao().insert(original)
      database.close()
      open()
      requestOpen()
      assertCounts(1)
      assertEquals(original, database.personDao().findById(original.id))
   }

   @Test
   fun existingEmptyDatabaseWithoutSeedCallback_isNotSeeded() = runTest {
      open(seedData = null)
      requestOpen()
      database.close()
      open()
      requestOpen()
      assertCounts(0)
   }

}
