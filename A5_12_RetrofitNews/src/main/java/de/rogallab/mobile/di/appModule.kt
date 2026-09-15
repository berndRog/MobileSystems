package de.rogallab.mobile.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.rogallab.mobile.BuildConfig
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.local.IArticleDao
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.remote.INewsWebservice
import de.rogallab.mobile.data.remote.network.ApiKeyInterceptor
import de.rogallab.mobile.data.repositories.ArticleRepository
import de.rogallab.mobile.data.repositories.NewsRepository
import de.rogallab.mobile.domain.IArticleRepository
import de.rogallab.mobile.domain.INewsRepository
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.article.ArticleEffect
import de.rogallab.mobile.ui.article.ArticleViewModel
import de.rogallab.mobile.ui.articles.ArticlesEffect
import de.rogallab.mobile.ui.articles.ArticlesViewModel
import de.rogallab.mobile.ui.news.NewsEffect
import de.rogallab.mobile.ui.news.NewsViewModel
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun appModule(): Module = module {
   single<AppDatabase> {
      Room.databaseBuilder<AppDatabase>(
         context = androidContext(),
         name = Globals.databaseName,
      )
         .setDriver(BundledSQLiteDriver())
         .setQueryCoroutineContext(Dispatchers.IO)
         .build()
   }
   single<IArticleDao> { get<AppDatabase>().createArticleDao() }
   single<IArticleRepository> { ArticleRepository(get()) }

   single { ApiKeyInterceptor(BuildConfig.NEWS_API_KEY) }
   single {
      HttpLoggingInterceptor().apply {
         level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.HEADERS
         }
         else {
            HttpLoggingInterceptor.Level.NONE
         }
      }
   }
   single<OkHttpClient> {
      OkHttpClient.Builder()
         .addInterceptor(get<ApiKeyInterceptor>())
         .addInterceptor(get<HttpLoggingInterceptor>())
         .build()
   }
   single<Retrofit> {
      Retrofit.Builder()
         .baseUrl(Globals.baseUrl)
         .client(get())
         .addConverterFactory(GsonConverterFactory.create())
         .build()
   }
   single<INewsWebservice> {
      get<Retrofit>().create(INewsWebservice::class.java)
   }
   single<INewsRepository> { NewsRepository(get()) }

   viewModel {
      NewsViewModel(
         _newsRepository = get<INewsRepository>(),
         _stringProvider = get<IStringProvider>(),
         _effectDelegate = get<EffectDelegate<NewsEffect>>(newsEffectQualifier),
      )
   }
   viewModel {
      ArticlesViewModel(
         _articleRepository = get<IArticleRepository>(),
         _stringProvider = get<IStringProvider>(),
         _effectDelegate = get<EffectDelegate<ArticlesEffect>>(articlesEffectQualifier),
      )
   }
   viewModel { parameters ->
      ArticleViewModel(
         article = parameters.get(),
         _articleRepository = get<IArticleRepository>(),
         _stringProvider = get<IStringProvider>(),
         _effectDelegate = get<EffectDelegate<ArticleEffect>>(articleEffectQualifier),
      )
   }
}
