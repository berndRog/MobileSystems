package de.rogallab.mobile.di

import androidx.navigation3.runtime.NavKey
import androidx.room.Room
import coil.ImageLoader
import de.rogallab.mobile.AppStart
import de.rogallab.mobile.createImageLoader
import de.rogallab.mobile.data.local.IArticleDao
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.remote.INewsWebservice
import de.rogallab.mobile.data.remote.network.ApiKeyInterceptor
import de.rogallab.mobile.data.remote.network.BearerTokenInterceptor
import de.rogallab.mobile.data.remote.network.NetworkConnection
import de.rogallab.mobile.data.remote.network.ConnectivityInterceptor
import de.rogallab.mobile.data.remote.network.createOkHttpClient
import de.rogallab.mobile.data.remote.network.createRetrofit
import de.rogallab.mobile.data.remote.network.createWebservice
import de.rogallab.mobile.data.repositories.ArticleRepository
import de.rogallab.mobile.data.repositories.NewsRepository
import de.rogallab.mobile.domain.IArticleRepository
import de.rogallab.mobile.domain.INewsRepository
import de.rogallab.mobile.domain.utilities.logError
import de.rogallab.mobile.domain.utilities.logInfo
import de.rogallab.mobile.ui.features.article.ArticlesViewModel
import de.rogallab.mobile.ui.features.news.NewsViewModel
import de.rogallab.mobile.ui.navigation.INavHandler
import de.rogallab.mobile.ui.navigation.Nav3ViewModelTopLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val defModules: Module = module {
   val tag = "<-defModules"

   logInfo(tag, "factory   -> CoroutineExceptionHandler")
   factory<CoroutineExceptionHandler> {
      CoroutineExceptionHandler { _, exception ->
         logError(tag, "Coroutine exception: ${exception.localizedMessage}")
      }
   }
   // Provide Dispatchers
   //logInfo(tag, "single    -> MainDispatcher:CoroutineDispatcher")
   //single<CoroutineDispatcher>(named("MainDispatcher")) { Dispatchers.Main }
   logInfo(tag, "single    -> DispatcherIo:CoroutineDispatcher")
   single<CoroutineDispatcher>(named("DispatcherIo")) { Dispatchers.IO }
   //logInfo(tag, "single    -> DispatcherDefault:CoroutineDispatcher")
   //single<CoroutineDispatcher>(named("DispatcherDefault")) { Dispatchers.Default }

   logInfo(tag, "single    -> createImageLoader")
   single<ImageLoader> { createImageLoader(androidContext()) }

   logInfo(tag, "viewModel -> Nav3ViewModelTopLevel as INavHandler (with params)")
   viewModel<Nav3ViewModelTopLevel> { (startDestination: NavKey) ->  // Parameter for startDestination
      Nav3ViewModelTopLevel(startDestination = startDestination)
   } bind INavHandler::class

   logInfo(tag, "viewModel -> NewsViewModel")
   viewModel<NewsViewModel> { (navHandler: INavHandler) ->
      NewsViewModel(
         _repository = get<INewsRepository>(),
         _imageLoader = get<ImageLoader>(),
         navHandler = navHandler,
      )
   }
   logInfo(tag, "viewModel -> ArticlesViewModel")
   viewModel<ArticlesViewModel> { (navHandler: INavHandler) ->
      ArticlesViewModel(
         _repository = get<IArticleRepository>(),
         _navHandler = navHandler,
      )
   }

   // local Room Database -----------------------------------------------------
   logInfo(tag, "single    -> AppDatabase")
   single<AppDatabase> {
      Room.databaseBuilder(
         context = androidContext(),
         klass = AppDatabase::class.java,
         name = AppStart.Companion.DATABASE_NAME
      ).build()
   }
   logInfo(tag, "single    -> IArticleDao")
   single<IArticleDao> { get<AppDatabase>().createArticleDao() }


   // remote OkHttp/Retrofit Webservice ---------------------------------------
   logInfo(tag, "single    -> NetworkConnection")
   single<NetworkConnection> {
      NetworkConnection(context = androidContext())
   }
   logInfo(tag, "single    -> ConnectivityInterceptor")
   single<ConnectivityInterceptor> {
      ConnectivityInterceptor(get<NetworkConnection>())
   }

   logInfo(tag, "single    -> InterceptorApiKey")
   single<ApiKeyInterceptor> { ApiKeyInterceptor(AppStart.Companion.API_KEY) }

   logInfo(tag, "single    -> InterceptorBearerToken")
   single<BearerTokenInterceptor> { BearerTokenInterceptor() }

   logInfo(tag, "single    -> HttpLoggingInterceptor")
   single<HttpLoggingInterceptor> {
      HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY  }
   }
   logInfo(tag, "single    -> OkHttpClient")
   single<OkHttpClient> {
      createOkHttpClient(
         bearerToken = get<BearerTokenInterceptor>(),
         apiKey = get<ApiKeyInterceptor>(),
         networkConnectivity = get<ConnectivityInterceptor>(),
         loggingInterceptor = get<HttpLoggingInterceptor>()
      )
   }

   logInfo(tag, "single    -> GsonConverterFactory")
   single<GsonConverterFactory> { GsonConverterFactory.create() }

   logInfo(tag, "single    -> Retrofit")
   single<Retrofit> {
      createRetrofit(
         okHttpClient = get<OkHttpClient>(),
         gsonConverterFactory = get<GsonConverterFactory>()
      )
   }
   logInfo(tag, "single    -> INewsWebservice")
   single<INewsWebservice> {
      createWebservice<INewsWebservice>(
         get<Retrofit>(),
         "INewsWebservice"
      )
   }

   // Provide IPersonRepository, injecting the `viewModelScope`
   logInfo(tag, "single    -> PersonRepository: IPersonRepository")
   single<INewsRepository> {
      NewsRepository(
         _newsWebservice = get<INewsWebservice>(),
         _dispatcher = get<CoroutineDispatcher>(named("DispatcherIo")   )
      )
   }
   logInfo(tag, "single    -> ArticleRepository: IArticleRepository")
   single<IArticleRepository> {
      ArticleRepository(
         _articleDao = get<IArticleDao>(),
         _dispatcher = get<CoroutineDispatcher>(named("DispatcherIo")),
         _exceptionHandler = get<CoroutineExceptionHandler>()
      )
   }

}
