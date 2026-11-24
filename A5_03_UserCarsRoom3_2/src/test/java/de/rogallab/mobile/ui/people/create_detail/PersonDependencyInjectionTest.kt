package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.di.effectModule
import de.rogallab.mobile.domain.readmodels.ICarReadModel
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

   @Test fun eachEditor_hasOneSessionSharedByItsDelegateAndSaveUseCases() =
      runTest(dispatcherRule.testDispatcher) {
         val sessions = mutableListOf<FakeImageEdit>()
         val repository = FakePersonRepository()
         val application = koinApplication {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(appModule(dispatcherRule.testDispatcher), effectModule(), module {
               single<IPersonRepository> { repository }
               single<ICarReadModel> { FakeCarReadModel() }
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
         } finally {
            store.clear()
            application.close()
         }
      }
}
