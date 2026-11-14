package de.rogallab.mobile.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.navigation.comp.AppNavigation
import de.rogallab.mobile.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
   @get:Rule val compose = createComposeRule()

   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val repository = RecordingRepository()
   private val testModule = module {
      single<IPersonRepository> { repository }
      single { AppTiming(delayRead = 10L, delayWrite = 10L, animationDuration = 10) }
   }

   @org.junit.Before fun setUp() {
      // Override the repository and demonstration timing before creating the UI.
      loadKoinModules(testModule)
      compose.setContent { AppTheme { AppNavigation() } }
      awaitList()
   }

   @org.junit.After fun tearDown() {
      unloadKoinModules(testModule)
   }

   @Test fun createThenCancelReturnsToList() {
      openCreate()
      compose.onNodeWithText(context.getString(R.string.action_cancel), useUnmergedTree = true).performClick()
      awaitList()
      compose.onNodeWithText("Test Person").assertIsDisplayed()
      compose.runOnIdle { assertEquals(0, repository.created.size) }
   }

   @Test fun successfulSaveReturnsToListAndSnackbarSurvivesDestinationChange() {
      openCreate()
      compose.onNodeWithText(context.getString(R.string.firstname)).performTextInput("Ada")
      compose.onNodeWithText(context.getString(R.string.lastname)).performTextInput("Lovelace")
      Espresso.closeSoftKeyboard()
      compose.onNodeWithText(context.getString(R.string.action_save), useUnmergedTree = true).performScrollTo().performClick()

      val message = context.getString(R.string.message_person_saved, "Ada Lovelace")
      compose.waitUntil(15_000) {
         compose.onAllNodesWithText(context.getString(R.string.people_list)).fetchSemanticsNodes().isNotEmpty() &&
            compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
      }
      compose.onNodeWithText(context.getString(R.string.people_list)).assertIsDisplayed()
      compose.onNodeWithText(message).assertIsDisplayed()
      compose.onNodeWithText("Ada Lovelace").assertIsDisplayed()
      assertSnackbarAboveListFab(message)
      compose.runOnIdle { assertEquals("Ada Lovelace", repository.created.single().fullName) }
   }

   @Test fun deleteConfirmationSnackbarStaysAboveListFab() {
      compose.onNodeWithText("Test Person").performTouchInput { swipeLeft() }

      val message = context.getString(R.string.message_person_remove_confirm, "Test", "Person")
      compose.waitUntil(15_000) {
         compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
      }
      compose.onNodeWithText(message).assertIsDisplayed()
      compose.onNodeWithText(context.getString(R.string.action_confirm)).assertIsDisplayed()
      assertSnackbarAboveListFab(message)
      compose.runOnIdle { assertEquals(0, repository.removed.size) }
   }

   @Test fun systemBackReturnsFromPersonToList() {
      openCreate()
      Espresso.pressBack()
      awaitList()
      compose.onNodeWithText("Test Person").assertIsDisplayed()
      compose.runOnIdle { assertEquals(0, repository.created.size) }
   }

   private fun openCreate() {
      compose.onNodeWithText(context.getString(R.string.action_create), useUnmergedTree = true).performClick()
      compose.waitUntil(15_000) {
         compose.onAllNodesWithText(context.getString(R.string.person_create)).fetchSemanticsNodes().isNotEmpty()
      }
      compose.onNodeWithText(context.getString(R.string.person_create)).assertIsDisplayed()
   }

   private fun awaitList() {
      compose.waitUntil(15_000) {
         compose.onAllNodesWithText("Test Person").fetchSemanticsNodes().isNotEmpty()
      }
      compose.onNodeWithText(context.getString(R.string.people_list)).assertIsDisplayed()
   }

   private fun assertSnackbarAboveListFab(message: String) {
      val snackbarBottom = compose.onNodeWithText(message).getUnclippedBoundsInRoot().bottom
      val fabTop = compose.onNodeWithTag("peopleCreateFab").getUnclippedBoundsInRoot().top
      assertTrue("Snackbar must remain above the list FAB", snackbarBottom < fabTop)
   }

   private class RecordingRepository : IPersonRepository {
      private val initial = Person(firstName = "Test", lastName = "Person", id = "test-person")
      private val people = MutableStateFlow(Result.success(listOf(initial)))
      val created = mutableListOf<Person>()
      val removed = mutableListOf<Person>()

      override fun observeAll(): Flow<Result<List<Person>>> = people
      override suspend fun findById(id: String): Result<Person?> =
         Result.success(people.value.getOrThrow().find { it.id == id })
      override suspend fun create(person: Person): Result<Unit> {
         created += person
         people.value = Result.success(people.value.getOrThrow() + person)
         return Result.success(Unit)
      }
      override suspend fun update(person: Person): Result<Unit> = Result.success(Unit)
      override suspend fun remove(person: Person): Result<Unit> {
         removed += person
         people.value = Result.success(people.value.getOrThrow().filterNot { it.id == person.id })
         return Result.success(Unit)
      }
   }
}
