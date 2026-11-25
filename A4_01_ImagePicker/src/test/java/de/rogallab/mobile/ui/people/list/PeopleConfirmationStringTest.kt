package de.rogallab.mobile.ui.people.list

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PeopleConfirmationStringTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()

   @Test fun confirmation_formatsOneFullName_inEnglishAndGerman() =
      runTest(dispatcherRule.testDispatcher) {
         val application = ApplicationProvider.getApplicationContext<Context>()
         val person = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
         for ((language, expected) in listOf(
            "en" to "Really delete Ada Lovelace?",
            "de-DE" to "Ada Lovelace wirklich löschen?",
         )) {
            val configuration = Configuration(application.resources.configuration).apply {
               setLocale(Locale.forLanguageTag(language))
            }
            // Use Android resources and the real provider, not a formatting fake.
            val context = application.createConfigurationContext(configuration)
            val repository = FakePersonRepository(listOf(person))
            val viewModel = PeopleViewModel(
               _repository = repository,
               _personUcDelete = PersonUcDelete(repository, FakeImageFileStorage()),
               _stringProvider = StringProvider(context),
               _effectDelegate = EffectDelegate(),
               _timing = AppTiming(delayRead = 0, delayWrite = 0),
            )
            val store = ViewModelStore().also { it.put("people", viewModel) }
            try {
               advanceUntilIdle()
               viewModel.effects.test {
                  viewModel.onIntent(PeopleIntent.RequestRemove(person.id))
                  advanceUntilIdle()
                  val confirmation = awaitItem() as PeopleEffect.ConfirmRemove
                  assertEquals(expected, confirmation.message)
                  assertEquals(person.id, confirmation.personId)
                  expectNoEvents()
               }
            } finally {
               store.clear()
            }
         }
      }
}
