package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.data.remote.SeedApi
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.di.effectModule
import de.rogallab.mobile.shared.di.imageStorageModule
import de.rogallab.mobile.shared.di.networkModule
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

         // Shared provides the generic Retrofit and OkHttp infrastructure.
         modules(
            networkModule(
               baseUrl = Globals.baseUrl,
               isDebug = BuildConfig.DEBUG,
            )
         )

         // A5_10 provides the People-specific webservice client and repository.
         modules(appModule())
         modules(effectModule())

         // Shared utilities and local image storage.
         modules(utilitiesModule())
         modules(imageStorageModule(Globals.imageDirectoryName))
      }

      // Seed PeopleApi only when its People table is empty.
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
 * - A5_10_PeopleRetrofit übernimmt UI, Navigation und UDF aus A5_01, ersetzt
 *   aber die lokale Room-Persistenz vollständig durch PeopleApi und Retrofit.
 *
 * - networkModule() aus Shared stellt HttpLoggingInterceptor, OkHttpClient, Json
 *   und Retrofit bereit. Base-URL und Debug-Status kommen aus der konkreten App.
 *
 * - appModule() ergänzt darauf die projektspezifische Schnittstelle
 *   IPersonWebservice. Retrofit erzeugt daraus den Client für den Webservice.
 *
 * - Die Beispieldaten werden ebenfalls vom Android-Client initialisiert:
 *   SeedApi prüft zunächst GET /people/count und sendet die 26 Personen nur an
 *   einen leeren Server. Die WebAPI selbst benötigt damit kein automatisches Seed.
 *
 * - imageStorageModule() bleibt erhalten, weil Galerie, Kamera und Seed private
 *   lokale Dateien liefern. Über Retrofit wird nur der Pfad als nullable JSON-
 *   String übertragen.
 */
