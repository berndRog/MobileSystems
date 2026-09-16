package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.data.remote.SeedApi
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.di.effectModule
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
      Alog.set(true, true, true, true, false)
      startKoin {
         androidLogger(Level.DEBUG)
         androidContext(this@MainApplication)
         modules(appModule())
         modules(effectModule())
         modules(utilitiesModule())
         modules(imageStorageModule(Globals.imageDirectoryName))
      }
      val seedApi: SeedApi = get()
      appScope.launch { seedApi.seed() }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A5_13 behält Domain, UI und Beziehungen aus A5_02 bei und ersetzt Room
 *   durch die UsedCarsApi als Remote-Datenquelle.
 * - Person- und Fahrzeugbilder werden nach dem JSON-Request separat hochgeladen.
 */
