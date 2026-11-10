package de.rogallab.mobile.ui.people.create_detail

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.rememberSnackbarController
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.comp.PersonAdapter
import de.rogallab.mobile.ui.theme.AppTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonSnackbarTest {
   @get:Rule val compose = createComposeRule()

   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val repository = RecordingRepository()

   @Test fun successfulSaveShowsSnackbar() {
      showCreateScreen()
      fillRequiredFields()
      compose.onNodeWithText(context.getString(R.string.action_save), useUnmergedTree = true).performClick()

      val message = context.getString(R.string.message_person_saved, "Ada Lovelace")
      compose.waitUntil(5_000) { compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
      compose.onNodeWithText(message).assertIsDisplayed()
      compose.runOnIdle { assertEquals("Ada Lovelace", repository.created.single().fullName) }
   }

   @Test fun failedSaveShowsErrorAndFormRemainsUsable() {
      repository.createResult = Result.failure(IllegalStateException("write failed"))
      showCreateScreen()
      fillRequiredFields()
      compose.onNodeWithText(context.getString(R.string.action_save), useUnmergedTree = true).performClick()

      val error = context.getString(R.string.error_person_save)
      compose.waitUntil(5_000) { compose.onAllNodesWithText(error).fetchSemanticsNodes().isNotEmpty() }
      compose.onNodeWithText(error).assertIsDisplayed()
      compose.onNodeWithText("Ada").assertIsDisplayed()
      compose.onNodeWithText(context.getString(R.string.action_save)).assertIsEnabled()
      compose.onNodeWithText(context.getString(R.string.action_cancel)).assertIsEnabled()
      compose.onNodeWithText("Lovelace").performTextReplacement("Byron")
      compose.onNodeWithText("Byron").assertIsDisplayed()
   }

   @Test fun formStaysVisibleAndActionsAreBlockedWhileSaving() {
      val pending = CompletableDeferred<Result<Unit>>()
      repository.pendingCreate = pending
      showCreateScreen()
      fillRequiredFields()
      compose.onNodeWithText(context.getString(R.string.action_save), useUnmergedTree = true).performClick()

      compose.waitUntil(5_000) { repository.createCalls == 1 }
      compose.onNodeWithText("Ada").assertIsDisplayed()
      compose.onNodeWithText("Lovelace").assertIsDisplayed()
      compose.onNodeWithText(context.getString(R.string.action_save)).assertIsNotEnabled()
      compose.onNodeWithText(context.getString(R.string.action_cancel)).assertIsNotEnabled()

      pending.complete(Result.failure(IllegalStateException("write failed")))
      compose.waitUntil(5_000) {
         compose.onAllNodesWithText(context.getString(R.string.error_person_save))
            .fetchSemanticsNodes().isNotEmpty()
      }
      compose.onNodeWithText("Ada").assertIsDisplayed()
      compose.onNodeWithText(context.getString(R.string.action_save)).assertIsEnabled()
   }

   private fun showCreateScreen() {
      val viewModel = PersonViewModel(
         null, repository, StringProvider(context), PersonValidator(context), EffectDelegate()
      )
      compose.setContent {
         val host = remember { SnackbarHostState() }
         val controller = rememberSnackbarController(host)
         AppTheme {
            Scaffold(snackbarHost = { SnackbarHost(host) }) { padding ->
               PersonAdapter(
                  viewModel = viewModel,
                  onMessage = controller::showMessage,
                  onError = controller::showError,
                  modifier = androidx.compose.ui.Modifier.padding(padding),
               )
            }
         }
      }
   }

   private fun fillRequiredFields() {
      compose.onNodeWithText(context.getString(R.string.firstname)).performTextInput("Ada")
      compose.onNodeWithText(context.getString(R.string.lastname)).performTextInput("Lovelace")
      Espresso.closeSoftKeyboard()
      compose.onNodeWithText(context.getString(R.string.action_save), useUnmergedTree = true)
         .performScrollTo().assertIsEnabled()
   }

   private class RecordingRepository : IPersonRepository {
      var createResult: Result<Unit> = Result.success(Unit)
      var pendingCreate: CompletableDeferred<Result<Unit>>? = null
      var createCalls = 0
      val created = mutableListOf<Person>()
      override fun observeAll(): Flow<Result<List<Person>>> = flowOf(Result.success(emptyList()))
      override suspend fun findById(id: String): Result<Person?> = Result.success(null)
      override suspend fun create(person: Person): Result<Unit> {
         createCalls++
         val result = pendingCreate?.await() ?: createResult
         if (result.isSuccess) created += person
         return result
      }
      override suspend fun update(person: Person): Result<Unit> = Result.success(Unit)
      override suspend fun remove(person: Person): Result<Unit> = Result.success(Unit)
   }
}
