package de.rogallab.mobile.ui.navigation

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.domain.utilities.Alog
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

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
   fun cancel_emitsNavigateBackWithCancelReason() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = PersonViewModel(
         personId = null,
         _repository = FakePersonRepository(),
         _stringProvider = stringProvider,
         _validator = validator,
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
   @Test
   fun cancelledSave_releasesGuardWithoutMessageOrNavigation() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (throwDirectly in listOf(false, true)) {
            val repository = object : IPersonRepository by FakePersonRepository() {
               override suspend fun create(person: Person): Result<Unit> {
                  val cancellation = CancellationException("write cancelled")
                  if (throwDirectly) throw cancellation
                  return Result.failure(cancellation)
               }
            }
            val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
            val store = ViewModelStore().also { it.put("person", viewModel) }
            try {
               viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
               viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
               viewModel.effects.test {
                  viewModel.onIntent(PersonIntent.Save)
                  assertTrue(viewModel.stateFlow.value.isSaving)
                  advanceUntilIdle()
                  assertFalse(viewModel.stateFlow.value.isSaving)
                  expectNoEvents()
               }
            } finally {
               store.clear()
            }
         }
      }

   @Test
   fun clearedScope_releasesSaveGuardBeforeOrDuringTheSave() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (clearBeforeSave in listOf(false, true)) {
            val repository = FakePersonRepository()
            val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
            val store = ViewModelStore().also { it.put("person", viewModel) }
            viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
            viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
            viewModel.effects.test {
               if (clearBeforeSave) store.clear()
               viewModel.onIntent(PersonIntent.Save)
               store.clear()
               advanceUntilIdle()
               assertFalse(viewModel.stateFlow.value.isSaving)
               assertTrue(repository.created.isEmpty())
               expectNoEvents()
            }
         }
      }

   @Test
   fun cancelledLoad_isNotAStoredFailureOrMessage() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (throwDirectly in listOf(false, true)) {
            val repository = object : IPersonRepository by FakePersonRepository() {
               override suspend fun findById(id: String): Result<Person?> {
                  val cancellation = CancellationException("load cancelled")
                  if (throwDirectly) throw cancellation
                  return Result.failure(cancellation)
               }
            }
            val viewModel = PersonViewModel("p1", repository, stringProvider, validator, EffectDelegate())
            val store = ViewModelStore().also { it.put("person", viewModel) }
            val loading = viewModel.viewModelScope.coroutineContext[Job]!!.children.single()
            try {
               viewModel.effects.test {
                  advanceUntilIdle()
                  assertTrue(loading.isCancelled)
                  assertNull(viewModel.stateFlow.value.loadFailure)
                  expectNoEvents()
               }
            } finally {
               store.clear()
            }
         }
      }

   @Test
   fun save_emitsFormattedMessageBeforeNavigation() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository()
      val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         viewModel.onIntent(PersonIntent.Save)
         viewModel.onIntent(PersonIntent.Cancel)
         assertTrue(viewModel.stateFlow.value.isSaving)
         advanceUntilIdle()
         assertEquals(
            PersonEffect.ShowMessage(stringProvider.getString(R.string.message_person_saved, "Ada Lovelace")),
            awaitItem(),
         )
         assertEquals(PersonEffect.NavigateBack(BackReason.Save), awaitItem())
         assertEquals(1, repository.created.size)
         assertFalse(viewModel.stateFlow.value.isSaving)
         expectNoEvents()
      }
   }

   @Test
   fun failedSave_logsOnceWithoutNavigation_andAllowsCancel() = runTest(mainDispatcherRule.testDispatcher) {
      Alog.set(useAndroidLog = true)
      ShadowLog.clear()
      val repository = FakePersonRepository().apply {
         createResult = Result.failure(IllegalStateException("write failed"))
      }
      val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         advanceUntilIdle()
         assertEquals(PersonEffect.ShowError(stringProvider.getString(R.string.error_person_save)), awaitItem())
         assertEquals(1, ShadowLog.getLogsForTag("<-PersonViewModel").count { it.type == Log.ERROR })
         assertFalse(viewModel.stateFlow.value.isSaving)
         expectNoEvents()
         viewModel.onIntent(PersonIntent.Cancel)
         advanceUntilIdle()
         assertEquals(PersonEffect.NavigateBack(BackReason.Cancel), awaitItem())
         expectNoEvents()
      }
   }

}
