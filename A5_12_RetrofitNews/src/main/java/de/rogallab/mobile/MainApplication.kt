package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.di.effectModule
import de.rogallab.mobile.shared.di.utilitiesModule
import de.rogallab.mobile.shared.domain.utilities.Alog
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {
   override fun onCreate() {
      super.onCreate()

      Alog.set(
         useAndroidLog = true,
         isVerbose = true,
         isDebug = true,
         isInfo = true,
         isComp = false,
      )

      startKoin {
         androidLogger(Level.DEBUG)
         androidContext(this@MainApplication)
         modules(appModule())
         modules(effectModule())
         modules(utilitiesModule())
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - MainApplication startet nur die anwendungsweiten Dienste.
 * - Allgemeine Utilities und IStringProvider kommen aus Shared.
 * - Room 3, Retrofit und die Feature-ViewModels bleiben im A5_10-Modul.
 */
