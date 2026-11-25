package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import org.koin.core.parameter.parametersOf
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.di.effectModule
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.testing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PersonDependencyInjectionTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()

   @Test fun useCases_areFactories_andSaveUsesTheSuppliedSession() =
      runTest(dispatcherRule.testDispatcher) {
         val repository = FakePersonRepository()
         var sessionsCreated = 0
         val application = koinApplication {
            modules(appModule(), module {
               single<IPersonRepository> { repository }
               single<IImageFileStorage> { FakeImageFileStorage() }
               factory<IImageEdit> {
                  sessionsCreated++
                  FakeImageEdit()
               }
            })
         }
         try {
            val session = FakeImageEdit()
            val create = application.koin.get<PersonUcCreate> { parametersOf(session) }
            val otherCreate = application.koin.get<PersonUcCreate> { parametersOf(session) }
            val update = application.koin.get<PersonUcUpdate> { parametersOf(session) }
            val otherUpdate = application.koin.get<PersonUcUpdate> { parametersOf(session) }
            assertNotSame(create, otherCreate)
            assertNotSame(update, otherUpdate)

            val person = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
            assertTrue(create(person).isSuccess)
            assertTrue(update(person).isSuccess)
            assertEquals(2, session.commitCount)
            // Save factories must use the supplied session, not resolve another one.
            assertEquals(0, sessionsCreated)
            assertNotSame(
               application.koin.get<PersonUcDelete>(),
               application.koin.get<PersonUcDelete>(),
            )
         } finally {
            application.close()
         }
      }

   @Test fun eachEditor_hasOneSessionSharedByItsDelegateAndSaveUseCases() =
      runTest(dispatcherRule.testDispatcher) {
         val sessions = mutableListOf<FakeImageEdit>()
         val repository = FakePersonRepository()
         val application = koinApplication {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(appModule(), effectModule(), module {
               single<IPersonRepository> { repository }
               single<IStringProvider> { FakeStringProvider() }
               single<IImageFileStorage> { FakeImageFileStorage() }
               factory<IImageEdit> { FakeImageEdit().also { sessions += it } }
            })
         }
         val store = ViewModelStore()
         try {
            val first = application.koin.get<PersonViewModel>().also { store.put("first", it) }
            val second = application.koin.get<PersonViewModel>().also { store.put("second", it) }
            assertNotSame(first, second)
            assertEquals(2, sessions.size)
            first.onIntent(PersonIntent.CameraImageTaken("/images/old-draft.jpg"))
            first.onIntent(PersonIntent.CameraImageTaken("/images/first.jpg"))
            second.onIntent(PersonIntent.CameraImageTaken("/images/second.jpg"))
            advanceUntilIdle()
            assertEquals(listOf("/images/first.jpg"), sessions[0].add(emptyList()))
            assertEquals(listOf("/images/second.jpg"), sessions[1].add(emptyList()))

            first.onIntent(PersonIntent.FirstNameChange("Ada"))
            first.onIntent(PersonIntent.LastNameChange("Lovelace"))
            first.onIntent(PersonIntent.Save)
            advanceUntilIdle()
            assertEquals("/images/first.jpg", repository.created.single().imagePath)
            assertEquals(1, sessions[0].commitCount)
            assertEquals(0, sessions[1].commitCount)
            second.onIntent(PersonIntent.Cancel)
            advanceUntilIdle()
            assertEquals(0, sessions[0].discardCount)
            assertEquals(1, sessions[1].discardCount)

            repository.findResult = Result.success(
               Person(id = "p1", firstName = "Grace", lastName = "Hopper",
                  imagePath = "/images/original.jpg")
            )
            val editor = application.koin.get<PersonViewModel> { parametersOf("p1") }
               .also { store.put("editor", it) }
            advanceUntilIdle()
            assertEquals(3, sessions.size)
            editor.onIntent(PersonIntent.CameraImageTaken("/images/updated.jpg"))
            editor.onIntent(PersonIntent.Save)
            advanceUntilIdle()
            assertEquals("/images/updated.jpg", repository.updated.single().imagePath)
            assertEquals(1, sessions[2].commitCount)
            assertEquals(1, sessions[0].commitCount)
            assertEquals(0, sessions[1].commitCount)
         } finally {
            store.clear()
            application.close()
         }
      }
}
