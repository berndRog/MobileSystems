package de.rogallab.mobile.ui.article

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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleViewModelTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val stringProvider = FakeStringProvider()
   private val article = Article(
      url = "https://example.test/article",
      title = "Kotlin News",
   )

   private fun createViewModel(repository: FakeArticleRepository) =
      ArticleViewModel(
         article = article,
         _articleRepository = repository,
         _stringProvider = stringProvider,
         _effectDelegate = EffectDelegate(),
      )

   @Test
   fun save_successStoresArticleAndEmitsMessage() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakeArticleRepository()
         val viewModel = createViewModel(repository)

         viewModel.effects.test {
            viewModel.onIntent(ArticleIntent.Save)
            advanceUntilIdle()

            val effect = awaitItem() as ArticleEffect.ShowMessage
            assertEquals(
               stringProvider.getString(R.string.message_article_saved),
               effect.message,
            )
            assertEquals(listOf(article), repository.saved)
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun save_failureEmitsErrorWithoutStoredArticle() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakeArticleRepository().apply {
            saveResult = Result.failure(IllegalStateException("save failed"))
         }
         val viewModel = createViewModel(repository)

         viewModel.effects.test {
            viewModel.onIntent(ArticleIntent.Save)
            advanceUntilIdle()

            val effect = awaitItem() as ArticleEffect.ShowError
            assertEquals(
               stringProvider.getString(R.string.error_article_save),
               effect.message,
            )
            assertEquals(emptyList<Article>(), repository.saved)
            cancelAndIgnoreRemainingEvents()
         }
      }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Tests prüfen den einfachen Save-Ablauf einschließlich Erfolgs- und
 *   Fehler-Effect direkt gegen einen Repository-Fake.
 *
 * Lernziele:
 *
 * - Einen schreibenden ViewModel-Ablauf ohne zusätzliche Use-Case-Schicht testen.
 * - Repository-Seiteneffekt und UI-Rückmeldung gemeinsam verifizieren.
 */
