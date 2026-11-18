package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PersonUseCasesTest {

   private val localPerson = Person(
      firstName = "Ada",
      lastName = "Lovelace",
      imagePath = "/images/ada.jpg",
      id = "p1",
   )
   private val remotePerson = localPerson.copy(
      imagePath = "http://127.0.0.1:5081/peopleapi/v1/images/ada.jpg",
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
   fun create_success_deletesLocalTransportFile() = runTest {
      val repository = FakePersonRepository()
      val storage = FakeImageFileStorage()

      val result = PersonUcCreate(repository, storage)(localPerson)

      assertTrue(result.isSuccess)
      assertEquals(listOf(localPerson), repository.created)
      assertEquals(listOf(localPerson.imagePath), storage.deletedPaths)
   }

   @Test
   fun create_failure_keepsLocalTransportFile() = runTest {
      val repository = FakePersonRepository().apply {
         createResult = Result.failure(IllegalStateException("upload failed"))
      }
      val storage = FakeImageFileStorage()

      val result = PersonUcCreate(repository, storage)(localPerson)

      assertTrue(result.isFailure)
      assertTrue(storage.deletedPaths.isEmpty())
   }

   @Test
   fun create_remoteUrl_neverDeletesLocalFile() = runTest {
      val repository = FakePersonRepository()
      val storage = FakeImageFileStorage()

      val result = PersonUcCreate(repository, storage)(remotePerson)

      assertTrue(result.isSuccess)
      assertTrue(storage.deletedPaths.isEmpty())
   }

   @Test
   fun update_success_deletesLocalReplacement() = runTest {
      val repository = FakePersonRepository()
      val storage = FakeImageFileStorage()

      val result = PersonUcUpdate(repository, storage)(localPerson)

      assertTrue(result.isSuccess)
      assertEquals(listOf(localPerson), repository.updated)
      assertEquals(listOf(localPerson.imagePath), storage.deletedPaths)
   }

   @Test
   fun update_failure_keepsLocalReplacement() = runTest {
      val repository = FakePersonRepository().apply {
         updateResult = Result.failure(IllegalStateException("update failed"))
      }
      val storage = FakeImageFileStorage()

      val result = PersonUcUpdate(repository, storage)(localPerson)

      assertTrue(result.isFailure)
      assertTrue(storage.deletedPaths.isEmpty())
   }

   @Test
   fun update_remoteUrl_neverDeletesLocalFile() = runTest {
      val repository = FakePersonRepository()
      val storage = FakeImageFileStorage()

      val result = PersonUcUpdate(repository, storage)(remotePerson)

      assertTrue(result.isSuccess)
      assertTrue(storage.deletedPaths.isEmpty())
   }

   @Test
   fun delete_remoteUrl_isOwnedByServer() = runTest {
      val repository = FakePersonRepository(listOf(remotePerson))
      val storage = FakeImageFileStorage()

      val result = PersonUcDelete(repository, storage)(remotePerson)

      assertTrue(result.isSuccess)
      assertEquals(listOf(remotePerson), repository.removed)
      assertTrue(storage.deletedPaths.isEmpty())
   }

   @Test
   fun delete_success_removesPossibleLocalFile() = runTest {
      val repository = FakePersonRepository(listOf(localPerson))
      val storage = FakeImageFileStorage()

      val result = PersonUcDelete(repository, storage)(localPerson)

      assertTrue(result.isSuccess)
      assertEquals(listOf(localPerson), repository.removed)
      assertEquals(listOf(localPerson.imagePath), storage.deletedPaths)
   }

   @Test
   fun delete_repositoryFailure_keepsLocalFile() = runTest {
      val repository = FakePersonRepository(listOf(localPerson)).apply {
         removeResult = Result.failure(IllegalStateException("delete failed"))
      }
      val storage = FakeImageFileStorage()

      val result = PersonUcDelete(repository, storage)(localPerson)

      assertTrue(result.isFailure)
      assertTrue(storage.deletedPaths.isEmpty())
   }

   @Test
   fun cleanupFailure_keepsSuccessfulRepositoryResult() = runTest {
      val repository = FakePersonRepository()
      val storage = FakeImageFileStorage().apply {
         deleteResult = Result.failure(IllegalStateException("cleanup failed"))
      }

      val result = PersonUcCreate(repository, storage)(localPerson)

      assertTrue(result.isSuccess)
      assertFalse(storage.deletedPaths.isEmpty())
   }

   @Test
   fun repositoryExceptions_becomeFailuresWithoutImageChanges() = runTest {
      for (operation in operations) {
         val failure = IllegalStateException("$operation threw")
         val repository = repositoryWithWrite { throw failure }
         val storage = FakeImageFileStorage()

         assertSame(failure, invokeFor(operation, repository, storage).exceptionOrNull())
         assertTrue(storage.deletedPaths.isEmpty())
      }
   }

   @Test
   fun repositoryResultFailures_preserveCauseWithoutImageChanges() = runTest {
      for (operation in operations) {
         val failure = IllegalStateException("$operation failed")
         val repository = repositoryWithWrite { Result.failure(failure) }
         val storage = FakeImageFileStorage()

         assertSame(failure, invokeFor(operation, repository, storage).exceptionOrNull())
         assertTrue(storage.deletedPaths.isEmpty())
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

            assertCancellation(cancellation) { invokeFor(operation, repository, storage) }
            assertTrue(storage.deletedPaths.isEmpty())
         }
      }
   }

   @Test
   fun imageCleanupExceptions_keepSuccessfulPersistence() = runTest {
      for (operation in operations) {
         val repository = FakePersonRepository()
         val failure = IllegalStateException("cleanup threw")
         val storage = object : IImageFileStorage by FakeImageFileStorage() {
            override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> =
               throw failure
         }
         assertTrue(invokeFor(operation, repository, storage).isSuccess)
         assertWriteRecorded(operation, repository)
      }
   }

   @Test
   fun imageCleanupCancellation_propagatesWhetherThrownOrReturned() = runTest {
      for (operation in operations) {
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
               invokeFor(operation, repository, storage)
            }
            assertWriteRecorded(operation, repository)
         }
      }
   }

   @Test
   fun imageCleanupResultFailures_keepSuccessfulPersistenceForAllWrites() = runTest {
      for (operation in operations) {
         val repository = FakePersonRepository()
         val storage = FakeImageFileStorage().apply {
            deleteResult = Result.failure(IllegalStateException("cleanup failed"))
         }
         assertTrue(invokeFor(operation, repository, storage).isSuccess)
         assertWriteRecorded(operation, repository)
         assertEquals(listOf(localPerson.imagePath), storage.deletedPaths)
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
   ): Result<Unit> = when (operation) {
      "create" -> PersonUcCreate(repository, storage)(localPerson)
      "update" -> PersonUcUpdate(repository, storage)(localPerson)
      "delete" -> PersonUcDelete(repository, storage)(localPerson)
      else -> error("Unknown operation: $operation")
   }

   private fun assertWriteRecorded(operation: String, repository: FakePersonRepository) {
      val writes = when (operation) {
         "create" -> repository.created
         "update" -> repository.updated
         "delete" -> repository.removed
         else -> error("Unknown operation: $operation")
      }
      assertEquals(listOf(localPerson), writes)
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
 * - Die Tests unterscheiden bewusst lokale Transportdateien von persistenten
 *   HTTP(S)-Ressourcen der PeopleImagesApi.
 *
 * - Erfolgs- und Fehlerpfade belegen, dass lokale Dateien nur nach vollständigem
 *   Servererfolg entfernt werden und für einen Retry erhalten bleiben.
 *
 * Lernziele:
 *
 * - Ownership-Regeln für Client- und Serverbilder automatisiert absichern.
 * - Mehrschrittige REST-Operationen ohne echtes Netzwerk isoliert testen.
 * - Best-Effort-Aufräumen vom Ergebnis der Hauptoperation trennen.
 */
