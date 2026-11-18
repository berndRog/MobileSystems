package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import org.junit.Assert.assertSame
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.testing.FakeImageEdit
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.Assert.fail

class PersonUseCasesTest {

   private val person = Person(
      firstName = "Ada",
      lastName = "Lovelace",
      imagePath = "/images/ada.jpg",
      id = "p1",
   )

   @Before
   fun setUp() {
      // Use the JVM logger backend when a cleanup failure is tested.
      Alog.set(useAndroidLog = false)
   }

   @After
   fun tearDown() {
      Alog.reset()
   }

   @Test
   fun create_success_commitsImageEdit() = runTest {
      val repository = FakePersonRepository()
      val imageEdit = FakeImageEdit()

      val result = PersonUcCreate(repository, imageEdit)(person)

      assertTrue(result.isSuccess)
      assertEquals(listOf(person), repository.created)
      assertEquals(1, imageEdit.commitCount)
   }

   @Test
   fun create_failure_doesNotCommitImageEdit() = runTest {
      val repository = FakePersonRepository().apply {
         createResult = Result.failure(IllegalStateException("create failed"))
      }
      val imageEdit = FakeImageEdit()

      val result = PersonUcCreate(repository, imageEdit)(person)

      assertTrue(result.isFailure)
      assertEquals(emptyList<Person>(), repository.created)
      assertEquals(0, imageEdit.commitCount)
   }

   @Test
   fun update_success_commitsImageEdit() = runTest {
      val repository = FakePersonRepository()
      val imageEdit = FakeImageEdit()

      val result = PersonUcUpdate(repository, imageEdit)(person)

      assertTrue(result.isSuccess)
      assertEquals(listOf(person), repository.updated)
      assertEquals(1, imageEdit.commitCount)
   }

   @Test
   fun update_failure_doesNotCommitImageEdit() = runTest {
      val repository = FakePersonRepository().apply {
         updateResult = Result.failure(IllegalStateException("update failed"))
      }
      val imageEdit = FakeImageEdit()

      val result = PersonUcUpdate(repository, imageEdit)(person)

      assertTrue(result.isFailure)
      assertEquals(emptyList<Person>(), repository.updated)
      assertEquals(0, imageEdit.commitCount)
   }

   @Test
   fun save_imageCleanupThrows_keepsPersistedSuccess() = runTest {
      val repository = FakePersonRepository()
      val imageEdit = object : IImageEdit by FakeImageEdit() {
         override suspend fun commit() {
            throw IllegalStateException("cleanup failed")
         }
      }

      assertTrue(PersonUcCreate(repository, imageEdit)(person).isSuccess)
      assertTrue(PersonUcUpdate(repository, imageEdit)(person).isSuccess)
      assertEquals(listOf(person), repository.created)
      assertEquals(listOf(person), repository.updated)
   }

   @Test
   fun save_imageCommitCancellation_propagates() = runTest {
      val imageEdit = object : IImageEdit by FakeImageEdit() {
         override suspend fun commit() {
            throw CancellationException("commit cancelled")
         }
      }

      try {
         PersonUcCreate(FakePersonRepository(), imageEdit)(person)
         fail("Expected cancellation")
      } catch (_: CancellationException) {
         // Cancellation must never be reported as a normal save result.
      }
   }

   @Test
   fun delete_success_removesPersonAndImage() = runTest {
      val repository = FakePersonRepository(listOf(person))
      val imageFileStorage = FakeImageFileStorage()

      val result = PersonUcDelete(repository, imageFileStorage)(person)

      assertTrue(result.isSuccess)
      assertEquals(listOf(person), repository.removed)
      assertEquals(listOf(person.imagePath), imageFileStorage.deletedPaths)
   }

   @Test
   fun delete_repositoryFailure_keepsImage() = runTest {
      val repository = FakePersonRepository(listOf(person)).apply {
         removeResult = Result.failure(IllegalStateException("delete failed"))
      }
      val imageFileStorage = FakeImageFileStorage()

      val result = PersonUcDelete(repository, imageFileStorage)(person)

      assertTrue(result.isFailure)
      assertEquals(emptyList<Person>(), repository.removed)
      assertTrue(imageFileStorage.deletedPaths.isEmpty())
   }

   @Test
   fun delete_imageCleanupFailure_keepsSuccessfulDeleteResult() = runTest {
      val repository = FakePersonRepository(listOf(person))
      val imageFileStorage = FakeImageFileStorage().apply {
         deleteResult = Result.failure(IllegalStateException("cleanup failed"))
      }

      val result = PersonUcDelete(repository, imageFileStorage)(person)

      assertTrue(result.isSuccess)
      assertEquals(listOf(person), repository.removed)
      assertFalse(imageFileStorage.deletedPaths.isEmpty())
   }

   @Test
   fun delete_imageCleanupThrows_keepsSuccessfulDeleteResult() = runTest {
      val repository = FakePersonRepository(listOf(person))
      val storage = object : IImageFileStorage by FakeImageFileStorage() {
         override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> =
            throw IllegalStateException("cleanup threw")
      }

      assertTrue(PersonUcDelete(repository, storage)(person).isSuccess)
      assertEquals(listOf(person), repository.removed)
   }

   @Test
   fun delete_imageCleanupCancellation_propagates() = runTest {
      val storage = object : IImageFileStorage by FakeImageFileStorage() {
         override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> =
            Result.failure(CancellationException("cleanup cancelled"))
      }

      try {
         PersonUcDelete(FakePersonRepository(), storage)(person)
         fail("Expected cancellation")
      } catch (_: CancellationException) {
         // A cancelled cleanup cannot be treated as a completed deletion.
      }
   }

   @Test
   fun repositoryExceptions_becomeFailuresWithoutImageChanges() = runTest {
      for (operation in operations) {
         val failure = IllegalStateException("$operation threw")
         val repository = repositoryWithWrite { throw failure }
         val storage = FakeImageFileStorage()
         val imageEdit = FakeImageEdit()

         assertSame(failure, invokeFor(operation, repository, storage, imageEdit).exceptionOrNull())
         assertTrue(storage.deletedPaths.isEmpty())
         assertEquals(0, imageEdit.commitCount)
      }
   }

   @Test
   fun repositoryResultFailures_preserveCauseWithoutImageChanges() = runTest {
      for (operation in operations) {
         val failure = IllegalStateException("$operation failed")
         val repository = repositoryWithWrite { Result.failure(failure) }
         val storage = FakeImageFileStorage()
         val imageEdit = FakeImageEdit()

         assertSame(failure, invokeFor(operation, repository, storage, imageEdit).exceptionOrNull())
         assertTrue(storage.deletedPaths.isEmpty())
         assertEquals(0, imageEdit.commitCount)
      }
   }

   @Test
   fun repositoryCancellation_propagatesWhetherThrownOrReturned() = runTest {
      for (operation in operations) {
         for (throwDirectly in listOf(false, true)) {
            val cancellation = CancellationException("$operation cancelled")
            val repository = repositoryWithWrite {
               if (throwDirectly) throw cancellation
               Result.failure(cancellation)
            }
            val storage = FakeImageFileStorage()
            val imageEdit = FakeImageEdit()

            assertCancellation(cancellation) { invokeFor(operation, repository, storage, imageEdit) }
            assertTrue(storage.deletedPaths.isEmpty())
            assertEquals(0, imageEdit.commitCount)
         }
      }
   }

   @Test
   fun imageCleanupExceptions_keepSuccessfulPersistence() = runTest {
      for (operation in listOf("delete")) {
         val repository = FakePersonRepository()
         val failure = IllegalStateException("cleanup threw")
         val storage = object : IImageFileStorage by FakeImageFileStorage() {
            override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> =
               throw failure
         }
         assertTrue(invokeFor(operation, repository, storage, FakeImageEdit()).isSuccess)
         assertWriteRecorded(operation, repository)
      }
   }

   @Test
   fun imageCleanupCancellation_propagatesWhetherThrownOrReturned() = runTest {
      for (operation in listOf("delete")) {
         for (throwDirectly in listOf(false, true)) {
            val repository = FakePersonRepository()
            val cancellation = CancellationException("cleanup cancelled")
            val storage = object : IImageFileStorage by FakeImageFileStorage() {
               override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> {
                  if (throwDirectly) throw cancellation
                  return Result.failure(cancellation)
               }
            }
            assertCancellation(cancellation) {
               invokeFor(operation, repository, storage, FakeImageEdit())
            }
            assertWriteRecorded(operation, repository)
         }
      }
   }

   @Test
   fun imageCommitExceptions_keepPersistedSuccess() = runTest {
      for (operation in listOf("create", "update")) {
         val repository = FakePersonRepository()
         val failure = IllegalStateException("commit threw")
         val imageEdit = object : IImageEdit by FakeImageEdit() {
            override suspend fun commit() = throw failure
         }
         val result = invokeFor(operation, repository, FakeImageFileStorage(), imageEdit)
         assertTrue(result.isSuccess)
         assertWriteRecorded(operation, repository)
      }
   }

   @Test
   fun imageCommitCancellation_propagatesForCreateAndUpdate() = runTest {
      for (operation in listOf("create", "update")) {
         val repository = FakePersonRepository()
         val cancellation = CancellationException("commit cancelled")
         val imageEdit = object : IImageEdit by FakeImageEdit() {
            override suspend fun commit() = throw cancellation
         }
         assertCancellation(cancellation) {
            invokeFor(operation, repository, FakeImageFileStorage(), imageEdit)
         }
         assertWriteRecorded(operation, repository)
      }
   }

   private val operations = listOf("create", "update", "delete")

   private fun repositoryWithWrite(
      write: suspend () -> Result<Unit>,
   ): IPersonRepository = object : IPersonRepository by FakePersonRepository() {
      override suspend fun create(person: Person): Result<Unit> = write()
      override suspend fun update(person: Person): Result<Unit> = write()
      override suspend fun remove(person: Person): Result<Unit> = write()
   }

   private suspend fun invokeFor(
      operation: String,
      repository: IPersonRepository,
      storage: IImageFileStorage,
      imageEdit: IImageEdit,
   ): Result<Unit> = when (operation) {
      "create" -> PersonUcCreate(repository, imageEdit)(person)
      "update" -> PersonUcUpdate(repository, imageEdit)(person)
      "delete" -> PersonUcDelete(repository, storage)(person)
      else -> error("Unknown operation: $operation")
   }

   private fun assertWriteRecorded(operation: String, repository: FakePersonRepository) {
      val writes = when (operation) {
         "create" -> repository.created
         "update" -> repository.updated
         "delete" -> repository.removed
         else -> error("Unknown operation: $operation")
      }
      assertEquals(listOf(person), writes)
   }

   private suspend fun assertCancellation(
      expected: CancellationException,
      block: suspend () -> Unit,
   ) {
      try {
         block()
         fail("Expected cancellation")
      } catch (actual: CancellationException) {
         assertSame(expected, actual)
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Tests prüfen die Use Cases unabhängig von ViewModel, Compose, Koin und
 *   der konkreten Room-Datenbank.
 *
 * - Erfolgs- und Fehlerpfade belegen die entscheidende Reihenfolge: Eine
 *   Bild-Session wird nur nach erfolgreichem Create oder Update committed und
 *   eine Bilddatei nur nach erfolgreichem Delete entfernt.
 *
 * Lernziele:
 *
 * - Zusammengesetzte Operationen mit kleinen Fakes isoliert testen.
 * - Room-unabhängige Anwendungslogik ohne Android-UI überprüfen.
 * - Reihenfolgen und Seiteneffekte durch beobachtbare Aufrufe absichern.
 */
