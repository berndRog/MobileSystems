package de.rogallab.mobile.ui.news

import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.INewsRepository
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewsEffectIntegrationTest {
   @Test fun validationNavigationAndSearchError_useRealAndroidResources() = runBlocking {
      val context = ApplicationProvider.getApplicationContext<android.content.Context>()
      val repository = object : INewsRepository {
         override suspend fun search(searchText: String, page: Int): Result<List<Article>> =
            Result.failure(IllegalStateException("Controlled search failure"))
      }
      val store = ViewModelStore()
      try {
         val vm = withContext(Dispatchers.Main) {
            NewsViewModel(repository, StringProvider(context), EffectDelegate(),
               AppTiming(delayRead = 0, delayWrite = 0)).also {
               store.put("news", it)
               it.onIntent(NewsIntent.Search)
            }
         }
         withTimeout(5_000) {
            assertEquals(NewsEffect.ShowError(context.getString(R.string.error_search_required)), vm.effects.first())
            val article = Article(url = "https://example.test/article", title = "Controlled article")
            withContext(Dispatchers.Main) { vm.onIntent(NewsIntent.Detail(article)) }
            assertEquals(NewsEffect.NavigateToArticle(article), vm.effects.first())
            // No API key or live network is needed for the repository failure path.
            withContext(Dispatchers.Main) {
               vm.onIntent(NewsIntent.SearchTextChanged("Kotlin"))
               vm.onIntent(NewsIntent.Search)
            }
            assertEquals(NewsEffect.ShowError(context.getString(R.string.error_news_load)), vm.effects.first())
         }
      } finally {
         withContext(Dispatchers.Main) { store.clear() }
      }
   }
}
