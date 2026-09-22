package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.testing.FakeImageEdit
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
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
}

/*
 * Didaktik und Lernziele
 *
 * - Die Tests prüfen die Use Cases unabhängig von ViewModel, Compose und Koin.
 *   Dadurch wird sichtbar, dass die Anwendungslogik keine Android-UI benötigt.
 *
 * - Erfolgs- und Fehlerpfade belegen die entscheidende Reihenfolge: Eine
 *   Bild-Session wird nur nach erfolgreichem Create oder Update committed und
 *   eine Bilddatei nur nach erfolgreichem Delete entfernt.
 *
 * - Der zusätzliche Cleanup-Fehler zeigt, dass ein bereits gelöschter Datensatz
 *   nicht durch ein irreführendes Fehlerergebnis zurückgenommen werden kann.
 *
 * Lernziele:
 *
 * - Zusammengesetzte Operationen mit kleinen Fakes isoliert testen.
 * - Erfolgs- und Fehlerpfade eines Use Cases vollständig abdecken.
 * - Reihenfolgen und Seiteneffekte durch beobachtbare Aufrufe absichern.
 */
