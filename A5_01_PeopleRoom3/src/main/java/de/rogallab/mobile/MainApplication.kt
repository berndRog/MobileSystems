package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.data.local.SeedDatabase
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

      Alog.set(
         useAndroidLog = true,
         isVerbose = true,
         isDebug = true,
         isInfo = true,
         isComp = false,
      )

      // Initialize Koin dependency injection.
      Alog.i(TAG, "onCreate(): startKoin{...}")
      startKoin {
         androidLogger(Level.DEBUG)
         androidContext(androidContext = this@MainApplication)

         // A5_01 owns its Room database and DAO in appModule().
         modules(appModule())
         modules(effectModule())

         // Generic utilities and image storage remain reusable Shared services.
         modules(utilitiesModule())
         modules(imageStorageModule(Globals.imageDirectoryName))
      }

      // Seed the local Room-3 database once when it is still empty.
      val seedDatabase: SeedDatabase = get()
      appScope.launch {
         seedDatabase.seedPerson()
      }
   }

   companion object {
      private const val TAG = "<-MainApplication"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A5_01_PeopleRoom3 verwendet weiterhin gemeinsame Infrastruktur aus Shared,
 *   beispielsweise Logging, StringProvider und Image-Storage.
 *
 * - Die Persistenz gehört dagegen bewusst zum Beispielmodul selbst. Deshalb
 *   wird kein databaseModule() aus Shared geladen. AppDatabase, DAO, DTO und
 *   Repository werden in appModule() von A5_01 registriert.
 *
 * - Dadurch ist im Kurs direkt sichtbar, welche Klassen zur Room-Schicht eines
 *   konkreten Projekts gehören und welche Infrastruktur allgemein wiederverwendbar
 *   ist.
 */
