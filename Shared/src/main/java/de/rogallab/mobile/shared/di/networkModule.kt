package de.rogallab.mobile.shared.di

import de.rogallab.mobile.shared.data.network.ConnectionChecker
import de.rogallab.mobile.shared.data.network.ConnectionInterceptor
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import de.rogallab.mobile.shared.domain.utilities.Alog
import java.util.concurrent.TimeUnit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalSerializationApi::class)
fun networkModule(
   baseUrl: String,
   isDebug: Boolean,

): Module = module {

   val tag = "<-networkModule"


   Alog.i(tag, "single    -> NetworkExceptionMapper")
   single<NetworkExceptionMapper> {
      NetworkExceptionMapper(
         context = androidContext(),
      )
   }

   Alog.i(tag, "single    -> ConnectionChecker")
   single<ConnectionChecker> {
      ConnectionChecker(
         context = androidContext(),
      )
   }

   Alog.i(tag, "single    -> ConnectionInterceptor")
   single<ConnectionInterceptor> {
      ConnectionInterceptor(
         _networkConnectionChecker = get<ConnectionChecker>(),
      )
   }

   Alog.i(tag, "single    -> HttpLoggingInterceptor")
   single<HttpLoggingInterceptor> {
      HttpLoggingInterceptor().apply {
         level = if (isDebug) {
            HttpLoggingInterceptor.Level.BASIC
         }
         else {
            HttpLoggingInterceptor.Level.NONE
         }
      }
   }

   Alog.i(tag, "single    -> OkHttpClient")
   single<OkHttpClient> {
      OkHttpClient.Builder()
         .addInterceptor(get<ConnectionInterceptor>())
         .addInterceptor(get<HttpLoggingInterceptor>())
         .connectTimeout(10, TimeUnit.SECONDS)
         .readTimeout(30, TimeUnit.SECONDS)
         .writeTimeout(30, TimeUnit.SECONDS)
         .callTimeout(60, TimeUnit.SECONDS)
         .build()
   }

   Alog.i(tag, "single    -> Json")
   single<Json> {
      Json {
         ignoreUnknownKeys = true  // JSON enthält evtl. zusätzliche Attribute, die nicht in DTOs abgebildet sind.
         explicitNulls = false     // JSON enthält evtl. optionale Attribute, die in DTOs als null abgebildet werden.
         coerceInputValues = true  // JSON enthält evtl. optionale Attribute, die in DTOs als default abgebildet werden.
         isLenient = false         // JSON erlaubt evtl. zusätzliche Whitespaces, Zeilenumbrüche oder Kommentare.
         prettyPrint = true        // JSON wird nicht formatiert, sondern kompakt übertragen. Für Debugging-Zwecke kann die Ausgabe in Logcat formatiert werden.
      }
   }

   Alog.i(tag, "single    -> Retrofit")
   single<Retrofit> {
      val json: Json = get()

      Retrofit.Builder()
         .baseUrl(baseUrl)
         .client(get<OkHttpClient>())
         .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
         )
         .build()
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die technische Netzwerk-Infrastruktur ist nicht an eine konkrete REST-API
 *   gebunden und kann deshalb von mehreren Vorlesungsprojekten verwendet werden.
 *
 * - HttpLoggingInterceptor protokolliert Requests nur in Debug-Builds.
 * - ConnectionChecker prüft das aktive Netzwerk und den von Android
 *   validierten Internetzugang.
 * - ConnectionInterceptor führt diese Prüfung unmittelbar vor jedem
 *   HTTP-Request aus und bricht den Request bei fehlender Verbindung ab.
 * - NetworkExceptionMapper übersetzt technische Netzwerk-, Timeout- und
 *   HTTP-Fehler in zentrale NetworkException-Typen mit Shared-Stringressourcen.
 * - OkHttpClient führt anschließend die eigentliche HTTP-Kommunikation aus.
 *   Connect-, Read-, Write- und Gesamt-Timeout verhindern unbegrenzt wartende
 *   Requests und werden zentral für alle verwendenden Projekte festgelegt.
 * - Json konfiguriert kotlinx.serialization für die JSON-Konvertierung.
 * - Retrofit verbindet Base-URL, OkHttpClient und JSON-Converter.
 *
 * - Die projektspezifische Webservice-Schnittstelle bleibt außerhalb von Shared.
 *   Retrofit erzeugt daraus im jeweiligen Projekt den Client für den Webservice.
 */
