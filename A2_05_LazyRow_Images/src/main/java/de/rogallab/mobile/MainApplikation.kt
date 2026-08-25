package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.di.appModules
import de.rogallab.mobile.domain.utilities.AppLogger
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {

   override fun onCreate() {
      super.onCreate()

      AppLogger.set(
         useAndroidLog = true,
         isVerbose = true,
         isDebug = true,
         isInfo = true,
         isComp = true
      )

      // Initialize any global state or dependencies here
      // Composition Root for Koin
      startKoin {
         AndroidLogger(Level.DEBUG)
         androidContext(androidContext = this@MainApplication)
         modules(appModules)
      }
   }

   companion object {
      private const val TAG = "<-MainApplication"
   }
}