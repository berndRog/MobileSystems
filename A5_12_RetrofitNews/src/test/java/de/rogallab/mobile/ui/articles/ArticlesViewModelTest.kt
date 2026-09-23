package de.rogallab.mobile.ui.articles

import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeArticleRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticlesViewModelTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val stringProvider = FakeStringProvider()
   private val article = Article(
      url = "https://example.test/article",
      title = "Kotlin News",
   )

   private fun createViewModel(repository: FakeArticleRepository) =
      ArticlesViewModel(
         _articleRepository = repository,
         _stringProvider = stringProvider,
         _effectDelegate = EffectDelegate(),
      )

   @Test
   fun observeArticles_updatesStateFromRepositoryFlow() =
      runTest(mainDispatcherRule.testDispatcher) {
         val viewModel = createViewModel(FakeArticleRepository(listOf(article)))

         advanceUntilIdle()

         assertEquals(listOf(article), viewModel.stateFlow.value.articles)
         assertFalse(viewModel.stateFlow.value.isLoading)
      }

   @Test
   fun observeArticles_failure_emitsLoadError() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakeArticleRepository().apply {
            articlesFlow.value = Result.failure(IllegalStateException("load failed"))
         }
         val viewModel = createViewModel(repository)

         viewModel.effects.test {
            advanceUntilIdle()

            val effect = awaitItem() as ArticlesEffect.ShowError
            assertEquals(
               stringProvider.getString(R.string.error_articles_load),
               effect.message,
            )
            assertFalse(viewModel.stateFlow.value.isLoading)
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun requestRemove_emitsConfirmationWithoutDeleting() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakeArticleRepository(listOf(article))
         val viewModel = createViewModel(repository)
         advanceUntilIdle()

         viewModel.effects.test {
            viewModel.onIntent(ArticlesIntent.RequestRemove(article.url))
            advanceUntilIdle()

            val effect = awaitItem() as ArticlesEffect.ConfirmRemove
            assertEquals(article.url, effect.url)
            assertEquals(
               stringProvider.getString(R.string.confirm_article_delete),
               effect.message,
            )
            assertEquals(emptyList<String>(), repository.removedUrls)
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun confirmRemove_successDeletesArticleAndEmitsMessage() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakeArticleRepository(listOf(article))
         val viewModel = createViewModel(repository)
         advanceUntilIdle()

         viewModel.effects.test {
            viewModel.onIntent(ArticlesIntent.ConfirmRemove(article.url))
            advanceUntilIdle()

            val effect = awaitItem() as ArticlesEffect.ShowMessage
            assertEquals(
               stringProvider.getString(R.string.message_article_deleted),
               effect.message,
            )
            assertEquals(listOf(article.url), repository.removedUrls)
            assertEquals(emptyList<Article>(), viewModel.stateFlow.value.articles)
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun confirmRemove_failureKeepsArticleAndEmitsError() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakeArticleRepository(listOf(article)).apply {
            removeResult = Result.failure(IllegalStateException("remove failed"))
         }
         val viewModel = createViewModel(repository)
         advanceUntilIdle()

         viewModel.effects.test {
            viewModel.onIntent(ArticlesIntent.ConfirmRemove(article.url))
            advanceUntilIdle()

            val effect = awaitItem() as ArticlesEffect.ShowError
            assertEquals(
               stringProvider.getString(R.string.error_article_delete),
               effect.message,
            )
            assertEquals(listOf(article), viewModel.stateFlow.value.articles)
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun detail_emitsNavigationEffectWithArticle() =
      runTest(mainDispatcherRule.testDispatcher) {
         val viewModel = createViewModel(FakeArticleRepository())

         viewModel.effects.test {
            viewModel.onIntent(ArticlesIntent.Detail(article))
            advanceUntilIdle()

            val effect = awaitItem() as ArticlesEffect.NavigateToArticle
            assertEquals(article, effect.article)
            cancelAndIgnoreRemainingEvents()
         }
      }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Tests decken Room-Beobachtung, Löschbestätigung, Erfolgs-/Fehler-Effects
 *   und Navigation ab, ohne eine Datenbank oder Compose zu starten.
 *
 * Lernziele:
 *
 * - Reaktive State-Updates und einmalige Effects gemeinsam testen.
 * - Sicherstellen, dass erst die bestätigte Aktion das Repository verändert.
 */
