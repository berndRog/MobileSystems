package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.testing.FakeImageEdit
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
   fun delete_success_removesPersonAndImage() = runTest {
      val repository = FakePersonRepository(listOf(person)).apply {
         findResult = Result.success(person)
      }
      val imageFileStorage = FakeImageFileStorage()

      val result = PersonUcDelete(repository, imageFileStorage)(person.id)

      assertTrue(result.isSuccess)
      assertEquals(listOf(person), repository.removed)
      assertEquals(listOf(person.imagePath), imageFileStorage.deletedPaths)
   }

   @Test
   fun delete_restrictedByRelation_keepsImage() = runTest {
      val repository = FakePersonRepository(listOf(person)).apply {
         findResult = Result.success(person)
         removeResult = Result.failure(IllegalStateException("foreign key restricted"))
      }
      val imageFileStorage = FakeImageFileStorage()

      val result = PersonUcDelete(repository, imageFileStorage)(person.id)

      assertTrue(result.isFailure)
      assertEquals(emptyList<Person>(), repository.removed)
      assertTrue(imageFileStorage.deletedPaths.isEmpty())
   }

   @Test
   fun delete_imageCleanupFailure_keepsSuccessfulDeleteResult() = runTest {
      val repository = FakePersonRepository(listOf(person)).apply {
         findResult = Result.success(person)
      }
      val imageFileStorage = FakeImageFileStorage().apply {
         deleteResult = Result.failure(IllegalStateException("cleanup failed"))
      }

      val result = PersonUcDelete(repository, imageFileStorage)(person.id)

      assertTrue(result.isSuccess)
      assertEquals(listOf(person), repository.removed)
      assertFalse(imageFileStorage.deletedPaths.isEmpty())
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
         val repository = FakePersonRepository().apply { findResult = Result.success(person) }
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
            val repository = FakePersonRepository().apply { findResult = Result.success(person) }
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
   fun imageCommitExceptions_becomeFailuresAfterPersistence() = runTest {
      for (operation in listOf("create", "update")) {
         val repository = FakePersonRepository()
         val failure = IllegalStateException("commit threw")
         val imageEdit = object : IImageEdit by FakeImageEdit() {
            override suspend fun commit() = throw failure
         }
         val result = invokeFor(operation, repository, FakeImageFileStorage(), imageEdit)
         assertSame(failure, result.exceptionOrNull())
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

   @Test
   fun delete_missingPerson_doesNotRemoveOrCleanImage() = runTest {
      val repository = FakePersonRepository()
      val storage = FakeImageFileStorage()
      var removals = 0
      val recording = object : IPersonRepository by repository {
         override suspend fun remove(person: Person): Result<Unit> {
            removals++
            return repository.remove(person)
         }
      }

      val result = PersonUcDelete(recording, storage)("missing")

      assertTrue(result.exceptionOrNull() is NoSuchElementException)
      assertEquals(0, removals)
      assertTrue(storage.deletedPaths.isEmpty())
   }

   @Test
   fun delete_lookupFailures_preserveCauseWithoutDeleting() = runTest {
      for (throwDirectly in listOf(false, true)) {
         val failure = IllegalStateException("lookup failed")
         val storage = FakeImageFileStorage()
         var removals = 0
         val repository = object : IPersonRepository by FakePersonRepository() {
            override suspend fun findById(id: String): Result<Person?> {
               if (throwDirectly) throw failure
               return Result.failure(failure)
            }
            override suspend fun remove(person: Person): Result<Unit> {
               removals++
               return Result.success(Unit)
            }
         }

         assertSame(failure, PersonUcDelete(repository, storage)(person.id).exceptionOrNull())
         assertEquals(0, removals)
         assertTrue(storage.deletedPaths.isEmpty())
      }
   }

   @Test
   fun delete_lookupCancellation_propagatesWithoutDeleting() = runTest {
      for (throwDirectly in listOf(false, true)) {
         val cancellation = CancellationException("lookup cancelled")
         val storage = FakeImageFileStorage()
         var removals = 0
         val repository = object : IPersonRepository by FakePersonRepository() {
            override suspend fun findById(id: String): Result<Person?> {
               if (throwDirectly) throw cancellation
               return Result.failure(cancellation)
            }
            override suspend fun remove(person: Person): Result<Unit> {
               removals++
               return Result.success(Unit)
            }
         }

         assertCancellation(cancellation) { PersonUcDelete(repository, storage)(person.id) }
         assertEquals(0, removals)
         assertTrue(storage.deletedPaths.isEmpty())
      }
   }

   @Test
   fun delete_usesReloadedEntityAndItsCurrentImage() = runTest {
      val current = person.copy(firstName = "Updated", imagePath = "/images/current.jpg")
      val source = FakePersonRepository().apply { findResult = Result.success(current) }
      val events = mutableListOf<String>()
      val repository = object : IPersonRepository by source {
         override suspend fun findById(id: String): Result<Person?> {
            events += "find:$id"
            return source.findById(id)
         }
         override suspend fun remove(person: Person): Result<Unit> {
            events += "remove:${person.id}"
            return source.remove(person)
         }
      }
      val storage = object : IImageFileStorage by FakeImageFileStorage() {
         override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> {
            events += "image:$imagePath"
            return Result.success(Unit)
         }
      }

      assertTrue(PersonUcDelete(repository, storage)(person.id).isSuccess)
      assertEquals(listOf(current), source.removed)
      assertEquals(listOf("find:p1", "remove:p1", "image:/images/current.jpg"), events)
   }

   private val operations = listOf("create", "update", "delete")

   private fun repositoryWithWrite(
      write: suspend () -> Result<Unit>,
   ): IPersonRepository = object : IPersonRepository by FakePersonRepository() {
      override suspend fun findById(id: String): Result<Person?> = Result.success(person)
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
      "delete" -> PersonUcDelete(repository, storage)(person.id)
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
 * - Die Tests prüfen die Personen-Use-Cases unabhängig von den zusätzlichen
 *   Car- und TestDrive-Features des UsedCars-Moduls.
 *
 * - Der simulierte Foreign-Key-Fehler zeigt, dass die Bilddatei bestehen bleibt,
 *   wenn relationale Verknüpfungen das Löschen der Person verhindern.
 *
 * Lernziele:
 *
 * - Zusammengesetzte Operationen mit kleinen Fakes isoliert testen.
 * - Relationale Löschfehler als Repository-Fehler behandeln.
 * - Reihenfolgen und Seiteneffekte durch beobachtbare Aufrufe absichern.
 */
