package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.di.defModules
import de.rogallab.mobile.domain.utilities.logInfo
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AppStart : Application() {

   override fun onCreate() {
      super.onCreate()

      val maxMemory = (Runtime.getRuntime().maxMemory() / 1024 ).toInt()
      logInfo(TAG, "onCreate() maxMemory $maxMemory kB")

      logInfo(TAG, "onCreate(): startKoin{...}")
      startKoin {
         // Log Koin into Android logger
         androidLogger(Level.DEBUG)
         // Reference Android context
         androidContext(this@AppStart)
         // Load modules
         modules(defModules)
      }
   }

   companion object {
      private const val TAG = "<-AppStart"
      const val DATABASE_NAME:    String = "A7_10_RetrofitNews.db"
      const val DATABASE_VERSION: Int    = 1

      const val BASE_URL: String = "https://newsapi.org/"
      const val API_KEY:  String = BuildConfig.NEWS_API_KEY
      const val BEARER_TOKEN:  String = ""

      const val isInfo = true
      const val isDebug = true

   }
}