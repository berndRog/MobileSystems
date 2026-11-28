package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.data.local.io.ImageFileStorage
import de.rogallab.mobile.shared.data.network.ServerUnreachableException
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.ImageEdit
import de.rogallab.mobile.testing.FakeCarRepository
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PersonViewModelEffectTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()

   @Test fun saveEffects_keepOrderNetworkTextAndCancellationRetry() = runTest(dispatcherRule.testDispatcher) {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val strings = StringProvider(context)
      Alog.set(useAndroidLog = true)
      for (outcome in listOf("success", "failure", "network", "cancelled")) {
         ShadowLog.clear()
         val repository = FakePersonRepository().apply {
            createResult = when (outcome) {
               "success" -> Result.success(Unit)
               "network" -> Result.failure(ServerUnreachableException("Controlled network error"))
               "cancelled" -> Result.failure(CancellationException("cancelled"))
               else -> Result.failure(IllegalStateException("write failed"))
            }
         }
         // No image is selected; the real session performs no file operations.
         val storage = ImageFileStorage(context, "effect-test")
         val vm = PersonViewModel(null, repository, FakeCarRepository(), strings,
            PersonValidator(context), storage, ImageEdit(storage), EffectDelegate())
         val store = ViewModelStore().also { it.put("person", vm) }
         try {
            vm.onIntent(PersonIntent.FirstNameChange("Ada"))
            vm.onIntent(PersonIntent.LastNameChange("Lovelace"))
            vm.effects.test {
               vm.onIntent(PersonIntent.Save)
               advanceUntilIdle()
               when (outcome) {
                  "success" -> {
                     assertEquals(PersonEffect.ShowMessage(strings.getString(
                        R.string.message_person_saved, "Ada Lovelace")), awaitItem())
                     assertEquals(PersonEffect.NavigateBack(BackReason.Save), awaitItem())
                  }
                  "network" -> assertEquals(PersonEffect.ShowError("Controlled network error"), awaitItem())
                  "failure" -> assertEquals(PersonEffect.ShowError(strings.getString(R.string.error_person_save)), awaitItem())
               }
               assertEquals(if (outcome == "failure" || outcome == "network") 1 else 0,
                  ShadowLog.getLogsForTag("<-PersonViewModel").count { it.type == Log.ERROR })
               expectNoEvents()

               if (outcome == "cancelled") {
                  // Completion must release the internal guard for a later Save.
                  repository.createResult = Result.success(Unit)
                  vm.onIntent(PersonIntent.Save)
                  advanceUntilIdle()
                  assertEquals(PersonEffect.ShowMessage(strings.getString(
                     R.string.message_person_saved, "Ada Lovelace")), awaitItem())
                  assertEquals(PersonEffect.NavigateBack(BackReason.Save), awaitItem())
                  assertEquals(1, repository.created.size)
                  expectNoEvents()
               }
            }
         } finally {
            store.clear()
         }
      }
   }
}
