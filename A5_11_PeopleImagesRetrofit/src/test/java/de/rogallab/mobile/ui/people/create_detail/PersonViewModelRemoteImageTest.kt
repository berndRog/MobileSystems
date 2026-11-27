package de.rogallab.mobile.ui.people.create_detail

import org.koin.android.ext.koin.androidContext
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
)
class PersonViewModelRemoteImageTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val repository = FakePersonRepository()
   private val storage = FakeImageFileStorage()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val stringProvider = StringProvider(context)

   private val store = androidx.lifecycle.ViewModelStore()
   private val applications = mutableListOf<org.koin.core.KoinApplication>()

   private fun createViewModel(personId: String): PersonViewModel {
      val application = org.koin.dsl.koinApplication {
         androidContext(context)
         modules(de.rogallab.mobile.di.appModule(mainDispatcherRule.testDispatcher),
            de.rogallab.mobile.di.effectModule(), org.koin.dsl.module {
               single<de.rogallab.mobile.domain.repositories.IPersonRepository> { repository }
               single<de.rogallab.mobile.shared.domain.IStringProvider> { stringProvider }
               single<de.rogallab.mobile.shared.domain.images.IImageFileStorage> { storage }
            })
      }
      applications += application
      return application.koin.get<PersonViewModel> {
         org.koin.core.parameter.parametersOf(personId)
      }.also { store.put("editor", it) }
   }

   @org.junit.After fun tearDown() {
      store.clear()
      applications.forEach { it.close() }
   }


   @Test
   fun saveReplacement_deletesLocalTemporaryButNeverServerUrl() = runTest(mainDispatcherRule.testDispatcher) {
      val originalUrl =
         "http://127.0.0.1:5081/peopleapi/v1/images/original.jpg"
      val original = Person(
         firstName = "Ada",
         lastName = "Lovelace",
         imagePath = originalUrl,
         id = "p1",
      )
      repository.findResult = Result.success(original)
      val viewModel = createViewModel("p1")
      advanceUntilIdle()
      viewModel.onIntent(PersonIntent.CameraImageTaken("/images/new.jpg"))
      advanceUntilIdle()
      assertFalse(originalUrl in storage.deletedPaths)
      viewModel.onIntent(PersonIntent.Save)
      advanceUntilIdle()
      assertFalse(originalUrl in storage.deletedPaths)
      assertTrue("/images/new.jpg" in storage.deletedPaths)
      assertEquals(1, repository.updated.size)
   }

   @Test
   fun failedSave_keepsServerUrlAndLocalReplacementForRetry() = runTest(mainDispatcherRule.testDispatcher) {
      val originalUrl =
         "http://127.0.0.1:5081/peopleapi/v1/images/original.jpg"
      val original = Person(
         firstName = "Ada",
         lastName = "Lovelace",
         imagePath = originalUrl,
         id = "p1",
      )
      repository.findResult = Result.success(original)
      repository.updateResult = Result.failure(IllegalStateException("write failed"))
      val viewModel = createViewModel("p1")
      advanceUntilIdle()
      viewModel.onIntent(PersonIntent.CameraImageTaken("/images/new.jpg"))
      advanceUntilIdle()
      viewModel.onIntent(PersonIntent.Save)
      advanceUntilIdle()
      assertFalse(originalUrl in storage.deletedPaths)
      assertFalse("/images/new.jpg" in storage.deletedPaths)
      assertEquals("/images/new.jpg", viewModel.stateFlow.value.form.imagePath)
      viewModel.onIntent(PersonIntent.Cancel)
      advanceUntilIdle()
      assertTrue("/images/new.jpg" in storage.deletedPaths)
   }

   @Test
   fun cancelWithServerImage_doesNotDeleteRemoteUrl() = runTest(mainDispatcherRule.testDispatcher) {
      val originalUrl =
         "http://127.0.0.1:5081/peopleapi/v1/images/original.jpg"
      repository.findResult = Result.success(
         Person(
            firstName = "Ada",
            lastName = "Lovelace",
            imagePath = originalUrl,
            id = "p1",
         )
      )
      val viewModel = createViewModel("p1")
      advanceUntilIdle()

      viewModel.onIntent(PersonIntent.Cancel)
      advanceUntilIdle()

      assertFalse(originalUrl in storage.deletedPaths)
   }
}
