package de.rogallab.mobile.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.ImageEdit
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
class PersonBackEffectTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val validator = PersonValidator(context)
   private val stringProvider = StringProvider(context)

   @Test
   fun saveEffects_stayOrdered_andFailureOrCancellationDoesNotNavigate() =
      runTest(mainDispatcherRule.testDispatcher) {
         de.rogallab.mobile.shared.domain.utilities.Alog.set(useAndroidLog = true)
         for (outcome in listOf("success", "failure", "cancelled")) {
            org.robolectric.shadows.ShadowLog.clear()
            val storage = FakeImageFileStorage()
            val repository = FakePersonRepository().apply {
               createResult = when (outcome) {
                  "success" -> Result.success(Unit)
                  "failure" -> Result.failure(IllegalStateException("offline"))
                  else -> Result.failure(kotlinx.coroutines.CancellationException("cancelled"))
               }
            }
            val edit = ImageEdit(storage)
            val vm = PersonViewModel(null, repository, stringProvider, validator, storage, edit,
               PersonUcCreate(repository, edit), PersonUcUpdate(repository, edit), EffectDelegate())
            val store = androidx.lifecycle.ViewModelStore().also { it.put("person", vm) }
            vm.onIntent(PersonIntent.FirstNameChange("Ada"))
            vm.onIntent(PersonIntent.LastNameChange("Lovelace"))
            vm.effects.test {
               vm.onIntent(PersonIntent.Save)
               advanceUntilIdle()
               when (outcome) {
                  "success" -> {
                     assertEquals(PersonEffect.ShowMessage(stringProvider.getString(
                        de.rogallab.mobile.R.string.message_person_saved, "Ada Lovelace")), awaitItem())
                     assertEquals(PersonEffect.NavigateBack(BackReason.Save), awaitItem())
                  }
                  "failure" -> assertEquals(PersonEffect.ShowError(stringProvider.getString(
                     de.rogallab.mobile.R.string.error_person_save)), awaitItem())
               }
               assertEquals(if (outcome == "failure") 1 else 0,
                  org.robolectric.shadows.ShadowLog.getLogsForTag("<-PersonViewModel").count { it.type == android.util.Log.ERROR })
               expectNoEvents()
            }
            store.clear()
         }
      }

   @Test
   fun cancel_emitsNavigateBackWithCancelReason() = runTest(mainDispatcherRule.testDispatcher) {
      val imageFileStorage = FakeImageFileStorage()
      val repository = FakePersonRepository()
      val imageEdit = ImageEdit(imageFileStorage)
      val viewModel = PersonViewModel(
         personId = null,
         _repository = repository,
         _stringProvider = stringProvider,
         _validator = validator,
         _imageFileStorage = imageFileStorage,
         _imageEdit = imageEdit,
         _personUcCreate = PersonUcCreate(repository, imageEdit),
         _personUcUpdate = PersonUcUpdate(repository, imageEdit),
         _effectDelegate = EffectDelegate(),
      )

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Cancel)
         advanceUntilIdle()

         val effect = awaitItem() as PersonEffect.NavigateBack
         assertEquals(BackReason.Cancel, effect.reason)
         cancelAndIgnoreRemainingEvents()
      }
   }
}
