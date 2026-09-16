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

         // Shared still provides generic utilities and local image storage.
         modules(utilitiesModule())
         modules(imageStorageModule(Globals.imageDirectoryName))
      }

      // Seed PeopleImagesApi only when its People table is empty.
      val seedApi: SeedApi = get()
      appScope.launch {
         seedApi.seedPerson()
      }
   }

   companion object {
      private const val TAG = "<-MainApplication"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A5_11_PeopleImagesRetrofit übernimmt UI, Navigation und UDF aus A5_10 und
 *   ergänzt PeopleImagesApi sowie die getrennte Übertragung von Bilddateien.
 *
 * - Die Beispieldaten werden jetzt ebenfalls vom Android-Client initialisiert:
 *   SeedApi prüft zunächst GET /people/count und sendet die 26 Personen nur an
 *   einen leeren Server. Die WebAPI selbst benötigt damit kein automatisches Seed.
 *
 * - appModule() registriert OkHttp, Retrofit, IPersonWebservice, SeedApi und das
 *   PersonRepository. Die ViewModels arbeiten weiterhin gegen IPersonRepository.
 *
 * - imageStorageModule() bleibt erhalten, weil Galerie, Kamera und Seed zunächst
 *   private lokale Dateien liefern. Nach dem getrennten Upload werden diese
 *   temporären Dateien gelöscht; dauerhaft bleibt die Server-URL erhalten.
 */
