package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
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
@Config(sdk = [35], application = Application::class)
class PersonViewModelNormalizationTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val repository = FakePersonRepository()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val validator = PersonValidator(context)
   private val stringProvider = StringProvider(context)

   @Test
   fun invalidSave_showsNormalizedValuesWithoutWriting() =
      runTest(mainDispatcherRule.testDispatcher) {
         val original = Person(
            firstName = "   ",
            lastName = "  Lovelace  ",
            email = " müller@example.de! ",
            phone = " +49abc123456 ",
            id = "person-1",
         )
         repository.findResult = Result.success(original)
         val viewModel = PersonViewModel("person-1", repository, stringProvider, validator, EffectDelegate())
         advanceUntilIdle()

         viewModel.onIntent(PersonIntent.Save)

         assertEquals(original.copy(
            firstName = "",
            lastName = "Lovelace",
            email = "mueller@example.de",
            phone = "+49123456",
         ), viewModel.stateFlow.value.person)
         assertFalse(viewModel.stateFlow.value.isSaving)
         advanceUntilIdle()
         assertTrue(repository.created.isEmpty())
         assertTrue(repository.updated.isEmpty())
      }

   @Test
   fun validSave_persistsTheNormalizedSnapshotShownInUi() =
      runTest(mainDispatcherRule.testDispatcher) {
         val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
         viewModel.onIntent(PersonIntent.FirstNameChange("  Ada  "))
         viewModel.onIntent(PersonIntent.LastNameChange("  Lovelace  "))
         viewModel.onIntent(PersonIntent.EmailChange(" müller@example.de! "))
         viewModel.onIntent(PersonIntent.PhoneChange(" +49abc123456 "))

         viewModel.onIntent(PersonIntent.Save)
         val snapshot = viewModel.stateFlow.value.person
         assertEquals("Ada", snapshot.firstName)
         assertEquals("Lovelace", snapshot.lastName)
         assertEquals("mueller@example.de", snapshot.email)
         assertEquals("+49123456", snapshot.phone)
         assertTrue(viewModel.stateFlow.value.isSaving)

         advanceUntilIdle()
         assertEquals(snapshot, repository.created.single())
      }
}
