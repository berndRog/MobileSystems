package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.data.local.database.SeedDatabase
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.domain.utilities.AppLogger
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

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

      val koin = startKoin {
         androidContext(this@MainApplication)
         modules(appModule)
      }.koin

      koin.get<SeedDatabase>().start()
   }
}
