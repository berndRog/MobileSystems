package de.rogallab.mobile.ui.news

import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeNewsRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val stringProvider = FakeStringProvider()
   private val article = Article(
      url = "https://example.test/article",
      title = "Kotlin News",
   )

   private fun createViewModel(repository: FakeNewsRepository) =
      NewsViewModel(
         _newsRepository = repository,
         _stringProvider = stringProvider,
         _effectDelegate = EffectDelegate(),
      )

   @Test
   fun search_blankText_emitsRequiredErrorWithoutRepositoryCall() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakeNewsRepository()
         val viewModel = createViewModel(repository)

         viewModel.effects.test {
            viewModel.onIntent(NewsIntent.SearchTextChanged("   "))
            viewModel.onIntent(NewsIntent.Search)
            advanceUntilIdle()

            val effect = awaitItem() as NewsEffect.ShowError
            assertEquals(
               stringProvider.getString(R.string.error_search_required),
               effect.message,
            )
            assertTrue(repository.searchRequests.isEmpty())
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun search_success_updatesArticlesAndFinishesLoading() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakeNewsRepository().apply {
            searchResult = Result.success(listOf(article))
         }
         val viewModel = createViewModel(repository)

         viewModel.onIntent(NewsIntent.SearchTextChanged("  kotlin  "))
         viewModel.onIntent(NewsIntent.Search)
         advanceUntilIdle()

         assertEquals(
            FakeNewsRepository.SearchRequest("kotlin", 1),
            repository.searchRequests.single(),
         )
         assertEquals(listOf(article), viewModel.stateFlow.value.articles)
         assertFalse(viewModel.stateFlow.value.isLoading)
      }

   @Test
   fun search_failure_emitsLoadErrorAndFinishesLoading() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakeNewsRepository().apply {
            searchResult = Result.failure(IllegalStateException("network failed"))
         }
         val viewModel = createViewModel(repository)

         viewModel.effects.test {
            viewModel.onIntent(NewsIntent.SearchTextChanged("kotlin"))
            viewModel.onIntent(NewsIntent.Search)
            advanceUntilIdle()

            val effect = awaitItem() as NewsEffect.ShowError
            assertEquals(
               stringProvider.getString(R.string.error_news_load),
               effect.message,
            )
            assertFalse(viewModel.stateFlow.value.isLoading)
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun detail_emitsNavigationEffectWithArticle() =
      runTest(mainDispatcherRule.testDispatcher) {
         val viewModel = createViewModel(FakeNewsRepository())

         viewModel.effects.test {
            viewModel.onIntent(NewsIntent.Detail(article))
            advanceUntilIdle()

            val effect = awaitItem() as NewsEffect.NavigateToArticle
            assertEquals(article, effect.article)
            cancelAndIgnoreRemainingEvents()
         }
      }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Tests prüfen Eingabevalidierung, State-Übergänge und einmalige Effects
 *   unabhängig von Retrofit und Compose.
 *
 * Lernziele:
 *
 * - UDF-Verhalten eines ViewModels mit kontrollierten Coroutines testen.
 * - Repository-Aufrufe, Ladezustand und Navigation gemeinsam absichern.
 */
