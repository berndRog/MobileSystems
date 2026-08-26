package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class PersonViewModelTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val repository = FakePersonRepository()
   private val validator =
      PersonValidator(ApplicationProvider.getApplicationContext())
   private val stringProvider = object : IStringProvider {
      override fun getString(resId: Int, vararg args: Any): String = "text-$resId"
   }

   @Test
   fun firstNameChange_trimsAndUpdatesState() {
      val viewModel = PersonViewModel(null, repository, stringProvider, validator)

      viewModel.onIntent(PersonIntent.FirstNameChange("  Ada  "))

      assertEquals("Ada", viewModel.stateFlow.value.person.firstName)
   }

   @Test
   fun save_validNewPersonCallsCreate() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = PersonViewModel(null, repository, stringProvider, validator)
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.onIntent(PersonIntent.Save)
      advanceUntilIdle()

      assertEquals(1, repository.created.size)
   }

   @Test
   fun existingPerson_isLoadedIntoState() = runTest(mainDispatcherRule.testDispatcher) {
      val person = Person(firstName = "Grace", lastName = "Hopper", id = "p1")
      repository.findResult = Result.success(person)

      val viewModel = PersonViewModel("p1", repository, stringProvider, validator)
      advanceUntilIdle()

      assertEquals(person, viewModel.stateFlow.value.person)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }
}
