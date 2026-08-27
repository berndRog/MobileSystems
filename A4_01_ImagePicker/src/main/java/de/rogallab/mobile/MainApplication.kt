package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.data.local.SeedDatabase
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.di.effectModule
import de.rogallab.mobile.shared.di.databaseModule
import de.rogallab.mobile.shared.di.imageStorageModule
import de.rogallab.mobile.shared.di.utilitiesModule
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {

   private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

   override fun onCreate() {
      super.onCreate()

      Alog.set(
         useAndroidLog = true,
         isVerbose = true,
         isDebug = true,
         isInfo = true,
         isComp = false
      )

      // Initialize Koin dependency injection
      Alog.i(TAG, "onCreate(): startKoin{...}")
      startKoin {
         // Use Koin Android Logger
         androidLogger(Level.DEBUG)
         // Reference to Android context
         androidContext(androidContext = this@MainApplication)
         // Load modules
         modules(appModule())
         modules(effectModule())
         modules(utilitiesModule())
         modules(databaseModule(Globals.databaseName))
         modules(imageStorageModule(Globals.imageDirectoryName))
      }

      val seedDatabase: SeedDatabase = get()
      val job = appScope.launch {
         seedDatabase.seedPerson()
      }
      appScope.launch {
         job.join()   // waits until seeding is finished
      }

   }

   companion object {
      private const val TAG = "<-MainApplication"
   }
}