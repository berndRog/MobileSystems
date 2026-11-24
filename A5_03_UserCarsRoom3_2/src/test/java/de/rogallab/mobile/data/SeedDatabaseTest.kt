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
import de.rogallab.mobile.data.local.database.SeedDatabase
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

   private suspend fun assertCounts(people: Int, cars: Int, tDrives: Int) {
      assertEquals(people, database.createPersonDao().count())
      assertEquals(cars, database.createCarDao().count())
      assertEquals(tDrives, database.createTDriveDao().count())
   }

   private fun invalidReferenceCallback(shouldFail: () -> Boolean = { true }) =
      object : RoomDatabase.Callback() {
         override suspend fun onCreate(connection: SQLiteConnection) {
            if (shouldFail()) {
               // Corrupt a seed reference while creation still has foreign keys disabled.
               connection.execSQL("""
                  CREATE TRIGGER invalid_seed AFTER INSERT ON TDrive
                  BEGIN
                     UPDATE TDrive SET carId = 'missing-car' WHERE id = NEW.id;
                  END
               """.trimIndent())
            }
         }
      }

   private suspend fun expectCreationFailure() {
      try {
         requestOpen()
         fail("Invalid seed must fail database creation")
      } catch (expected: IllegalStateException) {
         assertTrue(expected.message.orEmpty().contains("foreign key"))
      }
   }

   private fun assertCreationRolledBack() {
      AndroidSQLiteDriver().open(context.getDatabasePath(name).absolutePath).use { connection ->
         connection.prepare("PRAGMA user_version").use {
            assertTrue(it.step())
            assertEquals(0L, it.getLong(0))
         }
         connection.prepare(
            "SELECT COUNT(*) FROM sqlite_master WHERE name IN ('Person', 'Car', 'TDrive')"
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
   fun firstCreation_persistsOriginalSeedAndValidRelationships() = runTest {
      open()
      requestOpen()
      assertCounts(26, 7, 3)
      seed.createPersonDtos().forEach {
         assertEquals(it, database.createPersonDao().findById(it.id))
      }
      seed.createCarDtos().forEach {
         assertEquals(it, database.createCarDao().findById(it.id))
      }
      seed.createsTestDriveDtos().forEach {
         assertEquals(it, database.createTDriveDao().findById(it.id))
      }
      database.useReaderConnection { connection ->
         connection.usePrepared("PRAGMA foreign_key_check") { assertFalse(it.step()) }
         connection.usePrepared("PRAGMA foreign_keys") {
            assertTrue(it.step())
            assertEquals(1L, it.getLong(0))
         }
      }
   }

   @Test
   fun repeatedOpenRequests_doNotInsertAdditionalData() = runTest {
      open()
      repeat(3) { requestOpen() }
      assertCounts(26, 7, 3)
   }

   @Test
   fun closeAndReopen_doesNotRepeatSeed() = runTest {
      open()
      requestOpen()
      database.close()
      open()
      requestOpen()
      assertCounts(26, 7, 3)
   }

   @Test
   fun emptyTestDriveTable_staysEmptyOnRepeatedAccessAndReopen() = runTest {
      open()
      requestOpen()
      seed.createsTestDriveDtos().forEach { database.createTDriveDao().delete(it) }
      requestOpen()
      assertCounts(26, 7, 0)
      database.close()
      open()
      requestOpen()
      assertCounts(26, 7, 0)
   }

   @Test
   fun allTablesCleared_stayEmptyOnRepeatedAccessAndReopen() = runTest {
      open()
      requestOpen()
      database.clearAllTables()
      requestOpen()
      assertCounts(0, 0, 0)
      database.close()
      open()
      requestOpen()
      assertCounts(0, 0, 0)
   }

   @Test
   fun deletedPersonCarAndTestDrive_areNotRestored() = runTest {
      open()
      requestOpen()
      val person = seed.createPersonDtos().last()
      val car = seed.createCarDtos().last()
      val tDrive = seed.createsTestDriveDtos().first()
      database.createTDriveDao().delete(tDrive)
      database.createCarDao().delete(car)
      database.createPersonDao().delete(person)
      database.close()
      open()
      requestOpen()
      assertCounts(25, 6, 2)
      assertNull(database.createPersonDao().findById(person.id))
      assertNull(database.createCarDao().findById(car.id))
      assertNull(database.createTDriveDao().findById(tDrive.id))
   }

   @Test
   fun foreignKeyError_rollsBackSchemaAndEverySeedTable_thenReopenCanRetry() = runTest {
      open(beforeSeed = invalidReferenceCallback())
      expectCreationFailure()
      database.close()
      assertCreationRolledBack()
      open()
      requestOpen()
      assertCounts(26, 7, 3)
   }

   @Test
   fun failedCreation_canBeRetriedOnSameRoomInstance() = runTest {
      var shouldFail = true
      open(beforeSeed = invalidReferenceCallback { shouldFail })
      expectCreationFailure()
      shouldFail = false
      requestOpen()
      assertCounts(26, 7, 3)
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
      assertCounts(26, 7, 3)
   }

   @Test
   fun existingDatabaseWithoutSeedCallback_keepsItsDataUnchanged() = runTest {
      open(seedData = null)
      val original = PersonDto("existing", "Existing", "Person", null, null, null)
      database.createPersonDao().insert(original)
      database.close()
      open()
      requestOpen()
      assertCounts(1, 0, 0)
      assertEquals(original, database.createPersonDao().findById(original.id))
   }

   @Test
   fun existingEmptyDatabaseWithoutSeedCallback_isNotSeeded() = runTest {
      open(seedData = null)
      requestOpen()
      database.close()
      open()
      requestOpen()
      assertCounts(0, 0, 0)
   }

   @Test
   fun foreignKeys_rejectDeletingAReferencedPersonAfterCreation() = runTest {
      open()
      requestOpen()
      val seller = seed.createPersonDtos().first()
      try {
         database.createPersonDao().delete(seller)
         fail("Foreign key RESTRICT must reject deleting a seller")
      } catch (_: SQLiteException) {
         // Check the real SQLite constraint, not a fake DAO.
      }
      assertCounts(26, 7, 3)
   }
}
