package de.rogallab.mobile.domain.usecases

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
