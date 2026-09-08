package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.di.effectModule
import de.rogallab.mobile.shared.di.imageStorageModule
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

      // Initialize Koin dependency injection.
      Alog.i(TAG, "onCreate(): startKoin{...}")
      startKoin {
         androidLogger(Level.DEBUG)
         androidContext(androidContext = this@MainApplication)

         // A5_11 owns its Retrofit web-service and repository configuration.
         modules(appModule())
         modules(effectModule())

         // Shared still provides generic utilities and temporary image storage.
         modules(utilitiesModule())
         modules(imageStorageModule(Globals.imageDirectoryName))
      }
   }

   companion object {
      private const val TAG = "<-MainApplication"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A5_11_PeopleRetrofit übernimmt UI, Navigation und UDF aus A5_01, ersetzt
 *   aber die lokale Room-Persistenz vollständig durch PeopleApi und Retrofit.
 *
 * - Es gibt deshalb keine lokale AppDatabase, kein DAO und kein SeedDatabase.
 *   Die 26 Beispieldatensätze werden vom Server bereitgestellt.
 *
 * - appModule() registriert OkHttp, Retrofit, IPersonWebservice und das neue
 *   PersonRepository. Die ViewModels arbeiten weiterhin gegen IPersonRepository.
 *
 * - imageStorageModule() bleibt erhalten, weil Galerie und Kamera zunächst eine
 *   private lokale Datei liefern. Diese Datei existiert nur bis zum erfolgreichen
 *   Multipart-Upload; das persistente Bild wird anschließend vom Server verwaltet.
 */
